package io.github.r3neer.clingingreoriented.testmixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
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

    @Inject(
        method="hitByShulkerBullet",
        at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z")
    )
    private void clinging$beforeOffspringAdd(CallbackInfo ci){
        Shulker self=(Shulker)(Object)this;
        System.out.println("[SC-DUP-PROBE] before-add uuid="+self.getUUID()+" pos="+self.position()+" health="+self.getHealth()+" max="+self.getMaxHealth());
    }
}
