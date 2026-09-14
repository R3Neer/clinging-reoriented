package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.DirectionalMaceFall;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MaceItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Give mace thresholds/damage/knockback a geometric directional fall height. */
@Mixin(MaceItem.class)
public abstract class DirectionalMaceMixin {
    @Redirect(
        method={"hurtEnemy","getAttackDamageBonus","getKnockbackPower","canSmashAttack"},
        at=@At(value="FIELD",target="Lnet/minecraft/world/entity/Entity;fallDistance:F")
    )
    private static float clinging$directionalMaceHeight(Entity entity){
        return DirectionalMaceFall.value(entity,entity.fallDistance);
    }
}
