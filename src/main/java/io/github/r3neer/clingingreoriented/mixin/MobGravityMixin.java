package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class MobGravityMixin implements MobGravity.Holder {
    @Unique private final MobGravity.State clinging$mobState=new MobGravity.State();
    public MobGravity.State clinging$mobGravity(){return clinging$mobState;}
    @Inject(method="tick",at=@At("TAIL"))
    private void clinging$lifetime(CallbackInfo ci){MobGravity.tick((LivingEntity)(Object)this);}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void clinging$saveMob(ValueOutput out,CallbackInfo ci){out.putBoolean("clinging_reoriented:mob_effect_seen",clinging$mobState.effectSeen);out.putBoolean("clinging_reoriented:mob_borrowed",clinging$mobState.borrowed);out.putBoolean("clinging_reoriented:mob_air_used",clinging$mobState.airUsed);}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void clinging$loadMob(ValueInput in,CallbackInfo ci){clinging$mobState.effectSeen=in.getBooleanOr("clinging_reoriented:mob_effect_seen",false);clinging$mobState.borrowed=in.getBooleanOr("clinging_reoriented:mob_borrowed",false);clinging$mobState.airUsed=in.getBooleanOr("clinging_reoriented:mob_air_used",false);}
}
