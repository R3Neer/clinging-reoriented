package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** S05 combined visual holdouts: repeated physics turns, retained camera and persistent body attitude. */
public final class GameFeelAdversarialClientGameTest implements FabricClientGameTest {
    private static final double EPS=3.0E-3D;
    private static final float QUAT_EPS=2.0E-4F;
    private static final double AERO_BODY_MAX_OFFSET_RADIANS=Math.toRadians(10.0D);

    @Override public void runTest(ClientGameTestContext context){
        AtomicReference<Vec3> camera=new AtomicReference<>();AtomicLong visualSequence=new AtomicLong(50_000L);AtomicReference<Quaternionf> cancelledLanding=new AtomicReference<>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{var level=server.overworld();for(var pos:BlockPos.betweenClosed(new BlockPos(-12,76,-12),new BlockPos(12,96,12)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);p.teleport(new TeleportTransition(level,new Vec3(.5,86,.5),Vec3.ZERO,180.0F,0.0F,TeleportTransition.DO_NOTHING));p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.setOnGround(false);p.removeAllEffects();p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));ClingingReoriented.write(p,Direction.DOWN);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.DOWN;s.visualFrameOwned=true;s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;s.airborneTicks=0;});
            context.waitFor(mc->mc.player!=null&&mc.player.hasEffect(Reorientation.EFFECT)&&Math.abs(mc.player.getY()-86.0D)<.25D);
            context.runOnClient(mc->{mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);GravityFallVisuals.clear();});
            context.waitTicks(3);
            context.runOnClient(mc->camera.set(cameraForward(mc)));
            Direction current=Direction.DOWN;Direction[] turns={Direction.EAST,Direction.UP,Direction.NORTH,Direction.DOWN,Direction.WEST,Direction.UP};
            for(Direction target:turns){Direction previous=current;var result=world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();var r=ClingingReoriented.attempt(p,direction(target),GravityTransition.headingFromYaw(previous,p.getYRot()));ClingingReoriented.data(p).airborneTicks=0;return r;});if(result!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("client six-turn fixture rejected "+previous+" -> "+target+": "+result);context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==target);context.waitFor(mc->VisualTransitions.holding(mc.player));context.runOnClient(mc->{if(cameraForward(mc).distanceTo(camera.get())>EPS)throw new AssertionError("camera accumulated movement after physical turn to "+target);});current=target;}
            context.takeScreenshot("s05-six-turn-retained-camera");
            context.runOnClient(mc->{mc.player.setDeltaMovement(new Vec3(0,-.20,0));receive(mc,visualSequence,GravityFallSync.Phase.START,null,0.0F);advance(mc,8,new Vec3(0,-.20,0));assertVec(new Vec3(0,-1,0),BodyOrientation.bodyUp(body(mc)),"Gravity Fall body followed physical UP instead of DOWN velocity");assertVec(camera.get(),cameraForward(mc),"Gravity Fall start moved retained camera");Quaternionf stable=new Quaternionf(body(mc));Vec3[] jitter={new Vec3(5e-4,-2e-4,3e-4),new Vec3(-4e-4,3e-4,-2e-4),new Vec3(2e-4,2e-4,-4e-4),Vec3.ZERO};for(int i=0;i<8;i++){mc.player.setDeltaMovement(jitter[i%jitter.length]);GravityFallVisuals.tick(mc);}mc.player.setDeltaMovement(Vec3.ZERO);assertQuat(stable,body(mc),"sub-epsilon velocity jitter changed Gravity Fall twist/frame");assertVec(camera.get(),cameraForward(mc),"zero-jitter hold moved retained camera");});
            context.takeScreenshot("s05-gravity-fall-zero-jitter");
            var north=world.getServer().computeOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();ClingingReoriented.data(p).airborneTicks=0;var result=ClingingReoriented.attempt(p,direction(Direction.NORTH),GravityTransition.headingFromYaw(Direction.UP,p.getYRot()));ClingingReoriented.data(p).airborneTicks=0;return result;});if(north!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("physical UP -> NORTH turn failed during Gravity Fall holdout: "+north);context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.NORTH);context.waitFor(mc->VisualTransitions.holding(mc.player));
            context.runOnClient(mc->{advance(mc,1,new Vec3(0,-.20,0));Vec3 before=BodyOrientation.bodyUp(body(mc));assertVec(new Vec3(0,-1,0),before,"physical gravity turn rotated body before velocity changed");assertVec(camera.get(),cameraForward(mc),"physical gravity turn moved camera during Gravity Fall");Vec3 northVelocity=new Vec3(0,0,-1);double beforeError=angleBetween(before,northVelocity);advance(mc,1,new Vec3(0,0,-.20));Vec3 after=BodyOrientation.bodyUp(body(mc));double afterError=angleBetween(after,northVelocity);double bodyStep=angleBetween(before,after);if(!(afterError<beforeError-Math.toRadians(.25D)))throw new AssertionError("body did not begin gradual weathercocking toward NORTH velocity: beforeDeg="+Math.toDegrees(beforeError)+" afterDeg="+Math.toDegrees(afterError));if(afterError<=AERO_BODY_MAX_OFFSET_RADIANS)throw new AssertionError("body snapped to NORTH velocity instead of preserving attitude continuity: angleDeg="+Math.toDegrees(afterError));double maxStep=GravityFallAerodynamics.MAX_STABILIZE_RADIANS_PER_TICK+GravityFallAerodynamics.MAX_FOLLOW_RADIANS_PER_TICK+Math.toRadians(.5D);if(bodyStep>maxStep)throw new AssertionError("body attitude changed faster than the S02 steering budget: stepDeg="+Math.toDegrees(bodyStep)+" maxDeg="+Math.toDegrees(maxStep));assertVec(camera.get(),cameraForward(mc),"body weathercocking fed back into camera");});
            context.takeScreenshot("s05-gravity-fall-velocity-not-gravity");
            AtomicReference<Quaternionf> landingStart=new AtomicReference<>();context.runOnClient(mc->{landingStart.set(new Quaternionf(VisualTransitions.current(mc.player)));VisualTransitions.land(mc.player,Direction.NORTH,1,(float)LandingTiming.PRESENTATION_TICKS,90_000L);});context.waitTicks(1);
            context.runOnClient(mc->{Quaternionf partial=new Quaternionf(VisualTransitions.current(mc.player));if(equivalent(partial,landingStart.get()))throw new AssertionError("landing cancel holdout never left retained start frame");if(equivalent(partial,RotationUtil.getEntityRotationQuaternion(Direction.NORTH)))throw new AssertionError("landing cancel holdout reached endpoint before cancellation");cancelledLanding.set(partial);VisualTransitions.cancel(mc.player,true,90_001L);if(!VisualTransitions.holding(mc.player))throw new AssertionError("landing cancellation did not retain recovery ownership");assertQuat(cancelledLanding.get(),VisualTransitions.current(mc.player),"cancel snapped instead of beginning recovery from the current partial frame");});context.waitTicks(6);context.runOnClient(mc->{assertQuat(landingStart.get(),VisualTransitions.current(mc.player),"cancelled landing did not recover to the retained pre-landing frame");if(!VisualTransitions.holding(mc.player))throw new AssertionError("recovered landing did not settle into HOLD");});context.takeScreenshot("s05-cancelled-landing-recovers-hold");
            context.runOnClient(mc->{receive(mc,visualSequence,GravityFallSync.Phase.RESET,null,0.0F);GravityFallVisuals.clear();});context.takeScreenshot("s05-language-falling");
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));p.setNoGravity(true);p.setOnGround(false);p.setDeltaMovement(new Vec3(.2,-.25,0));if(!p.tryToStartFallFlying())throw new AssertionError("S05 visual-language fixture could not enter Elytra");});context.waitFor(mc->mc.player.isFallFlying());context.takeScreenshot("s05-language-elytra");world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.stopFallFlying();p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);});
        }
    }
    private static Vec3 direction(Direction direction){return new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());}
    private static void receive(net.minecraft.client.Minecraft mc,AtomicLong sequence,GravityFallSync.Phase phase,Direction target,float eta){GravityFallVisuals.receive(mc,new GravityFallSync.Visual(mc.player.getId(),mc.player.getUUID(),phase.ordinal(),target==null?-1:target.get3DDataValue(),eta,sequence.incrementAndGet()));}
    private static void advance(net.minecraft.client.Minecraft mc,int ticks,Vec3 velocity){for(int i=0;i<ticks;i++){mc.player.setDeltaMovement(velocity);GravityFallVisuals.tick(mc);}mc.player.setDeltaMovement(Vec3.ZERO);}
    private static Quaternionf body(net.minecraft.client.Minecraft mc){var body=GravityFallVisuals.body(mc.player,0.0F);if(body==null)throw new AssertionError("missing Gravity Fall body in S05 holdout");return body;}
    private static Vec3 cameraForward(net.minecraft.client.Minecraft mc){Vector3f forward=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));return new Vec3(forward.x,forward.y,forward.z).normalize();}
    private static void assertVec(Vec3 expected,Vec3 actual,String label){if(expected.distanceTo(actual)>EPS)throw new AssertionError(label+": expected="+expected+" actual="+actual);}
    private static void assertDirectionWithin(Vec3 expected,Vec3 actual,double maxRadians,String label){double angle=angleBetween(expected,actual);if(!Double.isFinite(angle)||angle>maxRadians)throw new AssertionError(label+": angleDeg="+Math.toDegrees(angle)+" expected="+expected.normalize()+" actual="+actual.normalize());}
    private static double angleBetween(Vec3 a,Vec3 b){Vec3 na=a.normalize(),nb=b.normalize();double dot=Math.max(-1.0D,Math.min(1.0D,na.dot(nb)));return Math.acos(dot);}
    private static void assertQuat(Quaternionf expected,Quaternionf actual,String label){if(!equivalent(expected,actual))throw new AssertionError(label+": expected="+expected+" actual="+actual);}
    private static boolean equivalent(Quaternionf a,Quaternionf b){if(a==null||b==null)return false;Quaternionf qa=new Quaternionf(a).normalize(),qb=new Quaternionf(b).normalize();return Math.abs(Math.abs(qa.dot(qb))-1.0F)<QUAT_EPS;}
}