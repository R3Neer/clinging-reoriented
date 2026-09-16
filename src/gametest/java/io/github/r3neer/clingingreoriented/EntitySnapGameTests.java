package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntitySnapGameTests {
    @GameTest(padding=32) public void petOwnedTurnPublishesOnlySuccessfulTransitions(GameTestHelper h){
        var p=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(p);wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(false);
        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));wolf.setYRot(37.0F);wolf.yRotO=37.0F;
        var expected=GravityTransition.plan(Direction.DOWN,Direction.EAST,GravityTransition.headingFromYaw(Direction.DOWN,37.0F));
        h.assertTrue(MobGravity.ownedTurn(wolf,Direction.EAST,true,null),"owned pet turn succeeds");
        var state=MobGravity.state(wolf);state.ownership=MobGravity.Ownership.OWNED_EFFECT;state.ownedDirection=Direction.EAST;
        h.assertTrue(state.visualSequence==1L,"successful owned turn advances entity visual sequence once");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"pet commits target gravity");
        h.assertTrue(Math.abs(Mth.wrapDegrees(wolf.getYRot()-(37.0F+expected.yawDelta())))<1.0E-3F,"server pet yaw gauge follows owned transition");
        float sameYaw=wolf.getYRot();long sameSequence=state.visualSequence;
        h.assertTrue(MobGravity.ownedTurn(wolf,Direction.EAST,true,null),"same-direction owned turn is a no-op success");
        h.assertTrue(state.visualSequence==sameSequence && Math.abs(Mth.wrapDegrees(wolf.getYRot()-sameYaw))<1.0E-3F,"same-direction turn sends no visual transition and mutates no yaw");

        AABB target=RotationUtil.makeBoxFromDimensions(wolf.getDimensions(wolf.getPose()),Direction.NORTH,wolf.position()).deflate(.01);
        BlockPos obstacle=BlockPos.containing(target.getCenter());h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        float beforeFailedYaw=wolf.getYRot();long beforeFailedSequence=state.visualSequence;
        h.assertFalse(MobGravity.ownedTurn(wolf,Direction.NORTH,true,null),"collision preflight rejects blocked pet turn");
        h.assertTrue(state.visualSequence==beforeFailedSequence,"failed preflight publishes no entity transition");
        h.assertTrue(Math.abs(Mth.wrapDegrees(wolf.getYRot()-beforeFailedYaw))<1.0E-3F,"failed preflight applies no yaw gauge");

        h.getLevel().setBlockAndUpdate(obstacle,Blocks.AIR.defaultBlockState());long beforeExternal=state.visualSequence;
        GravityDirectionUtil.setGravityDirection(wolf,Direction.UP);MobGravity.tick(wolf);
        h.assertTrue(state.ownership==MobGravity.Ownership.EXTERNAL,"foreign Gravity Changer write relinquishes ownership");
        h.assertTrue(state.visualSequence==beforeExternal,"foreign gravity write never gains Clinging visual ownership");h.succeed();
    }

    @GameTest(padding=40) public void mountedHalfTurnUsesOnePhysicalAxisAndMountGauge(GameTestHelper h){
        var p=player(h);var horse=h.spawn(EntityTypes.HORSE,new BlockPos(4,10,4));horse.setNoAi(true);horse.setNoGravity(true);horse.setYRot(73.0F);horse.yRotO=73.0F;
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));h.assertTrue(p.startRiding(horse,true,true),"fixture mounts rider");horse.positionRider(p);horse.setOnGround(false);p.setYRot(0.0F);p.yRotO=0.0F;
        Vec3 riderHeading=new Vec3(1,0,0);
        var physical=GravityTransition.plan(Direction.DOWN,Direction.UP,riderHeading);
        var mountPlan=GravityTransition.rebase(physical,GravityTransition.headingFromYaw(Direction.DOWN,73.0F));
        h.assertTrue(MountedGravity.attempt(p,new Vec3(0,1,0),riderHeading)==ClingingReoriented.Result.SUCCESS,"mounted opposite turn succeeds");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(horse)==Direction.UP,"root mount commits same target frame");
        h.assertTrue(MobGravity.state(horse).visualSequence==1L,"mount root receives owned entity snap");
        h.assertTrue(Math.abs(Mth.wrapDegrees(horse.getYRot()-(73.0F+mountPlan.yawDelta())))<1.0E-3F,"mount yaw is rebased through rider-selected physical axis");
        h.assertTrue(Math.abs(Mth.wrapDegrees(p.getYRot()-physical.yawDelta()))<1.0E-3F,"rider keeps its own yaw gauge for the same physical plan");
        h.assertTrue(physical.kind()==mountPlan.kind() && physical.axis().distanceToSqr(mountPlan.axis())<1.0E-8,"rider and mount share exact half-turn kind and axis");h.succeed();
    }

    private static ServerPlayer player(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(4,10,4)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-8,-7,-8),p.blockPosition().offset(20,12,20)))h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();return p;
    }
}
