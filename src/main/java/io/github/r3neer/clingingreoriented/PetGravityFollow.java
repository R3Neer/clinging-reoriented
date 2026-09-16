package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * S06 online executor for one pet FollowOwnerGoal. Planning stays pure in S05; this class owns only
 * approach/revalidation/commit/landing state and a small expiring memory of failed local maneuvers.
 */
public final class PetGravityFollow {
    public enum Phase { IDLE, APPROACH, REVALIDATE, COMMITTED, LANDING_CONFIRM, RECOVERY }

    private static final int TRANSITION_HORIZON_TICKS=80;
    private static final int LANDING_CONFIRM_TICKS=2;
    private static final long FAILED_MANEUVER_COOLDOWN_TICKS=80L;
    private static final long IDLE_REPLAN_TICKS=10L;
    private static final long NO_PROGRESS_TICKS=40L;
    private static final double PROGRESS_EPS=.35D;
    private static final double OWNER_REPLAN_DISTANCE_SQR=4.0D;
    private static final int FLIGHT_GRACE_TICKS=20;
    private static final int MAX_EXCLUSIONS=8;

    public static final class State {
        private Phase phase=Phase.IDLE;
        private MobGravityLocalPlanner.Plan plan;
        private Vec3 ownerAnchor;
        private PathNavigation routeNavigation;
        private boolean routeOwned;
        private MobGravityLocalPlanner.ManeuverKey committedKey;
        private long flightDeadline;
        private int landingTicks;
        private double bestFrontierDistance=Double.POSITIVE_INFINITY;
        private long progressAt;
        private long nextIdlePlanAt;
        private Vec3 idlePlanOwnerAnchor;
        private Direction idlePlanGravity;
        private final Map<MobGravityLocalPlanner.ManeuverKey,Long> excludedUntil=new HashMap<>();

        public Phase phase(){return phase;}
        public MobGravityLocalPlanner.Plan plan(){return plan;}
        public MobGravityLocalPlanner.ManeuverKey activeKey(){
            return committedKey!=null?committedKey:plan==null?null:plan.maneuverKey();
        }
        public int exclusionCount(){return excludedUntil.size();}
    }

    private PetGravityFollow() {}

    public static boolean active(State state){return state!=null&&state.phase!=Phase.IDLE;}

    /**
     * Used from FollowOwnerGoal.canUse when vanilla's start-distance gate said no. It only wakes the goal
     * for a real gravity transition; ordinary WALK remains vanilla behavior.
     */
    public static boolean shouldWake(TamableAnimal pet,LivingEntity owner,State state,float stopDistance){
        if(state==null)return false;
        if(active(state))return canRemainOwned(pet,owner,state);
        return prepareTransition(pet,owner,state,stopDistance,false);
    }

    /** Whether an already-owned special segment should keep FollowOwnerGoal alive. */
    public static boolean canContinue(TamableAnimal pet,LivingEntity owner,State state){
        return active(state)&&canRemainOwned(pet,owner,state);
    }

    /**
     * @return true when the special executor owns this FollowOwnerGoal tick and vanilla tick must be cancelled.
     */
    public static boolean tick(TamableAnimal pet,LivingEntity owner,State state,double speed,float stopDistance){
        if(state==null)return false;
        if(state.phase==Phase.COMMITTED||state.phase==Phase.LANDING_CONFIRM||state.phase==Phase.RECOVERY)
            return tickFlightOrRecovery(pet,owner,state,speed,stopDistance);

        if(!planningContext(pet,owner)){
            releaseRoute(pet,state);clearPlan(state);return false;
        }
        if(state.phase==Phase.IDLE&&!prepareTransition(pet,owner,state,stopDistance,false))return false;

        if(state.ownerAnchor==null||owner.position().distanceToSqr(state.ownerAnchor)>OWNER_REPLAN_DISTANCE_SQR){
            state.excludedUntil.clear();
            releaseRoute(pet,state);clearPlan(state);
            if(!prepareTransition(pet,owner,state,stopDistance,true))return false;
        }

        var plan=state.plan;
        if(plan==null||plan.kind()!=MobGravityLocalPlanner.Kind.TRANSITION){clearPlan(state);return false;}
        Direction current=GravityDirectionUtil.getOwnGravityDirection(pet);
        var key=plan.maneuverKey();
        if(key==null||current!=key.sourceGravity()){
            releaseRoute(pet,state);clearPlan(state);
            return prepareTransition(pet,owner,state,stopDistance,true);
        }

        if(state.phase==Phase.APPROACH){
            double distance=pet.position().distanceTo(plan.frontier());
            double arrivalRadius=Math.clamp(Math.max(.55D,pet.getBbWidth()*.75D),.55D,1.25D);
            if(distance<=arrivalRadius){
                releaseRoute(pet,state);state.phase=Phase.REVALIDATE;
            }else{
                if(!ensureRoute(pet,state,speed)){
                    rememberFailure(pet,state,key);clearPlan(state);
                    return prepareTransition(pet,owner,state,stopDistance,true);
                }
                long now=pet.level().getGameTime();
                if(distance<state.bestFrontierDistance-PROGRESS_EPS){state.bestFrontierDistance=distance;state.progressAt=now;}
                else if(now-state.progressAt>=NO_PROGRESS_TICKS){
                    rememberFailure(pet,state,key);releaseRoute(pet,state);clearPlan(state);
                    return prepareTransition(pet,owner,state,stopDistance,true);
                }
                return true;
            }
        }

        if(state.phase==Phase.REVALIDATE){
            if(!AirChanges.grounded(pet)||FluidContext.intersects(pet)){
                rememberFailure(pet,state,key);clearPlan(state);return false;
            }
            // The commit seam independently reruns the physical forecast from this exact support.
            var committed=MobGravity.executePlannedTransition(pet,plan.terminalGravity(),TRANSITION_HORIZON_TICKS);
            if(committed==null){
                rememberFailure(pet,state,key);clearPlan(state);
                return prepareTransition(pet,owner,state,stopDistance,true);
            }
            releaseRoute(pet,state);
            state.committedKey=key;
            state.phase=Phase.COMMITTED;
            state.landingTicks=0;
            long now=pet.level().getGameTime();
            state.flightDeadline=now+(long)Math.ceil(committed.etaTicks())+FLIGHT_GRACE_TICKS;
            return true;
        }
        return false;
    }

