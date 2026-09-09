package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.GravityBreadcrumbs;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FollowOwnerGoal.class)
public abstract class FollowGravityMixin {
    @Shadow @Final private TamableAnimal tamable;
    @Inject(method="tick",at=@At("HEAD"))
    private void clinging$follow(CallbackInfo ci){GravityBreadcrumbs.follow(tamable);}
}
