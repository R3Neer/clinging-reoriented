package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
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

/** S04 integration gate: real underwater keyboard input follows the camera rather than logical gravity. */
public final class WaterMovementClientGameTest implements FabricClientGameTest {
    private static final double MIN_DISTANCE=.025D;
    private static final double MIN_ALIGNMENT=.82D;
    private static final Vec3 ORIGIN=new Vec3(.5D,82.0D,.5D);
    private static final Vec3 DESIRED_LOOK=new Vec3(.18D,.48D,-.858D).normalize();

    @Override public void runTest(ClientGameTestContext context){
        AtomicReference<Vec3> start=new AtomicReference<>();
        AtomicReference<Vec3> expected=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-8,76,-8),new BlockPos(8,90,8)))
                    level.setBlockAndUpdate(pos,pos.getY()>=77&&pos.getY()<=89?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();
                p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
                float[] rotation=localRotationFor(Direction.EAST,DESIRED_LOOK);
                p.teleport(new TeleportTransition(level,ORIGIN,Vec3.ZERO,rotation[0],rotation[1],TeleportTransition.DO_NOTHING));
                ClingingReoriented.write(p,Direction.EAST);
                var state=ClingingReoriented.data(p);state.owned=true;state.selected=Direction.EAST;state.visualFrameOwned=true;state.airChangeUsed=false;
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
                // Mirror the production turn lifecycle so the client receives ownership, not only the gravity attribute.
                Payloads.publish(p);
            });
            context.waitFor(mc->mc.player!=null&&mc.player.isInWater()&&GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.EAST&&ClingingReoriented.controlsPhysics(mc.player));
            context.waitTicks(30);

            context.runOnClient(mc->{
                Vec3 forward=cameraForward(mc);
                if(Math.abs(forward.y)<.20D)throw new AssertionError("fixture lost camera pitch; W test would not cover 3D swimming: "+forward);
                start.set(mc.player.position());expected.set(forward);
            });
            context.getInput().holdKey(options->options.keyUp);context.waitTicks(6);
            context.getInput().releaseKey(options->options.keyUp);context.waitTicks(1);
            context.runOnClient(mc->assertMoved(mc.player.position().subtract(start.get()),expected.get(),"W/camera-forward"));
            context.runOnClient(mc->{if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("W changed logical EAST gravity");});

            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                float[] rotation=localRotationFor(Direction.EAST,DESIRED_LOOK);
                p.teleport(new TeleportTransition(server.overworld(),ORIGIN,Vec3.ZERO,rotation[0],rotation[1],TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);
            });
            context.waitTicks(4);
            context.runOnClient(mc->{start.set(mc.player.position());expected.set(cameraLeft(mc));});
            context.getInput().holdKey(options->options.keyLeft);context.waitTicks(6);
            context.getInput().releaseKey(options->options.keyLeft);context.waitTicks(1);
            context.runOnClient(mc->assertMoved(mc.player.position().subtract(start.get()),expected.get(),"A/camera-left"));
            context.runOnClient(mc->{if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("A changed logical EAST gravity");});
        }
    }

    private static void assertMoved(Vec3 displacement,Vec3 expected,String label){
        double distance=displacement.length();
        if(!Double.isFinite(distance)||distance<MIN_DISTANCE)throw new AssertionError(label+" produced too little real displacement: "+displacement);
        double alignment=displacement.normalize().dot(expected.normalize());
        if(!Double.isFinite(alignment)||alignment<MIN_ALIGNMENT)throw new AssertionError(label+" followed wrong frame: alignment="+alignment+" displacement="+displacement+" expected="+expected);
    }

    private static float[] localRotationFor(Direction gravity,Vec3 worldLook){
        Vec3 local=RotationUtil.vecWorldToPlayer(worldLook.normalize(),gravity);
        return new float[]{(float)Math.toDegrees(Math.atan2(-local.x,local.z)),(float)Math.toDegrees(Math.asin(-local.y))};
    }

    private static Vec3 cameraForward(net.minecraft.client.Minecraft mc){
        Vector3f value=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
        return new Vec3(value.x,value.y,value.z).normalize();
    }

    private static Vec3 cameraLeft(net.minecraft.client.Minecraft mc){
        Vector3f value=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(-1,0,0));
        return new Vec3(value.x,value.y,value.z).normalize();
    }
}
