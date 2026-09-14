package io.github.r3neer.clingingreoriented.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.gravitychanger.client.GravityRenderContext;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gravity Changer has already applied its visual gravity quaternion at this point.
 * Push an extra avatar-only root so internal model animation (including FA/EMF) runs
 * inside Gravity Fall, then pop it before dispatcher fire/shadow/nameplate work continues.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class GravityFallRenderMixin {
    @Unique private static final ThreadLocal<ArrayDeque<Boolean>> CLINGING_GRAVITY_FALL_PUSHES=ThreadLocal.withInitial(ArrayDeque::new);
    @Unique private static final String CLINGING_RENDER_TARGET="Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method="submit",at=@At(value="INVOKE",target=CLINGING_RENDER_TARGET,shift=At.Shift.BEFORE))
    private void clinging$pushGravityFallBody(EntityRenderState renderState,CameraRenderState camera,double x,double y,double z,
                                               PoseStack poseStack,SubmitNodeCollector submitNodeCollector,CallbackInfo ci){
        boolean pushed=false;
        if(renderState instanceof AvatarRenderState avatar && !GravityRenderContext.isRenderingGuiEntity()){
            Minecraft mc=Minecraft.getInstance();
            if(mc.level!=null && mc.level.getEntity(avatar.id) instanceof Player player){
                float partial=renderState.ageInTicks-(float)Math.floor(renderState.ageInTicks);
                Quaternionf extra=GravityFallVisuals.extraRoot(player,partial);
                if(extra!=null){
                    poseStack.pushPose();
                    // EntityRenderDispatcher's origin is the entity's feet and LivingEntityRenderer
                    // builds the humanoid upward from that point. A macro body rotation around that
                    // origin makes the whole avatar orbit its feet and disappear toward the HUD.
                    // Keep the currently displayed body center fixed instead; the pivot is expressed
                    // in the already-applied visual-gravity frame, so this is also continuous while
                    // Q_visual is held independently of physical gravity.
                    float pivot=Math.max(0.0F,avatar.boundingBoxHeight*0.5F);
                    poseStack.translate(0.0F,pivot,0.0F);
                    poseStack.mulPose(extra);
                    poseStack.translate(0.0F,-pivot,0.0F);
                    pushed=true;
                }
            }
        }
        CLINGING_GRAVITY_FALL_PUSHES.get().push(pushed);
    }

    @Inject(method="submit",at=@At(value="INVOKE",target=CLINGING_RENDER_TARGET,shift=At.Shift.AFTER))
    private void clinging$popGravityFallBody(EntityRenderState renderState,CameraRenderState camera,double x,double y,double z,
                                              PoseStack poseStack,SubmitNodeCollector submitNodeCollector,CallbackInfo ci){
        ArrayDeque<Boolean> pushes=CLINGING_GRAVITY_FALL_PUSHES.get();
        if(!pushes.isEmpty()&&pushes.pop())poseStack.popPose();
        if(pushes.isEmpty())CLINGING_GRAVITY_FALL_PUSHES.remove();
    }
}
