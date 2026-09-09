package io.github.r3neer.clingingreoriented.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Transform only First Person's own body offset, before external world-space handlers. */
@Pseudo
@Mixin(targets = "dev.tr7zw.firstperson.LogicHandler", remap = false)
public abstract class FirstPersonOffsetMixin {
    @ModifyExpressionValue(method = "updatePositionOffset", at = @At(value = "NEW",
        target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 clinging$orientBodyOffset(Vec3 offset, Entity entity, float partialTick) {
        var rotation = ((GravityAnimationEntity) entity).gravitychanger$getVisualGravityRotation(
            GravityDirectionUtil.getGravityDirection(entity));
        // Also follow the upstream animation returning to DOWN; settled DOWN is untouched.
        // Scale Visual Compat modifies constructor arguments, so its scaling has already run.
        return RotationUtil.isIdentityRotation(rotation) ? offset : RotationUtil.vecPlayerToWorld(offset, rotation);
    }
}
