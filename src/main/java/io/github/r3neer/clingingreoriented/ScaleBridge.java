package io.github.r3neer.clingingreoriented;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Optional Scale Brews linkage resolved without a compile-time dependency. */
public final class ScaleBridge {
    public static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("scalebrews");
    private static final LegacyApi LEGACY = resolveLegacy();

    private ScaleBridge() {}

    public static boolean legacyApiAvailable() { return LEGACY != null; }
    public static boolean eligible(Entity body, LivingEntity surface) { return LEGACY != null && LEGACY.eligible(body, surface); }
    public static void clear(Entity player) {
        if (!PRESENT) return;
        if (LEGACY != null) LEGACY.clear(player);
        AnatomyBridge.clear(player);
    }
    public static void baseline(ServerPlayer player, Vec3 delta) { if (LEGACY != null) LEGACY.baseline(player, delta); }
    public static LivingEntity parent(Entity entity) {
        if (AnatomyBridge.active(entity)) return AnatomyBridge.parent(entity);
        return LEGACY == null ? null : LEGACY.parent(entity);
    }

    private static LegacyApi resolveLegacy() {
        if (!PRESENT) return null;
        try {
            ClassLoader loader = ScaleBridge.class.getClassLoader();
            Class<?> platforms = Class.forName("io.github.r3neer.scalebrews.platform.Platforms", false, loader);
            Class<?> state = Class.forName("io.github.r3neer.scalebrews.platform.PlatformState", false, loader);
            Class<?> connection = Class.forName("io.github.r3neer.scalebrews.platform.PlatformConnection", false, loader);
            return new LegacyApi(
                platforms.getMethod("eligible", Entity.class, LivingEntity.class),
                platforms.getMethod("state", Entity.class),
                state.getMethod("clear"),
                state.getField("support"),
                connection,
                connection.getMethod("scalebrews$transportBaseline", Entity.class, Vec3.class)
            );
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            return null;
        }
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) {
        try { return method.invoke(receiver, arguments); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Scale Brews compatibility invocation failed", failure); }
    }

    private static Object read(Field field, Object receiver) {
        try { return field.get(receiver); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Scale Brews compatibility field access failed", failure); }
    }

    private record LegacyApi(Method eligible, Method state, Method clear, Field support, Class<?> connection, Method baseline) {
        boolean eligible(Entity body, LivingEntity surface) { return (boolean) invoke(eligible, null, body, surface); }
        void clear(Entity entity) { invoke(clear, invoke(state, null, entity)); }
        LivingEntity parent(Entity entity) { return (LivingEntity) read(support, invoke(state, null, entity)); }
        void baseline(ServerPlayer player, Vec3 delta) {
            if (connection.isInstance(player.connection)) invoke(baseline, player.connection, player, delta);
        }
    }
}
