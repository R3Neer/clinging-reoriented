package io.github.r3neer.clingingreoriented;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Bounded acquisition kernel. It owns selection only, never projectile movement. */
public final class GravityChargeTargeting {
    public static final double RANGE=32.0D;
    public static final double CONE_DEGREES=15.0D;
    private static final double COS_CONE=Math.cos(Math.toRadians(CONE_DEGREES));
    private static final int FAN_RADIUS=2;
    private static final double FAN_TAN=Math.tan(Math.toRadians(CONE_DEGREES));

    private GravityChargeTargeting() {}

    public record Score(double angularError,double distanceSq) implements Comparable<Score>{
        @Override public int compareTo(Score other){
            int angle=Double.compare(angularError,other.angularError);
            return angle!=0?angle:Double.compare(distanceSq,other.distanceSq);
        }
    }
    public record Acquisition(@Nullable Entity entity,@Nullable BlockPos block,boolean directBlock){
        public static Acquisition entity(Entity entity){return new Acquisition(entity,null,false);}
        public static Acquisition block(BlockPos block,boolean direct){return new Acquisition(null,block.immutable(),direct);}
        public static Acquisition none(){return new Acquisition(null,null,false);}
        public boolean present(){return entity!=null||block!=null;}
    }

    static Optional<Score> score(Vec3 rawIntent,Vec3 offset){
        if(!finite(rawIntent)||!finite(offset)||rawIntent.lengthSqr()<1.0E-12D||offset.lengthSqr()<1.0E-12D)return Optional.empty();
        Vec3 intent=rawIntent.normalize();double distanceSq=offset.lengthSqr();double cosine=intent.dot(offset)/Math.sqrt(distanceSq);
        if(!Double.isFinite(cosine)||cosine<COS_CONE)return Optional.empty();
        return Optional.of(new Score(1.0D-Math.min(1.0D,cosine),distanceSq));
    }

    public static Acquisition acquire(ServerLevel level,ShulkerBullet bullet,Vec3 rawIntent){
        Vec3 intent=safeIntent(rawIntent);Vec3 origin=bullet.position();
        BlockHitResult center=clip(level,bullet,origin,intent);
        if(center.getType()!=HitResult.Type.MISS&&level.getBlockState(center.getBlockPos()).is(Blocks.TARGET))return Acquisition.block(center.getBlockPos(),true);

        Entity bestEntity=null;Score bestEntityScore=null;Entity owner=bullet.getOwner();
        for(LivingEntity candidate:level.getEntitiesOfClass(LivingEntity.class,bullet.getBoundingBox().inflate(RANGE),e->e!=owner&&e.isAlive()&&e.canBeHitByProjectile()&&(!(e instanceof Player p)||!p.isSpectator()))){
            Vec3 aim=candidate.getBoundingBox().getCenter();var scored=score(intent,aim.subtract(origin));if(scored.isEmpty()||!visible(level,bullet,origin,aim))continue;
            Score candidateScore=scored.get();if(bestEntityScore==null||candidateScore.compareTo(bestEntityScore)<0){bestEntity=candidate;bestEntityScore=candidateScore;}
        }

        BlockPos bestBlock=null;Score bestBlockScore=null;Set<BlockPos> seen=new HashSet<>();
        Vec3 reference=Math.abs(intent.y)<0.9D?new Vec3(0,1,0):new Vec3(1,0,0);
        Vec3 right=intent.cross(reference).normalize();Vec3 up=right.cross(intent).normalize();
        for(int ix=-FAN_RADIUS;ix<=FAN_RADIUS;ix++)for(int iy=-FAN_RADIUS;iy<=FAN_RADIUS;iy++){
            if(ix==0&&iy==0)continue;
            double sx=FAN_TAN*ix/FAN_RADIUS,sy=FAN_TAN*iy/FAN_RADIUS;
            Vec3 ray=intent.add(right.scale(sx)).add(up.scale(sy)).normalize();BlockHitResult hit=clip(level,bullet,origin,ray);
            if(hit.getType()==HitResult.Type.MISS||!level.getBlockState(hit.getBlockPos()).is(Blocks.TARGET)||!seen.add(hit.getBlockPos()))continue;
            var scored=score(intent,Vec3.atCenterOf(hit.getBlockPos()).subtract(origin));if(scored.isEmpty())continue;
            Score candidateScore=scored.get();if(bestBlockScore==null||candidateScore.compareTo(bestBlockScore)<0){bestBlock=hit.getBlockPos().immutable();bestBlockScore=candidateScore;}
        }
        if(bestEntityScore==null&&bestBlockScore==null)return Acquisition.none();
        if(bestBlockScore!=null&&(bestEntityScore==null||bestBlockScore.compareTo(bestEntityScore)<0))return Acquisition.block(bestBlock,false);
        return Acquisition.entity(bestEntity);
    }

    public static Vec3 safeIntent(Vec3 intent){return finite(intent)&&intent.lengthSqr()>1.0E-12D?intent.normalize():new Vec3(0,0,1);}
    private static boolean finite(Vec3 v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z);}
    private static BlockHitResult clip(ServerLevel level,Entity context,Vec3 origin,Vec3 direction){
        return level.clip(new ClipContext(origin,origin.add(direction.scale(RANGE)),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,context));
    }
    private static boolean visible(ServerLevel level,Entity context,Vec3 origin,Vec3 target){
        BlockHitResult hit=level.clip(new ClipContext(origin,target,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,context));
        return hit.getType()==HitResult.Type.MISS||hit.getLocation().distanceToSqr(origin)+1.0E-6D>=target.distanceToSqr(origin);
    }
}
