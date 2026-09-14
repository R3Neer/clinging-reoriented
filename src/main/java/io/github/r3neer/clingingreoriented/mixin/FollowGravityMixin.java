package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.GravityBreadcrumbs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(FollowOwnerGoal.class)
public abstract class FollowGravityMixin {
    @Shadow @Final private TamableAnimal tamable;
    @Shadow private LivingEntity owner;
    @Shadow @Final private double speedModifier;
    @Shadow @Final private float stopDistance;
    @Shadow @Final @Mutable private PathNavigation navigation;
    @Unique private void clinging$refreshNavigation(){navigation=tamable.getNavigation();}
    @Inject(method="canUse",at=@At("RETURN"),cancellable=true)
    private void clinging$canUse(CallbackInfoReturnable<Boolean> cir){
        clinging$refreshNavigation();
        if(GravityBreadcrumbs.hasPending(tamable)){
            boolean can=GravityBreadcrumbs.canPursue(tamable);
            if(can)owner=tamable.getOwner();
            cir.setReturnValue(can);
        }
    }
    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void clinging$canContinue(CallbackInfoReturnable<Boolean> cir){
        clinging$refreshNavigation();
        if(GravityBreadcrumbs.hasPending(tamable))cir.setReturnValue(GravityBreadcrumbs.canPursue(tamable));
    }
    @Inject(method={"start","stop"},at=@At("HEAD"))
    private void clinging$lifecycle(CallbackInfo ci){clinging$refreshNavigation();}
    @Inject(method="stop",at=@At("TAIL"))
    private void clinging$stopped(CallbackInfo ci){GravityBreadcrumbs.goalStopped(tamable);}
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void clinging$follow(CallbackInfo ci){
        clinging$refreshNavigation();
        if(GravityBreadcrumbs.follow(tamable,speedModifier,stopDistance))ci.cancel();
    }
}
