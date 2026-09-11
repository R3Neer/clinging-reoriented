package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.AnatomyBridge;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.ScaleBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.r3neer.scalebrews.platform.PlatformPhysics", remap = false)
public abstract class ScalePhysicsMixin {
    @Unique
    private static boolean clinging$legacy(Entity entity) {
        return ClingingReoriented.controlsPhysics(entity) && !AnatomyBridge.active(entity);
    }

    @Inject(method = {"carry", "afterMove"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void clinging$singleTransport(Entity entity, CallbackInfo ci) {
        if (clinging$legacy(entity)) {
            ScaleBridge.clear(entity);
            ci.cancel();
        }
    }

    @Inject(method = "contact", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void clinging$contact(Entity entity, Vec3 delta, CallbackInfoReturnable<Object> cir) {
        if (clinging$legacy(entity)) cir.setReturnValue(null);
    }

    @Inject(method = "collide", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void clinging$collide(Entity entity, Vec3 requested, Vec3 vanilla, CallbackInfoReturnable<Vec3> cir) {
        if (clinging$legacy(entity)) cir.setReturnValue(vanilla);
    }

    @Inject(method = "edge", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void clinging$edge(Player entity, Vec3 delta, CallbackInfoReturnable<Vec3> cir) {
        if (clinging$legacy(entity)) cir.setReturnValue(delta);
    }
}
