package io.github.r3neer.clingingreoriented.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** Pure helpers for continuous, screen-relative full-sphere camera look during Gravity Fall. */
public final class GravityFallLookMath {
    private static final float DEG_TO_RAD=(float)Math.PI/180.0F;
    private static final float RAD_TO_DEG=180.0F/(float)Math.PI;
    private static final float POLE_EPS=1.0E-6F;
    public record VanillaLook(float yaw,float pitch) {}

    private GravityFallLookMath() {}

    /** Exact vanilla Camera base rotation before Gravity Changer prepends visual gravity. */
    public static Quaternionf vanillaRotation(float yaw,float pitch){
        if(!Float.isFinite(yaw)||!Float.isFinite(pitch))return new Quaternionf();
        return new Quaternionf().rotationYXZ((float)Math.PI-yaw*DEG_TO_RAD,-pitch*DEG_TO_RAD,0.0F).normalize();
    }

    /** Apply mouse deltas around the camera's own screen axes, not an Euler/world yaw axis. */
    public static Quaternionf screenTurn(Quaternionfc current,float horizontalDegrees,float verticalDegrees){
        if(current==null||!Float.isFinite(horizontalDegrees)||!Float.isFinite(verticalDegrees))return new Quaternionf();
        return new Quaternionf(current)
            .rotateY(-horizontalDegrees*DEG_TO_RAD)
            .rotateX(-verticalDegrees*DEG_TO_RAD)
            .normalize();
    }

    /** Re-express a gravity-frame yaw gauge change without changing the rendered world camera frame. */
    public static Quaternionf rebaseYaw(Quaternionfc current,float yawDelta){
        if(current==null||!Float.isFinite(yawDelta))return new Quaternionf();
        return new Quaternionf(current)
            .premul(new Quaternionf().rotateY(-yawDelta*DEG_TO_RAD))
            .normalize();
    }

    /** Canonical vanilla yaw/pitch carrying the same forward vector; roll intentionally remains camera-only. */
    public static VanillaLook forwardEquivalent(Quaternionfc rotation,float fallbackYaw){
        if(rotation==null||!Float.isFinite(fallbackYaw))return new VanillaLook(0.0F,0.0F);
        Vector3f forward=new Quaternionf(rotation).normalize().transform(new Vector3f(0,0,-1));
        if(!Float.isFinite(forward.x+forward.y+forward.z))return new VanillaLook(0.0F,0.0F);
        float y=Mth.clamp(forward.y,-1.0F,1.0F);
        float pitch=-(float)Math.asin(y)*RAD_TO_DEG;
        float horizontal=(float)Math.hypot(forward.x,forward.z);
        float yaw=horizontal<POLE_EPS?Mth.wrapDegrees(fallbackYaw):Mth.wrapDegrees((float)Math.atan2(-forward.x,forward.z)*RAD_TO_DEG);
        return new VanillaLook(yaw,pitch);
    }

    public static Vec3 forward(Quaternionfc rotation){
        if(rotation==null)return Vec3.ZERO;
        Vector3f v=new Quaternionf(rotation).normalize().transform(new Vector3f(0,0,-1));
        return new Vec3(v.x,v.y,v.z);
    }

    /** Legacy helper retained for compatibility with old test/document vocabulary. */
    public static float normalizationShift(float pitch){
        if(!Float.isFinite(pitch))return 0.0F;
        return 360.0F*(float)Math.floor((pitch+180.0F)/360.0F);
    }

    /** Legacy Euler canonicalization helper; production camera ownership now uses forwardEquivalent. */
    public static VanillaLook vanillaEquivalent(float yaw,float pitch){
        if(!Float.isFinite(yaw)||!Float.isFinite(pitch))return new VanillaLook(0.0F,0.0F);
        float normalized=pitch-normalizationShift(pitch);
        float mappedYaw=yaw;
        if(normalized>90.0F){normalized=180.0F-normalized;mappedYaw+=180.0F;}
        else if(normalized<-90.0F){normalized=-180.0F-normalized;mappedYaw+=180.0F;}
        return new VanillaLook(Mth.wrapDegrees(mappedYaw),normalized);
    }
}
