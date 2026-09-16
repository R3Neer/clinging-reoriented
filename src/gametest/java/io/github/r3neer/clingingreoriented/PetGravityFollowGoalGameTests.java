package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import java.lang.reflect.Field;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** S06-B integration gates for real FollowOwnerGoal delegation, with no breadcrumb history. */
public final class PetGravityFollowGoalGameTests {
    @GameTest(padding=64)
    public void safeGravityPlanWakesFollowGoalInsideVanillaStartDeadZone(GameTestHelper h) throws Exception {
        var owner=owner(h,new Vec3(14,10,5));Wolf wolf=wolf(h,owner,true);wall(h,9);
        h.assertTrue(wolf.distanceToSqr(owner)<100.0D,"fixture must remain inside vanilla 10-block start dead zone");
        var goal=new FollowOwnerGoal(wolf,1.0D,10.0F,2.0F);

        h.assertTrue(goal.canUse(),"safe gravity transition did not wake FollowOwnerGoal inside vanilla dead zone");
        var state=state(goal);
        h.assertTrue(state.phase()==PetGravityFollow.Phase.APPROACH,"woken goal did not own an APPROACH transition: "+state.phase());
        h.assertTrue(state.plan()!=null&&state.plan().terminalGravity()==Direction.EAST,"fixture expected EAST transition plan");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"canUse remotely changed pet gravity");

        goal.start();goal.tick();
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"approach tick changed gravity before frontier");
        h.assertTrue(wolf.getNavigation().getTargetPos()!=null,"special follow did not author the tactical approach route");

        Vec3 frontier=state.plan().frontier();
        wolf.setPos(frontier.x,frontier.y,frontier.z);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        goal.tick();
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"real FollowOwnerGoal did not commit the revalidated EAST transition at frontier");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.COMMITTED,"goal integration lost committed-flight ownership: "+state.phase());
        h.assertTrue(goal.canContinueToUse(),"vanilla navigation completion killed FollowOwnerGoal during committed gravity flight");
        goal.stop();
        h.succeed();
    }

    @GameTest(padding=48)
    public void reachableOwnerInsideVanillaDeadZoneDoesNotWakeSpecialFollow(GameTestHelper h) throws Exception {
        var owner=owner(h,new Vec3(9,10,5));Wolf wolf=wolf(h,owner,true);
        var goal=new FollowOwnerGoal(wolf,1.0D,10.0F,2.0F);
        h.assertTrue(wolf.distanceToSqr(owner)<100.0D,"fixture must be inside vanilla start dead zone");
        h.assertFalse(goal.canUse(),"ordinary same-surface owner incorrectly woke gravity-specific follow");
        var state=state(goal);
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE&&state.plan()==null,"ordinary dead-zone query left special planner state");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"ordinary dead-zone query changed gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void airborneOwnerInsideVanillaDeadZoneDoesNotWakeGravityFollow(GameTestHelper h) throws Exception {
        var owner=owner(h,new Vec3(14,13,5));Wolf wolf=wolf(h,owner,true);wall(h,9);
        owner.setOnGround(false);owner.setDeltaMovement(Vec3.ZERO);
        var goal=new FollowOwnerGoal(wolf,1.0D,10.0F,2.0F);
        h.assertTrue(wolf.distanceToSqr(owner)<100.0D,"fixture must be inside vanilla start dead zone");
        h.assertFalse(goal.canUse(),"airborne owner woke a new support-to-support gravity transition");
        h.assertTrue(state(goal).phase()==PetGravityFollow.Phase.IDLE,"airborne-owner query changed special executor phase");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"airborne owner remotely changed pet gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void effectFreePetRetainsVanillaFollowSemantics(GameTestHelper h) throws Exception {
        var owner=owner(h,new Vec3(14,10,5));Wolf wolf=wolf(h,owner,false);wall(h,9);
        var goal=new FollowOwnerGoal(wolf,1.0D,10.0F,2.0F);
        h.assertTrue(wolf.distanceToSqr(owner)<100.0D,"fixture must be inside vanilla start dead zone");
        h.assertFalse(goal.canUse(),"effect-free pet gained gravity-specific wake semantics");
        h.assertTrue(state(goal).phase()==PetGravityFollow.Phase.IDLE,"effect-free query allocated an active special plan");
        h.succeed();
    }

    private static PetGravityFollow.State state(FollowOwnerGoal goal) throws Exception {
        Field field=FollowOwnerGoal.class.getDeclaredField("clinging$gravityFollow");field.setAccessible(true);
        return (PetGravityFollow.State)field.get(goal);
    }

    private static Wolf wolf(GameTestHelper h,net.minecraft.server.level.ServerPlayer owner,boolean powered){
        clear(h);floor(h);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.tame(owner);
        if(powered)wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        return wolf;
    }

    private static net.minecraft.server.level.ServerPlayer owner(GameTestHelper h,Vec3 relative){
        clear(h);floor(h);
        var owner=h.makeMockServerPlayerInLevel();owner.setGameMode(GameType.SURVIVAL);owner.snapTo(h.absoluteVec(relative));
        owner.setNoGravity(true);owner.setOnGround(true);owner.setDeltaMovement(Vec3.ZERO);return owner;
    }

    private static void wall(GameTestHelper h,int x){for(int y=9;y<=16;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void floor(GameTestHelper h){for(int x=0;x<=22;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(26,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
