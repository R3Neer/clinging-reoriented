package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;
@Mixin(EntityGetter.class)
public interface EntityCollisionMixin {
    @Inject(method="getEntityCollisions",at=@At("RETURN"),cancellable=true)
    private void clinging$surface(Entity source,AABB area,CallbackInfoReturnable<List<VoxelShape>> cir){
        if(!(source instanceof Player p) || !ClingingReoriented.controlsPhysics(p) || AnatomyBridge.active(p)) return;
        var state=ClingingReoriented.data(p);
        var shapes=new ArrayList<>(cir.getReturnValue());
        // Collision is local to the swept query, not dependent on a previous selection.
        // A player can now fall onto an eligible entity from arbitrarily far away.
        for(var entity:p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,area,e->ScaleBridge.eligible(p,e))) {
            if(state.carrying && entity.getUUID().equals(state.support)) continue;
            shapes.add(Shapes.create(entity.getBoundingBox()));
        }
        cir.setReturnValue(List.copyOf(shapes));
    }
}
