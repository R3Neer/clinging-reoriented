package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.client.GravityFallLookMath;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla look input is deliberately bounded to a neck-like +/-90 degree pitch. Sustained
 * Gravity Fall owns a full-sphere camera instead, so update the underlying Euler fields directly
 * and bypass the pitch-clamping setter only for that ownership window.
 */
@Mixin(Entity.class)
public abstract class GravityFallLookMixin {
    @Shadow private float xRot;
    @Shadow private float yRot;
    @Shadow public float xRotO;
    @Shadow public float yRotO;

    @Inject(method="turn(DD)V",at=@At("HEAD"),cancellable=true)
    private void clinging$fullSphereTurn(double xo,double yo,CallbackInfo ci){
        if(!clinging$ownsFullSphereLook())return;
        ci.cancel();

        float xDelta=(float)yo*0.15F;
        float yDelta=(float)xo*0.15F;
        this.xRot+=xDelta;
        this.yRot+=yDelta;
        this.xRotO+=xDelta;
        this.yRotO+=yDelta;

        float shift=GravityFallLookMath.normalizationShift(this.xRot);
        if(shift!=0.0F){
            this.xRot-=shift;
            this.xRotO-=shift;
        }

        Entity self=(Entity)(Object)this;
        if(self.getVehicle()!=null)self.getVehicle().onPassengerTurned(self);
    }

    @Unique
    private boolean clinging$ownsFullSphereLook(){
        Entity self=(Entity)(Object)this;
        if(!(self instanceof Player player))return false;
        Minecraft mc=Minecraft.getInstance();
        return mc.player==player&&GravityFallVisuals.active(player);
    }
}
