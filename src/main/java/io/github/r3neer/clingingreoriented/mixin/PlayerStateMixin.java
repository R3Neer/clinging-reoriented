package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.PlayerData;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class PlayerStateMixin implements PlayerData.Holder {
    @Unique private final PlayerData clinging$state=new PlayerData();
    public PlayerData clinging$data(){return clinging$state;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void clinging$save(ValueOutput output,CallbackInfo ci){
        output.putBoolean("clinging_reoriented:owned",clinging$state.owned);
        output.putBoolean("clinging_reoriented:air_change_used",clinging$state.airChangeUsed);
        output.putInt("clinging_reoriented:direction",clinging$state.selected.get3DDataValue());
        output.putBoolean("clinging_reoriented:anchor_borrowed",clinging$state.anchorBorrowed);
        output.putBoolean("clinging_reoriented:anchor_expired",clinging$state.anchorExpired);
        if(clinging$state.lastSafeDown!=null){output.putDouble("clinging_reoriented:safe_x",clinging$state.lastSafeDown.x);output.putDouble("clinging_reoriented:safe_y",clinging$state.lastSafeDown.y);output.putDouble("clinging_reoriented:safe_z",clinging$state.lastSafeDown.z);}
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void clinging$load(ValueInput input,CallbackInfo ci){
        clinging$state.owned=input.getBooleanOr("clinging_reoriented:owned",false);
        clinging$state.airChangeUsed=input.getBooleanOr("clinging_reoriented:air_change_used",false);
        clinging$state.selected=Direction.from3DDataValue(Math.clamp(input.getIntOr("clinging_reoriented:direction",0),0,5));
        clinging$state.anchorBorrowed=input.getBooleanOr("clinging_reoriented:anchor_borrowed",false);
        clinging$state.anchorExpired=input.getBooleanOr("clinging_reoriented:anchor_expired",false);
        double x=input.getDoubleOr("clinging_reoriented:safe_x",Double.NaN),y=input.getDoubleOr("clinging_reoriented:safe_y",Double.NaN),z=input.getDoubleOr("clinging_reoriented:safe_z",Double.NaN);
        if(Double.isFinite(x+y+z))clinging$state.lastSafeDown=new net.minecraft.world.phys.Vec3(x,y,z);
    }
}
