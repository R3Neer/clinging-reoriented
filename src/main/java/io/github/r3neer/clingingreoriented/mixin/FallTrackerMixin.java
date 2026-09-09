package io.github.r3neer.clingingreoriented.mixin;
import com.moigferdsrte.gravitychanger.util.DirectionalFallTracker;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(DirectionalFallTracker.class)
public abstract class FallTrackerMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void clinging$freeFall(Direction direction,boolean falling,int maximumTicks,CallbackInfoReturnable<Boolean> cir) {
        var owner=FallContext.CURRENT.get();
        if(ClingingReoriented.controlsPhysics(owner) || owner!=null && !(owner instanceof net.minecraft.world.entity.player.Player) && MobGravity.active(owner)) {
            ((DirectionalFallTracker)(Object)this).reset();cir.setReturnValue(false);
        }
    }
}
