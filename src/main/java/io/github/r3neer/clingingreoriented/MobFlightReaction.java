package io.github.r3neer.clingingreoriented;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Shared S07 perception memory. Observations are cheap and continuous; strategic action becomes
 * eligible only after the mob's characteristic reaction delay. Continuing changes never push the
 * original deadline forward, so moving targets cannot starve a reaction forever.
 */
public final class MobFlightReaction {
    public static final double MATERIAL_TARGET_DISTANCE_SQR=4.0D;

    public static final class State {
        private MobGravityPlanner.Transition committed;
        private Vec3 targetAnchor;
        private Vec3 pendingTarget;
        private long targetReactAt;
        private MobFlightMonitor.Status pendingDanger;
        private long dangerReactAt;
        private MobFlightMonitor.Observation lastObservation;

        public MobGravityPlanner.Transition committed(){return committed;}
        public Vec3 targetAnchor(){return targetAnchor;}
        public Vec3 pendingTarget(){return pendingTarget;}
        public long targetReactAt(){return targetReactAt;}
        public MobFlightMonitor.Status pendingDanger(){return pendingDanger;}
        public long dangerReactAt(){return dangerReactAt;}
        public MobFlightMonitor.Observation lastObservation(){return lastObservation;}
        public boolean targetPending(){return pendingTarget!=null;}
        public boolean dangerPending(){return pendingDanger!=null;}
    }

    private MobFlightReaction() {}

    public static void begin(State state,MobGravityPlanner.Transition committed,Vec3 targetAnchor){
        if(state==null)return;
        state.committed=committed;
        state.targetAnchor=finite(targetAnchor)?targetAnchor:null;
        state.pendingTarget=null;state.targetReactAt=0;
        state.pendingDanger=null;state.dangerReactAt=0;state.lastObservation=null;
    }

    public static void clear(State state){begin(state,null,null);}

    public static void observeTarget(LivingEntity mob,State state,Vec3 currentTarget){
        if(mob==null||state==null)return;
        observeTarget(state,mob.level().getGameTime(),MobReactionTime.ticks(mob),currentTarget);
    }

    static void observeTarget(State state,long now,int reactionTicks,Vec3 currentTarget){
        if(state==null||!finite(currentTarget))return;
        int delay=Math.clamp(reactionTicks,MobReactionTime.MIN_TICKS,MobReactionTime.MAX_TICKS);
        if(state.targetAnchor==null){state.targetAnchor=currentTarget;return;}
        if(currentTarget.distanceToSqr(state.targetAnchor)<=MATERIAL_TARGET_DISTANCE_SQR){
            state.pendingTarget=null;state.targetReactAt=0;return;
        }
        if(state.pendingTarget==null)state.targetReactAt=now+delay;
        state.pendingTarget=currentTarget;
    }

    public static boolean targetReady(State state,long now){
        return state!=null&&state.pendingTarget!=null&&state.targetReactAt>0&&now>=state.targetReactAt;
    }

    /** Accept the latest observed target without changing the already-expired reaction decision. */
    public static Vec3 consumeTarget(State state){
        if(state==null||state.pendingTarget==null)return null;
        Vec3 result=state.pendingTarget;state.targetAnchor=result;state.pendingTarget=null;state.targetReactAt=0;return result;
    }

    public static void observeDanger(LivingEntity mob,State state,MobFlightMonitor.Observation observation){
        if(mob==null||state==null)return;
        observeDanger(state,mob.level().getGameTime(),MobReactionTime.ticks(mob),observation);
    }

    static void observeDanger(State state,long now,int reactionTicks,MobFlightMonitor.Observation observation){
        if(state==null)return;
        state.lastObservation=observation;
        if(observation==null||!observation.danger()){
            state.pendingDanger=null;state.dangerReactAt=0;return;
        }
        int delay=Math.clamp(reactionTicks,MobReactionTime.MIN_TICKS,MobReactionTime.MAX_TICKS);
        if(state.pendingDanger==null)state.dangerReactAt=now+delay;
        state.pendingDanger=observation.status();
    }

    public static boolean dangerReady(State state,long now){
        return state!=null&&state.pendingDanger!=null&&state.dangerReactAt>0&&now>=state.dangerReactAt;
    }

    public static MobFlightMonitor.Status consumeDanger(State state){
        if(state==null)return null;
        var result=state.pendingDanger;state.pendingDanger=null;state.dangerReactAt=0;return result;
    }

    public static void replaceCommitted(State state,MobGravityPlanner.Transition transition){
        if(state==null)return;state.committed=transition;state.pendingDanger=null;state.dangerReactAt=0;
    }

    private static boolean finite(Vec3 value){return value!=null&&Double.isFinite(value.x+value.y+value.z);}
}
