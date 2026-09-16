package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Persistent Gravity Fall body attitude plus body-relative aerodynamic response. */
public final class GravityFallAerodynamics {
    public static final double LOOK_DEADZONE_RADIANS=Math.toRadians(35.0D);
    public static final double MAX_FOLLOW_RADIANS_PER_TICK=Math.toRadians(7.5D);
    public static final double MAX_STABILIZE_RADIANS_PER_TICK=Math.toRadians(1.25D);
    public static final double EXTRA_TRANSVERSE_DRAG=0.025D;

    private GravityFallAerodynamics() {}

    /** Let the camera move freely inside a neck cone; beyond it the macro body follows gradually. */
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

    /**
     * Velocity may gently weathercock the body, but it no longer transports the whole body frame.
     * The closest end of the head/feet axis is used so stabilization can never manufacture a 180° flip.
     */
    public static BodyOrientation.State stabilize(BodyOrientation.State state,Vec3 velocity){
        if(state==null)return null;
        Vec3 direction=BodyOrientation.reliableDirection(velocity);
        if(direction==null)return new BodyOrientation.State(state.orientation(),state.direction());
        Quaternionf frame=state.orientation();
        Vec3 axis=BodyOrientation.bodyUp(frame);
        Vec3 target=axis.dot(direction)>=0.0D?direction:direction.scale(-1.0D);
        double dot=clamp(axis.dot(target),-1.0D,1.0D);
        double angle=Math.acos(dot);
        if(angle<=1.0E-8D)return new BodyOrientation.State(frame,direction);
        Quaternionf full=BodyOrientation.shortestArc(axis,target,frame);
        double applied=Math.min(angle,MAX_STABILIZE_RADIANS_PER_TICK);
        float fraction=(float)Math.min(1.0D,applied/angle);
        Quaternionf delta=new Quaternionf().slerp(full,fraction).normalize();
        return new BodyOrientation.State(delta.mul(frame).normalize(),direction);
    }

    /** Player gaze owns attitude intent; velocity only contributes weak passive stabilization. */
    public static BodyOrientation.State advanceBody(BodyOrientation.State state,Vec3 velocity,Vec3 worldLook,float bodyYaw){
        BodyOrientation.State next=stabilize(state,velocity);
        return worldLook==null?next:followLook(next,worldLook,bodyYaw);
    }

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

    /**
     * Decompose velocity in the visible body's long-axis frame. Longitudinal momentum is retained;
     * transverse momentum receives extra drag. Direction therefore bends toward the body axis as a
     * consequence of posture, without W, without adding energy and without an angular steering magnet.
     */
    public static Vec3 applyAerodynamics(BodyOrientation.State body,Vec3 velocity){
        if(!ImpactPhysics.finite(velocity))return Vec3.ZERO;
        if(body==null||velocity.lengthSqr()<=BodyOrientation.DIRECTION_EPSILON_SQR)return velocity;
        Vec3 axis=BodyOrientation.bodyUp(body.orientation());
        double along=velocity.dot(axis);
        Vec3 longitudinal=axis.scale(along);
        Vec3 transverse=velocity.subtract(longitudinal);
        return longitudinal.add(transverse.scale(1.0D-EXTRA_TRANSVERSE_DRAG));
    }

    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
