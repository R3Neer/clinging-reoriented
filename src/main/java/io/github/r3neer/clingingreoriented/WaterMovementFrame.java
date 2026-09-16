package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure camera-relative WASD remap for the water locomotion boundary. */
public final class WaterMovementFrame {
    private static final double EPS_SQR=1.0E-12D;
    public record Basis(Vec3 forward,Vec3 left) {}

    private WaterMovementFrame() {}

    /** Full 3D camera basis. Forward intentionally keeps pitch instead of projecting onto a floor. */
    public static Basis cameraBasis(Quaternionf cameraRotation){
        if(cameraRotation==null)return new Basis(Vec3.ZERO,Vec3.ZERO);
        Quaternionf q=new Quaternionf(cameraRotation).normalize();
        Vector3f forward=q.transform(new Vector3f(0,0,-1));
        Vector3f left=q.transform(new Vector3f(-1,0,0));
        Vec3 f=new Vec3(forward.x,forward.y,forward.z);
        Vec3 l=new Vec3(left.x,left.y,left.z);
        if(!finite(f)||!finite(l)||f.lengthSqr()<=EPS_SQR||l.lengthSqr()<=EPS_SQR)return new Basis(Vec3.ZERO,Vec3.ZERO);
        return new Basis(f.normalize(),l.normalize());
    }

    /**
     * Re-express only the already-normalized tangent WASD delta in the visible camera basis.
     * Gravity-axis input is deliberately discarded here: Space/Shift own world +/-Y separately.
     */
    public static Vec3 remap(Vec3 physicalMovement,Vec3 physicalForward,Quaternionf cameraRotation,Direction gravity){
        if(physicalMovement==null||physicalForward==null||gravity==null||!finite(physicalMovement)||!finite(physicalForward))return Vec3.ZERO;
        Vec3 down=GravityTransition.direction(gravity).normalize();
        Vec3 up=down.scale(-1.0D);
        Vec3 sourceForward=projectPlane(physicalForward,down);
        if(sourceForward.lengthSqr()<=EPS_SQR)return tangent(physicalMovement,up);
        sourceForward=sourceForward.normalize();
        Vec3 sourceLeft=up.cross(sourceForward);
        if(sourceLeft.lengthSqr()<=EPS_SQR)return tangent(physicalMovement,up);
        sourceLeft=sourceLeft.normalize();

        Vec3 tangent=tangent(physicalMovement,up);
        if(tangent.lengthSqr()<=EPS_SQR)return Vec3.ZERO;
        double forwardAmount=tangent.dot(sourceForward);
        double leftAmount=tangent.dot(sourceLeft);

        Basis camera=cameraBasis(cameraRotation);
        if(camera.forward().lengthSqr()<=EPS_SQR||camera.left().lengthSqr()<=EPS_SQR)return tangent;
        Vec3 remapped=camera.forward().scale(forwardAmount).add(camera.left().scale(leftAmount));
        return finite(remapped)?remapped:tangent;
    }

    private static Vec3 tangent(Vec3 movement,Vec3 up){return movement.subtract(up.scale(movement.dot(up)));}
    private static Vec3 projectPlane(Vec3 value,Vec3 normal){return value.subtract(normal.scale(value.dot(normal)));}
    private static boolean finite(Vec3 value){return value!=null&&Double.isFinite(value.x+value.y+value.z);}
}
