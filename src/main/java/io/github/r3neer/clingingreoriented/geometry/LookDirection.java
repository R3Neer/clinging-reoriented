package io.github.r3neer.clingingreoriented.geometry;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** World-space view intent, independent of motion, contact and target distance. */
public final class LookDirection {
    private LookDirection() {}
    public static Direction select(Vec3 look) {
        if (look == null || !Double.isFinite(look.lengthSqr()) || Math.abs(look.lengthSqr() - 1) > .01) return null;
        double x = Math.abs(look.x), y = Math.abs(look.y), z = Math.abs(look.z);
        double largest = Math.max(x, Math.max(y, z));
        // Nearest cardinal, with deterministic X/Y/Z priority for exact ties.
        if (largest == x) return look.x > 0 ? Direction.EAST : Direction.WEST;
        if (largest == y) return look.y > 0 ? Direction.UP : Direction.DOWN;
        return look.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
