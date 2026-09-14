package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Gravity policy for vanilla climbables while Clinging owns player physics. */
public final class ClimbablePolicy {
    public static final double MAX_AXIS_SPEED=0.15D;
    public static final double CLIMB_IMPULSE=0.2D;

    private ClimbablePolicy() {}

    public static boolean participates(Direction gravity){
        return gravity==Direction.DOWN||gravity==Direction.UP;
    }

    /** Mirror vanilla's ladder damping across world Y for upside-down gravity. */
    public static Vec3 clampForUpGravity(Vec3 delta,boolean suppressSlide,boolean scaffolding){
        if(delta==null||!Double.isFinite(delta.x+delta.y+delta.z))return Vec3.ZERO;
        double x=Mth.clamp(delta.x,-MAX_AXIS_SPEED,MAX_AXIS_SPEED);
        double z=Mth.clamp(delta.z,-MAX_AXIS_SPEED,MAX_AXIS_SPEED);
        double y=Math.min(delta.y,MAX_AXIS_SPEED);
        if(y>0.0D&&!scaffolding&&suppressSlide)y=0.0D;
        return new Vec3(x,y,z);
    }

    public static double climbImpulse(Direction gravity,double vanillaImpulse){
        return gravity==Direction.UP?-Math.abs(vanillaImpulse):vanillaImpulse;
    }
}
