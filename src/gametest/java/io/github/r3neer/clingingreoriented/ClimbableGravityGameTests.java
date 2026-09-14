package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;

/** Integration fence for world-vertical climbables under owned directional gravity. */
public final class ClimbableGravityGameTests {
    @GameTest
    public void lateralGravityIgnoresLadderWhileUpGravityKeepsIt(GameTestHelper h){
        BlockPos ladder=h.absolutePos(new BlockPos(4,4,4));
        h.setBlock(new BlockPos(4,4,5),Blocks.STONE);
        h.setBlock(new BlockPos(4,4,4),Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.NORTH));

        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(new Vec3(ladder.getX()+.5D,ladder.getY(),ladder.getZ()+.5D));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        var state=ClingingReoriented.data(p);state.owned=true;state.selected=Direction.DOWN;state.visualFrameOwned=true;
        ClingingReoriented.write(p,Direction.DOWN);
        h.assertTrue(p.onClimbable(),"DOWN gravity lost vanilla ladder participation");

        ClingingReoriented.write(p,Direction.EAST);state.selected=Direction.EAST;
        h.assertFalse(p.onClimbable(),"lateral EAST gravity was still affected by a world-vertical ladder");

        ClingingReoriented.write(p,Direction.SOUTH);state.selected=Direction.SOUTH;
        h.assertFalse(p.onClimbable(),"lateral SOUTH gravity was still affected by a world-vertical ladder");

        ClingingReoriented.write(p,Direction.UP);state.selected=Direction.UP;
        h.assertTrue(p.onClimbable(),"UP gravity should keep ladder participation so its Y controls can be mirrored");
        h.succeed();
    }
}
