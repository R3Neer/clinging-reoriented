package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** TM holdout for unrestricted local camera pitch during sustained Gravity Fall. */
public final class GravityFallFullSphereClientGameTest implements FabricClientGameTest {
    private static final double EPS=3.0E-3D;

    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(70_000L);
        AtomicReference<Vec3> startForward=new AtomicReference<>();
        AtomicReference<Vec3> exitForward=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-8,79,-8),new BlockPos(8,79,8)))
                    level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleport(new TeleportTransition(level,new Vec3(.5,100,.5),Vec3.ZERO,0.0F,0.0F,TeleportTransition.DO_NOTHING));
                player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getY()-100.0D)<.25D);
            context.runOnClient(mc->{
                GravityFallVisuals.clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setYRot(0.0F);mc.player.yRotO=0.0F;
                mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;
                mc.player.setDeltaMovement(new Vec3(.25,0,0));
                send(mc,sequence,GravityFallSync.Phase.START);
                for(int i=0;i<8;i++){
                    mc.player.setDeltaMovement(new Vec3(.25,0,0));
                    GravityFallVisuals.tick(mc);
                }
                mc.player.setDeltaMovement(Vec3.ZERO);
                if(!GravityFallVisuals.active(mc.player))throw new AssertionError("Gravity Fall did not activate for full-sphere look test");
            });
            context.waitTicks(2);
            context.runOnClient(mc->startForward.set(cameraForward(mc)));

            context.runOnClient(mc->{
                mc.player.turn(0.0D,800.0D);
                assertPitch(mc,120.0F,"first pole crossing was clamped by vanilla");
                assertInterpolationLocal(mc,"first pole crossing split xRot/xRotO");
            });
            context.waitTicks(3);
            context.runOnClient(mc->{
                assertPitch(mc,120.0F,"server/client tick snapped +120 pitch back into vanilla range");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"camera quaternion disagreed with >90 entity look");
            });
            context.takeScreenshot("gravity-fall-full-sphere-120");

            context.runOnClient(mc->{
                mc.player.turn(0.0D,800.0D);
                assertPitch(mc,-120.0F,"second pole crossing did not preserve equivalent full-sphere pitch");
                assertInterpolationLocal(mc,"240-degree normalization introduced an interpolation discontinuity");
            });
            context.waitTicks(3);
            context.runOnClient(mc->{
                assertPitch(mc,-120.0F,"server/client tick snapped -120 pitch back into vanilla range");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"camera quaternion disagreed after 240-degree turn");
            });
            context.takeScreenshot("gravity-fall-full-sphere-240");

            context.runOnClient(mc->{
                mc.player.turn(0.0D,800.0D);
                assertPitch(mc,0.0F,"full 360-degree pitch loop did not return to zero");
                assertInterpolationLocal(mc,"full loop left a stale previous-pitch branch");
            });
            context.waitTicks(3);
            context.runOnClient(mc->{
                assertPitch(mc,0.0F,"360-degree loop was corrected after network ticks");
                assertVec(startForward.get(),cameraForward(mc),"360-degree loop did not return camera to its starting forward vector");
            });
            context.takeScreenshot("gravity-fall-full-sphere-360");

            context.runOnClient(mc->{
                mc.player.turn(0.0D,800.0D);
                assertPitch(mc,120.0F,"exit fixture failed to reach +120 degrees");
            });
            context.waitTicks(1);
            context.runOnClient(mc->exitForward.set(cameraForward(mc)));
            context.takeScreenshot("gravity-fall-full-sphere-before-reset-120");
            context.runOnClient(mc->{
                send(mc,sequence,GravityFallSync.Phase.RESET);
                if(GravityFallVisuals.active(mc.player))throw new AssertionError("RESET left Gravity Fall active");
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                if(Math.abs(mc.player.getXRot())>90.0001F)
                    throw new AssertionError("Gravity Fall exit did not return pitch to vanilla range: "+mc.player.getXRot());
                assertVec(exitForward.get(),cameraForward(mc),"canonical Gravity Fall exit changed look direction");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"entity look and camera diverged after canonical exit");
            });
            context.takeScreenshot("gravity-fall-full-sphere-after-reset-120");

            context.runOnClient(mc->{
                GravityFallVisuals.clear();
                mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;mc.player.setYRot(0.0F);mc.player.yRotO=0.0F;
            });
        }
    }

    private static void send(Minecraft mc,AtomicLong sequence,GravityFallSync.Phase phase){
        GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
            mc.player.getId(),mc.player.getUUID(),phase.ordinal(),-1,0.0F,sequence.incrementAndGet()));
    }

    private static void assertPitch(Minecraft mc,float expected,String label){
        if(Math.abs(mc.player.getXRot()-expected)>1.0E-3F)
            throw new AssertionError(label+": expected "+expected+" got "+mc.player.getXRot());
    }

    private static void assertInterpolationLocal(Minecraft mc,String label){
        float delta=mc.player.getXRot()-mc.player.xRotO;
        if(Math.abs(delta)>1.0E-3F)throw new AssertionError(label+": current="+mc.player.getXRot()+" previous="+mc.player.xRotO);
    }

    private static Vec3 cameraForward(Minecraft mc){
        Vector3f forward=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
        return new Vec3(forward.x,forward.y,forward.z).normalize();
    }

    private static void assertVec(Vec3 expected,Vec3 actual,String label){
        if(expected.distanceTo(actual)>EPS)throw new AssertionError(label+": expected "+expected+" got "+actual);
    }
}
