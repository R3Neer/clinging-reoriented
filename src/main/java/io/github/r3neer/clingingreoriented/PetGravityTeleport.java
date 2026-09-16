package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.entity.ai.DirectionalGroundNodeEvaluator;
import com.moigferdsrte.gravitychanger.init.ModAttributes;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Conservative S06-C replacement for vanilla's DOWN-only pet teleport.
 * It only commits a fully habitable support state close to the current owner.
 */
public final class PetGravityTeleport {
    private static final int SEARCH_RADIUS=3;
    private static final double BODY_EPS=1.0E-7D;
    private static final double SUPPORT_PROBE=.05D;
    private static final double OWNER_CLEARANCE=.1D;
    private static final double EXTREME_DISTANCE_SQR=24.0D*24.0D;
    private static final int ATTEMPT_PERIOD_TICKS=20;
    private static final int WALK_RETRY_TICKS=10;

    private record Candidate(Vec3 position,Direction gravity,int egress,double distanceSqr) {}

    private PetGravityTeleport() {}

    /**
     * Owns the far-away branch for powered pets so vanilla cannot apply a DOWN-only teleport while
     * the mob is in a lateral frame. Ordinary navigation continues while separation is not extreme.
     */
    public static boolean handleFarFollow(TamableAnimal pet,LivingEntity owner,double speed){
        if(!handles(pet)||owner==null||!owner.isAlive()||pet.level()!=owner.level()||pet.unableToMoveToOwner()
            ||!pet.shouldTryTeleportToOwner())return false;
        long now=pet.level().getGameTime();
        double distanceSqr=pet.distanceToSqr(owner);
        if(distanceSqr>=EXTREME_DISTANCE_SQR&&AirChanges.grounded(pet)&&AirChanges.grounded(owner)
            &&Math.floorMod(now+pet.getId(),ATTEMPT_PERIOD_TICKS)==0&&tryTeleport(pet,owner))return true;
        if(Math.floorMod(now+pet.getId(),WALK_RETRY_TICKS)==0||pet.getNavigation().isDone())
            pet.getNavigation().moveTo(owner,speed);
        return true;
    }

    /** Purely bounded candidate search followed by one revalidated commit. */
    public static boolean tryTeleport(TamableAnimal pet,LivingEntity owner){
        if(!handles(pet)||owner==null||!owner.isAlive()||pet.level()!=owner.level()||pet.unableToMoveToOwner()
            ||!AirChanges.grounded(pet)||!AirChanges.grounded(owner))return false;
        Direction current=GravityDirectionUtil.getOwnGravityDirection(pet);
        for(Direction gravity:allowedGravities(pet,current)){
            Candidate candidate=findCandidate(pet,owner,gravity);
            if(candidate==null)continue;
            if(commit(pet,candidate))return true;
        }
        return false;
    }

    private static boolean handles(TamableAnimal pet){
        return pet!=null&&!pet.level().isClientSide()&&pet.isAlive()&&!pet.isPassenger()&&!pet.isVehicle()
            &&MobGravity.supported(pet)&&ClingingReoriented.hasEffect(pet)&&!FluidContext.intersects(pet);
    }

    private static List<Direction> allowedGravities(TamableAnimal pet,Direction current){
        ArrayList<Direction> result=new ArrayList<>(6);result.add(current);
        var state=MobGravity.state(pet);
        var attribute=pet.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        boolean canChange=attribute!=null&&attribute.getModifiers().isEmpty()
            &&state.ownership!=MobGravity.Ownership.EXTERNAL&&state.ownership!=MobGravity.Ownership.BORROWED_RIDER
            &&(state.ownership!=MobGravity.Ownership.NONE||current==Direction.DOWN);
        if(!canChange)return result;
        if(current!=Direction.DOWN)result.add(Direction.DOWN);
        for(Direction direction:Direction.values())if(direction!=current&&direction!=Direction.DOWN)result.add(direction);
        return result;
    }

