package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Gravity ownership survives contact loss. Transport never does. */
public final class PlayerData {
    public record SupportSample(long sequence,long tick,Vec3 position) {}
    public boolean owned; public boolean airChangeUsed; public boolean ownedAtDeath; public Direction selected=Direction.DOWN;
    public UUID support; public int supportId=-1; public Vec3 supportPosition; public net.minecraft.world.phys.AABB supportBox;
    public boolean retirementPending; public long nextRetirementAttempt; public Vec3 lastTransport=Vec3.ZERO; public long transportTick=Long.MIN_VALUE;
    public boolean carrying; public boolean groundedOnSurface; public boolean anchorBorrowed; public boolean anchorExpired; public boolean visualFrameOwned;
    public long visualSequence; public long lastRequest=-1; public long requestTick=-1; public int revision; public long supportSampleSequence;
    public long lastConsumedSupportSample=-1; public final java.util.ArrayDeque<SupportSample> supportHistory=new java.util.ArrayDeque<>(); public Payloads.MoveReference pendingMove;

    public int airborneTicks; public boolean landingCommitted; public LandingSurfaces.Contact landingContact; public Direction landingGravity=Direction.DOWN;
    public GravityTransition.TurnKind landingKind; public double landingEtaTicks; public long landingDeadlineTick; public Direction visualBaseDirection=Direction.DOWN;
    public boolean visualBaseKnown; public long landingSequence; public boolean freeFlightVisualHeld; public boolean freeFlightVisualHeldInFluid;
    public LandingPrediction.Candidate landingCandidate; public int landingCandidateStableTicks; public int landingCandidateMisses;
    public int landingCandidateRevision=-1; public long landingCandidateTick=Long.MIN_VALUE; public boolean landingCandidateConfirmed;
    public LandingSurfaces.SurfaceKey landingGrazeKey; public Direction landingGrazeGravity=Direction.DOWN; public long landingGrazeUntilTick=Long.MIN_VALUE;

    public boolean gravityFallActive; public boolean gravityFallLanding; public Direction gravityFallLandingGravity=Direction.DOWN;
    public double gravityFallLandingEtaTicks; public long gravityFallSequence;
    public Vec3 gravityFallLook; public long gravityFallLookSequence=-1L;
    public long gravityFallLookTick=Long.MIN_VALUE; public BodyOrientation.State gravityFallAeroBody;

    public Direction maceFallDirection=Direction.DOWN; public Vec3 maceFallLastPosition; public double maceFallDistance; public boolean maceFallActive;

    public Vec3 flightSafePosition; public boolean flightSafetyHolding; public Vec3 flightHeldVelocity=Vec3.ZERO;

    public void unbind(){support=null;supportId=-1;supportPosition=null;supportBox=null;lastTransport=Vec3.ZERO;groundedOnSurface=false;supportHistory.clear();supportSampleSequence=0;lastConsumedSupportSample=-1;pendingMove=null;}
    public void clearLandingCommit(){landingCommitted=false;landingContact=null;landingKind=null;landingEtaTicks=0.0D;landingDeadlineTick=0L;}
    public void clearLandingCandidate(){landingCandidate=null;landingCandidateStableTicks=0;landingCandidateMisses=0;landingCandidateRevision=-1;landingCandidateTick=Long.MIN_VALUE;landingCandidateConfirmed=false;}
    public void clearLandingGraze(){landingGrazeKey=null;landingGrazeGravity=Direction.DOWN;landingGrazeUntilTick=Long.MIN_VALUE;}
    public void clearGravityFall(){gravityFallActive=false;gravityFallLanding=false;gravityFallLandingGravity=Direction.DOWN;gravityFallLandingEtaTicks=0.0D;gravityFallLook=null;gravityFallLookSequence=-1L;gravityFallLookTick=Long.MIN_VALUE;gravityFallAeroBody=null;}
    public void clearMaceFall(){maceFallDirection=Direction.DOWN;maceFallLastPosition=null;maceFallDistance=0.0D;maceFallActive=false;}
    public void clearFlightSafetyHold(){flightSafetyHolding=false;flightHeldVelocity=Vec3.ZERO;}
    public void clearFlightSafety(){flightSafePosition=null;clearFlightSafetyHold();}
    public interface Holder { PlayerData clinging$data(); }
}
