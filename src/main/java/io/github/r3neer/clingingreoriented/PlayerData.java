package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Gravity ownership survives contact loss. Transport never does. */
public final class PlayerData {
    public record SupportSample(long sequence,long tick,Vec3 position) {}
    public boolean owned;
    public boolean airChangeUsed;
    public boolean ownedAtDeath;
    public Direction selected = Direction.DOWN;
    public UUID support;
    public int supportId = -1;
    public Vec3 supportPosition;
    public net.minecraft.world.phys.AABB supportBox;
    public boolean retirementPending;
    public long nextRetirementAttempt;
    public Vec3 lastTransport = Vec3.ZERO;
    public long transportTick = Long.MIN_VALUE;
    public boolean carrying;
    public boolean groundedOnSurface;
    public boolean anchorBorrowed;
    public boolean anchorExpired;
    public boolean visualFrameOwned;
    public long visualSequence;
    public long lastRequest = -1;
    public long requestTick = -1;
    public int revision;
    public long supportSampleSequence;
    public long lastConsumedSupportSample=-1;
    public final java.util.ArrayDeque<SupportSample> supportHistory = new java.util.ArrayDeque<>();
    public Payloads.MoveReference pendingMove;

    // Gamefeel update: server-side short-horizon landing state. Not persisted across lifecycle replacement.
    public int airborneTicks;
    public boolean landingCommitted;
    public LandingSurfaces.Contact landingContact;
    public Direction landingGravity=Direction.DOWN;
    public GravityTransition.TurnKind landingKind;
    public double landingEtaTicks;
    public long landingDeadlineTick;
    public Direction visualBaseDirection=Direction.DOWN;
    public boolean visualBaseKnown;
    public long landingSequence;
    public boolean freeFlightVisualHeld;

    // Gravity Fall is also transient server authority. Continuous orientation stays client-derived.
    public boolean gravityFallActive;
    public boolean gravityFallLanding;
    public Direction gravityFallLandingGravity=Direction.DOWN;
    public double gravityFallLandingEtaTicks;
    public long gravityFallSequence;

    // Geometric directional fall segment used by the mace. This deliberately does not use
    // Gravity Changer's gravity-strength-scaled fallDistance: it is literal blocks travelled in
    // the current gravity direction since this directional fall began.
    public Direction maceFallDirection=Direction.DOWN;
    public Vec3 maceFallLastPosition;
    public double maceFallDistance;
    public boolean maceFallActive;

    // Safety leash for high-speed free flight. It deliberately survives ordinary support unbinds
    // and gravity turns; only an actual context/lifecycle transfer should discard the safe anchor.
    public Vec3 flightSafePosition;
    public boolean flightSafetyHolding;
    public Vec3 flightHeldVelocity=Vec3.ZERO;

    public void unbind() {
        support = null; supportId = -1; supportPosition = null; supportBox = null; lastTransport = Vec3.ZERO; groundedOnSurface = false;
        supportHistory.clear();supportSampleSequence=0;lastConsumedSupportSample=-1;pendingMove=null;
    }
    public void clearLandingCommit(){landingCommitted=false;landingContact=null;landingKind=null;landingEtaTicks=0.0D;landingDeadlineTick=0L;}
    public void clearGravityFall(){gravityFallActive=false;gravityFallLanding=false;gravityFallLandingGravity=Direction.DOWN;gravityFallLandingEtaTicks=0.0D;}
    public void clearMaceFall(){maceFallDirection=Direction.DOWN;maceFallLastPosition=null;maceFallDistance=0.0D;maceFallActive=false;}
    public void clearFlightSafetyHold(){flightSafetyHolding=false;flightHeldVelocity=Vec3.ZERO;}
    public void clearFlightSafety(){flightSafePosition=null;clearFlightSafetyHold();}
    public interface Holder { PlayerData clinging$data(); }
}
