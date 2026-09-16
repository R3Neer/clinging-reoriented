package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.init.ModAttributes;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * S05 physical planner kernel. It evaluates one immediate gravity transition without mutating the mob.
 * Goal semantics and launch-point search live above this layer.
 */
public final class MobGravityPlanner {
    public static final int DEFAULT_TRANSITION_HORIZON_TICKS=80;
    private static final double BODY_EPS=1.0E-7D;
    private static final double EGRESS_STEP=.25D;
    private static final double SUPPORT_PROBE=.05D;
    private static final double TURN_BASE_COST=6.0D;
    private static final double RELOCATION_COST_PER_BLOCK=4.0D;
    private static final double FRAGILITY_COST=12.0D;
    private static final double MAX_RISK_COST=1_000_000.0D;

    public enum Rejection {
        NONE,
        NO_CAPABILITY,
        FOREIGN_GRAVITY,
        CAPABILITY_SPENT,
        INCOMPATIBLE_CONTEXT,
        UNCHANGED,
        NO_SPACE,
        UNKNOWN_GEOMETRY,
        INVALID_TRACE,
        NO_LANDING_IN_HORIZON,
        BLOCKING_CONTACT,
        STALE_CONTACT,
        TRAPPED_LANDING
    }

    public record Transition(
        Direction targetGravity,
        Vec3 launchPosition,
        LandingSurfaces.Contact landingContact,
        AABB landingBody,
        double etaTicks,
        double vanillaEquivalentFallDistance,
        double predictedDamagePoints,
        int egressDirections,
        double robustness,
        double riskCost,
        double physicalCost
    ) {}

    public record Evaluation(Transition transition,Rejection rejection) {
        public boolean accepted(){return transition!=null&&rejection==Rejection.NONE;}
    }

    private MobGravityPlanner() {}

    public static Evaluation evaluateImmediate(LivingEntity mob,Direction targetGravity){
        return evaluateImmediate(mob,targetGravity,DEFAULT_TRANSITION_HORIZON_TICKS);
    }

    public static Evaluation evaluateImmediate(LivingEntity mob,Direction targetGravity,int horizonTicks){
        Rejection preflight=preflight(mob,targetGravity,horizonTicks);
        if(preflight!=Rejection.NONE)return rejected(preflight);

        Direction current=GravityDirectionUtil.getOwnGravityDirection(mob);
        var state=MobGravity.state(mob);
        boolean grounded=AirChanges.grounded(mob);
        if(!grounded&&state.airUsed&&!mob.hasEffect(Reorientation.EFFECT))return rejected(Rejection.CAPABILITY_SPENT);

        Vec3 launch=launchPosition(mob,targetGravity);
        if(launch==null)return rejected(Rejection.NO_SPACE);
        AABB startBody=RotationUtil.makeBoxFromDimensions(mob.getDimensions(mob.getPose()),targetGravity,launch).deflate(BODY_EPS);
        if(!known(mob,startBody))return rejected(Rejection.UNKNOWN_GEOMETRY);
        Vec3 startVelocity=mob.getDeltaMovement();
        if(!ImpactPhysics.finite(startVelocity))return rejected(Rejection.INVALID_TRACE);

        Rejection[] probeFailure={Rejection.NONE};
        var trace=TrajectoryPrediction.simulate(startBody,startVelocity,horizonTicks,
            (completed,currentVelocity)->AirMotion.nextVelocity(mob,targetGravity,currentVelocity),
            (start,end)->{
                if(!known(mob,end)){probeFailure[0]=Rejection.UNKNOWN_GEOMETRY;return null;}
                return LandingSurfaces.sweep(mob,targetGravity,start,end);
            });
        if(!trace.valid())return rejected(probeFailure[0]!=Rejection.NONE?probeFailure[0]:Rejection.INVALID_TRACE);
        if(trace.firstContact().isEmpty())return rejected(Rejection.NO_LANDING_IN_HORIZON);

        var hit=trace.firstContact().get();
        if(!hit.support())return rejected(Rejection.BLOCKING_CONTACT);
        var contact=hit.surface().contact();
        if(!LandingSurfaces.revalidate(mob,targetGravity,contact))return rejected(Rejection.STALE_CONTACT);

        AABB landingBody=hit.impactBody();
        if(!known(mob,landingBody))return rejected(Rejection.UNKNOWN_GEOMETRY);
        if(!MobGravity.fits(mob,landingBody))return rejected(Rejection.NO_SPACE);
        int egress=egressDirections(mob,targetGravity,landingBody);
        if(egress==0)return rejected(Rejection.TRAPPED_LANDING);

        double robustness=egress/4.0D;
        double equivalent=hit.vanillaEquivalentFallDistance();
        double damage=predictedDamagePoints(equivalent);
        double risk=riskCost(equivalent,mob.getHealth());
        double relocation=Math.sqrt(launch.distanceToSqr(mob.position()));
        double physical=hit.etaTicks()+TURN_BASE_COST+relocation*RELOCATION_COST_PER_BLOCK
            +(1.0D-robustness)*FRAGILITY_COST+risk;
        if(!Double.isFinite(physical))return rejected(Rejection.INVALID_TRACE);

        return new Evaluation(new Transition(targetGravity,launch,contact,landingBody,hit.etaTicks(),equivalent,damage,
            egress,robustness,risk,physical),Rejection.NONE);
    }

