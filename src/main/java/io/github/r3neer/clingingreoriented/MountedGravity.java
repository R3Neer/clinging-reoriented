package io.github.r3neer.clingingreoriented;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.geometry.LookDirection;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Grounded jump belongs to the mount; every new airborne press may turn its root. */
public final class MountedGravity {
    public static void refresh(Player p){
        if(p.isPassenger() && p.getRootVehicle() instanceof LivingEntity living && AirChanges.grounded(living))ClingingReoriented.data(p).airChangeUsed=false;
    }
    public static ClingingReoriented.Result attempt(ServerPlayer p,Vec3 look){
        if(!p.hasEffect(Reorientation.EFFECT))return ClingingReoriented.Result.BLOCKED;
        var s=ClingingReoriented.data(p);refresh(p);
        if(!(p.getRootVehicle() instanceof LivingEntity root) || root instanceof Player || !MobGravity.supported(root))return ClingingReoriented.Result.BLOCKED;
        if(root.onGround() || AirChanges.grounded(root))return ClingingReoriented.Result.MOUNT_ACTION;
        if(root.isFallFlying() || root.isInWater())return ClingingReoriented.Result.BLOCKED;
        Direction direction=LookDirection.select(look);
        if(direction==null)return ClingingReoriented.Result.AMBIGUOUS;
        if(direction==GravityDirectionUtil.getGravityDirection(root))return ClingingReoriented.Result.UNCHANGED;
        if(!MobGravity.borrow(root,direction))return ClingingReoriented.Result.NO_SPACE;
        MobGravity.state(root).airUsed=true;
        s.airChangeUsed=true;root.positionRider(p);p.setOnGround(false);
        GravityBreadcrumbs.record(p,direction);
        return ClingingReoriented.Result.SUCCESS;
    }
}
