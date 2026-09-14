package io.github.r3neer.clingingreoriented.testmixin;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Shulker.class)
public interface ShulkerTestAccessor {
    @Invoker("setRawPeekAmount") void clinging$setRawPeekAmount(int amount);
}
