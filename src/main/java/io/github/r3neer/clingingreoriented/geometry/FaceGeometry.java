package io.github.r3neer.clingingreoriented.geometry;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FaceGeometry {
    public static final double EPS = 1e-7;
    public record Face(AABB box, Direction normal, Vec3 point, Vec3 from, double distance) {}
    public static Face face(AABB body, AABB box, Direction normal) {
        Direction.Axis axis = normal.getAxis();
        boolean positive = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double plane = positive ? box.max(axis) : box.min(axis);
        double bodyEdge = positive ? body.min(axis) : body.max(axis);
        double gap = (bodyEdge - plane) * (positive ? 1 : -1);
        if (gap < -EPS) return null; // The body must be outside this face.
        double[] target = new double[3], from = new double[3];
        for (Direction.Axis a : Direction.Axis.values()) {
            int i = a.ordinal();
            if (a == axis) { target[i] = plane; from[i] = bodyEdge; }
            else {
                double center = (body.min(a) + body.max(a)) * .5;
                target[i] = Math.clamp(center, box.min(a), box.max(a));
                from[i] = Math.clamp(target[i], body.min(a), body.max(a));
            }
        }
        Vec3 p = new Vec3(target[0], target[1], target[2]);
        Vec3 q = new Vec3(from[0], from[1], from[2]);
        return new Face(box, normal, p, q, p.distanceTo(q));
    }
    public static Vec3 vector(Direction d) { return new Vec3(d.getStepX(), d.getStepY(), d.getStepZ()); }
    public static boolean touching(AABB body, AABB support, Direction gravity) {
        Face f = face(body, support, gravity.getOpposite());
        if (f == null || f.distance > 1e-4) return false;
        for (Direction.Axis a : Direction.Axis.values()) if (a != gravity.getAxis()) {
            if (body.max(a) <= support.min(a) + EPS || body.min(a) >= support.max(a) - EPS) return false;
        }
        return true;
    }
    public static double reach(double width) { return Math.clamp(.625 * width / .6, .08, .9); }
}
