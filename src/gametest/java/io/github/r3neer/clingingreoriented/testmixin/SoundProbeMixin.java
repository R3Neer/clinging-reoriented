package io.github.r3neer.clingingreoriented.testmixin;
import io.github.r3neer.clingingreoriented.ClingingClientGameTest;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class SoundProbeMixin {
    @Inject(method="playSound",at=@At("HEAD"))
    private void clinging$record(SoundEvent event,float volume,float pitch,CallbackInfo ci){ClingingClientGameTest.SOUNDS.add(event);}
}
