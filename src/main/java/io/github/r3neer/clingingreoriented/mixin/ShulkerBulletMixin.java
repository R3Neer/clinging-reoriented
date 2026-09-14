package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ShulkerCharges;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBullet.class)
public abstract class ShulkerBulletMixin extends Projectile {
    @Unique private boolean clinging$captured;

    protected ShulkerBulletMixin(EntityType<? extends Projectile> type,Level level){super(type,level);}

    @Inject(method="hurtServer",at=@At("HEAD"))
    private void clinging$dropCapturedCharge(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Boolean> cir){
        if(clinging$captured)return;
        Entity direct=source.getDirectEntity();
        boolean arrow=direct instanceof AbstractArrow;
        boolean melee=!source.is(DamageTypeTags.IS_PROJECTILE) && direct instanceof LivingEntity;
        if(!arrow&&!melee)return;
        clinging$captured=true;
        this.spawnAtLocation(level,ShulkerCharges.ITEM);
    }
}
