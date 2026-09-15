package io.github.r3neer.clingingreoriented;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
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
            MethodHandles.Lookup lookup=MethodHandles.publicLookup();
            return new LegacyApi(
                lookup.unreflect(platforms.getMethod("eligible", Entity.class, LivingEntity.class)),
                lookup.unreflect(platforms.getMethod("state", Entity.class)),
                lookup.unreflect(state.getMethod("clear")),
                lookup.unreflectGetter(state.getField("support")),
                connection,
                lookup.unreflect(connection.getMethod("scalebrews$transportBaseline", Entity.class, Vec3.class))
            );
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            return null;
        }
    }

    private static IllegalStateException invocationFailure(Throwable failure) {
        return new IllegalStateException("Scale Brews compatibility invocation failed",failure);
    }

    private record LegacyApi(MethodHandle eligible, MethodHandle state, MethodHandle clear, MethodHandle support,
                             Class<?> connection, MethodHandle baseline) {
        boolean eligible(Entity body, LivingEntity surface) {
            try { return (boolean)eligible.invoke(body,surface); }
            catch(Throwable failure){throw invocationFailure(failure);}
        }
        void clear(Entity entity) {
            try { clear.invoke(state.invoke(entity)); }
            catch(Throwable failure){throw invocationFailure(failure);}
        }
        LivingEntity parent(Entity entity) {
            try { return (LivingEntity)support.invoke(state.invoke(entity)); }
            catch(Throwable failure){throw invocationFailure(failure);}
        }
        void baseline(ServerPlayer player, Vec3 delta) {
            if (!connection.isInstance(player.connection))return;
            try { baseline.invoke(player.connection,player,delta); }
            catch(Throwable failure){throw invocationFailure(failure);}
        }
    }
}
