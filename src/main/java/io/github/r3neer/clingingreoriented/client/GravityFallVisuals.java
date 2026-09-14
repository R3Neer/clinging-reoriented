package io.github.r3neer.clingingreoriented.client;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.BodyOrientation;
import io.github.r3neer.clingingreoriented.BodyRenderMath;
import io.github.r3neer.clingingreoriented.FluidContext;
import io.github.r3neer.clingingreoriented.GravityFallAerodynamics;
import io.github.r3neer.clingingreoriented.GravityFallSync;
import io.github.r3neer.clingingreoriented.LandingTiming;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Client-derived Gravity Fall macro body frame. */
public final class GravityFallVisuals {
    public static final int ENTRY_BLEND_TICKS=6;
    private static final int UNRESOLVED_TTL_TICKS=80;
    private enum Mode { SUSTAIN, LAND }

    private static final class Active {
        final UUID uuid; int entityId; long sequence; Mode mode=Mode.SUSTAIN; boolean initialized; int unresolvedTicks;
        BodyOrientation.State transport; Quaternionf blendStart; float blendTicks; Direction landGravity;
        float requestedEtaTicks; Quaternionf landStart; Quaternionf landTarget; float landTicks; float landDurationTicks;
        Active(UUID uuid,int entityId,long sequence){this.uuid=uuid;this.entityId=entityId;this.sequence=sequence;}
    }

    private static final Map<UUID,Active> ACTIVE=new HashMap<>();
    private static final Map<UUID,Long> LATEST_SEQUENCE=new HashMap<>();
    private GravityFallVisuals() {}

    public static void receive(Minecraft client,GravityFallSync.Visual packet){
        if(client==null||packet==null||packet.entityUuid()==null||packet.sequence()<0)return;
        GravityFallSync.Phase[] phases=GravityFallSync.Phase.values();
        if(packet.phase()<0||packet.phase()>=phases.length)return;
        GravityFallSync.Phase phase=phases[packet.phase()];
        if(phase==GravityFallSync.Phase.LAND && (packet.direction()<0||packet.direction()>5||!Float.isFinite(packet.etaTicks())||packet.etaTicks()<0.0F))return;
        synchronized(ACTIVE){
            long latest=LATEST_SEQUENCE.getOrDefault(packet.entityUuid(),-1L);
            if(packet.sequence()<=latest)return;
            LATEST_SEQUENCE.put(packet.entityUuid(),packet.sequence());
            if(phase==GravityFallSync.Phase.RESET){ACTIVE.remove(packet.entityUuid());return;}
            Active active=ACTIVE.get(packet.entityUuid());
            if(active==null){active=new Active(packet.entityUuid(),packet.entity(),packet.sequence());ACTIVE.put(packet.entityUuid(),active);}
            active.entityId=packet.entity();active.sequence=packet.sequence();active.unresolvedTicks=0;
            Entity entity=resolve(client,active);
            switch(phase){
                case START -> restart(active,entity);
                case LAND -> beginLand(active,entity,Direction.from3DDataValue(packet.direction()),packet.etaTicks());
                case RESUME -> resume(active,entity);
                case RESET -> { }
            }
        }
    }

    private static void restart(Active active,Entity entity){
        active.mode=Mode.SUSTAIN;active.initialized=false;active.transport=null;active.blendStart=null;active.blendTicks=0.0F;
        active.landGravity=null;active.landStart=null;active.landTarget=null;active.landTicks=0.0F;active.landDurationTicks=0.0F;active.requestedEtaTicks=0.0F;
        if(entity!=null)initializeSustain(active,entity,VisualTransitions.current(entity));
    }

    private static void beginLand(Active active,Entity entity,Direction target,float etaTicks){
        Quaternionf current=null;
        if(entity!=null){ensureInitialized(active,entity);current=currentBody(active,entity,0.0F);}
        active.mode=Mode.LAND;active.landGravity=target;active.requestedEtaTicks=etaTicks;active.landTicks=0.0F;
        if(entity==null){active.initialized=false;active.landStart=null;active.landTarget=null;return;}
        if(current==null)current=VisualTransitions.current(entity);
        active.landStart=new Quaternionf(current).normalize();
        active.landTarget=RotationUtil.getEntityRotationQuaternion(target);
        active.landDurationTicks=landingDuration(etaTicks);
        active.initialized=true;
    }

    private static void resume(Active active,Entity entity){
        Quaternionf current=null;
        if(entity!=null){ensureInitialized(active,entity);current=currentBody(active,entity,0.0F);}
        active.mode=Mode.SUSTAIN;active.landGravity=null;active.landStart=null;active.landTarget=null;active.landTicks=0.0F;active.landDurationTicks=0.0F;active.requestedEtaTicks=0.0F;
        if(entity==null){active.initialized=false;active.transport=null;active.blendStart=null;active.blendTicks=0.0F;return;}
        if(current==null)current=VisualTransitions.current(entity);
        initializeSustain(active,entity,current);
    }

    private static void initializeSustain(Active active,Entity entity,Quaternionf displayed){
        active.mode=Mode.SUSTAIN;active.blendStart=new Quaternionf(displayed).normalize();
        active.transport=BodyOrientation.start(active.blendStart,entity.getDeltaMovement());active.blendTicks=0.0F;active.initialized=true;
    }

