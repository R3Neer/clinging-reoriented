package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.VisualMovementFrame;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * When Clinging owns a visual frame that differs from physical gravity, keep the local
 * player's existing movement control in the plane the player actually sees. This is a
 * rigid frame correction only: it reuses vanilla input normalization and Gravity
 * Changer's movement-speed scale and never changes the acceleration magnitude.
 */
@Mixin(Entity.class)
public abstract class VisualMovementMixin {
    @Shadow protected static Vec3 getInputVector(Vec3 input,float speed,float yRot){throw new AssertionError();}

    @Unique private static UUID CLINGING_LAST_VISUAL_PLAYER;
    @Unique private static Vec3 CLINGING_LAST_VISUAL_FORWARD;

    @Inject(method="moveRelative",at=@At("HEAD"),cancellable=true)
    private void clinging$moveRelativeInVisualFrame(float speed,Vec3 input,CallbackInfo ci){
        Entity self=(Entity)(Object)this;
        Minecraft mc=Minecraft.getInstance();
        if(mc.player!=self){return;}

        boolean incompatible=self.isInWater()||self.isInLava()||(self instanceof LivingEntity living&&living.isFallFlying());
        if(!VisualTransitions.owns(self)||incompatible){
            CLINGING_LAST_VISUAL_PLAYER=null;
            CLINGING_LAST_VISUAL_FORWARD=null;
            return;
        }

        UUID uuid=self.getUUID();
        if(!uuid.equals(CLINGING_LAST_VISUAL_PLAYER)){
            CLINGING_LAST_VISUAL_PLAYER=uuid;
            CLINGING_LAST_VISUAL_FORWARD=null;
        }

        Direction gravity=GravityDirectionUtil.getGravityDirection(self);
        float scaledSpeed=speed*GravityDirectionUtil.getMovementSpeedScale(self);
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
