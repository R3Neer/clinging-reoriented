package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure body-follow, momentum-redirection and aerodynamic-drag math for sustained Gravity Fall. */
public final class GravityFallAerodynamics {
    public static final double LOOK_DEADZONE_RADIANS=Math.toRadians(35.0D);
    public static final double MAX_FOLLOW_RADIANS_PER_TICK=Math.toRadians(7.5D);
    public static final double MAX_EXTRA_DRAG=0.013D;
    public static final double MAX_REDIRECT_RADIANS_PER_TICK=Math.toRadians(6.0D);
    public static final double MIN_REDIRECT_SPEED=0.20D;

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

    public static double dragFactor(double streamlining){
        double aligned=clamp(streamlining,0.0D,1.0D);
        double broadside=1.0D-aligned*aligned;
        return 1.0D-MAX_EXTRA_DRAG*broadside;
    }

    public static Vec3 applyDrag(Vec3 velocity,double streamlining){
        if(velocity==null||!Double.isFinite(velocity.x+velocity.y+velocity.z))return Vec3.ZERO;
        return velocity.scale(dragFactor(streamlining));
    }

    /**
     * Holding W bends existing momentum toward gaze only when gaze projects positively onto the
     * current velocity. Magnitude is preserved exactly here; posture drag is applied separately.
     */
    public static Vec3 redirectMomentum(Vec3 velocity,Vec3 worldLook,double forwardIntent){
        if(velocity==null||!Double.isFinite(velocity.x+velocity.y+velocity.z))return Vec3.ZERO;
        double speed=velocity.length();
        if(speed<MIN_REDIRECT_SPEED)return velocity;
        Vec3 look=BodyOrientation.reliableDirection(worldLook);
        if(look==null)return velocity;
        double input=clamp(forwardIntent,0.0D,1.0D);
        if(input<=1.0E-6D)return velocity;

        Vec3 direction=velocity.scale(1.0D/speed);
        double dot=clamp(direction.dot(look),-1.0D,1.0D);
        double authority=Math.max(0.0D,dot);
        if(authority<=1.0E-6D)return velocity;
        double angle=Math.acos(dot);
        if(angle<=1.0E-7D)return velocity;

        double turn=Math.min(angle,MAX_REDIRECT_RADIANS_PER_TICK*input*authority);
        Vec3 tangent=look.subtract(direction.scale(dot));
        double tangentLength=tangent.length();
        if(tangentLength<=1.0E-8D)return velocity;
        tangent=tangent.scale(1.0D/tangentLength);
        Vec3 steered=direction.scale(Math.cos(turn)).add(tangent.scale(Math.sin(turn))).normalize();
        return steered.scale(speed);
    }

    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
