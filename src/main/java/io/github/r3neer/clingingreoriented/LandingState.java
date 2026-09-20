package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;

/** Server authority for early landing acquisition and the shorter final presentation commitment. */
public final class LandingState {
    private static final double PRESENTATION_EPS=1.0E-6D;
    private static final int MAX_ACQUISITION_MISSES=1;
    private LandingState() {}

    public static void tick(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        boolean fluid=FluidContext.intersects(player);

        if(fluid){
            if(!eligibleWithoutFluid(player) || !ClingingReoriented.controlsPhysics(player)
                || state.landingCommitted || !state.freeFlightVisualHeld || !state.freeFlightVisualHeldInFluid){
                transferClear(player);
            }else{
                clearFluidTransientPreservingHold(state);
            }
            return;
        }

        state.freeFlightVisualHeldInFluid=false;

        if(!eligibleWithoutFluid(player)){
            transferClear(player);return;
        }
        if(AirChanges.grounded(player)){
            touchdown(player,gravity);return;
        }
        state.airborneTicks=Math.min(1_000_000,state.airborneTicks+1);
        if(!state.visualBaseKnown){state.visualBaseDirection=gravity;state.visualBaseKnown=true;}
        if(!ClingingReoriented.controlsPhysics(player)){
            transferClear(player);return;
        }

        Optional<LandingPrediction.Candidate> predicted=updateCandidate(player,state,gravity);
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
        if(LandingPolicy.shouldCommit(predicted.get(),state.landingCandidateStableTicks))commit(player,predicted.get(),kind);
    }

    static Optional<LandingPrediction.Candidate> currentPrediction(ServerPlayer player){
        if(player==null)return Optional.empty();
        var state=ClingingReoriented.data(player);
        if(!state.landingCandidateConfirmed || state.landingCandidate==null
            || state.landingCandidateTick!=player.level().getGameTime())return Optional.empty();
        return Optional.of(state.landingCandidate);
    }

    public static boolean committed(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(!state.landingCommitted)return false;
        if(!eligibleContext(player)){transferClear(player);return false;}
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        if(AirChanges.grounded(player)){touchdown(player,gravity);return false;}
        return true;
    }

    /**
     * Abort an already-issued LAND presentation while preserving the exact visible frame. A LAND
     * packet owns client presentation even when it did not originate from a pre-existing HOLD, so
     * cancellation is keyed by landingCommitted rather than freeFlightVisualHeld.
     */
    public static void cancel(ServerPlayer player,boolean visualBecameNonCanonical){
        var state=ClingingReoriented.data(player);
        if(state.landingCommitted)Payloads.cancelLanding(player,true);
        state.clearLandingCommit();state.clearLandingCandidate();
        // A cancelled LAND now eases back to the pre-landing HOLD, so the previous visual base stays canonical.
        if(visualBecameNonCanonical&&!state.freeFlightVisualHeld)state.visualBaseKnown=false;
    }

    public static void lifecycleClear(ServerPlayer player){clearTransient(ClingingReoriented.data(player));}

    public static void transferClear(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(state.landingCommitted||state.freeFlightVisualHeld)Payloads.cancelLanding(player,false);
        clearTransient(state);
    }

