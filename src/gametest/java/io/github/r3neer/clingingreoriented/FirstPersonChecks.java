package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import dev.tr7zw.firstperson.FirstPersonModelCore;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Runs against the installed First Person binary without requiring Scale Brews. */
public final class FirstPersonChecks {
    public static void run(ClientGameTestContext context) {
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        long[] sequence={1000};
        for (Direction direction : new Direction[]{Direction.DOWN, Direction.EAST, Direction.WEST, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.DOWN}) {
            context.runOnClient(mc -> {
                ClingingReoriented.data(mc.player).visualFrameOwned=false;
                VisualTransitions.clear();
                GravityDirectionUtil.setGravityDirection(mc.player, direction);
            });
            context.waitFor(mc -> Math.abs(((GravityAnimationEntity)mc.player).gravitychanger$getVisualGravityRotation(direction)
                .dot(RotationUtil.getEntityRotationQuaternion(direction))) > .99999f);
            context.waitTicks(30);
            context.runOnClient(mc -> {
                var player = mc.player;
                var logic = FirstPersonModelCore.instance.getLogicHandler();
                var rotation = ((GravityAnimationEntity)player).gravitychanger$getVisualGravityRotation(direction);
                for (Pose pose : new Pose[]{Pose.STANDING, Pose.CROUCHING}) {
                    player.setPose(pose);
                    for (float yaw : new float[]{0, 90, 180, -90}) {
                        player.yBodyRotO = yaw; player.yBodyRot = yaw;
                        ClingingReoriented.data(player).visualFrameOwned=false;
                        VisualTransitions.clear();
                        logic.updatePositionOffset(player, 1);
                        Vec3 baseline=logic.getOffset();
                        if(!Double.isFinite(baseline.x+baseline.y+baseline.z))
                            throw new AssertionError("First Person produced a non-finite baseline " + direction + " " + pose);
                        ClingingReoriented.data(player).visualFrameOwned=true;
                        Vec3 expected = RotationUtil.vecPlayerToWorld(baseline, rotation);
                        logic.updatePositionOffset(player, 1);
                        if (logic.getOffset().distanceTo(expected) > 1e-5)
                            throw new AssertionError("Clinging-owned body offset mismatch " + direction + " " + pose + " actual=" + logic.getOffset() + " expected=" + expected + " baseline=" + baseline);
                        Vec3 worldOffset = new Vec3(.13, .24, .35);
                        PlayerOffsetHandler handler = (p, delta, original, current) -> current.add(worldOffset);
                        FirstPersonAPI.getPlayerOffsetHandlers().add(handler);
                        try {
                            logic.updatePositionOffset(player, 1);
                            if (logic.getOffset().distanceTo(expected.add(worldOffset)) > 1e-5)
                                throw new AssertionError("External world offset was rotated");
                        } finally { FirstPersonAPI.getPlayerOffsetHandlers().remove(handler); }
                    }
                }
                ClingingReoriented.data(player).visualFrameOwned=false;
                float oldYaw=player.getYRot(), oldYawO=player.yRotO, oldPitch=player.getXRot();
                Direction syntheticTarget=direction.getOpposite();
                var plan=GravityTransition.plan(direction,syntheticTarget,GravityTransition.headingFromYaw(direction,oldYaw));
                VisualTransitions.begin(player,syntheticTarget,plan.yawDelta(),plan.kind().ordinal(),++sequence[0]);
                if(!VisualTransitions.owns(player))throw new AssertionError("Clinging transition epoch was not associated with the player animation");
                VisualTransitions.clear();
                player.setYRot(oldYaw);player.yRotO=oldYawO;player.setXRot(oldPitch);
                player.setPose(Pose.STANDING);
                player.yBodyRotO = 0; player.yBodyRot = 0;
            });
            context.waitTicks(5);
            context.takeScreenshot("firstperson-" + direction.getName());
        }

        AtomicReference<Vec3> cameraBefore=new AtomicReference<>();
        context.runOnClient(mc->{
            var player=mc.player;
            GravityFallVisuals.clear();VisualTransitions.clear();
            GravityDirectionUtil.setGravityDirection(player,Direction.DOWN);
            player.setPose(Pose.STANDING);player.setDeltaMovement(new Vec3(.25,0,0));
            player.setYRot(0.0F);player.yRotO=0.0F;player.setXRot(0.0F);player.xRotO=0.0F;
        });
        // mainCamera consumes entity rotation during the render/tick pipeline. Give it one tick
        // before taking the baseline or the harness compares a stale previous pitch to the new one.
        context.waitTicks(1);
        context.runOnClient(mc->{
            var player=mc.player;
            cameraBefore.set(cameraForward(mc));
            GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                player.getId(),player.getUUID(),GravityFallSync.Phase.START.ordinal(),-1,0.0F,50_001L));
            for(int i=0;i<8;i++){
                player.setDeltaMovement(new Vec3(.25,0,0));
                GravityFallVisuals.tick(mc);
            }
            player.setDeltaMovement(Vec3.ZERO);
            if(!GravityFallVisuals.active(player))throw new AssertionError("Gravity Fall did not activate in First Person holdout");
            if(GravityFallVisuals.extraRoot(player,0.0F)==null)throw new AssertionError("First Person holdout has no Gravity Fall avatar root");
            Vec3 bodyUp=BodyOrientation.bodyUp(GravityFallVisuals.body(player,0.0F));
            if(bodyUp.distanceTo(new Vec3(1,0,0))>3.0E-3D)throw new AssertionError("First Person holdout body was not visibly velocity-aligned: "+bodyUp);
        });
        context.waitTicks(3);
        context.runOnClient(mc->{
            Vec3 after=cameraForward(mc);
            if(after.distanceTo(cameraBefore.get())>2.0E-3D)
                throw new AssertionError("Gravity Fall avatar root fed back into First Person camera: before="+cameraBefore.get()+" after="+after);
            if(!GravityFallVisuals.active(mc.player))throw new AssertionError("Gravity Fall root vanished before First Person render checkpoint");
        });
        context.takeScreenshot("firstperson-gravity-fall-root");

        // TM reproduction matrix for the reported clipping/body-in-camera failure. Keep the
        // Gravity Fall body horizontal while pitching the real First Person camera downward.
        // These are evidence snapshots, not a production workaround: the point is to freeze
        // the broken composition before changing any render math.
        for(float pitch : new float[]{30.0F,45.0F,60.0F,75.0F,89.0F}){
            final float lookPitch=pitch;
            context.runOnClient(mc->{
                var player=mc.player;
                player.setXRot(lookPitch);player.xRotO=lookPitch;
                if(!GravityFallVisuals.active(player))throw new AssertionError("Gravity Fall root vanished during look-down reproduction at pitch="+lookPitch);
                Vec3 bodyUp=BodyOrientation.bodyUp(GravityFallVisuals.body(player,0.0F));
                if(bodyUp.distanceTo(new Vec3(1,0,0))>3.0E-3D)
                    throw new AssertionError("Body frame changed while only camera pitch changed at pitch="+lookPitch+": "+bodyUp);
            });
            context.waitTicks(2);
            context.takeScreenshot("firstperson-gravity-fall-lookdown-"+(int)pitch);
        }

        context.runOnClient(mc->{mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;});
        context.waitTicks(1);
        context.runOnClient(mc->{
            GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                mc.player.getId(),mc.player.getUUID(),GravityFallSync.Phase.LAND.ordinal(),Direction.DOWN.get3DDataValue(),4.0F,50_002L));
            GravityFallVisuals.tick(mc);
            if(!GravityFallVisuals.landing(mc.player))throw new AssertionError("First Person Gravity Fall landing did not enter BODY_LANDING");
        });
        context.waitTicks(2);
        context.runOnClient(mc->{
            Vec3 after=cameraForward(mc);
            if(after.distanceTo(cameraBefore.get())>2.0E-3D)
                throw new AssertionError("First Person BODY_LANDING fed avatar root rotation back into camera: before="+cameraBefore.get()+" after="+after);
            if(!GravityFallVisuals.landing(mc.player))throw new AssertionError("First Person BODY_LANDING ended before partial landing checkpoint");
        });
        context.takeScreenshot("firstperson-gravity-fall-landing");

        context.runOnClient(mc->{mc.player.setXRot(75.0F);mc.player.xRotO=75.0F;});
        context.waitTicks(1);
        context.takeScreenshot("firstperson-gravity-fall-landing-lookdown-75");

        context.runOnClient(mc->{
            GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                mc.player.getId(),mc.player.getUUID(),GravityFallSync.Phase.RESET.ordinal(),-1,0.0F,50_003L));
            GravityFallVisuals.clear();
            ClingingReoriented.data(mc.player).visualFrameOwned=false;
            VisualTransitions.clear();
            mc.player.setDeltaMovement(Vec3.ZERO);
            mc.player.setXRot(0.0F);mc.player.xRotO=0.0F;
        });
    }

    private static Vec3 cameraForward(Minecraft mc){
        Vector3f forward=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
        return new Vec3(forward.x,forward.y,forward.z).normalize();
    }
}
