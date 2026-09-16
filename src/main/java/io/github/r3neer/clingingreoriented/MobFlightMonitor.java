package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * S07 cheap committed-flight observer. It predicts only the current ballistic segment: no surface
 * pathfinding, no gravity mutation and no strategic replanning.
 */
public final class MobFlightMonitor {
    public static final int DEFAULT_HORIZON_TICKS=20;

    public enum Status {
        CLEAR,
        EXPECTED_SUPPORT,
        SAFE_CHANGED_SUPPORT,
        BLOCKING_CONTACT,
        TRAPPED_CONTACT,
        EXPECTED_STALE,
        UNKNOWN_GEOMETRY,
        INVALID_TRACE,
        FLUID
    }

    public record Observation(Status status,TrajectoryPrediction.Hit hit) {
        public boolean danger(){
            return status==Status.BLOCKING_CONTACT||status==Status.TRAPPED_CONTACT||status==Status.EXPECTED_STALE
                ||status==Status.UNKNOWN_GEOMETRY||status==Status.INVALID_TRACE||status==Status.FLUID;
        }
        public boolean material(){return danger()||status==Status.SAFE_CHANGED_SUPPORT;}
        public boolean hasContact(){return hit!=null;}
    }

    private MobFlightMonitor() {}

    public static Observation observe(LivingEntity mob,MobGravityPlanner.Transition committed){
        return observe(mob,committed,DEFAULT_HORIZON_TICKS);
    }

    public static Observation observe(LivingEntity mob,MobGravityPlanner.Transition committed,int horizonTicks){
        if(mob==null||committed==null||!mob.isAlive()||horizonTicks<1||horizonTicks>TrajectoryPrediction.MAX_TICKS)
            return new Observation(Status.INVALID_TRACE,null);
        if(FluidContext.intersects(mob))return new Observation(Status.FLUID,null);

        Direction gravity=GravityDirectionUtil.getOwnGravityDirection(mob);
        if(gravity!=committed.targetGravity())return new Observation(Status.INVALID_TRACE,null);
        if(!LandingSurfaces.revalidate(mob,gravity,committed.landingContact()))
            return new Observation(Status.EXPECTED_STALE,null);

        boolean[] unknown={false};
        var trace=TrajectoryPrediction.simulate(mob.getBoundingBox(),mob.getDeltaMovement(),horizonTicks,
            (completed,currentVelocity)->AirMotion.nextVelocity(mob,gravity,currentVelocity),
            (start,end)->{
                if(!known(mob,end)){unknown[0]=true;return Optional.empty();}
                return LandingSurfaces.sweep(mob,gravity,start,end);
            });
        if(unknown[0])return new Observation(Status.UNKNOWN_GEOMETRY,null);
        if(!trace.valid())return new Observation(Status.INVALID_TRACE,null);
        if(trace.firstContact().isEmpty())return new Observation(Status.CLEAR,null);

        var hit=trace.firstContact().get();
        if(!hit.support())return new Observation(Status.BLOCKING_CONTACT,hit);
        if(!LandingSurfaces.revalidate(mob,gravity,hit.surface().contact()))
            return new Observation(Status.EXPECTED_STALE,hit);
        if(!MobGravity.fits(mob,hit.impactBody())||MobGravityPlanner.egressDirections(mob,gravity,hit.impactBody())==0)
            return new Observation(Status.TRAPPED_CONTACT,hit);

        var expected=committed.landingContact().key();
        var actual=hit.surface().contact().key();
        return new Observation(expected.equals(actual)?Status.EXPECTED_SUPPORT:Status.SAFE_CHANGED_SUPPORT,hit);
    }

    private static boolean known(LivingEntity mob,AABB box){
        if(box==null||!Double.isFinite(box.minX+box.minY+box.minZ+box.maxX+box.maxY+box.maxZ))return false;
        var level=mob.level();
        if(box.minY<level.getMinY()||box.maxY>level.getMaxY()+1||!level.getWorldBorder().isWithinBounds(box))return false;
        int y=Math.max(level.getMinY(),Math.min(level.getMaxY(),(int)Math.floor((box.minY+box.maxY)*.5D)));
        int minChunkX=((int)Math.floor(box.minX))>>4,maxChunkX=((int)Math.floor(box.maxX))>>4;
        int minChunkZ=((int)Math.floor(box.minZ))>>4,maxChunkZ=((int)Math.floor(box.maxZ))>>4;
        for(int x=minChunkX;x<=maxChunkX;x++)for(int z=minChunkZ;z<=maxChunkZ;z++)
            if(!level.hasChunkAt(new BlockPos(x<<4,y,z<<4)))return false;
        return true;
    }
}
