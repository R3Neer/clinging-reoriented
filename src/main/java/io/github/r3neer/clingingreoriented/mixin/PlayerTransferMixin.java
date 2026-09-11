package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class PlayerTransferMixin {
    @Inject(method="teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",at=@At("HEAD"))
    private void clinging$transfer(TeleportTransition transition,CallbackInfoReturnable<ServerPlayer> cir){
        var p=(ServerPlayer)(Object)this;
        if(!transition.newLevel().dimension().equals(p.level().dimension())){
            MovingSurface.teleported(p);
            var state=ClingingReoriented.data(p);
            state.retirementPending=false;
            state.nextRetirementAttempt=0;
        }
    }
}
