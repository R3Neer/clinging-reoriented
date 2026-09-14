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
        assertEquals(720.0F,GravityFallLookMath.normalizationShift(540.0F),0.0F);
        assertEquals(-360.0F,GravityFallLookMath.normalizationShift(-180.1F),0.0F);
        assertEquals(-720.0F,GravityFallLookMath.normalizationShift(-540.1F),0.0F);
    }

    @Test void poleCrossingsMapToEquivalentVanillaCoordinates(){
        var downPastPole=GravityFallLookMath.vanillaEquivalent(0.0F,120.0F);
        assertEquals(180.0F,Math.abs(downPastPole.yaw()),1.0E-6F);
        assertEquals(60.0F,downPastPole.pitch(),1.0E-6F);

        var upPastPole=GravityFallLookMath.vanillaEquivalent(35.0F,-120.0F);
        assertEquals(-145.0F,upPastPole.yaw(),1.0E-6F);
        assertEquals(-60.0F,upPastPole.pitch(),1.0E-6F);

        var fullLoop=GravityFallLookMath.vanillaEquivalent(22.0F,360.0F);
        assertEquals(22.0F,fullLoop.yaw(),1.0E-6F);
        assertEquals(0.0F,fullLoop.pitch(),1.0E-6F);
    }

    @Test void nonFinitePitchFailsClosed(){
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(Float.NaN),0.0F);
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(Float.POSITIVE_INFINITY),0.0F);
        assertEquals(0.0F,GravityFallLookMath.vanillaEquivalent(Float.NaN,20.0F).pitch(),0.0F);
    }
}
