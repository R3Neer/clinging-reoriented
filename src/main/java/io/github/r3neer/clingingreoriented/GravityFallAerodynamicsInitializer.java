package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative body-follow, momentum redirection and aerodynamic drag. */
public final class GravityFallAerodynamicsInitializer implements ModInitializer {
    private static final long LOOK_STALE_TICKS=10L;

    @Override public void onInitialize(){
        GravityFallLookSync.register();
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer player:server.getPlayerList().getPlayers())tick(player);
        });
    }

    static void tick(ServerPlayer player){
        var state=ClingingReoriented.data(player);
        if(!state.gravityFallActive||state.gravityFallLanding||!ClingingReoriented.controlsPhysics(player)
            ||player.isInWater()||player.isInLava()||player.isFallFlying()||player.isPassenger()||!player.isAlive()){
            state.gravityFallAeroBody=null;
            return;
        }

        Vec3 velocity=player.getDeltaMovement();
        Vec3 direction=BodyOrientation.reliableDirection(velocity);
        if(direction==null)return;

        if(state.gravityFallAeroBody==null){
            state.gravityFallAeroBody=BodyOrientation.start(
                RotationUtil.getEntityRotationQuaternion(GravityDirectionUtil.getGravityDirection(player)),velocity);
        }else{
            state.gravityFallAeroBody=BodyOrientation.transport(state.gravityFallAeroBody,velocity);
        }

        long age=player.level().getGameTime()-state.gravityFallLookTick;
        boolean freshLook=state.gravityFallLook!=null&&age>=0L&&age<=LOOK_STALE_TICKS;
        if(freshLook){
            state.gravityFallAeroBody=GravityFallAerodynamics.followLook(
                state.gravityFallAeroBody,state.gravityFallLook,player.yBodyRot);

            // W spends existing fall momentum to bend the trajectory toward gaze. This happens
            // before drag so steering itself is energy-neutral; any speed loss comes from posture.
            velocity=GravityFallAerodynamics.redirectMomentum(
                velocity,state.gravityFallLook,state.gravityFallForwardIntent);
        }

        double streamline=GravityFallAerodynamics.streamlining(state.gravityFallAeroBody,velocity);
        player.setDeltaMovement(GravityFallAerodynamics.applyDrag(velocity,streamline));
    }
}
