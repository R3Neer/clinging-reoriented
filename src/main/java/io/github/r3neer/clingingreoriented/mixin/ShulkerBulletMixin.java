package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ShulkerChargeProjectile;
import io.github.r3neer.clingingreoriented.ShulkerChargeTargeting;
import io.github.r3neer.clingingreoriented.ShulkerCharges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBullet.class)
public abstract class ShulkerBulletMixin extends Projectile implements ShulkerChargeProjectile {
    @Shadow private @Nullable EntityReference<Entity> finalTarget;
    @Shadow private @Nullable Direction currentMoveDirection;
    @Shadow private int flightSteps;
    @Shadow private double targetDeltaX;
    @Shadow private double targetDeltaY;
    @Shadow private double targetDeltaZ;
    @Shadow private void selectNextMoveDirection(@Nullable Direction.Axis avoidAxis,@Nullable Entity target){throw new AssertionError();}

    @Unique private boolean clinging$captured;
    @Unique private boolean clinging$launchedCharge;
    @Unique private double clinging$intentX;
    @Unique private double clinging$intentY;
    @Unique private double clinging$intentZ=1.0D;
    @Unique private @Nullable BlockPos clinging$targetBlock;
    @Unique private int clinging$reacquireTicks;

    protected ShulkerBulletMixin(EntityType<? extends Projectile> type,Level level){super(type,level);}

    @Override public void clinging$initializeCharge(Vec3 rawIntent){
        Vec3 intent=ShulkerChargeTargeting.safeIntent(rawIntent);clinging$launchedCharge=true;clinging$captured=false;
        clinging$intentX=intent.x;clinging$intentY=intent.y;clinging$intentZ=intent.z;setNoGravity(true);clinging$reacquireTicks=0;
        if(level() instanceof ServerLevel)clinging$acquireOrFly();else clinging$setFreeFlight(intent);
    }
    @Override public boolean clinging$isLaunchedCharge(){return clinging$launchedCharge;}
    @Override public Vec3 clinging$intent(){return new Vec3(clinging$intentX,clinging$intentY,clinging$intentZ);}
    @Override public @Nullable BlockPos clinging$targetBlock(){return clinging$targetBlock;}
    @Override public @Nullable Entity clinging$targetEntity(){return clinging$resolveEntityTarget();}
    @Override public void clinging$forceAcquire(){if(level() instanceof ServerLevel&&clinging$launchedCharge&&!clinging$hasValidTarget())clinging$acquireOrFly();}

    @Inject(method="tick",at=@At("HEAD"))
    private void clinging$tickCharge(CallbackInfo ci){
        if(!clinging$launchedCharge||level().isClientSide())return;setNoGravity(true);
        if(clinging$hasValidTarget())return;
        finalTarget=null;clinging$targetBlock=null;
        if(clinging$reacquireTicks>0){clinging$reacquireTicks--;return;}
        clinging$acquireOrFly();
    }

    @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
    private void clinging$saveCharge(ValueOutput output,CallbackInfo ci){
        output.putBoolean("ClingingCharge",clinging$launchedCharge);output.putBoolean("ClingingCaptured",clinging$captured);
        if(!clinging$launchedCharge)return;
        output.putDouble("ClingingIntentX",clinging$intentX);output.putDouble("ClingingIntentY",clinging$intentY);output.putDouble("ClingingIntentZ",clinging$intentZ);
        if(clinging$targetBlock!=null)output.store("ClingingTargetBlock",BlockPos.CODEC,clinging$targetBlock);
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void clinging$loadCharge(ValueInput input,CallbackInfo ci){
        clinging$launchedCharge=input.getBooleanOr("ClingingCharge",false);clinging$captured=input.getBooleanOr("ClingingCaptured",false);
        if(!clinging$launchedCharge)return;
        Vec3 intent=ShulkerChargeTargeting.safeIntent(new Vec3(input.getDoubleOr("ClingingIntentX",0.0D),input.getDoubleOr("ClingingIntentY",0.0D),input.getDoubleOr("ClingingIntentZ",1.0D)));
        clinging$intentX=intent.x;clinging$intentY=intent.y;clinging$intentZ=intent.z;clinging$targetBlock=input.read("ClingingTargetBlock",BlockPos.CODEC).orElse(null);setNoGravity(true);
    }

    @Inject(method="hurtServer",at=@At("HEAD"))
    private void clinging$dropCapturedCharge(ServerLevel level,DamageSource source,float damage,CallbackInfoReturnable<Boolean> cir){
        if(clinging$captured)return;Entity direct=source.getDirectEntity();
        boolean arrow=direct instanceof AbstractArrow;boolean melee=!source.is(DamageTypeTags.IS_PROJECTILE)&&direct instanceof LivingEntity;
        if(!arrow&&!melee)return;clinging$captured=true;spawnAtLocation(level,ShulkerCharges.ITEM);
    }

    @Unique private boolean clinging$hasValidTarget(){
        Entity entity=clinging$resolveEntityTarget();if(entity!=null&&entity.isAlive()&&(!(entity instanceof Player p)||!p.isSpectator()))return true;
        if(clinging$targetBlock==null)return false;
        if(!(level() instanceof ServerLevel server)||!server.isLoaded(clinging$targetBlock))return false;
        return server.getBlockState(clinging$targetBlock).is(Blocks.TARGET);
    }
    @Unique private @Nullable Entity clinging$resolveEntityTarget(){return finalTarget==null?null:EntityReference.getEntity(finalTarget,level());}
    @Unique private void clinging$acquireOrFly(){
        if(!(level() instanceof ServerLevel server))return;Vec3 intent=clinging$intent();var acquisition=ShulkerChargeTargeting.acquire(server,(ShulkerBullet)(Object)this,intent);
        clinging$reacquireTicks=4;
        if(acquisition.entity()!=null){clinging$targetBlock=null;finalTarget=EntityReference.of(acquisition.entity());selectNextMoveDirection(null,acquisition.entity());return;}
        if(acquisition.block()!=null){finalTarget=null;clinging$targetBlock=acquisition.block();clinging$setFreeFlight(Vec3.atCenterOf(acquisition.block()).subtract(position()));return;}
        finalTarget=null;clinging$targetBlock=null;clinging$setFreeFlight(intent);
    }
    @Unique private void clinging$setFreeFlight(Vec3 vector){
        Vec3 safe=ShulkerChargeTargeting.safeIntent(vector);Direction direction=Direction.getApproximateNearest(safe.x,safe.y,safe.z);currentMoveDirection=direction;flightSteps=10;
        Vec3 delta=new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ()).scale(0.15D);targetDeltaX=delta.x;targetDeltaY=delta.y;targetDeltaZ=delta.z;setDeltaMovement(delta);
    }
}
