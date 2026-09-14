package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
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

/** Analysis-only regression: an underwater gravity turn must not surrender the just-created camera HOLD. */
public final class WaterCameraHoldRegressionClientGameTest implements FabricClientGameTest {
    private static final double CAMERA_EPS=0.03D;

    @Override public void runTest(ClientGameTestContext context){
        AtomicReference<Vec3> beforeTurn=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-4,78,-4),new BlockPos(4,88,4)))
                    level.setBlockAndUpdate(pos,pos.getY()>=79&&pos.getY()<=87?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(level,new Vec3(.5,82,.5),Vec3.ZERO,-90.0F,0.0F,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.removeAllEffects();
                p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
                var s=ClingingReoriented.data(p);
                s.owned=true;s.visualFrameOwned=true;s.selected=Direction.DOWN;s.airChangeUsed=false;
            });
            context.waitFor(mc->mc.player!=null&&mc.player.isInWater()&&mc.player.hasEffect(Reorientation.EFFECT));
            context.waitTicks(3);

            quickTap(context); // first rising edge of the underwater pair
            context.runOnClient(mc->beforeTurn.set(cameraForward(mc)));

            context.getInput().holdKey(options->options.keyJump); // second rising edge: request EAST
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.EAST);
            context.getInput().releaseKey(options->options.keyJump);
            context.waitTicks(3); // enough for END_SERVER_TICK LandingState + ordered client packets

            boolean serverHeld=world.getServer().computeOnServer(server->
                ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).freeFlightVisualHeld);
            if(!serverHeld)
                throw new AssertionError("underwater turn lost freeFlightVisualHeld on the server immediately after the turn");

            context.runOnClient(mc->{
                if(!VisualTransitions.holding(mc.player))
                    throw new AssertionError("underwater gravity turn surrendered client camera HOLD");
                Vec3 after=cameraForward(mc);
                if(beforeTurn.get().distanceTo(after)>CAMERA_EPS)
                    throw new AssertionError("underwater gravity turn rotated rendered camera: before="+beforeTurn.get()+", after="+after);
            });
        }
    }

    private static void quickTap(ClientGameTestContext context){
        context.getInput().holdKey(options->options.keyJump);context.waitTicks(1);
        context.getInput().releaseKey(options->options.keyJump);context.waitTicks(1);
    }

    private static Vec3 cameraForward(net.minecraft.client.Minecraft mc){
        Vector3f forward=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
        return new Vec3(forward.x,forward.y,forward.z).normalize();
    }
}
