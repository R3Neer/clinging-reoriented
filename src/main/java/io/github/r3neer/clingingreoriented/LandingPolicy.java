package io.github.r3neer.clingingreoriented;

import net.minecraft.world.phys.Vec3;

/** Player-facing landing intent policy layered on top of geometric feet contact. */
public final class LandingPolicy {
    public enum Approach { GRAZE, AMBIGUOUS, CLEAR }

    /** Below this, approach direction is too noisy to treat as an immediate committed landing. */
    public static final double LOW_SPEED=0.12D;
    /** At or below this fraction of total speed into the support normal, contact is a grazing pass. */
    public static final double GRAZE_NORMAL_RATIO=0.12D;
    /** At or above this fraction, feet-first approach is clear enough to trust without persistence. */
    public static final double CLEAR_NORMAL_RATIO=0.30D;

    /** Clear landings may own camera/input only during the final quarter second. */
    public static final int CLEAR_COMMIT_TICKS=5;
    /** Ambiguous contacts commit later and only after being observed on consecutive server ticks. */
    public static final int AMBIGUOUS_COMMIT_TICKS=3;
    public static final int AMBIGUOUS_STABLE_TICKS=2;
    /** Ambiguous contacts may begin body anticipation only very near impact, after persistence. */
    public static final int AMBIGUOUS_BODY_TICKS=4;

    private static final double EPS=1.0E-9D;

    private LandingPolicy() {}

    public static Approach classify(Vec3 incomingVelocity,Vec3 supportNormal){
        if(incomingVelocity==null||supportNormal==null
            || !Double.isFinite(incomingVelocity.x+incomingVelocity.y+incomingVelocity.z+supportNormal.x+supportNormal.y+supportNormal.z)
            || Math.abs(supportNormal.lengthSqr()-1.0D)>1.0E-5D)return Approach.GRAZE;
        double speed=incomingVelocity.length();
        if(!Double.isFinite(speed)||speed<=EPS)return Approach.AMBIGUOUS;
        double normalSpeed=Math.max(0.0D,-incomingVelocity.dot(supportNormal));
        if(normalSpeed<=EPS)return Approach.GRAZE;
        if(speed<LOW_SPEED)return Approach.AMBIGUOUS;
        double ratio=normalSpeed/speed;
        if(ratio<=GRAZE_NORMAL_RATIO)return Approach.GRAZE;
        if(ratio>=CLEAR_NORMAL_RATIO)return Approach.CLEAR;
        return Approach.AMBIGUOUS;
    }

    public static boolean shouldCommit(LandingPrediction.Candidate candidate,int stableTicks){
        if(candidate==null)return false;
        return switch(candidate.approach()){
            case GRAZE -> false;
            case CLEAR -> candidate.etaTicks()<=CLEAR_COMMIT_TICKS+1.0E-6D;
            case AMBIGUOUS -> stableTicks>=AMBIGUOUS_STABLE_TICKS
                && candidate.etaTicks()<=AMBIGUOUS_COMMIT_TICKS+1.0E-6D;
        };
    }

    public static boolean bodyApproachReady(LandingPrediction.Candidate candidate,int stableTicks){
        if(candidate==null)return false;
        return switch(candidate.approach()){
            case GRAZE -> false;
            case CLEAR -> candidate.etaTicks()<=LandingTiming.PRESENTATION_TICKS+1.0E-6D;
            case AMBIGUOUS -> stableTicks>=AMBIGUOUS_STABLE_TICKS
                && candidate.etaTicks()<=AMBIGUOUS_BODY_TICKS+1.0E-6D;
        };
    }
}
