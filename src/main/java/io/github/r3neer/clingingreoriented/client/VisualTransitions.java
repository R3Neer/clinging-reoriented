package io.github.r3neer.clingingreoriented.client;

import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.GravityTransition;
import io.github.r3neer.clingingreoriented.LandingTiming;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

/** Client-only ownership fence for held free-flight frames and landing/immediate snaps. */
public final class VisualTransitions {
    private static final long TARGET_WAIT_NANOS=2_000_000_000L;
    private static final Map<GravityRotationAnimation,Active> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<UUID,Long> LATEST_ENTITY_SEQUENCE=new HashMap<>();
    private static volatile Field animationField;
    private static volatile boolean fieldResolved;
    /** Makes the overwhelmingly common upstream-only getRotation path lock-free. */
    private static volatile boolean hasActiveTransitions;
    private static long latestSequence=-1;

    private enum Mode { HOLD, LAND, SNAP }
    private record Active(Entity entity,Direction target,GravityTransition.TurnKind kind,long sequence,Quaternionf startVisual,
                          long receivedNanos,long startedNanos,boolean targetSeen,Mode mode) {}

    private VisualTransitions() {}

    /** Connection-scoped immediate local-player transition (forced cleanup / legacy mounted presentation). */
    public static void begin(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        if(sequence<=latestSequence)return;
        if(enrollSnap(entity,target,yawDelta,kindId,sequence))latestSequence=sequence;
    }

    /** Physical turn with stable rendered world frame. */
    public static void hold(Entity entity,Direction target,float yawDelta,long sequence){
        if(sequence<=latestSequence||entity==null||target==null||!Float.isFinite(yawDelta)||sequence<0)return;
        GravityRotationAnimation animation=animation(entity);if(animation==null)return;
        long now=System.nanoTime();Direction actual=GravityDirectionUtil.getGravityDirection(entity);
        Quaternionf current=animation.getRotation(actual,now);
        Quaternionf held=GravityTransition.compensatedVisualStart(current,yawDelta);
        GravityTransition.applyYawGauge(entity,yawDelta);
        animation.forceSet(target,now);
        put(animation,new Active(entity,target,null,sequence,held,now,0L,actual==target,Mode.HOLD));
        latestSequence=sequence;
    }

    /** Commit a retained free-flight frame toward its imminent floor; no new yaw gauge is applied. */
    public static void land(Entity entity,Direction target,int kindId,long sequence){
        if(sequence<=latestSequence)return;
        GravityTransition.TurnKind kind=GravityTransition.TurnKind.fromId(kindId);
        if(entity==null||target==null||kind==null||sequence<0)return;
        GravityRotationAnimation animation=animation(entity);if(animation==null)return;
        long now=System.nanoTime();Direction actual=GravityDirectionUtil.getGravityDirection(entity);
        Quaternionf current=animation.getRotation(actual,now);
        boolean seen=actual==target;if(seen)animation.forceSet(target,now);
        put(animation,new Active(entity,target,kind,sequence,current,now,seen?now:0L,seen,Mode.LAND));
        latestSequence=sequence;
    }

    /** Cancel a landing: either freeze the exact current frame or release presentation ownership completely. */
    public static void cancel(Entity entity,boolean holdCurrent,long sequence){
        if(sequence<=latestSequence||entity==null||sequence<0)return;
        GravityRotationAnimation animation=animation(entity);if(animation==null)return;
        long now=System.nanoTime();Direction actual=GravityDirectionUtil.getGravityDirection(entity);
        Quaternionf current=animation.getRotation(actual,now);
        animation.forceSet(actual,now);
        if(holdCurrent)put(animation,new Active(entity,actual,null,sequence,current,now,0L,true,Mode.HOLD));
        else clear(animation);
        latestSequence=sequence;
    }

    /** UUID-scoped tracked-entity transition; entity-id reuse cannot steal ownership. */
    public static void beginTracked(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        if(entity==null)return;
        UUID uuid=entity.getUUID();
        synchronized(LATEST_ENTITY_SEQUENCE){if(sequence<=LATEST_ENTITY_SEQUENCE.getOrDefault(uuid,-1L))return;}
        if(enrollSnap(entity,target,yawDelta,kindId,sequence))synchronized(LATEST_ENTITY_SEQUENCE){LATEST_ENTITY_SEQUENCE.put(uuid,sequence);}
    }

    private static boolean enrollSnap(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        GravityTransition.TurnKind kind=GravityTransition.TurnKind.fromId(kindId);
        if(entity==null||target==null||kind==null||!Float.isFinite(yawDelta)||sequence<0)return false;
        GravityRotationAnimation animation=animation(entity);if(animation==null)return false;
        long now=System.nanoTime();Direction actual=GravityDirectionUtil.getGravityDirection(entity);
        Quaternionf currentVisual=animation.getRotation(actual,now);
        Quaternionf start=GravityTransition.compensatedVisualStart(currentVisual,yawDelta);
        GravityTransition.applyYawGauge(entity,yawDelta);
        put(animation,new Active(entity,target,kind,sequence,start,now,0L,false,Mode.SNAP));
        return true;
    }

