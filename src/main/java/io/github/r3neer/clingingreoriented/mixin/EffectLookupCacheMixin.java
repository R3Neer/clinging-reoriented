package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.EffectLookupCache;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces repeated registry/id construction in the hot ownership predicate with a cached holder. */
@Mixin(ClingingReoriented.class)
public abstract class EffectLookupCacheMixin {
    @Inject(method="hasEffect",at=@At("HEAD"),cancellable=true)
    private static void clinging$cachedEffectLookup(LivingEntity entity,CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(EffectLookupCache.hasClingingOrReorientation(entity));
    }
}
