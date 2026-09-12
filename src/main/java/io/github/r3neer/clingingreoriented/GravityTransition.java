package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure geometry for Clinging/Reorientation gravity snaps. */
public final class GravityTransition {
    public static final long QUARTER_TURN_NANOS = 120_000_000L;
    public static final long HALF_TURN_NANOS = 180_000_000L;
    private static final double EPSILON = 1.0E-8;

    public enum TurnKind {
        QUARTER(QUARTER_TURN_NANOS),
        HALF(HALF_TURN_NANOS);

        private final long durationNanos;
        TurnKind(long durationNanos) { this.durationNanos = durationNanos; }
        public long durationNanos() { return durationNanos; }
        public static TurnKind fromId(int id) { return id == 0 ? QUARTER : id == 1 ? HALF : null; }
    }

    public record Plan(
        Direction previous,
        Direction target,
        TurnKind kind,
        Vec3 axis,
        Vec3 oldWorldLook,
        Vec3 transportedWorldLook,
        float yawDelta
    ) {}

    private GravityTransition() {}

    public static Plan plan(Direction previous, Direction target, float yaw, float pitch) {
        if (previous == null || target == null || previous == target) {
            throw new IllegalArgumentException("Gravity transition requires two distinct directions");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Rotation must be finite");
        }

        Vec3 oldGravity = direction(previous);
        Vec3 newGravity = direction(target);
        Vec3 oldLocalLook = RotationUtil.rotToVec(yaw, pitch);
        Vec3 oldWorldLook = RotationUtil.vecPlayerToWorld(oldLocalLook, previous).normalize();

        double dot = oldGravity.dot(newGravity);
        TurnKind kind;
        Vec3 axis;
        double angle;
        if (dot < -0.5D) {
            kind = TurnKind.HALF;
            angle = Math.PI;
            Vec3 projectedHeading = oldWorldLook.subtract(oldGravity.scale(oldWorldLook.dot(oldGravity)));
            if (projectedHeading.lengthSqr() <= EPSILON) {
                axis = RotationUtil.vecPlayerToWorld(new Vec3(1.0D, 0.0D, 0.0D), previous).normalize();
            } else {
                axis = projectedHeading.normalize();
            }
        } else {
            kind = TurnKind.QUARTER;
            angle = Math.PI * 0.5D;
            axis = oldGravity.cross(newGravity);
            if (axis.lengthSqr() <= EPSILON) {
                throw new IllegalArgumentException("Unsupported non-opposite gravity pair");
            }
            axis = axis.normalize();
        }

        Vec3 transported = rotate(oldWorldLook, axis, angle).normalize();
        Vec3 targetLocalLook = RotationUtil.vecWorldToPlayer(transported, target).normalize();
        var targetRotation = RotationUtil.vecToRot(targetLocalLook);
        double horizontal = Math.hypot(targetLocalLook.x, targetLocalLook.z);
        float targetYaw = horizontal <= 1.0E-6D ? yaw : targetRotation.x;
        float yawDelta = Mth.wrapDegrees(targetYaw - yaw);

        return new Plan(previous, target, kind, axis, oldWorldLook, transported, yawDelta);
    }

    public static Quaternionf compensatedVisualStart(Quaternionf currentVisual, float yawDelta) {
        return new Quaternionf(currentVisual)
            .mul(new Quaternionf().rotateY((float)Math.toRadians(yawDelta)));
    }

    public static float easeOutCubic(float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        float remaining = 1.0F - t;
        return 1.0F - remaining * remaining * remaining;
    }

    public static Vec3 rotate(Vec3 vector, Vec3 axis, double angle) {
        Vector3f transformed = new Quaternionf()
            .rotateAxis((float)angle, (float)axis.x, (float)axis.y, (float)axis.z)
            .transform(new Vector3f((float)vector.x, (float)vector.y, (float)vector.z));
        return new Vec3(transformed.x, transformed.y, transformed.z);
    }

    public static Vec3 direction(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
