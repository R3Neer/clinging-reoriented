package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** S08-B integration gates for per-tick gravity-planning budgets. */
public final class MobGravityPlanningBudgetGameTests {
    @GameTest(padding=64,maxTicks=40)
    public void exhaustedRegionDefersIntentAndPlansOnNextTick(GameTestHelper h){
        clear(h);floor(h);wall(h,9);
        Zombie zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,10,5));
        zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setOnGround(true);zombie.setDeltaMovement(Vec3.ZERO);
        zombie.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));

        int acquired=0;
        while(MobGravityPlanningBudget.tryAcquire(zombie)){
            acquired++;
            if(acquired>MobGravityPlanningBudget.REGION_PLANS_PER_TICK)
                h.fail("regional budget exceeded its documented cap");
        }
        h.assertTrue(acquired<=MobGravityPlanningBudget.REGION_PLANS_PER_TICK,
            "fixture could not exhaust a bounded regional budget");

        Vec3 target=h.absoluteVec(new Vec3(14,10,5));
        h.assertTrue(MobGravityNavigation.requestPositionAfterVanilla(zombie,target,1.0D,false),
            "budget exhaustion discarded the blocked navigation intent");
        var state=MobGravityNavigation.state(zombie);
        h.assertTrue(state.phase()==MobGravityNavigation.Phase.WAITING_PLAN,
            "budget exhaustion did not enter WAITING_PLAN: "+state.phase());
        h.assertTrue(state.plan()==null,"deferred intent somehow ran the planner without a token");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,
            "waiting for planner budget changed gravity remotely");

        h.runAfterDelay(1,()->{
            MobGravityNavigation.tick(zombie);
            h.assertTrue(state.phase()==MobGravityNavigation.Phase.APPROACH||state.phase()==MobGravityNavigation.Phase.REVALIDATE,
                "deferred intent did not acquire next-tick budget: "+state.phase());
            h.assertTrue(state.plan()!=null&&state.plan().kind()==MobGravityLocalPlanner.Kind.TRANSITION,
                "deferred intent did not produce its gravity transition after budget reset");
            h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,
                "planning after defer committed gravity before reaching launch frontier");
            h.succeed();
        });
    }

    @GameTest(padding=48)
    public void stoppingGoalWhileWaitingDropsDeferredIntent(GameTestHelper h){
        clear(h);floor(h);wall(h,9);
        Zombie zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,10,5));
        zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setOnGround(true);zombie.setDeltaMovement(Vec3.ZERO);
        zombie.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        while(MobGravityPlanningBudget.tryAcquire(zombie)){}

        Vec3 target=h.absoluteVec(new Vec3(14,10,5));
        h.assertTrue(MobGravityNavigation.requestPositionAfterVanilla(zombie,target,1.0D,false),"fixture did not defer intent");
        h.assertTrue(MobGravityNavigation.state(zombie).phase()==MobGravityNavigation.Phase.WAITING_PLAN,"fixture is not waiting");
        MobGravityNavigation.goalStopped(zombie);
        h.assertFalse(MobGravityNavigation.active(zombie),"stopped goal left a stale WAITING_PLAN owner");
        h.assertTrue(MobGravityNavigation.state(zombie).intent()==null,"stopped goal retained deferred intent");
        h.succeed();
    }

    private static void wall(GameTestHelper h,int x){for(int y=9;y<=16;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void floor(GameTestHelper h){for(int x=0;x<=22;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(26,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
