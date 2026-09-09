package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.client.ClingingClient;
import net.minecraft.client.player.LocalPlayer;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class JumpInputMixin {
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z"))
    private boolean clinging$airJump(LocalPlayer player,Operation<Boolean> original) {
        boolean gliding=original.call(player);
        if(!gliding)ClingingClient.press();
        return gliding;
    }
}
