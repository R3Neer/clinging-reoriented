package io.github.r3neer.clingingreoriented.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class GravityFallLookMathTest {
    private static final double EPS=1.0E-5D;

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
        assertEquals(180.0F,Math.abs(downPastPole.yaw()),1.0E-6F);assertEquals(60.0F,downPastPole.pitch(),1.0E-6F);
        var upPastPole=GravityFallLookMath.vanillaEquivalent(35.0F,-120.0F);
        assertEquals(-145.0F,upPastPole.yaw(),1.0E-6F);assertEquals(-60.0F,upPastPole.pitch(),1.0E-6F);
    }

    @Test void screenHorizontalKeepsHandednessAcrossBothPoles(){
        assertHandedness(80.0F,100.0F);
        assertHandedness(-80.0F,-100.0F);
        assertHandedness(170.0F,-170.0F);
    }

    @Test void diagonalInputKeepsBothScreenAxesAcrossPoles(){
        assertDiagonal(80.0F,100.0F);
        assertDiagonal(-80.0F,-100.0F);
    }

    @Test void verticalLoopReturnsFullCameraFrame(){
        Quaternionf start=GravityFallLookMath.vanillaRotation(0,0);
        Quaternionf loop=new Quaternionf(start);
        for(int i=0;i<3;i++)loop=GravityFallLookMath.screenTurn(loop,0,120);
        assertQuat(start,loop,"360 vertical loop");
    }

    @Test void forwardGaugeStaysVanillaCompatiblePastPole(){
        Quaternionf q=GravityFallLookMath.screenTurn(GravityFallLookMath.vanillaRotation(0,0),0,120);
        var look=GravityFallLookMath.forwardEquivalent(q,0);
        assertTrue(Math.abs(look.pitch())<=90.0F);
        Vec3 fromQ=GravityFallLookMath.forward(q);
        Vec3 fromGauge=GravityFallLookMath.forward(GravityFallLookMath.vanillaRotation(look.yaw(),look.pitch()));
        assertVec(fromQ,fromGauge,"canonical forward gauge");
    }

    @Test void yawGaugeRebasePreservesWorldComposition(){
        Quaternionf visual=new Quaternionf().rotateZ(.47F).rotateX(-.19F);
        Quaternionf base=GravityFallLookMath.vanillaRotation(35,70);
        Quaternionf before=new Quaternionf(visual).mul(base).normalize();
        float delta=90.0F;
        Quaternionf compensatedVisual=new Quaternionf(visual).mul(new Quaternionf().rotateY((float)Math.toRadians(delta)));
        Quaternionf rebased=GravityFallLookMath.rebaseYaw(base,delta);
        Quaternionf after=compensatedVisual.mul(rebased).normalize();
        assertQuat(before,after,"yaw-gauge compensation");
    }

    @Test void nonFiniteInputFailsClosed(){
        assertEquals(0.0F,GravityFallLookMath.normalizationShift(Float.NaN),0.0F);
        assertEquals(0.0F,GravityFallLookMath.vanillaEquivalent(Float.NaN,20.0F).pitch(),0.0F);
    }

    private static void assertHandedness(float canonicalPitch,float pastPitch){
        double canonical=response(canonicalPitch),past=response(pastPitch);
        assertTrue(Math.abs(canonical)>EPS&&Math.abs(past)>EPS);
        assertEquals(Math.signum(canonical),Math.signum(past),0.0D,"horizontal handedness changed between "+canonicalPitch+" and "+pastPitch);
    }

    private static void assertDiagonal(float canonicalPitch,float pastPitch){
        double[] canonical=diagonalResponse(canonicalPitch),past=diagonalResponse(pastPitch);
        assertTrue(Math.abs(canonical[0])>EPS&&Math.abs(canonical[1])>EPS&&Math.abs(past[0])>EPS&&Math.abs(past[1])>EPS);
        assertEquals(Math.signum(canonical[0]),Math.signum(past[0]),0.0D,"diagonal horizontal axis inverted across pole");
        assertEquals(Math.signum(canonical[1]),Math.signum(past[1]),0.0D,"diagonal vertical axis inverted across pole");
    }

    private static double response(float pitch){return diagonalResponse(pitch)[0];}
    private static double[] diagonalResponse(float pitch){
        Quaternionf q=GravityFallLookMath.vanillaRotation(0,pitch);
        Vec3 before=GravityFallLookMath.forward(q);
        Vector3f r=new Quaternionf(q).transform(new Vector3f(1,0,0));Vec3 right=new Vec3(r.x,r.y,r.z);
        Vector3f u=new Quaternionf(q).transform(new Vector3f(0,1,0));Vec3 up=new Vec3(u.x,u.y,u.z);
        Vec3 after=GravityFallLookMath.forward(GravityFallLookMath.screenTurn(q,6,6));Vec3 delta=after.subtract(before);
        return new double[]{delta.dot(right),delta.dot(up)};
    }

    private static void assertQuat(Quaternionf a,Quaternionf b,String label){float dot=Math.abs(new Quaternionf(a).normalize().dot(new Quaternionf(b).normalize()));assertTrue(1.0F-dot<1.0E-5F,label+" dot="+dot);}
    private static void assertVec(Vec3 a,Vec3 b,String label){assertTrue(a.distanceTo(b)<EPS,label+": "+a+" vs "+b);}
}
