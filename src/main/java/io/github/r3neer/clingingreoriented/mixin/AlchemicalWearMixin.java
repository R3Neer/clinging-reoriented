package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.compat.AlchemicalLeatherCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Publishes exactly one semantic event for the authoritative turn attempt after SUCCESS. */
@Mixin(ClingingReoriented.class)
public abstract class AlchemicalWearMixin {
    @Inject(method="attempt(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lio/github/r3neer/clingingreoriented/ClingingReoriented$Result;",at=@At("RETURN"))
    private static void clinging$alchemicalWear(ServerPlayer player,Vec3 selectionLook,Vec3 requestedHeading,CallbackInfoReturnable<ClingingReoriented.Result> cir){
        if(cir.getReturnValue()==ClingingReoriented.Result.SUCCESS)AlchemicalLeatherCompat.successfulTurn(player);
    }
}
