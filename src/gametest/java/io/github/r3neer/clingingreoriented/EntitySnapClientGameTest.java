package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

public final class EntitySnapClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        var entityId=new AtomicInteger(-1);var entityUuid=new AtomicReference<java.util.UUID>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var wolf=EntityTypes.WOLF.create(level,EntitySpawnReason.COMMAND);
                if(wolf==null)throw new AssertionError("Could not create tracked snap fixture");
                wolf.snapTo(2.0,82.0,0.0);wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(false);wolf.setYRot(37.0F);wolf.yRotO=37.0F;
                if(!level.addFreshEntity(wolf))throw new AssertionError("Could not add tracked snap fixture to server level");
                entityId.set(wolf.getId());entityUuid.set(wolf.getUUID());
            });
            context.waitFor(mc->mc.level!=null && mc.level.getEntity(entityId.get()) instanceof LivingEntity living
                && living.getUUID().equals(entityUuid.get()) && GravityDirectionUtil.getOwnGravityDirection(living)==Direction.DOWN);

            // Focused client coverage: enroll a real non-player Gravity Changer animation first,
            // then expose the target frame immediately so the 180 ms Clinging trajectory can
            // be asserted without racing a server->client packet against that short window.
            context.runOnClient(mc->{
                var living=(LivingEntity)mc.level.getEntity(entityId.get());
                VisualTransitions.clear();
                var plan=GravityTransition.plan(Direction.DOWN,Direction.EAST,GravityTransition.headingFromYaw(Direction.DOWN,living.getYRot()));
                VisualTransitions.beginTracked(living,Direction.EAST,plan.yawDelta(),plan.kind().ordinal(),1L);
                if(!VisualTransitions.owns(living))throw new AssertionError("Tracked non-player animation was not enrolled");
                if(!GravityDirectionUtil.setGravityDirection(living,Direction.EAST))throw new AssertionError("Could not expose client target gravity");
                var current=VisualTransitions.current(living);var target=RotationUtil.getEntityRotationQuaternion(Direction.EAST);
                if(Math.abs(current.dot(target))>.99999F)throw new AssertionError("Owned non-player quarter turn completed immediately instead of using the short snap");
            });
            world.getServer().runOnServer(server->{
                var entity=server.overworld().getEntity(entityUuid.get());
                if(!(entity instanceof LivingEntity living) || !GravityDirectionUtil.setGravityDirection(living,Direction.EAST))throw new AssertionError("Could not converge server target gravity");
            });
            context.waitFor(mc->{
                var entity=mc.level.getEntity(entityId.get());
                if(!(entity instanceof LivingEntity living) || VisualTransitions.owns(living))return false;
                return GravityDirectionUtil.getOwnGravityDirection(living)==Direction.EAST
                    && Math.abs(VisualTransitions.current(living).dot(RotationUtil.getEntityRotationQuaternion(Direction.EAST)))>.99999F;
            });

            // A later foreign Gravity Changer write remains outside Clinging presentation ownership.
            world.getServer().runOnServer(server->{
                var entity=server.overworld().getEntity(entityUuid.get());
                if(!(entity instanceof LivingEntity living) || !GravityDirectionUtil.setGravityDirection(living,Direction.NORTH))throw new AssertionError("Missing foreign-transition fixture");
                MobGravity.tick(living);
            });
            context.waitFor(mc->{var entity=mc.level.getEntity(entityId.get());return entity instanceof LivingEntity living&&GravityDirectionUtil.getOwnGravityDirection(living)==Direction.NORTH;});
            context.runOnClient(mc->{
                var entity=mc.level.getEntity(entityId.get());
                if(VisualTransitions.owns(entity))throw new AssertionError("Foreign Gravity Changer transition incorrectly gained Clinging visual ownership");
            });
        }
    }
}
