package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

/**
 * Generic online gravity locomotion for mobs. Vanilla goals remain owners of intent; this controller
 * only owns a bounded gravity-specific movement segment when ordinary navigation cannot reach it.
 */
public final class MobGravityNavigation {
    public enum Phase { IDLE, APPROACH, REVALIDATE, COMMITTED, LANDING_CONFIRM, RECOVERY }

    private static final int TRANSITION_HORIZON_TICKS=80;
    private static final int LANDING_CONFIRM_TICKS=2;
    private static final int FLIGHT_GRACE_TICKS=20;
    private static final long FAILED_MANEUVER_COOLDOWN_TICKS=80L;
    private static final long IDLE_REPLAN_TICKS=10L;
    private static final long NO_PROGRESS_TICKS=40L;
    private static final double PROGRESS_EPS=.35D;
    private static final double INTENT_REPLAN_DISTANCE_SQR=4.0D;
    private static final int MAX_EXCLUSIONS=8;

    public sealed interface Intent permits EntityIntent,PositionIntent {
        Vec3 focus();
        boolean valid(Mob mob);
        double satisfiedRadius(Mob mob);
    }

    public record EntityIntent(Entity target) implements Intent {
        @Override public Vec3 focus(){return target==null?null:target.position();}
        @Override public boolean valid(Mob mob){
            return target!=null&&!target.isRemoved()&&target.isAlive()&&target.level()==mob.level();
        }
        @Override public double satisfiedRadius(Mob mob){
            if(target==null)return 1.5D;
            return Math.max(1.5D,(mob.getBbWidth()+target.getBbWidth())*.75D);
        }
    }

    public record PositionIntent(Vec3 target) implements Intent {
        @Override public Vec3 focus(){return target;}
        @Override public boolean valid(Mob mob){return finite(target);}
        @Override public double satisfiedRadius(Mob mob){return Math.max(.75D,mob.getBbWidth());}
    }

    public static final class State {
        private Phase phase=Phase.IDLE;
        private Intent intent;
        private double speed;
        private MobGravityLocalPlanner.Plan plan;
        private Vec3 focusAnchor;
        private PathNavigation routeNavigation;
        private boolean routeOwned;
        private MobGravityLocalPlanner.ManeuverKey committedKey;
        private long flightDeadline;
        private int landingTicks;
        private double bestFrontierDistance=Double.POSITIVE_INFINITY;
        private long progressAt;
        private long nextIdlePlanAt;
        private Vec3 idleFocusAnchor;
        private Direction idleGravity;
        private boolean forceReplan;
        private final Map<MobGravityLocalPlanner.ManeuverKey,Long> excludedUntil=new HashMap<>();

        public Phase phase(){return phase;}
        public Intent intent(){return intent;}
        public MobGravityLocalPlanner.Plan plan(){return plan;}
        public MobGravityLocalPlanner.ManeuverKey activeKey(){
            return committedKey!=null?committedKey:plan==null?null:plan.maneuverKey();
        }
        public int exclusionCount(){return excludedUntil.size();}
    }

    private MobGravityNavigation() {}

    public static State state(Mob mob){return MobGravity.state(mob).navigation;}
    public static boolean active(Mob mob){return mob!=null&&state(mob).phase!=Phase.IDLE;}
    public static boolean flightOwned(Mob mob){
        if(mob==null)return false;
        Phase phase=state(mob).phase;
        return phase==Phase.COMMITTED||phase==Phase.LANDING_CONFIRM||phase==Phase.RECOVERY;
    }

    /** A tamed animal following its owner is handled by PetGravityFollow, not by this generic bridge. */
    public static boolean reservedPetOwnerIntent(Mob mob,Entity target){
        return mob instanceof TamableAnimal pet&&target!=null&&target==pet.getOwner();
    }

    /** Called at the HEAD of a vanilla navigation request. Active special movement owns the request. */
    public static boolean interceptEntityRequest(Mob mob,Entity target,double speed){
        if(mob==null||!active(mob))return false;
        observeIntent(mob,new EntityIntent(target),speed);
        return true;
    }

    public static boolean interceptPositionRequest(Mob mob,Vec3 target,double speed){
        if(mob==null||!active(mob))return false;
        observeIntent(mob,new PositionIntent(target),speed);
        return true;
    }

