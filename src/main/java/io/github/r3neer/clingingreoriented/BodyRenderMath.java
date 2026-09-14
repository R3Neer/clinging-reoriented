package io.github.r3neer.clingingreoriented;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure quaternion/pivot composition used by the avatar root mixin. */
public final class BodyRenderMath {
    private BodyRenderMath() {}

    /**
     * PoseStack post-multiplies rotations. Gravity Changer has already applied visual,
     * so visual * extra must equal the absolute body frame.
     */
    public static Quaternionf extraRoot(Quaternionf visual,Quaternionf body){
        if(visual==null||body==null)throw new IllegalArgumentException("visual/body quaternion required");
        Quaternionf inverseVisual=new Quaternionf(visual).normalize().conjugate();
        return inverseVisual.mul(new Quaternionf(body).normalize()).normalize();
    }

    /** Third-person macro roots rotate around the displayed model centre. */
    public static float bodyCenterPivot(float bodyHeight){
        return Float.isFinite(bodyHeight)?Math.max(0.0F,bodyHeight)*0.5F:0.0F;
    }

    /**
     * First Person temporarily extracts the local avatar at a translated world position while
     * the real camera remains at the origin of render space. EntityRenderDispatcher therefore
     * enters the avatar renderer with transform T(renderedEntity-camera) * R_visual.
     *
     * To keep the actual camera point fixed while applying the additional Gravity Fall root,
     * express the world-space vector from rendered origin back to camera in the already-rotated
     * local frame: P = inverse(R_visual) * (-T).
     */
    public static Vec3 localCameraPivot(double translatedX,double translatedY,double translatedZ,Quaternionf visual){
        if(visual==null)throw new IllegalArgumentException("visual quaternion required");
        if(!Double.isFinite(translatedX+translatedY+translatedZ))return Vec3.ZERO;
        Quaternionf inverse=new Quaternionf(visual).normalize().conjugate();
        Vector3f local=inverse.transform(new Vector3f((float)-translatedX,(float)-translatedY,(float)-translatedZ));
        return new Vec3(local.x,local.y,local.z);
    }

    /** Test/debug helper mirroring the PoseStack rotation order used by the renderer. */
    public static Quaternionf composed(Quaternionf visual,Quaternionf extra){
        if(visual==null||extra==null)throw new IllegalArgumentException("visual/extra quaternion required");
        return new Quaternionf(visual).normalize().mul(new Quaternionf(extra).normalize()).normalize();
    }
}
