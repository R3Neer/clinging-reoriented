package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class EntitySnapClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        var entityId=new AtomicInteger(-1);var entityUuid=new AtomicReference<java.util.UUID>();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                server.runCommand("summon minecraft:wolf 2 82 0 {NoAI:1b,NoGravity:1b,Tags:[\"clinging_entity_snap_test\"]}");
                for(var entity:server.overworld().getAllEntities())if(entity.getTags().contains("clinging_entity_snap_test") && entity instanceof LivingEntity living){
                    living.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));living.setOnGround(false);living.setYRot(37.0F);living.yRotO=37.0F;
                    entityId.set(living.getId());entityUuid.set(living.getUUID());break;
                }
                if(entityId.get()<0)throw new AssertionError("Could not spawn tracked snap fixture");
            });
            context.waitFor(mc->mc.level!=null && mc.level.getEntity(entityId.get())!=null && mc.level.getEntity(entityId.get()).getUUID().equals(entityUuid.get()));
            world.getServer().runOnServer(server->{
                var entity=server.overworld().getEntity(entityUuid.get());
                if(!(entity instanceof LivingEntity living) || !MobGravity.replay(living,Direction.EAST))throw new AssertionError("Server pet replay failed");
            });
            context.waitFor(mc->{
                var entity=mc.level.getEntity(entityId.get());
                return entity!=null && GravityDirectionUtil.getOwnGravityDirection(entity)==Direction.EAST && VisualTransitions.owns(entity);
            });
            context.runOnClient(mc->{
                var entity=mc.level.getEntity(entityId.get());
                var current=VisualTransitions.current(entity);var target=RotationUtil.getEntityRotationQuaternion(Direction.EAST);
                if(Math.abs(current.dot(target))>.99999F)throw new AssertionError("Owned non-player quarter turn completed immediately instead of using the short snap");
            });
            context.waitFor(mc->{
                var entity=mc.level.getEntity(entityId.get());
                if(entity==null||VisualTransitions.owns(entity))return false;
                return Math.abs(VisualTransitions.current(entity).dot(RotationUtil.getEntityRotationQuaternion(Direction.EAST)))>.99999F;
            });

            world.getServer().runOnServer(server->{
                var entity=server.overworld().getEntity(entityUuid.get());
                if(!(entity instanceof LivingEntity living))throw new AssertionError("Missing foreign-transition fixture");
                GravityDirectionUtil.setGravityDirection(living,Direction.NORTH);MobGravity.tick(living);
            });
            context.waitFor(mc->{var entity=mc.level.getEntity(entityId.get());return entity!=null&&GravityDirectionUtil.getOwnGravityDirection(entity)==Direction.NORTH;});
            context.runOnClient(mc->{
                var entity=mc.level.getEntity(entityId.get());
                if(VisualTransitions.owns(entity))throw new AssertionError("Foreign Gravity Changer transition incorrectly gained Clinging visual ownership");
            });
        }
    }
}
