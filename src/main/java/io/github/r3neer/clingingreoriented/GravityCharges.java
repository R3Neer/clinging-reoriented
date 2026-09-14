package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;

/** Registration seam for the capturable Gravity Charge item. */
public final class GravityCharges {
    public static final ResourceKey<Item> KEY=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ClingingReoriented.ID,"gravity_charge"));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,KEY,
        new GravityChargeItem(new Item.Properties().setId(KEY).stacksTo(64).useCooldown(0.5F)));
    private static boolean initialized;

    private GravityCharges() {}
    public static synchronized void initialize(){if(initialized)return;initialized=true;DispenserBlock.registerProjectileBehavior(ITEM);}
}