    public static Quaternionf override(GravityRotationAnimation animation,Direction actual,long now){
        if(!hasActiveTransitions||animation==null||actual==null)return null;
        Active active; synchronized(ACTIVE){active=ACTIVE.get(animation);} if(active==null)return null;
        if(active.mode()==Mode.HOLD)return holdOverride(animation,active,actual,now);
        return snapOverride(animation,active,actual,now);
    }

    private static Quaternionf holdOverride(GravityRotationAnimation animation,Active active,Direction actual,long now){
        if(active.targetSeen()&&actual!=active.target()){clear(animation);return null;}
        if(!active.targetSeen()){
            if(actual==active.target()){
                animation.forceSet(active.target(),now);
                active=new Active(active.entity(),active.target(),null,active.sequence(),active.startVisual(),active.receivedNanos(),0L,true,Mode.HOLD);
                put(animation,active);
            }else if(now-active.receivedNanos()>TARGET_WAIT_NANOS){clear(animation);return null;}
        }
        return new Quaternionf(active.startVisual());
    }

    private static Quaternionf snapOverride(GravityRotationAnimation animation,Active active,Direction actual,long now){
        if(actual!=active.target()){
            if(active.targetSeen()||now-active.receivedNanos()>TARGET_WAIT_NANOS){clear(animation);return null;}
            return new Quaternionf(active.startVisual());
        }
        if(!active.targetSeen()){
            animation.forceSet(active.target(),now);
            active=new Active(active.entity(),active.target(),active.kind(),active.sequence(),active.startVisual(),active.receivedNanos(),now,true,active.mode());
            put(animation,active);
        }
        long elapsed=Math.max(0L,now-active.startedNanos());
        long duration=active.mode()==Mode.LAND?LandingTiming.PRESENTATION_NANOS:active.kind().durationNanos();
        float progress=(float)Math.min(1.0D,elapsed/(double)duration);
        Quaternionf target=RotationUtil.getEntityRotationQuaternion(active.target());
        Quaternionf result=new Quaternionf(active.startVisual()).slerp(target,GravityTransition.easeOutQuadratic(progress));
        if(progress>=1.0F){clear(animation);return target;}
        return result;
    }

    public static boolean owns(GravityRotationAnimation animation){if(!hasActiveTransitions||animation==null)return false;synchronized(ACTIVE){return ACTIVE.containsKey(animation);}}
    public static boolean owns(Entity entity){if(!hasActiveTransitions)return false;GravityRotationAnimation animation=animation(entity);return animation!=null&&owns(animation);}
    public static boolean holding(Entity entity){if(!hasActiveTransitions)return false;GravityRotationAnimation animation=animation(entity);if(animation==null)return false;synchronized(ACTIVE){var active=ACTIVE.get(animation);return active!=null&&active.mode()==Mode.HOLD;}}

    /** Advance every owned animation once per client tick, including tracked entities outside the renderer/frustum. */
    public static void tickAll(){
        if(!hasActiveTransitions)return;
        java.util.List<Map.Entry<GravityRotationAnimation,Active>> snapshot;
        synchronized(ACTIVE){
            if(ACTIVE.isEmpty()){hasActiveTransitions=false;return;}
            snapshot=new ArrayList<>(ACTIVE.entrySet());
        }
        long now=System.nanoTime();
        for(var entry:snapshot){
            var animation=entry.getKey();var active=entry.getValue();var entity=active.entity();
            if(animation==null||entity==null||entity.isRemoved()){if(animation!=null)clear(animation);continue;}
            animation.getRotation(GravityDirectionUtil.getGravityDirection(entity),now);
        }
    }

    public static Quaternionf current(Entity entity){GravityRotationAnimation animation=animation(entity);return animation==null?RotationUtil.getEntityRotationQuaternion(GravityDirectionUtil.getGravityDirection(entity)):animation.getRotation(GravityDirectionUtil.getGravityDirection(entity),System.nanoTime());}
    public static void clear(){synchronized(ACTIVE){ACTIVE.clear();hasActiveTransitions=false;}synchronized(LATEST_ENTITY_SEQUENCE){LATEST_ENTITY_SEQUENCE.clear();}latestSequence=-1;}
    private static void put(GravityRotationAnimation animation,Active active){synchronized(ACTIVE){ACTIVE.put(animation,active);hasActiveTransitions=true;}}
    private static void clear(GravityRotationAnimation animation){synchronized(ACTIVE){ACTIVE.remove(animation);if(ACTIVE.isEmpty())hasActiveTransitions=false;}}

    private static GravityRotationAnimation animation(Entity entity){
        Field field=animationField;
        if(!fieldResolved){
            synchronized(VisualTransitions.class){
                if(!fieldResolved){
                    for(Field candidate:Entity.class.getDeclaredFields())if(candidate.getType()==GravityRotationAnimation.class){candidate.setAccessible(true);field=candidate;break;}
                    animationField=field;fieldResolved=true;
                    if(field==null)org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID).warn("Cannot locate Gravity Changer visual animation field; Clinging snap presentation is disabled");
                }else field=animationField;
            }
        }
        if(field==null)return null;
        try{return (GravityRotationAnimation)field.get(entity);}catch(IllegalAccessException e){return null;}
    }
}
