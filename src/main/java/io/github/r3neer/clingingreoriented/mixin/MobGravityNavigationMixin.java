package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.MobGravityNavigation;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Advances generic gravity locomotion after vanilla goals/navigation have emitted their intent for this AI tick. */
@Mixin(Mob.class)
public abstract class MobGravityNavigationMixin {
    @Inject(method="serverAiStep",at=@At("TAIL"))
    private void clinging$gravityNavigationTick(CallbackInfo ci){
        MobGravityNavigation.tick((Mob)(Object)this);
    }
}
