package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class GravityChargeLaunchGameTests {
    private static ShulkerBullet charge(GameTestHelper h,Vec3 relative,Vec3 intent){
        Vec3 p=h.absoluteVec(relative);var bullet=new GravityChargeBullet(h.getLevel());bullet.snapTo(p.x,p.y,p.z,0,0);
        ((GravityChargeProjectile)(Object)bullet).clinging$initializeCharge(intent);h.getLevel().addFreshEntity(bullet);return bullet;
    }
    private static GravityChargeProjectile duck(ShulkerBullet bullet){return (GravityChargeProjectile)(Object)bullet;}

    @GameTest(padding=40) public void manualUseConsumesOneAndSpawnsOwnedVanillaBullet(GameTestHelper h){
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);player.snapTo(h.absoluteVec(new Vec3(8,10,8)));player.setYRot(-90);player.setXRot(0);
        var stack=new ItemStack(GravityCharges.ITEM,2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        GravityCharges.ITEM.use(h.getLevel(),player,InteractionHand.MAIN_HAND);
        h.assertTrue(stack.getCount()==1,"manual launch consumes exactly one Charge for a survival player");
        var cooldown=stack.get(DataComponents.USE_COOLDOWN);h.assertTrue(cooldown!=null&&Math.abs(cooldown.seconds()-0.5F)<1.0E-6F,"item carries 0.5 s cooldown component");
        var bullets=h.getLevel().getEntitiesOfClass(ShulkerBullet.class,player.getBoundingBox().inflate(4),b->duck(b).clinging$isLaunchedCharge());
        h.assertTrue(bullets.size()==1,"manual launch creates one marked bullet");var bullet=bullets.getFirst();
        h.assertTrue(bullet.getType()==EntityTypes.SHULKER_BULLET,"launched Gravity Charge keeps exact vanilla entity type");h.assertTrue(bullet.getOwner()==player,"player attribution retained");h.assertTrue(bullet.isNoGravity(),"targetless Gravity Charge does not fall");h.succeed();
    }

    @GameTest public void dispenserFactoryKeepsFacingIntentAndExactEntityType(GameTestHelper h){
        Position p=new Position(){public double x(){return h.absolutePos(new BlockPos(4,5,4)).getX()+0.5;}public double y(){return h.absolutePos(new BlockPos(4,5,4)).getY()+0.5;}public double z(){return h.absolutePos(new BlockPos(4,5,4)).getZ()+0.5;}};
        var projectile=((GravityChargeItem)GravityCharges.ITEM).asProjectile(h.getLevel(),p,new ItemStack(GravityCharges.ITEM),Direction.WEST);
        var bullet=(ShulkerBullet)projectile;var state=duck(bullet);
        h.assertTrue(bullet.getType()==EntityTypes.SHULKER_BULLET,"dispenser factory preserves vanilla bullet type");h.assertTrue(bullet.getOwner()==null,"dispenser projectile is ownerless");
        h.assertTrue(state.clinging$isLaunchedCharge()&&state.clinging$intent().distanceToSqr(new Vec3(-1,0,0))<1.0E-12,"facing becomes exact original intent");h.assertTrue(bullet.isNoGravity(),"dispenser Gravity Charge is no-gravity");h.succeed();
    }

    @GameTest(padding=48) public void alignedEntityWinsOverCloserOffAxisEntity(GameTestHelper h){
        Vec3 origin=new Vec3(6,10,6);var aligned=h.spawn(EntityTypes.COW,new BlockPos(22,10,6));var near=h.spawn(EntityTypes.PIG,new BlockPos(11,10,8));aligned.setNoAi(true);near.setNoAi(true);
        var bullet=charge(h,origin,new Vec3(1,0,0));h.assertTrue(duck(bullet).clinging$targetEntity()==aligned,"crosshair alignment outranks nearer off-axis mob");h.succeed();
    }

    @GameTest(padding=48) public void directTargetBlockHasAbsolutePriority(GameTestHelper h){
        Vec3 origin=new Vec3(6,10,6);BlockPos target=new BlockPos(16,10,6);h.setBlock(target,Blocks.TARGET);var cow=h.spawn(EntityTypes.COW,new BlockPos(11,10,6));cow.setNoAi(true);
        var bullet=charge(h,origin,new Vec3(1,0,0));h.assertTrue(duck(bullet).clinging$targetBlock()!=null,"direct target block selected");h.assertTrue(duck(bullet).clinging$targetEntity()==null,"direct target block beats aligned entity");h.succeed();
    }

    @GameTest(padding=48) public void opaqueBlockPreventsInitialEntityLock(GameTestHelper h){
        Vec3 origin=new Vec3(6,10,6);var cow=h.spawn(EntityTypes.COW,new BlockPos(16,10,6));cow.setNoAi(true);h.setBlock(new BlockPos(11,10,6),Blocks.STONE);
        var bullet=charge(h,origin,new Vec3(1,0,0));h.assertTrue(duck(bullet).clinging$targetEntity()==null,"occluded entity cannot be initially acquired");h.succeed();
    }

    @GameTest(padding=56) public void validLockSurvivesLostLosThenReacquiresAfterRemoval(GameTestHelper h){
        Vec3 origin=new Vec3(6,10,6);var first=h.spawn(EntityTypes.COW,new BlockPos(18,10,6));var second=h.spawn(EntityTypes.PIG,new BlockPos(18,10,8));first.setNoAi(true);second.setNoAi(true);
        var bullet=charge(h,origin,new Vec3(1,0,0));var state=duck(bullet);h.assertTrue(state.clinging$targetEntity()==first,"initial aligned target acquired");
        h.setBlock(new BlockPos(12,10,6),Blocks.STONE);state.clinging$forceAcquire();h.assertTrue(state.clinging$targetEntity()==first,"temporary LOS loss does not discard valid lock");
        first.discard();h.setBlock(new BlockPos(12,10,6),Blocks.AIR);state.clinging$forceAcquire();h.assertTrue(state.clinging$targetEntity()==second,"removed target allows reacquisition");h.succeed();
    }

    @GameTest(padding=48) public void removedTargetBlockAllowsEntityReacquisition(GameTestHelper h){
        Vec3 origin=new Vec3(6,10,6);BlockPos target=new BlockPos(16,10,6);h.setBlock(target,Blocks.TARGET);var pig=h.spawn(EntityTypes.PIG,new BlockPos(18,10,7));pig.setNoAi(true);
        var bullet=charge(h,origin,new Vec3(1,0,0));var state=duck(bullet);h.assertTrue(state.clinging$targetBlock()!=null,"target block acquired");
        h.setBlock(target,Blocks.AIR);state.clinging$forceAcquire();h.assertTrue(state.clinging$targetBlock()==null&&state.clinging$targetEntity()==pig,"removed block permits entity reacquisition");h.succeed();
    }

    @GameTest public void launchedStateAndIntentSurviveSaveLoad(GameTestHelper h){
        var original=charge(h,new Vec3(4,8,4),new Vec3(1,0.2,0));var output=TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());original.saveWithoutId(output);
        var restored=new ShulkerBullet(EntityTypes.SHULKER_BULLET,h.getLevel());var input=TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult());restored.load(input);
        var state=duck(restored);h.assertTrue(state.clinging$isLaunchedCharge(),"save/load keeps Gravity Charge identity");h.assertTrue(state.clinging$intent().distanceToSqr(GravityChargeTargeting.safeIntent(new Vec3(1,0.2,0)))<1.0E-12,"save/load keeps original aim intent");h.assertTrue(restored.isNoGravity(),"restored Gravity Charge remains no-gravity");h.succeed();
    }
}
