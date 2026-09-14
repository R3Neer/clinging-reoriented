package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
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

    @Test void thirdPersonStillPivotsAtBodyCentre(){
        assertEquals(0.9F,BodyRenderMath.bodyCenterPivot(1.8F),1.0E-6F);
        assertEquals(0.0F,BodyRenderMath.bodyCenterPivot(Float.NaN),0.0F);
        assertEquals(0.0F,BodyRenderMath.bodyCenterPivot(-2.0F),0.0F);
    }

    @Test void firstPersonCameraPivotExactlyCancelsRenderedTranslationInVisualFrame(){
        Quaternionf visual=new Quaternionf().rotateZ((float)(Math.PI/2.0)).rotateY(.31F).normalize();
        Vec3 translated=new Vec3(.23D,-.11D,.37D);
        Vec3 pivot=BodyRenderMath.localCameraPivot(translated.x,translated.y,translated.z,visual);
        Vector3f world=new Quaternionf(visual).transform(new Vector3f((float)pivot.x,(float)pivot.y,(float)pivot.z));
        Vec3 residual=translated.add(world.x,world.y,world.z);
        assertTrue(residual.length()<2.0E-6D,"camera anchor residual="+residual+" pivot="+pivot);
    }

    @Test void firstPersonDisplayPivotStaysBetweenBodyCentreAndExactCamera(){
        Vec3 camera=new Vec3(-.25D,1.62D,.4D);
        Vec3 centre=new Vec3(0.0D,.9D,0.0D);
        Vec3 pivot=BodyRenderMath.firstPersonPivot(camera,1.8F);
        Vec3 expected=centre.add(camera.subtract(centre).scale(BodyRenderMath.FIRST_PERSON_CAMERA_PIVOT_BLEND));
        assertTrue(pivot.distanceTo(expected)<1.0E-6D,"unexpected blended pivot "+pivot);
        assertTrue(pivot.distanceTo(camera)>1.0E-3D,"blend collapsed to exact camera and would hide the body again");
        assertTrue(pivot.distanceTo(centre)>1.0E-3D,"blend collapsed to body centre and would reintroduce clipping");
    }

    @Test void identityVisualPivotIsJustVectorBackToCamera(){
        Vec3 pivot=BodyRenderMath.localCameraPivot(.25D,.0D,-.4D,new Quaternionf());
        assertTrue(pivot.distanceTo(new Vec3(-.25D,.0D,.4D))<1.0E-6D,"unexpected identity pivot "+pivot);
    }

    @Test void nonFiniteCameraTranslationFailsClosed(){
        assertEquals(Vec3.ZERO,BodyRenderMath.localCameraPivot(Double.NaN,0,0,new Quaternionf()));
    }

    private static void assertEquivalent(Quaternionf expected,Quaternionf actual){
        Quaternionf a=new Quaternionf(expected).normalize(),b=new Quaternionf(actual).normalize();
        assertTrue(Math.abs(Math.abs(a.dot(b))-1.0F)<1.0E-5F,"expected equivalent quaternion "+a+" but got "+b);
    }
}
