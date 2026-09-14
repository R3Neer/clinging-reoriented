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
        // Teleport changes the spatial/world context without closing the client connection. Release
        // transient landing/body presentation before the move so no HOLD/LAND root can leak through
        // a portal/command teleport and become the starting frame of an unrelated location.
        GravityFallState.reset(p);
        LandingState.transferClear(p);
        if(!transition.newLevel().dimension().equals(p.level().dimension())){
            MovingSurface.teleported(p);
            var state=ClingingReoriented.data(p);
            state.retirementPending=false;
            state.nextRetirementAttempt=0;
        }
    }
}
