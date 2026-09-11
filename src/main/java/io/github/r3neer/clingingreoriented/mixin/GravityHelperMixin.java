package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(GravityDirectionUtil.class)
public abstract class GravityHelperMixin {
    @Inject(method="setGravityDirection",at=@At("HEAD"))
    private static void clinging$external(LivingEntity entity,Direction direction,CallbackInfoReturnable<Boolean> cir){
        if(entity instanceof Player p)ClingingReoriented.externalWrite(p,direction);
        else MobGravity.externalWrite(entity,direction);
    }
}
