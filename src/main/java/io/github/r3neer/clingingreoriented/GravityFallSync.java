package io.github.r3neer.clingingreoriented;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Sparse server->client Gravity Fall phase synchronization. Continuous body orientation is never networked. */
public final class GravityFallSync {
    public enum Phase { START, LAND, RESUME, RESET }

    public record Visual(int entity, UUID entityUuid, int phase, int direction, float etaTicks, long sequence) implements CustomPacketPayload {
        public static final Type<Visual> TYPE=new Type<>(Identifier.fromNamespaceAndPath(ClingingReoriented.ID,"gravity_fall_visual_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Visual> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarInt(v.entity);b.writeUUID(v.entityUuid);b.writeVarInt(v.phase);b.writeVarInt(v.direction);b.writeFloat(v.etaTicks);b.writeVarLong(v.sequence);},
            b->new Visual(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readFloat(),b.readVarLong()));
        @Override public Type<Visual> type(){return TYPE;}
    }

    private GravityFallSync() {}

    public static void register(){PayloadTypeRegistry.clientboundPlay().register(Visual.TYPE,Visual.CODEC);}

    public static void publish(ServerPlayer player,Phase phase,Direction target,double etaTicks){
        var state=ClingingReoriented.data(player);
        long sequence=++state.gravityFallSequence;
        float eta=phase==Phase.LAND && Double.isFinite(etaTicks)?(float)Math.max(0.0D,etaTicks):0.0F;
        var packet=new Visual(player.getId(),player.getUUID(),phase.ordinal(),target==null?-1:target.get3DDataValue(),eta,sequence);
        Set<ServerPlayer> recipients=new LinkedHashSet<>(PlayerLookup.tracking(player));
        recipients.add(player);
        for(ServerPlayer recipient:recipients)if(ServerPlayNetworking.canSend(recipient,Visual.TYPE))ServerPlayNetworking.send(recipient,packet);
    }

    public static void publish(ServerPlayer player,Phase phase,Direction target){publish(player,phase,target,0.0D);}

    /**
     * A late/re-entering tracker receives the current semantic phase, not a replay of the original
     * start event. The snapshot gets a fresh global epoch even if the phase did not change: clients
     * deliberately retain their latest UUID-scoped sequence after dropping unresolved render state,
     * so replaying the old START/LAND sequence would be rejected after a long tracking gap.
     */
    public static void sendSnapshot(ServerPlayer player,ServerPlayer recipient){
        var state=ClingingReoriented.data(player);
        if(!state.gravityFallActive)return;
        long sequence=++state.gravityFallSequence;
        if(!ServerPlayNetworking.canSend(recipient,Visual.TYPE))return;
        Phase phase=state.gravityFallLanding?Phase.LAND:Phase.START;
        Direction target=state.gravityFallLanding?state.gravityFallLandingGravity:null;
        float eta=state.gravityFallLanding?(float)Math.max(0.0D,state.gravityFallLandingEtaTicks):0.0F;
        ServerPlayNetworking.send(recipient,new Visual(player.getId(),player.getUUID(),phase.ordinal(),target==null?-1:target.get3DDataValue(),eta,sequence));
    }
}
