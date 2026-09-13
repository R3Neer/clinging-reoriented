package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class ImpactGameTests {
    private static net.minecraft.server.level.ServerPlayer owned(GameTestHelper h,Vec3 pos,Direction gravity){
        var p=h.makeMockServerPlayerInLevel();p.snapTo(pos);p.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));
        ClingingReoriented.write(p,gravity);var s=ClingingReoriented.data(p);s.owned=true;s.selected=gravity;s.visualFrameOwned=true;
        p.setOnGround(false);p.fallDistance=0;p.setHealth(20);return p;
    }
    private static void floor(GameTestHelper h,int y,net.minecraft.world.level.block.Block block){
        for(int x=1;x<=10;x++)for(int z=1;z<=10;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,y,z)),block.defaultBlockState());
    }

    @GameTest(padding=20)
    public void lateGravityTurnCannotEraseDownwardImpact(GameTestHelper h){
        floor(h,4,Blocks.STONE);var p=owned(h,h.absoluteVec(new Vec3(5.5,5.01,5.5)),Direction.EAST);
        float before=p.getHealth();p.setDeltaMovement(0,-1.2,0);p.move(MoverType.SELF,new Vec3(0,-1.2,0));
        h.assertTrue(p.getHealth()<before,"DOWN surface still hurts after gravity already changed to EAST");
        h.assertTrue(p.fallDistance==0.0F,"impact engine does not leave stale vanilla fall distance");h.succeed();
    }

    @GameTest(padding=20)
    public void lowerRealImpactSpeedProducesLessDamage(GameTestHelper h){
        floor(h,4,Blocks.STONE);
        var fast=owned(h,h.absoluteVec(new Vec3(4.0,5.01,4.0)),Direction.EAST);float beforeFast=fast.getHealth();fast.move(MoverType.SELF,new Vec3(0,-1.2,0));float fastLoss=beforeFast-fast.getHealth();
        var slow=owned(h,h.absoluteVec(new Vec3(7.0,5.01,7.0)),Direction.EAST);float beforeSlow=slow.getHealth();slow.move(MoverType.SELF,new Vec3(0,-.55,0));float slowLoss=beforeSlow-slow.getHealth();
        h.assertTrue(fastLoss>slowLoss,"damage follows real impact speed, so physical braking is rewarded");h.succeed();
    }

    @GameTest(padding=20)
    public void lowSpeedAndTangentialMotionDoNotCreateImpactDamage(GameTestHelper h){
        floor(h,4,Blocks.STONE);var p=owned(h,h.absoluteVec(new Vec3(5.5,5.01,5.5)),Direction.EAST);
        float before=p.getHealth();p.move(MoverType.SELF,new Vec3(.5,0,.5));
        h.assertTrue(p.getHealth()==before,"unblocked/tangential movement is not fall damage");h.succeed();
    }

    @GameTest(padding=20)
    public void hayRetainsVanillaImpactReduction(GameTestHelper h){
        floor(h,4,Blocks.HAY_BLOCK);var hay=owned(h,h.absoluteVec(new Vec3(4.5,5.01,4.5)),Direction.EAST);
        float beforeHay=hay.getHealth();hay.move(MoverType.SELF,new Vec3(0,-1.2,0));float hayLoss=beforeHay-hay.getHealth();
        floor(h,4,Blocks.STONE);var stone=owned(h,h.absoluteVec(new Vec3(7.5,5.01,7.5)),Direction.EAST);
        float beforeStone=stone.getHealth();stone.move(MoverType.SELF,new Vec3(0,-1.2,0));float stoneLoss=beforeStone-stone.getHealth();
        h.assertTrue(stoneLoss>0,"stone fixture produces impact damage");
        h.assertTrue(hayLoss<stoneLoss,"hay block keeps its vanilla fallOn reduction for lateral-gravity impact");h.succeed();
    }

    @GameTest(padding=20)
    public void armedImpactSurvivesOwnershipExitAfterLateTurnAndDamagesOnce(GameTestHelper h){
        floor(h,4,Blocks.STONE);var p=owned(h,h.absoluteVec(new Vec3(5.5,5.01,5.5)),Direction.EAST);
        p.move(MoverType.SELF,new Vec3(0,.01,0));
        h.assertTrue(ImpactState.state(p).armed,"managed airborne motion arms the impact tracker");
        p.removeAllEffects();var owner=ClingingReoriented.data(p);owner.owned=false;owner.visualFrameOwned=false;
        p.setOnGround(false);float before=p.getHealth();p.move(MoverType.SELF,new Vec3(0,-1.2,0));
        h.assertTrue(p.getHealth()<before,"ownership exit after arming cannot erase pending high-speed impact");
        float after=p.getHealth();p.move(MoverType.SELF,Vec3.ZERO);
        h.assertTrue(p.getHealth()==after,"one collision sample cannot be charged twice");h.succeed();
    }
}
