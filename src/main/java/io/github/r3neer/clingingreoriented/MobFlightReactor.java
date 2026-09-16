package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

/**
 * S07 runtime shared by pet follow, chase and flee. It observes every owned airborne segment cheaply,
 * delays strategic action by MobReactionTime, and never runs surface pathfinding while airborne.
 */
public final class MobFlightReactor {
    private static final int REACTION_FORECAST_TICKS=80;
    private static final double TARGET_CORRECTION_MIN_IMPROVEMENT=1.5D;
    private static final Map<LivingEntity,FlightState> STATES=new WeakHashMap<>();

    private static final class FlightState {
        final MobFlightReaction.State reaction=new MobFlightReaction.State();
        LandingSurfaces.Contact expectedContact;
        Vec3 expectedLanding;
    }

    private record Candidate(MobGravityPlanner.Transition transition,Vec3 landing,double score) {}

    private MobFlightReactor() {}

    public static void clear(LivingEntity entity){if(entity!=null)STATES.remove(entity);}

    public static void tick(LivingEntity entity){
        if(!(entity instanceof Mob mob)||entity.level().isClientSide())return;
        var gravityState=MobGravity.state(entity);
        if(gravityState.ownership!=MobGravity.Ownership.OWNED_EFFECT||!gravityState.airUsed
            ||AirChanges.grounded(entity)||!ClingingReoriented.hasEffect(entity)){
            clear(entity);return;
        }

        FlightState state=STATES.get(entity);
        Vec3 focus=currentFocus(mob);
        if(state==null){
            state=new FlightState();STATES.put(entity,state);
            MobFlightReaction.begin(state.reaction,null,focus);
            var baseline=MobFlightMonitor.forecast(entity);
            adoptSafeBaseline(entity,state,baseline);
            MobFlightReaction.observeDanger(entity,state.reaction,baseline);
            return;
        }

        var observation=state.expectedContact==null
            ?MobFlightMonitor.forecast(entity)
            :MobFlightMonitor.observe(entity,state.expectedContact,MobFlightMonitor.DEFAULT_HORIZON_TICKS);
        if(observation.status()==MobFlightMonitor.Status.SAFE_SUPPORT
            ||observation.status()==MobFlightMonitor.Status.SAFE_CHANGED_SUPPORT
            ||observation.status()==MobFlightMonitor.Status.EXPECTED_SUPPORT)
            adoptSafeBaseline(entity,state,observation);
        MobFlightReaction.observeDanger(entity,state.reaction,observation);
        if(focus!=null)MobFlightReaction.observeTarget(entity,state.reaction,focus);

        long now=entity.level().getGameTime();
        if(MobFlightReaction.dangerReady(state.reaction,now)){
            Vec3 latest=state.reaction.pendingTarget()!=null?state.reaction.pendingTarget():focus;
            if(tryCorrection(mob,state,latest,true))return;
            MobFlightReaction.consumeDanger(state.reaction);
        }
        if(MobFlightReaction.targetReady(state.reaction,now)){
            Vec3 latest=state.reaction.pendingTarget();
            if(tryCorrection(mob,state,latest,false))return;
            MobFlightReaction.consumeTarget(state.reaction);
        }
    }

    private static void adoptSafeBaseline(LivingEntity mob,FlightState state,MobFlightMonitor.Observation observation){
        if(observation==null||observation.hit()==null||!observation.hit().support())return;
        Direction gravity=GravityDirectionUtil.getOwnGravityDirection(mob);
        state.expectedContact=observation.hit().surface().contact();
        state.expectedLanding=landingPosition(mob,observation.hit().impactBody(),gravity);
    }

