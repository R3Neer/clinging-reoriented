package io.github.r3neer.clingingreoriented;

import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;

/** Reserved SC-S05 attacks against integration boundaries and state ownership. */
public final class ShulkerChargeAdversarialGameTests {
    private static void clear(GameTestHelper h,int minX,int minY,int minZ,int maxX,int maxY,int maxZ){
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++)h.setBlock(new BlockPos(x,y,z),Blocks.AIR);
    }

    private static ShulkerBullet launched(GameTestHelper h,Vec3 relative,Vec3 intent){
        var bullet=new ShulkerChargeBullet(h.getLevel());
        Vec3 at=h.absoluteVec(relative);
        bullet.snapTo(at.x,at.y,at.z,0,0);
        ((ShulkerChargeProjectile)(Object)bullet).clinging$initializeCharge(intent);
        h.assertTrue(h.getLevel().addFreshEntity(bullet),"adversarial Charge must enter server entity manager");
        return bullet;
    }

    private static long chargeDrops(ServerLevel level,Vec3 at,double radius){
        AABB area=new AABB(at,at).inflate(radius);
        return level.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().is(ShulkerCharges.ITEM)).size();
    }

    @GameTest(maxTicks=80,padding=40)
    public void realDispenserUsesRegisteredBehaviorConsumesOneAndKeepsFacing(GameTestHelper h){
        ServerLevel level=h.getLevel();
        BlockPos dispenser=h.absolutePos(new BlockPos(10,8,10));
        for(BlockPos p:BlockPos.betweenClosed(dispenser.offset(-2,-2,-2),dispenser.offset(10,3,2)))level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
        var state=Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST);
        level.setBlock(dispenser,state,3);
        h.assertTrue(level.getBlockEntity(dispenser) instanceof DispenserBlockEntity,"real dispenser fixture must create its block entity");
        var be=(DispenserBlockEntity)level.getBlockEntity(dispenser);
        be.setItem(0,new ItemStack(ShulkerCharges.ITEM,2));
        level.setBlock(dispenser.above(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
        Vec3 center=Vec3.atCenterOf(dispenser);

        h.startSequence()
            .thenExecuteAfter(6,()->{
                List<ShulkerBullet> charges=level.getEntitiesOfClass(ShulkerBullet.class,new AABB(center,center).inflate(12),e->((ShulkerChargeProjectile)(Object)e).clinging$isLaunchedCharge());
                h.assertTrue(charges.size()==1,"one powered dispenser activation must launch exactly one Shulker Charge; found="+charges.size());
                var bullet=charges.getFirst();var duck=(ShulkerChargeProjectile)(Object)bullet;
                h.assertTrue(duck.clinging$intent().distanceToSqr(new Vec3(1,0,0))<1.0E-6,"registered projectile behavior must preserve dispenser EAST facing; intent="+duck.clinging$intent());
                h.assertTrue(bullet.getOwner()==null,"dispenser Charge must remain ownerless");
                h.assertTrue(be.getItem(0).getCount()==1,"real dispenser behavior must consume exactly one Charge; count="+be.getItem(0).getCount());
            })
            .thenSucceed();
    }

    @GameTest(padding=40)
    public void validLockIsNotReplacedByLaterBetterCandidate(GameTestHelper h){
        clear(h,4,5,4,25,13,12);
        var cow=h.spawn(EntityTypes.COW,new BlockPos(18,8,8));cow.setNoAi(true);cow.setNoGravity(true);
        var bullet=launched(h,new Vec3(7.5,8.8,8.5),new Vec3(1,0,0));
        var duck=(ShulkerChargeProjectile)(Object)bullet;
        h.assertTrue(duck.clinging$targetEntity()==cow,"fixture must acquire original farther cow");
        var pig=h.spawn(EntityTypes.PIG,new BlockPos(12,8,8));pig.setNoAi(true);pig.setNoGravity(true);
        duck.clinging$forceAcquire();
        h.assertTrue(duck.clinging$targetEntity()==cow,"valid existing lock must survive later closer candidate");
        h.succeed();
    }

    @GameTest(maxTicks=40,padding=44)
    public void freeFlightAutomaticallyAcquiresTargetThatAppearsLater(GameTestHelper h){
        clear(h,4,5,4,30,13,12);
        var bullet=launched(h,new Vec3(7.5,8.8,8.5),new Vec3(1,0,0));
        var duck=(ShulkerChargeProjectile)(Object)bullet;
        h.assertTrue(duck.clinging$targetEntity()==null&&duck.clinging$targetBlock()==null,"late-target fixture must begin in free flight");
        final LivingEntity[] cow={null};
        h.startSequence()
            .thenExecuteAfter(2,()->{cow[0]=h.spawn(EntityTypes.COW,new BlockPos(18,8,8));if(cow[0] instanceof Mob mob)mob.setNoAi(true);cow[0].setNoGravity(true);})
            .thenWaitUntil(()->h.assertTrue(duck.clinging$targetEntity()==cow[0],"periodic reacquisition must discover a target that appears after launch; tick="+bullet.tickCount+" target="+duck.clinging$targetEntity()))
            .thenSucceed();
    }

    @GameTest(padding=48)
    public void targetChangingDimensionInvalidatesReferenceAndReacquiresLocally(GameTestHelper h){
        clear(h,4,5,4,28,13,12);
        ServerLevel overworld=h.getLevel();
        var cow=h.spawn(EntityTypes.COW,new BlockPos(18,8,8));cow.setNoAi(true);cow.setNoGravity(true);
        var bullet=launched(h,new Vec3(7.5,8.8,8.5),new Vec3(1,0,0));
        var duck=(ShulkerChargeProjectile)(Object)bullet;
        h.assertTrue(duck.clinging$targetEntity()==cow,"dimension fixture must own cow before transfer");
        Vec3 originalIntent=duck.clinging$intent();
        var pig=h.spawn(EntityTypes.PIG,new BlockPos(14,8,8));pig.setNoAi(true);pig.setNoGravity(true);
        ServerLevel nether=overworld.getServer().getLevel(Level.NETHER);
        h.assertTrue(nether!=null,"GameTest server must expose Nether for lifecycle holdout");
        var moved=cow.teleport(new TeleportTransition(nether,new Vec3(0.5,80,0.5),Vec3.ZERO,0,0,TeleportTransition.DO_NOTHING));
        h.assertTrue(moved!=null&&moved.level()==nether,"target must complete cross-dimension transfer");
        duck.clinging$forceAcquire();
        h.assertTrue(duck.clinging$targetEntity()==pig,"old cross-dimension reference must invalidate and allow local reacquisition");
        h.assertTrue(duck.clinging$intent().distanceToSqr(originalIntent)<1.0E-12,"reacquisition must preserve original launch intent");
        h.succeed();
    }

    @GameTest(padding=44)
    public void mixedArrowAndMeleeRecaptureRaceProducesOneDropPerCharge(GameTestHelper h){
        clear(h,4,5,4,24,13,16);
        ServerLevel level=h.getLevel();
        var owner=h.makeMockServerPlayerInLevel();owner.snapTo(h.absoluteVec(new Vec3(6,8,6)));
        var other=h.makeMockServerPlayerInLevel();other.snapTo(h.absoluteVec(new Vec3(6,8,12)));

        var first=launched(h,new Vec3(12,8,7),new Vec3(1,0,0));first.setOwner(owner);Vec3 firstAt=first.position();
        var foreignArrow=new Arrow(level,other,new ItemStack(Items.ARROW),null);
        first.hurtServer(level,level.damageSources().arrow(foreignArrow,other),1.0F);
        first.hurtServer(level,level.damageSources().playerAttack(owner),1.0F);
        h.assertTrue(chargeDrops(level,firstAt,4)==1,"foreign arrow then owner melee must produce exactly one Charge drop");

        var second=launched(h,new Vec3(18,8,11),new Vec3(1,0,0));second.setOwner(owner);Vec3 secondAt=second.position();
        var ownerArrow=new Arrow(level,owner,new ItemStack(Items.ARROW),null);
        second.hurtServer(level,level.damageSources().playerAttack(other),1.0F);
        second.hurtServer(level,level.damageSources().arrow(ownerArrow,owner),1.0F);
        h.assertTrue(chargeDrops(level,secondAt,4)==1,"foreign melee then owner arrow must produce exactly one Charge drop");
        h.succeed();
    }
}
