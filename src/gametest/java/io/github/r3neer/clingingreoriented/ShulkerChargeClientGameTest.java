package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Visual evidence for the two item presentations and the unchanged vanilla projectile renderer. */
public final class ShulkerChargeClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-8,79,-8),new BlockPos(8,86,12)))
                    level.setBlockAndUpdate(pos,pos.getY()==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleport(new TeleportTransition(level,new Vec3(.5,80,.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
                player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ShulkerCharges.ITEM));
            });
            context.waitFor(mc->mc.player!=null&&mc.player.getMainHandItem().is(ShulkerCharges.ITEM));
            context.waitTicks(4);

            context.runOnClient(mc->mc.gui.setScreen(new InventoryScreen(mc.player)));
            context.waitTicks(3);
            context.takeScreenshot("shulker-charge-inventory-icon");
            context.runOnClient(mc->mc.gui.setScreen(null));
            context.waitTicks(2);

            context.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.waitTicks(3);
            context.takeScreenshot("shulker-charge-first-person-held");

            context.runOnClient(mc->mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            context.waitTicks(3);
            context.takeScreenshot("shulker-charge-third-person-held");

            context.runOnClient(mc->mc.options.setCameraType(CameraType.FIRST_PERSON));
            world.getServer().runOnServer(server->{
                var level=server.overworld();var player=server.getPlayerList().getPlayers().getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                Vec3 pos=player.getEyePosition().add(0,0,3.0D);
                var bullet=new ShulkerChargeBullet(level);bullet.snapTo(pos.x,pos.y,pos.z,0,0);bullet.setOwner(player);
                ((ShulkerChargeProjectile)(Object)bullet).clinging$initializeCharge(new Vec3(0,0,1));
                bullet.setDeltaMovement(Vec3.ZERO);level.addFreshEntity(bullet);
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                if(mc.level==null||mc.level.getEntities().getAll().stream().noneMatch(e->e instanceof net.minecraft.world.entity.projectile.ShulkerBullet))
                    throw new AssertionError("Shulker Charge projectile is not present on client for renderer evidence");
            });
            context.takeScreenshot("shulker-charge-projectile-renderer");
        }
    }
}
