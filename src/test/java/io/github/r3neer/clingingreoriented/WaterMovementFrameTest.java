package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.client.GravityFallLookMath;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class WaterMovementFrameTest {
    private static final double EPS=4.0E-5D;

    @Test void eastGravityForwardIsReexpressedAsCameraForward(){
        Direction gravity=Direction.EAST;
        Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(0.0F,0.0F),gravity).normalize();
        Vec3 physical=physicalForward.scale(.18D);
        Quaternionf camera=GravityFallLookMath.vanillaRotation(0.0F,0.0F);
        Vec3 expected=WaterMovementFrame.cameraBasis(camera).forward().scale(.18D);
        assertVec(expected,WaterMovementFrame.remap(physical,physicalForward,camera,gravity),"EAST forward");
    }

    @Test void cameraPitchSurvivesInsteadOfBeingProjectedOntoGravityPlane(){
        Direction gravity=Direction.EAST;
        Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(0.0F,0.0F),gravity).normalize();
        Quaternionf camera=GravityFallLookMath.vanillaRotation(0.0F,45.0F);
        Vec3 result=WaterMovementFrame.remap(physicalForward.scale(.2D),physicalForward,camera,gravity);
        Vec3 expected=WaterMovementFrame.cameraBasis(camera).forward().scale(.2D);
        assertVec(expected,result,"pitched camera forward");
        assertEquals(expected.y,result.y,EPS,"W lost camera pitch");
    }

    @Test void strafeUsesScreenLeftEvenWhenPhysicalGravityIsSideways(){
        Direction gravity=Direction.EAST;
        Vec3 down=GravityTransition.direction(gravity),up=down.scale(-1.0D);
        Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(0.0F,0.0F),gravity).normalize();
        Vec3 physicalLeft=up.cross(physicalForward).normalize();
        Quaternionf camera=GravityFallLookMath.vanillaRotation(-90.0F,-20.0F);
        Vec3 expected=WaterMovementFrame.cameraBasis(camera).left().scale(.11D);
        assertVec(expected,WaterMovementFrame.remap(physicalLeft.scale(.11D),physicalForward,camera,gravity),"camera left");
    }

    @Test void diagonalRemapPreservesTangentMagnitude(){
        Direction gravity=Direction.NORTH;
        Vec3 down=GravityTransition.direction(gravity),up=down.scale(-1.0D);
        Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(37.0F,0.0F),gravity).normalize();
        Vec3 physicalLeft=up.cross(physicalForward).normalize();
        Vec3 movement=physicalForward.scale(.8D).add(physicalLeft.scale(.6D));
        Quaternionf camera=GravityFallLookMath.vanillaRotation(122.0F,32.0F);
        Vec3 result=WaterMovementFrame.remap(movement,physicalForward,camera,gravity);
        assertEquals(movement.length(),result.length(),EPS,"camera remap changed normalized WASD magnitude");
    }

    @Test void gravityAxisComponentIsNotSmuggledIntoWasd(){
        Direction gravity=Direction.WEST;
        Vec3 down=GravityTransition.direction(gravity),up=down.scale(-1.0D);
        Vec3 physicalForward=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(0.0F,0.0F),gravity).normalize();
        Vec3 movement=physicalForward.scale(.16D).add(up.scale(.09D));
        Quaternionf camera=GravityFallLookMath.vanillaRotation(0.0F,0.0F);
        Vec3 result=WaterMovementFrame.remap(movement,physicalForward,camera,gravity);
        assertEquals(.16D,result.length(),EPS,"gravity-local vertical leaked into camera WASD; Space/Shift own vertical water motion");
    }

    @Test void zeroInputNeverCreatesSwimmingImpulse(){
        Vec3 result=WaterMovementFrame.remap(Vec3.ZERO,new Vec3(0,0,1),new Quaternionf(),Direction.DOWN);
        assertVec(Vec3.ZERO,result,"zero input");
    }

    private static void assertVec(Vec3 expected,Vec3 actual,String label){
        assertEquals(expected.x,actual.x,EPS,label+" x");
        assertEquals(expected.y,actual.y,EPS,label+" y");
        assertEquals(expected.z,actual.z,EPS,label+" z");
    }
}
