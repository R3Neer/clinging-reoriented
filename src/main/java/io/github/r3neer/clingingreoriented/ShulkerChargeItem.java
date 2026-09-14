package io.github.r3neer.clingingreoriented;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/** Captured shulker projectile; both player and dispenser launch the same vanilla entity type. */
public final class ShulkerChargeItem extends Item implements ProjectileItem {
    public ShulkerChargeItem(Properties properties){super(properties);}

    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);Vec3 intent=ShulkerChargeTargeting.safeIntent(player.getLookAngle());
        if(level instanceof ServerLevel server){
            var bullet=new ShulkerChargeBullet(server);Vec3 start=new Vec3(player.getX(),player.getEyeY(),player.getZ()).add(intent.scale(0.5D));
            bullet.snapTo(start.x,start.y,start.z,player.getYRot(),player.getXRot());bullet.setOwner(player);
            ((ShulkerChargeProjectile)(Object)bullet).clinging$initializeCharge(intent);server.addFreshEntity(bullet);
        }
        level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SHULKER_SHOOT,SoundSource.PLAYERS,1.0F,1.0F);
        player.awardStat(Stats.ITEM_USED.get(this));stack.consume(1,player);return InteractionResult.SUCCESS;
    }

    @Override public Projectile asProjectile(Level level,Position position,ItemStack stack,Direction direction){
        var bullet=new ShulkerChargeBullet(level);bullet.snapTo(position.x(),position.y(),position.z(),0.0F,0.0F);
        ((ShulkerChargeProjectile)(Object)bullet).clinging$initializeCharge(new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ()));
        return bullet;
    }

    @Override public void shoot(Projectile projectile,double x,double y,double z,float power,float uncertainty){/* ShulkerChargeBullet owns routing. */}
    @Override public DispenseConfig createDispenseConfig(){
        return DispenseConfig.builder().positionFunction((source,direction)->DispenserBlock.getDispensePosition(source,1.0D,Vec3.ZERO)).power(1.0F).uncertainty(0.0F).build();
    }
}
