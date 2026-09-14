package io.github.r3neer.clingingreoriented;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/** Captures the pre-movement safe anchor, then fences any invalid post-movement result. */
public final class FlightSafetyInitializer implements ModInitializer {
    @Override public void onInitialize(){
        ServerTickEvents.START_SERVER_TICK.register(server->{
            for(ServerPlayer player:server.getPlayerList().getPlayers())FlightSafety.capture(player);
        });
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer player:server.getPlayerList().getPlayers())FlightSafety.tick(player);
        });
    }
}
