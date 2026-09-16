package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded landing forecast backed by the shared volumetric trajectory engine. */
public final class LandingPrediction {
    public static final int ACQUISITION_TICKS=40;
    public static final int MAX_TICKS=ACQUISITION_TICKS;
    public record Candidate(LandingSurfaces.Contact contact,Direction gravity,double etaTicks) {}
    private LandingPrediction() {}

    public static Optional<Candidate> predict(LivingEntity entity,int horizon){
        if(entity==null||horizon<1||horizon>MAX_TICKS||entity.isPassenger()||entity.isFallFlying()||FluidContext.intersects(entity))return Optional.empty();
        Direction gravity=GravityDirectionUtil.getGravityDirection(entity);
        Vec3 velocity=entity.getDeltaMovement();
        if(!ImpactPhysics.finite(velocity))return Optional.empty();
        AABB body=entity.getBoundingBox().deflate(1.0E-7D);
        var trace=TrajectoryPrediction.simulate(body,velocity,horizon,
            (completed,current)->AirMotion.nextVelocity(entity,gravity,current),
            (start,end)->LandingSurfaces.sweep(entity,gravity,start,end));
        if(!trace.valid()||trace.firstContact().isEmpty())return Optional.empty();
        var hit=trace.firstContact().get();
        if(!hit.support())return Optional.empty();
        return Optional.of(new Candidate(hit.surface().contact(),gravity,hit.etaTicks()));
    }

    /** Kept package-visible for existing calibration tests and callers while S01 centralizes motion. */
    static Vec3 nextAirVelocity(LivingEntity entity,Direction gravity,Vec3 worldVelocity){
        return AirMotion.nextVelocity(entity,gravity,worldVelocity);
    }
}