package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Short-horizon, recomputed-every-tick landing forecast. It predicts no more than one visual snap window. */
public final class LandingPrediction {
    public static final int MAX_TICKS=5;
    private static final double AIR_HORIZONTAL_DRAG=.91D;
    private static final double AIR_VERTICAL_DRAG=.98D;
    public record Candidate(LandingSurfaces.Contact contact,Direction gravity,double etaTicks) {}
    private LandingPrediction() {}

    public static Optional<Candidate> predict(LivingEntity entity,int horizon){
        if(entity==null||horizon<1||horizon>MAX_TICKS||entity.isPassenger()||entity.isFallFlying()||entity.isInWater()||entity.isInLava())return Optional.empty();
        Direction gravity=GravityDirectionUtil.getGravityDirection(entity);
        Vec3 velocity=entity.getDeltaMovement();
        if(!ImpactPhysics.finite(velocity))return Optional.empty();
        AABB body=entity.getBoundingBox().deflate(1.0E-7D);
        for(int tick=0;tick<horizon;tick++){
            AABB next=body.move(velocity);
            var hit=LandingSurfaces.sweep(entity,gravity,body,next);
            if(hit.isPresent()){
                if(!hit.get().support())return Optional.empty();
                return Optional.of(new Candidate(hit.get().contact(),gravity,tick+hit.get().fraction()));
            }
            body=next;
            velocity=nextAirVelocity(entity,gravity,velocity);
            if(!ImpactPhysics.finite(velocity))return Optional.empty();
        }
        return Optional.empty();
    }

    static Vec3 nextAirVelocity(LivingEntity entity,Direction gravity,Vec3 worldVelocity){
        Vec3 local=RotationUtil.vecWorldToPlayer(worldVelocity,gravity);
        double effective=GravityDirectionUtil.getEffectiveGravity(.08D,local.y,entity.hasEffect(MobEffects.SLOW_FALLING));
        double scaled=GravityDirectionUtil.scaleGravity(entity,effective);
        Vec3 nextLocal=new Vec3(local.x*AIR_HORIZONTAL_DRAG,(local.y-scaled)*AIR_VERTICAL_DRAG,local.z*AIR_HORIZONTAL_DRAG);
        return RotationUtil.vecPlayerToWorld(nextLocal,gravity);
    }
}
