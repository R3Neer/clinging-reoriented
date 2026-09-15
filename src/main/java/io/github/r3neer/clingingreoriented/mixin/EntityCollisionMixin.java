package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.clingingreoriented.*;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
        if(!(source instanceof Player p) || !ClingingReoriented.controlsPhysics(p) || AnatomyBridge.active(p)
            || !ScaleBridge.legacyApiAvailable()) return;
        var state=ClingingReoriented.data(p);
        var surfaces=p.level().getEntitiesOfClass(LivingEntity.class,area,e->ScaleBridge.eligible(p,e)
            && !(state.carrying&&e.getUUID().equals(state.support)));
        if(surfaces.isEmpty())return;

        // Collision is local to the swept query, not dependent on a previous selection. Allocate a
        // replacement list only when a real compatible moving surface actually contributes shapes.
        var original=cir.getReturnValue();
        var shapes=new ArrayList<VoxelShape>(original.size()+surfaces.size());
        shapes.addAll(original);
        for(var entity:surfaces)shapes.add(Shapes.create(entity.getBoundingBox()));
        cir.setReturnValue(shapes);
    }
}
