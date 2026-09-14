package io.github.r3neer.clingingreoriented;

import org.joml.Quaternionf;

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

    /**
     * Third person keeps the model centre fixed; a local First Person body pass keeps the
     * hidden head/eye anchor fixed so the macro rotation cannot swing torso/legs through camera.
     */
    public static float pivotHeight(float bodyHeight,float eyeHeight,boolean firstPersonBodyPass){
        float body=Float.isFinite(bodyHeight)?Math.max(0.0F,bodyHeight):0.0F;
        float eye=Float.isFinite(eyeHeight)?Math.max(0.0F,eyeHeight):0.0F;
        return firstPersonBodyPass?eye:body*0.5F;
    }

    /** Test/debug helper mirroring the PoseStack rotation order used by the renderer. */
    public static Quaternionf composed(Quaternionf visual,Quaternionf extra){
        if(visual==null||extra==null)throw new IllegalArgumentException("visual/extra quaternion required");
        return new Quaternionf(visual).normalize().mul(new Quaternionf(extra).normalize()).normalize();
    }
}
