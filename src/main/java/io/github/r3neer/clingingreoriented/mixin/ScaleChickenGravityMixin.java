package io.github.r3neer.clingingreoriented.mixin;
import io.github.r3neer.scalebrews.mount.TinyMounts;
import com.moigferdsrte.gravitychanger.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.phys.Vec3;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Chicken.class)
public abstract class ScaleChickenGravityMixin {
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 clinging$noWorldYDamping(Vec3 movement,double x,double y,double z,Operation<Vec3> original){
        var chicken=(Chicken)(Object)this;
        return GravityDirectionUtil.getGravityDirection(chicken)!=Direction.DOWN && TinyMounts.controller(chicken)!=null?movement:original.call(movement,x,y,z);
    }
    @Inject(method="aiStep",at=@At("TAIL"))
    private void clinging$localGlide(CallbackInfo ci){
        var chicken=(Chicken)(Object)this;var gravity=GravityDirectionUtil.getGravityDirection(chicken);var rider=TinyMounts.controller(chicken);
        if(gravity==Direction.DOWN || rider==null || chicken.onGround())return;
        if(TinyMounts.definition(chicken).ability()==io.github.r3neer.scalebrews.mount.TinyMountDefinition.Ability.CHICKEN_GLIDE && !TinyMounts.input(rider).jump())return;
        var local=RotationUtil.vecWorldToPlayer(chicken.getDeltaMovement(),gravity);
        if(local.y<0)chicken.setDeltaMovement(RotationUtil.vecPlayerToWorld(local.multiply(1,.6,1),gravity));
    }
}
