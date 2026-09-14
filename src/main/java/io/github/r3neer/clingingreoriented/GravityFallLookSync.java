package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Client-to-server world-space gaze samples used only to derive extra Gravity Fall drag. */
public final class GravityFallLookSync {
    public record Look(long sequence,Vec3 worldLook) implements CustomPacketPayload {
        public static final Type<Look> TYPE=new Type<>(Identifier.fromNamespaceAndPath(ClingingReoriented.ID,"gravity_fall_look_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Look> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarLong(v.sequence);b.writeDouble(v.worldLook.x);b.writeDouble(v.worldLook.y);b.writeDouble(v.worldLook.z);},
            b->new Look(b.readVarLong(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble())));
        @Override public Type<Look> type(){return TYPE;}
    }

    private GravityFallLookSync() {}

    public static void register(){
        PayloadTypeRegistry.serverboundPlay().register(Look.TYPE,Look.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Look.TYPE,(packet,context)->{
            var player=context.player();
            var state=ClingingReoriented.data(player);
            if(!state.gravityFallActive||!ClingingReoriented.controlsPhysics(player)||packet.sequence()<=state.gravityFallLookSequence)return;
            Vec3 look=packet.worldLook();
            if(look==null||!Double.isFinite(look.x+look.y+look.z)||look.lengthSqr()<1.0E-8D)return;
            state.gravityFallLookSequence=packet.sequence();
            state.gravityFallLook=look.normalize();
            state.gravityFallLookTick=player.level().getGameTime();
        });
    }
}
