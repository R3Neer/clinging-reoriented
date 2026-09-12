package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import dev.tr7zw.firstperson.FirstPersonModelCore;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

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
        context.runOnClient(mc->{ClingingReoriented.data(mc.player).visualFrameOwned=false;VisualTransitions.clear();});
    }
}
