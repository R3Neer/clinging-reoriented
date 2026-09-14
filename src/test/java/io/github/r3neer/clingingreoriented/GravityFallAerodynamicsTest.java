package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class GravityFallAerodynamicsTest {
    @Test void cameraInsideNeckConeDoesNotMoveMacroBody(){
        var body=BodyOrientation.start(new Quaternionf(),new Vec3(0,-1,0));
        Vec3 facing=GravityFallAerodynamics.bodyFacing(body.orientation(),0.0F);
        var followed=GravityFallAerodynamics.followLook(body,facing,0.0F);
        assertTrue(Math.abs(Math.abs(body.orientation().dot(followed.orientation()))-1.0F)<1.0E-6F);
        assertEquals(1.0D,GravityFallAerodynamics.streamlining(followed,new Vec3(0,-2,0)),1.0E-6D);
    }

    @Test void largeLookOffsetPullsBodyGraduallyAndAddsOnlyDrag(){
        Vec3 velocity=new Vec3(0,-2,0);
        var body=BodyOrientation.start(new Quaternionf(),velocity);
        Vec3 look=BodyOrientation.bodyUp(body.orientation()); // 90 degrees away from normal body-facing
        Vec3 beforeFacing=GravityFallAerodynamics.bodyFacing(body.orientation(),0.0F);
        double beforeAngle=Math.acos(Math.max(-1.0D,Math.min(1.0D,beforeFacing.dot(look))));

        var followed=GravityFallAerodynamics.followLook(body,look,0.0F);
        Vec3 afterFacing=GravityFallAerodynamics.bodyFacing(followed.orientation(),0.0F);
        double afterAngle=Math.acos(Math.max(-1.0D,Math.min(1.0D,afterFacing.dot(look))));
        assertEquals(GravityFallAerodynamics.MAX_FOLLOW_RADIANS_PER_TICK,beforeAngle-afterAngle,2.0E-4D);

        double streamline=GravityFallAerodynamics.streamlining(followed,velocity);
        assertTrue(streamline<1.0D&&streamline>0.9D,"one body-follow tick should add mild misalignment: "+streamline);
        Vec3 dragged=GravityFallAerodynamics.applyDrag(velocity,streamline);
        assertTrue(dragged.length()<velocity.length(),"misalignment did not add drag");
        assertTrue(dragged.normalize().distanceTo(velocity.normalize())<1.0E-9D,"drag changed velocity direction");
    }

    @Test void broadsideDragIsBoundedAndStreamlinedFlightIsUntouched(){
        assertEquals(1.0D,GravityFallAerodynamics.dragFactor(1.0D),1.0E-12D);
        assertEquals(1.0D-GravityFallAerodynamics.MAX_EXTRA_DRAG,GravityFallAerodynamics.dragFactor(0.0D),1.0E-12D);
        assertTrue(GravityFallAerodynamics.dragFactor(0.0D)>0.98D,"broadside air brake became implausibly abrupt");
    }
}
