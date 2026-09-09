package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import com.moigferdsrte.gravitychanger.init.ModEvents;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
@Mixin(ModEvents.class)
public abstract class AnchorMixin {
    @Shadow @Final private static Map<UUID,Direction> GRAVITY_BEFORE_ANCHOR;
    @Inject(method="tickGravityAnchor",at=@At("HEAD"))
    private static void clinging$borrow(ServerPlayer p,CallbackInfo ci){
        var s=ClingingReoriented.data(p);
        if(ClingingReoriented.anchor(p) && s.owned && !GRAVITY_BEFORE_ANCHOR.containsKey(p.getUUID())) {s.anchorBorrowed=true;s.anchorExpired=false;s.unbind();Payloads.publish(p);}
        if(s.anchorBorrowed && !ClingingReoriented.hasEffect(p)) s.anchorExpired=true;
    }
    @Inject(method="restoreGravityBeforeAnchor",at=@At("HEAD"),cancellable=true)
    private static void clinging$restore(ServerPlayer p,CallbackInfo ci){
        var s=ClingingReoriented.data(p);
        if(!s.anchorBorrowed) return;
        Direction previous=GRAVITY_BEFORE_ANCHOR.remove(p.getUUID());
        s.anchorBorrowed=false;
        if(s.anchorExpired || !ClingingReoriented.hasEffect(p)) {
            s.retirementPending=!ClingingReoriented.retire(p);
            s.owned=s.retirementPending;
            if(s.retirementPending)s.selected=com.moigferdsrte.gravitychanger.util.GravityDirectionUtil.getOwnGravityDirection(p);
        } else ClingingReoriented.write(p,s.selected);
        s.anchorExpired=false;Payloads.publish(p);ci.cancel();
    }
}
