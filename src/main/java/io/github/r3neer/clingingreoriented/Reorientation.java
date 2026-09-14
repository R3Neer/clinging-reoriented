package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.registry.FabricPotionBrewingBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

/** A separate effect lets armor infusion and ordinary Clinging retain distinct rules. */
public final class Reorientation {
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(ClingingReoriented.ID,path); }
    public static final Holder<MobEffect> EFFECT=Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
        ResourceKey.create(Registries.MOB_EFFECT,id("reorientation")),new ReorientationEffect());
    public static final Holder<Potion> POTION=potion("reorientation",3600);
    public static final Holder<Potion> LONG_POTION=potion("long_reorientation",9600);
    private static Holder<Potion> potion(String path,int ticks) {
        return Registry.registerForHolder(BuiltInRegistries.POTION,ResourceKey.create(Registries.POTION,id(path)),
            new Potion("clinging_reoriented.reorientation",new MobEffectInstance(EFFECT,ticks)));
    }
    public static void initialize() {
        GravityCharges.initialize();
        FabricPotionBrewingBuilder.BUILD.register(builder -> {
            builder.addMix(BuiltInRegistries.POTION.get(Identifier.parse("alexsmobs:clinging")).orElseThrow(),GravityCharges.ITEM,POTION);
            builder.addMix(BuiltInRegistries.POTION.get(Identifier.parse("alexsmobs:long_clinging")).orElseThrow(),GravityCharges.ITEM,LONG_POTION);
            builder.addMix(POTION,Items.REDSTONE,LONG_POTION);
        });
    }
    private static final class ReorientationEffect extends MobEffect {
        private ReorientationEffect() { super(MobEffectCategory.BENEFICIAL,0xB68DDA); }
    }
}
