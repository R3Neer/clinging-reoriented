package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure body-follow and aerodynamic-drag math for sustained Gravity Fall. */
public final class GravityFallAerodynamics {
    /** The head can look this far away from body-facing before the macro body starts following. */
    public static final double LOOK_DEADZONE_RADIANS=Math.toRadians(35.0D);
    /** Body catch-up is intentionally inertial rather than snapping to the camera. */
    public static final double MAX_FOLLOW_RADIANS_PER_TICK=Math.toRadians(7.5D);
    /** Additional drag at a perfectly broadside posture; streamlined posture adds none. */
    public static final double MAX_EXTRA_DRAG=0.013D;

    private GravityFallAerodynamics() {}

    /**
     * Let the camera move freely inside a neck cone. Beyond it, rotate the macro body only enough
     * to chase the excess angle, capped per tick. This never rotates velocity or adds thrust.
     */
    public static BodyOrientation.State followLook(BodyOrientation.State state,Vec3 worldLook,float bodyYaw){
        if(state==null)return null;
        Vec3 look=BodyOrientation.reliableDirection(worldLook);
        if(look==null||!Float.isFinite(bodyYaw))return new BodyOrientation.State(state.orientation(),state.direction());
        Quaternionf frame=state.orientation();
        Vec3 facing=bodyFacing(frame,bodyYaw);
        double dot=clamp(facing.dot(look),-1.0D,1.0D);
        double angle=Math.acos(dot);
        double excess=angle-LOOK_DEADZONE_RADIANS;
        if(excess<=1.0E-8D)return new BodyOrientation.State(frame,state.direction());

        Quaternionf full=BodyOrientation.shortestArc(facing,look,frame);
        double applied=Math.min(excess,MAX_FOLLOW_RADIANS_PER_TICK);
        float fraction=(float)Math.min(1.0D,applied/Math.max(angle,1.0E-8D));
        Quaternionf delta=new Quaternionf().slerp(full,fraction).normalize();
        Quaternionf steered=delta.mul(frame).normalize();
        return new BodyOrientation.State(steered,state.direction());
    }

    /** Actual world-space facing after inner vanilla body yaw and the macro Gravity Fall root. */
    public static Vec3 bodyFacing(Quaternionf frame,float bodyYaw){
        Vec3 local=RotationUtil.rotToVec(bodyYaw,0.0F).normalize();
        Vector3f out=new Quaternionf(frame).normalize().transform(new Vector3f((float)local.x,(float)local.y,(float)local.z));
        return new Vec3(out.x,out.y,out.z).normalize();
    }

    /** 1 = long body axis parallel/anti-parallel to velocity, 0 = perfectly broadside. */
    public static double streamlining(BodyOrientation.State body,Vec3 velocity){
        Vec3 direction=BodyOrientation.reliableDirection(velocity);
        if(body==null||direction==null)return 1.0D;
        return clamp(Math.abs(BodyOrientation.bodyUp(body.orientation()).dot(direction)),0.0D,1.0D);
    }

    /** Extra multiplicative air retention after vanilla/gravity-changer drag. */
    public static double dragFactor(double streamlining){
        double aligned=clamp(streamlining,0.0D,1.0D);
        double broadside=1.0D-aligned*aligned;
        return 1.0D-MAX_EXTRA_DRAG*broadside;
    }

    public static Vec3 applyDrag(Vec3 velocity,double streamlining){
        if(velocity==null||!Double.isFinite(velocity.x+velocity.y+velocity.z))return Vec3.ZERO;
        return velocity.scale(dragFactor(streamlining));
    }

    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