    private static boolean tryCorrection(Mob mob,FlightState state,Vec3 focus,boolean danger){
        if(!mob.hasEffect(Reorientation.EFFECT)||AirChanges.grounded(mob)||!MobGravity.canOwnEffectTransition(mob))return false;
        Vec3 baseline=state.expectedLanding;
        if(!danger&&baseline==null){
            var farther=MobFlightMonitor.forecast(mob,REACTION_FORECAST_TICKS);
            if(farther.hit()!=null&&farther.hit().support())baseline=landingPosition(mob,farther.hit().impactBody(),GravityDirectionUtil.getOwnGravityDirection(mob));
        }
        Candidate candidate=bestCandidate(mob,focus,danger,baseline);
        if(candidate==null||!executeCorrection(mob,candidate.transition()))return false;
        state.expectedContact=candidate.transition().landingContact();state.expectedLanding=candidate.landing();
        MobFlightReaction.begin(state.reaction,candidate.transition(),focus);
        return true;
    }

    private static Candidate bestCandidate(Mob mob,Vec3 focus,boolean danger,Vec3 baselineLanding){
        if(!danger&&(!finite(focus)||!finite(baselineLanding)))return null;
        double baselineDistance=!finite(focus)||!finite(baselineLanding)?Double.POSITIVE_INFINITY:baselineLanding.distanceTo(focus);
        Direction current=GravityDirectionUtil.getOwnGravityDirection(mob);Candidate best=null;
        for(Direction candidateGravity:Direction.values()){
            if(candidateGravity==current)continue;
            var evaluation=MobGravityPlanner.evaluateImmediate(mob,candidateGravity,REACTION_FORECAST_TICKS);
            if(!evaluation.accepted())continue;
            var transition=evaluation.transition();
            Vec3 landing=landingPosition(mob,transition.landingBody(),candidateGravity);
            if(!finite(landing))continue;
            double remaining=finite(focus)?landing.distanceTo(focus):0.0D;
            if(!danger&&baselineDistance-remaining<TARGET_CORRECTION_MIN_IMPROVEMENT)continue;
            double score=transition.physicalCost()+remaining;
            if(best==null||score<best.score())best=new Candidate(transition,landing,score);
        }
        return best;
    }

    /** Fresh evaluateImmediate output is committed in the same tick; momentum is preserved across the reorientation. */
    private static boolean executeCorrection(Mob mob,MobGravityPlanner.Transition transition){
        if(transition==null||!mob.hasEffect(Reorientation.EFFECT)||AirChanges.grounded(mob)||!MobGravity.canOwnEffectTransition(mob))return false;
        Direction previous=GravityDirectionUtil.getOwnGravityDirection(mob),target=transition.targetGravity();
        if(previous==target)return false;
        Vec3 velocity=mob.getDeltaMovement();
        var visual=GravityTransition.plan(previous,target,GravityTransition.headingFromYaw(previous,mob.getYRot()));
        if(!MobGravity.relocateTree(mob,target,transition.launchPosition()))return false;
        Payloads.visual(mob,visual);GravityTransition.applyYawGauge(mob,visual.yawDelta());mob.setDeltaMovement(velocity);
        var state=MobGravity.state(mob);state.ownership=MobGravity.Ownership.OWNED_EFFECT;state.ownedDirection=target;
        state.airUsed=true;state.retryAt=0;state.clearBorrow();
        return true;
    }

    private static Vec3 currentFocus(Mob mob){
        var navigation=MobGravityNavigation.state(mob);var intent=navigation.intent();
        if(MobGravityNavigation.active(mob)&&intent!=null&&intent.valid(mob)){
            Vec3 focus=intent.focus();if(finite(focus))return focus;
        }
        if(mob instanceof TamableAnimal pet&&pet.getOwner() instanceof LivingEntity owner&&owner.isAlive()&&owner.level()==mob.level())
            return owner.position();
        return null;
    }

    private static Vec3 landingPosition(LivingEntity mob,net.minecraft.world.phys.AABB landingBody,Direction gravity){
        if(mob==null||landingBody==null||gravity==null)return null;
        return RotationUtil.getCenterAlignedPosition(landingBody,mob.getDimensions(mob.getPose()),gravity);
    }

    private static boolean finite(Vec3 value){return value!=null&&Double.isFinite(value.x+value.y+value.z);}
}
