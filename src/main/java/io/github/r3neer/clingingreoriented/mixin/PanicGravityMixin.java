package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.MobGravityNavigation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps a vanilla panic goal alive while its moveTo intent is being executed through gravity. */
@Mixin(PanicGoal.class)
public abstract class PanicGravityMixin {
    @Shadow @Final protected PathfinderMob mob;

    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void clinging$gravityContinue(CallbackInfoReturnable<Boolean> cir){
        if(MobGravityNavigation.active(mob))cir.setReturnValue(mob.isAlive());
    }

    @Inject(method="stop",at=@At("TAIL"))
    private void clinging$gravityStop(CallbackInfo ci){MobGravityNavigation.goalStopped(mob);}
}
