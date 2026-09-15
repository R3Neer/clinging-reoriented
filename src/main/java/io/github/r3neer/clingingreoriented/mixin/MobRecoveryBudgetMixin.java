package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.MobGravity;
import io.github.r3neer.clingingreoriented.RecoveryBudget;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spreads rare mob gravity recovery scans across ticks instead of monopolizing one tick. */
@Mixin(MobGravity.class)
public abstract class MobRecoveryBudgetMixin {
    @Inject(method="restore",at=@At("HEAD"),cancellable=true)
    private static void clinging$budgetRecovery(LivingEntity entity,Direction direction,CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(RecoveryBudget.restoreMob(entity,direction));
    }
}
