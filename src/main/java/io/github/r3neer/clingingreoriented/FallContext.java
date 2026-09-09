package io.github.r3neer.clingingreoriented;
import net.minecraft.world.entity.LivingEntity;
public final class FallContext {
    private FallContext() {}
    public static final ThreadLocal<LivingEntity> CURRENT=new ThreadLocal<>();
}
