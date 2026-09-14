package io.github.r3neer.clingingreoriented.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Captures First Person's final per-frame model offset without a production compile dependency on that mod. */
public final class FirstPersonRenderBridge {
    private static final Map<Entity,Vec3> OFFSETS=new WeakHashMap<>();
    private FirstPersonRenderBridge() {}

    public static void capture(Entity entity,Vec3 worldOffset){
        if(entity==null||worldOffset==null||!Double.isFinite(worldOffset.x+worldOffset.y+worldOffset.z))return;
        synchronized(OFFSETS){OFFSETS.put(entity,worldOffset);}
    }

    public static Vec3 offset(Entity entity){
        if(entity==null)return Vec3.ZERO;
        synchronized(OFFSETS){return OFFSETS.getOrDefault(entity,Vec3.ZERO);}
    }

    public static void clear(){synchronized(OFFSETS){OFFSETS.clear();}}
}
