package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class GravityFallAerodynamicsTest {
    private static final double EPS=1.0E-6D;

    @Test void cameraInsideNeckConeDoesNotMoveMacroBody(){
        var body=BodyOrientation.start(new Quaternionf(),new Vec3(0,-1,0));
        Vec3 facing=GravityFallAerodynamics.bodyFacing(body.orientation(),0.0F);
        var followed=GravityFallAerodynamics.followLook(body,facing,0.0F);
        assertTrue(Math.abs(Math.abs(body.orientation().dot(followed.orientation()))-1.0F)<1.0E-6F);
    }

    @Test void velocityOnlyWeaklyStabilizesInsteadOfOwningBodyFrame(){
        var body=BodyOrientation.start(new Quaternionf(),new Vec3(0,-1,0));
        var stabilized=GravityFallAerodynamics.stabilize(body,new Vec3(1,0,0));
        Vec3 before=BodyOrientation.bodyUp(body.orientation()),after=BodyOrientation.bodyUp(stabilized.orientation());
        double moved=Math.acos(clamp(before.dot(after)));
        assertEquals(GravityFallAerodynamics.MAX_STABILIZE_RADIANS_PER_TICK,moved,2.0E-4D);
        assertTrue(after.distanceTo(new Vec3(1,0,0))>1.0D,"one velocity sample must not transport the whole body frame");
    }

    @Test void largeLookOffsetOutvotesPassiveStabilization(){
        Vec3 velocity=new Vec3(0,-2,0);var body=BodyOrientation.start(new Quaternionf(),velocity);
        Vec3 facing=GravityFallAerodynamics.bodyFacing(body.orientation(),0.0F);
        Vec3 look=facing.add(BodyOrientation.bodyUp(body.orientation())).normalize();
        var advanced=GravityFallAerodynamics.advanceBody(body,velocity,look,0.0F);
        double before=Math.acos(clamp(facing.dot(look)));
        double after=Math.acos(clamp(GravityFallAerodynamics.bodyFacing(advanced.orientation(),0.0F).dot(look)));
        assertTrue(after<before,"look intent did not pull macro body toward gaze");
    }

    @Test void anisotropicDragBendsMomentumTowardVisibleBodyWithoutAddingEnergy(){
        var body=BodyOrientation.start(new Quaternionf(),new Vec3(0,-1,0));
        Vec3 velocity=new Vec3(0,-2,1);
        Vec3 next=GravityFallAerodynamics.applyAerodynamics(body,velocity);
        assertEquals(-2.0D,next.y,EPS,"longitudinal component should be retained");
        assertEquals(1.0D-GravityFallAerodynamics.EXTRA_TRANSVERSE_DRAG,next.z,EPS,"transverse component did not receive extra drag");
        assertTrue(next.length()<velocity.length(),"aerodynamics created energy");
        assertTrue(Math.abs(next.normalize().y)>Math.abs(velocity.normalize().y),"trajectory did not bend toward body axis");
    }

    @Test void alignedHeadOrFeetFirstMotionIsSymmetricAndUntouched(){
        var down=BodyOrientation.start(new Quaternionf(),new Vec3(0,-1,0));
        assertVec(new Vec3(0,-3,0),GravityFallAerodynamics.applyAerodynamics(down,new Vec3(0,-3,0)));
        assertVec(new Vec3(0,3,0),GravityFallAerodynamics.applyAerodynamics(down,new Vec3(0,3,0)));
    }

    @Test void broadsidePostureActsAsAirBrakeWithoutInventingDirection(){
        var body=BodyOrientation.start(new Quaternionf(),new Vec3(0,-1,0));
        Vec3 broadside=new Vec3(2,0,0);
        Vec3 next=GravityFallAerodynamics.applyAerodynamics(body,broadside);
        assertEquals(2.0D*(1.0D-GravityFallAerodynamics.EXTRA_TRANSVERSE_DRAG),next.x,EPS);
        assertEquals(0.0D,next.y,EPS);assertEquals(0.0D,next.z,EPS);
    }

    private static double clamp(double value){return Math.max(-1.0D,Math.min(1.0D,value));}
    private static void assertVec(Vec3 expected,Vec3 actual){assertTrue(expected.distanceTo(actual)<EPS,"expected "+expected+" but got "+actual);}
}
