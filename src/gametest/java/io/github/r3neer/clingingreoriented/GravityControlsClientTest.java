package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import com.moigferdsrte.gravitychanger.util.*;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

/** Physical jump edges, owned snap presentation, vanilla Elytra deployment and the real beacon screen. */
public final class GravityControlsClientTest implements FabricClientGameTest {
    private static void tap(ClientGameTestContext c){c.getInput().holdKey(o->o.keyJump);c.waitTicks(2);c.getInput().releaseKey(o->o.keyJump);c.waitTicks(3);}
    @Override public void runTest(ClientGameTestContext c){
        try(var world=c.worldBuilder().create()){
            c.waitFor(mc->mc.player!=null);
            checkSnapOwnership(c);
            world.getServer().runOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(server.overworld(),new Vec3(0,150,0),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
                p.addEffect(new MobEffectInstance(Reorientation.EFFECT,10000));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
                ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.visualFrameOwned=true;s.selected=Direction.EAST;
            });
            c.waitFor(mc->mc.player.hasEffect(Reorientation.EFFECT) && GravityDirectionUtil.getGravityDirection(mc.player)==Direction.EAST);
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
        }
    }
    private static void checkSnapOwnership(ClientGameTestContext c){
        c.runOnClient(mc->{
            try {
                var target=RotationUtil.getEntityRotationQuaternion(Direction.EAST);

                var unrelated=new GravityRotationAnimation();
                unrelated.forceSet(Direction.DOWN,0);unrelated.getRotation(Direction.EAST,0);
                if(Math.abs(unrelated.getRotation(Direction.EAST,1_250_000_000L).dot(target))<.99999f)
                    throw new AssertionError("Unowned Gravity Changer transition did not retain upstream 1.25 s timing");

                var owned=animation(mc.player);
                owned.forceSet(Direction.DOWN,0);
                float originalYaw=mc.player.getYRot();float originalPitch=mc.player.getXRot();float originalYawOld=mc.player.yRotO;
                var plan=GravityTransition.plan(Direction.DOWN,Direction.EAST,originalYaw,originalPitch);
                VisualTransitions.begin(mc.player,Direction.EAST,plan.yawDelta(),plan.kind().ordinal(),1);
                var start=owned.getRotation(Direction.EAST,0);
                if(Math.abs(start.dot(target))>.999f)throw new AssertionError("Clinging snap began at its target instead of a compensated start frame");
                if(Math.abs(owned.getRotation(Direction.EAST,GravityTransition.QUARTER_TURN_NANOS).dot(target))<.99999f)
                    throw new AssertionError("Clinging quarter-turn did not finish at fixed 120 ms duration");
                if(VisualTransitions.owns(owned))throw new AssertionError("Completed snap still owns Gravity Changer animation");
                mc.player.setYRot(originalYaw);mc.player.setXRot(originalPitch);mc.player.yRotO=originalYawOld;
                VisualTransitions.clear();
            } catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
        });
    }
    private static GravityRotationAnimation animation(Entity entity) throws ReflectiveOperationException {
        for(var field:Entity.class.getDeclaredFields())if(field.getType()==GravityRotationAnimation.class){field.setAccessible(true);return (GravityRotationAnimation)field.get(entity);}
        throw new NoSuchFieldException("GravityRotationAnimation on Entity");
    }
    private static void checkBeacon(ClientGameTestContext c,boolean enabled){
        c.runOnClient(mc->{
            boolean found=false;
            for(var child:mc.gui.screen().children())if(child instanceof AbstractWidget widget){
                for(Class<?> type=child.getClass();type!=null;type=type.getSuperclass())for(var field:type.getDeclaredFields())if(field.getType()==Holder.class){
                    try{
                        field.setAccessible(true);var effect=field.get(child);
                        if(Reorientation.EFFECT.equals(effect))throw new AssertionError("Reorientation exposed by beacon UI");
                        var clinging=net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.Identifier.parse("alexsmobs:clinging")).orElseThrow();
                        if(clinging.equals(effect)){found=true;if(widget.active!=enabled)throw new AssertionError("Clinging tier activation mismatch");}
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                }
            }
            if(!found)throw new AssertionError("Clinging missing from beacon UI");
        });
    }
}
