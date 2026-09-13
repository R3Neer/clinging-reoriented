package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Pure geometry and gauge helpers for Clinging/Reorientation gravity snaps. */
public final class GravityTransition {
    public static final long QUARTER_TURN_NANOS = 180_000_000L;
    public static final long HALF_TURN_NANOS = 240_000_000L;
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
        Vec3 oldWorldHeading,
        Vec3 transportedWorldHeading,
        float yawDelta
    ) {}

    private GravityTransition() {}

    /** World-space navigation heading encoded by local yaw, deliberately ignoring pitch. */
    public static Vec3 headingFromYaw(Direction gravity, float yaw) {
        if (gravity == null || !Float.isFinite(yaw)) throw new IllegalArgumentException("Heading source must be finite");
        return RotationUtil.vecPlayerToWorld(RotationUtil.rotToVec(yaw, 0.0F), gravity).normalize();
    }

    /**
     * Treat client heading as intent, not authority: remove any component along gravity.
     * A degenerate/malformed value falls back to the authoritative server yaw.
     */
    public static Vec3 sanitizeHeading(Direction gravity, Vec3 requested, float fallbackYaw) {
        Vec3 g = direction(gravity);
        if (requested != null && Double.isFinite(requested.x + requested.y + requested.z) && requested.lengthSqr() > EPSILON) {
            Vec3 projected = requested.subtract(g.scale(requested.dot(g)));
            if (Double.isFinite(projected.x + projected.y + projected.z) && projected.lengthSqr() > EPSILON) return projected.normalize();
        }
        return headingFromYaw(gravity, fallbackYaw);
    }

    /** Plan a gravity-frame transport from a normalized world-space navigation heading. */
    public static Plan plan(Direction previous, Direction target, Vec3 navigationHeading) {
        if (previous == null || target == null || previous == target) {
            throw new IllegalArgumentException("Gravity transition requires two distinct directions");
        }
        Vec3 oldGravity = direction(previous);
        Vec3 newGravity = direction(target);
        if (navigationHeading == null || !Double.isFinite(navigationHeading.x + navigationHeading.y + navigationHeading.z)) {
            throw new IllegalArgumentException("Navigation heading must be finite");
        }
        Vec3 heading = navigationHeading.subtract(oldGravity.scale(navigationHeading.dot(oldGravity)));
        if (heading.lengthSqr() <= EPSILON) throw new IllegalArgumentException("Navigation heading must lie in the old gravity plane");
        heading = heading.normalize();

        double dot = oldGravity.dot(newGravity);
        TurnKind kind;
        Vec3 axis;
        double angle;
        if (dot < -0.5D) {
            kind = TurnKind.HALF;
            angle = Math.PI;
            axis = heading;
        } else {
            kind = TurnKind.QUARTER;
            angle = Math.PI * 0.5D;
            axis = oldGravity.cross(newGravity);
            if (axis.lengthSqr() <= EPSILON) throw new IllegalArgumentException("Unsupported gravity pair");
            axis = axis.normalize();
        }

        return build(previous,target,kind,axis,heading,angle);
    }

    /**
     * Re-express another entity's heading through an already chosen physical rotation.
     * Mounted hierarchies use this so rider and mount share one axis/kind while each
     * receives the yaw-gauge delta required by its own pre-turn heading.
     */
    public static Plan rebase(Plan physicalPlan, Vec3 entityHeading) {
        if (physicalPlan == null) throw new IllegalArgumentException("Physical plan is required");
        Direction previous=physicalPlan.previous();
        Direction target=physicalPlan.target();
        Vec3 oldGravity=direction(previous);
        if(entityHeading==null || !Double.isFinite(entityHeading.x+entityHeading.y+entityHeading.z))
            throw new IllegalArgumentException("Entity heading must be finite");
        Vec3 heading=entityHeading.subtract(oldGravity.scale(entityHeading.dot(oldGravity)));
        if(heading.lengthSqr()<=EPSILON)throw new IllegalArgumentException("Entity heading must lie in the old gravity plane");
        heading=heading.normalize();
        double angle=physicalPlan.kind()==TurnKind.HALF?Math.PI:Math.PI*0.5D;
        return build(previous,target,physicalPlan.kind(),physicalPlan.axis(),heading,angle);
    }

    private static Plan build(Direction previous,Direction target,TurnKind kind,Vec3 axis,Vec3 heading,double angle){
        Vec3 normalizedAxis=axis.normalize();
        Vec3 transportedHeading = rotate(heading, normalizedAxis, angle).normalize();
        Vec3 oldLocalHeading = RotationUtil.vecWorldToPlayer(heading, previous).normalize();
        Vec3 targetLocalHeading = RotationUtil.vecWorldToPlayer(transportedHeading, target).normalize();
        float oldYaw = RotationUtil.vecToRot(oldLocalHeading).x;
        float targetYaw = RotationUtil.vecToRot(targetLocalHeading).x;
        float yawDelta = Mth.wrapDegrees(targetYaw - oldYaw);
        return new Plan(previous, target, kind, normalizedAxis, heading, transportedHeading, yawDelta);
    }

    /**
     * Changing gravity changes the local yaw gauge, not the relative head/body pose.
     * Apply the same delta to every yaw accumulator so vanilla interpolation cannot
     * manufacture an extra third-person twist while the gravity frame snaps.
     */
    public static void applyYawGauge(Entity entity,float yawDelta) {
        entity.setYRot(Mth.wrapDegrees(entity.getYRot()+yawDelta));
        entity.yRotO=Mth.wrapDegrees(entity.yRotO+yawDelta);
        if(entity instanceof LivingEntity living){
            living.yBodyRot=Mth.wrapDegrees(living.yBodyRot+yawDelta);
            living.yBodyRotO=Mth.wrapDegrees(living.yBodyRotO+yawDelta);
            living.yHeadRot=Mth.wrapDegrees(living.yHeadRot+yawDelta);
            living.yHeadRotO=Mth.wrapDegrees(living.yHeadRotO+yawDelta);
        }
    }

    public static Quaternionf compensatedVisualStart(Quaternionf currentVisual, float yawDelta) {
        return new Quaternionf(currentVisual)
            .mul(new Quaternionf().rotateY((float)Math.toRadians(yawDelta)));
    }

    public static float easeOutQuadratic(float progress) {
        float t = Mth.clamp(progress, 0.0F, 1.0F);
        float remaining = 1.0F - t;
        return 1.0F - remaining * remaining;
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
