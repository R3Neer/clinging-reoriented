package io.github.r3neer.clingingreoriented.client;

import net.minecraft.util.Mth;

/** Pure helpers for continuous full-sphere camera pitch during Gravity Fall. */
public final class GravityFallLookMath {
    public record VanillaLook(float yaw,float pitch) {}

    private GravityFallLookMath() {}

    /** Keep the full-sphere pitch numerically local without changing its orientation. */
    public static float normalizationShift(float pitch){
        if(!Float.isFinite(pitch))return 0.0F;
        return 360.0F*(float)Math.floor((pitch+180.0F)/360.0F);
    }

    /** Map a full-sphere yaw/pitch pair to vanilla's +/-90 pitch without changing look direction. */
    public static VanillaLook vanillaEquivalent(float yaw,float pitch){
        if(!Float.isFinite(yaw)||!Float.isFinite(pitch))return new VanillaLook(0.0F,0.0F);
        float normalized=pitch-normalizationShift(pitch);
        float mappedYaw=yaw;
        if(normalized>90.0F){normalized=180.0F-normalized;mappedYaw+=180.0F;}
        else if(normalized<-90.0F){normalized=-180.0F-normalized;mappedYaw+=180.0F;}
        return new VanillaLook(Mth.wrapDegrees(mappedYaw),normalized);
    }
}
