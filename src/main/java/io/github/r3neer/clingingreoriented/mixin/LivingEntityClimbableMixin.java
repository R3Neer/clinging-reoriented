package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.ClimbablePolicy;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mirrors vanilla climbable Y mechanics when the owned player is upside down. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityClimbableMixin {
    @Inject(method="handleOnClimbable",at=@At("HEAD"),cancellable=true)
    private void clinging$mirrorUpsideDownClimbDamping(Vec3 delta,CallbackInfoReturnable<Vec3> cir){
        LivingEntity self=(LivingEntity)(Object)this;
        if(!(self instanceof Player player)||!ClingingReoriented.controlsPhysics(player))return;
        if(GravityDirectionUtil.getGravityDirection(player)!=Direction.UP||!player.onClimbable())return;
        player.resetFallDistance();
        cir.setReturnValue(ClimbablePolicy.clampForUpGravity(
            delta,
            player.isSuppressingSlidingDownLadder(),
            player.getInBlockState().is(Blocks.SCAFFOLDING)
        ));
    }

    @ModifyConstant(method="handleRelativeFrictionAndCalculateMovement",constant=@Constant(doubleValue=0.2D))
    private double clinging$mirrorUpsideDownAirClimbImpulse(double vanilla){
        LivingEntity self=(LivingEntity)(Object)this;
        if(!(self instanceof Player player)||!ClingingReoriented.controlsPhysics(player))return vanilla;
        return ClimbablePolicy.climbImpulse(GravityDirectionUtil.getGravityDirection(player),vanilla);
    }

    @ModifyConstant(method="travelInWater",constant=@Constant(doubleValue=0.2D))
    private double clinging$mirrorUpsideDownWaterClimbImpulse(double vanilla){
        LivingEntity self=(LivingEntity)(Object)this;
        if(!(self instanceof Player player)||!ClingingReoriented.controlsPhysics(player))return vanilla;
        return ClimbablePolicy.climbImpulse(GravityDirectionUtil.getGravityDirection(player),vanilla);
    }
}
