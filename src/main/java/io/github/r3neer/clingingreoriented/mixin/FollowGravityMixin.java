package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.PetGravityFollow;
import io.github.r3neer.clingingreoriented.PetGravityTeleport;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Delegates only gravity-specific follow segments to the S06 online executor.
 * Ordinary same-surface following remains vanilla; powered far-away pets use the gravity-safe fallback.
 */
@Mixin(FollowOwnerGoal.class)
public abstract class FollowGravityMixin {
    @Shadow @Final private TamableAnimal tamable;
    @Shadow private LivingEntity owner;
    @Shadow @Final private double speedModifier;
    @Shadow @Final private float stopDistance;
    @Shadow @Final @Mutable private PathNavigation navigation;

    @Unique private final PetGravityFollow.State clinging$gravityFollow=new PetGravityFollow.State();

    @Unique private void clinging$refreshNavigation(){navigation=tamable.getNavigation();}
    @Unique private LivingEntity clinging$owner(){return owner!=null?owner:tamable.getOwner();}

    @Inject(method="canUse",at=@At("RETURN"),cancellable=true)
    private void clinging$canUse(CallbackInfoReturnable<Boolean> cir){
        clinging$refreshNavigation();
        if(cir.getReturnValue())return;
        LivingEntity candidate=tamable.getOwner();
        if(PetGravityFollow.shouldWake(tamable,candidate,clinging$gravityFollow,stopDistance)){
            owner=candidate;
            cir.setReturnValue(true);
        }
    }

    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void clinging$canContinue(CallbackInfoReturnable<Boolean> cir){
        clinging$refreshNavigation();
        if(PetGravityFollow.active(clinging$gravityFollow))
            cir.setReturnValue(PetGravityFollow.canContinue(tamable,clinging$owner(),clinging$gravityFollow));
    }

    @Inject(method={"start","stop"},at=@At("HEAD"))
    private void clinging$lifecycle(CallbackInfo ci){clinging$refreshNavigation();}

    @Inject(method="stop",at=@At("TAIL"))
    private void clinging$stopped(CallbackInfo ci){PetGravityFollow.goalStopped(tamable,clinging$gravityFollow);}

    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void clinging$follow(CallbackInfo ci){
        clinging$refreshNavigation();
        LivingEntity candidate=clinging$owner();
        if(PetGravityFollow.tick(tamable,candidate,clinging$gravityFollow,speedModifier,stopDistance)){
            ci.cancel();return;
        }
        if(PetGravityTeleport.handleFarFollow(tamable,candidate,speedModifier))ci.cancel();
    }
}
