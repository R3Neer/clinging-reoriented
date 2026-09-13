package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class WaterClingingGameTests {
    @GameTest(padding=16)
    public void waterAndBodyContactDoNotRechargeButSeabedSupportDoes(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();
        var feet=h.absoluteVec(new Vec3(5.5,6.0,5.5));
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(2,3,2)),h.absolutePos(new BlockPos(9,10,9))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        p.snapTo(feet);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);
        h.getLevel().setBlockAndUpdate(BlockPos.containing(feet),Blocks.WATER.defaultBlockState());
        h.getLevel().setBlockAndUpdate(BlockPos.containing(feet.add(0,1,0)),Blocks.WATER.defaultBlockState());

        var state=ClingingReoriented.data(p);state.airChangeUsed=true;
        AirChanges.refresh(p);
        h.assertTrue(state.airChangeUsed,"water with a ground flag but no solid feet support must not recharge Clinging");

        var body=p.getBoundingBox();
        var side=new BlockPos((int)Math.floor(body.maxX+.01),BlockPos.containing(body.getCenter()).getY(),BlockPos.containing(body.getCenter()).getZ());
        h.getLevel().setBlockAndUpdate(side,Blocks.STONE.defaultBlockState());
        p.setOnGround(true);AirChanges.refresh(p);
        h.assertTrue(state.airChangeUsed,"solid body/side contact underwater must not recharge Clinging");

        var seabed=BlockPos.containing(feet.add(0,-.01,0));
        h.getLevel().setBlockAndUpdate(seabed,Blocks.STONE.defaultBlockState());
        p.setOnGround(true);p.setDeltaMovement(Vec3.ZERO);AirChanges.refresh(p);
        h.assertFalse(state.airChangeUsed,"standing on a solid seabed block with the feet-side face supported recharges Clinging");
        h.succeed();
    }
}
