package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class MovementMixin {
    @Inject(method="move",at=@At("HEAD"))
    private void clinging$carry(MoverType type,Vec3 delta,CallbackInfo ci){if((Object)this instanceof Player p)MovingSurface.carry(p);}
    @Inject(method="move",at=@At("TAIL"))
    private void clinging$contact(MoverType type,Vec3 delta,CallbackInfo ci){if((Object)this instanceof Player p)MovingSurface.afterMove(p);}
    @Inject(method="teleportTo(DDD)V",at=@At("HEAD"))
    private void clinging$teleport(double x,double y,double z,CallbackInfo ci){MovingSurface.teleported((Entity)(Object)this);}
    @Inject(method="teleport",at=@At("HEAD"))
    private void clinging$dimension(TeleportTransition transition,CallbackInfoReturnable<Entity> cir){
        MovingSurface.teleported((Entity)(Object)this);
        if((Object)this instanceof Player p){var s=ClingingReoriented.data(p);s.retirementPending=false;s.nextRetirementAttempt=0;}
    }
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void clinging$pair(Entity other,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Player p && ClingingReoriented.managed(p) && other.getUUID().equals(ClingingReoriented.data(p).support)) cir.setReturnValue(false);
    }
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void clinging$push(Entity other,CallbackInfo ci){
        Entity self=(Entity)(Object)this;
        if(self instanceof Player p && ClingingReoriented.managed(p) && other.getUUID().equals(ClingingReoriented.data(p).support))ci.cancel();
        if(other instanceof Player p && ClingingReoriented.managed(p) && self.getUUID().equals(ClingingReoriented.data(p).support))ci.cancel();
    }
}