    /** Called after vanilla attempted a route. Returns true only when a gravity segment took ownership. */
    public static boolean requestEntityAfterVanilla(Mob mob,Entity target,double speed,boolean ordinaryReachable){
        if(mob==null||target==null||reservedPetOwnerIntent(mob,target)||ordinaryReachable)return false;
        return activate(mob,new EntityIntent(target),speed,false);
    }

    public static boolean requestPositionAfterVanilla(Mob mob,Vec3 target,double speed,boolean ordinaryReachable){
        if(mob==null||ordinaryReachable)return false;
        return activate(mob,new PositionIntent(target),speed,false);
    }

    /** Wake seam for goals such as MeleeAttackGoal that may abort before they ever call moveTo. */
    public static boolean wakeEntity(Mob mob,LivingEntity target,double speed){
        if(mob==null||target==null||reservedPetOwnerIntent(mob,target))return false;
        if(active(mob)){observeIntent(mob,new EntityIntent(target),speed);return true;}
        return activate(mob,new EntityIntent(target),speed,true);
    }

    public static void tick(Mob mob){
        if(mob==null||mob.level().isClientSide())return;
        State s=state(mob);
        if(s.phase==Phase.IDLE)return;
        Intent intent=s.intent;
        if(intent==null||!intent.valid(mob)){clear(mob,s,true);return;}

        if(s.phase==Phase.COMMITTED||s.phase==Phase.LANDING_CONFIRM||s.phase==Phase.RECOVERY){
            tickFlight(mob,s);return;
        }
        if(!planningContext(mob)){clear(mob,s,true);return;}

        Vec3 focus=intent.focus();
        if(!finite(focus)){clear(mob,s,true);return;}
        if(s.forceReplan||s.focusAnchor==null||focus.distanceToSqr(s.focusAnchor)>INTENT_REPLAN_DISTANCE_SQR){
            s.forceReplan=false;
            releaseRoute(mob,s);
            MobGravityLocalPlanner.ManeuverKey previous=s.plan==null?null:s.plan.maneuverKey();
            s.plan=null;s.phase=Phase.IDLE;
            if(!activate(mob,intent,s.speed,true)&&previous!=null)rememberFailure(mob,s,previous);
            return;
        }

        var plan=s.plan;
        if(plan==null||plan.kind()!=MobGravityLocalPlanner.Kind.TRANSITION){clear(mob,s,false);return;}
        Direction current=GravityDirectionUtil.getOwnGravityDirection(mob);
        var key=plan.maneuverKey();
        if(key==null||current!=key.sourceGravity()){
            releaseRoute(mob,s);s.phase=Phase.IDLE;s.plan=null;
            activate(mob,intent,s.speed,true);return;
        }

        if(s.phase==Phase.APPROACH){
            double distance=mob.position().distanceTo(plan.frontier());
            double arrivalRadius=Math.clamp(Math.max(.55D,mob.getBbWidth()*.75D),.55D,1.25D);
            if(distance<=arrivalRadius){releaseRoute(mob,s);s.phase=Phase.REVALIDATE;}
            else{
                if(!ensureRoute(mob,s)){
                    rememberFailure(mob,s,key);clearPlanOnly(s);activate(mob,intent,s.speed,true);return;
                }
                long now=mob.level().getGameTime();
                if(distance<s.bestFrontierDistance-PROGRESS_EPS){s.bestFrontierDistance=distance;s.progressAt=now;}
                else if(now-s.progressAt>=NO_PROGRESS_TICKS){
                    rememberFailure(mob,s,key);releaseRoute(mob,s);clearPlanOnly(s);activate(mob,intent,s.speed,true);
                }
                return;
            }
        }

        if(s.phase==Phase.REVALIDATE){
            if(!AirChanges.grounded(mob)||FluidContext.intersects(mob)){
                rememberFailure(mob,s,key);clear(mob,s,false);return;
            }
            var committed=MobGravity.executePlannedTransition(mob,plan.terminalGravity(),TRANSITION_HORIZON_TICKS);
            if(committed==null){
                rememberFailure(mob,s,key);clearPlanOnly(s);activate(mob,intent,s.speed,true);return;
            }
            releaseRoute(mob,s);
            s.committedKey=key;s.phase=Phase.COMMITTED;s.landingTicks=0;
            long now=mob.level().getGameTime();
            s.flightDeadline=now+(long)Math.ceil(committed.etaTicks())+FLIGHT_GRACE_TICKS;
        }
    }

