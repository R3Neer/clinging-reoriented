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

/** CAM-S01: screen-space handedness must survive full-sphere pole crossings in first and third person. */
public final class GravityFallCameraIntentClientGameTest implements FabricClientGameTest {
    private static final double RESPONSE_EPS=1.0E-4D;

    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(90_000L);
        AtomicReference<Sample> sample=new AtomicReference<>();
        AtomicReference<Double> canonicalDown=new AtomicReference<>();
        AtomicReference<Double> canonicalUp=new AtomicReference<>();
        AtomicReference<Double> thirdPersonCanonical=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-12,75,-12),new BlockPos(12,105,12)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleport(new TeleportTransition(level,new Vec3(.5,90,.5),Vec3.ZERO,0.0F,0.0F,TeleportTransition.DO_NOTHING));
                player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getY()-90.0D)<.25D);
            context.runOnClient(mc->{GravityFallVisuals.clear();GravityFallLookState.clear();mc.options.setCameraType(CameraType.FIRST_PERSON);activate(mc,sequence);setLook(mc,0.0F,80.0F);});
            context.waitTicks(3);

            context.runOnClient(mc->sample.set(beginHorizontalSample(mc)));context.waitTicks(2);
            context.runOnClient(mc->canonicalDown.set(horizontalResponse(mc,sample.get(),"first-person +80 baseline")));

            context.runOnClient(mc->setLook(mc,0.0F,100.0F));context.waitTicks(2);
            context.runOnClient(mc->sample.set(beginHorizontalSample(mc)));context.waitTicks(2);
            context.runOnClient(mc->assertSameSign(canonicalDown.get(),horizontalResponse(mc,sample.get(),"first-person +100 pole crossing"),"first-person lower pole"));
            context.takeScreenshot("cam-s01-first-person-past-pole");

            context.runOnClient(mc->setLook(mc,0.0F,-80.0F));context.waitTicks(2);
            context.runOnClient(mc->sample.set(beginHorizontalSample(mc)));context.waitTicks(2);
            context.runOnClient(mc->canonicalUp.set(horizontalResponse(mc,sample.get(),"first-person -80 baseline")));
            context.runOnClient(mc->setLook(mc,0.0F,-100.0F));context.waitTicks(2);
            context.runOnClient(mc->sample.set(beginHorizontalSample(mc)));context.waitTicks(2);
            context.runOnClient(mc->assertSameSign(canonicalUp.get(),horizontalResponse(mc,sample.get(),"first-person -100 pole crossing"),"first-person upper pole"));

            context.runOnClient(mc->{mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);setLook(mc,0.0F,80.0F);});context.waitTicks(3);
            context.runOnClient(mc->sample.set(beginOrbitSample(mc)));context.waitTicks(3);
            context.runOnClient(mc->thirdPersonCanonical.set(orbitResponse(mc,sample.get(),"third-person +80 baseline")));

            context.runOnClient(mc->setLook(mc,0.0F,100.0F));context.waitTicks(3);
            context.runOnClient(mc->sample.set(beginOrbitSample(mc)));context.waitTicks(3);
            context.runOnClient(mc->assertSameSign(thirdPersonCanonical.get(),orbitResponse(mc,sample.get(),"third-person +100 pole crossing"),"third-person orbit lower pole"));
            context.takeScreenshot("cam-s01-third-person-past-pole");

            context.runOnClient(mc->{GravityFallVisuals.clear();GravityFallLookState.clear();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setXRot(0);mc.player.xRotO=0;mc.player.setYRot(0);mc.player.yRotO=0;});
        }
    }

    private static void activate(Minecraft mc,AtomicLong sequence){
        mc.player.setDeltaMovement(new Vec3(0,-.2,0));
        GravityFallVisuals.receive(mc,new GravityFallSync.Visual(mc.player.getId(),mc.player.getUUID(),GravityFallSync.Phase.START.ordinal(),-1,0.0F,sequence.incrementAndGet()));
        for(int i=0;i<8;i++){mc.player.setDeltaMovement(new Vec3(0,-.2,0));GravityFallVisuals.tick(mc);}
        mc.player.setDeltaMovement(Vec3.ZERO);
        if(!GravityFallVisuals.active(mc.player))throw new AssertionError("CAM-S01 could not activate Gravity Fall");
    }

    private static void setLook(Minecraft mc,float yaw,float pitch){
        mc.player.setYRot(yaw);mc.player.yRotO=yaw;mc.player.setXRot(pitch);mc.player.xRotO=pitch;
        GravityFallLookState.reseed(mc.player);
    }

    private static Sample beginHorizontalSample(Minecraft mc){Sample before=new Sample(cameraForward(mc),cameraRight(mc),mc.gameRenderer.mainCamera().position());mc.player.turn(40.0D,0.0D);return before;}
    private static Sample beginOrbitSample(Minecraft mc){Sample before=new Sample(cameraForward(mc),cameraRight(mc),mc.gameRenderer.mainCamera().position());mc.player.turn(40.0D,0.0D);return before;}

    private static double horizontalResponse(Minecraft mc,Sample before,String label){double response=cameraForward(mc).subtract(before.forward()).dot(before.right());if(Math.abs(response)<RESPONSE_EPS)throw new AssertionError(label+" produced no measurable horizontal response");return response;}
    private static double orbitResponse(Minecraft mc,Sample before,String label){double response=mc.gameRenderer.mainCamera().position().subtract(before.position()).dot(before.right());if(Math.abs(response)<RESPONSE_EPS)throw new AssertionError(label+" produced no measurable orbit response");return response;}
    private static void assertSameSign(double canonical,double actual,String label){if(Math.signum(canonical)!=Math.signum(actual))throw new AssertionError(label+" inverted screen-space handedness: canonical="+canonical+" actual="+actual);}

    private record Sample(Vec3 forward,Vec3 right,Vec3 position) {}
    private static Vec3 cameraForward(Minecraft mc){Vector3f v=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));return new Vec3(v.x,v.y,v.z).normalize();}
    private static Vec3 cameraRight(Minecraft mc){Vector3f v=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(1,0,0));return new Vec3(v.x,v.y,v.z).normalize();}
}
