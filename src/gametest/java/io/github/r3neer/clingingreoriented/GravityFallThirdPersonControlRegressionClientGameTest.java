package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Analysis-only: detached chase-camera orbit must keep the same mouse-X handedness across pitch poles. */
public final class GravityFallThirdPersonControlRegressionClientGameTest implements FabricClientGameTest {
    private record OrbitSample(Vec3 cameraPos,Vec3 screenRight) {}

    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(95_000L);
        AtomicReference<OrbitSample> beforeUpright=new AtomicReference<>();
        AtomicReference<Double> uprightResponse=new AtomicReference<>();
        AtomicReference<OrbitSample> beforePastPole=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(server.overworld(),new Vec3(.5,110,.5),Vec3.ZERO,0.0F,0.0F,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getY()-110.0D)<.25D);
            context.runOnClient(mc->{
                GravityFallVisuals.clear();
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setYRot(0.0F);mc.player.yRotO=0.0F;
                mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;
                mc.player.setDeltaMovement(new Vec3(.25,0,0));
                GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                    mc.player.getId(),mc.player.getUUID(),GravityFallSync.Phase.START.ordinal(),-1,0.0F,sequence.incrementAndGet()));
                for(int i=0;i<8;i++){mc.player.setDeltaMovement(new Vec3(.25,0,0));GravityFallVisuals.tick(mc);}
                mc.player.setDeltaMovement(Vec3.ZERO);
                mc.player.turn(0.0D,400.0D); // +60 pitch
            });
            context.waitTicks(2);
            context.runOnClient(mc->{beforeUpright.set(sample(mc));mc.player.turn(80.0D,0.0D);});
            context.waitTicks(2);
            context.runOnClient(mc->{
                double response=orbitResponse(beforeUpright.get(),mc);
                if(Math.abs(response)<1.0E-4D)throw new AssertionError("upright chase orbit unexpectedly degenerate: "+response);
                uprightResponse.set(response);
                mc.player.turn(-80.0D,0.0D);
                mc.player.turn(0.0D,400.0D); // +120 pitch
            });
            context.waitTicks(2);
            context.runOnClient(mc->{beforePastPole.set(sample(mc));mc.player.turn(80.0D,0.0D);});
            context.waitTicks(2);
            context.runOnClient(mc->{
                double response=orbitResponse(beforePastPole.get(),mc);
                if(uprightResponse.get()*response<=0.0D)
                    throw new AssertionError("third-person chase orbit inverted after pitch pole: upright="+uprightResponse.get()+", pastPole="+response+", pitch="+mc.player.getXRot());
                GravityFallVisuals.clear();mc.options.setCameraType(CameraType.FIRST_PERSON);
            });
        }
    }

    private static OrbitSample sample(Minecraft mc){
        Vector3f rightF=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(1,0,0));
        return new OrbitSample(mc.gameRenderer.mainCamera().position(),new Vec3(rightF.x,rightF.y,rightF.z).normalize());
    }

    private static double orbitResponse(OrbitSample before,Minecraft mc){
        return mc.gameRenderer.mainCamera().position().subtract(before.cameraPos()).dot(before.screenRight());
    }
}
