package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.scalebrews.platform.PlatformPhysics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(PlatformPhysics.class)
public abstract class ScalePhysicsMixin {
    @org.spongepowered.asm.mixin.Unique
    private static boolean clinging$legacy(Entity e){return ClingingReoriented.controlsPhysics(e) && !io.github.r3neer.clingingreoriented.AnatomyBridge.active(e);}
    @Inject(method={"carry","afterMove"},at=@At("HEAD"),cancellable=true)
    private static void clinging$singleTransport(Entity e,CallbackInfo ci){if(clinging$legacy(e)){io.github.r3neer.clingingreoriented.ScaleBridge.clear(e);ci.cancel();}}
    @Inject(method="contact",at=@At("HEAD"),cancellable=true)
    private static void clinging$contact(Entity e,Vec3 delta,CallbackInfoReturnable<PlatformPhysics.Contact> cir){if(clinging$legacy(e))cir.setReturnValue(null);}
    @Inject(method="collide",at=@At("HEAD"),cancellable=true)
    private static void clinging$collide(Entity e,Vec3 requested,Vec3 vanilla,CallbackInfoReturnable<Vec3> cir){if(clinging$legacy(e))cir.setReturnValue(vanilla);}
    @Inject(method="edge",at=@At("HEAD"),cancellable=true)
    private static void clinging$edge(Player e,Vec3 delta,CallbackInfoReturnable<Vec3> cir){if(clinging$legacy(e))cir.setReturnValue(delta);}
}
