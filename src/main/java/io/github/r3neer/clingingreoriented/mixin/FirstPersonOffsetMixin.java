package io.github.r3neer.clingingreoriented.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.client.FirstPersonRenderBridge;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Transform First Person's body offset only for frames/transitions owned by Clinging. */
@Pseudo
@Mixin(targets = "dev.tr7zw.firstperson.LogicHandler", remap = false)
public abstract class FirstPersonOffsetMixin {
    @Shadow private Vec3 offset;

    @ModifyExpressionValue(method = "updatePositionOffset", at = @At(value = "NEW",
        target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 clinging$orientBodyOffset(Vec3 offset, Entity entity, float partialTick) {
        if(!(entity instanceof Player player))return offset;
        var state=ClingingReoriented.data(player);
        if(!state.visualFrameOwned&&!VisualTransitions.owns(entity))return offset;
        var rotation = ((GravityAnimationEntity) entity).gravitychanger$getVisualGravityRotation(
            GravityDirectionUtil.getGravityDirection(entity));
        return RotationUtil.isIdentityRotation(rotation) ? offset : RotationUtil.vecPlayerToWorld(offset, rotation);
    }

    @Inject(method="updatePositionOffset",at=@At("RETURN"))
    private void clinging$captureFinalOffset(Entity entity,float partialTick,CallbackInfo ci){
        FirstPersonRenderBridge.capture(entity,offset);
    }
}
