package io.github.r3neer.clingingreoriented.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.github.alexthe666.alexsmobs.client.event.ClientEvents")
public abstract class LegacyClientMixin {
    @Inject(method="tickClinging",at=@At("HEAD"),cancellable=true)
    private static void clinging$disableLegacy(CallbackInfo ci){ci.cancel();}
}
