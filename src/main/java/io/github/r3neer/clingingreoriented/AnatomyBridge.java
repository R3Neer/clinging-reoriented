package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Optional version bridge. Dynamic linkage is resolved once; no geometry or transport is implemented here. */
public final class AnatomyBridge {
    private AnatomyBridge() {}

    private record Api(MethodHandle active,MethodHandle clear,MethodHandle contact,MethodHandle support,MethodHandle fits,MethodHandle parent) {}
    private static Api api;

    public static void initialize() {
        if (!ScaleBridge.PRESENT) return;
        try {
            ClassLoader loader = AnatomyBridge.class.getClassLoader();
            Class<?> core;
            try {
                core = Class.forName("io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement", false, loader);
            } catch (ClassNotFoundException oldVersion) {
                return;
            }
            MethodHandles.Lookup lookup=MethodHandles.publicLookup();
            MethodHandle frame=lookup.unreflectConstructor(Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GravityFrame", false, loader).getConstructor(Direction.class));
            var frames = Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GravityFrames", false, loader);
            MethodHandle parent=lookup.unreflect(Class.forName("io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement$Contact", false, loader).getMethod("support"));
            var candidate = new Api(
                lookup.unreflect(core.getMethod("active", Entity.class)),
                lookup.unreflect(core.getMethod("clear", Entity.class)),
                lookup.unreflect(core.getMethod("contact", Entity.class)),
                lookup.unreflect(core.getMethod("supported", Entity.class)),
                lookup.unreflect(core.getMethod("spaceClear", Entity.class, AABB.class)),
                parent
            );
            Function<Entity, Object> resolver = entity -> {
                try { return frame.invoke(GravityDirectionUtil.getGravityDirection(entity)); }
                catch (Throwable failure) { throw new IllegalStateException("Cannot query anatomical gravity frame", failure); }
            };
            frames.getMethod("install", String.class, Function.class).invoke(null, "clinging_reoriented:gravity_changer", resolver);
            api = candidate;
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            api = null;
        }
    }

    private static IllegalStateException invocationFailure(Throwable failure){
        return new IllegalStateException("Anatomical bridge invocation failed",failure);
    }

    public static boolean active(Entity entity) {
        Api current=api;if(current==null)return false;
        try{return (boolean)current.active().invoke(entity);}catch(Throwable failure){throw invocationFailure(failure);}
    }
    public static void clear(Entity entity) {
        Api current=api;if(current==null)return;
        try{current.clear().invoke(entity);}catch(Throwable failure){throw invocationFailure(failure);}
    }
    public static boolean supported(Entity entity) {
        Api current=api;if(current==null)return false;
        try{return (boolean)current.support().invoke(entity);}catch(Throwable failure){throw invocationFailure(failure);}
    }
    public static boolean spaceClear(Entity entity,AABB box) {
        Api current=api;if(current==null)return true;
        try{return (boolean)current.fits().invoke(entity,box);}catch(Throwable failure){throw invocationFailure(failure);}
    }
    public static LivingEntity parent(Entity entity) {
        Api current=api;if(current==null)return null;
        try{
            Object contact=current.contact().invoke(entity);
            return contact==null?null:(LivingEntity)current.parent().invoke(contact);
        }catch(Throwable failure){throw invocationFailure(failure);}
    }
}
