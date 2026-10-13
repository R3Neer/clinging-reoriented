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
    enum ContactRelation {
        NON_SUPPORT(false,false), SUPPORT(true,false), MIXED(true,true);
        private final boolean support;private final boolean mixed;
        ContactRelation(boolean support,boolean mixed){this.support=support;this.mixed=mixed;}
        public boolean support(){return support;}
        public boolean mixed(){return mixed;}
        public static ContactRelation merge(ContactRelation a,ContactRelation b){
            if(a==null)return b;if(b==null)return a;if(a==MIXED||b==MIXED)return MIXED;
            return a==b?a:MIXED;
        }
    }
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

    /**
     * Earliest geometric contact inside one caller-owned swept segment. `support`
     * states whether that contact may be a gravity-relative feet surface. Returning
     * non-support contacts lets the core stop prediction at a wall instead of seeing
     * a valid floor through it.
     */
    record LocalSweep(LocalContact contact, double fraction, boolean support) {
        public LocalSweep {
            if (contact == null || !Double.isFinite(fraction) || fraction < 0.0D || fraction > 1.0D)
                throw new IllegalArgumentException("Invalid landing sweep");
        }
    }

    /** Rich first-contact evidence. MIXED means feet and a non-support face arrived simultaneously. */
    record LocalSweepEvidence(LocalContact contact,double fraction,ContactRelation relation) {
        public LocalSweepEvidence {
            if(contact==null||!Double.isFinite(fraction)||fraction<0.0D||fraction>1.0D||relation==null)
                throw new IllegalArgumentException("Invalid landing sweep evidence");
        }
    }

    /** Current gravity-relative feet support, if this provider owns one. */
    Optional<LocalContact> currentSupport(Query query);

    /**
     * Optional geometry query over one bounded swept body segment. Physics and the
     * time horizon are owned by Clinging; providers only report geometric contact.
     */
    default Optional<LocalSweep> sweep(Query query, AABB startBody, AABB endBody) { return Optional.empty(); }

    /** Backward-compatible richer query; existing providers keep their former support/non-support meaning. */
    default Optional<LocalSweepEvidence> sweepEvidence(Query query,AABB startBody,AABB endBody){
        var legacy=sweep(query,startBody,endBody);
        if(legacy==null||legacy.isEmpty())return Optional.empty();
        var value=legacy.get();
        return Optional.of(new LocalSweepEvidence(value.contact(),value.fraction(),value.support()?ContactRelation.SUPPORT:ContactRelation.NON_SUPPORT));
    }

    /** Revalidate a previously returned contact under the current query/frame. */
    boolean revalidate(Query query, LocalContact contact);
}
