package io.github.r3neer.clingingreoriented.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moigferdsrte.gravitychanger.client.GravityRenderContext;
import com.moigferdsrte.gravitychanger.client.GravityRenderState;
import io.github.r3neer.clingingreoriented.BodyRenderMath;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import java.util.ArrayDeque;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
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
    /**
     * Render submit can nest, so retain a stack, but only avatars ever need it. Keep one deque per
     * render thread instead of removing/recreating its ThreadLocal entry for every rendered entity.
     */
    @Unique private static final ThreadLocal<ArrayDeque<Boolean>> CLINGING_GRAVITY_FALL_PUSHES=ThreadLocal.withInitial(ArrayDeque::new);
    @Unique private static final boolean CLINGING_FIRST_PERSON_PRESENT=FabricLoader.getInstance().isModLoaded("firstperson");
    @Unique private static final String CLINGING_RENDER_TARGET="Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";

    @Inject(method="submit",at=@At(value="INVOKE",target=CLINGING_RENDER_TARGET,shift=At.Shift.BEFORE))
    private void clinging$pushGravityFallBody(EntityRenderState renderState,CameraRenderState camera,double x,double y,double z,
                                               PoseStack poseStack,SubmitNodeCollector submitNodeCollector,CallbackInfo ci){
        if(!(renderState instanceof AvatarRenderState avatar))return;
        boolean pushed=false;
        if(!GravityRenderContext.isRenderingGuiEntity()){
            Minecraft mc=Minecraft.getInstance();
            if(mc.level!=null && mc.level.getEntity(avatar.id) instanceof Player player){
                float partial=renderState.ageInTicks-(float)Math.floor(renderState.ageInTicks);
                Quaternionf extra=GravityFallVisuals.extraRoot(player,partial);
                if(extra!=null){
                    poseStack.pushPose();
                    if(clinging$firstPersonBodyPass(mc,player)){
                        @SuppressWarnings({"rawtypes","unchecked"})
                        EntityRenderer renderer=((EntityRenderDispatcher)(Object)this).getRenderer(renderState);
                        Vec3 renderOffset=renderer.getRenderOffset(renderState);
                        Quaternionf visual=((GravityRenderState)renderState).gravitychanger$getGravityRotation();
                        Vec3 cameraPivot=BodyRenderMath.localCameraPivot(
                            x+renderOffset.x,y+renderOffset.y,z+renderOffset.z,visual);
                        Vec3 pivot=BodyRenderMath.firstPersonPivot(cameraPivot,avatar.boundingBoxHeight);
                        poseStack.translate(pivot.x,pivot.y,pivot.z);
                        poseStack.mulPose(extra);
                        poseStack.translate(-pivot.x,-pivot.y,-pivot.z);
                    }else{
                        float pivot=BodyRenderMath.bodyCenterPivot(avatar.boundingBoxHeight);
                        poseStack.translate(0.0F,pivot,0.0F);
                        poseStack.mulPose(extra);
                        poseStack.translate(0.0F,-pivot,0.0F);
                    }
                    pushed=true;
                }
            }
        }
        CLINGING_GRAVITY_FALL_PUSHES.get().push(pushed);
    }

    @Unique
    private static boolean clinging$firstPersonBodyPass(Minecraft mc,Player player){
        return CLINGING_FIRST_PERSON_PRESENT
            && mc.options.getCameraType()==CameraType.FIRST_PERSON
            && mc.getCameraEntity()==player;
    }

    @Inject(method="submit",at=@At(value="INVOKE",target=CLINGING_RENDER_TARGET,shift=At.Shift.AFTER))
    private void clinging$popGravityFallBody(EntityRenderState renderState,CameraRenderState camera,double x,double y,double z,
                                              PoseStack poseStack,SubmitNodeCollector submitNodeCollector,CallbackInfo ci){
        if(!(renderState instanceof AvatarRenderState))return;
        ArrayDeque<Boolean> pushes=CLINGING_GRAVITY_FALL_PUSHES.get();
        if(!pushes.isEmpty()&&pushes.pop())poseStack.popPose();
    }
}
