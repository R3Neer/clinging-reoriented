package io.github.r3neer.clingingreoriented.client;

import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.GravityTransition;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

/** Client-only ownership fence and snap trajectory for transitions initiated by Clinging. */
public final class VisualTransitions {
    private static final long TARGET_WAIT_NANOS=2_000_000_000L;
    private static final Map<GravityRotationAnimation,Active> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<UUID,Long> LATEST_ENTITY_SEQUENCE=new HashMap<>();
    private static volatile Field animationField;
    private static volatile boolean fieldResolved;
    private static long latestSequence=-1;

    private record Active(Entity entity,Direction target,GravityTransition.TurnKind kind,long sequence,
                          Quaternionf startVisual,long receivedNanos,long startedNanos,boolean targetSeen) {}

    private VisualTransitions() {}

    /** Connection-scoped local-player transition. */
    public static void begin(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        if(sequence<=latestSequence)return;
        if(enroll(entity,target,yawDelta,kindId,sequence))latestSequence=sequence;
    }

    /** UUID-scoped tracked-entity transition; entity-id reuse cannot steal ownership. */
    public static void beginTracked(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        if(entity==null)return;
        UUID uuid=entity.getUUID();
        synchronized(LATEST_ENTITY_SEQUENCE){if(sequence<=LATEST_ENTITY_SEQUENCE.getOrDefault(uuid,-1L))return;}
        if(enroll(entity,target,yawDelta,kindId,sequence))synchronized(LATEST_ENTITY_SEQUENCE){LATEST_ENTITY_SEQUENCE.put(uuid,sequence);}
    }

    private static boolean enroll(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        GravityTransition.TurnKind kind=GravityTransition.TurnKind.fromId(kindId);
        if(entity==null||target==null||kind==null||!Float.isFinite(yawDelta)||sequence<0)return false;
        GravityRotationAnimation animation=animation(entity);
        if(animation==null)return false;
        long now=System.nanoTime();
        Direction actual=GravityDirectionUtil.getGravityDirection(entity);
        Quaternionf currentVisual=animation.getRotation(actual,now);
        Quaternionf start=GravityTransition.compensatedVisualStart(currentVisual,yawDelta);
        GravityTransition.applyYawGauge(entity,yawDelta);
        synchronized(ACTIVE){ACTIVE.put(animation,new Active(entity,target,kind,sequence,start,now,0L,false));}
        return true;
    }

    public static Quaternionf override(GravityRotationAnimation animation,Direction actual,long now){
        if(animation==null||actual==null)return null;
        Active active;
        synchronized(ACTIVE){active=ACTIVE.get(animation);}
        if(active==null)return null;
        if(actual!=active.target()){
            if(active.targetSeen()||now-active.receivedNanos()>TARGET_WAIT_NANOS){clear(animation);return null;}
            return new Quaternionf(active.startVisual());
        }
        if(!active.targetSeen()){
            animation.forceSet(active.target(),now);
            active=new Active(active.entity(),active.target(),active.kind(),active.sequence(),active.startVisual(),active.receivedNanos(),now,true);
            synchronized(ACTIVE){ACTIVE.put(animation,active);}
        }
        long elapsed=Math.max(0L,now-active.startedNanos());
        float progress=(float)Math.min(1.0D,elapsed/(double)active.kind().durationNanos());
        Quaternionf target=RotationUtil.getEntityRotationQuaternion(active.target());
        Quaternionf result=new Quaternionf(active.startVisual()).slerp(target,GravityTransition.easeOutQuadratic(progress));
        if(progress>=1.0F){clear(animation);return target;}
        return result;
    }

    public static boolean owns(GravityRotationAnimation animation){if(animation==null)return false;synchronized(ACTIVE){return ACTIVE.containsKey(animation);}}
    public static boolean owns(Entity entity){GravityRotationAnimation animation=animation(entity);return animation!=null&&owns(animation);}
    public static void tick(Entity local){if(local==null)return;GravityRotationAnimation animation=animation(local);if(animation!=null&&owns(animation))animation.getRotation(GravityDirectionUtil.getGravityDirection(local),System.nanoTime());}
    public static Quaternionf current(Entity entity){GravityRotationAnimation animation=animation(entity);return animation==null?RotationUtil.getEntityRotationQuaternion(GravityDirectionUtil.getGravityDirection(entity)):animation.getRotation(GravityDirectionUtil.getGravityDirection(entity),System.nanoTime());}
    public static void clear(){synchronized(ACTIVE){ACTIVE.clear();}synchronized(LATEST_ENTITY_SEQUENCE){LATEST_ENTITY_SEQUENCE.clear();}latestSequence=-1;}
    private static void clear(GravityRotationAnimation animation){synchronized(ACTIVE){ACTIVE.remove(animation);}}

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
