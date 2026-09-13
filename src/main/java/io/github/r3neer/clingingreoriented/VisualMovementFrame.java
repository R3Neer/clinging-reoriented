package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Pure FR-GF-060/061 movement-frame correction. It never creates acceleration: an
 * already-normalized Gravity Changer movement delta is rigidly rotated around physical
 * up so its forward axis follows the rendered camera projected onto the walk plane.
 */
public final class VisualMovementFrame {
    public static final double PROJECTION_EPSILON_SQR=1.0E-8D;

    public record Basis(Vec3 forward,Vec3 right) {}
    public record Result(Vec3 movement,Vec3 forward) {}

    private VisualMovementFrame() {}

    public static Basis basis(Quaternionf cameraRotation,Direction gravity,Vec3 lastStableForward){
        if(cameraRotation==null||gravity==null)throw new IllegalArgumentException("camera rotation and gravity are required");
        Vec3 down=GravityTransition.direction(gravity).normalize();
        Vec3 up=down.scale(-1.0D);

        Vec3 cameraForward=transform(cameraRotation,new Vec3(0,0,-1)).normalize();
        Vec3 projectedForward=projectPlane(cameraForward,down);
        Vec3 forward;
        if(reliable(projectedForward)){
            forward=projectedForward.normalize();
        }else{
            // Looking straight into/out of the physical floor: screen-right remains a stable
            // tangent. Reconstruct screen-up/forward from it instead of amplifying tiny noise.
            Vec3 cameraRight=transform(cameraRotation,new Vec3(1,0,0)).normalize();
            Vec3 projectedRight=projectPlane(cameraRight,down);
            if(reliable(projectedRight)){
                Vec3 right=projectedRight.normalize();
                forward=up.cross(right).normalize();
            }else{
                Vec3 remembered=projectPlane(lastStableForward,down);
                if(reliable(remembered))forward=remembered.normalize();
                else forward=canonicalTangent(up,down);
            }
        }
        Vec3 right=forward.cross(up).normalize();
        return new Basis(forward,right);
    }

    /**
     * Rotate the complete physical delta around physical up. Gravity-axis input, if any,
     * is therefore preserved exactly while the tangent WASD basis is corrected.
     */
    public static Result remap(Vec3 physicalMovement,Vec3 physicalForward,Quaternionf cameraRotation,Direction gravity,Vec3 lastStableForward){
        if(physicalMovement==null||physicalForward==null)throw new IllegalArgumentException("movement and physical forward are required");
        Basis target=basis(cameraRotation,gravity,lastStableForward);
        Vec3 down=GravityTransition.direction(gravity).normalize();
        Vec3 up=down.scale(-1.0D);
        Vec3 sourceProjected=projectPlane(physicalForward,down);
        if(!reliable(sourceProjected))return new Result(physicalMovement,target.forward());
        Vec3 source=sourceProjected.normalize();
        double cos=clamp(source.dot(target.forward()),-1.0D,1.0D);
        double sin=clamp(up.dot(source.cross(target.forward())),-1.0D,1.0D);
        double angle=Math.atan2(sin,cos);
        Vec3 corrected=rotateAroundAxis(physicalMovement,up,angle);
        return new Result(corrected,target.forward());
    }

    public static Vec3 projectPlane(Vec3 value,Vec3 normal){
        if(value==null||normal==null||!finite(value)||!finite(normal)||normal.lengthSqr()<=PROJECTION_EPSILON_SQR)return Vec3.ZERO;
        Vec3 n=normal.normalize();
        return value.subtract(n.scale(value.dot(n)));
    }

    private static Vec3 canonicalTangent(Vec3 up,Vec3 down){
        Vec3 candidate=Math.abs(down.y)<0.9D?new Vec3(0,1,0):new Vec3(0,0,-1);
        Vec3 tangent=projectPlane(candidate,down);
        if(!reliable(tangent))tangent=projectPlane(new Vec3(1,0,0),down);
        Vec3 right=tangent.normalize().cross(up).normalize();
        return up.cross(right).normalize();
    }

    private static Vec3 rotateAroundAxis(Vec3 value,Vec3 axis,double angle){
        if(value.lengthSqr()==0.0D||Math.abs(angle)<1.0E-12D)return value;
        Vec3 k=axis.normalize();
        double cos=Math.cos(angle),sin=Math.sin(angle);
        return value.scale(cos).add(k.cross(value).scale(sin)).add(k.scale(k.dot(value)*(1.0D-cos)));
    }

    private static Vec3 transform(Quaternionf quaternion,Vec3 vector){
        Vector3f out=new Quaternionf(quaternion).normalize().transform(new Vector3f((float)vector.x,(float)vector.y,(float)vector.z));
        return new Vec3(out.x,out.y,out.z);
    }
    private static boolean reliable(Vec3 value){return value!=null&&finite(value)&&value.lengthSqr()>PROJECTION_EPSILON_SQR;}
    private static boolean finite(Vec3 value){return Double.isFinite(value.x+value.y+value.z);}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
}
