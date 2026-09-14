package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ClingingReoriented;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Water remains a world-space navigation context even while Clinging owns non-DOWN gravity.
 * Gravity Changer normally rotates vanilla's +/-Y liquid impulses into the local gravity frame;
 * intercept them before that redirect so Space always means world +Y and Shift world -Y.
 */
@Mixin(LivingEntity.class)
public abstract class WorldVerticalWaterMixin {
    @Inject(method="goDownInWater",at=@At("HEAD"),cancellable=true)
    private void clinging$worldDownInWater(CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;
        if(!(self instanceof Player player)||!player.isInWater()||!ClingingReoriented.controlsPhysics(player))return;
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D,-0.04D,0.0D));
        ci.cancel();
    }

    @Inject(method="jumpInLiquid",at=@At("HEAD"),cancellable=true)
    private void clinging$worldUpInWater(TagKey<Fluid> fluid,CallbackInfo ci){
        LivingEntity self=(LivingEntity)(Object)this;
        if(!(self instanceof Player player)||fluid!=FluidTags.WATER||!player.isInWater()||!ClingingReoriented.controlsPhysics(player))return;
        player.setDeltaMovement(player.getDeltaMovement().add(new Vec3(0.0D,0.04D,0.0D)));
        ci.cancel();
    }
}
