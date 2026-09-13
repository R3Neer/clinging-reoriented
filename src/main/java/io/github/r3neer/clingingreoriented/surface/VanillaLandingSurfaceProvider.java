package io.github.r3neer.clingingreoriented.surface;

import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import java.util.Optional;
import net.minecraft.world.phys.AABB;

/** Vanilla block-collision support. Future sweep prediction is added by the landing-prediction sprint. */
public final class VanillaLandingSurfaceProvider implements LandingSurfaceProvider {
    private static final double PROBE = 1.0E-4D;

    @Override public Optional<LocalContact> currentSupport(Query query) {
        if (!query.entity().onGround()) return Optional.empty();
        var body = query.entity().getBoundingBox();
        var probe = body.inflate(PROBE);
        for (var shape : query.entity().level().getBlockCollisions(query.entity(), probe)) {
            for (var box : shape.toAabbs()) {
                if (FaceGeometry.touching(body, box, query.gravity()))
                    return Optional.of(new LocalContact(identity(box), 0L, FaceGeometry.vector(query.gravity().getOpposite())));
            }
        }
        return Optional.empty();
    }

    @Override public boolean revalidate(Query query, LocalContact contact) {
        if (contact == null || !query.entity().onGround()) return false;
        var body = query.entity().getBoundingBox();
        var probe = body.inflate(PROBE);
        for (var shape : query.entity().level().getBlockCollisions(query.entity(), probe)) {
            for (var box : shape.toAabbs()) {
                if (identity(box).equals(contact.localId()) && FaceGeometry.touching(body, box, query.gravity())) return true;
            }
        }
        return false;
    }

    private static String identity(AABB box) {
        return Double.toHexString(box.minX) + "," + Double.toHexString(box.minY) + "," + Double.toHexString(box.minZ) + ";"
            + Double.toHexString(box.maxX) + "," + Double.toHexString(box.maxY) + "," + Double.toHexString(box.maxZ);
    }
}
