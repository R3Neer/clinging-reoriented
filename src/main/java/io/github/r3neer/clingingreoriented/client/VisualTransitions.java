package io.github.r3neer.clingingreoriented.client;

import com.moigferdsrte.gravitychanger.client.GravityAnimationEntity;
import com.moigferdsrte.gravitychanger.client.GravityRotationAnimation;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.GravityTransition;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Quaternionf;

/** Client-only ownership fence and snap trajectory for transitions initiated by Clinging. */
public final class VisualTransitions {
    private static final long TARGET_WAIT_NANOS=2_000_000_000L;
    private static final Map<GravityRotationAnimation,Active> ACTIVE=Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Field animationField;
    private static volatile boolean fieldResolved;
    private static long latestSequence=-1;

    private record Active(
        Entity entity,
        Direction target,
        GravityTransition.TurnKind kind,
        long sequence,
        Quaternionf startVisual,
        long receivedNanos,
        long startedNanos,
        boolean targetSeen
    ) {}

    private VisualTransitions() {}

    public static void begin(Entity entity,Direction target,float yawDelta,int kindId,long sequence){
        GravityTransition.TurnKind kind=GravityTransition.TurnKind.fromId(kindId);
        if(entity==null||target==null||kind==null||!Float.isFinite(yawDelta)||sequence<=latestSequence)return;
        GravityRotationAnimation animation=animation(entity);
        if(animation==null)return;
        long now=System.nanoTime();
        Direction actual=GravityDirectionUtil.getGravityDirection(entity);
        // If Reorientation interrupts an active snap, this reads exactly the frame currently on screen.
        Quaternionf currentVisual=animation.getRotation(actual,now);
        Quaternionf start=GravityTransition.compensatedVisualStart(currentVisual,yawDelta);

        entity.setYRot(Mth.wrapDegrees(entity.getYRot()+yawDelta));
        entity.yRotO=Mth.wrapDegrees(entity.yRotO+yawDelta);

        latestSequence=sequence;
        synchronized(ACTIVE){
            ACTIVE.put(animation,new Active(entity,target,kind,sequence,start,now,0L,false));
        }
    }

    /**
     * Returns a Clinging-owned visual frame, or null to let Gravity Changer run normally.
     * Called from the GravityRotationAnimation mixin before upstream interpolation.
     */
    public static Quaternionf override(GravityRotationAnimation animation,Direction actual,long now){
        if(animation==null||actual==null)return null;
        Active active;
        synchronized(ACTIVE){active=ACTIVE.get(animation);}
        if(active==null)return null;

        if(actual!=active.target()){
            if(active.targetSeen()||now-active.receivedNanos()>TARGET_WAIT_NANOS){
                clear(animation);
                return null;
            }
            return new Quaternionf(active.startVisual());
        }

        if(!active.targetSeen()){
            // Put Gravity Changer's hidden state at the exact canonical endpoint so the
            // upstream 1.25 s animation can never resume when our short snap releases it.
            animation.forceSet(active.target(),now);
            active=new Active(active.entity(),active.target(),active.kind(),active.sequence(),
                active.startVisual(),active.receivedNanos(),now,true);
            synchronized(ACTIVE){ACTIVE.put(animation,active);}
        }

        long elapsed=Math.max(0L,now-active.startedNanos());
        float progress=(float)Math.min(1.0D,elapsed/(double)active.kind().durationNanos());
        float eased=GravityTransition.easeOutCubic(progress);
        Quaternionf target=RotationUtil.getEntityRotationQuaternion(active.target());
        Quaternionf result=new Quaternionf(active.startVisual()).slerp(target,eased);
        if(progress>=1.0F){
            clear(animation);
            return target;
        }
        return result;
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
        if(animation==null||!owns(animation))return;
        animation.getRotation(GravityDirectionUtil.getGravityDirection(local),System.nanoTime());
    }

    public static Quaternionf current(Entity entity){
        GravityRotationAnimation animation=animation(entity);
        if(animation==null)return RotationUtil.getEntityRotationQuaternion(GravityDirectionUtil.getGravityDirection(entity));
        return animation.getRotation(GravityDirectionUtil.getGravityDirection(entity),System.nanoTime());
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
                    if(field==null)org.slf4j.LoggerFactory.getLogger(ClingingReoriented.ID).warn("Cannot locate Gravity Changer visual animation field; Clinging snap presentation is disabled");
                }else field=animationField;
            }
        }
        if(field==null)return null;
        try{return (GravityRotationAnimation)field.get(entity);}catch(IllegalAccessException e){return null;}
    }
}
