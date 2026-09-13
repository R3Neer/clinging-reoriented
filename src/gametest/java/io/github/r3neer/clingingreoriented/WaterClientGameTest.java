package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class WaterClientGameTest implements FabricClientGameTest {
    private static void quickTap(ClientGameTestContext context){
        context.getInput().holdKey(options->options.keyJump);context.waitTicks(1);
        context.getInput().releaseKey(options->options.keyJump);context.waitTicks(1);
    }

    private static float[] localRotationFor(Direction gravity,Vec3 worldLook){
        var local=RotationUtil.vecWorldToPlayer(worldLook.normalize(),gravity);
        return new float[]{(float)Math.toDegrees(Math.atan2(-local.x,local.z)),(float)Math.toDegrees(Math.asin(-local.y))};
    }

    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-4,78,-4),new BlockPos(4,88,4)))
                    level.setBlockAndUpdate(pos,pos.getY()>=79&&pos.getY()<=87?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(level,new Vec3(.5,82,.5),Vec3.ZERO,-90,0,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.removeAllEffects();
                p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
                ClingingReoriented.data(p).airChangeUsed=false;
            });
            context.waitFor(mc->mc.player!=null&&mc.player.isInWater()&&mc.player.hasEffect(Reorientation.EFFECT));
            context.waitTicks(4);

            long baseline=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            double startY=world.getServer().computeOnServer(server->server.getPlayerList().getPlayers().getFirst().getY());
            context.getInput().holdKey(options->options.keyJump);context.waitTicks(7);
            context.runOnClient(mc->{if(mc.player.getY()<=startY+.02)throw new AssertionError("Single held Space did not preserve upward swimming");});
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);
            long afterSingle=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(afterSingle!=baseline)throw new AssertionError("Single/held underwater Space sent a gravity request");

            quickTap(context);
            long afterFirst=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(afterFirst!=baseline)throw new AssertionError("First underwater tap sent a gravity request");

            context.getInput().holdKey(options->options.keyJump);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.EAST);
            long firstTurn=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(firstTurn<=baseline)throw new AssertionError("Underwater double tap did not send a request");
            context.waitTicks(8);
            long held=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(held!=firstTurn)throw new AssertionError("Holding second underwater tap repeated gravity requests");
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);

            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();var r=localRotationFor(Direction.EAST,new Vec3(0,0,1));
                p.teleport(new TeleportTransition(server.overworld(),p.position(),Vec3.ZERO,r[0],r[1],TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            });
            context.waitTicks(3);quickTap(context);
            long newPairFirst=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(newPairFirst!=firstTurn)throw new AssertionError("First tap of a new underwater pair should not request");
            context.getInput().holdKey(options->options.keyJump);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.SOUTH);
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);

            // A rejected spent-Clinging double tap still leaves Vanilla swim ascent untouched.
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                ClingingReoriented.write(p,Direction.DOWN);p.removeAllEffects();
                var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
                p.addEffect(new MobEffectInstance(clinging,1200));
                var s=ClingingReoriented.data(p);s.owned=true;s.visualFrameOwned=true;s.selected=Direction.DOWN;s.airChangeUsed=true;
                p.teleport(new TeleportTransition(server.overworld(),new Vec3(.5,82,.5),Vec3.ZERO,-90,0,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player.isInWater()&&GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.DOWN
                &&ClingingReoriented.hasEffect(mc.player)&&!mc.player.hasEffect(Reorientation.EFFECT));
            context.waitTicks(3);
            long beforeRejected=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            double rejectedStartY=world.getServer().computeOnServer(server->server.getPlayerList().getPlayers().getFirst().getY());
            quickTap(context);context.getInput().holdKey(options->options.keyJump);context.waitTicks(4);
            long rejected=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(rejected<=beforeRejected)throw new AssertionError("Spent-Clinging double tap never reached server authority");
            context.runOnClient(mc->{
                if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.DOWN)throw new AssertionError("Spent underwater Clinging changed gravity");
                if(mc.player.getY()<=rejectedStartY+.02)throw new AssertionError("Rejected underwater turn stole Vanilla ascent");
            });
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);
        }
    }
}
