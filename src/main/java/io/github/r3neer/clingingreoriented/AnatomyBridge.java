package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import java.lang.reflect.Method;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** Optional version bridge. Reflection is resolved once; no geometry or transport is implemented here. */
public final class AnatomyBridge {
    private AnatomyBridge() {}

    private record Api(Method active, Method clear, Method contact, Method support, Method fits, Method parent) {}
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
            var frame = Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GravityFrame", false, loader).getConstructor(Direction.class);
            var frames = Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GravityFrames", false, loader);
            var parent = Class.forName("io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement$Contact", false, loader).getMethod("support");
            var candidate = new Api(
                core.getMethod("active", Entity.class),
                core.getMethod("clear", Entity.class),
                core.getMethod("contact", Entity.class),
                core.getMethod("supported", Entity.class),
                core.getMethod("spaceClear", Entity.class, AABB.class),
                parent
            );
            Function<Entity, Object> resolver = entity -> {
                try { return frame.newInstance(GravityDirectionUtil.getGravityDirection(entity)); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot query anatomical gravity frame", failure); }
            };
            frames.getMethod("install", String.class, Function.class).invoke(null, "clinging_reoriented:gravity_changer", resolver);
            api = candidate;
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            api = null;
        }
    }

    private static Object call(Method method, Object receiver, Object... arguments) {
        try { return method.invoke(receiver, arguments); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Anatomical bridge invocation failed", failure); }
    }

    public static boolean active(Entity entity) { return api != null && (boolean) call(api.active(), null, entity); }
    public static void clear(Entity entity) { if (api != null) call(api.clear(), null, entity); }
    public static boolean supported(Entity entity) { return api != null && (boolean) call(api.support(), null, entity); }
    public static boolean spaceClear(Entity entity, AABB box) { return api == null || (boolean) call(api.fits(), null, entity, box); }
    public static LivingEntity parent(Entity entity) {
        if (api == null) return null;
        var contact = call(api.contact(), null, entity);
        return contact == null ? null : (LivingEntity) call(api.parent(), contact);
    }
}
