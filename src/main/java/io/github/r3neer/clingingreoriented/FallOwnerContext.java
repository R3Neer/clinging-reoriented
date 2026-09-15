package io.github.r3neer.clingingreoriented;

import net.minecraft.world.entity.LivingEntity;

/** Synchronous owner handoff for Gravity Changer's per-entity fall tracker. */
public final class FallOwnerContext {
    private FallOwnerContext() {}
    public static final ThreadLocal<LivingEntity> CURRENT=new ThreadLocal<>();
}
