package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.MobGravityNavigation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets vanilla melee goals stay alive while the generic gravity locomotion layer crosses surfaces. */
@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGravityMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow @Final private double speedModifier;
    @Shadow @Final private boolean followingTargetEvenIfNotSeen;
    @Unique private boolean clinging$pathAttempted;

    @Inject(method="canUse",at=@At("HEAD"))
    private void clinging$resetPathAttempt(CallbackInfoReturnable<Boolean> cir){clinging$pathAttempted=false;}

    @Inject(method="canUse",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/ai/navigation/PathNavigation;createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;"))
    private void clinging$markPathAttempt(CallbackInfoReturnable<Boolean> cir){clinging$pathAttempted=true;}

    @Inject(method="canUse",at=@At("RETURN"),cancellable=true)
    private void clinging$gravityWake(CallbackInfoReturnable<Boolean> cir){
        // Never use the gravity layer to bypass MeleeAttackGoal's own 20-tick canUse cadence.
        // Wake here only when vanilla actually attempted path creation and still rejected the goal.
        if(cir.getReturnValue()||!clinging$pathAttempted)return;
        LivingEntity target=mob.getTarget();
        if(target==null||!target.isAlive())return;
        if(MobGravityNavigation.requestEntityAfterVanilla(mob,target,speedModifier,false))cir.setReturnValue(true);
    }

    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void clinging$gravityContinue(CallbackInfoReturnable<Boolean> cir){
        if(!MobGravityNavigation.active(mob))return;
        LivingEntity target=mob.getTarget();
        boolean valid=target!=null&&target.isAlive();
        if(valid&&followingTargetEvenIfNotSeen){
            valid=mob.isWithinHome(target.blockPosition())
                &&!(target instanceof Player player&&(player.isSpectator()||player.isCreative()));
        }
        cir.setReturnValue(valid);
    }

    @Inject(method="stop",at=@At("TAIL"))
    private void clinging$gravityStop(CallbackInfo ci){MobGravityNavigation.goalStopped(mob);}
}
