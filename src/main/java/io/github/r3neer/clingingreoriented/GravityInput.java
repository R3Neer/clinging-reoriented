package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
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

    /**
     * A sprint-jump may queue its next Space a fraction before contact. Reserve that input
     * only when the player is actually descending toward support they are predicted to hit
     * on the next simulation step. All math follows the active gravity direction.
     */
    public static boolean sprintLandingJumpReserved(Player p) {
        if(!p.isSprinting() || p.onGround() || AirChanges.grounded(p))return false;
        var gravity=GravityDirectionUtil.getGravityDirection(p);
        var gravityUnit=GravityTransition.direction(gravity);
        double toward=p.getDeltaMovement().dot(gravityUnit);
        if(!Double.isFinite(toward) || toward<=1.0E-4D)return false;
        double acceleration=GravityDirectionUtil.scaleGravity(p,0.08D);
        double travel=Math.max(0.10D,Math.min(0.60D,toward+acceleration));
        var current=p.getBoundingBox().deflate(1.0E-5D);
        if(!p.level().noCollision(p,current))return false;
        var predicted=current.move(gravityUnit.scale(travel));
        return !p.level().noCollision(p,predicted);
    }

    public static boolean available(Player p) {
        if(p.isPassenger())return p.hasEffect(Reorientation.EFFECT) && !p.isSpectator() && !p.isSleeping();
        return !p.onGround() && !AirChanges.grounded(p) && !sprintLandingJumpReserved(p)
            && !p.isPassenger() && !p.isSleeping()
            && !p.isSpectator() && !p.getAbilities().flying && !elytraWins(p);
    }
}
