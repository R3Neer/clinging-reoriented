package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Shared vanilla-like airborne velocity recurrence in an arbitrary gravity frame. */
public final class AirMotion {
    private static final double HORIZONTAL_DRAG=.91D;
    private static final double VERTICAL_DRAG=.98D;

    private AirMotion() {}

    public static Vec3 nextVelocity(LivingEntity entity,Direction gravity,Vec3 worldVelocity){
        if(entity==null||gravity==null||!ImpactPhysics.finite(worldVelocity))return Vec3.ZERO;
        Vec3 local=RotationUtil.vecWorldToPlayer(worldVelocity,gravity);
        double effective=GravityDirectionUtil.getEffectiveGravity(.08D,local.y,entity.hasEffect(MobEffects.SLOW_FALLING));
        double scaled=GravityDirectionUtil.scaleGravity(entity,effective);
        Vec3 nextLocal=new Vec3(local.x*HORIZONTAL_DRAG,(local.y-scaled)*VERTICAL_DRAG,local.z*HORIZONTAL_DRAG);
        return RotationUtil.vecPlayerToWorld(nextLocal,gravity);
    }
}
