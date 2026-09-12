package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class Payloads {
    public record MoveReference(int support,int revision,net.minecraft.world.phys.Vec3 absolute,net.minecraft.world.phys.Vec3 origin) implements CustomPacketPayload {
        public static final Type<MoveReference> TYPE=Payloads.type("move_reference");
        public static final StreamCodec<RegistryFriendlyByteBuf,MoveReference> CODEC=StreamCodec.of((b,v)->{
            b.writeVarInt(v.support);b.writeVarInt(v.revision);
            b.writeDouble(v.absolute.x);b.writeDouble(v.absolute.y);b.writeDouble(v.absolute.z);
            b.writeDouble(v.origin.x);b.writeDouble(v.origin.y);b.writeDouble(v.origin.z);
        },b->new MoveReference(b.readVarInt(),b.readVarInt(),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble())));
        @Override public Type<MoveReference> type(){return TYPE;}
    }
    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String path) { return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ClingingReoriented.ID,path)); }
    public record Request(long sequence,int revision,net.minecraft.world.phys.Vec3 selectionLook,net.minecraft.world.phys.Vec3 navigationHeading) implements CustomPacketPayload {
        public static final Type<Request> TYPE=Payloads.type("select_intent_v3");
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,v)->{
            b.writeVarLong(v.sequence);b.writeVarInt(v.revision);
            b.writeDouble(v.selectionLook.x);b.writeDouble(v.selectionLook.y);b.writeDouble(v.selectionLook.z);
            b.writeDouble(v.navigationHeading.x);b.writeDouble(v.navigationHeading.y);b.writeDouble(v.navigationHeading.z);
        },b->new Request(b.readVarLong(),b.readVarInt(),
            new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble()),
            new net.minecraft.world.phys.Vec3(b.readDouble(),b.readDouble(),b.readDouble())));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Reply(long sequence, int result) implements CustomPacketPayload {
        public static final Type<Reply> TYPE=Payloads.type("result");
        public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.of((b,v)->{b.writeVarLong(v.sequence);b.writeVarInt(v.result);},b->new Reply(b.readVarLong(),b.readVarInt()));
        @Override public Type<Reply> type(){return TYPE;}
    }
    /** Sent only to the affected player for a transition initiated by Clinging/Reorientation. */
    public record VisualTransition(int direction,float yawDelta,int kind,long sequence) implements CustomPacketPayload {
        public static final Type<VisualTransition> TYPE=Payloads.type("visual_transition_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,VisualTransition> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarInt(v.direction);b.writeFloat(v.yawDelta);b.writeVarInt(v.kind);b.writeVarLong(v.sequence);},
            b->new VisualTransition(b.readVarInt(),b.readFloat(),b.readVarInt(),b.readVarLong()));
        @Override public Type<VisualTransition> type(){return TYPE;}
    }
    public record State(int player,int direction,boolean owned,boolean visualOwned,int support,java.util.UUID supportUuid,int revision) implements CustomPacketPayload {
        public static final Type<State> TYPE=Payloads.type("state_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.of((b,v)->{b.writeVarInt(v.player);b.writeVarInt(v.direction);b.writeBoolean(v.owned);b.writeBoolean(v.visualOwned);b.writeVarInt(v.support);b.writeUUID(v.supportUuid);b.writeVarInt(v.revision);},b->new State(b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readUUID(),b.readVarInt()));
        @Override public Type<State> type(){return TYPE;}
    }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(MoveReference.TYPE,MoveReference.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MoveReference.TYPE,(r,c)->{
            var s=ClingingReoriented.data(c.player());
            if(ClingingReoriented.managed(c.player()) && s.supportId==r.support && s.revision==r.revision && Double.isFinite(r.absolute.lengthSqr()+r.origin.lengthSqr()))s.pendingMove=r;
        });
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Reply.TYPE,Reply.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisualTransition.TYPE,VisualTransition.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(request,context)-> {
            var p=context.player(); var s=ClingingReoriented.data(p);
            long tick=p.level().getGameTime();
            if(request.sequence<0 || request.sequence<=s.lastRequest) return;
            s.lastRequest=request.sequence;
            if(request.revision!=s.revision){ServerPlayNetworking.send(p,new Reply(request.sequence,ClingingReoriented.Result.BLOCKED.ordinal()));return;}
            if(s.requestTick==tick) { ServerPlayNetworking.send(p,new Reply(request.sequence,ClingingReoriented.Result.BLOCKED.ordinal())); return; }
            s.requestTick=tick;
            var result=ClingingReoriented.attempt(p,request.selectionLook,request.navigationHeading);
            ServerPlayNetworking.send(p,new Reply(request.sequence,result.ordinal()));
        });
    }
    public static void visual(ServerPlayer p,GravityTransition.Plan plan){
        var s=ClingingReoriented.data(p);long sequence=++s.visualSequence;
        if(ServerPlayNetworking.canSend(p,VisualTransition.TYPE))ServerPlayNetworking.send(p,new VisualTransition(
            plan.target().get3DDataValue(),plan.yawDelta(),plan.kind().ordinal(),sequence));
    }
    /** Mount-transfer compatibility: if the effective rider frame did not change, no animation is required. */
    public static void visual(ServerPlayer p,net.minecraft.core.Direction direction){
        var previous=com.moigferdsrte.gravitychanger.util.GravityDirectionUtil.getGravityDirection(p);
        if(previous==direction)return;
        visual(p,GravityTransition.plan(previous,direction,GravityTransition.headingFromYaw(previous,p.getYRot())));
    }
    public static void sendState(ServerPlayer p,ServerPlayer recipient) {
        if(!ServerPlayNetworking.canSend(recipient,State.TYPE)) return;
        var s=ClingingReoriented.data(p);
        ServerPlayNetworking.send(recipient,new State(p.getId(),s.selected.get3DDataValue(),s.owned,s.visualFrameOwned,s.supportId,s.support==null?new java.util.UUID(0,0):s.support,s.revision));
    }
    public static void publish(ServerPlayer p) {
        ClingingReoriented.data(p).revision++;
        sendState(p,p); for(ServerPlayer viewer:PlayerLookup.tracking(p)) sendState(p,viewer);
    }
}
