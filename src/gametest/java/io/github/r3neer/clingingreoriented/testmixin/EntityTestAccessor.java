package io.github.r3neer.clingingreoriented.testmixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only access to Entity's RNG so Shulker routing fixtures are reproducible. */
@Mixin(Entity.class)
public interface EntityTestAccessor {
    @Accessor("random") RandomSource clinging$random();
}
