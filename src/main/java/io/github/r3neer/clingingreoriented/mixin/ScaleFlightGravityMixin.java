package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.scalebrews.mount.*;
import com.moigferdsrte.gravitychanger.util.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TinyMounts.class)
public abstract class ScaleFlightGravityMixin {
    @Inject(method="flightVelocity",at=@At("RETURN"),cancellable=true)
    private static void clinging$flight(Player player,TinyMountDefinition definition,CallbackInfoReturnable<Vec3> cir){
        cir.setReturnValue(RotationUtil.vecPlayerToWorld(cir.getReturnValue(),GravityDirectionUtil.getGravityDirection(player.getRootVehicle())));
    }
}
