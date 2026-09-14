package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Registration seam for the capturable Shulker Charge item. */
public final class ShulkerCharges {
    public static final ResourceKey<Item> KEY=ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(ClingingReoriented.ID,"shulker_charge"));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,KEY,
        new ShulkerChargeItem(new Item.Properties().setId(KEY).stacksTo(64).useCooldown(0.5F)));

    private ShulkerCharges() {}
    public static void initialize() { /* force class initialization before brewing registration */ }
}
