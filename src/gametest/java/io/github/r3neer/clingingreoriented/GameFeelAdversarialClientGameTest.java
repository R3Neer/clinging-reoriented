package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** S05 combined visual holdout: repeated physics turns, retained camera and velocity-owned body frame. */
public final class GameFeelAdversarialClientGameTest implements FabricClientGameTest {
    private static final double EPS=3.0E-3D;

    @Override public void runTest(ClientGameTestContext context){
        AtomicReference<Vec3> camera=new AtomicReference<>();
        AtomicLong visualSequence=new AtomicLong(50_000L);
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-12,76,-12),new BlockPos(12,96,12)))
                    level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(level,new Vec3(.5,86,.5),Vec3.ZERO,180.0F,0.0F,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(false);
                p.removeAllEffects();p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
                ClingingReoriented.write(p,Direction.DOWN);
                var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.DOWN;s.visualFrameOwned=true;
                s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;s.airborneTicks=0;
            });
            context.waitFor(mc->mc.player!=null&&mc.player.hasEffect(Reorientation.EFFECT)&&Math.abs(mc.player.getY()-86.0D)<.25D);
            context.runOnClient(mc->{
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                GravityFallVisuals.clear();camera.set(cameraForward(mc));
            });

            Direction current=Direction.DOWN;
            Direction[] turns={Direction.EAST,Direction.UP,Direction.NORTH,Direction.DOWN,Direction.WEST,Direction.UP};
            for(Direction target:turns){
                Direction previous=current;
                var result=world.getServer().computeOnServer(server->{
                    var p=server.getPlayerList().getPlayers().getFirst();
                    var r=ClingingReoriented.attempt(p,direction(target),GravityTransition.headingFromYaw(previous,p.getYRot()));
                    ClingingReoriented.data(p).airborneTicks=0; // keep this holdout about turn composition, not auto-start timing
                    return r;
                });
                if(result!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("client six-turn fixture rejected "+previous+" -> "+target+": "+result);
                context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==target);
                context.waitFor(mc->VisualTransitions.holding(mc.player));
                context.runOnClient(mc->{
                    if(cameraForward(mc).distanceTo(camera.get())>EPS)throw new AssertionError("camera accumulated movement after physical turn to "+target);
                });
                current=target;
            }
            context.takeScreenshot("s05-six-turn-retained-camera");

            // Begin a client-derived Gravity Fall body while physical gravity is UP. Its body axis
            // follows DOWN velocity, proving that physical gravity is not being reused as body direction.
            context.runOnClient(mc->{
                mc.player.setDeltaMovement(new Vec3(0,-.20,0));
                receive(mc,visualSequence,GravityFallSync.Phase.START,null,0.0F);
                advance(mc,8,new Vec3(0,-.20,0));
                assertVec(new Vec3(0,-1,0),BodyOrientation.bodyUp(body(mc)),"Gravity Fall body followed physical UP instead of DOWN velocity");
                assertVec(camera.get(),cameraForward(mc),"Gravity Fall start moved retained camera");
            });

            var north=world.getServer().computeOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                ClingingReoriented.data(p).airborneTicks=0;
                var result=ClingingReoriented.attempt(p,direction(Direction.NORTH),GravityTransition.headingFromYaw(Direction.UP,p.getYRot()));
                ClingingReoriented.data(p).airborneTicks=0;
                return result;
            });
            if(north!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("physical UP -> NORTH turn failed during Gravity Fall holdout: "+north);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.NORTH);
            context.waitFor(mc->VisualTransitions.holding(mc.player));
            context.runOnClient(mc->{
                advance(mc,1,new Vec3(0,-.20,0));
                assertVec(new Vec3(0,-1,0),BodyOrientation.bodyUp(body(mc)),"physical gravity turn rotated body before velocity changed");
                assertVec(camera.get(),cameraForward(mc),"physical gravity turn moved camera during Gravity Fall");
                advance(mc,1,new Vec3(0,0,-.20));
                assertVec(new Vec3(0,0,-1),BodyOrientation.bodyUp(body(mc)),"body did not follow new NORTH velocity after gravity/velocity decoupling");
                assertVec(camera.get(),cameraForward(mc),"velocity-owned body turn fed back into camera");
            });
            context.takeScreenshot("s05-gravity-fall-velocity-not-gravity");

            context.runOnClient(mc->{receive(mc,visualSequence,GravityFallSync.Phase.RESET,null,0.0F);GravityFallVisuals.clear();});
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);});
        }
    }

    private static Vec3 direction(Direction direction){return new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());}

    private static void receive(net.minecraft.client.Minecraft mc,AtomicLong sequence,GravityFallSync.Phase phase,Direction target,float eta){
        GravityFallVisuals.receive(mc,new GravityFallSync.Visual(mc.player.getId(),mc.player.getUUID(),phase.ordinal(),target==null?-1:target.get3DDataValue(),eta,sequence.incrementAndGet()));
    }

    private static void advance(net.minecraft.client.Minecraft mc,int ticks,Vec3 velocity){
        for(int i=0;i<ticks;i++){mc.player.setDeltaMovement(velocity);GravityFallVisuals.tick(mc);}
        mc.player.setDeltaMovement(Vec3.ZERO);
    }

    private static org.joml.Quaternionf body(net.minecraft.client.Minecraft mc){
        var body=GravityFallVisuals.body(mc.player,0.0F);if(body==null)throw new AssertionError("missing Gravity Fall body in S05 holdout");return body;
    }

    private static Vec3 cameraForward(net.minecraft.client.Minecraft mc){
        Vector3f forward=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
        return new Vec3(forward.x,forward.y,forward.z).normalize();
    }

    private static void assertVec(Vec3 expected,Vec3 actual,String label){if(expected.distanceTo(actual)>EPS)throw new AssertionError(label+": expected="+expected+" actual="+actual);}
}
