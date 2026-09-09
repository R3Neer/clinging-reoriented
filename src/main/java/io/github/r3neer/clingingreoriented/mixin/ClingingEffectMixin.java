package io.github.r3neer.clingingreoriented.mixin;
import com.github.alexthe666.alexsmobs.effect.EffectClinging;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EffectClinging.class)
public abstract class ClingingEffectMixin {
    @Inject(method="applyEffectTick",at=@At("HEAD"),cancellable=true)
    private void clinging$tick(ServerLevel level,LivingEntity entity,int amplifier,CallbackInfoReturnable<Boolean> cir){if(entity instanceof Player || io.github.r3neer.clingingreoriented.MobGravity.supported(entity))cir.setReturnValue(true);}
    @Inject(method="isUpsideDown",at=@At("HEAD"),cancellable=true)
    private static void clinging$flip(LivingEntity entity,CallbackInfoReturnable<Boolean> cir){if(entity instanceof Player || io.github.r3neer.clingingreoriented.MobGravity.supported(entity))cir.setReturnValue(false);}
}
