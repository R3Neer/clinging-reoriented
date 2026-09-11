package io.github.r3neer.clingingreoriented;

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
    public void unbind() {
        support = null; supportId = -1; supportPosition = null; supportBox = null; lastTransport = Vec3.ZERO; groundedOnSurface = false;
        supportHistory.clear();supportSampleSequence=0;lastConsumedSupportSample=-1;pendingMove=null;
    }
    public interface Holder { PlayerData clinging$data(); }
}
