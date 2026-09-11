package io.github.r3neer.clingingreoriented.client;

import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

/** Client-only ownership fence for visual transitions initiated by Clinging. */
public final class VisualTransitions {
    private static final long TARGET_WAIT_NANOS=2_000_000_000L;
    private static final Map<GravityRotationAnimation,Active> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Field animationField;
    private static volatile boolean fieldResolved;
    private static long latestSequence=-1;
    private record Active(Entity entity,Direction target,long sequence,long startedNanos,boolean targetSeen) {}
    private VisualTransitions() {}

    public static void begin(Entity entity,Direction target,long sequence){
        if(entity==null||target==null||sequence<=latestSequence)return;
        latestSequence=sequence;
        GravityRotationAnimation animation=animation(entity);
        if(animation==null)return;
        synchronized(ACTIVE){ACTIVE.put(animation,new Active(entity,target,sequence,System.nanoTime(),false));}
    }

    public static boolean owns(GravityRotationAnimation animation){
        if(animation==null)return false;
        synchronized(ACTIVE){return ACTIVE.containsKey(animation);}
    }

    public static boolean owns(Entity entity){
        GravityRotationAnimation animation=animation(entity);
        return animation!=null&&owns(animation);
    }

    public static void tick(Entity local){
        if(local==null)return;
        GravityRotationAnimation animation=animation(local);
        if(animation==null)return;
        Active active;
        synchronized(ACTIVE){active=ACTIVE.get(animation);}
        if(active==null)return;
        long now=System.nanoTime();
        Direction actual=GravityDirectionUtil.getGravityDirection(local);
        if(actual!=active.target()){
            if(active.targetSeen()||now-active.startedNanos()>TARGET_WAIT_NANOS)clear(animation);
            return;
        }
        if(!active.targetSeen()){
            active=new Active(local,active.target(),active.sequence(),active.startedNanos(),true);
            synchronized(ACTIVE){ACTIVE.put(animation,active);}
        }
        Quaternionf visual=((GravityAnimationEntity)local).gravitychanger$getVisualGravityRotation(actual);
        Quaternionf target=RotationUtil.getEntityRotationQuaternion(actual);
        float dot=Math.abs(visual.dot(target));
        if(Float.isFinite(dot)&&dot>=0.999999f)clear(animation);
    }

    public static void clear(){
        synchronized(ACTIVE){ACTIVE.clear();}
        latestSequence=-1;
    }
    private static void clear(GravityRotationAnimation animation){synchronized(ACTIVE){ACTIVE.remove(animation);}}

    private static GravityRotationAnimation animation(Entity entity){
        Field field=animationField;
        if(!fieldResolved){
            synchronized(VisualTransitions.class){
                if(!fieldResolved){
                    for(Field candidate:Entity.class.getDeclaredFields())if(candidate.getType()==GravityRotationAnimation.class){candidate.setAccessible(true);field=candidate;break;}
                    animationField=field;fieldResolved=true;
                    if(field==null)org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID).warn("Cannot locate Gravity Changer visual animation field; Clinging camera timing ownership is disabled");
                }else field=animationField;
            }
        }
        if(field==null)return null;
        try{return (GravityRotationAnimation)field.get(entity);}catch(IllegalAccessException e){return null;}
    }
}
