package io.github.r3neer.clingingreoriented.api;

import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Geometry-only extension point for surfaces that may support a gravity-relative
 * living entity. Providers never own gravity, placement, camera or landing policy.
 */
public interface LandingSurfaceProvider {
    record Query(LivingEntity entity, Direction gravity) {
        public Query {
            if (entity == null || gravity == null) throw new IllegalArgumentException("Landing query requires entity and gravity");
        }
    }

    /** Provider-local immutable identity. The registry adds the provider id. */
    record LocalContact(String localId, long revision, Vec3 normal) {
        public LocalContact {
            if (localId == null || localId.isBlank() || localId.length() > 512 || revision < 0 || normal == null
                || !Double.isFinite(normal.x + normal.y + normal.z) || Math.abs(normal.lengthSqr() - 1.0D) > 1.0E-5D)
                throw new IllegalArgumentException("Invalid landing surface contact");
        }
    }

    /** Earliest contact fraction inside one caller-owned swept segment. */
    record LocalSweep(LocalContact contact, double fraction) {
        public LocalSweep {
            if (contact == null || !Double.isFinite(fraction) || fraction < 0.0D || fraction > 1.0D)
                throw new IllegalArgumentException("Invalid landing sweep");
        }
    }

    /** Current gravity-relative feet support, if this provider owns one. */
    Optional<LocalContact> currentSupport(Query query);

    /**
     * Optional geometry query over one bounded swept body segment. Physics and the
     * time horizon are owned by Clinging; providers only report geometric contact.
     */
    default Optional<LocalSweep> sweep(Query query, AABB startBody, AABB endBody) { return Optional.empty(); }

    /** Revalidate a previously returned contact under the current query/frame. */
    boolean revalidate(Query query, LocalContact contact);
}
