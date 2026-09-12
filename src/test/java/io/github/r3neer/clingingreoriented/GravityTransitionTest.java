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
                for (float pitch : new float[]{0.0F, 25.0F, -47.0F, 89.999F, -89.999F}) {
                    var plan = GravityTransition.plan(from, to, yaw, pitch);
                    double gravityDot = GravityTransition.direction(from).dot(GravityTransition.direction(to));
                    double expected = gravityDot < -0.5D ? Math.PI : Math.PI * 0.5D;
                    assertEquals(expected, quaternionAngle(
                        GravityTransition.compensatedVisualStart(RotationUtil.getEntityRotationQuaternion(from), plan.yawDelta()),
                        RotationUtil.getEntityRotationQuaternion(to)
                    ), 3.0E-4, from + " -> " + to + " yaw=" + yaw + " pitch=" + pitch);
                    assertEquals(expected == Math.PI ? GravityTransition.TurnKind.HALF : GravityTransition.TurnKind.QUARTER, plan.kind());
                }
            }
        }
    }

    @Test
    void compensatedStartPreservesCompositeWorldLook() {
        for (Direction from : Direction.values()) for (Direction to : Direction.values()) {
            if (from == to) continue;
            for (float yaw : new float[]{0.0F, 45.0F, -120.0F}) for (float pitch : new float[]{0.0F, 35.0F, -35.0F}) {
                var plan = GravityTransition.plan(from, to, yaw, pitch);
                float newYaw = Mth.wrapDegrees(yaw + plan.yawDelta());
                Vec3 oldLocal = RotationUtil.rotToVec(yaw, pitch);
                Vec3 newLocal = RotationUtil.rotToVec(newYaw, pitch);
                Quaternionf oldFrame = RotationUtil.getEntityRotationQuaternion(from);
                Quaternionf startFrame = GravityTransition.compensatedVisualStart(oldFrame, plan.yawDelta());
                assertVecEquals(transform(oldFrame, oldLocal), transform(startFrame, newLocal), EPS,
                    "composite start " + from + " -> " + to);
            }
        }
    }

    @Test
    void interruptionCompensationIsContinuousForArbitraryDisplayedFrame() {
        Quaternionf displayed = new Quaternionf().rotateXYZ(0.31F, -0.77F, 0.43F).normalize();
        for (float yaw : new float[]{-140.0F, -25.0F, 63.0F, 179.0F}) {
            for (float pitch : new float[]{-50.0F, 0.0F, 41.0F}) {
                for (float delta : new float[]{-180.0F, -90.0F, 37.0F, 90.0F, 180.0F}) {
                    Vec3 oldLocal = RotationUtil.rotToVec(yaw, pitch);
                    Vec3 newLocal = RotationUtil.rotToVec(Mth.wrapDegrees(yaw + delta), pitch);
                    Quaternionf compensated = GravityTransition.compensatedVisualStart(displayed, delta);
                    assertVecEquals(transform(displayed, oldLocal), transform(compensated, newLocal), EPS,
                        "interrupted frame continuity yaw=" + yaw + " pitch=" + pitch + " delta=" + delta);
                }
            }
        }
    }

    @Test
    void transportedLookEndsAtCanonicalTargetWithPitchPreserved() {
        for (Direction from : Direction.values()) for (Direction to : Direction.values()) {
            if (from == to) continue;
            for (float yaw : new float[]{0.0F, 67.0F, -143.0F}) for (float pitch : new float[]{0.0F, 20.0F, -55.0F}) {
                var plan = GravityTransition.plan(from, to, yaw, pitch);
                float newYaw = Mth.wrapDegrees(yaw + plan.yawDelta());
                Vec3 finalWorld = RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(newYaw, pitch), to).normalize();
                assertVecEquals(plan.transportedWorldLook(), finalWorld, EPS, "transported look " + from + " -> " + to);
            }
        }
    }

    @Test
    void oppositeTurnUsesHeadingAndVerticalLookFallsBackToRightAxis() {
        var eastFacing = GravityTransition.plan(Direction.DOWN, Direction.UP, -90.0F, 0.0F);
        assertVecEquals(new Vec3(1, 0, 0), eastFacing.axis(), EPS, "east heading axis");
        assertVecEquals(new Vec3(1, 0, 0), eastFacing.transportedWorldLook(), EPS, "east look preserved");

        var vertical = GravityTransition.plan(Direction.DOWN, Direction.UP, 31.0F, -90.0F);
        Vec3 expectedRight = RotationUtil.vecPlayerToWorld(new Vec3(1, 0, 0), Direction.DOWN);
        assertVecEquals(expectedRight, vertical.axis(), EPS, "vertical fallback axis");
        assertEquals(0.0F, vertical.yawDelta(), 1.0E-3F, "yaw stays stable when yaw is geometrically undefined");
    }

    @Test
    void easeOutCubicIsFastSnapAndMonotonic() {
        assertEquals(0.0F, GravityTransition.easeOutCubic(0.0F), 1.0E-6F);
        assertEquals(0.875F, GravityTransition.easeOutCubic(0.5F), 1.0E-6F);
        assertEquals(1.0F, GravityTransition.easeOutCubic(1.0F), 1.0E-6F);
        float previous = -1.0F;
        for (int i = 0; i <= 100; i++) {
            float value = GravityTransition.easeOutCubic(i / 100.0F);
            assertTrue(value >= previous);
            previous = value;
        }
    }

    private static Vec3 transform(Quaternionf quaternion, Vec3 vector) {
        Vector3f result = new Quaternionf(quaternion).transform(new Vector3f((float)vector.x, (float)vector.y, (float)vector.z));
        return new Vec3(result.x, result.y, result.z);
    }

    private static double quaternionAngle(Quaternionf a, Quaternionf b) {
        float dot = Math.abs(new Quaternionf(a).normalize().dot(new Quaternionf(b).normalize()));
        dot = Math.max(-1.0F, Math.min(1.0F, dot));
        return 2.0D * Math.acos(dot);
    }

    private static void assertVecEquals(Vec3 expected, Vec3 actual, double epsilon, String message) {
        assertEquals(expected.x, actual.x, epsilon, message + " x");
        assertEquals(expected.y, actual.y, epsilon, message + " y");
        assertEquals(expected.z, actual.z, epsilon, message + " z");
    }
}
