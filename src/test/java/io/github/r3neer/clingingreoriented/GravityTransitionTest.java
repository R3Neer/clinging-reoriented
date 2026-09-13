package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class GravityTransitionTest {
    private static final double EPS = 2.0E-4;

    @Test
    void allDirectionPairsUseOnlyPhysicalGravityAngle() {
        for (Direction from : Direction.values()) for (Direction to : Direction.values()) {
            if (from == to) continue;
            for (float yaw : new float[]{0.0F, 37.0F, -91.0F, 179.0F}) {
                Vec3 heading=GravityTransition.headingFromYaw(from,yaw);
                var plan = GravityTransition.plan(from, to, heading);
                double gravityDot = GravityTransition.direction(from).dot(GravityTransition.direction(to));
                double expected = gravityDot < -0.5D ? Math.PI : Math.PI * 0.5D;
                assertEquals(expected, quaternionAngle(
                    GravityTransition.compensatedVisualStart(RotationUtil.getEntityRotationQuaternion(from), plan.yawDelta()),
                    RotationUtil.getEntityRotationQuaternion(to)
                ), 3.0E-4, from + " -> " + to + " yaw=" + yaw);
                assertEquals(expected == Math.PI ? GravityTransition.TurnKind.HALF : GravityTransition.TurnKind.QUARTER, plan.kind());
            }
        }
    }

    @Test
    void headingDeltaTransportsEveryPitchWithoutChangingPitchGauge() {
        for (Direction from : Direction.values()) for (Direction to : Direction.values()) {
            if (from == to) continue;
            for (float yaw : new float[]{0.0F, 45.0F, -120.0F}) {
                Vec3 heading=GravityTransition.headingFromYaw(from,yaw);
                var plan=GravityTransition.plan(from,to,heading);
                double angle=plan.kind()==GravityTransition.TurnKind.HALF?Math.PI:Math.PI*0.5D;
                for (float pitch : new float[]{0.0F,35.0F,-35.0F,89.0F,-89.0F}) {
                    Vec3 oldWorld=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(yaw,pitch),from).normalize();
                    Vec3 expected=GravityTransition.rotate(oldWorld,plan.axis(),angle).normalize();
                    float newYaw=Mth.wrapDegrees(yaw+plan.yawDelta());
                    Vec3 actual=RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(newYaw,pitch),to).normalize();
                    assertVecEquals(expected,actual,EPS,"pitch transport "+from+" -> "+to+" yaw="+yaw+" pitch="+pitch);
                }
            }
        }
    }

    @Test
    void rebaseKeepsSharedPhysicalAxisButUsesEntityHeading() {
        var riderQuarter=GravityTransition.plan(Direction.DOWN,Direction.NORTH,new Vec3(1,0,0));
        var mountQuarter=GravityTransition.rebase(riderQuarter,new Vec3(0,0,-1));
        assertEquals(riderQuarter.previous(),mountQuarter.previous());
        assertEquals(riderQuarter.target(),mountQuarter.target());
        assertEquals(riderQuarter.kind(),mountQuarter.kind());
        assertVecEquals(riderQuarter.axis(),mountQuarter.axis(),EPS,"quarter turn axis is shared");
        assertVecEquals(new Vec3(0,0,-1),mountQuarter.oldWorldHeading(),EPS,"mount keeps its own source heading");
        assertVecEquals(GravityTransition.rotate(new Vec3(0,0,-1),riderQuarter.axis(),Math.PI*.5D).normalize(),mountQuarter.transportedWorldHeading(),EPS,"mount heading follows rider physical quarter turn");

        var riderHalf=GravityTransition.plan(Direction.DOWN,Direction.UP,new Vec3(1,0,0));
        var mountHalf=GravityTransition.rebase(riderHalf,new Vec3(0,0,-1));
        assertEquals(GravityTransition.TurnKind.HALF,mountHalf.kind());
        assertVecEquals(riderHalf.axis(),mountHalf.axis(),EPS,"half turn axis is chosen once by rider");
        assertVecEquals(GravityTransition.rotate(new Vec3(0,0,-1),riderHalf.axis(),Math.PI).normalize(),mountHalf.transportedWorldHeading(),EPS,"mount heading rotates around rider-selected half-turn axis");
        assertNotEquals(riderHalf.yawDelta(),mountHalf.yawDelta(),1.0E-4F,"different headings may need different yaw gauges");
    }

    @Test
    void rebaseProjectsHeadingOntoOldGravityPlane() {
        var physical=GravityTransition.plan(Direction.EAST,Direction.WEST,new Vec3(0,0,-1));
        var rebased=GravityTransition.rebase(physical,new Vec3(7,2,-3));
        assertEquals(0.0D,rebased.oldWorldHeading().x,EPS);
        assertEquals(1.0D,rebased.oldWorldHeading().length(),EPS);
        assertThrows(IllegalArgumentException.class,()->GravityTransition.rebase(physical,new Vec3(1,0,0)));
        assertThrows(IllegalArgumentException.class,()->GravityTransition.rebase(physical,new Vec3(Double.NaN,0,0)));
    }

    @Test
    void compensatedStartPreservesCompositeWorldLook() {
        for (Direction from : Direction.values()) for (Direction to : Direction.values()) {
            if (from == to) continue;
            for (float yaw : new float[]{0.0F,45.0F,-120.0F}) for (float pitch : new float[]{0.0F,35.0F,-35.0F}) {
                var plan=GravityTransition.plan(from,to,GravityTransition.headingFromYaw(from,yaw));
                float newYaw=Mth.wrapDegrees(yaw+plan.yawDelta());
                Vec3 oldLocal=RotationUtil.rotToVec(yaw,pitch);
                Vec3 newLocal=RotationUtil.rotToVec(newYaw,pitch);
                Quaternionf oldFrame=RotationUtil.getEntityRotationQuaternion(from);
                Quaternionf startFrame=GravityTransition.compensatedVisualStart(oldFrame,plan.yawDelta());
                assertVecEquals(transform(oldFrame,oldLocal),transform(startFrame,newLocal),EPS,"composite start "+from+" -> "+to);
            }
        }
    }

    @Test
    void interruptionCompensationIsContinuousForArbitraryDisplayedFrame() {
        Quaternionf displayed=new Quaternionf().rotateXYZ(0.31F,-0.77F,0.43F).normalize();
        for(float yaw:new float[]{-140.0F,-25.0F,63.0F,179.0F})for(float pitch:new float[]{-50.0F,0.0F,41.0F})for(float delta:new float[]{-180.0F,-90.0F,37.0F,90.0F,180.0F}){
            Vec3 oldLocal=RotationUtil.rotToVec(yaw,pitch);
            Vec3 newLocal=RotationUtil.rotToVec(Mth.wrapDegrees(yaw+delta),pitch);
            Quaternionf compensated=GravityTransition.compensatedVisualStart(displayed,delta);
            assertVecEquals(transform(displayed,oldLocal),transform(compensated,newLocal),EPS,"interrupted continuity");
        }
    }

    @Test
    void oppositeTurnsPreserveWorldNavigationHeading() {
        Direction[][] opposites={{Direction.DOWN,Direction.UP},{Direction.UP,Direction.DOWN},{Direction.NORTH,Direction.SOUTH},{Direction.SOUTH,Direction.NORTH},{Direction.EAST,Direction.WEST},{Direction.WEST,Direction.EAST}};
        for(var pair:opposites)for(float yaw:new float[]{0.0F,90.0F,-90.0F,180.0F,37.0F}){
            Vec3 heading=GravityTransition.headingFromYaw(pair[0],yaw);
            var plan=GravityTransition.plan(pair[0],pair[1],heading);
            assertVecEquals(heading,plan.axis(),EPS,"opposite axis follows heading");
            assertVecEquals(heading,plan.transportedWorldHeading(),EPS,"opposite heading remains world-stable");
        }
    }

    @Test
    void downToNorthTransportsNorthHeadingToWorldUp() {
        Vec3 north=new Vec3(0,0,-1);
        var plan=GravityTransition.plan(Direction.DOWN,Direction.NORTH,north);
        assertVecEquals(new Vec3(0,1,0),plan.transportedWorldHeading(),EPS,"front wall becomes forward/up along wall");
    }

    @Test
    void sanitizeHeadingProjectsOffPlaneAndFallsBackWhenDegenerate() {
        Vec3 projected=GravityTransition.sanitizeHeading(Direction.DOWN,new Vec3(2,7,-3),0);
        assertEquals(0.0,projected.y,EPS);
        assertEquals(1.0,projected.length(),EPS);
        Vec3 fallback=GravityTransition.sanitizeHeading(Direction.DOWN,new Vec3(0,1,0),-90);
        assertVecEquals(new Vec3(1,0,0),fallback,EPS,"vertical malformed heading falls back to yaw");
        Vec3 nan=GravityTransition.sanitizeHeading(Direction.DOWN,new Vec3(Double.NaN,0,0),0);
        assertVecEquals(new Vec3(0,0,1),nan,EPS,"non-finite heading falls back to yaw");
    }

    @Test
    void easeOutQuadraticIsReadableSnapAndMonotonic() {
        assertEquals(0.0F,GravityTransition.easeOutQuadratic(0.0F),1.0E-6F);
        assertEquals(0.75F,GravityTransition.easeOutQuadratic(0.5F),1.0E-6F);
        assertEquals(1.0F,GravityTransition.easeOutQuadratic(1.0F),1.0E-6F);
        assertEquals(180_000_000L,GravityTransition.QUARTER_TURN_NANOS);
        assertEquals(240_000_000L,GravityTransition.HALF_TURN_NANOS);
        float previous=-1.0F;
        for(int i=0;i<=100;i++){
            float value=GravityTransition.easeOutQuadratic(i/100.0F);
            assertTrue(value>=previous);
            previous=value;
        }
    }

    private static Vec3 transform(Quaternionf quaternion, Vec3 vector) {
        Vector3f result=new Quaternionf(quaternion).transform(new Vector3f((float)vector.x,(float)vector.y,(float)vector.z));
        return new Vec3(result.x,result.y,result.z);
    }
    private static double quaternionAngle(Quaternionf a,Quaternionf b){
        float dot=Math.abs(new Quaternionf(a).normalize().dot(new Quaternionf(b).normalize()));
        dot=Math.max(-1.0F,Math.min(1.0F,dot));return 2.0D*Math.acos(dot);
    }
    private static void assertVecEquals(Vec3 expected,Vec3 actual,double epsilon,String message){
        assertEquals(expected.x,actual.x,epsilon,message+" x");assertEquals(expected.y,actual.y,epsilon,message+" y");assertEquals(expected.z,actual.z,epsilon,message+" z");
    }
}
