package io.github.r3neer.clingingreoriented;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pure Gravity Fall body-frame transport. The quaternion maps the avatar's canonical
 * local frame into world space; local +Y is the feet-to-head axis. Once a reliable
 * velocity direction exists, subsequent samples parallel-transport the frame by the
 * shortest world-space rotation between velocity directions. Near zero speed the
 * previous frame/direction are held exactly so reversals cannot manufacture roll.
 */
public final class BodyOrientation {
    public static final double DIRECTION_EPSILON_SQR=1.0E-6D;
    private static final double PARALLEL_EPSILON=1.0E-6D;

    public record State(Quaternionf orientation,Vec3 direction) {
        public State {
            orientation=new Quaternionf(orientation).normalize();
            if(direction!=null)direction=direction.normalize();
        }
        @Override public Quaternionf orientation(){return new Quaternionf(orientation);}
    }

    private BodyOrientation() {}

    /** Start from the body frame that is actually being displayed. */
    public static State start(Quaternionf displayed,Vec3 velocity){
        if(displayed==null)throw new IllegalArgumentException("Displayed body frame is required");
        Quaternionf frame=new Quaternionf(displayed).normalize();
        Vec3 direction=reliableDirection(velocity);
        if(direction==null)return new State(frame,null);
        return new State(alignUp(frame,direction),direction);
    }

    /** Advance from synchronized world velocity; unreliable samples deliberately do nothing. */
    public static State transport(State previous,Vec3 velocity){
        if(previous==null)throw new IllegalArgumentException("Previous body state is required");
        Vec3 next=reliableDirection(velocity);
        if(next==null)return new State(previous.orientation(),previous.direction());
        if(previous.direction()==null)return new State(alignUp(previous.orientation(),next),next);
        Quaternionf delta=shortestArc(previous.direction(),next,previous.orientation());
        Quaternionf transported=new Quaternionf(delta).mul(previous.orientation()).normalize();
        return new State(transported,next);
    }

    /** Align local +Y with a world direction while retaining as much of the displayed twist as possible. */
    public static Quaternionf alignUp(Quaternionf frame,Vec3 targetDirection){
        Vec3 target=reliableDirection(targetDirection);
        if(target==null)return new Quaternionf(frame).normalize();
        Vec3 currentUp=transform(frame,new Vec3(0,1,0)).normalize();
        Quaternionf delta=shortestArc(currentUp,target,frame);
        return new Quaternionf(delta).mul(new Quaternionf(frame).normalize()).normalize();
    }

    /** World-space shortest arc, with a deterministic twist-preserving axis for the 180-degree case. */
    public static Quaternionf shortestArc(Vec3 fromDirection,Vec3 toDirection,Quaternionf frame){
        Vec3 from=normalized(fromDirection,"from");
        Vec3 to=normalized(toDirection,"to");
        double dot=Math.max(-1.0D,Math.min(1.0D,from.dot(to)));
        if(dot>=1.0D-PARALLEL_EPSILON)return new Quaternionf();
        if(dot<=-1.0D+PARALLEL_EPSILON){
            Vec3 axis=oppositeAxis(from,frame);
            return new Quaternionf().rotateAxis((float)Math.PI,(float)axis.x,(float)axis.y,(float)axis.z).normalize();
        }
        Vec3 axis=from.cross(to).normalize();
        float angle=(float)Math.acos(dot);
        return new Quaternionf().rotateAxis(angle,(float)axis.x,(float)axis.y,(float)axis.z).normalize();
    }

    public static Vec3 bodyUp(Quaternionf frame){return transform(frame,new Vec3(0,1,0)).normalize();}
    public static Vec3 bodyForward(Quaternionf frame){return transform(frame,new Vec3(0,0,-1)).normalize();}

    public static Vec3 reliableDirection(Vec3 velocity){
        if(velocity==null || !Double.isFinite(velocity.x+velocity.y+velocity.z) || velocity.lengthSqr()<=DIRECTION_EPSILON_SQR)return null;
        return velocity.normalize();
    }

    private static Vec3 oppositeAxis(Vec3 from,Quaternionf frame){
        Vec3 forward=bodyForward(frame);
        Vec3 projected=forward.subtract(from.scale(forward.dot(from)));
        if(projected.lengthSqr()<=DIRECTION_EPSILON_SQR){
            Vec3 right=transform(frame,new Vec3(1,0,0)).normalize();
            projected=right.subtract(from.scale(right.dot(from)));
        }
        if(projected.lengthSqr()<=DIRECTION_EPSILON_SQR){
            Vec3 fallback=Math.abs(from.y)<0.9D?new Vec3(0,1,0):new Vec3(1,0,0);
            projected=from.cross(fallback);
        }
        return projected.normalize();
    }

    private static Vec3 normalized(Vec3 value,String name){
        if(value==null || !Double.isFinite(value.x+value.y+value.z) || value.lengthSqr()<=DIRECTION_EPSILON_SQR)
            throw new IllegalArgumentException(name+" direction must be finite and non-zero");
        return value.normalize();
    }

    private static Vec3 transform(Quaternionf quaternion,Vec3 vector){
        Vector3f out=new Quaternionf(quaternion).normalize().transform(new Vector3f((float)vector.x,(float)vector.y,(float)vector.z));
        return new Vec3(out.x,out.y,out.z);
    }
}