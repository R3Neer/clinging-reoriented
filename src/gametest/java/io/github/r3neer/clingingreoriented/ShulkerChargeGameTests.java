package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ShulkerChargeGameTests {
    private static ShulkerBullet bullet(GameTestHelper h,Vec3 relative){
        var level=h.getLevel();var p=h.absoluteVec(relative);
        var bullet=new ShulkerBullet(EntityTypes.SHULKER_BULLET,level);
        bullet.snapTo(p.x,p.y,p.z,0.0F,0.0F);level.addFreshEntity(bullet);return bullet;
    }
    private static long drops(GameTestHelper h,Vec3 relative){
        Vec3 p=h.absoluteVec(relative);AABB box=new AABB(p.x-2,p.y-2,p.z-2,p.x+2,p.y+2,p.z+2);
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,box,e->e.getItem().is(ShulkerCharges.ITEM)).size();
    }

    @GameTest public void itemStacksToSixtyFour(GameTestHelper h){
        h.assertTrue(new ItemStack(ShulkerCharges.ITEM).getMaxStackSize()==64,"Shulker Charge stacks to 64");h.succeed();
    }

    @GameTest(padding=8) public void meleeCapturesNaturalBulletExactlyOnce(GameTestHelper h){
        Vec3 at=new Vec3(3,4,3);var bullet=bullet(h,at);var player=h.makeMockServerPlayerInLevel();
        bullet.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1.0F);
        h.assertTrue(drops(h,at)==1,"melee interception drops exactly one Shulker Charge");
        bullet.hurtServer(h.getLevel(),h.getLevel().damageSources().playerAttack(player),1.0F);
        h.assertTrue(drops(h,at)==1,"a second damage event cannot duplicate the captured Charge");h.succeed();
    }

    @GameTest(padding=8) public void playerArrowAndOwnerlessArrowBothCapture(GameTestHelper h){
        Vec3 a=new Vec3(3,4,3),b=new Vec3(7,4,3);var player=h.makeMockServerPlayerInLevel();
        var arrowByPlayer=new Arrow(h.getLevel(),player,new ItemStack(Items.ARROW),null);
        bullet(h,a).hurtServer(h.getLevel(),h.getLevel().damageSources().arrow(arrowByPlayer,player),1.0F);
        h.assertTrue(drops(h,a)==1,"player arrow captures bullet");
        Vec3 abs=h.absoluteVec(b);var dispenserArrow=new Arrow(h.getLevel(),abs.x,abs.y,abs.z,new ItemStack(Items.ARROW),null);
        bullet(h,b).hurtServer(h.getLevel(),h.getLevel().damageSources().arrow(dispenserArrow,null),1.0F);
        h.assertTrue(drops(h,b)==1,"ownerless/dispenser-style arrow captures bullet");h.succeed();
    }

    @GameTest(padding=8) public void unrelatedDestructionDoesNotCreateCharge(GameTestHelper h){
        Vec3 at=new Vec3(3,4,3);var bullet=bullet(h,at);
        bullet.hurtServer(h.getLevel(),h.getLevel().damageSources().generic(),1.0F);
        h.assertTrue(drops(h,at)==0,"generic/non-interception destruction produces no Charge");h.succeed();
    }
}
