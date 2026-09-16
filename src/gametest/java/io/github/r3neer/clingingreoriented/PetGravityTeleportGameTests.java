package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.entity.ai.DirectionalGroundNodeEvaluator;
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

/** S06-C gates for the gravity-aware replacement of vanilla pet teleport. */
public final class PetGravityTeleportGameTests {
    @GameTest(padding=96)
    public void lateralPetTeleportsOnlyToARealSupportInItsCurrentFrame(GameTestHelper h){
        clear(h);floor(h,0,35);wall(h,6);wall(h,29);
        var owner=owner(h,new Vec3(26,10,5));
        Wolf wolf=eastWolf(h,owner,MobGravity.Ownership.OWNED_EFFECT);
        Vec3 before=wolf.position();

        h.assertTrue(AirChanges.grounded(wolf),"fixture EAST wolf is not really supported before teleport");
        h.assertTrue(wolf.distanceToSqr(owner)>144.0D,"fixture is not far enough to exercise fallback semantics");
        h.assertTrue(PetGravityTeleport.tryTeleport(wolf,owner),"safe EAST support near owner was not found");
        h.assertTrue(!wolf.position().equals(before),"safe teleport reported success without moving the pet");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"same-frame safe teleport changed logical gravity");
        h.assertTrue(MobGravity.state(wolf).ownership==MobGravity.Ownership.OWNED_EFFECT,"same-frame teleport lost effect ownership");
        h.assertTrue(AirChanges.grounded(wolf),"teleported EAST pet did not arrive on real EAST support");
        h.assertTrue(wolf.distanceToSqr(owner)<36.0D,"teleport destination is not actually near the owner: "+wolf.position());
        h.assertTrue(h.getLevel().noCollision(wolf,wolf.getBoundingBox().deflate(1.0E-5D)),"safe teleport placed wolf inside collision geometry");
        h.succeed();
    }

    @GameTest(padding=96)
    public void externalLateralGravityNeverFallsBackToDownTeleport(GameTestHelper h){
        clear(h);floor(h,0,35);wall(h,6);
        var owner=owner(h,new Vec3(26,10,5));
        Wolf wolf=eastWolf(h,owner,MobGravity.Ownership.EXTERNAL);
        Vec3 before=wolf.position();

        h.assertTrue(AirChanges.grounded(wolf),"fixture external EAST wolf is not supported");
        h.assertFalse(PetGravityTeleport.tryTeleport(wolf,owner),"external EAST ownership illegally fell back to another gravity frame");
        h.assertTrue(wolf.position().equals(before),"failed external-gravity fallback moved the pet");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"failed fallback changed external gravity");
        h.assertTrue(MobGravity.state(wolf).ownership==MobGravity.Ownership.EXTERNAL,"failed fallback stole external ownership");
        h.succeed();
    }

    @GameTest(padding=96)
    public void hazardousNearbyFloorIsNotAValidTeleportSupport(GameTestHelper h){
        clear(h);floor(h,0,8);
        var owner=owner(h,new Vec3(26,10,5));
        for(int x=22;x<=30;x++)for(int z=1;z<=9;z++)h.setBlock(new BlockPos(x,9,z),Blocks.MAGMA_BLOCK);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        Vec3 before=wolf.position();

        h.assertFalse(PetGravityTeleport.tryTeleport(wolf,owner),"hazardous magma support was accepted as safe fallback");
        h.assertTrue(wolf.position().equals(before),"rejected hazardous fallback moved the pet");
        h.succeed();
    }

    private static Wolf eastWolf(GameTestHelper h,net.minecraft.server.level.ServerPlayer owner,MobGravity.Ownership ownership){
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        wolf.setNoAi(true);wolf.setNoGravity(true);
        GravityDirectionUtil.setGravityDirection(wolf,Direction.EAST);
        Vec3 position=DirectionalGroundNodeEvaluator.entityPosition(h.absolutePos(new BlockPos(5,10,5)),Direction.EAST);
        wolf.setPos(position.x,position.y,position.z);
        wolf.setBoundingBox(RotationUtil.makeBoxFromDimensions(wolf.getDimensions(wolf.getPose()),Direction.EAST,position));
        wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        var state=MobGravity.state(wolf);state.ownership=ownership;state.ownedDirection=Direction.EAST;
        return wolf;
    }

    private static net.minecraft.server.level.ServerPlayer owner(GameTestHelper h,Vec3 relative){
        var owner=h.makeMockServerPlayerInLevel();owner.setGameMode(GameType.SURVIVAL);owner.snapTo(h.absoluteVec(relative));
        owner.setNoGravity(true);owner.setOnGround(true);owner.setDeltaMovement(Vec3.ZERO);return owner;
    }

    private static void floor(GameTestHelper h,int minX,int maxX){for(int x=minX;x<=maxX;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void wall(GameTestHelper h,int x){for(int y=8;y<=17;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-6,3,-6)),h.absolutePos(new BlockPos(40,20,16))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
