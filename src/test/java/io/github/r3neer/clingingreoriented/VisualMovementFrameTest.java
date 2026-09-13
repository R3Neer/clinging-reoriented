package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class VisualMovementFrameTest {
    private static final double EPS=3.0E-5D;

    @Test void canonicalDownCameraUsesScreenForwardAndRight(){
        var basis=VisualMovementFrame.basis(new Quaternionf(),Direction.DOWN,null);
        assertVec(new Vec3(0,0,-1),basis.forward(),"DOWN forward");
        assertVec(new Vec3(1,0,0),basis.right(),"DOWN right");
    }

    @Test void lookingStraightIntoNorthFloorUsesProjectedScreenRightWithoutNoise(){
        // Identity camera looks world NORTH (-Z), exactly parallel to NORTH gravity.
        // Forward projection is therefore degenerate; screen-right (+X) is still tangent,
        // and reconstructing screen-up/forward from it yields world UP (+Y) along the wall.
        var basis=VisualMovementFrame.basis(new Quaternionf(),Direction.NORTH,new Vec3(0,-1,0));
        assertVec(new Vec3(0,1,0),basis.forward(),"degenerate NORTH forward");
        assertVec(new Vec3(1,0,0),basis.right(),"degenerate NORTH right");
        assertEquals(0.0D,basis.forward().dot(GravityTransition.direction(Direction.NORTH)),EPS);
    }

    @Test void remapIsRigidAndPreservesGravityAxisComponent(){
        Direction gravity=Direction.DOWN;
        Quaternionf camera=new Quaternionf().rotateY((float)(Math.PI/2.0));
        Vec3 sourceForward=new Vec3(0,0,1);
        Vec3 movement=new Vec3(.6D,.25D,.8D);
        var result=VisualMovementFrame.remap(movement,sourceForward,camera,gravity,null);

        assertEquals(movement.length(),result.movement().length(),EPS,"rigid remap changed total movement magnitude");
        Vec3 down=GravityTransition.direction(gravity);
        assertEquals(movement.dot(down),result.movement().dot(down),EPS,"rigid remap changed gravity-axis input");
        assertEquals(0.0D,result.forward().dot(down),EPS,"desired forward left movement plane");
    }

    @Test void diagonalTangentInputKeepsLengthAndRelativeAngle(){
        Direction gravity=Direction.EAST;
        Quaternionf camera=new Quaternionf().rotateXYZ(.28F,-.73F,.19F).normalize();
        Vec3 down=GravityTransition.direction(gravity);
        Vec3 up=down.scale(-1.0D);
        Vec3 sourceForward=new Vec3(0,0,-1);
        Vec3 sourceRight=sourceForward.cross(up).normalize();
        Vec3 movement=sourceForward.scale(.8D).add(sourceRight.scale(.6D));

        var result=VisualMovementFrame.remap(movement,sourceForward,camera,gravity,new Vec3(0,1,0));
        assertEquals(1.0D,movement.length(),EPS);
        assertEquals(movement.length(),result.movement().length(),EPS,"diagonal normalization changed");
        assertEquals(0.0D,result.movement().dot(down),EPS,"tangent diagonal gained gravity-axis component");

        var basis=VisualMovementFrame.basis(camera,gravity,new Vec3(0,1,0));
        Vec3 expected=basis.forward().scale(.8D).add(basis.right().scale(.6D));
        assertVec(expected,result.movement(),"diagonal visual basis");
    }

    @Test void rememberedHeadingIsUsedOnlyWhenBothCameraAxesDegenerate(){
        // A valid quaternion cannot make both screen axes parallel to one cardinal gravity,
        // but malformed/degenerate fallback still must be deterministic and finite. Supplying
        // a remembered tangent gives the final guard rail used by the local-player mixin.
        Vec3 remembered=new Vec3(0,0,1);
        Vec3 projected=VisualMovementFrame.projectPlane(remembered,GravityTransition.direction(Direction.DOWN));
        assertTrue(projected.lengthSqr()>VisualMovementFrame.PROJECTION_EPSILON_SQR);
        assertVec(remembered,projected.normalize(),"remembered tangent projection");
    }

    @Test void remappingZeroMovementDoesNotCreateAirSteering(){
        var result=VisualMovementFrame.remap(Vec3.ZERO,new Vec3(0,0,1),new Quaternionf(),Direction.DOWN,null);
        assertVec(Vec3.ZERO,result.movement(),"zero input");
    }

    private static void assertVec(Vec3 expected,Vec3 actual,String label){
        assertEquals(expected.x,actual.x,EPS,label+" x");
        assertEquals(expected.y,actual.y,EPS,label+" y");
        assertEquals(expected.z,actual.z,EPS,label+" z");
    }
}
