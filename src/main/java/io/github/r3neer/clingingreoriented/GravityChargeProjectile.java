package io.github.r3neer.clingingreoriented;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Runtime duck implemented by vanilla ShulkerBullet through the common mixin. */
public interface GravityChargeProjectile {
    void clinging$initializeCharge(Vec3 intent);
    boolean clinging$isLaunchedCharge();
    Vec3 clinging$intent();
    @Nullable BlockPos clinging$targetBlock();
    @Nullable Entity clinging$targetEntity();
    void clinging$forceAcquire();
}
