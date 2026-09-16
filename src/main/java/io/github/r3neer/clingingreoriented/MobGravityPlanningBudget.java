package io.github.r3neer.clingingreoriented;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Server-thread budget for expensive grounded gravity-planner invocations.
 *
 * S05 already bounds one plan to one mirror path and at most 20 transition forecasts. This class bounds
 * how many such plans may begin in the same level/tick, with an additional local-region cap so a mob horde
 * cannot concentrate all work into one area. It never budgets vanilla navigation or committed-flight physics.
 */
final class MobGravityPlanningBudget {
    static final int GLOBAL_PLANS_PER_TICK=32;
    static final int REGION_PLANS_PER_TICK=4;
    static final int REGION_SHIFT=6; // 64 world blocks per region on X/Z.

    private static final Map<Level,LevelBudget> LEVELS=new WeakHashMap<>();

    static final class LevelBudget {
        long tick=Long.MIN_VALUE;
        int globalUsed;
        final Map<Long,Integer> regionUsed=new HashMap<>();

        boolean tryAcquire(long now,int blockX,int blockZ){
            if(now!=tick){tick=now;globalUsed=0;regionUsed.clear();}
            if(globalUsed>=GLOBAL_PLANS_PER_TICK)return false;
            long key=regionKey(blockX,blockZ);
            int local=regionUsed.getOrDefault(key,0);
            if(local>=REGION_PLANS_PER_TICK)return false;
            globalUsed++;
            regionUsed.put(key,local+1);
            return true;
        }
    }

    private MobGravityPlanningBudget() {}

    static boolean tryAcquire(LivingEntity entity){
        if(entity==null||entity.level().isClientSide())return false;
        Level level=entity.level();
        LevelBudget budget=LEVELS.computeIfAbsent(level,ignored->new LevelBudget());
        return budget.tryAcquire(level.getGameTime(),entity.blockPosition().getX(),entity.blockPosition().getZ());
    }

    static long regionKey(int blockX,int blockZ){
        int rx=blockX>>REGION_SHIFT,rz=blockZ>>REGION_SHIFT;
        return ((long)rx<<32)^(rz&0xffffffffL);
    }
}
