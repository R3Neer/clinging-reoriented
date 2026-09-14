package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.testmixin.EntityTestAccessor;
import io.github.r3neer.clingingreoriented.testmixin.ShulkerTestAccessor;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShulkerChargeProjectileGameTests {
    private static final long ROUTE_SEED=0x5EED5EEDL;

    /** Keep the whole small flight corridor at ENTITY_TICKING level for these tests. */
    private static void keepSimulated(GameTestHelper h,Vec3... points){
        if(points.length==0)return;
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(Vec3 point:points){
            ChunkPos chunk=ChunkPos.containing(BlockPos.containing(point));
            minX=Math.min(minX,chunk.x());maxX=Math.max(maxX,chunk.x());
            minZ=Math.min(minZ,chunk.z());maxZ=Math.max(maxZ,chunk.z());
        }
        // TicketStorage maps radius 2 to level 31, which is ENTITY_TICKING in 26.2.
        // Expand one whole chunk because vanilla Shulker routing can take an orthogonal detour.
        minX--;minZ--;maxX++;maxZ++;
        var source=h.getLevel().getChunkSource();
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)
            source.addTicketWithRadius(TicketType.PORTAL,new ChunkPos(x,z),2);
    }

    private static ShulkerBullet charge(GameTestHelper h,Vec3 relative,Vec3 intent){return chargeAbsolute(h,h.absoluteVec(relative),intent);}
    private static ShulkerBullet chargeAbsolute(GameTestHelper h,Vec3 absolute,Vec3 intent){
        keepSimulated(h,absolute);
        var bullet=new ShulkerChargeBullet(h.getLevel());
        ((EntityTestAccessor)(Object)bullet).clinging$random().setSeed(ROUTE_SEED);
        bullet.snapTo(absolute.x,absolute.y,absolute.z,0,0);
        ((ShulkerChargeProjectile)(Object)bullet).clinging$initializeCharge(intent);
        h.assertTrue(h.getLevel().addFreshEntity(bullet),"Shulker Charge fixture must be admitted to the server entity manager");
        return bullet;
    }
    private static long drops(GameTestHelper h,Vec3 relative){Vec3 p=h.absoluteVec(relative);return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p.x-4,p.y-4,p.z-4,p.x+4,p.y+4,p.z+4),e->e.getItem().is(ShulkerCharges.ITEM)).size();}
    private static void clear(GameTestHelper h,int minX,int minY,int minZ,int maxX,int maxY,int maxZ){for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);}
    private static void clearAbsolute(ServerLevel level,BlockPos min,BlockPos max){for(BlockPos p:BlockPos.betweenClosed(min,max))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);}

    /**
     * Test-only Shulker that removes the random search from vanilla duplication without
     * replacing the vanilla hurtServer/hitByShulkerBullet/spawn path itself.
     */
    private static final class DeterministicTeleportShulker extends Shulker {
        private final Vec3 destination;
        private DeterministicTeleportShulker(Level level,Vec3 destination){super(EntityTypes.SHULKER,level);this.destination=destination;}
        @Override protected boolean teleportSomewhere(){setPos(destination.x,destination.y,destination.z);return true;}
    }

    @GameTest(maxTicks=220,padding=48) public void targetBlockReceivesRealProjectileSignal(GameTestHelper h){
        ServerLevel level=h.getLevel();BlockPos target=h.absolutePos(new BlockPos(12,8,8));clearAbsolute(level,target.offset(-8,-3,-4),target.offset(4,3,4));level.setBlock(target,Blocks.TARGET.defaultBlockState(),3);
        Vec3 center=Vec3.atCenterOf(target),origin=center.add(-6,0,0);keepSimulated(h,origin,center);var bullet=chargeAbsolute(h,origin,center.subtract(origin));var state=(ShulkerChargeProjectile)(Object)bullet;
        h.assertTrue(level.getBlockState(target).is(Blocks.TARGET),"absolute Target Block fixture exists");h.assertTrue(target.equals(state.clinging$targetBlock()),"direct Target Block lock resolves to the same absolute block");
        h.startSequence().thenExecuteAfter(1,()->h.assertTrue(bullet.tickCount>0,"direct Target Block Charge must enter entity ticking")).thenWaitUntil(()->h.assertTrue(level.getBlockState(target).getValue(BlockStateProperties.POWER)>0,"Target Block must be powered by physical projectile impact; ticks="+bullet.tickCount+" alive="+bullet.isAlive()+" pos="+bullet.position()+" vel="+bullet.getDeltaMovement()+" target="+target)).thenExecute(()->h.assertTrue(!bullet.isAlive(),"impact destroys Charge through vanilla ShulkerBullet onHit")).thenSucceed();
    }

    @GameTest(maxTicks=260,padding=64) public void offsetTargetBlockRequiresOrthogonalRouteAndStillHits(GameTestHelper h){
        ServerLevel level=h.getLevel();BlockPos target=h.absolutePos(new BlockPos(18,9,14));clearAbsolute(level,target.offset(-16,-4,-12),target.offset(4,4,4));level.setBlock(target,Blocks.TARGET.defaultBlockState(),3);
        Vec3 center=Vec3.atCenterOf(target),origin=center.add(-10,0,-6);keepSimulated(h,origin,center);var bullet=chargeAbsolute(h,origin,center.subtract(origin));var state=(ShulkerChargeProjectile)(Object)bullet;
        h.assertTrue(target.equals(state.clinging$targetBlock()),"offset Target Block fixture locks exact absolute target");
        h.startSequence().thenExecuteAfter(1,()->{h.assertTrue(bullet.tickCount>0,"offset Target Block Charge must enter entity ticking");Vec3 movement=bullet.getDeltaMovement();int axes=(Math.abs(movement.x)>1.0E-4?1:0)+(Math.abs(movement.y)>1.0E-4?1:0)+(Math.abs(movement.z)>1.0E-4?1:0);h.assertTrue(axes==1,"active Shulker route segment is cardinal after first steering tick");}).thenWaitUntil(()->h.assertTrue(level.getBlockState(target).getValue(BlockStateProperties.POWER)>0,"multi-axis Target Block route reaches real block; ticks="+bullet.tickCount+" alive="+bullet.isAlive()+" pos="+bullet.position()+" vel="+bullet.getDeltaMovement()+" target="+target)).thenSucceed();
    }

    @GameTest(maxTicks=180,padding=48) public void entityImpactKeepsVanillaDamageAndLevitation(GameTestHelper h){
        clear(h,7,5,4,22,15,14);var cow=h.spawn(EntityTypes.COW,new BlockPos(15,8,8));cow.setNoAi(true);cow.setNoGravity(true);float before=cow.getHealth();Vec3 origin=new Vec3(12.5,8.8,8.5);Vec3 absoluteOrigin=h.absoluteVec(origin);keepSimulated(h,absoluteOrigin,cow.getBoundingBox().getCenter());var bullet=chargeAbsolute(h,absoluteOrigin,cow.getBoundingBox().getCenter().subtract(absoluteOrigin));
        h.assertTrue(((ShulkerChargeProjectile)(Object)bullet).clinging$targetEntity()==cow,"damage fixture owns cow target before flight");
        h.startSequence().thenExecuteAfter(1,()->h.assertTrue(bullet.tickCount>0,"entity-impact Charge must enter entity ticking")).thenWaitUntil(()->{h.assertTrue(cow.getHealth()<before,"Charge damages stationary entity through vanilla hit path; ticks="+bullet.tickCount+" alive="+bullet.isAlive()+" pos="+bullet.position()+" vel="+bullet.getDeltaMovement());h.assertTrue(cow.hasEffect(MobEffects.LEVITATION),"Charge applies vanilla Levitation");}).thenExecute(()->h.assertTrue(Math.abs((before-cow.getHealth())-4.0F)<0.01F,"unarmored target receives vanilla 4 damage")).thenExecute(()->h.assertTrue(!bullet.isAlive(),"entity impact consumes projectile")).thenSucceed();
    }

    @GameTest(padding=40) public void launchedChargeCanBeRecapturedExactlyOnceByMeleeOrArrow(GameTestHelper h){
        Vec3 a=new Vec3(7,8,7),b=new Vec3(14,8,7);var player=h.makeMockServerPlayerInLevel();
        var melee=charge(h,a,new Vec3(0,1,0));melee.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1.0F);h.assertTrue(drops(h,a)==1,"melee recaptures launched Charge");melee.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1.0F);h.assertTrue(drops(h,a)==1,"melee race cannot duplicate item");
        var arrowEntity=new Arrow(h.getLevel(),player,new ItemStack(Items.ARROW),null);var ranged=charge(h,b,new Vec3(0,1,0));ranged.hurtServer(h.getLevel(),h.getLevel().damageSources().arrow(arrowEntity,player),1.0F);h.assertTrue(drops(h,b)==1,"arrow recaptures launched Charge");h.succeed();
    }

    @GameTest(maxTicks=100,padding=80) public void blockImpactAndPlainDiscardReturnNoItem(GameTestHelper h){
        ServerLevel level=h.getLevel();BlockPos wall=h.absolutePos(new BlockPos(12,8,8));clearAbsolute(level,wall.offset(-5,-3,-3),wall.offset(3,3,3));level.setBlock(wall,Blocks.STONE.defaultBlockState(),3);
        Vec3 center=Vec3.atCenterOf(wall),origin=center.add(-2,0,0);keepSimulated(h,origin,center);var bullet=chargeAbsolute(h,origin,center.subtract(origin));var state=(ShulkerChargeProjectile)(Object)bullet;
        h.assertTrue(level.getBlockState(wall).is(Blocks.STONE),"absolute wall fixture exists");h.assertTrue(state.clinging$targetEntity()==null&&state.clinging$targetBlock()==null,"ordinary wall is not an autoaim target");
        h.startSequence().thenExecuteAfter(1,()->h.assertTrue(bullet.tickCount>0,"free-flight Charge must enter entity ticking")).thenWaitUntil(()->h.assertTrue(!bullet.isAlive(),"Charge reaches ordinary block and is consumed; ticks="+bullet.tickCount+" start="+origin+" wall="+wall+" pos="+bullet.position()+" vel="+bullet.getDeltaMovement())).thenExecute(()->h.assertTrue(level.getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atCenterOf(wall),Vec3.atCenterOf(wall)).inflate(5),e->e.getItem().is(ShulkerCharges.ITEM)).isEmpty(),"ordinary block impact never returns Charge item")).thenExecute(()->{var discarded=chargeAbsolute(h,center.add(-2,4,0),new Vec3(0,1,0));discarded.discard();h.assertTrue(!discarded.isAlive(),"plain discard removes Charge");}).thenSucceed();
    }

    @GameTest(maxTicks=180,padding=56) public void shieldedPlayerNeutralizesChargeWithoutDrop(GameTestHelper h){
        clear(h,7,5,4,22,15,14);var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);player.snapTo(h.absoluteVec(new Vec3(16,8,8)));player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));player.startUsingItem(InteractionHand.OFF_HAND);
        Vec3 origin=player.getEyePosition().add(-3,0,0);keepSimulated(h,origin,player.getEyePosition());var bullet=chargeAbsolute(h,origin,player.getEyePosition().subtract(origin));player.lookAt(EntityAnchorArgument.Anchor.EYES,bullet.position());float before=player.getHealth();
        h.assertTrue(((ShulkerChargeProjectile)(Object)bullet).clinging$targetEntity()==player,"shield fixture owns the intended stationary player lock");
        h.startSequence().thenExecuteAfter(1,()->h.assertTrue(bullet.tickCount>0,"shield Charge must enter entity ticking")).thenWaitUntil(()->h.assertTrue(!bullet.isAlive(),"shield encounter consumes Shulker Charge projectile; ticks="+bullet.tickCount+" pos="+bullet.position()+" player="+player.position())).thenExecute(()->{h.assertTrue(player.getHealth()==before,"shield blocks Shulker Charge damage");}).thenSucceed();
    }

    @GameTest(maxTicks=180,padding=60) public void launchedChargePreservesVanillaShulkerDuplication(GameTestHelper h){
        ServerLevel level=h.getLevel();Vec3 parentPos=h.absoluteVec(new Vec3(18.5,7,14.5));Vec3 destination=parentPos.add(12,0,0);keepSimulated(h,parentPos,destination);
        var shulker=new DeterministicTeleportShulker(level,destination);shulker.snapTo(parentPos);shulker.setNoAi(true);((ShulkerTestAccessor)(Object)shulker).clinging$setRawPeekAmount(100);h.assertTrue(level.addFreshEntity(shulker),"deterministic vanilla-duplication Shulker enters world");float before=shulker.getHealth();
        Vec3 origin=parentPos.add(-2,0.5,0);keepSimulated(h,origin,parentPos);var bullet=chargeAbsolute(h,origin,shulker.getBoundingBox().getCenter().subtract(origin));Vec3 oldPosition=shulker.position();AABB oldArea=new AABB(oldPosition,oldPosition).inflate(8.0D);
        h.assertTrue(bullet.getType()==EntityTypes.SHULKER_BULLET,"launched Charge keeps exact vanilla type required by Shulker duplication");h.assertTrue(((ShulkerChargeProjectile)(Object)bullet).clinging$targetEntity()==shulker,"duplication fixture owns shulker target");
        h.startSequence().thenExecuteAfter(1,()->h.assertTrue(bullet.tickCount>0,"duplication Charge must enter entity ticking")).thenWaitUntil(()->h.assertTrue(!bullet.isAlive(),"Charge physically reaches and is consumed by shulker interaction; ticks="+bullet.tickCount+" pos="+bullet.position()+" vel="+bullet.getDeltaMovement())).thenExecute(()->h.assertTrue(shulker.getHealth()<before,"vanilla shulker damage path was reached before duplication assertion")).thenWaitUntil(()->{List<Shulker> all=level.getEntities(EntityTypes.SHULKER,oldArea,Shulker::isAlive);h.assertTrue(all.size()>=1,"vanilla hitByShulkerBullet path creates a second shulker at the old position");}).thenSucceed();
    }
}
