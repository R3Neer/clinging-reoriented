package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.util.DirectionalFallTracker;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.FallOwnerContext;
import io.github.r3neer.clingingreoriented.MobGravity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Gravity Changer's directional-fall limiter disabled only while Clinging owns that entity. */
@Mixin(DirectionalFallTracker.class)
public abstract class DirectionalFallSuppressionMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void clinging$freeFall(Direction direction,boolean falling,int maximumTicks,CallbackInfoReturnable<Boolean> cir){
        var owner=FallOwnerContext.CURRENT.get();
        if(ClingingReoriented.controlsPhysics(owner)
            || owner!=null&&!(owner instanceof Player)&&MobGravity.active(owner)){
            ((DirectionalFallTracker)(Object)this).reset();
            cir.setReturnValue(false);
        }
    }
}
