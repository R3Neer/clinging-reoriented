package io.github.r3neer.clingingreoriented;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Optional linkage is isolated from the base mod's class-loading path. */
public final class ScaleBridge {
    public static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("scalebrews");
    public static boolean eligible(Entity body, LivingEntity surface) {
        return PRESENT && Installed.eligible(body, surface);
    }
    public static void clear(Entity player) { if (PRESENT) { Installed.clear(player); AnatomyBridge.clear(player); } }
    public static void baseline(ServerPlayer player, Vec3 delta) { if (PRESENT) Installed.baseline(player, delta); }
    public static LivingEntity parent(Entity entity) { return AnatomyBridge.active(entity)?AnatomyBridge.parent(entity):PRESENT ? Installed.parent(entity) : null; }
    private static final class Installed {
        static boolean eligible(Entity body, LivingEntity surface) {
            return io.github.r3neer.scalebrews.platform.Platforms.eligible(body, surface);
        }
        static void clear(Entity player) { io.github.r3neer.scalebrews.platform.Platforms.state(player).clear(); }
        static LivingEntity parent(Entity entity) { return io.github.r3neer.scalebrews.platform.Platforms.state(entity).support; }
        static void baseline(ServerPlayer player, Vec3 delta) {
            ((io.github.r3neer.scalebrews.platform.PlatformConnection) player.connection).scalebrews$transportBaseline(player, delta);
        }
    }
}
