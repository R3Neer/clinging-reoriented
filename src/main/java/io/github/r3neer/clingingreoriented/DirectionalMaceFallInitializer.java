package io.github.r3neer.clingingreoriented;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerPlayer;

/** Advances geometric mace fall segments after each authoritative movement tick. */
public final class DirectionalMaceFallInitializer implements ModInitializer {
    @Override public void onInitialize(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer player:server.getPlayerList().getPlayers())DirectionalMaceFall.tick(player);
        });
    }
}
