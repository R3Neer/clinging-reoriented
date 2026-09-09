package io.github.r3neer.clingingreoriented.mixin;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BeaconBlockEntity.class)
public interface BeaconAccess {
    @Accessor("BEACON_EFFECTS") @Mutable static void clinging$effects(List<List<Holder<MobEffect>>> effects){throw new AssertionError();}
    @Accessor("VALID_EFFECTS") static Set<Holder<MobEffect>> clinging$valid(){throw new AssertionError();}
}
