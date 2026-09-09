package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/** Physical jump edges, vanilla Elytra deployment and the real beacon screen. */
public final class GravityControlsClientTest implements FabricClientGameTest {
    private static void tap(ClientGameTestContext c){c.getInput().holdKey(o->o.keyJump);c.waitTicks(2);c.getInput().releaseKey(o->o.keyJump);c.waitTicks(3);}
    @Override public void runTest(ClientGameTestContext c){
        c.runOnClient(mc->{
            var config=io.github.r3neer.clingingreoriented.client.CameraConfig.path();
            try {
                var temporary=java.nio.file.Files.createTempFile("clinging-camera-test-",".json");
                try { for(double seconds:new double[]{1.0,2.0}) {
                    java.nio.file.Files.writeString(temporary,"{\"cameraRotationSeconds\":"+seconds+"}");
                    io.github.r3neer.clingingreoriented.client.CameraConfig.load(temporary);
                    var animation=new com.moigferdsrte.gravitychanger.client.GravityRotationAnimation();
                    animation.getRotation(Direction.DOWN,0);animation.getRotation(Direction.EAST,0);
                    long duration=(long)(seconds*1_000_000_000L);
                    var target=RotationUtil.getEntityRotationQuaternion(Direction.EAST);
                    if(Math.abs(animation.getRotation(Direction.EAST,duration/2).dot(target))>.999f)throw new AssertionError("Camera transition ended before configured duration");
                    if(Math.abs(animation.getRotation(Direction.EAST,duration).dot(target))<.99999f)throw new AssertionError("Camera transition did not finish at configured duration");
                }} finally {java.nio.file.Files.deleteIfExists(temporary);}
            } catch(java.io.IOException failure){throw new AssertionError(failure);}
            finally {io.github.r3neer.clingingreoriented.client.CameraConfig.load(config);}
        });
        try(var world=c.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(server.overworld(),new Vec3(0,150,0),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
                p.addEffect(new MobEffectInstance(Reorientation.EFFECT,10000));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
                ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;
            });
            c.waitFor(mc->mc.player!=null && mc.player.hasEffect(Reorientation.EFFECT) && GravityDirectionUtil.getGravityDirection(mc.player)==Direction.EAST);
            c.waitFor(mc->!mc.player.onGround() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && !mc.player.getAbilities().mayfly);
            c.waitTicks(5);tap(c);c.waitTicks(5);
            c.runOnClient(mc->{if(!mc.player.isFallFlying())throw new AssertionError("Glide did not start: eligible="+GravityInput.elytraWins(mc.player)+" ground="+mc.player.onGround()+" equipment="+mc.player.getItemBySlot(EquipmentSlot.CHEST)+" screen="+mc.gui.screen());});
            c.runOnClient(mc->{if(GravityDirectionUtil.getGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("Elytra deployment lost EAST frame");});
            tap(c);c.waitTicks(5);
            c.runOnClient(mc->{if(!mc.player.isFallFlying() || GravityDirectionUtil.getGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("Space during glide changed gravity");});
            c.takeScreenshot("elytra-east-priority");
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.stopFallFlying();p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);});
            c.waitFor(mc->!mc.player.isFallFlying() && mc.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty());
            c.setScreen(()->{
                var p=net.minecraft.client.Minecraft.getInstance().player;
                var menu=new BeaconMenu(99,p.getInventory());menu.setData(0,1);
                return new BeaconScreen(menu,p.getInventory(),Component.literal("Clinging tier 2"));
            });
            c.waitTicks(3);checkBeacon(c,false);
            c.runOnClient(mc->((BeaconScreen)mc.gui.screen()).getMenu().setData(0,2));
            c.waitTicks(3);checkBeacon(c,true);c.takeScreenshot("beacon-clinging-tier-two");c.setScreen(()->null);
            if(ScaleBridge.PRESENT){
                world.getServer().runOnServer(server->{
                    var p=server.getPlayerList().getPlayers().getFirst();p.removeAllEffects();ClingingReoriented.write(p,Direction.DOWN);
                    p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
                    p.teleport(new TeleportTransition(server.overworld(),new Vec3(0,150,0),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
                    p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow(),10000));
                    var chicken=EntityTypes.CHICKEN.create(server.overworld(),EntitySpawnReason.COMMAND);chicken.setPos(p.position());chicken.setNoGravity(true);chicken.setNoAi(true);
                    chicken.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));server.overworld().addFreshEntity(chicken);p.startRiding(chicken,true,true);
                });
                c.waitFor(mc->mc.player.isPassenger() && !mc.player.hasEffect(Reorientation.EFFECT));
                tap(c);tap(c);
                c.runOnClient(mc->{if(GravityDirectionUtil.getGravityDirection(mc.player.getRootVehicle())!=Direction.DOWN)throw new AssertionError("Mounted Clinging turned mount");});
                world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().addEffect(new MobEffectInstance(Reorientation.EFFECT,10000)));
                c.waitFor(mc->mc.player.hasEffect(Reorientation.EFFECT));
                c.waitFor(mc->Math.abs(((com.moigferdsrte.gravitychanger.client.GravityAnimationEntity)mc.player).gravitychanger$getVisualGravityRotation(Direction.DOWN).dot(RotationUtil.getEntityRotationQuaternion(Direction.DOWN)))>.99999f);
                var expected=new java.util.concurrent.atomic.AtomicReference<Direction>();
                c.runOnClient(mc->{var look=mc.gameRenderer.mainCamera().rotation().transform(new org.joml.Vector3f(0,0,-1));expected.set(io.github.r3neer.clingingreoriented.geometry.LookDirection.select(new Vec3(look.x,look.y,look.z)));});
                tap(c);c.waitTicks(5);
                c.runOnClient(mc->{var actual=GravityDirectionUtil.getGravityDirection(mc.player.getRootVehicle());if(expected.get()==Direction.DOWN || actual!=expected.get())throw new AssertionError("First airborne mounted Space: expected="+expected.get()+", actual="+actual+", rootGround="+mc.player.getRootVehicle().onGround()+", sounds="+ClingingClientGameTest.SOUNDS);});
                // The mounted client owns movement, so exercise real Chicken.aiStep here.
                for(var d:Direction.values()){
                    c.runOnClient(mc->{
                        var chicken=(net.minecraft.world.entity.animal.chicken.Chicken)mc.player.getVehicle();
                        MobGravity.turn(chicken,d,false);chicken.setOnGround(false);
                        mc.player.input.keyPresses=net.minecraft.world.entity.player.Input.EMPTY;
                        chicken.setDeltaMovement(RotationUtil.vecPlayerToWorld(new Vec3(0,-1,0),d));chicken.aiStep();
                        double without=RotationUtil.vecWorldToPlayer(chicken.getDeltaMovement(),d).y;
                        chicken.setOnGround(false);mc.player.input.keyPresses=new net.minecraft.world.entity.player.Input(false,false,false,false,true,false,false);
                        chicken.setDeltaMovement(RotationUtil.vecPlayerToWorld(new Vec3(0,-1,0),d));chicken.aiStep();
                        double with=RotationUtil.vecWorldToPlayer(chicken.getDeltaMovement(),d).y;
                        if(!(Math.abs(with)<Math.abs(without)))throw new AssertionError("Chicken local glide "+d+": "+with+"/"+without);
                        mc.player.input.keyPresses=net.minecraft.world.entity.player.Input.EMPTY;
                    });
                }
                c.takeScreenshot("mounted-reorientation-chicken");
            }
        }
    }
    private static void checkBeacon(ClientGameTestContext c,boolean enabled){
        c.runOnClient(mc->{
            boolean found=false;
            for(var child:mc.gui.screen().children())if(child instanceof AbstractWidget widget){
                for(Class<?> type=child.getClass();type!=null;type=type.getSuperclass())for(var field:type.getDeclaredFields())if(field.getType()==Holder.class){
                    try{
                        field.setAccessible(true);var effect=field.get(child);
                        if(Reorientation.EFFECT.equals(effect))throw new AssertionError("Reorientation exposed by beacon UI");
                        var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
                        if(clinging.equals(effect)){found=true;if(widget.active!=enabled)throw new AssertionError("Clinging tier activation mismatch");}
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                }
            }
            if(!found)throw new AssertionError("Clinging missing from beacon UI");
        });
    }
}