    private static Optional<LandingPrediction.Candidate> updateCandidate(ServerPlayer player,PlayerData state,Direction gravity){
        long tick=player.level().getGameTime();
        state.landingCandidateConfirmed=false;
        state.landingCandidateTick=tick;
        Optional<LandingPrediction.Candidate> predicted=LandingPrediction.predict(player,LandingPrediction.ACQUISITION_TICKS);
        if(predicted.isPresent()){
            var current=predicted.get();
            if(current.gravity()!=gravity || !LandingSurfaces.revalidate(player,gravity,current.contact())){
                state.clearLandingCandidate();
                return Optional.empty();
            }
            if(current.approach()==LandingPolicy.Approach.GRAZE){
                state.landingGrazeKey=current.contact().key();
                state.landingGrazeGravity=gravity;
                state.landingGrazeUntilTick=tick+1L;
                state.clearLandingCandidate();
                return Optional.empty();
            }
            boolean sameEpoch=state.landingCandidate!=null && state.landingCandidateRevision==state.revision
                && same(current.contact(),state.landingCandidate.contact());
            if(sameEpoch){
                state.landingCandidate=current;
                state.landingCandidateStableTicks=Math.min(1_000_000,state.landingCandidateStableTicks+1);
                state.landingCandidateMisses=0;
            }else{
                state.clearLandingCandidate();
                state.landingCandidate=current;
                state.landingCandidateStableTicks=1;
                state.landingCandidateRevision=state.revision;
            }
            state.landingCandidateTick=tick;
            state.landingCandidateConfirmed=true;
            return Optional.of(current);
        }

        if(state.landingCandidate!=null){
            boolean epochValid=state.landingCandidateRevision==state.revision
                && state.landingCandidate.gravity()==gravity
                && LandingSurfaces.revalidate(player,gravity,state.landingCandidate.contact());
            boolean safelyOutsidePresentation=state.landingCandidate.etaTicks()>LandingTiming.PRESENTATION_TICKS+PRESENTATION_EPS;
            if(epochValid && !state.landingCommitted && safelyOutsidePresentation
                && state.landingCandidateMisses<MAX_ACQUISITION_MISSES){
                state.landingCandidateMisses++;
                return Optional.empty();
            }
            state.clearLandingCandidate();
        }
        return Optional.empty();
    }

    private static void clearTransient(PlayerData state){
        state.airborneTicks=0;state.clearLandingCommit();state.clearLandingCandidate();state.clearLandingGraze();state.visualBaseKnown=false;
        state.freeFlightVisualHeld=false;state.freeFlightVisualHeldInFluid=false;
    }

    private static void clearFluidTransientPreservingHold(PlayerData state){state.airborneTicks=0;state.clearLandingCommit();state.clearLandingCandidate();state.clearLandingGraze();}

    /** Real support is authoritative: retire any remaining LAND/HOLD ownership exactly at touchdown. */
    private static void touchdown(ServerPlayer player,Direction gravity){
        var state=ClingingReoriented.data(player);boolean wasCommitted=state.landingCommitted;
        if(wasCommitted||state.freeFlightVisualHeld)Payloads.cancelLanding(player,false);
        state.freeFlightVisualHeld=false;state.freeFlightVisualHeldInFluid=false;state.airborneTicks=0;
        state.clearLandingCommit();state.clearLandingCandidate();state.clearLandingGraze();state.visualBaseDirection=gravity;state.visualBaseKnown=true;
    }

    private static void commit(ServerPlayer player,LandingPrediction.Candidate candidate,GravityTransition.TurnKind kind){
        var state=ClingingReoriented.data(player);
        state.landingCommitted=true;state.landingContact=candidate.contact();state.landingGravity=candidate.gravity();state.landingKind=kind;
        state.landingEtaTicks=candidate.etaTicks();state.landingDeadlineTick=player.level().getGameTime()+(long)Math.ceil(candidate.etaTicks())+2L;state.landingSequence++;
        Payloads.land(player,candidate.gravity(),kind,candidate.etaTicks());
    }

    private static GravityTransition.TurnKind kindFor(PlayerData state,Direction target){
        if(!state.visualBaseKnown)return GravityTransition.TurnKind.HALF;
        if(state.visualBaseDirection==target)return null;
        if(state.visualBaseDirection.getOpposite()==target)return GravityTransition.TurnKind.HALF;
        return GravityTransition.TurnKind.QUARTER;
    }

    private static boolean same(LandingSurfaces.Contact a,LandingSurfaces.Contact b){return a!=null&&b!=null&&a.gravity()==b.gravity()&&a.key().equals(b.key());}

    private static boolean eligibleContext(ServerPlayer player){return eligibleWithoutFluid(player)&&!FluidContext.intersects(player);}
    private static boolean eligibleWithoutFluid(ServerPlayer player){
        return player.isAlive()&&!player.isSpectator()&&!player.isSleeping()&&!player.isPassenger()
            &&!player.isFallFlying()&&!player.getAbilities().flying;
    }
}