package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative coarse Gravity Fall phase machine. It deliberately owns no render quaternion. */
public final class GravityFallState {
    public static final int START_AIRBORNE_TICKS=12;
    public static final int BODY_LANDING_HORIZON=LandingPrediction.MAX_TICKS;

    private GravityFallState() {}

    public static void tick(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(!eligible(player) || AirChanges.grounded(player) || !ClingingReoriented.controlsPhysics(player)){
            reset(player,state);
            return;
        }
        if(state.airborneTicks<START_AIRBORNE_TICKS){
            if(state.gravityFallActive)reset(player,state);
            return;
        }

        var candidate=LandingPrediction.predict(player,BODY_LANDING_HORIZON);
        boolean imminent=candidate.isPresent() && candidate.get().etaTicks()<=BODY_LANDING_HORIZON;

        if(!state.gravityFallActive){
            // Do not flash into Gravity Fall if the first eligible frame is already the landing window.
            if(state.landingCommitted || imminent)return;
            state.gravityFallActive=true;
            state.gravityFallLanding=false;
            state.gravityFallLandingGravity=GravityDirectionUtil.getGravityDirection(player);
            state.gravityFallLandingEtaTicks=0.0D;
            GravityFallSync.publish(player,GravityFallSync.Phase.START,null);
            return;
        }

        Direction landingTarget=null;
        double landingEta=0.0D;
        if(state.landingCommitted){
            landingTarget=state.landingGravity;
            landingEta=state.landingEtaTicks;
        }else if(imminent){
            landingTarget=candidate.get().gravity();
            landingEta=candidate.get().etaTicks();
        }

        if(landingTarget!=null){
            landingEta=Math.max(0.0D,Math.min(BODY_LANDING_HORIZON,landingEta));
            boolean changed=!state.gravityFallLanding || state.gravityFallLandingGravity!=landingTarget;
            state.gravityFallLanding=true;
            state.gravityFallLandingGravity=landingTarget;
            state.gravityFallLandingEtaTicks=landingEta;
            if(changed)GravityFallSync.publish(player,GravityFallSync.Phase.LAND,landingTarget,landingEta);
        }else if(state.gravityFallLanding){
            state.gravityFallLanding=false;
            state.gravityFallLandingEtaTicks=0.0D;
            GravityFallSync.publish(player,GravityFallSync.Phase.RESUME,null);
        }
    }

    public static void reset(ServerPlayer player){reset(player,ClingingReoriented.data(player));}

    private static void reset(ServerPlayer player,PlayerData state){
        if(state.gravityFallActive)GravityFallSync.publish(player,GravityFallSync.Phase.RESET,null);
        state.clearGravityFall();
    }

    private static boolean eligible(ServerPlayer player){
        return player!=null && player.isAlive() && !player.isSpectator() && !player.isSleeping() && !player.isPassenger()
            && !player.isFallFlying() && !player.isInWater() && !player.isInLava() && !player.getAbilities().flying;
    }
}
