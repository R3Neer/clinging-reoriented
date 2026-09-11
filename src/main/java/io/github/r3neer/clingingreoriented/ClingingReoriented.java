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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import java.util.List;

public final class ClingingReoriented implements ModInitializer {
    public static final String ID = "clinging_reoriented";
    private static final double RETIREMENT_RADIUS = 4.0;
    private static final ThreadLocal<Boolean> WRITING = ThreadLocal.withInitial(() -> false);
    public static PlayerData data(Player p) { return ((PlayerData.Holder)p).clinging$data(); }
    public static boolean hasEffect(net.minecraft.world.entity.LivingEntity p) { return p.hasEffect(Reorientation.EFFECT) || BuiltInRegistries.MOB_EFFECT.get(Identifier.fromNamespaceAndPath("alexsmobs", "clinging")).map(p::hasEffect).orElse(false); }
    public static boolean anchor(Player p) { return p.getMainHandItem().getItem() instanceof GravityAnchorItem || p.getOffhandItem().getItem() instanceof GravityAnchorItem; }
    public static boolean managed(Entity e) { return e instanceof Player p && data(p).support != null && controlsPhysics(p); }
    public static boolean controlsPhysics(Entity e) {
        if (!(e instanceof Player p)) return false;
        var s=data(p); var attr=p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        return s.owned && hasEffect(p) && p.isAlive() && !p.isPassenger() && !s.anchorBorrowed
            && !anchor(p) && attr!=null && attr.getModifiers().isEmpty()
            && GravityDirectionUtil.getGravityDirection(p)==s.selected;
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
            } else if(data(oldPlayer).owned || data(oldPlayer).ownedAtDeath || data(oldPlayer).anchorBorrowed) {
                // Vanilla restoreFrom copies base attributes even on death.
                write(newPlayer,Direction.DOWN);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer,newPlayer,alive)->Payloads.publish(newPlayer));
        EntityTrackingEvents.START_TRACKING.register((entity, observer) -> { if (entity instanceof ServerPlayer p) Payloads.sendState(p, observer); });
    }
    public enum Result { SUCCESS, NO_SURFACE, AMBIGUOUS, NO_SPACE, BLOCKED, FOREIGN_GRAVITY, UNCHANGED, AIR_CHANGE_USED, MOUNT_ACTION }
    public static Result attempt(ServerPlayer p) {
        return attempt(p, p.getLookAngle());
    }
    public static Result attempt(ServerPlayer p, Vec3 worldLook) {
        reconcile(p);
        var s = data(p);
        if (!hasEffect(p) || !p.isAlive() || p.isSpectator() || p.isSleeping() || p.isFallFlying() || p.getAbilities().flying || anchor(p) || s.retirementPending) return Result.BLOCKED;
        if(p.isPassenger())return MountedGravity.attempt(p,worldLook);
        if(!GravityInput.available(p))return Result.BLOCKED;
        var attr = p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if (attr == null || !attr.getModifiers().isEmpty() || (!s.owned && GravityDirectionUtil.getOwnGravityDirection(p) != Direction.DOWN)) return Result.FOREIGN_GRAVITY;
        Direction direction=LookDirection.select(worldLook);
        if (direction==null) return Result.AMBIGUOUS;
        if (direction==GravityDirectionUtil.getGravityDirection(p)) return Result.UNCHANGED;
        if (s.airChangeUsed && !p.hasEffect(Reorientation.EFFECT)) return Result.AIR_CHANGE_USED;
        AABB box = RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()), direction, p.position());
        if (!fits(p, box, null)) return Result.NO_SPACE;
        boolean airborne=!AirChanges.grounded(p);
        write(p, direction);
        if(airborne)s.airChangeUsed=true;
        s.owned = true; s.selected = direction; s.unbind();
        ScaleBridge.clear(p);
        // Ground flags describe the previous frame. Preserve world momentum and fall
        // distance, but do not allow a phantom jump from the old floor after turning.
        p.setOnGround(false); p.verticalCollision=false; p.verticalCollisionBelow=false; p.horizontalCollision=false;
        Payloads.publish(p);
        GravityBreadcrumbs.record(p,direction);
        return Result.SUCCESS;
    }
    public static boolean fits(Player p, AABB box, Entity selected) {
        if (!Double.isFinite(box.minX+box.minY+box.minZ+box.maxX+box.maxY+box.maxZ) || box.getXsize() <= 0 || box.getYsize() <= 0 || box.getZsize() <= 0) return false;
        if (box.minY < p.level().getMinY() || box.maxY > p.level().getMaxY()+1 || !p.level().getWorldBorder().isWithinBounds(box)) return false;
        for (int x=((int)Math.floor(box.minX))>>4; x<=((int)Math.floor(box.maxX))>>4; x++) for(int z=((int)Math.floor(box.minZ))>>4; z<=((int)Math.floor(box.maxZ))>>4; z++) {
            if (!p.level().hasChunkAt(new BlockPos(x<<4, (int)box.minY, z<<4))) return false;
        }
        AABB interior = box.deflate(1e-7);
        if(AnatomyBridge.active(p)) {
            if(!AnatomyBridge.spaceClear(p,interior))return false;
        }else {
            if (selected != null && interior.intersects(selected.getBoundingBox())) return false;
            if(ScaleBridge.PRESENT)for(var entity:p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,interior,e->ScaleBridge.eligible(p,e)))if(interior.intersects(entity.getBoundingBox()))return false;
        }
        return p.level().noCollision(p, interior);
    }
    public static void write(Player p, Direction direction) {
        boolean old = WRITING.get(); WRITING.set(true);
        try {
            GravityDirectionUtil.setGravityDirection(p, direction);
            p.setBoundingBox(RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()), direction, p.position()));
            if (p instanceof ServerPlayer sp) sp.connection.send(new ClientboundUpdateAttributesPacket(p.getId(), List.of(p.getAttribute(ModAttributes.GRAVITY_DIRECTION))));
        } finally { WRITING.set(old); }
    }
    public static void externalWrite(Player p, Direction direction) {
        if (WRITING.get() || p.level().isClientSide()) return;
        var s = data(p);
        if (s.anchorBorrowed && anchor(p)) return;
        if (s.owned) { s.owned=false; s.retirementPending=false; s.unbind(); if (p instanceof ServerPlayer sp) Payloads.publish(sp); }
    }
    public static void reconcile(ServerPlayer p) {
        var s=data(p);
        MountedGravity.refresh(p);
        AirChanges.refresh(p);
        if (!p.isAlive()) { s.ownedAtDeath |= s.owned || s.anchorBorrowed; s.owned=false; s.unbind(); return; }
        if (s.anchorBorrowed) {
            if (!hasEffect(p)) s.anchorExpired=true;
            return;
        }
        if (!s.owned) return;
        var attr=p.getAttribute(ModAttributes.GRAVITY_DIRECTION);
        if (attr == null || !attr.getModifiers().isEmpty() || GravityDirectionUtil.getOwnGravityDirection(p) != s.selected) { s.owned=false; s.unbind(); Payloads.publish(p); return; }
        if (!hasEffect(p)) {
            s.unbind();
            if(s.retirementPending && p.level().getGameTime()<s.nextRetirementAttempt)return;
            if (!retire(p)) {
                if(!s.retirementPending) org.slf4j.LoggerFactory.getLogger(ID).warn("Cannot retire Clinging for {} yet: no collision-free DOWN placement within {} blocks",p.getUUID(),RETIREMENT_RADIUS);
                s.retirementPending=true;s.nextRetirementAttempt=p.level().getGameTime()+20;return;
            }
            s.owned=false; s.retirementPending=false; Payloads.publish(p);
        }
    }
    /** Forced cleanup is local and bounded; voluntary selection never calls this search. */
    public static boolean retire(ServerPlayer p) {
        Vec3 origin=p.position();
        var dimensions=p.getDimensions(p.getPose());
        if (fits(p, RotationUtil.makeBoxFromDimensions(dimensions, Direction.DOWN, origin), null)) { write(p,Direction.DOWN); return true; }
        double maxDistanceSqr=RETIREMENT_RADIUS*RETIREMENT_RADIUS+1.0E-9;
        for (int radius=1; radius<=8; radius++) for (int y=-radius; y<=radius; y++) for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if (Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z))) != radius) continue;
            Vec3 target=origin.add(x*.5,y*.5,z*.5);
            if(target.distanceToSqr(origin)>maxDistanceSqr)continue;
            if (fits(p,RotationUtil.makeBoxFromDimensions(dimensions,Direction.DOWN,target),null)) { write(p,Direction.DOWN); p.teleportTo(target.x,target.y,target.z); return true; }
        }
        return false;
    }
}
