package io.github.r3neer.clingingreoriented;

import net.minecraft.world.phys.Vec3;

/** Physical confidence policy for deciding whether a feet-first contact is a landing or only a graze. */
public final class LandingContactPolicy {
    public enum ContactKind { CLEAR, AMBIGUOUS, GRAZE }

    public static final double CLEAR_MIN_NORMAL_FRACTION=0.25D;
    public static final double GRAZE_MAX_NORMAL_FRACTION=0.08D;
    public static final double MIN_CLEAR_NORMAL_SPEED=0.06D;
    public static final double FAST_GRAZE_SPEED=0.20D;

    public record Assessment(ContactKind kind,double normalSpeed,double totalSpeed,double normalFraction) {}

    private LandingContactPolicy() {}

    public static Assessment assess(Vec3 incomingVelocity,Vec3 supportNormal){
        if(!ImpactPhysics.finite(incomingVelocity)||supportNormal==null
            || !Double.isFinite(supportNormal.x+supportNormal.y+supportNormal.z)
            || Math.abs(supportNormal.lengthSqr()-1.0D)>1.0E-5D)
            return new Assessment(ContactKind.GRAZE,0.0D,0.0D,0.0D);

        double totalSpeed=Math.sqrt(Math.max(0.0D,incomingVelocity.lengthSqr()));
        double normalSpeed=Math.max(0.0D,-incomingVelocity.dot(supportNormal));
        double normalFraction=totalSpeed<=1.0E-12D?0.0D:Math.min(1.0D,normalSpeed/totalSpeed);

        ContactKind kind;
        if(totalSpeed>=FAST_GRAZE_SPEED && normalFraction<GRAZE_MAX_NORMAL_FRACTION)kind=ContactKind.GRAZE;
        else if(normalSpeed>=MIN_CLEAR_NORMAL_SPEED && normalFraction>=CLEAR_MIN_NORMAL_FRACTION)kind=ContactKind.CLEAR;
        else kind=ContactKind.AMBIGUOUS;
        return new Assessment(kind,normalSpeed,totalSpeed,normalFraction);
    }
}
