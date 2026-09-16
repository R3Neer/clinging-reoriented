package io.github.r3neer.clingingreoriented;

import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
    /** Immediate legacy/forced local-player transition. */
    public record VisualTransition(int direction,float yawDelta,int kind,long sequence) implements CustomPacketPayload {
        public static final Type<VisualTransition> TYPE=Payloads.type("visual_transition_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,VisualTransition> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarInt(v.direction);b.writeFloat(v.yawDelta);b.writeVarInt(v.kind);b.writeVarLong(v.sequence);},
            b->new VisualTransition(b.readVarInt(),b.readFloat(),b.readVarInt(),b.readVarLong()));
        @Override public Type<VisualTransition> type(){return TYPE;}
    }
    /** Physical gravity/yaw changed, but keep the rendered world frame stable. */
    public record VisualHold(int direction,float yawDelta,long sequence) implements CustomPacketPayload {
        public static final Type<VisualHold> TYPE=Payloads.type("visual_hold_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf,VisualHold> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarInt(v.direction);b.writeFloat(v.yawDelta);b.writeVarLong(v.sequence);},
            b->new VisualHold(b.readVarInt(),b.readFloat(),b.readVarLong()));
        @Override public Type<VisualHold> type(){return TYPE;}
    }
    /** Commit the retained local-player frame toward the imminent gravity-relative floor. */
    public record LandingVisual(int direction,int kind,float etaTicks,long sequence) implements CustomPacketPayload {
        public static final Type<LandingVisual> TYPE=Payloads.type("visual_land_v2");
        public static final StreamCodec<RegistryFriendlyByteBuf,LandingVisual> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarInt(v.direction);b.writeVarInt(v.kind);b.writeFloat(v.etaTicks);b.writeVarLong(v.sequence);},
            b->new LandingVisual(b.readVarInt(),b.readVarInt(),b.readFloat(),b.readVarLong()));
        @Override public Type<LandingVisual> type(){return TYPE;}
    }
    /** Cancel a landing trajectory. holdCurrent=true freezes the exact currently displayed frame. */
    public record VisualCancel(boolean holdCurrent,long sequence) implements CustomPacketPayload {
        public static final Type<VisualCancel> TYPE=Payloads.type("visual_cancel_v1");
        public static final StreamCodec<RegistryFriendlyByteBuf,VisualCancel> CODEC=StreamCodec.of(
            (b,v)->{b.writeBoolean(v.holdCurrent);b.writeVarLong(v.sequence);},b->new VisualCancel(b.readBoolean(),b.readVarLong()));
        @Override public Type<VisualCancel> type(){return TYPE;}
    }
    /** Clinging-owned snap presentation for a tracked non-player entity. */
    public record EntityVisualTransition(int entity,java.util.UUID entityUuid,int direction,float yawDelta,int kind,long sequence) implements CustomPacketPayload {
        public static final Type<EntityVisualTransition> TYPE=Payloads.type("entity_visual_transition");
        public static final StreamCodec<RegistryFriendlyByteBuf,EntityVisualTransition> CODEC=StreamCodec.of(
            (b,v)->{b.writeVarInt(v.entity);b.writeUUID(v.entityUuid);b.writeVarInt(v.direction);b.writeFloat(v.yawDelta);b.writeVarInt(v.kind);b.writeVarLong(v.sequence);},
            b->new EntityVisualTransition(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readFloat(),b.readVarInt(),b.readVarLong()));
        @Override public Type<EntityVisualTransition> type(){return TYPE;}
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
        PayloadTypeRegistry.clientboundPlay().register(VisualHold.TYPE,VisualHold.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(LandingVisual.TYPE,LandingVisual.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VisualCancel.TYPE,VisualCancel.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(EntityVisualTransition.TYPE,EntityVisualTransition.CODEC);
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
    private static long nextVisualSequence(ServerPlayer p){return ++ClingingReoriented.data(p).visualSequence;}
    public static void hold(ServerPlayer p,GravityTransition.Plan plan){
        var s=ClingingReoriented.data(p);
        boolean inFluid=FluidContext.intersects(p);
        if(inFluid&&!s.freeFlightVisualHeld){
            s.visualBaseDirection=com.moigferdsrte.gravitychanger.util.GravityDirectionUtil.getGravityDirection(p);
            s.visualBaseKnown=true;
        }
        s.freeFlightVisualHeld=true;
        s.freeFlightVisualHeldInFluid=inFluid;
        long sequence=nextVisualSequence(p);
        if(ServerPlayNetworking.canSend(p,VisualHold.TYPE))ServerPlayNetworking.send(p,new VisualHold(plan.target().get3DDataValue(),plan.yawDelta(),sequence));
    }
    public static void land(ServerPlayer p,net.minecraft.core.Direction direction,GravityTransition.TurnKind kind,double etaTicks){
        long sequence=nextVisualSequence(p);
        float eta=(float)(Double.isFinite(etaTicks)?Math.max(0.0D,Math.min(LandingTiming.PRESENTATION_TICKS,etaTicks)):LandingTiming.PRESENTATION_TICKS);
        if(ServerPlayNetworking.canSend(p,LandingVisual.TYPE))ServerPlayNetworking.send(p,new LandingVisual(direction.get3DDataValue(),kind.ordinal(),eta,sequence));
    }
    public static void cancelLanding(ServerPlayer p,boolean holdCurrent){
        var s=ClingingReoriented.data(p);
        if(!holdCurrent){s.freeFlightVisualHeld=false;s.freeFlightVisualHeldInFluid=false;}
        long sequence=nextVisualSequence(p);
        if(ServerPlayNetworking.canSend(p,VisualCancel.TYPE))ServerPlayNetworking.send(p,new VisualCancel(holdCurrent,sequence));
    }
    public static void visual(ServerPlayer p,GravityTransition.Plan plan){
        var s=ClingingReoriented.data(p);s.freeFlightVisualHeld=false;s.freeFlightVisualHeldInFluid=false;long sequence=nextVisualSequence(p);
        if(ServerPlayNetworking.canSend(p,VisualTransition.TYPE))ServerPlayNetworking.send(p,new VisualTransition(
            plan.target().get3DDataValue(),plan.yawDelta(),plan.kind().ordinal(),sequence));
    }
    /** Mount-transfer compatibility: if the effective rider frame did not change, no animation is required. */
    public static void visual(ServerPlayer p,net.minecraft.core.Direction direction){
        var previous=com.moigferdsrte.gravitychanger.util.GravityDirectionUtil.getGravityDirection(p);
        if(previous==direction)return;
        visual(p,GravityTransition.plan(previous,direction,GravityTransition.headingFromYaw(previous,p.getYRot())));
    }
    public static void visual(LivingEntity entity,GravityTransition.Plan plan){
        var s=MobGravity.state(entity);long sequence=++s.visualSequence;
        var packet=new EntityVisualTransition(entity.getId(),entity.getUUID(),plan.target().get3DDataValue(),plan.yawDelta(),plan.kind().ordinal(),sequence);
        Set<ServerPlayer> recipients=new LinkedHashSet<>(PlayerLookup.tracking(entity));
        collectPlayerPassengers(entity,recipients);
        for(ServerPlayer recipient:recipients)if(ServerPlayNetworking.canSend(recipient,EntityVisualTransition.TYPE))ServerPlayNetworking.send(recipient,packet);
    }
    private static void collectPlayerPassengers(Entity entity,Set<ServerPlayer> recipients){
        for(Entity passenger:entity.getPassengers()){
            if(passenger instanceof ServerPlayer player)recipients.add(player);
            collectPlayerPassengers(passenger,recipients);
        }
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
