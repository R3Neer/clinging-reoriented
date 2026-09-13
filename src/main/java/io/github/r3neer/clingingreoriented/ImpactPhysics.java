package io.github.r3neer.clingingreoriented;

import net.minecraft.world.phys.Vec3;

/** Pure impact geometry and vanilla-equivalent fall calibration. */
public final class ImpactPhysics {
    private static final double COLLISION_EPSILON=1.0E-7D;
    private static final double VANILLA_GRAVITY=0.08D;
    private static final double VANILLA_DRAG=0.98D;
    private static final int MAX_CALIBRATION_TICKS=160;

    private ImpactPhysics() {}

    /** Components of requested motion that a collision actually removed. */
    public static Vec3 absorbedVelocity(Vec3 intended,Vec3 actual){
        if(!finite(intended)||!finite(actual))return Vec3.ZERO;
        return new Vec3(absorbed(intended.x,actual.x),absorbed(intended.y,actual.y),absorbed(intended.z,actual.z));
    }

    public static double impactSpeed(Vec3 intended,Vec3 actual){return absorbedVelocity(intended,actual).length();}

    private static double absorbed(double intended,double actual){
        double requested=Math.abs(intended);
        if(requested<=COLLISION_EPSILON)return 0.0D;
        if(Math.signum(actual)!=0.0D && Math.signum(actual)!=Math.signum(intended))return intended;
        if(Math.abs(actual)>=requested-COLLISION_EPSILON)return 0.0D;
        return intended-actual;
    }

    /**
     * Distance a standard vanilla free fall needs to reach this pre-collision speed.
     * The table is generated from vanilla's 0.08 gravity / 0.98 air-drag recurrence.
     * Beyond the useful survival range it saturates monotonically at an already-lethal distance.
     */
    public static double vanillaEquivalentFallDistance(double speed){
        if(!Double.isFinite(speed)||speed<=0.0D)return 0.0D;
        double velocity=0.0D,distance=0.0D;
        for(int tick=0;tick<MAX_CALIBRATION_TICKS;tick++){
            velocity-=VANILLA_GRAVITY;
            distance-=velocity;
            if(-velocity>=speed-COLLISION_EPSILON)return distance;
            velocity*=VANILLA_DRAG;
        }
        return distance;
    }

    public static boolean finite(Vec3 vector){return vector!=null&&Double.isFinite(vector.x+vector.y+vector.z);}
}
