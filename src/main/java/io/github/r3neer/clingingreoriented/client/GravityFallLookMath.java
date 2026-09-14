package io.github.r3neer.clingingreoriented.client;

/** Pure helpers for continuous full-sphere camera pitch during Gravity Fall. */
public final class GravityFallLookMath {
    private GravityFallLookMath() {}

    /**
     * Returns the multiple of 360 degrees to subtract from both current and previous pitch so
     * current pitch stays in [-180, 180) without changing the rendered orientation or interpolation delta.
     */
    public static float normalizationShift(float pitch){
        if(!Float.isFinite(pitch))return 0.0F;
        return 360.0F*(float)Math.floor((pitch+180.0F)/360.0F);
    }
}
