package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces only Clinging-owned visual interpolation; every other Gravity Changer animation stays upstream. */
@Mixin(GravityRotationAnimation.class)
public abstract class GravitySnapMixin {
    @Inject(
        method="getRotation(Lnet/minecraft/core/Direction;J)Lorg/joml/Quaternionf;",
        at=@At("HEAD"),
        cancellable=true,
        require=1
    )
    private void clinging$snap(Direction direction,long nowNanos,CallbackInfoReturnable<Quaternionf> cir){
        Quaternionf owned=VisualTransitions.override((GravityRotationAnimation)(Object)this,direction,nowNanos);
        if(owned!=null)cir.setReturnValue(owned);
    }
}
