package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.scalebrews.mount.WolfMount;
import com.moigferdsrte.gravitychanger.util.*;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(WolfMount.class)
public abstract class ScaleWolfGravityMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/animal/wolf/Wolf;setDeltaMovement(DDD)V"))
    private static void clinging$landing(Wolf wolf,double x,double y,double z,Operation<Void> original){
        var gravity=GravityDirectionUtil.getGravityDirection(wolf);
        if(gravity==net.minecraft.core.Direction.DOWN){original.call(wolf,x,y,z);return;}
        var local=RotationUtil.vecWorldToPlayer(wolf.getDeltaMovement(),gravity);
        wolf.setDeltaMovement(RotationUtil.vecPlayerToWorld(new Vec3(0,local.y,0),gravity));
    }
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lio/github/r3neer/scalebrews/mount/WolfMount;launchVelocity(FFD)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 clinging$pounce(float yaw,float pitch,double charge,Operation<Vec3> original,Wolf wolf){
        return RotationUtil.vecPlayerToWorld(original.call(yaw,pitch,charge),GravityDirectionUtil.getGravityDirection(wolf));
    }
}
