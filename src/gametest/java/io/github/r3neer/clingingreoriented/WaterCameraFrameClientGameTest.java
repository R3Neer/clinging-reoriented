package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.client.WaterCameraVisuals;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** S04 visual holdout: water chooses world-up while free and gravity-relative floor-up only on real support. */
public final class WaterCameraFrameClientGameTest implements FabricClientGameTest {
    private static final double UP_DOT=.97D;
    private static final double FORWARD_EPS=.035D;

    @Override public void runTest(ClientGameTestContext context){
        AtomicReference<Vec3> freeForward=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-5,78,-5),new BlockPos(4,88,5)))level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
                for(int y=78;y<=88;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(2,y,z),Blocks.STONE.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(level,new Vec3(-1.0,82,.5),Vec3.ZERO,-90.0F,0.0F,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.removeAllEffects();p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
                ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;s.visualFrameOwned=true;s.airChangeUsed=false;
            });
            context.waitFor(mc->mc.player!=null&&mc.player.isInWater()&&GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.EAST);
            context.waitTicks(30); // let upstream's direct fixture gravity animation settle before measuring our water frame.
            context.runOnClient(mc->{
                if(!WaterCameraVisuals.active(mc.player))throw new AssertionError("water camera owner never activated");
                Vec3 up=cameraUp(mc);if(up.dot(new Vec3(0,1,0))<UP_DOT)throw new AssertionError("free swim camera is not world-up: "+up);
                freeForward.set(cameraForward(mc));
            });
            context.takeScreenshot("s04-water-free-world-up");

            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.teleport(new TeleportTransition(server.overworld(),new Vec3(2.0,82,.5),Vec3.ZERO,p.getYRot(),p.getXRot(),TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);});
            context.waitTicks(10);
            context.runOnClient(mc->{
                if(!WaterCameraVisuals.supported(mc.player))throw new AssertionError("real EAST wall support was not acquired in water");
                Vec3 up=cameraUp(mc);if(up.dot(new Vec3(-1,0,0))<UP_DOT)throw new AssertionError("supported EAST floor did not become camera-up: "+up);
                if(cameraForward(mc).distanceTo(freeForward.get())>FORWARD_EPS)throw new AssertionError("support roll changed camera forward instead of only camera-up");
                if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("water presentation changed logical gravity");
            });
            context.takeScreenshot("s04-water-supported-east");

            world.getServer().runOnServer(server->{var level=server.overworld();for(int y=78;y<=88;y++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(2,y,z),Blocks.WATER.defaultBlockState());var p=server.getPlayerList().getPlayers().getFirst();p.setDeltaMovement(Vec3.ZERO);});
            context.waitTicks(10);
            context.runOnClient(mc->{
                if(WaterCameraVisuals.supported(mc.player))throw new AssertionError("removed support remained latched beyond hysteresis window");
                Vec3 up=cameraUp(mc);if(up.dot(new Vec3(0,1,0))<UP_DOT)throw new AssertionError("free swim did not return continuously to world-up: "+up);
                if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("support release changed logical gravity");
            });
            context.takeScreenshot("s04-water-release-world-up");

            Vec3 beforeTurn=freeForward.get();
            var result=world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var r=ClingingReoriented.attempt(p,new Vec3(0,0,-1),GravityTransition.headingFromYaw(Direction.EAST,p.getYRot()));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);return r;});
            if(result!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("underwater free-swim gravity turn fixture failed: "+result);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.NORTH);context.waitTicks(8);
            context.runOnClient(mc->{
                Vec3 up=cameraUp(mc);if(up.dot(new Vec3(0,1,0))<UP_DOT)throw new AssertionError("free-swim gravity turn tilted camera out of world-up: "+up);
                if(cameraForward(mc).distanceTo(beforeTurn)>FORWARD_EPS)throw new AssertionError("free-swim gravity turn moved rendered forward despite camera HOLD");
            });
            context.takeScreenshot("s04-water-free-gravity-turn-world-up");
        }finally{WaterCameraVisuals.clear();}
    }

    private static Vec3 cameraForward(net.minecraft.client.Minecraft mc){Vector3f value=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));return new Vec3(value.x,value.y,value.z).normalize();}
    private static Vec3 cameraUp(net.minecraft.client.Minecraft mc){Vector3f value=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,1,0));return new Vec3(value.x,value.y,value.z).normalize();}
}
