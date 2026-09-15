package io.github.r3neer.clingingreoriented.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.r3neer.clingingreoriented.FallOwnerContext;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Identify the owner of Gravity Changer's timer while retaining one ThreadLocal entry per thread. */
@Mixin(LivingEntity.class)
public abstract class DirectionalFallOwnerMixin {
    @WrapMethod(method="tick")
    private void clinging$fallOwner(Operation<Void> original){
        LivingEntity previous=FallOwnerContext.CURRENT.get();
        FallOwnerContext.CURRENT.set((LivingEntity)(Object)this);
        try{original.call();}
        finally{
            // Keep the ThreadLocalMap entry alive. get() still returns null outside a top-level tick,
            // but the JVM no longer creates a new ThreadLocalMap.Entry for every living entity.
            FallOwnerContext.CURRENT.set(previous);
        }
    }
}
