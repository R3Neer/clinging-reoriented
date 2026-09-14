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
 * Vanilla Entity.turn clamps both current and previous pitch to +/-90 degrees. During local
 * Gravity Fall, reproduce the same turn pipeline without those two clamps, then periodically
 * rebase whole 360-degree turns so interpolation remains numerically well behaved.
 */
@Mixin(Entity.class)
public abstract class GravityFallLookMixin {
    @Shadow public float xRotO;
    @Shadow public float yRotO;
    @Shadow public abstract float getXRot();
    @Shadow public abstract float getYRot();
    @Shadow public abstract void setXRot(float value);
    @Shadow public abstract void setYRot(float value);

    @Inject(method="turn(DD)V",at=@At("HEAD"),cancellable=true)
    private void clinging$fullSphereTurn(double xo,double yo,CallbackInfo ci){
        if(!clinging$ownsFullSphereLook())return;
        ci.cancel();

        float xDelta=(float)yo*0.15F;
        float yDelta=(float)xo*0.15F;
        setXRot(getXRot()+xDelta);
        setYRot(getYRot()+yDelta);
        xRotO+=xDelta;
        yRotO+=yDelta;

        float shift=GravityFallLookMath.normalizationShift(getXRot());
        if(shift!=0.0F){
            setXRot(getXRot()-shift);
            xRotO-=shift;
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
