package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class GravityPolishGameTests {
    private static void clear(GameTestHelper h,BlockPos center,int radius){
        for(var pos:BlockPos.betweenClosed(center.offset(-radius,-radius,-radius),center.offset(radius,radius,radius)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }

    @GameTest(padding=24)
    public void successfulPlayerTurnStartsNewFallSegmentButUnchangedDoesNot(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(5,10,5)));clear(h,p.blockPosition(),6);
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));p.setOnGround(false);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance=13.0F;
        var heading=GravityTransition.headingFromYaw(Direction.DOWN,p.getYRot());
        var result=ClingingReoriented.attempt(p,new Vec3(1,0,0),heading);
        h.assertTrue(result==ClingingReoriented.Result.SUCCESS,"successful turn fixture: "+result);
        h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==Direction.EAST,"fixture changed gravity");
        h.assertTrue(p.fallDistance==0.0F,"successful gravity change resets vanilla fall distance");

        p.fallDistance=7.0F;
        var unchanged=ClingingReoriented.attempt(p,new Vec3(1,0,0),GravityTransition.headingFromYaw(Direction.EAST,p.getYRot()));
        h.assertTrue(unchanged==ClingingReoriented.Result.UNCHANGED,"same direction is unchanged: "+unchanged);
        h.assertTrue(p.fallDistance==7.0F,"unchanged turn must not reset fall distance");
        h.succeed();
    }

    @GameTest(padding=24)
    public void mobAndMountedRootDirectionChangesStartNewFallSegments(GameTestHelper h){
        var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.setNoAi(true);clear(h,wolf.blockPosition(),6);
        wolf.fallDistance=11.0F;
        h.assertTrue(MobGravity.turn(wolf,Direction.EAST,false),"direct owned-style mob turn succeeds");
        h.assertTrue(wolf.fallDistance==0.0F,"mob direction change resets fall distance");
        wolf.fallDistance=6.0F;
        h.assertTrue(MobGravity.turn(wolf,Direction.EAST,false),"same-frame mob commit remains valid");
        h.assertTrue(wolf.fallDistance==6.0F,"same mob gravity does not reset fall distance");

        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(10,10,5));horse.setNoAi(true);clear(h,horse.blockPosition(),5);
        horse.fallDistance=15.0F;horse.setOnGround(false);
        h.assertTrue(MobGravity.borrow(horse,Direction.NORTH),"mounted-root borrow succeeds");
        h.assertTrue(horse.fallDistance==0.0F,"mounted/root gravity change resets fall distance");
        h.succeed();
    }

    @GameTest
    public void visualEpochRemainsMonotonicAcrossDeathRespawn(GameTestHelper h){
        var oldPlayer=h.makeMockServerPlayerInLevel();
        var replacement=h.makeMockServerPlayerInLevel();
        var old=ClingingReoriented.data(oldPlayer);old.visualSequence=37;old.revision=9;
        ServerPlayerEvents.COPY_FROM.invoker().copyFrom(oldPlayer,replacement,false);
        var next=ClingingReoriented.data(replacement);
        h.assertTrue(next.visualSequence==37,"death respawn preserves connection visual epoch");
        h.assertTrue(next.revision==10,"respawn revision advances");
        var plan=GravityTransition.plan(Direction.DOWN,Direction.EAST,GravityTransition.headingFromYaw(Direction.DOWN,0));
        Payloads.visual(replacement,plan);
        h.assertTrue(next.visualSequence==38,"first post-respawn visual packet remains monotonic");
        h.succeed();
    }
}
