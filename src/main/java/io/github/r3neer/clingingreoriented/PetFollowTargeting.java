package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

/**
 * Strategic target filter for pet follow. The owner is observed every tick, but an airborne owner only
 * moves the planning anchor after material displacement or bounded staleness. Grounded owners remain exact.
 */
public final class PetFollowTargeting {
    static final double AIRBORNE_REFRESH_DISTANCE=8.0D;
    static final double AIRBORNE_REFRESH_DISTANCE_SQR=AIRBORNE_REFRESH_DISTANCE*AIRBORNE_REFRESH_DISTANCE;
    static final long AIRBORNE_MAX_STALE_TICKS=20L;

    public static final class State {
        private Vec3 anchor;
        private boolean airborne;
        private long anchorAt;

        public Vec3 anchor(){return anchor;}
        public boolean airborne(){return airborne;}
        public long anchorAt(){return anchorAt;}
        public void clear(){anchor=null;airborne=false;anchorAt=0L;}
    }

    public record Target(Vec3 anchor,boolean airborne,boolean changed) {}

    private PetFollowTargeting() {}

    public static Target observe(TamableAnimal pet,LivingEntity owner,State state){
        if(pet==null||owner==null||state==null||!owner.isAlive()||owner.level()!=pet.level()){
            if(state!=null)state.clear();
            return null;
        }
        Vec3 current=owner.position();
        if(!finite(current)){state.clear();return null;}
        long now=pet.level().getGameTime();
        boolean airborne=!AirChanges.grounded(owner);

        if(!airborne){
            boolean changed=state.anchor==null||state.airborne||state.anchor.distanceToSqr(current)>1.0E-8D;
            state.anchor=current;state.airborne=false;state.anchorAt=now;
            return new Target(current,false,changed);
        }

        boolean refresh=state.anchor==null||!state.airborne
            ||state.anchor.distanceToSqr(current)>=AIRBORNE_REFRESH_DISTANCE_SQR
            ||now-state.anchorAt>=AIRBORNE_MAX_STALE_TICKS;
        if(refresh){state.anchor=current;state.airborne=true;state.anchorAt=now;}
        return new Target(state.anchor,true,refresh);
    }

    /** Distance inside the movement plane for a candidate gravity. Axial separation is deliberately ignored. */
    static double tangentialDistanceSqr(Vec3 position,Vec3 anchor,Direction gravity){
        if(!finite(position)||!finite(anchor)||gravity==null)return Double.POSITIVE_INFINITY;
        double dx=position.x-anchor.x,dy=position.y-anchor.y,dz=position.z-anchor.z;
        return switch(gravity.getAxis()){
            case X -> dy*dy+dz*dz;
            case Y -> dx*dx+dz*dz;
            case Z -> dx*dx+dy*dy;
        };
    }

    static double goalDistance(Vec3 position,Vec3 anchor,Direction gravity,boolean ownerAirborne){
        double sq=ownerAirborne?tangentialDistanceSqr(position,anchor,gravity):position.distanceToSqr(anchor);
        return Double.isFinite(sq)&&sq>=0.0D?Math.sqrt(sq):Double.POSITIVE_INFINITY;
    }

    private static boolean finite(Vec3 value){return value!=null&&Double.isFinite(value.x+value.y+value.z);}
}
