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

/** Analysis-only regression: mouse X must keep the same screen-space sense after crossing a pitch pole. */
public final class GravityFallHorizontalControlRegressionClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(90_000L);
        AtomicReference<Double> uprightResponse=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(server.overworld(),new Vec3(.5,100,.5),Vec3.ZERO,0.0F,0.0F,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getY()-100.0D)<.25D);
            context.runOnClient(mc->{
                GravityFallVisuals.clear();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setYRot(0.0F);mc.player.yRotO=0.0F;
                mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;
                mc.player.setDeltaMovement(new Vec3(.25,0,0));
                GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                    mc.player.getId(),mc.player.getUUID(),GravityFallSync.Phase.START.ordinal(),-1,0.0F,sequence.incrementAndGet()));
                for(int i=0;i<8;i++){
                    mc.player.setDeltaMovement(new Vec3(.25,0,0));
                    GravityFallVisuals.tick(mc);
                }
                mc.player.setDeltaMovement(Vec3.ZERO);
                if(!GravityFallVisuals.active(mc.player))throw new AssertionError("Gravity Fall did not activate");
                mc.player.turn(0.0D,400.0D); // +60 degrees pitch.
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                double response=mouseXResponse(mc,80.0D);
                if(Math.abs(response)<1.0E-4D)throw new AssertionError("upright horizontal response unexpectedly degenerate: "+response);
                uprightResponse.set(response);
                mc.player.turn(-80.0D,0.0D); // restore yaw.
                mc.player.turn(0.0D,400.0D); // +60 -> +120, crossing the pole.
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                double response=mouseXResponse(mc,80.0D);
                double upright=uprightResponse.get();
                if(upright*response<=0.0D)
                    throw new AssertionError("horizontal mouse sense inverted after pitch pole: upright="+upright+", pastPole="+response+", pitch="+mc.player.getXRot());
                mc.player.turn(-80.0D,0.0D);
                GravityFallVisuals.clear();
            });
        }
    }

    private static double mouseXResponse(Minecraft mc,double mouseDelta){
        Vec3 before=mc.player.getLookAngle().normalize();
        Vector3f rightF=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(1,0,0));
        Vec3 screenRight=new Vec3(rightF.x,rightF.y,rightF.z).normalize();
        mc.player.turn(mouseDelta,0.0D);
        Vec3 after=mc.player.getLookAngle().normalize();
        return after.subtract(before).dot(screenRight);
    }
}
