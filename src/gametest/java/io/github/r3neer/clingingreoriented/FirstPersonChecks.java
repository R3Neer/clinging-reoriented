package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import dev.tr7zw.firstperson.FirstPersonModelCore;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import dev.tr7zw.firstperson.api.PlayerOffsetHandler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Runs against the installed First Person and Scale Visual Compat binaries. */
public final class FirstPersonChecks {
    public static void run(ClientGameTestContext context) {
        context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        for (Direction direction : new Direction[]{Direction.DOWN, Direction.EAST, Direction.WEST, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.DOWN}) {
            context.runOnClient(mc -> GravityDirectionUtil.setGravityDirection(mc.player, direction));
            context.waitFor(mc -> Math.abs(((GravityAnimationEntity)mc.player).gravitychanger$getVisualGravityRotation(direction)
                .dot(RotationUtil.getEntityRotationQuaternion(direction))) > .99999f);
            // The visual animation uses wall-clock time: a near-target quaternion is
            // still moving between calls. Let it finish before strict vector checks.
            context.waitTicks(30);
            context.runOnClient(mc -> {
                var player = mc.player;
                var logic = FirstPersonModelCore.instance.getLogicHandler();
                var rotation = ((GravityAnimationEntity)player).gravitychanger$getVisualGravityRotation(direction);
                for (double scale : new double[]{.28, 1, 3.88}) {
                    player.getAttribute(Attributes.SCALE).setBaseValue(scale);
                    for (Pose pose : new Pose[]{Pose.STANDING, Pose.CROUCHING}) {
                        player.setPose(pose);
                        for (float yaw : new float[]{0, 90, 180, -90}) {
                            player.yBodyRotO = yaw; player.yBodyRot = yaw;
                            double distance = pose == Pose.STANDING
                                ? .25f + FirstPersonModelCore.instance.getConfig().xOffset / 100f + .10
                                : .27f + FirstPersonModelCore.instance.getConfig().sneakXOffset / 100f;
                            Vec3 local = new Vec3(distance * Math.sin(Math.toRadians(yaw)), 0, -distance * Math.cos(Math.toRadians(yaw))).scale(scale);
                            Vec3 expected = RotationUtil.vecPlayerToWorld(local, rotation);
                            logic.updatePositionOffset(player, 1);
                            if (logic.getOffset().distanceTo(expected) > 1e-5) throw new AssertionError("Body offset mismatch " + direction + " " + pose + " " + scale + " actual=" + logic.getOffset() + " expected=" + expected);
                            Vec3 worldOffset = new Vec3(.13, .24, .35);
                            PlayerOffsetHandler handler = (p, delta, original, current) -> current.add(worldOffset);
                            FirstPersonAPI.getPlayerOffsetHandlers().add(handler);
                            try {
                                logic.updatePositionOffset(player, 1);
                                if (logic.getOffset().distanceTo(expected.add(worldOffset)) > 1e-5) throw new AssertionError("External world offset was rotated");
                            } finally { FirstPersonAPI.getPlayerOffsetHandlers().remove(handler); }
                        }
                    }
                }
                player.getAttribute(Attributes.SCALE).setBaseValue(1);
                player.setPose(Pose.STANDING);
                player.yBodyRotO = 0; player.yBodyRot = 0;
            });
            context.waitTicks(5);
            context.takeScreenshot("firstperson-" + direction.getName());
        }
    }
}
