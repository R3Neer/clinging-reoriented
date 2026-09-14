package io.github.r3neer.clingingreoriented.client;

import io.github.r3neer.clingingreoriented.GravityFallSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client entrypoint kept separate from input/camera ownership so body presentation remains an independent layer. */
public final class GravityFallClientInitializer implements ClientModInitializer {
    @Override public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(GravityFallSync.Visual.TYPE,(packet,context)->GravityFallVisuals.receive(context.client(),packet));
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            GravityFallVisuals.tick(client);
            GravityFallAirSound.tick(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
            GravityFallAirSound.clear();
            GravityFallVisuals.clear();
        });
    }
}
