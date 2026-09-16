package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded volumetric trajectory forecast shared by landing and gravity-navigation planning. */
public final class TrajectoryPrediction {
    public static final int MAX_TICKS=200;

    @FunctionalInterface public interface VelocityStepper {
        Vec3 nextVelocity(int completedTicks,Vec3 currentVelocity);
    }
    @FunctionalInterface public interface SweepProbe {
        Optional<LandingSurfaces.SweepHit> sweep(AABB startBody,AABB endBody);
    }

    public record Hit(LandingSurfaces.SweepHit surface,double etaTicks,Vec3 incomingVelocity,AABB impactBody) {
        public boolean support(){return surface.support();}
        public double normalImpactSpeed(){
            double into=-incomingVelocity.dot(surface.contact().normal());
            return Double.isFinite(into)?Math.max(0.0D,into):0.0D;
        }
        public double vanillaEquivalentFallDistance(){return ImpactPhysics.vanillaEquivalentFallDistance(normalImpactSpeed());}
    }

    public record Trace(Optional<Hit> firstContact,AABB endBody,Vec3 endVelocity,int completedTicks,boolean valid) {}

    private TrajectoryPrediction() {}

    /**
     * Advances one full entity AABB at a time and asks the supplied probe for the earliest contact
     * on each swept segment. The caller owns the motion law and collision provider, so the same
     * bounded engine can forecast a player landing or a candidate mob gravity transition.
     */
    public static Trace simulate(AABB startBody,Vec3 startVelocity,int horizon,VelocityStepper stepper,SweepProbe probe){
        if(!finite(startBody)||!ImpactPhysics.finite(startVelocity)||horizon<1||horizon>MAX_TICKS||stepper==null||probe==null)
            return invalid(startBody,startVelocity);
        AABB body=startBody;Vec3 velocity=startVelocity;
        for(int tick=0;tick<horizon;tick++){
            AABB next=body.move(velocity);
            Optional<LandingSurfaces.SweepHit> swept=probe.sweep(body,next);
            if(swept==null)return invalid(body,velocity);
            if(swept.isPresent()){
                var surface=swept.get();
                double fraction=surface.fraction();
                AABB impact=body.move(velocity.scale(fraction));
                return new Trace(Optional.of(new Hit(surface,tick+fraction,velocity,impact)),impact,velocity,tick,true);
            }
            body=next;
            Vec3 advanced=stepper.nextVelocity(tick+1,velocity);
            if(!ImpactPhysics.finite(advanced))return new Trace(Optional.empty(),body,velocity,tick+1,false);
            velocity=advanced;
        }
        return new Trace(Optional.empty(),body,velocity,horizon,true);
    }

    private static Trace invalid(AABB body,Vec3 velocity){
        AABB safe=finite(body)?body:new AABB(0,0,0,0,0,0);
        Vec3 motion=ImpactPhysics.finite(velocity)?velocity:Vec3.ZERO;
        return new Trace(Optional.empty(),safe,motion,0,false);
    }
    private static boolean finite(AABB box){
        return box!=null&&Double.isFinite(box.minX+box.minY+box.minZ+box.maxX+box.maxY+box.maxZ)
            &&box.getXsize()>=0.0D&&box.getYsize()>=0.0D&&box.getZsize()>=0.0D;
    }
}
