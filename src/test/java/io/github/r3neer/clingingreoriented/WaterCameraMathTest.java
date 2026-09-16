package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.r3neer.clingingreoriented.client.GravityFallLookMath;
import io.github.r3neer.clingingreoriented.client.WaterCameraMath;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.junit.jupiter.api.Test;

final class WaterCameraMathTest {
    private static final double EPS=5.0E-5D;

    @Test void rollCorrectionPreservesForwardExactly(){
        Quaternionf base=GravityFallLookMath.vanillaRotation(-73.0F,28.0F);
        Vec3 before=WaterCameraMath.forward(base);
        double roll=WaterCameraMath.targetRoll(base,new Vec3(-1,0,0));
        Quaternionf corrected=WaterCameraMath.applyRoll(base,roll);
        assertVec(before,WaterCameraMath.forward(corrected),"forward changed under water roll correction");
    }

    @Test void freeSwimmingAlignsScreenUpToWorldUpProjection(){
        Quaternionf base=new Quaternionf().rotateXYZ(.31F,.72F,-.44F).normalize();
        Vec3 desired=new Vec3(0,1,0);
        double roll=WaterCameraMath.targetRoll(base,desired);
        Quaternionf corrected=WaterCameraMath.applyRoll(base,roll);
        Vec3 forward=WaterCameraMath.forward(corrected).normalize();
        Vec3 projected=desired.subtract(forward.scale(desired.dot(forward))).normalize();
        assertVec(projected,WaterCameraMath.up(corrected).normalize(),"world-up projection");
    }

    @Test void supportedWallCanBecomeScreenUpWithoutChangingForward(){
        Quaternionf base=GravityFallLookMath.vanillaRotation(0.0F,0.0F);
        Vec3 wallUp=new Vec3(-1,0,0);Vec3 before=WaterCameraMath.forward(base);
        double roll=WaterCameraMath.targetRoll(base,wallUp);
        assertTrue(Double.isFinite(roll),"orthogonal wall-up fixture accidentally entered roll pole");
        Quaternionf corrected=WaterCameraMath.applyRoll(base,roll);
        assertVec(before,WaterCameraMath.forward(corrected),"wall support changed forward");
        assertVec(wallUp,WaterCameraMath.up(corrected),"wall normal did not become screen-up");
    }

    @Test void poleLeavesRollUndefinedInsteadOfInventingAFlip(){
        Quaternionf vertical=GravityFallLookMath.vanillaRotation(0.0F,-90.0F);
        assertTrue(Double.isNaN(WaterCameraMath.targetRoll(vertical,new Vec3(0,1,0))),"world-up parallel to forward should keep existing roll");
    }

    @Test void angularStepUsesShortestArcAndNeverOvershoots(){
        double current=Math.toRadians(179.0D),target=Math.toRadians(-179.0D);
        double next=WaterCameraMath.stepAngle(current,target,Math.toRadians(.5D));
        assertEquals(Math.toRadians(.5D),Math.atan2(Math.sin(next-current),Math.cos(next-current)),EPS,"step took long angular arc");
        double settled=WaterCameraMath.stepAngle(current,target,Math.toRadians(5.0D));
        assertEquals(0.0D,Math.atan2(Math.sin(target-settled),Math.cos(target-settled)),EPS,"large step overshot target");
    }

    private static void assertVec(Vec3 expected,Vec3 actual,String label){assertEquals(expected.x,actual.x,EPS,label+" x");assertEquals(expected.y,actual.y,EPS,label+" y");assertEquals(expected.z,actual.z,EPS,label+" z");}
}
