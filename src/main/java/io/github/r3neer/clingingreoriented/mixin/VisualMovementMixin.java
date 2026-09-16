package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.VisualMovementFrame;
import io.github.r3neer.clingingreoriented.WaterMovementFrame;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Corrects the local player's movement into the presentation frame without changing vanilla's
 * input normalization or Gravity Changer's configured movement-speed scale.
 */
@Mixin(Entity.class)
public abstract class VisualMovementMixin {
    @Shadow protected static Vec3 getInputVector(Vec3 input,float speed,float yRot){throw new AssertionError();}

    @Unique private static Entity CLINGING_LAST_VISUAL_ENTITY;
    @Unique private static Vec3 CLINGING_LAST_VISUAL_FORWARD;

    @Inject(method="moveRelative",at=@At("HEAD"),cancellable=true)
    private void clinging$moveRelativeInVisualFrame(float speed,Vec3 input,CallbackInfo ci){
        Entity self=(Entity)(Object)this;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=self)return;

        Direction gravity=GravityDirectionUtil.getGravityDirection(self);
        float scaledSpeed=speed*GravityDirectionUtil.getMovementSpeedScale(self);

        // Water is a 3D camera-navigation context. Gravity Changer would otherwise rotate the
        // complete moveRelative vector into the logical gravity frame. Recreate that same
        // normalized physical delta first, then rigidly express only WASD on the visible camera
        // forward/left axes. Space/Shift remain owned by WorldVerticalWaterMixin.
        if(self instanceof Player player&&self.isInWater()&&ClingingReoriented.controlsPhysics(player)){
            CLINGING_LAST_VISUAL_ENTITY=null;
            CLINGING_LAST_VISUAL_FORWARD=null;
            Vec3 local=getInputVector(input,scaledSpeed,self.getYRot());
            Vec3 physical=gravity==Direction.DOWN?local:RotationUtil.vecPlayerToWorld(local,gravity);
            Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(self.getYRot(),0.0F),gravity).normalize();
            Vec3 remapped=WaterMovementFrame.remap(physical,physicalForward,mc.gameRenderer.mainCamera().rotation(),gravity);
            self.setDeltaMovement(self.getDeltaMovement().add(remapped));
            ci.cancel();
            return;
        }

        boolean incompatible=self.isInLava()||(self instanceof LivingEntity living&&living.isFallFlying());
        if(!VisualTransitions.owns(self)||incompatible){
            CLINGING_LAST_VISUAL_ENTITY=null;
            CLINGING_LAST_VISUAL_FORWARD=null;
            return;
        }

        if(self!=CLINGING_LAST_VISUAL_ENTITY){
            CLINGING_LAST_VISUAL_ENTITY=self;
            CLINGING_LAST_VISUAL_FORWARD=null;
        }

        Vec3 local=getInputVector(input,scaledSpeed,self.getYRot());
        Vec3 physical=gravity==Direction.DOWN?local:RotationUtil.vecPlayerToWorld(local,gravity);
        Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(self.getYRot(),0.0F),gravity).normalize();

        var remapped=VisualMovementFrame.remap(
            physical,
            physicalForward,
            mc.gameRenderer.mainCamera().rotation(),
            gravity,
            CLINGING_LAST_VISUAL_FORWARD
        );
        CLINGING_LAST_VISUAL_FORWARD=remapped.forward();
        self.setDeltaMovement(self.getDeltaMovement().add(remapped.movement()));
        ci.cancel();
    }
}
