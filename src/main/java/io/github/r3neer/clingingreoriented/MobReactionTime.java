package io.github.r3neer.clingingreoriented;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** S07 shared perception/reaction latency derived from characteristic base movement speed. */
public final class MobReactionTime {
    public static final int MIN_TICKS=2;
    public static final int MAX_TICKS=10;
    static final double HALF_RESPONSE_SPEED=.25D;
    static final double CURVE_POWER=1.5D;

    private MobReactionTime() {}

    /**
     * Uses the base MOVEMENT_SPEED attribute, not deltaMovement and not transient attribute modifiers.
     * Faster characteristic locomotion implies lower latency, but reaction is never instantaneous.
     */
    public static int ticks(LivingEntity entity){
        if(entity==null)return MAX_TICKS;
        return ticksForBaseSpeed(entity.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
    }

    static int ticksForBaseSpeed(double baseSpeed){
        if(!Double.isFinite(baseSpeed)||baseSpeed<=0.0D)return MAX_TICKS;
        double ratio=baseSpeed/HALF_RESPONSE_SPEED;
        double response=MIN_TICKS+(MAX_TICKS-MIN_TICKS)/(1.0D+Math.pow(ratio,CURVE_POWER));
        return Math.clamp((int)Math.round(response),MIN_TICKS,MAX_TICKS);
    }
}
