package io.github.r3neer.clingingreoriented;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import io.github.r3neer.clingingreoriented.mixin.BeaconAccess;
public final class BeaconPowers {
    public static void install(){
        var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
        var tiers=new ArrayList<List<Holder<MobEffect>>>(BeaconBlockEntity.BEACON_EFFECTS);
        if(!tiers.get(1).contains(clinging)){var second=new ArrayList<>(tiers.get(1));second.add(clinging);tiers.set(1,List.copyOf(second));BeaconAccess.clinging$effects(List.copyOf(tiers));}
        BeaconAccess.clinging$valid().add(clinging);
    }
}
