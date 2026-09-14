package io.github.r3neer.clingingreoriented.mixin;

import io.github.r3neer.clingingreoriented.ShulkerChargeProjectile;
import io.github.r3neer.clingingreoriented.ShulkerChargeTargeting;
import io.github.r3neer.clingingreoriented.ShulkerCharges;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
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
    @Shadow private void selectNextMoveDirection(Direction.Axis avoidAxis,@Nullable Entity target){throw new AssertionError();}

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
    private void clinging$tickChargeHead(CallbackInfo ci){
        if(!clinging$launchedCharge||level().isClientSide())return;setNoGravity(true);
        if(clinging$validBlockTarget()){clinging$steerBlockTarget();return;}
        if(clinging$validEntityTarget())return;
        finalTarget=null;clinging$targetBlock=null;
        if(clinging$reacquireTicks>0){clinging$reacquireTicks--;return;}
        clinging$acquireOrFly();
    }

    @Inject(method="tick",at=@At("TAIL"))
    private void clinging$tickChargeTail(CallbackInfo ci){
        if(!clinging$launchedCharge||level().isClientSide()||!isAlive()||!clinging$validBlockTarget())return;
        if(flightSteps>0){--flightSteps;if(flightSteps==0)clinging$selectNextBlockMoveDirection(currentMoveDirection==null?null:currentMoveDirection.getAxis());}
        if(currentMoveDirection==null)return;
        BlockPos current=blockPosition();Direction.Axis axis=currentMoveDirection.getAxis();
        if(level().loadedAndEntityCanStandOn(current.relative(currentMoveDirection),this)){
            clinging$selectNextBlockMoveDirection(axis);return;
        }
        BlockPos target=clinging$targetBlock;
        if(target==null)return;
        if((axis==Direction.Axis.X&&current.getX()==target.getX())||(axis==Direction.Axis.Y&&current.getY()==target.getY())||(axis==Direction.Axis.Z&&current.getZ()==target.getZ()))clinging$selectNextBlockMoveDirection(axis);
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

    @Unique private boolean clinging$hasValidTarget(){return clinging$validEntityTarget()||clinging$validBlockTarget();}
    @Unique private boolean clinging$validEntityTarget(){Entity entity=clinging$resolveEntityTarget();return entity!=null&&entity.isAlive()&&(!(entity instanceof Player p)||!p.isSpectator());}
    @Unique private boolean clinging$validBlockTarget(){
        if(clinging$targetBlock==null||!(level() instanceof ServerLevel server)||!server.isLoaded(clinging$targetBlock))return false;
        return server.getBlockState(clinging$targetBlock).is(Blocks.TARGET);
    }
    @Unique private @Nullable Entity clinging$resolveEntityTarget(){return finalTarget==null?null:EntityReference.getEntity(finalTarget,level());}
    @Unique private void clinging$acquireOrFly(){
        if(!(level() instanceof ServerLevel server))return;Vec3 intent=clinging$intent();var acquisition=ShulkerChargeTargeting.acquire(server,(ShulkerBullet)(Object)this,intent);
        clinging$reacquireTicks=4;
        if(acquisition.entity()!=null){clinging$targetBlock=null;finalTarget=EntityReference.of(acquisition.entity());selectNextMoveDirection(null,acquisition.entity());return;}
        if(acquisition.block()!=null){finalTarget=null;clinging$targetBlock=acquisition.block();clinging$selectNextBlockMoveDirection(null);return;}
        finalTarget=null;clinging$targetBlock=null;clinging$setFreeFlight(intent);
    }
    @Unique private void clinging$steerBlockTarget(){
        targetDeltaX=Mth.clamp(targetDeltaX*1.025D,-1.0D,1.0D);targetDeltaY=Mth.clamp(targetDeltaY*1.025D,-1.0D,1.0D);targetDeltaZ=Mth.clamp(targetDeltaZ*1.025D,-1.0D,1.0D);
        Vec3 movement=getDeltaMovement();setDeltaMovement(movement.add((targetDeltaX-movement.x)*0.2D,(targetDeltaY-movement.y)*0.2D,(targetDeltaZ-movement.z)*0.2D));
    }
    @Unique private void clinging$selectNextBlockMoveDirection(Direction.Axis avoidAxis){
        BlockPos targetPos=clinging$targetBlock;if(targetPos==null)return;
        double targetX=targetPos.getX()+0.5D,targetY=targetPos.getY()+0.5D,targetZ=targetPos.getZ()+0.5D;Direction selection=null;
        if(!targetPos.closerToCenterThan(position(),2.0D)){
            BlockPos current=blockPosition();ArrayList<Direction> options=new ArrayList<>();
            if(avoidAxis!=Direction.Axis.X){if(current.getX()<targetPos.getX()&&level().isEmptyBlock(current.east()))options.add(Direction.EAST);else if(current.getX()>targetPos.getX()&&level().isEmptyBlock(current.west()))options.add(Direction.WEST);}
            if(avoidAxis!=Direction.Axis.Y){if(current.getY()<targetPos.getY()&&level().isEmptyBlock(current.above()))options.add(Direction.UP);else if(current.getY()>targetPos.getY()&&level().isEmptyBlock(current.below()))options.add(Direction.DOWN);}
            if(avoidAxis!=Direction.Axis.Z){if(current.getZ()<targetPos.getZ()&&level().isEmptyBlock(current.south()))options.add(Direction.SOUTH);else if(current.getZ()>targetPos.getZ()&&level().isEmptyBlock(current.north()))options.add(Direction.NORTH);}
            selection=Direction.getRandom(random);
            if(options.isEmpty()){for(int attempts=5;!level().isEmptyBlock(current.relative(selection))&&attempts>0;--attempts)selection=Direction.getRandom(random);}else selection=options.get(random.nextInt(options.size()));
            targetX=getX()+selection.getStepX();targetY=getY()+selection.getStepY();targetZ=getZ()+selection.getStepZ();
        }
        currentMoveDirection=selection;double x=targetX-getX(),y=targetY-getY(),z=targetZ-getZ(),distance=Math.sqrt(x*x+y*y+z*z);
        if(distance==0.0D){targetDeltaX=targetDeltaY=targetDeltaZ=0.0D;}else{targetDeltaX=x/distance*0.15D;targetDeltaY=y/distance*0.15D;targetDeltaZ=z/distance*0.15D;}
        needsSync=true;flightSteps=10+random.nextInt(5)*10;
    }
    @Unique private void clinging$setFreeFlight(Vec3 vector){
        Vec3 safe=ShulkerChargeTargeting.safeIntent(vector);Direction direction=Direction.getApproximateNearest(safe.x,safe.y,safe.z);currentMoveDirection=direction;flightSteps=10;
        Vec3 delta=new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ()).scale(0.15D);targetDeltaX=delta.x;targetDeltaY=delta.y;targetDeltaZ=delta.z;setDeltaMovement(delta);needsSync=true;
    }
}
