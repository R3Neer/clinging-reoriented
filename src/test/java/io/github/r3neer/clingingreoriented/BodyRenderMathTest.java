package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class BodyRenderMathTest {
    @Test void extraRootCancelsVisualFrameAndProducesAbsoluteBodyFrame(){
        Quaternionf visual=new Quaternionf().rotateY(.71f).rotateX(-.43f).normalize();
        Quaternionf body=new Quaternionf().rotateZ(1.17f).rotateY(-.28f).normalize();
        Quaternionf extra=BodyRenderMath.extraRoot(visual,body);
        assertEquivalent(body,BodyRenderMath.composed(visual,extra));
    }

    @Test void identityBodyExactlyCancelsNonCanonicalVisualGravity(){
        Quaternionf visual=new Quaternionf().rotateX((float)(Math.PI/2.0)).normalize();
        Quaternionf body=new Quaternionf();
        assertEquivalent(body,BodyRenderMath.composed(visual,BodyRenderMath.extraRoot(visual,body)));
    }

    @Test void identicalVisualAndBodyNeedNoExtraRoot(){
        Quaternionf visual=new Quaternionf().rotateY(.3f).rotateZ(-.9f).normalize();
        Quaternionf extra=BodyRenderMath.extraRoot(visual,visual);
        assertEquivalent(new Quaternionf(),extra);
    }

    @Test void firstPersonPivotsAtEyeWhileWorldRenderingPivotsAtBodyCentre(){
        assertEquals(1.62F,BodyRenderMath.pivotHeight(1.8F,1.62F,true),1.0E-6F);
        assertEquals(0.9F,BodyRenderMath.pivotHeight(1.8F,1.62F,false),1.0E-6F);
    }

    @Test void invalidPivotInputsFailClosedToFiniteNonNegativeValues(){
        assertEquals(0.0F,BodyRenderMath.pivotHeight(Float.NaN,Float.NaN,true),0.0F);
        assertEquals(0.0F,BodyRenderMath.pivotHeight(-2.0F,-1.0F,false),0.0F);
    }

    private static void assertEquivalent(Quaternionf expected,Quaternionf actual){
        Quaternionf a=new Quaternionf(expected).normalize(),b=new Quaternionf(actual).normalize();
        assertTrue(Math.abs(Math.abs(a.dot(b))-1.0F)<1.0E-5F,"expected equivalent quaternion "+a+" but got "+b);
    }
}
