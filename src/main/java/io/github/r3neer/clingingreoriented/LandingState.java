package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

/** Short-lived server authority for the final landing window and its presentation commitment. */
public final class LandingState {
    private LandingState() {}

    public static void tick(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        boolean fluid=FluidContext.intersects(player);

        if(fluid){
            // Entering a fluid still tears down any solid-air landing/camera presentation from the
            // previous context. The one exception is a HOLD that was created by a gravity turn
            // requested while the player was already inside this fluid context: cancelling that
            // HOLD at END_SERVER_TICK would undo the very turn presentation we just accepted.
            if(!eligibleWithoutFluid(player) || !ClingingReoriented.controlsPhysics(player)
                || !state.freeFlightVisualHeld || !state.freeFlightVisualHeldInFluid){
                transferClear(player);
            }else{
                clearFluidTransientPreservingHold(state);
            }
            return;
        }

        // Once the player leaves fluid, a surviving HOLD is an ordinary dry free-flight HOLD again.
        // Re-entering a fluid later must therefore cross the normal transfer fence rather than being
        // mistaken for the same underwater interaction epoch.
        state.freeFlightVisualHeldInFluid=false;

        if(!eligibleWithoutFluid(player)){
            // Elytra/vehicles/death/etc. own the next presentation. Unlike an invalidated solid
            // landing surface, there is no Clinging landing frame to preserve here.
            transferClear(player);return;
        }
        if(AirChanges.grounded(player)){
            touchdown(player,gravity);return;
        }
        state.airborneTicks=Math.min(1_000_000,state.airborneTicks+1);
        if(!state.visualBaseKnown){state.visualBaseDirection=gravity;state.visualBaseKnown=true;}
        if(!ClingingReoriented.controlsPhysics(player)){
            // Losing physics ownership is not a landing cancellation/resume. Release the retained
            // camera outright so an anchor, foreign gravity source, expiry or other owner can render
            // its own frame instead of inheriting a frozen Clinging HOLD.
            transferClear(player);return;
        }

        Optional<LandingPrediction.Candidate> predicted=LandingPrediction.predict(player,LandingPrediction.MAX_TICKS);
        if(state.landingCommitted){
            if(gravity!=state.landingGravity || player.level().getGameTime()>state.landingDeadlineTick
                || predicted.isEmpty() || !same(predicted.get().contact(),state.landingContact)
                || !LandingSurfaces.revalidate(player,gravity,state.landingContact)){
                cancel(player,true);
            }else state.landingEtaTicks=predicted.get().etaTicks();
            return;
        }

        GravityTransition.TurnKind kind=kindFor(state,gravity);
        if(kind==null||predicted.isEmpty())return;
        if(predicted.get().etaTicks()<=LandingTiming.PRESENTATION_TICKS+1.0E-6D)commit(player,predicted.get(),kind);
    }

    /** Input-side check closes the one-tick gap between real touchdown and END_SERVER_TICK. */
    public static boolean committed(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(!state.landingCommitted)return false;
        if(!eligibleContext(player)){transferClear(player);return false;}
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        if(AirChanges.grounded(player)){touchdown(player,gravity);return false;}
        return true;
    }

    /** Surface invalidation while Clinging still owns physics: preserve the exact visible frame. */
    public static void cancel(ServerPlayer player,boolean visualBecameNonCanonical){
        var state=ClingingReoriented.data(player);
        if(state.landingCommitted&&state.freeFlightVisualHeld)Payloads.cancelLanding(player,true);
        state.clearLandingCommit();
        if(visualBecameNonCanonical)state.visualBaseKnown=false;
    }

    /** Connection teardown: no packet is useful because the play connection is going away. */
    public static void lifecycleClear(ServerPlayer player){
        clearTransient(ClingingReoriented.data(player));
    }

    /**
     * Ownership/teleport/fluid teardown while the connection remains alive. Release every retained
     * Clinging camera/landing presentation before another subsystem or interaction context takes over.
     */
    public static void transferClear(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(state.landingCommitted||state.freeFlightVisualHeld)Payloads.cancelLanding(player,false);
        clearTransient(state);
    }

    private static void clearTransient(PlayerData state){
        state.airborneTicks=0;state.clearLandingCommit();state.visualBaseKnown=false;
        state.freeFlightVisualHeld=false;state.freeFlightVisualHeldInFluid=false;
    }

    private static void clearFluidTransientPreservingHold(PlayerData state){
        state.airborneTicks=0;
        state.clearLandingCommit();
        state.visualBaseKnown=false;
    }

    private static void touchdown(ServerPlayer player,Direction gravity){
        var state=ClingingReoriented.data(player);boolean wasCommitted=state.landingCommitted;
        if(!wasCommitted&&state.freeFlightVisualHeld)Payloads.cancelLanding(player,false);
        state.freeFlightVisualHeld=false;state.freeFlightVisualHeldInFluid=false;state.airborneTicks=0;
        state.clearLandingCommit();state.visualBaseDirection=gravity;state.visualBaseKnown=true;
    }

    private static void commit(ServerPlayer player,LandingPrediction.Candidate candidate,GravityTransition.TurnKind kind){
        var state=ClingingReoriented.data(player);
        state.landingCommitted=true;state.landingContact=candidate.contact();state.landingGravity=candidate.gravity();state.landingKind=kind;
        state.landingEtaTicks=candidate.etaTicks();state.landingDeadlineTick=player.level().getGameTime()+(long)Math.ceil(candidate.etaTicks())+2L;state.landingSequence++;
        Payloads.land(player,candidate.gravity(),kind);
    }

    private static GravityTransition.TurnKind kindFor(PlayerData state,Direction target){
        if(!state.visualBaseKnown)return GravityTransition.TurnKind.HALF;
        if(state.visualBaseDirection==target)return null;
        if(state.visualBaseDirection.getOpposite()==target)return GravityTransition.TurnKind.HALF;
        return GravityTransition.TurnKind.QUARTER;
    }

    private static boolean same(LandingSurfaces.Contact a,LandingSurfaces.Contact b){return a!=null&&b!=null&&a.gravity()==b.gravity()&&a.key().equals(b.key());}

    private static boolean eligibleContext(ServerPlayer player){
        return eligibleWithoutFluid(player)&&!FluidContext.intersects(player);
    }

    private static boolean eligibleWithoutFluid(ServerPlayer player){
        return player.isAlive()&&!player.isSpectator()&&!player.isSleeping()&&!player.isPassenger()
            &&!player.isFallFlying()&&!player.getAbilities().flying;
    }
}
