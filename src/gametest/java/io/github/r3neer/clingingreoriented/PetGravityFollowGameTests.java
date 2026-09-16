package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** S06-A direct executor tests before FollowOwnerGoal is allowed to delegate to it. */
public final class PetGravityFollowGameTests {
    @GameTest(padding=64)
    public void blockedOwnerRouteApproachesThenCommitsOnlyAtFrontierAndConfirmsLanding(GameTestHelper h){
        var owner=owner(h,new Vec3(14,10,5));Wolf wolf=wolf(h,owner);
        wall(h,9);
        var state=new PetGravityFollow.State();

        h.assertTrue(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),"blocked owner route did not produce a special transition plan");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.APPROACH,"special wake did not enter APPROACH");
        h.assertTrue(state.plan()!=null&&state.plan().kind()==MobGravityLocalPlanner.Kind.TRANSITION,"wake did not cache a transition plan");
        h.assertTrue(state.plan().terminalGravity()==Direction.EAST,"fixture expected EAST wall transition, got "+state.plan().terminalGravity());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"planning remotely changed pet gravity before reaching frontier");

        Vec3 frontier=state.plan().frontier();
        wolf.setPos(frontier.x,frontier.y,frontier.z);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F),"executor relinquished the tick at commit frontier");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.COMMITTED,"frontier revalidation did not enter COMMITTED: "+state.phase());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"committed executor did not apply EAST gravity");
        h.assertTrue(MobGravity.state(wolf).airUsed,"committed transition did not consume the airborne stage");

        var landing=state.plan().transition().landingBody();
        Vec3 landed=RotationUtil.getCenterAlignedPosition(landing,wolf.getDimensions(wolf.getPose()),Direction.EAST);
        wolf.setPos(landed.x,landed.y,landed.z);wolf.setDeltaMovement(Vec3.ZERO);wolf.setOnGround(true);
        h.assertTrue(PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F),"first landing contact should remain owned for debounce");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.LANDING_CONFIRM,"first support tick did not enter LANDING_CONFIRM");

        owner.snapTo(wolf.position());owner.setOnGround(true);owner.setDeltaMovement(Vec3.ZERO);
        h.assertFalse(PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F),"second stable support should release special ownership when owner is already close");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE,"stable landing did not become a new idle support root");
        h.succeed();
    }

    @GameTest(padding=48)
    public void airborneOwnerNeverTriggersNewGravityTransition(GameTestHelper h){
        var owner=owner(h,new Vec3(14,13,5));Wolf wolf=wolf(h,owner);wall(h,9);
        owner.setOnGround(false);owner.setDeltaMovement(Vec3.ZERO);
        var state=new PetGravityFollow.State();
        h.assertFalse(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),"airborne owner woke gravity-transition pursuit");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE,"airborne owner changed executor phase");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"airborne owner remotely changed pet gravity");
        h.succeed();
    }

    @GameTest(padding=48)
    public void ordinaryReachableOwnerDoesNotWakeSpecialExecutorInsideVanillaDeadZone(GameTestHelper h){
        var owner=owner(h,new Vec3(9,10,5));Wolf wolf=wolf(h,owner);
        var state=new PetGravityFollow.State();
        h.assertFalse(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),"ordinary same-surface WALK incorrectly became special gravity follow");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE&&state.plan()==null,"ordinary WALK left special executor state behind");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"ordinary planning changed gravity");
        h.succeed();
    }

    private static Wolf wolf(GameTestHelper h,net.minecraft.server.level.ServerPlayer owner){
        clear(h);floor(h);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
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
