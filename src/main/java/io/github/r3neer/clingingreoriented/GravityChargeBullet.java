package io.github.r3neer.clingingreoriented;

import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;

/**
 * Creation-time subtype only. Its EntityType remains the exact vanilla SHULKER_BULLET;
 * the subtype merely stops dispenser ProjectileDispenseBehavior from overwriting the
 * cardinal velocity selected by the Gravity Charge initializer.
 */
public final class GravityChargeBullet extends ShulkerBullet {
    public GravityChargeBullet(Level level){super(EntityTypes.SHULKER_BULLET,level);}
    @Override public void shoot(double x,double y,double z,float power,float uncertainty){/* routing already initialized */}
}
