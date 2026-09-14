package io.github.r3neer.clingingreoriented;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/** Runs after core ownership reconciliation and before Gravity Fall presentation state. */
public final class FlightSafetyInitializer implements ModInitializer {
    @Override public void onInitialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer player:server.getPlayerList().getPlayers())FlightSafety.tick(player);
        });
    }
}