    private static Candidate findCandidate(TamableAnimal pet,LivingEntity owner,Direction gravity){
        BlockPos origin=DirectionalGroundNodeEvaluator.nodePosition(owner.position(),gravity);
        AABB ownerBody=owner.getBoundingBox().inflate(OWNER_CLEARANCE);
        for(int radius=1;radius<=SEARCH_RADIUS;radius++){
            Candidate best=null;
            for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
                if(Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))!=radius)continue;
                BlockPos node=origin.offset(x,y,z);
                Vec3 position=DirectionalGroundNodeEvaluator.entityPosition(node,gravity);
                AABB body=RotationUtil.makeBoxFromDimensions(pet.getDimensions(pet.getPose()),gravity,position);
                if(body.intersects(ownerBody)||!validSupportState(pet,gravity,body))continue;
                int egress=MobGravityPlanner.egressDirections(pet,gravity,body);
                if(egress<=0)continue;
                double distance=position.distanceToSqr(owner.position());
                if(!Double.isFinite(distance))continue;
                Candidate candidate=new Candidate(position,gravity,egress,distance);
                if(best==null||better(candidate,best))best=candidate;
            }
            if(best!=null)return best;
        }
        return null;
    }

    private static boolean better(Candidate a,Candidate b){
        if(a.distanceSqr()<b.distanceSqr()-1.0E-9D)return true;
        if(Math.abs(a.distanceSqr()-b.distanceSqr())<=1.0E-9D&&a.egress()>b.egress())return true;
        return false;
    }

    private static boolean validSupportState(TamableAnimal pet,Direction gravity,AABB body){
        if(!MobGravity.fits(pet,body)||FluidContext.intersects(pet,body)||!hazardFree(pet,body))return false;
        AABB probe=body.deflate(BODY_EPS);
        Vec3 down=GravityTransition.direction(gravity).scale(SUPPORT_PROBE);
        var hit=LandingSurfaces.sweep(pet,gravity,probe,probe.move(down));
        return hit.isPresent()&&hit.get().support()&&LandingSurfaces.revalidate(pet,gravity,hit.get().contact());
    }

    private static boolean hazardFree(TamableAnimal pet,AABB body){
        AABB probe=body.inflate(.08D);
        BlockPos min=BlockPos.containing(probe.minX,probe.minY,probe.minZ);
        BlockPos max=BlockPos.containing(probe.maxX,probe.maxY,probe.maxZ);
        for(BlockPos pos:BlockPos.betweenClosed(min,max)){
            var block=pet.level().getBlockState(pos).getBlock();
            if(block==Blocks.CACTUS||block==Blocks.MAGMA_BLOCK||block==Blocks.FIRE||block==Blocks.SOUL_FIRE
                ||block==Blocks.CAMPFIRE||block==Blocks.SOUL_CAMPFIRE||block==Blocks.SWEET_BERRY_BUSH
                ||block==Blocks.WITHER_ROSE||block==Blocks.POWDER_SNOW)return false;
        }
        return true;
    }

    private static boolean commit(TamableAnimal pet,Candidate candidate){
        Direction current=GravityDirectionUtil.getOwnGravityDirection(pet);
        AABB body=RotationUtil.makeBoxFromDimensions(pet.getDimensions(pet.getPose()),candidate.gravity(),candidate.position());
        if(!validSupportState(pet,candidate.gravity(),body)||MobGravityPlanner.egressDirections(pet,candidate.gravity(),body)<=0)return false;

        if(candidate.gravity()==current){
            pet.teleportTo(candidate.position().x,candidate.position().y,candidate.position().z);
            pet.setBoundingBox(body);
        }else{
            if(!MobGravity.canOwnEffectTransition(pet))return false;
            var attribute=pet.getAttribute(ModAttributes.GRAVITY_DIRECTION);
            var state=MobGravity.state(pet);
            if(attribute==null||!attribute.getModifiers().isEmpty()||state.ownership==MobGravity.Ownership.EXTERNAL
                ||state.ownership==MobGravity.Ownership.BORROWED_RIDER)return false;
            Vec3 heading=GravityTransition.headingFromYaw(current,pet.getYRot());
            GravityTransition.Plan visual=GravityTransition.plan(current,candidate.gravity(),heading);
            if(!MobGravity.relocateTree(pet,candidate.gravity(),candidate.position()))return false;
            Payloads.visual(pet,visual);GravityTransition.applyYawGauge(pet,visual.yawDelta());
            state.ownership=MobGravity.Ownership.OWNED_EFFECT;state.ownedDirection=candidate.gravity();state.clearBorrow();state.retryAt=0;
        }
        var state=MobGravity.state(pet);state.airUsed=false;
        pet.setDeltaMovement(Vec3.ZERO);pet.setOnGround(true);pet.getNavigation().stop();
        return true;
    }
}