    public static void goalStopped(Mob mob){
        if(mob==null)return;State s=state(mob);releaseRoute(mob,s);
        if(s.phase==Phase.APPROACH||s.phase==Phase.REVALIDATE)clear(mob,s,true);
    }

    private static void tickFlight(Mob mob,State s){
        releaseRoute(mob,s);
        if(!mob.isAlive()){clear(mob,s,true);return;}
        if(FluidContext.intersects(mob)){
            if(s.committedKey!=null)rememberFailure(mob,s,s.committedKey);
            clear(mob,s,true);return;
        }
        boolean grounded=AirChanges.grounded(mob);
        if(s.phase==Phase.COMMITTED){
            if(grounded){s.phase=Phase.LANDING_CONFIRM;s.landingTicks=1;return;}
            if(mob.level().getGameTime()>s.flightDeadline){
                if(s.committedKey!=null)rememberFailure(mob,s,s.committedKey);
                s.phase=Phase.RECOVERY;s.landingTicks=0;
            }
            return;
        }
        if(s.phase==Phase.LANDING_CONFIRM){
            if(!grounded){s.phase=Phase.COMMITTED;s.landingTicks=0;return;}
            if(++s.landingTicks<LANDING_CONFIRM_TICKS)return;
            Intent intent=s.intent;double speed=s.speed;clearPlanOnly(s);
            if(intent!=null&&intent.valid(mob)&&planningContext(mob))activate(mob,intent,speed,true);
            return;
        }
        if(s.phase==Phase.RECOVERY){
            if(!grounded){s.landingTicks=0;return;}
            if(++s.landingTicks<LANDING_CONFIRM_TICKS)return;
            Intent intent=s.intent;double speed=s.speed;clearPlanOnly(s);
            if(intent!=null&&intent.valid(mob)&&planningContext(mob))activate(mob,intent,speed,true);
        }
    }

    private static boolean activate(Mob mob,Intent intent,double speed,boolean force){
        if(mob==null||intent==null||!intent.valid(mob)||!planningContext(mob))return false;
        State s=state(mob);long now=mob.level().getGameTime();pruneFailures(s,now);
        if(!force&&!idlePlanningDue(mob,intent,s,now))return false;
        Vec3 focus=intent.focus();if(!finite(focus))return false;
        double radius=intent.satisfiedRadius(mob);var goal=goal(focus,radius);
        Set<MobGravityLocalPlanner.ManeuverKey> excluded=Set.copyOf(s.excludedUntil.keySet());
        var plan=MobGravityLocalPlanner.plan(mob,goal,TRANSITION_HORIZON_TICKS,excluded);
        if(plan.kind()!=MobGravityLocalPlanner.Kind.TRANSITION||plan.maneuverKey()==null){
            rememberIdleMiss(mob,intent,s,now);return false;
        }
        s.phase=Phase.APPROACH;s.intent=intent;s.speed=sanitizeSpeed(speed);s.plan=plan;s.focusAnchor=focus;
        s.routeNavigation=null;s.routeOwned=false;s.committedKey=null;s.flightDeadline=0;s.landingTicks=0;s.forceReplan=false;
        s.bestFrontierDistance=mob.position().distanceTo(plan.frontier());s.progressAt=now;clearIdleThrottle(s);
        if(s.bestFrontierDistance<=Math.clamp(Math.max(.55D,mob.getBbWidth()*.75D),.55D,1.25D))s.phase=Phase.REVALIDATE;
        return true;
    }

    private static void observeIntent(Mob mob,Intent next,double speed){
        State s=state(mob);if(next==null||!next.valid(mob))return;
        if(!sameIntent(s.intent,next))s.forceReplan=true;
        s.intent=next;s.speed=sanitizeSpeed(speed);
    }

    private static boolean sameIntent(Intent a,Intent b){
        if(a==null||b==null||a.getClass()!=b.getClass())return false;
        if(a instanceof EntityIntent ea&&b instanceof EntityIntent eb)return ea.target()==eb.target();
        Vec3 av=a.focus(),bv=b.focus();return finite(av)&&finite(bv)&&av.distanceToSqr(bv)<=1.0D;
    }

