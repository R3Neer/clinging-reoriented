package io.github.r3neer.clingingreoriented;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;

/** Kept as a second common entrypoint so Gravity Fall can evolve without entangling the core ownership loop. */
public final class GravityFallInitializer implements ModInitializer {
    @Override public void onInitialize(){
        GravityFallSync.register();
        // This entrypoint is listed after ClingingReoriented, so its END_SERVER_TICK listener observes
        // LandingState's freshly updated airborne counter/commitment from the same server tick.
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer player:server.getPlayerList().getPlayers())GravityFallState.tick(player);
        });
        EntityTrackingEvents.START_TRACKING.register((entity,viewer)->{
            if(entity instanceof ServerPlayer player)GravityFallSync.sendSnapshot(player,viewer);
        });
    }
}
