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
        Vec3 look=BodyOrientation.bodyUp(body.orientation());
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

    @Test void forwardAirDiveRedirectsExistingMomentumWithoutAddingSpeed(){
        Vec3 velocity=new Vec3(0.0D,-3.0D,0.0D);
        Vec3 look=new Vec3(0.0D,-1.0D,1.0D).normalize();
        Vec3 steered=GravityFallAerodynamics.redirectMomentum(velocity,look,1.0D);
        assertEquals(velocity.length(),steered.length(),1.0E-10D,"air dive created or destroyed speed before drag");
        assertTrue(steered.z>0.0D,"forward gaze did not bend fall momentum toward +Z");
        assertTrue(steered.y<0.0D,"one steering tick unrealistically removed the falling component");
    }

    @Test void steeringAuthorityIsDotProductGated(){
        Vec3 velocity=new Vec3(0.0D,-3.0D,0.0D);
        Vec3 perpendicular=new Vec3(1.0D,0.0D,0.0D);
        Vec3 backward=new Vec3(0.0D,1.0D,0.0D);
        assertEquals(velocity,GravityFallAerodynamics.redirectMomentum(velocity,perpendicular,1.0D),
            "perpendicular gaze stole fall momentum for a free sidestep");
        assertEquals(velocity,GravityFallAerodynamics.redirectMomentum(velocity,backward,1.0D),
            "backward gaze reversed fall momentum for free");
        assertEquals(velocity,GravityFallAerodynamics.redirectMomentum(velocity,new Vec3(0,-1,1),0.0D),
            "air dive steered without W input");
    }

    @Test void strongerAlignmentProducesStrongerTurn(){
        Vec3 velocity=new Vec3(0.0D,-3.0D,0.0D);
        Vec3 mostlyAligned=new Vec3(0.0D,-1.0D,0.5D).normalize();
        Vec3 weaklyAligned=new Vec3(0.0D,-0.2D,1.0D).normalize();
        Vec3 a=GravityFallAerodynamics.redirectMomentum(velocity,mostlyAligned,1.0D);
        Vec3 b=GravityFallAerodynamics.redirectMomentum(velocity,weaklyAligned,1.0D);
        assertTrue(a.z>b.z,"larger velocity/look dot product did not yield stronger redirection");
    }
}
