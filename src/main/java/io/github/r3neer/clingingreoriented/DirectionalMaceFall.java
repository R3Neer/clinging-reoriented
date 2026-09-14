package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Geometric fall-height accounting for the mace under directional gravity.
 *
 * <p>Gravity Changer correctly feeds gravity-local movement into vanilla fallDistance, but that
 * value may be scaled by gravity strength and can outlive the spatial axis that produced it.
 * Mace damage wants something simpler: literal blocks travelled along the current fall direction
 * since this directional fall segment began.
 */
public final class DirectionalMaceFall {
    private static final double FALL_EPSILON=1.0E-5D;

    private DirectionalMaceFall() {}

    public static void tick(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(!directionalContext(player)||!eligible(player)){
            state.clearMaceFall();
            return;
        }

        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        Vec3 position=player.position();
        if(state.maceFallLastPosition==null||state.maceFallDirection!=gravity){
            beginDirection(state,gravity,position);
            return;
        }

        Vec3 delta=position.subtract(state.maceFallLastPosition);
        state.maceFallLastPosition=position;
        double fallen=alongGravity(delta,gravity);
        double fallSpeed=alongGravity(player.getDeltaMovement(),gravity);
        boolean unsupported=!AirChanges.grounded(player)&&!player.onClimbable();
        boolean falling=unsupported&&(fallen>FALL_EPSILON||fallSpeed>FALL_EPSILON);

        if(!falling){
            state.maceFallDistance=0.0D;
            state.maceFallActive=false;
            return;
        }
        if(!state.maceFallActive)state.maceFallDistance=0.0D;
        state.maceFallActive=true;
        if(fallen>0.0D)state.maceFallDistance+=fallen;
    }

    /** Value substituted only for mace fall-distance reads in a directional-gravity context. */
    public static double value(Entity entity,double vanilla){
        if(!(entity instanceof Player player)||!directionalContext(player))return vanilla;
        var state=ClingingReoriented.data(player);
        Direction gravity=GravityDirectionUtil.getGravityDirection(player);
        if(state.maceFallLastPosition==null||state.maceFallDirection!=gravity)return 0.0D;
        double value=state.maceFallDistance;
        if(!AirChanges.grounded(player)&&!player.onClimbable()){
            double pending=alongGravity(player.position().subtract(state.maceFallLastPosition),gravity);
            if(pending>0.0D)value+=pending;
        }
        return Math.max(0.0D,value);
    }

    public static void rebase(Player player){
        var state=ClingingReoriented.data(player);
        state.clearMaceFall();
        if(directionalContext(player))beginDirection(state,GravityDirectionUtil.getGravityDirection(player),player.position());
    }

    static double alongGravity(Vec3 delta,Direction gravity){
        if(delta==null||gravity==null||!Double.isFinite(delta.x+delta.y+delta.z))return 0.0D;
        return delta.x*gravity.getStepX()+delta.y*gravity.getStepY()+delta.z*gravity.getStepZ();
    }

    private static void beginDirection(PlayerData state,Direction gravity,Vec3 position){
        state.maceFallDirection=gravity;
        state.maceFallLastPosition=position;
        state.maceFallDistance=0.0D;
        state.maceFallActive=false;
    }

    private static boolean directionalContext(Player player){
        return ClingingReoriented.controlsPhysics(player)
            ||GravityDirectionUtil.getGravityDirection(player)!=Direction.DOWN;
    }

    private static boolean eligible(Player player){
        return player.isAlive()&&!player.isSpectator()&&!player.isPassenger()&&!player.isFallFlying()
            &&!player.getAbilities().flying&&!player.isInWater()&&!player.isInLava();
    }
}