    static double predictedDamagePoints(double vanillaEquivalentFallDistance){
        if(!Double.isFinite(vanillaEquivalentFallDistance)||vanillaEquivalentFallDistance<=3.0D)return 0.0D;
        return Math.max(0.0D,vanillaEquivalentFallDistance-3.0D);
    }

    /** Strongly non-linear but still finite: severe/lethal impact is a cost, not a geometry veto. */
    static double riskCost(double vanillaEquivalentFallDistance,double currentHealth){
        if(!Double.isFinite(vanillaEquivalentFallDistance)||!Double.isFinite(currentHealth)||currentHealth<=0.0D)return MAX_RISK_COST;
        double damage=predictedDamagePoints(vanillaEquivalentFallDistance);
        if(damage<=0.0D)return 0.0D;
        double health=Math.max(.5D,currentHealth);
        double fraction=damage/health;
        double cost=damage*4.0D+60.0D*fraction*fraction;
        if(fraction>.5D)cost+=160.0D*Math.pow(fraction-.5D,3.0D);
        if(fraction>=1.0D)cost+=1000.0D+1000.0D*(fraction-1.0D);
        return Math.min(MAX_RISK_COST,cost);
    }

    static int egressDirections(LivingEntity mob,Direction gravity,AABB landingBody){
        int result=0;
        for(Direction tangent:Direction.values()){
            if(tangent.getAxis()==gravity.getAxis())continue;
            Vec3 step=new Vec3(tangent.getStepX(),tangent.getStepY(),tangent.getStepZ()).scale(EGRESS_STEP);
            AABB moved=landingBody.move(step);
            if(known(mob,moved)&&MobGravity.fits(mob,moved)&&hasSupport(mob,gravity,moved))result++;
        }
        return result;
    }

    private static boolean hasSupport(LivingEntity mob,Direction gravity,AABB body){
        AABB probe=body.deflate(BODY_EPS);
        Vec3 down=GravityTransition.direction(gravity).scale(SUPPORT_PROBE);
        var hit=LandingSurfaces.sweep(mob,gravity,probe,probe.move(down));
        return hit.isPresent()&&hit.get().support()&&LandingSurfaces.revalidate(mob,gravity,hit.get().contact());
    }

    private static Rejection preflight(LivingEntity mob,Direction targetGravity,int horizonTicks){
        if(mob==null||targetGravity==null||horizonTicks<1||horizonTicks>TrajectoryPrediction.MAX_TICKS)return Rejection.INVALID_TRACE;
        if(mob instanceof Player||!mob.isAlive()||mob.isPassenger()||mob.isVehicle()||mob.isFallFlying()||FluidContext.intersects(mob))
            return Rejection.INCOMPATIBLE_CONTEXT;
        var attribute=mob.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if(attribute==null||!ClingingReoriented.hasEffect(mob))return Rejection.NO_CAPABILITY;
        if(!attribute.getModifiers().isEmpty())return Rejection.FOREIGN_GRAVITY;
        Direction current=GravityDirectionUtil.getOwnGravityDirection(mob);
        if(targetGravity==current)return Rejection.UNCHANGED;
        var state=MobGravity.state(mob);
        if(state.ownership==MobGravity.Ownership.EXTERNAL||state.ownership==MobGravity.Ownership.BORROWED_RIDER
            ||state.ownership==MobGravity.Ownership.NONE&&current!=Direction.DOWN)return Rejection.FOREIGN_GRAVITY;
        return Rejection.NONE;
    }

    private static Vec3 launchPosition(LivingEntity mob,Direction targetGravity){
        Vec3 current=mob.position();
        var dimensions=mob.getDimensions(mob.getPose());
        AABB direct=RotationUtil.makeBoxFromDimensions(dimensions,targetGravity,current);
        if(MobGravity.fits(mob,direct))return current;
        Vec3 centered=RotationUtil.getCenterAlignedPosition(mob.getBoundingBox(),dimensions,targetGravity);
        if(centered.distanceToSqr(current)<=1.0E-12D)return null;
        AABB centeredBody=RotationUtil.makeBoxFromDimensions(dimensions,targetGravity,centered);
        return MobGravity.fits(mob,centeredBody)?centered:null;
    }

    private static boolean known(LivingEntity mob,AABB box){
        if(mob==null||box==null||!Double.isFinite(box.minX+box.minY+box.minZ+box.maxX+box.maxY+box.maxZ))return false;
        var level=mob.level();
        if(box.minY<level.getMinY()||box.maxY>level.getMaxY()+1||!level.getWorldBorder().isWithinBounds(box))return false;
        int y=Math.max(level.getMinY(),Math.min(level.getMaxY(),(int)Math.floor((box.minY+box.maxY)*.5D)));
        int minChunkX=((int)Math.floor(box.minX))>>4,maxChunkX=((int)Math.floor(box.maxX))>>4;
        int minChunkZ=((int)Math.floor(box.minZ))>>4,maxChunkZ=((int)Math.floor(box.maxZ))>>4;
        for(int x=minChunkX;x<=maxChunkX;x++)for(int z=minChunkZ;z<=maxChunkZ;z++)
            if(!level.hasChunkAt(new BlockPos(x<<4,y,z<<4)))return false;
        return true;
    }

    private static Evaluation rejected(Rejection rejection){return new Evaluation(null,rejection);}
}
