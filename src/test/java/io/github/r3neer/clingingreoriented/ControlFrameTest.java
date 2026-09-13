package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Lower-layer control invariant: GravityTransition's yaw-gauge rebase rigidly transports
 * the physical two-dimensional movement basis into each new gravity plane and preserves
 * the compensated rendered navigation frame. VisualMovementFrameTest separately covers
 * the local-player correction from that physical basis to the actually rendered camera.
 */
final class ControlFrameTest {
    private static final double EPS=3.0E-4D;

    @Test void gravityGaugeTransportsTheWholePhysicalWasdPlaneWithoutChangingMagnitude(){
        for(Direction from:Direction.values())for(Direction to:Direction.values()){
            if(from==to)continue;
            for(float yaw:new float[]{0.0F,37.0F,-91.0F,179.0F}){
                Vec3 oldForward=worldYaw(from,yaw);
                Vec3 oldSide=worldYaw(from,yaw+90.0F);
                var plan=GravityTransition.plan(from,to,oldForward);
                float newYaw=Mth.wrapDegrees(yaw+plan.yawDelta());
                Vec3 newForward=worldYaw(to,newYaw);
                Vec3 newSide=worldYaw(to,newYaw+90.0F);
                double angle=plan.kind()==GravityTransition.TurnKind.HALF?Math.PI:Math.PI*.5D;

                assertVec(GravityTransition.rotate(oldForward,plan.axis(),angle).normalize(),newForward,
                    "forward basis "+from+" -> "+to+" yaw="+yaw);
                assertVec(GravityTransition.rotate(oldSide,plan.axis(),angle).normalize(),newSide,
                    "strafe basis "+from+" -> "+to+" yaw="+yaw);
                assertEquals(1.0D,newForward.length(),EPS);
                assertEquals(1.0D,newSide.length(),EPS);
                assertEquals(0.0D,newForward.dot(newSide),EPS,"WASD basis lost orthogonality");

                Vec3 oldDiagonal=oldForward.scale(.8D).add(oldSide.scale(.6D));
                Vec3 newDiagonal=newForward.scale(.8D).add(newSide.scale(.6D));
                assertEquals(oldDiagonal.length(),newDiagonal.length(),EPS,"diagonal magnitude changed");
                assertVec(GravityTransition.rotate(oldDiagonal,plan.axis(),angle),newDiagonal,
                    "diagonal basis "+from+" -> "+to+" yaw="+yaw);
            }
        }
    }

    @Test void repeatedReorientationTurnsKeepRenderedNavigationFrameContinuousWhilePhysicalPlaneMoves(){
        Direction gravity=Direction.DOWN;
        float yaw=-33.0F;
        Quaternionf visual=RotationUtil.getEntityRotationQuaternion(gravity);
        Direction[] path={Direction.NORTH,Direction.EAST,Direction.UP,Direction.WEST,Direction.SOUTH,Direction.DOWN};

        for(Direction target:path){
            Vec3 renderedBefore=transform(visual,RotationUtil.rotToVec(yaw,0.0F)).normalize();
            Vec3 physicalBefore=worldYaw(gravity,yaw);
            var plan=GravityTransition.plan(gravity,target,physicalBefore);

            float newYaw=Mth.wrapDegrees(yaw+plan.yawDelta());
            Quaternionf newVisual=GravityTransition.compensatedVisualStart(visual,plan.yawDelta());
            Vec3 renderedAfter=transform(newVisual,RotationUtil.rotToVec(newYaw,0.0F)).normalize();
            assertVec(renderedBefore,renderedAfter,"held visual navigation frame changed on "+gravity+" -> "+target);

            Vec3 physicalAfter=worldYaw(target,newYaw);
            assertVec(plan.transportedWorldHeading(),physicalAfter,"physical W basis did not follow transported walking plane");
            assertEquals(0.0D,physicalAfter.dot(GravityTransition.direction(target)),EPS,"W gained a gravity-axis component");

            gravity=target;yaw=newYaw;visual=newVisual;
        }
    }

    @Test void frontWallPhysicalGaugeAlreadyMapsForwardToUpAlongWall(){
        float yaw=180.0F;
        Vec3 heading=worldYaw(Direction.DOWN,yaw);
        var plan=GravityTransition.plan(Direction.DOWN,Direction.NORTH,heading);
        float newYaw=Mth.wrapDegrees(yaw+plan.yawDelta());
        Vec3 movement=worldYaw(Direction.NORTH,newYaw);
        assertVec(new Vec3(0,1,0),movement,"front-wall W should be screen-up along the wall");
        assertEquals(0.0D,movement.dot(GravityTransition.direction(Direction.NORTH)),EPS,"W pushed into the new floor");
    }

    private static Vec3 worldYaw(Direction gravity,float yaw){
        return RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(yaw,0.0F),gravity).normalize();
    }
    private static Vec3 transform(Quaternionf q,Vec3 v){
        Vector3f out=new Quaternionf(q).transform(new Vector3f((float)v.x,(float)v.y,(float)v.z));
        return new Vec3(out.x,out.y,out.z);
    }
    private static void assertVec(Vec3 expected,Vec3 actual,String message){
        assertEquals(expected.x,actual.x,EPS,message+" x");
        assertEquals(expected.y,actual.y,EPS,message+" y");
        assertEquals(expected.z,actual.z,EPS,message+" z");
    }
}
