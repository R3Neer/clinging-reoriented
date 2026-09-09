package io.github.r3neer.clingingreoriented;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;

public final class GravityInput {
    /** Mirrors the audited vanilla glide eligibility without starting a glide. */
    public static boolean elytraWins(Player p) {
        if(p.isFallFlying())return true;
        if(p.onGround() || p.isPassenger() || p.isInWater() || p.onClimbable() || p.hasEffect(MobEffects.LEVITATION))return false;
        for(var slot:EquipmentSlot.VALUES)if(LivingEntity.canGlideUsing(p.getItemBySlot(slot),slot))return true;
        return false;
    }
    public static boolean available(Player p) {
        if(p.isPassenger())return p.hasEffect(Reorientation.EFFECT) && !p.isSpectator() && !p.isSleeping();
        return !p.onGround() && !AirChanges.grounded(p) && !p.isPassenger() && !p.isSleeping()
            && !p.isSpectator() && !p.getAbilities().flying && !elytraWins(p);
    }
}
