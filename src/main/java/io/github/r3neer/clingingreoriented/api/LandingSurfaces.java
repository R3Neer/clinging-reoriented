package io.github.r3neer.clingingreoriented.api;

import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.surface.VanillaLandingSurfaceProvider;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Public registry and validation fence for gravity-relative landing geometry. */
public final class LandingSurfaces {
    public static final Identifier VANILLA = Identifier.fromNamespaceAndPath(ClingingReoriented.ID, "vanilla");
    private static final Object LOCK = new Object();
    private static final Map<Identifier,LandingSurfaceProvider> PROVIDERS = new HashMap<>();
    private static final Set<String> WARNED = new HashSet<>();

    static { PROVIDERS.put(VANILLA, new VanillaLandingSurfaceProvider()); }

    private LandingSurfaces() {}

    public record SurfaceKey(Identifier provider, String localId, long revision) {
        public SurfaceKey {
            if (provider == null || localId == null || localId.isBlank() || localId.length() > 512 || revision < 0)
                throw new IllegalArgumentException("Invalid landing surface key");
        }
    }

    /** Contact identity is frame-bound: a DOWN contact can never be reused after a gravity change to EAST. */
    public record Contact(SurfaceKey key, Direction gravity, Vec3 normal) {
        public Contact {
            if (key == null || gravity == null || normal == null || !Double.isFinite(normal.x + normal.y + normal.z)
                || Math.abs(normal.lengthSqr() - 1.0D) > 1.0E-5D)
                throw new IllegalArgumentException("Invalid landing contact");
        }
    }

    public record SweepHit(Contact contact, double fraction) {
        public SweepHit {
            if (contact == null || !Double.isFinite(fraction) || fraction < 0.0D || fraction > 1.0D)
                throw new IllegalArgumentException("Invalid landing sweep hit");
        }
    }

    public interface Registration extends AutoCloseable { @Override void close(); }

    /** Register one provider id. Duplicate ids are rejected rather than silently replacing ownership. */
    public static Registration register(Identifier id, LandingSurfaceProvider provider) {
        if (id == null || provider == null) throw new IllegalArgumentException("Landing provider id/provider required");
        synchronized (LOCK) {
            if (PROVIDERS.containsKey(id)) throw new IllegalStateException("Landing surface provider already registered: " + id);
            PROVIDERS.put(id, provider);
        }
        return new Registration() {
            private boolean closed;
            @Override public void close() {
                synchronized (LOCK) {
                    if (closed) return;
                    closed = true;
                    if (PROVIDERS.get(id) == provider) PROVIDERS.remove(id);
                    WARNED.removeIf(key -> key.startsWith(id.toString() + ":"));
                }
            }
        };
    }

    public static Optional<Contact> currentSupport(LivingEntity entity, Direction gravity) {
        if (entity == null || gravity == null) return Optional.empty();
        var query = new LandingSurfaceProvider.Query(entity, gravity);
        for (var entry : snapshot()) {
            var local = safeSupport(entry.getKey(), entry.getValue(), query);
            if (local.isPresent()) return Optional.of(wrap(entry.getKey(), gravity, local.get()));
        }
        return Optional.empty();
    }

    /** Earliest provider hit; equal fractions are resolved by stable provider id ordering. */
    public static Optional<SweepHit> sweep(LivingEntity entity, Direction gravity, AABB startBody, AABB endBody) {
        if (entity == null || gravity == null || !finite(startBody) || !finite(endBody)) return Optional.empty();
        var query = new LandingSurfaceProvider.Query(entity, gravity);
        SweepHit best = null;
        for (var entry : snapshot()) {
            var local = safeSweep(entry.getKey(), entry.getValue(), query, startBody, endBody);
            if (local.isEmpty()) continue;
            var hit = new SweepHit(wrap(entry.getKey(), gravity, local.get().contact()), local.get().fraction());
            if (best == null || hit.fraction() < best.fraction() - 1.0E-12D) best = hit;
        }
        return Optional.ofNullable(best);
    }

    public static boolean revalidate(LivingEntity entity, Direction gravity, Contact contact) {
        if (entity == null || gravity == null || contact == null || contact.gravity() != gravity) return false;
        LandingSurfaceProvider provider;
        synchronized (LOCK) { provider = PROVIDERS.get(contact.key().provider()); }
        if (provider == null) return false;
        try {
            var local = new LandingSurfaceProvider.LocalContact(contact.key().localId(), contact.key().revision(), contact.normal());
            return provider.revalidate(new LandingSurfaceProvider.Query(entity, gravity), local);
        } catch (RuntimeException failure) {
            warnOnce(contact.key().provider(), "revalidate", failure);
            return false;
        }
    }

    private static ArrayList<Map.Entry<Identifier,LandingSurfaceProvider>> snapshot() {
        ArrayList<Map.Entry<Identifier,LandingSurfaceProvider>> result;
        synchronized (LOCK) { result = new ArrayList<>(PROVIDERS.entrySet()); }
        result.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        return result;
    }

    private static Optional<LandingSurfaceProvider.LocalContact> safeSupport(Identifier id, LandingSurfaceProvider provider,
                                                                             LandingSurfaceProvider.Query query) {
        try {
            var value = provider.currentSupport(query);
            return value == null ? Optional.empty() : value;
        } catch (RuntimeException failure) {
            warnOnce(id, "support", failure);
            return Optional.empty();
        }
    }

    private static Optional<LandingSurfaceProvider.LocalSweep> safeSweep(Identifier id, LandingSurfaceProvider provider,
                                                                          LandingSurfaceProvider.Query query,
                                                                          AABB startBody, AABB endBody) {
        try {
            var value = provider.sweep(query, startBody, endBody);
            return value == null ? Optional.empty() : value;
        } catch (RuntimeException failure) {
            warnOnce(id, "sweep", failure);
            return Optional.empty();
        }
    }

    private static Contact wrap(Identifier id, Direction gravity, LandingSurfaceProvider.LocalContact local) {
        return new Contact(new SurfaceKey(id, local.localId(), local.revision()), gravity, local.normal());
    }

    private static boolean finite(AABB box) {
        return box != null && Double.isFinite(box.minX + box.minY + box.minZ + box.maxX + box.maxY + box.maxZ)
            && box.getXsize() >= 0.0D && box.getYsize() >= 0.0D && box.getZsize() >= 0.0D;
    }

    private static void warnOnce(Identifier id, String operation, RuntimeException failure) {
        String key = id + ":" + operation;
        synchronized (LOCK) {
            if (!WARNED.add(key)) return;
        }
        org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID)
            .warn("Landing surface provider {} failed during {}; ignoring this result", id, operation, failure);
    }
}
