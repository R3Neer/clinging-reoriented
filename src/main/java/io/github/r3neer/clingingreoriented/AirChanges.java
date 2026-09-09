package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class AirChanges {
    /** Check real support in the active frame, never replenish from a client ground flag alone. */
    public static boolean grounded(LivingEntity p) {
        if (!p.onGround() || p.isPassenger() || p.isFallFlying() || p instanceof Player player && player.getAbilities().flying) return false;
        var gravity=GravityDirectionUtil.getGravityDirection(p);
        if(p.getDeltaMovement().dot(FaceGeometry.vector(gravity.getOpposite()))>1e-5)return false;
        var body=p.getBoundingBox();
        var probe=body.inflate(1e-4);
        for(var shape:p.level().getBlockCollisions(p,probe))
            for(var box:shape.toAabbs())if(FaceGeometry.touching(body,box,gravity))return true;
        if(AnatomyBridge.active(p))return AnatomyBridge.supported(p);
        if(ScaleBridge.PRESENT)
            for(var entity:p.level().getEntitiesOfClass(LivingEntity.class,probe,e->p instanceof Player player?MovingSurface.canBind(player,e):ScaleBridge.eligible(p,e)))
                if(FaceGeometry.touching(body,entity.getBoundingBox(),gravity))return true;
        return false;
    }
    public static void refresh(Player p) {
        if(ClingingReoriented.data(p).airChangeUsed && grounded(p))ClingingReoriented.data(p).airChangeUsed=false;
    }
}
