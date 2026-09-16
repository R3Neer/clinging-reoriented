package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** S07-B physical timing gates for delayed airborne reactions. */
public final class MobFlightReactionGameTests {
    @GameTest(padding=40,maxTicks=30)
    public void lateralObstacleInsideReactionWindowIsNotInstantlyDodged(GameTestHelper h){
        clear(h);
        for(int x=1;x<=10;x++)for(int y=8;y<=14;y++)h.setBlock(new BlockPos(x,y,7),Blocks.STONE);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));
        wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(false);
        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));
        GravityDirectionUtil.setGravityDirection(wolf,Direction.EAST);
        var state=MobGravity.state(wolf);state.ownership=MobGravity.Ownership.OWNED_EFFECT;state.ownedDirection=Direction.EAST;
        state.airUsed=true;state.retryAt=0;
        wolf.setDeltaMovement(new Vec3(0.0D,0.0D,1.4D));
        MobFlightReactor.clear(wolf);

        var forecast=MobFlightMonitor.forecast(wolf,10);
        int reactionTicks=MobReactionTime.ticks(wolf);
        h.assertTrue(forecast.status()==MobFlightMonitor.Status.BLOCKING_CONTACT&&forecast.hit()!=null,
            "fixture did not see the lateral wall as a blocking first contact: "+forecast.status());
        h.assertTrue(forecast.hit().etaTicks()<reactionTicks,
            "wall is not actually inside the reaction window: eta="+forecast.hit().etaTicks()+", reaction="+reactionTicks);
        long sequence=state.visualSequence;

        // Perception begins now. The physical collision arrives before the characteristic reaction deadline.
        MobFlightReactor.tick(wolf);
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,
            "mob reacted in the same tick that danger was first perceived");
        h.assertTrue(state.visualSequence==sequence,"same-tick danger perception emitted a correction visual");

        h.runAfterDelay(2,()->{
            double wallZ=h.absolutePos(new BlockPos(1,8,7)).getZ();
            h.assertTrue(wolf.getBoundingBox().maxZ<=wallZ+1.0E-5D,
                "wolf passed through the lateral wall instead of physically colliding: "+wolf.getBoundingBox());
            h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,
                "mob dodged a hazard that arrived before its reaction deadline");
            h.assertTrue(state.visualSequence==sequence,
                "late-impact fixture emitted a gravity correction before the reaction deadline");
            h.succeed();
        });
    }

    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(18,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
