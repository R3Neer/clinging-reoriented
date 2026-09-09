package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(LivingEntity.class)
public abstract class JumpMixin {
    @WrapMethod(method="jumpFromGround")
    private void clinging$departure(Operation<Void> original){
        Player p=(Object)this instanceof Player player?player:null;
        var s=p==null?null:ClingingReoriented.data(p);
        if(p!=null && !p.level().isClientSide())AirChanges.refresh(p);
        boolean anatomy=p!=null && AnatomyBridge.active(p);
        Vec3 inherited=!anatomy && s!=null && s.groundedOnSurface?s.lastTransport:Vec3.ZERO;
        original.call();
        if(anatomy)AnatomyBridge.clear(p); // Departure keeps vanilla jump, without support impulse.
        if(p!=null && s.groundedOnSurface){p.addDeltaMovement(inherited);s.groundedOnSurface=false;s.lastTransport=Vec3.ZERO;}
    }
}
