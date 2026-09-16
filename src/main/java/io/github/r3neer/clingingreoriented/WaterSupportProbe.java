package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Tiny volumetric contact probe used only to decide the underwater presentation frame. */
public final class WaterSupportProbe {
    public static final double PROBE_DISTANCE=0.035D;
    private WaterSupportProbe() {}

    public static Optional<LandingSurfaces.Contact> current(LivingEntity entity){
        if(entity==null||!entity.isAlive())return Optional.empty();
        Direction gravity=GravityDirectionUtil.getGravityDirection(entity);
        AABB start=entity.getBoundingBox().deflate(1.0E-7D);
        Vec3 down=GravityTransition.direction(gravity).scale(PROBE_DISTANCE);
        var hit=LandingSurfaces.sweep(entity,gravity,start,start.move(down));
        if(hit.isEmpty()||!hit.get().support())return Optional.empty();
        var contact=hit.get().contact();
        return LandingSurfaces.revalidate(entity,gravity,contact)?Optional.of(contact):Optional.empty();
    }
}
