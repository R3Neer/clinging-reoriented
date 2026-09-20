package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

public final class GravityInput {
    static final double BASE_PLAYER_JUMP_POWER=0.42D;
    static final double MAX_LANDING_GRACE_TICKS=3.0D;

    /** Mirrors the audited vanilla glide eligibility without starting a glide. */
    public static boolean elytraWins(Player p) {
        if(p.isFallFlying())return true;
        if(p.onGround() || p.isPassenger() || p.isInWater() || p.onClimbable() || p.hasEffect(MobEffects.LEVITATION))return false;
        for(var slot:EquipmentSlot.VALUES)if(LivingEntity.canGlideUsing(p.getItemBySlot(slot),slot))return true;
        return false;
    }

    static double effectiveJumpPower(Player p){
        double base=BASE_PLAYER_JUMP_POWER;
        var jump=p.getAttribute(Attributes.JUMP_STRENGTH);
        if(jump!=null && Double.isFinite(jump.getValue()) && jump.getValue()>=0.0D)base=jump.getValue();
        double power=base+p.getJumpBoostPower();
        return Double.isFinite(power)&&power>=0.0D?power:BASE_PLAYER_JUMP_POWER;
    }

    static double landingGraceTicks(Player p){
        return Math.max(1.0D,Math.min(MAX_LANDING_GRACE_TICKS,effectiveJumpPower(p)/BASE_PLAYER_JUMP_POWER));
    }

    private static AABB swept(AABB current,AABB predicted){
        return new AABB(
            Math.min(current.minX,predicted.minX),Math.min(current.minY,predicted.minY),Math.min(current.minZ,predicted.minZ),
            Math.max(current.maxX,predicted.maxX),Math.max(current.maxY,predicted.maxY),Math.max(current.maxZ,predicted.maxZ));
    }

    /**
     * A sprint-jump may queue its next Space shortly before contact. Reserve that input only
     * while actually descending toward support predicted inside a short gravity-relative
     * landing horizon. Stronger jump power widens that horizon proportionally so Leaping
     * keeps the same sprint-jump feel; normal jump power retains alpha.11's one-tick policy.
     */
    public static boolean sprintLandingJumpReserved(Player p) {
        if(!p.isSprinting() || p.onGround() || AirChanges.grounded(p))return false;
        var gravity=GravityDirectionUtil.getGravityDirection(p);
        var gravityUnit=GravityTransition.direction(gravity);
        double toward=p.getDeltaMovement().dot(gravityUnit);
        if(!Double.isFinite(toward) || toward<=1.0E-4D)return false;
        double acceleration=GravityDirectionUtil.scaleGravity(p,0.08D);
        double horizon=landingGraceTicks(p);
        double predictedTravel=toward*horizon+acceleration*horizon*(horizon+1.0D)*0.5D;
        double travel=Math.max(0.10D,Math.min(0.60D*horizon,predictedTravel));
        var current=p.getBoundingBox().deflate(1.0E-5D);
        if(!p.level().noCollision(p,current))return false;
        var predicted=current.move(gravityUnit.scale(travel));
        return !p.level().noCollision(p,swept(current,predicted));
    }

    public static boolean available(Player p) {
        if(p.isPassenger())return p.hasEffect(Reorientation.EFFECT) && !p.isSpectator() && !p.isSleeping();
        return !AirChanges.grounded(p) && !sprintLandingJumpReserved(p)
            && !p.isPassenger() && !p.isSleeping()
            && !p.isSpectator() && !p.getAbilities().flying && !elytraWins(p);
    }
}
