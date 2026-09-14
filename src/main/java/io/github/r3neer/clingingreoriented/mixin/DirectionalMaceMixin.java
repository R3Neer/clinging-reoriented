package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.DirectionalMaceFall;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Give mace thresholds/damage/knockback a geometric directional fall height. */
@Mixin(MaceItem.class)
public abstract class DirectionalMaceMixin {
    // Most mace routines statically type the attacker as LivingEntity, so javac emits the
    // inherited fallDistance access with LivingEntity as its constant-pool owner.
    @Redirect(
        method={"hurtEnemy","getAttackDamageBonus","canSmashAttack"},
        at=@At(value="FIELD",target="Lnet/minecraft/world/entity/LivingEntity;fallDistance:D")
    )
    private static double clinging$directionalLivingMaceHeight(LivingEntity entity){
        return DirectionalMaceFall.value(entity,entity.fallDistance);
    }

    // getKnockbackPower receives its attacker as Entity and therefore uses Entity as field owner.
    @Redirect(
        method="getKnockbackPower",
        at=@At(value="FIELD",target="Lnet/minecraft/world/entity/Entity;fallDistance:D")
    )
    private static double clinging$directionalEntityMaceHeight(Entity entity){
        return DirectionalMaceFall.value(entity,entity.fallDistance);
    }
}
