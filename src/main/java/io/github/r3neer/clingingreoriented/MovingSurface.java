package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import java.util.*;

public final class MovingSurface {
    private static final long REFERENCE_MAX_AGE_TICKS=20;
    private static final class CycleScratch {
        final Set<UUID> stack=new HashSet<>();
        final Set<UUID> done=new HashSet<>();
        void reset(){stack.clear();done.clear();}
    }
    private static final ThreadLocal<CycleScratch> CYCLE_SCRATCH=ThreadLocal.withInitial(CycleScratch::new);

    public static LivingEntity resolve(Player p) {
        var s=ClingingReoriented.data(p);
        Entity e=p.level().getEntity(s.supportId);
        return e instanceof LivingEntity living && living.getUUID().equals(s.support) && ScaleBridge.eligible(p,living) ? living : null;
    }
    public static boolean canBind(Player p, LivingEntity entity) {
        if (!ScaleBridge.eligible(p,entity)) return false;
        CycleScratch scratch=CYCLE_SCRATCH.get();scratch.reset();
        return acyclic(p,entity,scratch.stack,scratch.done);
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
    private static void sample(Player p,Vec3 position){
        var s=ClingingReoriented.data(p);var latest=s.supportHistory.peekLast();
        if(latest!=null&&latest.position().equals(position))return;
        s.supportHistory.add(new PlayerData.SupportSample(++s.supportSampleSequence,p.level().getGameTime(),position));
        while(s.supportHistory.size()>20)s.supportHistory.removeFirst();
    }
    public static void bind(Player p, LivingEntity entity) {
        var s=ClingingReoriented.data(p);
        s.support=entity.getUUID(); s.supportId=entity.getId(); s.supportPosition=entity.position(); s.supportBox=entity.getBoundingBox();
        s.supportHistory.clear();s.supportSampleSequence=0;s.lastConsumedSupportSample=-1;sample(p,entity.position());
        ScaleBridge.clear(p);
    }
    public static void clear(Player p) {
        var s=ClingingReoriented.data(p);
        if(s.support==null) return;
        s.unbind(); if(p instanceof ServerPlayer sp) Payloads.publish(sp);
    }
    public static void teleported(Entity entity) {
        if(entity instanceof Player p){
            boolean wasBound=ClingingReoriented.data(p).support!=null;
            clear(p);
            if(!wasBound && p instanceof ServerPlayer sp)Payloads.publish(sp);
        }
        for(Player p:entity.level().players())if(entity.getUUID().equals(ClingingReoriented.data(p).support))clear(p);
    }
    public static void carry(Player p) {
        if(AnatomyBridge.active(p))return;
        var s=ClingingReoriented.data(p);
        if(s.support==null || s.carrying || (p.level().isClientSide() && !p.isLocalInstanceAuthoritative())) return;
        LivingEntity support=resolve(p);
        if(support==null || !ClingingReoriented.controlsPhysics(p) || !canBind(p,support)) { clear(p); return; }
        if(s.supportPosition==null || s.supportBox==null) { s.supportPosition=support.position(); s.supportBox=support.getBoundingBox();sample(p,support.position()); return; }
        Vec3 delta=support.position().subtract(s.supportPosition);
        sample(p,support.position());
        boolean touching=FaceGeometry.touching(p.getBoundingBox(),s.supportBox,s.selected);
        s.supportPosition=support.position(); s.supportBox=support.getBoundingBox();
        if(delta.lengthSqr()>16 || !Double.isFinite(delta.lengthSqr())) { clear(p); return; }
        double away=p.getDeltaMovement().dot(FaceGeometry.vector(s.selected.getOpposite()));
        s.groundedOnSurface=touching && away <= 1e-5;
        if(!s.groundedOnSurface) { s.lastTransport=Vec3.ZERO; return; }
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
            p.setOnGround(true);p.verticalCollisionBelow=true;
        } finally { s.carrying=false; }
    }
    public static void afterMove(Player p) {
        if(AnatomyBridge.active(p)) {
            var state=ClingingReoriented.data(p);state.groundedOnSurface=AnatomyBridge.supported(p);
            var support=AnatomyBridge.parent(p);
            if(state.groundedOnSurface && support!=null && !support.getUUID().equals(state.support)) {
                state.support=support.getUUID();state.supportId=support.getId();state.supportPosition=support.position();
                state.supportHistory.clear();state.supportSampleSequence=0;state.lastConsumedSupportSample=-1;if(p instanceof ServerPlayer sp)Payloads.publish(sp);
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
    static Vec3 consumeLatestCorrection(PlayerData s,Vec3 observed,long now){
        if(observed==null||!Double.isFinite(observed.lengthSqr()))return null;
        var latest=s.supportHistory.peekLast();
        if(latest==null)return null;
        long age=now-latest.tick();
        if(age<0||age>REFERENCE_MAX_AGE_TICKS||latest.sequence()<=s.lastConsumedSupportSample)return null;
        Vec3 correction=latest.position().subtract(observed);
        if(!Double.isFinite(correction.lengthSqr())||correction.lengthSqr()>16||correction.lengthSqr()<1e-12)return null;
        var descending=s.supportHistory.descendingIterator();descending.next();
        var previous=descending.hasNext()?descending.next():null;
        if(previous==null||previous.sequence()+1!=latest.sequence())return null;
        Vec3 segment=latest.position().subtract(previous.position());double len=segment.lengthSqr();
        if(len<1e-12)return null;
        double t=Math.clamp(observed.subtract(previous.position()).dot(segment)/len,0,1);
        if(previous.position().add(segment.scale(t)).distanceToSqr(observed)>=1e-6)return null;
        s.lastConsumedSupportSample=latest.sequence();
        return correction;
    }
    public static Vec3 resolveMovement(ServerPlayer p,Vec3 absolute) {
        if(AnatomyBridge.active(p)){ClingingReoriented.data(p).pendingMove=null;return absolute;}
        var s=ClingingReoriented.data(p); var reference=s.pendingMove;s.pendingMove=null;
        var surface=resolve(p);
        if(reference==null || surface==null || !s.groundedOnSurface || reference.support()!=s.supportId || reference.revision()!=s.revision || !reference.absolute().equals(absolute)) return absolute;
        Vec3 correction=consumeLatestCorrection(s,reference.origin(),p.level().getGameTime());
        if(correction==null)return absolute;
        Vec3 target=absolute.add(correction);
        if(target.distanceToSqr(p.position())>16)return absolute;
        var box=com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),s.selected,target);
        return ClingingReoriented.fits(p,box,surface)?target:absolute;
    }
}
