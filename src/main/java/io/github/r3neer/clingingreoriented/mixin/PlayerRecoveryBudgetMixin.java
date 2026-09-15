package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.RecoveryBudget;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spreads the rare 4-block retirement search across ticks without changing its candidate order. */
@Mixin(ClingingReoriented.class)
public abstract class PlayerRecoveryBudgetMixin {
    @Inject(method="retire",at=@At("HEAD"),cancellable=true)
    private static void clinging$budgetRetirement(ServerPlayer player,CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(RecoveryBudget.retirePlayer(player));
    }

    @Inject(method="reconcile",at=@At("RETURN"))
    private static void clinging$scheduleRetirementContinuation(ServerPlayer player,CallbackInfo ci){
        var state=ClingingReoriented.data(player);
        if(!state.owned||!state.retirementPending||GravityDirectionUtil.getGravityDirection(player)==Direction.DOWN){
            RecoveryBudget.clearPlayer(player);return;
        }
        if(RecoveryBudget.playerNeedsNextTick(player))state.nextRetirementAttempt=player.level().getGameTime()+1L;
    }
}
