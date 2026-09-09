package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import java.util.*;

public final class MovingSurface {
    public static LivingEntity resolve(Player p) {
        var s=ClingingReoriented.data(p);
        Entity e=p.level().getEntity(s.supportId);
        return e instanceof LivingEntity living && living.getUUID().equals(s.support) && ScaleBridge.eligible(p,living) ? living : null;
    }
    public static boolean canBind(Player p, LivingEntity entity) {
        if (!ScaleBridge.eligible(p,entity)) return false;
        return acyclic(p,entity,new HashSet<>(),new HashSet<>());
    }
    private static boolean acyclic(Player p,Entity node,Set<UUID> stack,Set<UUID> done) {
        if(node==null)return true;
        if(node==p || stack.contains(node.getUUID()))return false;
        if(done.contains(node.getUUID()))return true;
        stack.add(node.getUUID());
        if(!acyclic(p,ScaleBridge.parent(node),stack,done))return false;
        if(node instanceof Player other && !acyclic(p,resolve(other),stack,done))return false;
        stack.remove(node.getUUID());done.add(node.getUUID());return true;
    }
    public static void bind(Player p, LivingEntity entity) {
        var s=ClingingReoriented.data(p);
        s.support=entity.getUUID(); s.supportId=entity.getId(); s.supportPosition=entity.position(); s.supportBox=entity.getBoundingBox();
        s.supportHistory.clear();s.supportHistory.add(entity.position());
        ScaleBridge.clear(p);
    }
    public static void clear(Player p) {
        var s=ClingingReoriented.data(p);
        if(s.support==null) return;
        s.unbind(); if(p instanceof ServerPlayer sp) Payloads.publish(sp);
    }
    public static void teleported(Entity entity) {
        if(entity instanceof ServerPlayer)GravityBreadcrumbs.clear(entity.getUUID());
        if(entity instanceof net.minecraft.world.entity.TamableAnimal pet)GravityBreadcrumbs.forget(pet);
        if(entity instanceof Player p){
            boolean wasBound=ClingingReoriented.data(p).support!=null;
            clear(p);
            if(!wasBound && p instanceof ServerPlayer sp)Payloads.publish(sp);
        }
        for(Player p:entity.level().players())if(entity.getUUID().equals(ClingingReoriented.data(p).support))clear(p);
    }
    public static void carry(Player p) {
        if(AnatomyBridge.active(p))return; // The shared core owns carry, even without Clinging selected.
        var s=ClingingReoriented.data(p);
        if(s.support==null || s.carrying || (p.level().isClientSide() && !p.isLocalInstanceAuthoritative())) return;
        LivingEntity support=resolve(p);
        if(support==null || !ClingingReoriented.controlsPhysics(p) || !canBind(p,support)) { clear(p); return; }
        if(s.supportPosition==null || s.supportBox==null) { s.supportPosition=support.position(); s.supportBox=support.getBoundingBox(); return; }
        Vec3 delta=support.position().subtract(s.supportPosition);
        if(!support.position().equals(s.supportHistory.peekLast())) {s.supportHistory.add(support.position());while(s.supportHistory.size()>20)s.supportHistory.removeFirst();}
        boolean touching=FaceGeometry.touching(p.getBoundingBox(),s.supportBox,s.selected);
        s.supportPosition=support.position(); s.supportBox=support.getBoundingBox();
        if(delta.lengthSqr()>16 || !Double.isFinite(delta.lengthSqr())) { clear(p); return; }
        double away=p.getDeltaMovement().dot(FaceGeometry.vector(s.selected.getOpposite()));
        s.groundedOnSurface=touching && away <= 1e-5;
        if(!s.groundedOnSurface) { s.lastTransport=Vec3.ZERO; return; }
        // Keep the departure velocity for repeated calls within this tick, but never
        // reuse a previous tick's displacement after a platform has stopped.
        if(delta.lengthSqr()<1e-12) {
            if(s.transportTick!=p.level().getGameTime())s.lastTransport=Vec3.ZERO;
            return;
        }
        s.carrying=true;
        try {
            Vec3 before=p.position();
            p.move(MoverType.SELF,delta);
            Vec3 actual=p.position().subtract(before);
            s.lastTransport=actual;
            s.transportTick=p.level().getGameTime();
            if(p instanceof ServerPlayer sp) ScaleBridge.baseline(sp,actual);
            if(actual.distanceToSqr(delta)>1e-8) { clear(p); return; }
            // The collider is excluded during carry to avoid self-blocking. Restore the
            // real support flags so an entity moving every tick doesn't prevent jumping.
            p.setOnGround(true);p.verticalCollisionBelow=true;
        } finally { s.carrying=false; }
    }
    public static void afterMove(Player p) {
        if(AnatomyBridge.active(p)) {
            var state=ClingingReoriented.data(p);state.groundedOnSurface=AnatomyBridge.supported(p);
            var support=AnatomyBridge.parent(p);
            if(state.groundedOnSurface && support!=null && !support.getUUID().equals(state.support)) {
                state.support=support.getUUID();state.supportId=support.getId();state.supportPosition=support.position();
                state.supportHistory.clear();if(p instanceof ServerPlayer sp)Payloads.publish(sp);
            }
            return;
        }
        var s=ClingingReoriented.data(p);
        if(s.carrying || !ClingingReoriented.controlsPhysics(p)) return;
        LivingEntity entity=resolve(p);
        boolean contact=entity!=null && FaceGeometry.touching(p.getBoundingBox(),entity.getBoundingBox(),s.selected);
        if(!contact && p.getDeltaMovement().dot(FaceGeometry.vector(s.selected.getOpposite()))<=1e-5) {
            var contacts=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(1e-4),e->canBind(p,e)
                && FaceGeometry.touching(p.getBoundingBox(),e.getBoundingBox(),s.selected));
            contacts.sort(Comparator.comparing(Entity::getUUID));
            if(!contacts.isEmpty()) {
                entity=contacts.getFirst(); bind(p,entity); contact=true;
                if(p instanceof ServerPlayer sp)Payloads.publish(sp);
            }
        }
        s.groundedOnSurface=contact && p.getDeltaMovement().dot(FaceGeometry.vector(s.selected.getOpposite()))<=1e-5;
    }
    public static Vec3 resolveMovement(ServerPlayer p,Vec3 absolute) {
        if(AnatomyBridge.active(p)){ClingingReoriented.data(p).pendingMove=null;return absolute;}
        var s=ClingingReoriented.data(p); var reference=s.pendingMove;s.pendingMove=null;
        var surface=resolve(p);
        if(reference==null || surface==null || !s.groundedOnSurface || reference.support()!=s.supportId || reference.revision()!=s.revision || !reference.absolute().equals(absolute)) return absolute;
        Vec3 observed=reference.origin();
        if(!Double.isFinite(observed.lengthSqr()))return absolute;
        boolean known=false;Vec3 previous=null;
        for(Vec3 frame:s.supportHistory) {
            if(frame.distanceToSqr(observed)<1e-6)known=true;
            if(previous!=null){
                Vec3 segment=frame.subtract(previous);double len=segment.lengthSqr();
                double t=len>1e-12?Math.clamp(observed.subtract(previous).dot(segment)/len,0,1):0;
                if(previous.add(segment.scale(t)).distanceToSqr(observed)<1e-6)known=true;
            }
            previous=frame;
        }
        Vec3 correction=surface.position().subtract(observed);
        if(!known || correction.lengthSqr()>16)return absolute;
        Vec3 target=absolute.add(correction);
        if(target.distanceToSqr(p.position())>16)return absolute;
        var box=com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),s.selected,target);
        return ClingingReoriented.fits(p,box,surface)?target:absolute;
    }
}
