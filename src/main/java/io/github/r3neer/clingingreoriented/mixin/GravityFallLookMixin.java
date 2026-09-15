package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.client.GravityFallLookState;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gravity Fall replaces Euler pole handling with a continuous screen-relative camera orientation. */
@Mixin(Entity.class)
public abstract class GravityFallLookMixin {
    @Inject(method="turn(DD)V",at=@At("HEAD"),cancellable=true)
    private void clinging$fullSphereTurn(double xo,double yo,CallbackInfo ci){
        if(!clinging$ownsFullSphereLook())return;
        ci.cancel();
        Entity self=(Entity)(Object)this;
        GravityFallLookState.turn((Player)self,xo,yo);
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
