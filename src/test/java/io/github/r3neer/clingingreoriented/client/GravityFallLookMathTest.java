package io.github.r3neer.clingingreoriented.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GravityFallLookMathTest {
    @Test void keepsCanonicalPitchUnshifted(){
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(0.0F),0.0F);
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(179.9F),0.0F);
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(-180.0F),0.0F);
    }

    @Test void wrapsWholeTurnsWithoutChangingLocalDelta(){
        assertEquals(360.0F,GravityFallLookMath.normalizationShift(180.0F),0.0F);
        assertEquals(360.0F,GravityFallLookMath.normalizationShift(540.0F),0.0F);
        assertEquals(-360.0F,GravityFallLookMath.normalizationShift(-180.1F),0.0F);
        assertEquals(-720.0F,GravityFallLookMath.normalizationShift(-540.1F),0.0F);
    }

    @Test void nonFinitePitchFailsClosed(){
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(Float.NaN),0.0F);
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(Float.POSITIVE_INFINITY),0.0F);
    }
}
