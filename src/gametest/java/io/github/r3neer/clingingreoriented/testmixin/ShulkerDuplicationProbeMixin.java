package io.github.r3neer.clingingreoriented.testmixin;

import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Diagnostic-only probe for the private vanilla Shulker duplication path. */
@Mixin(Shulker.class)
public abstract class ShulkerDuplicationProbeMixin {
    @Inject(
        method="hitByShulkerBullet",
        at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;")
    )
    private void clinging$beforeOffspringCreate(CallbackInfo ci){
        Shulker self=(Shulker)(Object)this;
        System.out.println("[SC-DUP-PROBE] before-create uuid="+self.getUUID()+" pos="+self.position()+" health="+self.getHealth()+" max="+self.getMaxHealth());
    }
}
