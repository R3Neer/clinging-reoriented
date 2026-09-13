package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.core.Direction;
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
    private void clinging$saveMob(ValueOutput out,CallbackInfo ci){
        out.putInt("clinging_reoriented:mob_ownership",clinging$mobState.ownership.ordinal());
        out.putInt("clinging_reoriented:mob_owned_direction",clinging$mobState.ownedDirection.get3DDataValue());
        out.putInt("clinging_reoriented:mob_borrow_previous_ownership",clinging$mobState.borrowedPreviousOwnership.ordinal());
        out.putInt("clinging_reoriented:mob_borrow_previous_direction",clinging$mobState.borrowedPreviousDirection.get3DDataValue());
        out.putBoolean("clinging_reoriented:mob_air_used",clinging$mobState.airUsed);
        out.putLong("clinging_reoriented:mob_visual_sequence",clinging$mobState.visualSequence);
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void clinging$loadMob(ValueInput in,CallbackInfo ci){
        int ownership=in.getIntOr("clinging_reoriented:mob_ownership",-1);
        if(ownership>=0&&ownership<MobGravity.Ownership.values().length){
            clinging$mobState.ownership=MobGravity.Ownership.values()[ownership];
            clinging$mobState.ownedDirection=Direction.from3DDataValue(Math.clamp(in.getIntOr("clinging_reoriented:mob_owned_direction",0),0,5));
            int previous=Math.clamp(in.getIntOr("clinging_reoriented:mob_borrow_previous_ownership",0),0,MobGravity.Ownership.values().length-1);
            clinging$mobState.borrowedPreviousOwnership=MobGravity.Ownership.values()[previous];
            clinging$mobState.borrowedPreviousDirection=Direction.from3DDataValue(Math.clamp(in.getIntOr("clinging_reoriented:mob_borrow_previous_direction",0),0,5));
        }else{
            // Old effectSeen merely proved that an effect had existed, not that this mod
            // authored the gravity. Migrate ambiguous non-DOWN state as EXTERNAL.
            boolean borrowed=in.getBooleanOr("clinging_reoriented:mob_borrowed",false);
            boolean effectSeen=in.getBooleanOr("clinging_reoriented:mob_effect_seen",false);
            Direction actual=GravityDirectionUtil.getOwnGravityDirection((LivingEntity)(Object)this);
            if(borrowed){clinging$mobState.ownership=MobGravity.Ownership.BORROWED_RIDER;clinging$mobState.ownedDirection=actual;}
            else if(effectSeen&&actual!=Direction.DOWN){clinging$mobState.ownership=MobGravity.Ownership.EXTERNAL;clinging$mobState.ownedDirection=actual;}
        }
        clinging$mobState.airUsed=in.getBooleanOr("clinging_reoriented:mob_air_used",false);
        clinging$mobState.visualSequence=Math.max(0L,in.getLongOr("clinging_reoriented:mob_visual_sequence",0L));
    }
}