    /** Called from FollowOwnerGoal.stop. Approach is relinquished; a committed fall keeps its safety state. */
    public static void goalStopped(TamableAnimal pet,State state){
        if(state==null)return;
        releaseRoute(pet,state);
        if(state.phase==Phase.APPROACH||state.phase==Phase.REVALIDATE)clearPlan(state);
    }

    private static boolean tickFlightOrRecovery(TamableAnimal pet,LivingEntity owner,State state,double speed,float stopDistance){
        releaseRoute(pet,state);
        if(!pet.isAlive()){clearPlan(state);return false;}
        if(FluidContext.intersects(pet)){
            if(state.committedKey!=null)rememberFailure(pet,state,state.committedKey);
            clearPlan(state);
            return false;
        }

        boolean grounded=AirChanges.grounded(pet);
        if(state.phase==Phase.COMMITTED){
            if(grounded){state.phase=Phase.LANDING_CONFIRM;state.landingTicks=1;return true;}
            if(pet.level().getGameTime()>state.flightDeadline){
                if(state.committedKey!=null)rememberFailure(pet,state,state.committedKey);
                state.phase=Phase.RECOVERY;state.landingTicks=0;
            }
            return true;
        }

        if(state.phase==Phase.LANDING_CONFIRM){
            if(!grounded){state.phase=Phase.COMMITTED;state.landingTicks=0;return true;}
            if(++state.landingTicks<LANDING_CONFIRM_TICKS)return true;
            // Real stable support is the new root of the online graph. A successful edge is not blacklisted.
            clearPlan(state);
            if(planningContext(pet,owner)&&prepareTransition(pet,owner,state,stopDistance,true))return true;
            return false;
        }

        if(state.phase==Phase.RECOVERY){
            if(!grounded){state.landingTicks=0;return true;}
            if(++state.landingTicks<LANDING_CONFIRM_TICKS)return true;
            clearPlan(state);
            if(planningContext(pet,owner)&&prepareTransition(pet,owner,state,stopDistance,true))return true;
            return false;
        }
        return false;
    }

    private static boolean prepareTransition(TamableAnimal pet,LivingEntity owner,State state,float stopDistance,boolean force){
        if(!planningContext(pet,owner))return false;
        long now=pet.level().getGameTime();pruneFailures(state,now);
        if(!force&&!idlePlanningDue(pet,owner,state,now))return false;
        Vec3 anchor=owner.position();double radius=Math.max(stopDistance,Math.max(.75D,pet.getBbWidth()));
        if(!MobGravityPlanningBudget.tryAcquire(pet))return false;
        var goal=followGoal(anchor,radius);
        Set<MobGravityLocalPlanner.ManeuverKey> excluded=Set.copyOf(state.excludedUntil.keySet());
        var plan=MobGravityLocalPlanner.plan(pet,goal,TRANSITION_HORIZON_TICKS,excluded);
        if(plan.kind()!=MobGravityLocalPlanner.Kind.TRANSITION||plan.maneuverKey()==null){
            rememberIdleMiss(pet,owner,state,now);return false;
        }
        clearIdleThrottle(state);
        state.plan=plan;state.ownerAnchor=anchor;state.phase=Phase.APPROACH;state.committedKey=null;
        state.routeNavigation=null;state.routeOwned=false;state.landingTicks=0;state.flightDeadline=0;
        state.bestFrontierDistance=pet.position().distanceTo(plan.frontier());state.progressAt=now;
        return true;
    }

