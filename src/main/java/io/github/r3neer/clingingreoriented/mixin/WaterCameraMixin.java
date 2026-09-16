package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.client.WaterCameraVisuals;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Final camera roll fence for water. Lower priority makes this run after the normal gravity camera mixins. */
@Mixin(value=Camera.class,priority=900)
public abstract class WaterCameraMixin {
    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;

    @Inject(method="setRotation(FF)V",at=@At("TAIL"))
    private void clinging$applyWaterUpFrame(float yRot,float xRot,CallbackInfo ci){
        if(!(entity instanceof Player player))return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=player||mc.getCameraEntity()!=player)return;
        if(!WaterCameraVisuals.apply(player,rotation,System.nanoTime()))return;
        forwards.set(0.0F,0.0F,-1.0F).rotate(rotation);
        up.set(0.0F,1.0F,0.0F).rotate(rotation);
        left.set(-1.0F,0.0F,0.0F).rotate(rotation);
        matrixPropertiesDirty|=3;
    }
}
