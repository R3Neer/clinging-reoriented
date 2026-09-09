package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.FallContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Identify the owner of upstream's timer without depending on injected private fields. */
@Mixin(LivingEntity.class)
public abstract class DirectionalFallGraceMixin {
    @WrapMethod(method="tick")
    private void clinging$fallOwner(Operation<Void> original) {
        var previous=FallContext.CURRENT.get();
        FallContext.CURRENT.set((LivingEntity)(Object)this);
        try { original.call(); }
        finally { if(previous==null)FallContext.CURRENT.remove();else FallContext.CURRENT.set(previous); }
    }
}