    private static void ensureInitialized(Active active,Entity entity){
        if(active.initialized)return;
        if(active.mode==Mode.LAND && active.landGravity!=null){
            Quaternionf current=VisualTransitions.current(entity);
            active.landStart=new Quaternionf(current).normalize();active.landTarget=RotationUtil.getEntityRotationQuaternion(active.landGravity);
            active.landDurationTicks=landingDuration(active.requestedEtaTicks);active.landTicks=0.0F;active.initialized=true;
        }else initializeSustain(active,entity,VisualTransitions.current(entity));
    }

    public static void tick(Minecraft client){
        if(client==null||client.level==null)return;
        synchronized(ACTIVE){
            for(Iterator<Map.Entry<UUID,Active>> it=ACTIVE.entrySet().iterator();it.hasNext();){
                Active active=it.next().getValue();Entity entity=resolve(client,active);
                if(entity==null){if(++active.unresolvedTicks>UNRESOLVED_TTL_TICKS)it.remove();continue;}
                active.unresolvedTicks=0;
                if(entity.isRemoved()||(entity instanceof LivingEntity living&&(living.isFallFlying()||FluidContext.intersects(living)))){it.remove();continue;}
                ensureInitialized(active,entity);
                if(active.mode==Mode.SUSTAIN){
                    active.transport=BodyOrientation.transport(active.transport,entity.getDeltaMovement());
                    Vec3 look=entity==client.getCameraEntity()?cameraForward(client):entity.getLookAngle();
                    if(entity instanceof LivingEntity living)active.transport=GravityFallAerodynamics.followLook(active.transport,look,living.yBodyRot);
                    if(active.transport.direction()!=null&&active.blendTicks<ENTRY_BLEND_TICKS)active.blendTicks=Math.min(ENTRY_BLEND_TICKS,active.blendTicks+1.0F);
                }else active.landTicks+=1.0F;
            }
        }
    }

    public static Quaternionf body(Entity entity,float partialTick){
        if(entity==null)return null;
        synchronized(ACTIVE){Active active=ACTIVE.get(entity.getUUID());if(active==null||active.entityId!=entity.getId())return null;ensureInitialized(active,entity);return currentBody(active,entity,clampPartial(partialTick));}
    }
    public static Quaternionf extraRoot(Entity entity,float partialTick){Quaternionf body=body(entity,partialTick);if(body==null)return null;return BodyRenderMath.extraRoot(VisualTransitions.current(entity),body);}
    public static boolean active(Entity entity){if(entity==null)return false;synchronized(ACTIVE){Active a=ACTIVE.get(entity.getUUID());return a!=null&&a.entityId==entity.getId();}}
    public static boolean landing(Entity entity){if(entity==null)return false;synchronized(ACTIVE){Active a=ACTIVE.get(entity.getUUID());return a!=null&&a.entityId==entity.getId()&&a.mode==Mode.LAND;}}
    public static float blendProgress(Entity entity,float partialTick){if(entity==null)return 0.0F;synchronized(ACTIVE){Active a=ACTIVE.get(entity.getUUID());if(a==null||a.entityId!=entity.getId()||a.mode!=Mode.SUSTAIN)return 0.0F;return Math.min(1.0F,(a.blendTicks+clampPartial(partialTick))/ENTRY_BLEND_TICKS);}}
    public static void clear(){synchronized(ACTIVE){ACTIVE.clear();LATEST_SEQUENCE.clear();}}

    private static Quaternionf currentBody(Active active,Entity entity,float partialTick){
        if(!active.initialized)return null;
        if(active.mode==Mode.LAND){
            if(active.landStart==null||active.landTarget==null)return VisualTransitions.current(entity);
            if(active.landDurationTicks<=1.0E-4F)return new Quaternionf(active.landTarget);
            float progress=Math.min(1.0F,(active.landTicks+partialTick)/active.landDurationTicks);
            return new Quaternionf(active.landStart).slerp(active.landTarget,smoothstep(progress)).normalize();
        }
        if(active.transport==null)return new Quaternionf(active.blendStart);
        Quaternionf target=active.transport.orientation();
        if(active.blendStart==null||active.blendTicks>=ENTRY_BLEND_TICKS)return target;
        float progress=Math.min(1.0F,(active.blendTicks+partialTick)/ENTRY_BLEND_TICKS);
        return new Quaternionf(active.blendStart).slerp(target,smoothstep(progress)).normalize();
    }

    private static float landingDuration(float etaTicks){if(!Float.isFinite(etaTicks)||etaTicks<=0.0F)return 0.0F;return Math.min(etaTicks,LandingTiming.PRESENTATION_TICKS);}
    private static Entity resolve(Minecraft client,Active active){if(client.level==null)return null;Entity entity=client.level.getEntity(active.entityId);if(entity==null||!active.uuid.equals(entity.getUUID()))return null;return entity;}
    private static Vec3 cameraForward(Minecraft client){Vector3f forward=client.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));return new Vec3(forward.x,forward.y,forward.z).normalize();}
    private static float smoothstep(float value){float t=Math.max(0.0F,Math.min(1.0F,value));return t*t*(3.0F-2.0F*t);}
    private static float clampPartial(float value){return Float.isFinite(value)?Math.max(0.0F,Math.min(1.0F,value)):0.0F;}
}
