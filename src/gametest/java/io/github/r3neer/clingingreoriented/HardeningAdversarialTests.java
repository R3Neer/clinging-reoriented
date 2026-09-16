package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Holdouts for the failure modes that motivated the hardening work. */
public final class HardeningAdversarialTests {
    private static Holder<MobEffect> clinging(){
        return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
    }
    private static ServerPlayer player(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(8,12,8)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-10,-8,-10),p.blockPosition().offset(10,12,10)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        return p;
    }

    @GameTest(padding=32) public void retirementPendingRetriesLocally(GameTestHelper h){
        var p=player(h);p.addEffect(new MobEffectInstance(clinging(),400));
        ClingingReoriented.write(p,Direction.EAST);
        var s=ClingingReoriented.data(p);s.owned=true;s.visualFrameOwned=true;s.selected=Direction.EAST;
        Vec3 origin=p.position();

        BlockPos center=p.blockPosition();
        for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,7,5)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        p.removeAllEffects();ClingingReoriented.reconcile(p);
        h.assertTrue(s.retirementPending,"impossible local recovery enters pending state");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.EAST,"pending retirement preserves the current frame");
        h.assertTrue(p.position().equals(origin),"failed recovery does not teleport");

        for(var pos:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,7,5)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        s.nextRetirementAttempt=0;ClingingReoriented.reconcile(p);
        h.assertFalse(s.retirementPending,"retry clears pending once local geometry is valid");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.DOWN,"retry restores DOWN");
        h.assertTrue(p.position().distanceTo(origin)<=4.000001,"retirement never relocates beyond four blocks");
        h.succeed();
    }

    @GameTest(padding=24) public void externalMobWriteRevokesClingingOwnership(GameTestHelper h){
        var pig=h.spawn(EntityTypes.PIG,new BlockPos(6,10,6));pig.setNoAi(true);pig.setNoGravity(true);pig.setOnGround(false);
        pig.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));
        h.assertTrue(MobGravity.ownedTurn(pig,Direction.EAST,true,null),"fixture could not establish owned EAST frame");
        var state=MobGravity.state(pig);state.ownership=MobGravity.Ownership.OWNED_EFFECT;state.ownedDirection=Direction.EAST;
        h.assertTrue(state.ownership==MobGravity.Ownership.OWNED_EFFECT,"effect owns the established gravity frame");

        GravityDirectionUtil.setGravityDirection(pig,Direction.NORTH);
        h.assertTrue(state.ownership==MobGravity.Ownership.EXTERNAL,"external write revokes Clinging ownership");
        pig.removeAllEffects();MobGravity.tick(pig);
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(pig)==Direction.NORTH,"effect expiry preserves the external frame");
        h.succeed();
    }

    @GameTest(padding=40) public void relocationFailureLeavesRootAndPassengerUntouched(GameTestHelper h){
        var p=player(h);p.getAttribute(Attributes.SCALE).setBaseValue(2);p.refreshDimensions();
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(8,10,8));horse.setNoAi(true);horse.getAttribute(Attributes.SCALE).setBaseValue(2);horse.refreshDimensions();
        h.assertTrue(p.startRiding(horse,true,true),"fixture mounts passenger");horse.positionRider(p);

        Direction direction=Direction.EAST;Vec3 targetRoot=horse.position().add(1,0,0);
        AABB rootBox=RotationUtil.makeBoxFromDimensions(horse.getDimensions(horse.getPose()),direction,targetRoot);
        var offset=horse.getPassengerRidingPosition(p).subtract(horse.position()).subtract(p.getVehicleAttachmentPoint(horse));
        Vec3 passengerTarget=targetRoot.add(RotationUtil.vecPlayerToWorld(offset,direction));
        AABB passengerBox=RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),direction,passengerTarget);
        BlockPos obstacle=null;
        for(var b:BlockPos.betweenClosed(BlockPos.containing(passengerBox.minX,passengerBox.minY,passengerBox.minZ),BlockPos.containing(passengerBox.maxX,passengerBox.maxY,passengerBox.maxZ))){
            AABB block=new AABB(b);
            if(block.intersects(passengerBox.deflate(.01))&&!block.intersects(rootBox)){obstacle=b.immutable();break;}
        }
        h.assertTrue(obstacle!=null,"fixture isolates a passenger-only obstruction");h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        Vec3 rootBefore=horse.position(),passengerBefore=p.position(),velocity=new Vec3(.2,.1,.3);horse.setDeltaMovement(velocity);

        h.assertFalse(MobGravity.relocateTree(horse,direction,targetRoot),"passenger obstruction rejects relocation");
        h.assertTrue(horse.position().equals(rootBefore)&&p.position().equals(passengerBefore),"failed candidate mutates no position");
        h.assertTrue(horse.getDeltaMovement().equals(velocity),"failed candidate preserves momentum");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(horse)==Direction.DOWN,"failed candidate preserves root gravity");
        h.succeed();
    }

    @GameTest public void movingReferenceAcceptsOnlyLatestIntervalOnce(GameTestHelper h){
        var state=new PlayerData();
        state.supportHistory.add(new PlayerData.SupportSample(1,100,new Vec3(0,0,0)));
        state.supportHistory.add(new PlayerData.SupportSample(2,101,new Vec3(1,0,0)));
        Vec3 correction=MovingSurface.consumeLatestCorrection(state,new Vec3(.5,0,0),101);
        h.assertTrue(correction!=null&&correction.distanceTo(new Vec3(.5,0,0))<1e-9,"latest authentic interval is accepted");
        h.assertTrue(MovingSurface.consumeLatestCorrection(state,new Vec3(.5,0,0),101)==null,"same material interval is one-shot");

        var oldInterval=new PlayerData();
        oldInterval.supportHistory.add(new PlayerData.SupportSample(1,100,new Vec3(0,0,0)));
        oldInterval.supportHistory.add(new PlayerData.SupportSample(2,101,new Vec3(1,0,0)));
        oldInterval.supportHistory.add(new PlayerData.SupportSample(3,102,new Vec3(2,0,0)));
        h.assertTrue(MovingSurface.consumeLatestCorrection(oldInterval,new Vec3(.5,0,0),102)==null,"authentic origin from an older interval is rejected");

        var stale=new PlayerData();
        stale.supportHistory.add(new PlayerData.SupportSample(1,1,new Vec3(0,0,0)));
        stale.supportHistory.add(new PlayerData.SupportSample(2,2,new Vec3(1,0,0)));
        h.assertTrue(MovingSurface.consumeLatestCorrection(stale,new Vec3(.5,0,0),30)==null,"stale authentic interval is rejected by age");

        var gap=new PlayerData();
        gap.supportHistory.add(new PlayerData.SupportSample(1,100,new Vec3(0,0,0)));
        gap.supportHistory.add(new PlayerData.SupportSample(3,101,new Vec3(1,0,0)));
        h.assertTrue(MovingSurface.consumeLatestCorrection(gap,new Vec3(.5,0,0),101)==null,"non-consecutive causal samples are rejected");
        h.succeed();
    }
}
