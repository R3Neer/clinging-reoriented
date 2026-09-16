package io.github.r3neer.clingingreoriented;

import java.lang.reflect.Field;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** S06-E gates for vanilla goal adapters. Goals own intent; generic gravity navigation owns locomotion. */
public final class MobGravityGoalAdapterGameTests {
    @GameTest(padding=64)
    public void meleeAttackGoalHandsBlockedPartialPathToGravityNavigation(GameTestHelper h) throws Exception {
        Zombie zombie=zombie(h,true);var target=player(h,new Vec3(14,10,5));wall(h,9);zombie.setTarget(target);
        h.assertTrue(zombie.getTarget()==target&&target.isAlive(),"fixture lost its live melee target before canUse");
        h.assertTrue(MobGravityNavigation.requestEntityAfterVanilla(zombie,target,1.0D,false),
            "fixture has no direct safe gravity route for the melee target");
        MobGravityNavigation.goalStopped(zombie);
        h.assertFalse(MobGravityNavigation.active(zombie),"diagnostic gravity route did not release before goal test");

        var goal=new MeleeAttackGoal(zombie,1.0D,true);bypassCanUseCooldown(goal,zombie);
        boolean canUse=goal.canUse();
        h.assertTrue(canUse,"blocked melee goal did not start after vanilla path attempt and gravity wake");
        goal.start();goal.tick();
        h.assertTrue(MobGravityNavigation.active(zombie),"running melee goal did not hand its blocked entity intent to gravity navigation");
        h.assertTrue(MobGravityNavigation.state(zombie).plan()!=null,"melee handoff produced no gravity plan");
        h.assertTrue(goal.canContinueToUse(),"gravity-owned melee goal died immediately after handoff");
        goal.stop();
        h.assertFalse(MobGravityNavigation.active(zombie),"stopping pre-commit melee goal left stale gravity navigation");
        h.succeed();
    }

    @GameTest(padding=64)
    public void effectFreeBlockedMeleeGoalCannotGainGravityLocomotion(GameTestHelper h) throws Exception {
        Zombie zombie=zombie(h,false);var target=player(h,new Vec3(14,10,5));wall(h,9);zombie.setTarget(target);
        var goal=new MeleeAttackGoal(zombie,1.0D,true);bypassCanUseCooldown(goal,zombie);
        boolean canUse=goal.canUse();
        if(canUse){goal.start();goal.tick();}
        h.assertFalse(MobGravityNavigation.active(zombie),"effect-free melee goal gained gravity locomotion");
        if(canUse)goal.stop();
        h.succeed();
    }

    @GameTest(padding=64)
    public void reachableMeleeGoalStaysVanilla(GameTestHelper h) throws Exception {
        Zombie zombie=zombie(h,true);var target=player(h,new Vec3(10,10,5));zombie.setTarget(target);
        h.assertTrue(zombie.getNavigation().createPath(target,0)!=null,"reachable fixture itself produced no vanilla path");
        var goal=new MeleeAttackGoal(zombie,1.0D,true);bypassCanUseCooldown(goal,zombie);
        h.assertTrue(goal.canUse(),"reachable vanilla melee goal unexpectedly failed after cooldown");
        goal.start();goal.tick();
        h.assertFalse(MobGravityNavigation.active(zombie),"reachable melee path was stolen by gravity locomotion");
        goal.stop();h.succeed();
    }

    @GameTest(padding=64)
    public void avoidGoalStartCannotOverwriteAlreadyOwnedGravityEscape(GameTestHelper h) throws Exception {
        Zombie zombie=zombie(h,true);wall(h,9);
        Vec3 escape=h.absoluteVec(new Vec3(14,10,5));
        h.assertTrue(MobGravityNavigation.requestPositionAfterVanilla(zombie,escape,1.0D,false),
            "fixture failed to activate gravity escape intent");
        var state=MobGravityNavigation.state(zombie);var planned=state.plan();
        h.assertTrue(planned!=null,"gravity escape fixture produced no plan");
        MobGravityNavigation.tick(zombie);
        var ownedPath=zombie.getNavigation().getPath();
        h.assertTrue(ownedPath!=null,"gravity escape never authored its approach path");

        @SuppressWarnings({"rawtypes","unchecked"})
        var goal=new AvoidEntityGoal(zombie,Player.class,12.0F,1.0D,1.2D);
        set(goal,"toAvoid",player(h,new Vec3(3,10,5)));
        set(goal,"path",null);
        goal.start();

        h.assertTrue(MobGravityNavigation.active(zombie),"avoid start released an already-owned gravity escape");
        h.assertTrue(state.plan()==planned,"avoid start discarded the active gravity plan");
        h.assertTrue(zombie.getNavigation().getPath()==ownedPath,"avoid start overwrote planner-owned approach path");
        goal.stop();h.succeed();
    }

    private static void bypassCanUseCooldown(MeleeAttackGoal goal,Zombie zombie) throws Exception {
        Field field=MeleeAttackGoal.class.getDeclaredField("lastCanUseCheck");field.setAccessible(true);
        field.setLong(goal,zombie.level().getGameTime()-100L);
    }

    private static void set(Object target,String name,Object value) throws Exception {
        Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }

    private static Zombie zombie(GameTestHelper h,boolean powered){
        clear(h);floor(h);
        Zombie zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,10,5));
        zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setOnGround(true);zombie.setDeltaMovement(Vec3.ZERO);
        if(powered)zombie.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        return zombie;
    }

    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h,Vec3 relative){
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);player.snapTo(h.absoluteVec(relative));
        player.setNoGravity(true);player.setOnGround(true);player.setDeltaMovement(Vec3.ZERO);return player;
    }

    private static void wall(GameTestHelper h,int x){for(int y=9;y<=16;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void floor(GameTestHelper h){for(int x=0;x<=22;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(26,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
