package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.MobGravityNavigation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures vanilla navigation intent without intercepting the planner's internal moveTo(Path, speed). */
@Mixin(PathNavigation.class)
public abstract class GravityNavigationMixin {
    @Shadow @Final protected Mob mob;
    @Shadow public abstract Path getPath();

    @Inject(method="moveTo(Lnet/minecraft/world/entity/Entity;D)Z",at=@At("HEAD"),cancellable=true)
    private void clinging$entityIntentHead(Entity target,double speed,CallbackInfoReturnable<Boolean> cir){
        if(MobGravityNavigation.interceptEntityRequest(mob,target,speed))cir.setReturnValue(true);
    }

    @Inject(method="moveTo(Lnet/minecraft/world/entity/Entity;D)Z",at=@At("RETURN"),cancellable=true)
    private void clinging$entityIntentReturn(Entity target,double speed,CallbackInfoReturnable<Boolean> cir){
        Path path=getPath();
        var intent=new MobGravityNavigation.EntityIntent(target);
        boolean satisfied=cir.getReturnValue()&&MobGravityNavigation.ordinaryPathSatisfies(mob,intent,path);
        if(MobGravityNavigation.requestEntityAfterVanilla(mob,target,speed,satisfied))cir.setReturnValue(true);
    }

    @Inject(method="moveTo(DDDD)Z",at=@At("HEAD"),cancellable=true)
    private void clinging$positionIntentHead(double x,double y,double z,double speed,CallbackInfoReturnable<Boolean> cir){
        if(MobGravityNavigation.interceptPositionRequest(mob,new Vec3(x,y,z),speed))cir.setReturnValue(true);
    }

    @Inject(method="moveTo(DDDD)Z",at=@At("RETURN"),cancellable=true)
    private void clinging$positionIntentReturn(double x,double y,double z,double speed,CallbackInfoReturnable<Boolean> cir){
        Vec3 target=new Vec3(x,y,z);Path path=getPath();
        var intent=new MobGravityNavigation.PositionIntent(target);
        boolean satisfied=cir.getReturnValue()&&MobGravityNavigation.ordinaryPathSatisfies(mob,intent,path);
        if(MobGravityNavigation.requestPositionAfterVanilla(mob,target,speed,satisfied))cir.setReturnValue(true);
    }

    @Inject(method="moveTo(DDDID)Z",at=@At("HEAD"),cancellable=true)
    private void clinging$positionRangeIntentHead(double x,double y,double z,int reachRange,double speed,CallbackInfoReturnable<Boolean> cir){
        if(MobGravityNavigation.interceptPositionRequest(mob,new Vec3(x,y,z),speed))cir.setReturnValue(true);
    }

    @Inject(method="moveTo(DDDID)Z",at=@At("RETURN"),cancellable=true)
    private void clinging$positionRangeIntentReturn(double x,double y,double z,int reachRange,double speed,CallbackInfoReturnable<Boolean> cir){
        Vec3 target=new Vec3(x,y,z);Path path=getPath();
        var intent=new MobGravityNavigation.PositionIntent(target);
        boolean satisfied=cir.getReturnValue()&&MobGravityNavigation.ordinaryPathSatisfies(mob,intent,path);
        if(MobGravityNavigation.requestPositionAfterVanilla(mob,target,speed,satisfied))cir.setReturnValue(true);
    }
}
