package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ImpactState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class ImpactStateMixin implements ImpactState.Holder {
    @Unique private final ImpactState.State clinging$impactState=new ImpactState.State();
    @Override public ImpactState.State clinging$impactState(){return clinging$impactState;}
}
