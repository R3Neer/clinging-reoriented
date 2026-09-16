package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.MobGravityNavigation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gravity fallback for vanilla AvoidEntityGoal when its ordinary escape path is absent or partial. */
@Mixin(AvoidEntityGoal.class)
public abstract class AvoidEntityGravityMixin {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow protected LivingEntity toAvoid;
    @Shadow protected Path path;
    @Shadow @Final private double walkSpeedModifier;

    @Unique private Vec3 clinging$escapeFocus(){
        if(toAvoid==null)return null;
        Vec3 away=mob.position().subtract(toAvoid.position());
        if(!Double.isFinite(away.lengthSqr()))return null;
        if(away.lengthSqr()<1.0E-8D)away=new Vec3(1,0,0);
        return mob.position().add(away.normalize().scale(12.0D));
    }

    @Inject(method="canUse",at=@At("RETURN"),cancellable=true)
    private void clinging$gravityWake(CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValue())return;
        Vec3 focus=clinging$escapeFocus();
        if(focus!=null&&MobGravityNavigation.requestPositionAfterVanilla(mob,focus,walkSpeedModifier,false))cir.setReturnValue(true);
    }

    @Inject(method="start",at=@At("HEAD"),cancellable=true)
    private void clinging$gravityStart(CallbackInfo ci){
        if(path==null||path.canReach())return;
        Vec3 focus=clinging$escapeFocus();
        if(focus!=null&&MobGravityNavigation.requestPositionAfterVanilla(mob,focus,walkSpeedModifier,false))ci.cancel();
    }

    @Inject(method="canContinueToUse",at=@At("HEAD"),cancellable=true)
    private void clinging$gravityContinue(CallbackInfoReturnable<Boolean> cir){
        if(MobGravityNavigation.active(mob))cir.setReturnValue(toAvoid!=null&&toAvoid.isAlive());
    }

    @Inject(method="stop",at=@At("TAIL"))
    private void clinging$gravityStop(CallbackInfo ci){MobGravityNavigation.goalStopped(mob);}
}
