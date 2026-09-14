package io.github.r3neer.clingingreoriented.client;

import io.github.r3neer.clingingreoriented.GravityFallLookSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Sends sparse world-space camera gaze samples while local Gravity Fall owns body presentation. */
public final class GravityFallAerodynamicsClientInitializer implements ClientModInitializer {
    private static final double RESEND_DOT=Math.cos(Math.toRadians(1.5D));
    private static final int HEARTBEAT_TICKS=6;
    private static long sequence;
    private static Vec3 lastLook;
    private static int ticksSinceSend;

    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            var player=client.player;
            if(player==null||!GravityFallVisuals.active(player)||!ClientPlayNetworking.canSend(GravityFallLookSync.Look.TYPE)){
                lastLook=null;ticksSinceSend=0;return;
            }
            Vector3f forward=client.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
            Vec3 look=new Vec3(forward.x,forward.y,forward.z);
            if(!Double.isFinite(look.x+look.y+look.z)||look.lengthSqr()<1.0E-8D)return;
            look=look.normalize();ticksSinceSend++;
            boolean changed=lastLook==null||lastLook.dot(look)<RESEND_DOT;
            if(changed||ticksSinceSend>=HEARTBEAT_TICKS){
                ClientPlayNetworking.send(new GravityFallLookSync.Look(++sequence,look));
                lastLook=look;ticksSinceSend=0;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{sequence=0;lastLook=null;ticksSinceSend=0;});
    }
}
