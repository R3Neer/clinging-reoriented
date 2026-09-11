package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import io.github.r3neer.clingingreoriented.client.CameraConfig;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Preserve upstream timing for every animation not explicitly initiated by Clinging. */
@Mixin(GravityRotationAnimation.class)
public abstract class CameraDurationMixin {
    @ModifyConstant(method="interpolate", constant=@Constant(doubleValue=1_250_000_000.0), require=1)
    private double clinging$cameraDuration(double original) {
        return VisualTransitions.owns((GravityRotationAnimation)(Object)this)?CameraConfig.durationNanos():original;
    }
}
