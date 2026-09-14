package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.client.GravityFallLookMath;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla clamps Entity.turn pitch to +/-90 degrees. During local Gravity Fall ownership,
 * keep the exact vanilla mouse/vehicle turn pipeline but remove only that vertical neck stop.
 */
@Mixin(Entity.class)
public abstract class GravityFallLookMixin {
    @Shadow public float xRotO;
    @Shadow public abstract float getXRot();
    @Shadow public abstract void setXRot(float value);

    @Redirect(
        method="turn(DD)V",
        at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;clamp(FFF)F")
    )
    private float clinging$allowFullSpherePitch(float value,float min,float max){
        return clinging$ownsFullSphereLook()?value:Mth.clamp(value,min,max);
    }

    @Inject(method="turn(DD)V",at=@At("TAIL"))
    private void clinging$normalizeFullSpherePitch(double yaw,double pitch,CallbackInfo ci){
        if(!clinging$ownsFullSphereLook())return;
        float current=getXRot();
        float shift=GravityFallLookMath.normalizationShift(current);
        if(shift==0.0F)return;
        setXRot(current-shift);
        xRotO-=shift;
    }

    @Unique
    private boolean clinging$ownsFullSphereLook(){
        Entity self=(Entity)(Object)this;
        if(!(self instanceof Player player))return false;
        Minecraft mc=Minecraft.getInstance();
        // Local-player ownership is the meaningful fence. CameraEntity can transiently lag the
        // input call (notably in client GameTest and camera-mode transitions), while the look state
        // still belongs to this player. Third person should get the same free-look semantics too.
        return mc.player==player&&GravityFallVisuals.active(player);
    }
}
