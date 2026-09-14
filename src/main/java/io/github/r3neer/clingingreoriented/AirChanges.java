package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class AirChanges {
    /** Check real support in the active frame, never replenish from a client ground flag alone. */
    public static boolean grounded(LivingEntity p) {
        if (p.isPassenger() || p.isFallFlying() || FluidContext.intersects(p)
            || p instanceof Player player && player.getAbilities().flying) return false;
        var gravity=GravityDirectionUtil.getGravityDirection(p);
        if(p.getDeltaMovement().dot(FaceGeometry.vector(gravity.getOpposite()))>1e-5)return false;

        // Public geometry seam first. Vanilla itself still requires the ordinary onGround flag,
        // while a future custom provider may establish real support without forging that flag.
        if(LandingSurfaces.currentSupport(p,gravity).isPresent())return true;

        // Existing optional integrations remain legacy fallbacks until their own mods adopt the API.
        // Preserve their previous onGround gate so S00 changes no current compatibility semantics.
        if(!p.onGround())return false;
        var body=p.getBoundingBox();
        var probe=body.inflate(1e-4);
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
