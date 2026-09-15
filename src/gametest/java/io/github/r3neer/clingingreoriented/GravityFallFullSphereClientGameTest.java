package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.client.GravityFallLookState;
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

/** TM holdout for continuous full-sphere camera loops with a vanilla-compatible entity forward gauge. */
public final class GravityFallFullSphereClientGameTest implements FabricClientGameTest {
    private static final double EPS=3.0E-3D;

    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(70_000L);
        AtomicReference<Vec3> startForward=new AtomicReference<>();
        AtomicReference<Vec3> startUp=new AtomicReference<>();
        AtomicReference<Vec3> exitForward=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-8,79,-8),new BlockPos(8,79,8)))level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleport(new TeleportTransition(level,new Vec3(.5,100,.5),Vec3.ZERO,0.0F,0.0F,TeleportTransition.DO_NOTHING));
                player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getY()-100.0D)<.25D);
            context.runOnClient(mc->{
                GravityFallVisuals.clear();GravityFallLookState.clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setYRot(0.0F);mc.player.yRotO=0.0F;mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;
                mc.player.setDeltaMovement(new Vec3(.25,0,0));send(mc,sequence,GravityFallSync.Phase.START);
                for(int i=0;i<8;i++){mc.player.setDeltaMovement(new Vec3(.25,0,0));GravityFallVisuals.tick(mc);}mc.player.setDeltaMovement(Vec3.ZERO);
                if(!GravityFallVisuals.active(mc.player))throw new AssertionError("Gravity Fall did not activate for full-sphere look test");
            });
            context.waitTicks(2);
            context.runOnClient(mc->{startForward.set(cameraForward(mc));startUp.set(cameraUp(mc));});

            context.runOnClient(mc->{mc.player.turn(0.0D,800.0D);assertCanonicalGauge(mc,"first 120-degree segment");});
            context.waitTicks(3);
            context.runOnClient(mc->{
                assertVec(new Vec3(0,-Math.sqrt(3)/2,-.5),cameraForward(mc),"120-degree camera forward");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"entity forward diverged after first pole crossing");
            });
            context.takeScreenshot("gravity-fall-full-sphere-120");

            context.runOnClient(mc->{mc.player.turn(0.0D,800.0D);assertCanonicalGauge(mc,"240-degree segment");});
            context.waitTicks(3);
            context.runOnClient(mc->{
                assertVec(new Vec3(0,Math.sqrt(3)/2,-.5),cameraForward(mc),"240-degree camera forward");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"entity forward diverged after 240 degrees");
            });
            context.takeScreenshot("gravity-fall-full-sphere-240");

            context.runOnClient(mc->{mc.player.turn(0.0D,800.0D);assertCanonicalGauge(mc,"360-degree segment");});
            context.waitTicks(3);
            context.runOnClient(mc->{
                assertVec(startForward.get(),cameraForward(mc),"360-degree loop did not restore forward");
                assertVec(startUp.get(),cameraUp(mc),"360-degree loop accumulated roll");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"entity forward diverged after full loop");
            });
            context.takeScreenshot("gravity-fall-full-sphere-360");

            context.runOnClient(mc->mc.player.turn(0.0D,800.0D));context.waitTicks(1);
            context.runOnClient(mc->{exitForward.set(cameraForward(mc));assertCanonicalGauge(mc,"pre-reset 120-degree segment");});
            context.takeScreenshot("gravity-fall-full-sphere-before-reset-120");
            context.runOnClient(mc->{send(mc,sequence,GravityFallSync.Phase.RESET);if(GravityFallVisuals.active(mc.player))throw new AssertionError("RESET left Gravity Fall active");});
            context.waitTicks(2);
            context.runOnClient(mc->{
                if(Math.abs(mc.player.getXRot())>90.0001F)throw new AssertionError("Gravity Fall exit left non-vanilla pitch: "+mc.player.getXRot());
                assertVec(exitForward.get(),cameraForward(mc),"Gravity Fall exit changed gaze");
                assertVec(mc.player.getLookAngle(),cameraForward(mc),"entity look and camera diverged after exit");
            });
            context.takeScreenshot("gravity-fall-full-sphere-after-reset-120");

            context.runOnClient(mc->{GravityFallVisuals.clear();GravityFallLookState.clear();mc.player.setXRot(0);mc.player.xRotO=0;mc.player.setYRot(0);mc.player.yRotO=0;});
        }
    }

    private static void send(Minecraft mc,AtomicLong sequence,GravityFallSync.Phase phase){GravityFallVisuals.receive(mc,new GravityFallSync.Visual(mc.player.getId(),mc.player.getUUID(),phase.ordinal(),-1,0.0F,sequence.incrementAndGet()));}
    private static void assertCanonicalGauge(Minecraft mc,String label){if(Math.abs(mc.player.getXRot())>90.0001F)throw new AssertionError(label+" leaked full-sphere state into vanilla pitch: "+mc.player.getXRot());if(Math.abs(mc.player.getXRot()-mc.player.xRotO)>1.0E-3F||Math.abs(mc.player.getYRot()-mc.player.yRotO)>1.0E-3F)throw new AssertionError(label+" split current/previous look gauge");}
    private static Vec3 cameraForward(Minecraft mc){Vector3f v=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));return new Vec3(v.x,v.y,v.z).normalize();}
    private static Vec3 cameraUp(Minecraft mc){Vector3f v=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,1,0));return new Vec3(v.x,v.y,v.z).normalize();}
    private static void assertVec(Vec3 expected,Vec3 actual,String label){if(expected.distanceTo(actual)>EPS)throw new AssertionError(label+": expected "+expected+" got "+actual);}
}
