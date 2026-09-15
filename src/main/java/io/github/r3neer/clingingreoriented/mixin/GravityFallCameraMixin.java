package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.client.GravityFallLookState;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces only Gravity Fall's local-camera basis after vanilla and Gravity Changer have run.
 * Camera.alignWithEntity then keeps vanilla third-person boom distance and wall clipping, but they
 * consume this continuous basis instead of a pole-singular Euler reconstruction.
 */
@Mixin(Camera.class)
public abstract class GravityFallCameraMixin {
    @Shadow private Entity entity;
    @Shadow @Final private Quaternionf rotation;
    @Shadow @Final private Vector3f forwards;
    @Shadow @Final private Vector3f up;
    @Shadow @Final private Vector3f left;
    @Shadow private int matrixPropertiesDirty;
    @Unique private final Quaternionf clinging$baseScratch=new Quaternionf();

    @Inject(method="setRotation(FF)V",at=@At("TAIL"))
    private void clinging$applyFullSphereCamera(float yRot,float xRot,CallbackInfo ci){
        if(!(entity instanceof Player player))return;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=player||mc.getCameraEntity()!=player)return;
        if(!GravityFallLookState.copyCameraBase(player,clinging$baseScratch))return;

        // Front third person looks back along the same camera frame while preserving its screen-up.
        if(mc.options.getCameraType().isMirrored())clinging$baseScratch.rotateY((float)Math.PI);

        // Camera already owns reusable quaternion/vector storage: compose directly into it instead
        // of manufacturing a base copy plus a second world quaternion every camera update.
        rotation.set(VisualTransitions.current(player)).mul(clinging$baseScratch).normalize();
        forwards.set(0.0F,0.0F,-1.0F).rotate(rotation);
        up.set(0.0F,1.0F,0.0F).rotate(rotation);
        left.set(-1.0F,0.0F,0.0F).rotate(rotation);
        matrixPropertiesDirty|=3;
    }
}