    private static MobGravityLocalPlanner.Goal goal(Vec3 focus,double radius){
        double radiusSqr=radius*radius;
        return new MobGravityLocalPlanner.Goal(){
            @Override public Vec3 focus(){return focus;}
            @Override public boolean satisfied(Vec3 position,Direction gravity){return position.distanceToSqr(focus)<=radiusSqr;}
            @Override public double heuristic(Vec3 position,Direction gravity){return Math.max(0.0D,position.distanceTo(focus)-radius);}
        };
    }

    private static boolean planningContext(Mob mob){
        return mob.isAlive()&&!mob.isPassenger()&&!mob.isVehicle()&&!mob.isFallFlying()&&ClingingReoriented.hasEffect(mob)
            &&MobGravity.supported(mob)&&AirChanges.grounded(mob)&&!FluidContext.intersects(mob);
    }

    private static boolean ensureRoute(Mob mob,State s){
        var path=s.plan==null?null:s.plan.walkPath();if(path==null||path.getNodeCount()==0)return false;
        PathNavigation navigation=mob.getNavigation();
        if(s.routeOwned&&s.routeNavigation==navigation&&navigation.getPath()!=null&&!navigation.getPath().isDone())return true;
        boolean accepted=navigation.moveTo(path.copy(),s.speed);
        s.routeNavigation=navigation;s.routeOwned=accepted;return accepted;
    }

    private static void releaseRoute(Mob mob,State s){
        if(s.routeOwned&&s.routeNavigation!=null)s.routeNavigation.stop();
        s.routeOwned=false;s.routeNavigation=null;
    }

    private static boolean idlePlanningDue(Mob mob,Intent intent,State s,long now){
        Vec3 focus=intent.focus();Direction gravity=GravityDirectionUtil.getOwnGravityDirection(mob);
        return s.idleFocusAnchor==null||s.idleGravity!=gravity||!finite(focus)
            ||focus.distanceToSqr(s.idleFocusAnchor)>INTENT_REPLAN_DISTANCE_SQR||now>=s.nextIdlePlanAt;
    }

    private static void rememberIdleMiss(Mob mob,Intent intent,State s,long now){
        s.intent=intent;s.speed=sanitizeSpeed(s.speed);s.idleFocusAnchor=intent.focus();
        s.idleGravity=GravityDirectionUtil.getOwnGravityDirection(mob);s.nextIdlePlanAt=now+IDLE_REPLAN_TICKS;
    }

    private static void clearIdleThrottle(State s){s.idleFocusAnchor=null;s.idleGravity=null;s.nextIdlePlanAt=0;}

    private static void rememberFailure(Mob mob,State s,MobGravityLocalPlanner.ManeuverKey key){
        if(key==null)return;long now=mob.level().getGameTime();s.excludedUntil.put(key,now+FAILED_MANEUVER_COOLDOWN_TICKS);pruneFailures(s,now);
        while(s.excludedUntil.size()>MAX_EXCLUSIONS){
            MobGravityLocalPlanner.ManeuverKey oldest=null;long expiry=Long.MAX_VALUE;
            for(var entry:s.excludedUntil.entrySet())if(entry.getValue()<expiry){oldest=entry.getKey();expiry=entry.getValue();}
            if(oldest==null)break;s.excludedUntil.remove(oldest);
        }
    }

    private static void pruneFailures(State s,long now){s.excludedUntil.entrySet().removeIf(entry->entry.getValue()<=now);}

    private static void clearPlanOnly(State s){
        s.phase=Phase.IDLE;s.plan=null;s.focusAnchor=null;s.routeNavigation=null;s.routeOwned=false;s.committedKey=null;
        s.flightDeadline=0;s.landingTicks=0;s.bestFrontierDistance=Double.POSITIVE_INFINITY;s.progressAt=0;s.forceReplan=false;
    }

    private static void clear(Mob mob,State s,boolean clearIntent){
        releaseRoute(mob,s);clearPlanOnly(s);clearIdleThrottle(s);
        if(clearIntent){s.intent=null;s.speed=0.0D;s.excludedUntil.clear();}
    }

    private static double sanitizeSpeed(double speed){return Double.isFinite(speed)&&speed>0.0D?speed:1.0D;}
    private static boolean finite(Vec3 value){return value!=null&&Double.isFinite(value.x+value.y+value.z);}
}
