package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.testmixin.ShulkerTestAccessor;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShulkerChargeProjectileGameTests {
    private static ShulkerBullet charge(GameTestHelper h,Vec3 relative,Vec3 intent){
        Vec3 p=h.absoluteVec(relative);var bullet=new ShulkerChargeBullet(h.getLevel());bullet.snapTo(p.x,p.y,p.z,0,0);
        ((ShulkerChargeProjectile)(Object)bullet).clinging$initializeCharge(intent);h.getLevel().addFreshEntity(bullet);return bullet;
    }
    private static long drops(GameTestHelper h,Vec3 relative){Vec3 p=h.absoluteVec(relative);return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p.x-4,p.y-4,p.z-4,p.x+4,p.y+4,p.z+4),e->e.getItem().is(ShulkerCharges.ITEM)).size();}
    private static void clear(GameTestHelper h,int minX,int minY,int minZ,int maxX,int maxY,int maxZ){for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);}

    @GameTest(maxTicks=180,padding=40) public void targetBlockReceivesRealProjectileSignal(GameTestHelper h){
        clear(h,3,7,6,19,10,10);BlockPos target=new BlockPos(16,8,8);h.setBlock(target,Blocks.TARGET);Vec3 origin=new Vec3(5.5,8.5,8.5);Vec3 intent=Vec3.atCenterOf(target).subtract(origin);
        var bullet=charge(h,origin,intent);h.assertTrue(((ShulkerChargeProjectile)(Object)bullet).clinging$targetBlock()!=null,"direct Target Block locked in owned clear corridor");
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(target).getValue(BlockStateProperties.POWER)>0,"Target Block must be powered by physical projectile impact")).thenExecute(()->h.assertTrue(!bullet.isAlive(),"impact destroys Charge through vanilla ShulkerBullet onHit")).thenSucceed();
    }

    @GameTest(maxTicks=260,padding=48) public void offsetTargetBlockRequiresOrthogonalRouteAndStillHits(GameTestHelper h){
        clear(h,3,7,3,22,11,17);BlockPos target=new BlockPos(18,8,13);h.setBlock(target,Blocks.TARGET);Vec3 origin=new Vec3(5.5,8.5,5.5);Vec3 intent=Vec3.atCenterOf(target).subtract(origin);
        var bullet=charge(h,origin,intent);Vec3 initial=bullet.getDeltaMovement();
        h.assertTrue((Math.abs(initial.x)>0.14?1:0)+(Math.abs(initial.y)>0.14?1:0)+(Math.abs(initial.z)>0.14?1:0)==1,"first Shulker route segment is cardinal");
        h.startSequence().thenWaitUntil(()->h.assertTrue(h.getBlockState(target).getValue(BlockStateProperties.POWER)>0,"multi-axis Target Block route reaches real block")).thenSucceed();
    }

    @GameTest(maxTicks=180,padding=40) public void entityImpactKeepsVanillaDamageAndLevitation(GameTestHelper h){
        clear(h,3,7,6,19,11,10);var cow=h.spawn(EntityTypes.COW,new BlockPos(15,8,8));cow.setNoAi(true);float before=cow.getHealth();Vec3 origin=new Vec3(5.5,8.8,8.5);var bullet=charge(h,origin,cow.getBoundingBox().getCenter().subtract(h.absoluteVec(origin)));
        h.startSequence().thenWaitUntil(()->{h.assertTrue(cow.getHealth()<before,"Charge damages entity through vanilla hit path");h.assertTrue(cow.hasEffect(MobEffects.LEVITATION),"Charge applies vanilla Levitation");}).thenExecute(()->h.assertTrue(Math.abs((before-cow.getHealth())-4.0F)<0.01F,"unarmored target receives vanilla 4 damage")).thenExecute(()->h.assertTrue(!bullet.isAlive(),"entity impact consumes projectile")).thenSucceed();
    }

    @GameTest(padding=30) public void launchedChargeCanBeRecapturedExactlyOnceByMeleeOrArrow(GameTestHelper h){
        Vec3 a=new Vec3(7,8,7),b=new Vec3(14,8,7);var player=h.makeMockServerPlayerInLevel();
        var melee=charge(h,a,new Vec3(0,1,0));melee.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1.0F);h.assertTrue(drops(h,a)==1,"melee recaptures launched Charge");melee.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1.0F);h.assertTrue(drops(h,a)==1,"melee race cannot duplicate item");
        var arrowEntity=new Arrow(h.getLevel(),player,new ItemStack(Items.ARROW),null);var ranged=charge(h,b,new Vec3(0,1,0));ranged.hurtServer(h.getLevel(),h.getLevel().damageSources().arrow(arrowEntity,player),1.0F);h.assertTrue(drops(h,b)==1,"arrow recaptures launched Charge");h.succeed();
    }

    @GameTest(maxTicks=100,padding=30) public void blockImpactAndPlainDiscardReturnNoItem(GameTestHelper h){
        clear(h,5,7,6,14,13,10);BlockPos wall=new BlockPos(11,8,8);h.setBlock(wall,Blocks.STONE);Vec3 origin=new Vec3(7.5,8.5,8.5);var bullet=charge(h,origin,new Vec3(1,0,0));
        h.startSequence().thenWaitUntil(()->h.assertTrue(!bullet.isAlive(),"Charge reaches ordinary block and is consumed")).thenExecute(()->h.assertTrue(drops(h,new Vec3(9,8,8))==0,"ordinary block impact never returns Charge item")).thenExecute(()->{var discarded=charge(h,new Vec3(7,12,8),new Vec3(0,1,0));discarded.discard();h.assertTrue(drops(h,new Vec3(7,12,8))==0,"plain expiry/discard path returns no item");}).thenSucceed();
    }

    @GameTest(maxTicks=180,padding=50) public void shieldedPlayerNeutralizesChargeWithoutDrop(GameTestHelper h){
        clear(h,5,7,6,19,12,10);var player=h.makeMockServerPlayerInLevel();player.snapTo(h.absoluteVec(new Vec3(16,8,8)));player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));player.startUsingItem(InteractionHand.OFF_HAND);
        Vec3 origin=new Vec3(7.5,player.getEyeY()-h.absoluteVec(Vec3.ZERO).y,8.5);var bullet=charge(h,origin,player.getEyePosition().subtract(h.absoluteVec(origin)));player.lookAt(EntityAnchorArgument.Anchor.EYES,bullet.position());float before=player.getHealth();
        h.startSequence().thenWaitUntil(()->h.assertTrue(!bullet.isAlive(),"shield encounter consumes Shulker Charge projectile")).thenExecute(()->{h.assertTrue(drops(h,new Vec3(12,8,8))==0,"shield never converts projectile back to item");h.assertTrue(player.getHealth()==before,"shield blocks Shulker Charge damage");}).thenSucceed();
    }

    @GameTest(maxTicks=260,padding=60) public void launchedChargePreservesVanillaShulkerDuplication(GameTestHelper h){
        clear(h,1,7,1,30,20,30);for(int x=2;x<=28;x++)for(int z=2;z<=28;z++)h.setBlock(new BlockPos(x,6,z),Blocks.STONE);
        Shulker shulker=h.spawn(EntityTypes.SHULKER,new BlockPos(18,7,14));((ShulkerTestAccessor)(Object)shulker).clinging$setRawPeekAmount(100);
        Vec3 origin=new Vec3(8.5,7.5,14.5);var bullet=charge(h,origin,shulker.getBoundingBox().getCenter().subtract(h.absoluteVec(origin)));
        h.assertTrue(bullet.getType()==EntityTypes.SHULKER_BULLET,"launched Charge keeps exact vanilla type required by Shulker duplication");
        h.startSequence().thenWaitUntil(()->{List<Shulker> all=h.getLevel().getEntitiesOfClass(Shulker.class,new AABB(h.absoluteVec(new Vec3(1,5,1)),h.absoluteVec(new Vec3(30,20,30))),Shulker::isAlive);h.assertTrue(all.size()>=2,"vanilla hitByShulkerBullet path creates a second shulker");}).thenSucceed();
    }
}