    private static boolean idlePlanningDue(TamableAnimal pet,LivingEntity owner,State state,long now){
        Direction gravity=GravityDirectionUtil.getOwnGravityDirection(pet);
        return state.idlePlanOwnerAnchor==null||state.idlePlanGravity!=gravity
            ||owner.position().distanceToSqr(state.idlePlanOwnerAnchor)>OWNER_REPLAN_DISTANCE_SQR
            ||now>=state.nextIdlePlanAt;
    }

    private static void rememberIdleMiss(TamableAnimal pet,LivingEntity owner,State state,long now){
        state.idlePlanOwnerAnchor=owner.position();
        state.idlePlanGravity=GravityDirectionUtil.getOwnGravityDirection(pet);
        state.nextIdlePlanAt=now+IDLE_REPLAN_TICKS;
    }

    private static void clearIdleThrottle(State state){
        state.idlePlanOwnerAnchor=null;state.idlePlanGravity=null;state.nextIdlePlanAt=0;
    }

    private static MobGravityLocalPlanner.Goal followGoal(Vec3 anchor,double radius){
        double radiusSqr=radius*radius;
        return new MobGravityLocalPlanner.Goal(){
            @Override public Vec3 focus(){return anchor;}
            @Override public boolean satisfied(Vec3 position,Direction gravity){return position.distanceToSqr(anchor)<=radiusSqr;}
            @Override public double heuristic(Vec3 position,Direction gravity){return Math.max(0.0D,position.distanceTo(anchor)-radius);}
        };
    }

    private static boolean planningContext(TamableAnimal pet,LivingEntity owner){
        return pet!=null&&owner!=null&&!pet.level().isClientSide()&&pet.isAlive()&&owner.isAlive()&&pet.level()==owner.level()
            &&!pet.unableToMoveToOwner()&&ClingingReoriented.hasEffect(pet)&&MobGravity.supported(pet)
            &&AirChanges.grounded(pet)&&AirChanges.grounded(owner)&&!FluidContext.intersects(pet);
    }

    private static boolean canRemainOwned(TamableAnimal pet,LivingEntity owner,State state){
        if(pet==null||!pet.isAlive()||pet.unableToMoveToOwner())return false;
        if(state.phase==Phase.COMMITTED||state.phase==Phase.LANDING_CONFIRM||state.phase==Phase.RECOVERY)return true;
        return owner!=null&&owner.isAlive()&&pet.level()==owner.level();
    }

    private static boolean ensureRoute(TamableAnimal pet,State state,double speed){
        var plan=state.plan;if(plan==null)return false;
        var path=plan.walkPath();
        if(path==null||path.getNodeCount()==0)return false;
        PathNavigation navigation=pet.getNavigation();
        if(state.routeOwned&&state.routeNavigation==navigation&&!navigation.isDone())return true;
        boolean accepted=navigation.moveTo(path.copy(),speed);
        state.routeNavigation=navigation;state.routeOwned=accepted;
        return accepted;
    }

    private static void releaseRoute(TamableAnimal pet,State state){
        if(state.routeOwned&&state.routeNavigation!=null)state.routeNavigation.stop();
        state.routeOwned=false;state.routeNavigation=null;
    }

    private static void rememberFailure(TamableAnimal pet,State state,MobGravityLocalPlanner.ManeuverKey key){
        if(key==null)return;long now=pet.level().getGameTime();
        state.excludedUntil.put(key,now+FAILED_MANEUVER_COOLDOWN_TICKS);
        pruneFailures(state,now);
        while(state.excludedUntil.size()>MAX_EXCLUSIONS){
            MobGravityLocalPlanner.ManeuverKey oldest=null;long expiry=Long.MAX_VALUE;
            for(var entry:state.excludedUntil.entrySet())if(entry.getValue()<expiry){oldest=entry.getKey();expiry=entry.getValue();}
            if(oldest==null)break;state.excludedUntil.remove(oldest);
        }
    }

    private static void pruneFailures(State state,long now){state.excludedUntil.entrySet().removeIf(entry->entry.getValue()<=now);}

    private static void clearPlan(State state){
        state.phase=Phase.IDLE;state.plan=null;state.ownerAnchor=null;state.routeNavigation=null;state.routeOwned=false;
        state.committedKey=null;state.flightDeadline=0;state.landingTicks=0;state.bestFrontierDistance=Double.POSITIVE_INFINITY;state.progressAt=0;
    }
}
