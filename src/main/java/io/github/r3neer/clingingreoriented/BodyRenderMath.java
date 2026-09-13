package io.github.r3neer.clingingreoriented;

import org.joml.Quaternionf;

/** Pure quaternion composition used by the avatar root mixin. */
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

    /** Test/debug helper mirroring the PoseStack rotation order used by the renderer. */
    public static Quaternionf composed(Quaternionf visual,Quaternionf extra){
        if(visual==null||extra==null)throw new IllegalArgumentException("visual/extra quaternion required");
        return new Quaternionf(visual).normalize().mul(new Quaternionf(extra).normalize()).normalize();
    }
}
