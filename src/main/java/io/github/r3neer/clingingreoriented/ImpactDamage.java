package io.github.r3neer.clingingreoriented;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/** Converts collision-absorbed speed into the existing vanilla fall-damage/block pipeline. */
public final class ImpactDamage {
    private static final double MIN_EFFECTIVE_FALL_DISTANCE=0.5D;
    private static final double FACE_PROBE=2.0E-4D;
    private ImpactDamage() {}

    /** @return true when vanilla distance accumulation must be suppressed. */
    public static boolean intercept(LivingEntity entity,double fallbackVertical,boolean onGround,BlockState onState,BlockPos onPos){
        var state=ImpactState.state(entity);
        if(!state.armed)return false;
        entity.fallDistance=0.0F;
        if(entity.level().isClientSide())return true;

        Vec3 intended;
        Vec3 actual;
        if(state.moveActive){
            if(state.handledSequence==state.moveSequence)return true;
            state.handledSequence=state.moveSequence;
            intended=state.intended;
            actual=entity.position().subtract(state.start);
        }else{
            // Rare doCheckFallDamage path outside Entity.move: retain safe local vertical semantics.
            intended=new Vec3(0.0D,fallbackVertical,0.0D);actual=onGround?Vec3.ZERO:intended;
        }
        Vec3 absorbed=ImpactPhysics.absorbedVelocity(intended,actual);
        double speed=absorbed.length();
        if(speed<=1.0E-7D)return true;
        double equivalent=ImpactPhysics.vanillaEquivalentFallDistance(speed);
        if(equivalent<MIN_EFFECTIVE_FALL_DISTANCE)return true;

        Optional<Surface> surface=onGround&&onState!=null&&onPos!=null&&!onState.isAir()
            ?Optional.of(new Surface(onPos,onState))
            :impactSurface(entity,absorbed);
        if(surface.isPresent()){
            var hit=surface.get();
            hit.state().getBlock().fallOn(entity.level(),hit.state(),hit.pos(),entity,equivalent);
        }else{
            entity.causeFallDamage(equivalent,1.0F,entity.damageSources().fall());
        }
        entity.fallDistance=0.0F;
        return true;
    }

    private record Surface(BlockPos pos,BlockState state) {}

    private static Optional<Surface> impactSurface(LivingEntity entity,Vec3 absorbed){
        Direction direction=dominant(absorbed);
        if(direction==null)return Optional.empty();
        AABB body=entity.getBoundingBox();
        AABB probe=faceProbe(body,direction);
        var level=entity.level();
        for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(probe.minX,probe.minY,probe.minZ),BlockPos.containing(probe.maxX,probe.maxY,probe.maxZ))){
            BlockState block=level.getBlockState(pos);
            if(block.isAir())continue;
            var shape=block.getCollisionShape(level,pos);
            if(shape.isEmpty())continue;
            var world=shape.move(pos.getX(),pos.getY(),pos.getZ());
            if(Shapes.joinIsNotEmpty(world,Shapes.create(probe),BooleanOp.AND))return Optional.of(new Surface(pos.immutable(),block));
        }
        return Optional.empty();
    }

    private static Direction dominant(Vec3 vector){
        double x=Math.abs(vector.x),y=Math.abs(vector.y),z=Math.abs(vector.z);
        if(x<=1.0E-7D&&y<=1.0E-7D&&z<=1.0E-7D)return null;
        if(x>=y&&x>=z)return vector.x>=0?Direction.EAST:Direction.WEST;
        if(y>=z)return vector.y>=0?Direction.UP:Direction.DOWN;
        return vector.z>=0?Direction.SOUTH:Direction.NORTH;
    }

    private static AABB faceProbe(AABB box,Direction side){
        double e=FACE_PROBE;
        return switch(side){
            case DOWN->new AABB(box.minX+e,box.minY-e,box.minZ+e,box.maxX-e,box.minY+e,box.maxZ-e);
            case UP->new AABB(box.minX+e,box.maxY-e,box.minZ+e,box.maxX-e,box.maxY+e,box.maxZ-e);
            case NORTH->new AABB(box.minX+e,box.minY+e,box.minZ-e,box.maxX-e,box.maxY-e,box.minZ+e);
            case SOUTH->new AABB(box.minX+e,box.minY+e,box.maxZ-e,box.maxX-e,box.maxY-e,box.maxZ+e);
            case WEST->new AABB(box.minX-e,box.minY+e,box.minZ+e,box.minX+e,box.maxY-e,box.maxZ-e);
            case EAST->new AABB(box.maxX-e,box.minY+e,box.minZ+e,box.maxX+e,box.maxY-e,box.maxZ-e);
        };
    }
}
