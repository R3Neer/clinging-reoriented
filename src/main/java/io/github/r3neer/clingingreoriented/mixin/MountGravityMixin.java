package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.MobGravity;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Entity.class)
public abstract class MountGravityMixin {
    @WrapMethod(method="startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z")
    private boolean clinging$mount(Entity vehicle,boolean force,boolean sendEvents,Operation<Boolean> original){
        var self=(Entity)(Object)this;var before=GravityDirectionUtil.getGravityDirection(self);
        boolean mounted=original.call(vehicle,force,sendEvents);
        // Internal teleport reattachment must not overwrite the root's preserved frame.
        if(mounted && sendEvents && !self.level().isClientSide() && self instanceof Player p)MobGravity.transfer(p,vehicle,before);
        return mounted;
    }
}
