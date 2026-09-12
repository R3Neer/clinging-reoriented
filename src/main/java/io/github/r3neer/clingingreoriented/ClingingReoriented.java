package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import com.moigferdsrte.gravitychanger.init.ModAttributes;
import com.moigferdsrte.gravitychanger.item.GravityAnchorItem;
import io.github.r3neer.clingingreoriented.geometry.LookDirection;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import java.util.List;
import java.util.Set;

public final class ClingingReoriented implements ModInitializer {
    public static final String ID = "clinging_reoriented";
    private static final double RETIREMENT_RADIUS = 4.0;
    private static final ThreadLocal<Boolean> WRITING = ThreadLocal.withInitial(() -> false);
    private record TurnPlacement(Vec3 position,AABB box) {}
    public static PlayerData data(Player p) { return ((PlayerData.Holder)p).clinging$data(); }
    public static boolean hasEffect(LivingEntity p) { return p.hasEffect(Reorientation.EFFECT) || BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath("alexsmobs", "clinging")).map(p::hasEffect).orElse(false); }
    public static boolean anchor(Player p) { return p.getMainHandItem().getItem() instanceof GravityAnchorItem || p.getOffhandItem().getItem() instanceof GravityAnchorItem; }
    public static boolean managed(Entity e) { return e instanceof Player p && data(p).support != null && controlsPhysics(p); }
    public static boolean controlsPhysics(Entity e) {
        if (!(e instanceof Player p)) return false;
        var s=data(p); var attr=p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        return s.owned && hasEffect(p) && p.isAlive() && !p.isPassenger() && !s.anchorBorrowed
            && !anchor(p) && attr!=null && attr.getModifiers().isEmpty()
            && GravityDirectionUtil.getGravityDirection(p)==s.selected;
    }
    private static boolean mountedVisualOwned(ServerPlayer p){
        if(!(p.getRootVehicle() instanceof LivingEntity root)||root==p)return false;
        var state=MobGravity.state(root);
        return state.ownership==MobGravity.Ownership.BORROWED_RIDER
            && GravityDirectionUtil.getOwnGravityDirection(root)==state.ownedDirection;
    }
    private static void reconcileVisualOwnership(ServerPlayer p){
        var s=data(p);boolean next=s.owned||mountedVisualOwned(p);
        if(s.visualFrameOwned!=next){s.visualFrameOwned=next;Payloads.publish(p);}
    }
    @Override public void onInitialize() {
        AnatomyBridge.initialize();
        Reorientation.initialize();
        Payloads.register();
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server->BeaconPowers.install());
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->GravityBreadcrumbs.clearAll());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            GravityBreadcrumbs.prune(server);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                reconcile(p);
                MovingSurface.carry(p);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { reconcile(handler.player); Payloads.publish(handler.player); });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {data(handler.player).unbind();GravityBreadcrumbs.clear(handler.player.getUUID());});
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            data(newPlayer).revision=data(oldPlayer).revision+1;
            if (alive) {
                var old = data(oldPlayer); var next = data(newPlayer);
                next.owned = old.owned; next.selected = old.selected;
                next.airChangeUsed = old.airChangeUsed;
                next.anchorBorrowed = old.anchorBorrowed; next.anchorExpired = old.anchorExpired;
                next.visualFrameOwned=old.visualFrameOwned;
            } else if(data(oldPlayer).owned || data(oldPlayer).ownedAtDeath || data(oldPlayer).anchorBorrowed) {
                // Vanilla restoreFrom copies base attributes even on death.
                write(newPlayer,Direction.DOWN);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->Payloads.publish(newPlayer));
        EntityTrackingEvents.START_TRACKING.register((entity, observer) -> { if (entity instanceof ServerPlayer p) Payloads.sendState(p, observer); });
    }
    public enum Result { SUCCESS, NO_SURFACE, AMBIGUOUS, NO_SPACE, BLOCKED, FOREIGN_GRAVITY, UNCHANGED, AIR_CHANGE_USED, MOUNT_ACTION }
    public static Result attempt(ServerPlayer p) { return attempt(p, p.getLookAngle()); }
    public static Result attempt(ServerPlayer p, Vec3 worldLook) {
        reconcile(p);
        var s = data(p);
        if (!hasEffect(p) || !p.isAlive() || p.isSpectator() || p.isSleeping() || p.isFallFlying() || p.getAbilities().flying || anchor(p) || s.retirementPending) return Result.BLOCKED;
        if(worldLook==null || !Double.isFinite(worldLook.x+worldLook.y+worldLook.z) || worldLook.lengthSqr()<1.0E-10D)return Result.AMBIGUOUS;
        worldLook=worldLook.normalize();
        if(p.isPassenger())return MountedGravity.attempt(p,worldLook);
        if(!GravityInput.available(p))return Result.BLOCKED;
        var attr = p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if (attr == null || !attr.getModifiers().isEmpty() || (!s.owned && GravityDirectionUtil.getOwnGravityDirection(p) != Direction.DOWN)) return Result.FOREIGN_GRAVITY;
        Direction previous=GravityDirectionUtil.getGravityDirection(p);
        Direction direction=LookDirection.select(worldLook);
        if (direction==null) return Result.AMBIGUOUS;
        if (direction==previous) return Result.UNCHANGED;
        if (s.airChangeUsed && !p.hasEffect(Reorientation.EFFECT)) return Result.AIR_CHANGE_USED;
        TurnPlacement placement=findTurnPlacement(p,direction);
        if (placement==null) return Result.NO_SPACE;
        GravityTransition.Plan transition=GravityTransition.plan(previous,direction,p.getYRot(),p.getXRot());
        boolean airborne=!AirChanges.grounded(p);
        Payloads.visual(p,transition);
        writeTransition(p,direction,placement.position(),transition);
        if(airborne)s.airChangeUsed=true;
        s.owned = true; s.visualFrameOwned=true; s.selected = direction; s.unbind();
        ScaleBridge.clear(p);
        p.setOnGround(false); p.verticalCollision=false;p.verticalCollisionBelow=false;p.horizontalCollision=false;
        Payloads.publish(p);
        GravityBreadcrumbs.record(p,direction);
        return Result.SUCCESS;
    }
    private static TurnPlacement findTurnPlacement(Player p,Direction direction){
        var dimensions=p.getDimensions(p.getPose());
        Vec3 current=p.position();
        AABB direct=RotationUtil.makeBoxFromDimensions(dimensions,direction,current);
        if(fits(p,direct,null))return new TurnPlacement(current,direct);
        Vec3 centered=RotationUtil.getCenterAlignedPosition(p.getBoundingBox(),dimensions,direction);
        if(centered.distanceToSqr(current)<=1.0E-12)return null;
        AABB centeredBox=RotationUtil.makeBoxFromDimensions(dimensions,direction,centered);
        return fits(p,centeredBox,null)?new TurnPlacement(centered,centeredBox):null;
    }
    public static boolean fits(Player p, AABB box, Entity selected) {
        if (!Double.isFinite(box.minX+box.minY+box.minZ+box.maxX+box.maxY+box.maxZ) || box.getXsize() <= 0 || box.getYsize() <= 0 || box.getZsize() <= 0) return false;
        if (box.minY < p.level().getMinY() || box.maxY > p.level().getMaxY()+1 || !p.level().getWorldBorder().isWithinBounds(box)) return false;
        for (int x=((int)Math.floor(box.minX))>>4; x<=((int)Math.floor(box.maxX))>>4; x++) for(int z=((int)Math.floor(box.minZ))>>4; z<=((int)Math.floor(box.maxZ))>>4; z++) if (!p.level().hasChunkAt(new BlockPos(x<<4, (int)box.minY, z<<4))) return false;
        AABB interior = box.deflate(1e-7);
        if(AnatomyBridge.active(p)) {
            if(!AnatomyBridge.spaceClear(p,interior))return false;
        }else {
            if (selected != null && interior.intersects(selected.getBoundingBox())) return false;
            if(ScaleBridge.PRESENT)for(var entity:p.level().getEntitiesOfClass(LivingEntity.class,interior,e->ScaleBridge.eligible(p,e)))if(interior.intersects(entity.getBoundingBox()))return false;
        }
        return p.level().noCollision(p, interior);
    }
    public static void write(Player p, Direction direction) { write(p,direction,p.position()); }
    public static void write(Player p, Direction direction,Vec3 position) {
        boolean old = WRITING.get(); WRITING.set(true);
        try {
            GravityDirectionUtil.setGravityDirection(p, direction);
            var attribute=p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
            if (p instanceof ServerPlayer sp && attribute!=null) {
                sp.connection.send(new ClientboundUpdateAttributesPacket(p.getId(), List.of(attribute)));
                if(position.distanceToSqr(p.position())>1.0E-12)
                    sp.connection.teleport(new PositionMoveRotation(position,p.getDeltaMovement(),p.getYRot(),p.getXRot()),Set.of());
            } else if(position.distanceToSqr(p.position())>1.0E-12) p.setPos(position);
            p.setBoundingBox(RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()), direction, position));
        } finally { WRITING.set(old); }
    }
    static void applyYaw(ServerPlayer p,GravityTransition.Plan transition){
        float next=Mth.wrapDegrees(p.getYRot()+transition.yawDelta());
        p.setYRot(next);p.yRotO=next;
    }
    private static void writeTransition(ServerPlayer p,Direction direction,Vec3 position,GravityTransition.Plan transition){
        boolean old=WRITING.get();WRITING.set(true);
        try{
            GravityDirectionUtil.setGravityDirection(p,direction);
            applyYaw(p,transition);
            var attribute=p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
            if(attribute!=null){
                p.connection.send(new ClientboundUpdateAttributesPacket(p.getId(),List.of(attribute)));
                if(position.distanceToSqr(p.position())>1.0E-12)
                    // Position must be authoritative, but rotation is already transported by
                    // visual_transition_v2. Relative zero keeps any mouse input made after
                    // the request instead of restoring a stale absolute yaw/pitch.
                    p.connection.teleport(new PositionMoveRotation(position,p.getDeltaMovement(),0.0F,0.0F),Set.of(Relative.Y_ROT,Relative.X_ROT));
            }
            p.setBoundingBox(RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),direction,position));
        }finally{WRITING.set(old);}
    }
    public static void externalWrite(Player p, Direction direction) {
        if (WRITING.get() || p.level().isClientSide()) return;
        var s = data(p);
        if (s.anchorBorrowed && anchor(p)) return;
        if (s.owned) { s.owned=false; s.visualFrameOwned=false; s.retirementPending=false; s.unbind(); if (p instanceof ServerPlayer sp) Payloads.publish(sp); }
    }
    public static void reconcile(ServerPlayer p) {
        var s=data(p);
        MountedGravity.refresh(p);
        AirChanges.refresh(p);
        reconcileVisualOwnership(p);
        if (!p.isAlive()) { s.ownedAtDeath |= s.owned || s.anchorBorrowed; s.owned=false; s.visualFrameOwned=false; s.unbind(); return; }
        if (s.anchorBorrowed) {
            if (!hasEffect(p)) s.anchorExpired=true;
            return;
        }
        if (!s.owned) return;
        var attr=p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if (attr == null || !attr.getModifiers().isEmpty() || GravityDirectionUtil.getOwnGravityDirection(p) != s.selected) { s.owned=false; s.visualFrameOwned=false; s.unbind(); Payloads.publish(p); return; }
        if (!hasEffect(p)) {
            s.unbind();
            if(s.retirementPending && p.level().getGameTime()<s.nextRetirementAttempt)return;
            if (!retire(p)) {
                if(!s.retirementPending) org.slf4j.LoggerFactory.getLogger(ID).warn("Cannot retire Clinging for {} yet: no collision-free DOWN placement within {} blocks",p.getUUID(),RETIREMENT_RADIUS);
                s.retirementPending=true;s.nextRetirementAttempt=p.level().getGameTime()+20;return;
            }
            s.owned=false; s.visualFrameOwned=false; s.retirementPending=false; Payloads.publish(p);
        }
    }
    public static boolean retire(ServerPlayer p) {
        Direction previous=GravityDirectionUtil.getGravityDirection(p);
        if(previous==Direction.DOWN)return true;
        GravityTransition.Plan transition=GravityTransition.plan(previous,Direction.DOWN,p.getYRot(),p.getXRot());
        Vec3 origin=p.position();
        var dimensions=p.getDimensions(p.getPose());
        if (fits(p, RotationUtil.makeBoxFromDimensions(dimensions, Direction.DOWN, origin), null)) {
            Payloads.visual(p,transition);writeTransition(p,Direction.DOWN,origin,transition);return true;
        }
        double maxDistanceSqr=RETIREMENT_RADIUS*RETIREMENT_RADIUS+1.0E-9;
        for (int radius=1; radius<=8; radius++) for (int y=-radius; y<=radius; y++) for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if (Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z))) != radius) continue;
            Vec3 target=origin.add(x*.5,y*.5,z*.5);
            if(target.distanceToSqr(origin)>maxDistanceSqr)continue;
            if (fits(p,RotationUtil.makeBoxFromDimensions(dimensions,Direction.DOWN,target),null)) {
                Payloads.visual(p,transition);writeTransition(p,Direction.DOWN,target,transition);return true;
            }
        }
        return false;
    }
}
