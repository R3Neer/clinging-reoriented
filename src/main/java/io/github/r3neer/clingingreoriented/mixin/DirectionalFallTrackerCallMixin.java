package io.github.r3neer.clingingreoriented.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.moigferdsrte.gravitychanger.util.DirectionalFallTracker;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.MobGravity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Intercepts only Gravity Changer's directional-fall timer call. Applying after Gravity Changer's
 * default-priority LivingEntity mixin avoids wrapping every LivingEntity.tick merely to recover the
 * owner of one tracker invocation.
 */
@Mixin(value=LivingEntity.class,priority=900)
public abstract class DirectionalFallTrackerCallMixin {
    @WrapOperation(
        method="tick",
        at=@At(
            value="INVOKE",
            target="Lcom/moigferdsrte/gravitychanger/util/DirectionalFallTracker;tick(Lnet/minecraft/core/Direction;ZI)Z"
        ),
        require=1
    )
    private boolean clinging$directionalFallTimer(DirectionalFallTracker tracker,Direction direction,boolean falling,int maximumTicks,
                                                   Operation<Boolean> original){
        LivingEntity owner=(LivingEntity)(Object)this;
        if(ClingingReoriented.controlsPhysics(owner)
            || !(owner instanceof Player)&&MobGravity.active(owner)){
            tracker.reset();
            return false;
        }
        return original.call(tracker,direction,falling,maximumTicks);
    }
}
