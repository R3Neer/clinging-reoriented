package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/** Stable registry handle used by hot ownership checks after registries are bootstrapped. */
public final class EffectLookupCache {
    private static final Identifier ALEX_CLINGING=Identifier.fromNamespaceAndPath("alexsmobs","clinging");
    private static volatile Holder<MobEffect> alexClinging;

    private EffectLookupCache() {}

    public static boolean hasClingingOrReorientation(LivingEntity entity){
        if(entity==null)return false;
        if(entity.hasEffect(Reorientation.EFFECT))return true;
        Holder<MobEffect> effect=alexClinging;
        if(effect==null){
            effect=BuiltInRegistries.MOB_EFFECT.get(ALEX_CLINGING).orElse(null);
            if(effect!=null)alexClinging=effect;
        }
        return effect!=null&&entity.hasEffect(effect);
    }
}
