package io.github.r3neer.clingingreoriented.mixin;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.ClimbablePolicy;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ladders/vines are world-vertical tools, so lateral player gravity ignores them entirely. */
@Mixin(Player.class)
public abstract class PlayerClimbableMixin {
    @Inject(method="onClimbable",at=@At("HEAD"),cancellable=true)
    private void clinging$ignoreWorldVerticalClimbablesForLateralGravity(CallbackInfoReturnable<Boolean> cir){
        Player self=(Player)(Object)this;
        if(!ClingingReoriented.controlsPhysics(self))return;
        if(!ClimbablePolicy.participates(GravityDirectionUtil.getGravityDirection(self)))cir.setReturnValue(false);
    }
}
