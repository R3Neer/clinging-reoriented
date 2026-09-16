package io.github.r3neer.clingingreoriented;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/**
 * Shared "entity is actually intersecting a fluid volume" fence.
 *
 * <p>Do not special-case only water/lava here. Modded fluids are the same interaction context for
 * Clinging: once the player's body enters any non-empty fluid volume, surface landing/support
 * presentation yields to fluid movement until the body is clear again.
 */
public final class FluidContext {
    private static final double EPS=1.0E-7D;

    private FluidContext() {}

    public static boolean intersects(LivingEntity entity){
        return entity!=null&&intersects(entity,entity.getBoundingBox());
    }

    /** Same fluid-volume test for a hypothetical body, used by pure planning and safe fallback queries. */
    public static boolean intersects(LivingEntity entity,AABB candidateBody){
        if(entity==null||entity.level()==null||candidateBody==null)return false;
        AABB body=candidateBody.deflate(EPS);
        if(body.getXsize()<=0.0D||body.getYsize()<=0.0D||body.getZsize()<=0.0D)return false;

        int minX=(int)Math.floor(body.minX);
        int maxX=(int)Math.floor(body.maxX);
        int minY=Math.max(entity.level().getMinY(),(int)Math.floor(body.minY));
        int maxY=Math.min(entity.level().getMaxY(),(int)Math.floor(body.maxY));
        int minZ=(int)Math.floor(body.minZ);
        int maxZ=(int)Math.floor(body.maxZ);

        BlockPos.MutableBlockPos pos=new BlockPos.MutableBlockPos();
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
            pos.set(x,y,z);
            var fluid=entity.level().getFluidState(pos);
            if(fluid.isEmpty())continue;
            AABB volume=fluid.getAABB(entity.level(),pos);
            if(volume!=null&&volume.intersects(body))return true;
        }
        return false;
    }
}
