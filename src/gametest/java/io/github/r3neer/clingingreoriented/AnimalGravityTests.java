package io.github.r3neer.clingingreoriented;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.phys.Vec3;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;

public final class AnimalGravityTests {
    @GameTest(padding=40) public void mountedTurnPreflightsPassengerBox(GameTestHelper h){
        var p=player(h);p.getAttribute(Attributes.SCALE).setBaseValue(2);p.refreshDimensions();p.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        var horse=h.spawn(EntityTypes.HORSE,new BlockPos(4,10,4));horse.setNoAi(true);horse.getAttribute(Attributes.SCALE).setBaseValue(2);horse.refreshDimensions();h.assertTrue(p.startRiding(horse,true,true),"mount for passenger preflight");horse.positionRider(p);horse.setOnGround(false);
        var direction=Direction.EAST;
        var rootBox=com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(horse.getDimensions(horse.getPose()),direction,horse.position());
        var offset=horse.getPassengerRidingPosition(p).subtract(horse.position()).subtract(p.getVehicleAttachmentPoint(horse));
        var target=horse.position().add(com.moigferdsrte.gravitychanger.util.RotationUtil.vecPlayerToWorld(offset,direction));
        var passengerBox=com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),direction,target);
        BlockPos obstacle=null;
        for(var b:BlockPos.betweenClosed(BlockPos.containing(passengerBox.minX,passengerBox.minY,passengerBox.minZ),BlockPos.containing(passengerBox.maxX,passengerBox.maxY,passengerBox.maxZ))){
            var block=new net.minecraft.world.phys.AABB(b);
            if(block.intersects(passengerBox.deflate(.01))&&!block.intersects(rootBox)){obstacle=b.immutable();break;}
        }
        h.assertTrue(obstacle!=null,"fixture isolates passenger-only obstruction root="+rootBox+", passenger="+passengerBox);h.getLevel().setBlockAndUpdate(obstacle,Blocks.STONE.defaultBlockState());
        var before=horse.position();var riderBefore=p.position();horse.setDeltaMovement(.2,.1,.3);var momentum=horse.getDeltaMovement();
        h.assertTrue(ClingingReoriented.attempt(p,new Vec3(1,0,0))==ClingingReoriented.Result.NO_SPACE,"passenger obstruction rejects turn");
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.DOWN && before.equals(horse.position()) && riderBefore.equals(p.position()) && momentum.equals(horse.getDeltaMovement()),"failed mounted turn is atomic");h.succeed();
    }
    @GameTest(padding=40,maxTicks=100) public void actualSplashEffectsDoNotClaimExternalGravity(GameTestHelper h){
        player(h);var animals=new java.util.ArrayList<LivingEntity>();
        for(var type:new EntityType[]{EntityTypes.HORSE,EntityTypes.WOLF,EntityTypes.PIG})for(var effect:new Holder[]{clinging(),Reorientation.EFFECT}){
            var mob=(Mob)h.spawn(type,new BlockPos(4,10,4));mob.setNoAi(true);mob.setNoGravity(true);
            var stack=new ItemStack(Items.SPLASH_POTION);
            stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(),java.util.Optional.empty(),java.util.List.of(new MobEffectInstance(effect,40)),java.util.Optional.empty()));
            var splash=new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion(h.getLevel(),mob.getX(),mob.getY(),mob.getZ(),stack);
            splash.onHitAsPotion(h.getLevel(),stack,new net.minecraft.world.phys.EntityHitResult(mob));
            h.assertTrue(mob.hasEffect(effect),"real splash delivery to "+type);
            GravityDirectionUtil.setGravityDirection(mob,Direction.UP);MobGravity.tick(mob);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.UP,"passive effect does not overwrite external direction");
            h.assertTrue(MobGravity.state(mob).ownership==MobGravity.Ownership.EXTERNAL,"external write is explicitly owned elsewhere");animals.add(mob);
        }
        h.runAfterDelay(55,()->{for(var mob:animals)h.assertTrue(!ClingingReoriented.hasEffect(mob) && GravityDirectionUtil.getGravityDirection(mob)==Direction.UP,"natural expiry preserves external gravity");h.succeed();});
    }
    @GameTest(padding=40) public void breadcrumbQueueBoundExpiryAndDimension(GameTestHelper h) throws Exception {
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        for(int i=0;i<100;i++)GravityBreadcrumbs.record(owner,Direction.EAST);
        var field=GravityBreadcrumbs.class.getDeclaredField("TRAILS");field.setAccessible(true);
        var trails=(java.util.Map<java.util.UUID,java.util.ArrayDeque<GravityBreadcrumbs.Step>>)field.get(null);
        var trail=trails.get(owner.getUUID());h.assertTrue(trail.size()==GravityBreadcrumbs.LIMIT,"trail has a hard bound");
        long sequence=trail.getLast().sequence();trail.clear();
        trail.add(new GravityBreadcrumbs.Step(sequence,owner.level().dimension(),wolf.position(),Direction.EAST,owner.level().getGameTime()-GravityBreadcrumbs.TTL-1));
        GravityBreadcrumbs.follow(wolf,1,2);h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"expired step ignored");
        trail.add(new GravityBreadcrumbs.Step(sequence+1,net.minecraft.world.level.Level.NETHER,wolf.position(),Direction.EAST,owner.level().getGameTime()));
        GravityBreadcrumbs.follow(wolf,1,2);h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"other dimension ignored");
        GravityBreadcrumbs.clear(owner.getUUID());h.assertFalse(trails.containsKey(owner.getUUID()),"owner cleanup removes storage");h.succeed();
    }
    private static Holder<MobEffect> clinging(){return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();}
    private static ServerPlayer player(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(4,10,4)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-8,-7,-8),p.blockPosition().offset(20,12,20)))h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();return p;
    }
    @GameTest(padding=40) public void mountingTransfersOnlyReorientationAndExpires(GameTestHelper h){
        var p=player(h);var horse=h.spawn(EntityTypes.HORSE,new BlockPos(4,10,4));horse.setNoAi(true);
        p.addEffect(new MobEffectInstance(clinging(),300));ClingingReoriented.write(p,Direction.EAST);
        h.assertTrue(p.startRiding(horse,true,true),"mount with Clinging");
        h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==Direction.DOWN && GravityDirectionUtil.getGravityDirection(horse)==Direction.DOWN,"Clinging passenger adopts mount frame");
        h.assertTrue(ClingingReoriented.attempt(p,new Vec3(0,0,1))==ClingingReoriented.Result.BLOCKED,"mounted Clinging cannot shift");
        p.stopRiding();p.addEffect(new MobEffectInstance(Reorientation.EFFECT,300));
        h.assertTrue(p.startRiding(horse,true,true),"mount with Reorientation");
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.EAST,"Reorientation transfers pre-mount gravity");
        horse.setDeltaMovement(.3,.1,-.2);horse.setOnGround(false);var velocity=horse.getDeltaMovement();
        h.assertTrue(ClingingReoriented.attempt(p,new Vec3(0,0,1))==ClingingReoriented.Result.SUCCESS,"first airborne mounted Space turns without prior press");
        h.assertTrue(horse.getDeltaMovement().equals(velocity) && GravityDirectionUtil.getGravityDirection(p)==Direction.SOUTH,"mount momentum and hierarchy");
        h.assertTrue(ClingingReoriented.attempt(p,new Vec3(0,0,-1))==ClingingReoriented.Result.SUCCESS,"mounted Reorientation repeats");
        p.stopRiding();MobGravity.tick(horse);
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.DOWN,"unpowered mount resets after dismount");
        ClingingReoriented.write(p,Direction.EAST);horse.addEffect(new MobEffectInstance(clinging(),300));p.startRiding(horse,true,true);p.stopRiding();MobGravity.tick(horse);
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.EAST,"own Clinging adopts retained mount orientation");
        horse.removeAllEffects();MobGravity.tick(horse);
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.DOWN,"last own effect removed resets mount");h.succeed();
    }
    @GameTest(padding=32) public void passiveEffectsNeverClaimExternalGravity(GameTestHelper h){
        player(h);
        for(var type:new EntityType[]{EntityTypes.HORSE,EntityTypes.WOLF,EntityTypes.PIG}){
            var mob=(Mob)h.spawn(type,new BlockPos(4,10,4));mob.setNoAi(true);
            for(var effect:new Holder[]{clinging(),Reorientation.EFFECT}){
                mob.addEffect(new MobEffectInstance(effect,200));MobGravity.tick(mob);
                h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.DOWN,"effect alone makes no direction decision");
                GravityDirectionUtil.setGravityDirection(mob,Direction.UP);MobGravity.tick(mob);
                h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.UP,"passive effect retains external orientation");
                h.assertTrue(MobGravity.state(mob).ownership==MobGravity.Ownership.EXTERNAL,"external direction is not claimed by effect");
                mob.removeAllEffects();MobGravity.tick(mob);
                h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.UP,"effect removal cannot reset external gravity");
                GravityDirectionUtil.setGravityDirection(mob,Direction.DOWN);MobGravity.tick(mob);
            }
            mob.discard();
        }h.succeed();
    }
    @GameTest(padding=32) public void ownedPetGravityRetiresWhenEffectEnds(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.setNoAi(true);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));wolf.setOnGround(false);
        h.assertTrue(MobGravity.replay(wolf,Direction.EAST),"breadcrumb-style replay acquires gravity ownership");
        h.assertTrue(MobGravity.state(wolf).ownership==MobGravity.Ownership.OWNED_EFFECT && GravityDirectionUtil.getGravityDirection(wolf)==Direction.EAST,"owned frame recorded");
        wolf.removeAllEffects();MobGravity.tick(wolf);
        h.assertTrue(MobGravity.state(wolf).ownership==MobGravity.Ownership.NONE && GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"owned effect frame retires to DOWN");h.succeed();
    }
    @GameTest(padding=40) public void petReplayRespectsClingingCharge(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);
        wolf.addEffect(new MobEffectInstance(clinging(),500));wolf.setOnGround(false);
        h.assertTrue(MobGravity.replay(wolf,Direction.EAST),"first airborne Clinging replay succeeds");
        h.assertFalse(MobGravity.replay(wolf,Direction.NORTH),"Clinging pet cannot replay a second airborne turn");
        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        h.assertTrue(MobGravity.replay(wolf,Direction.NORTH),"own Reorientation permits the pending airborne turn");h.succeed();
    }
    @GameTest(padding=40) public void followerTargetsTheProjectionOfAnAirborneNearbyBreadcrumb(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        for(int x=3;x<=10;x++)for(int z=3;z<=5;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,9,z)),Blocks.STONE.defaultBlockState());
        wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        Vec3 step=wolf.position().add(4,8,0);owner.setPos(step);GravityBreadcrumbs.record(owner,Direction.EAST);
        owner.setPos(step.add(0,0,4));GravityBreadcrumbs.record(owner,Direction.NORTH);
        h.assertTrue(wolf.distanceToSqr(owner)<100,"fixture keeps owner inside vanilla follow start radius");
        var goal=new FollowOwnerGoal(wolf,1,10,2);h.assertTrue(goal.canUse(),"breadcrumb activates follow goal inside vanilla start radius");goal.start();goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"pet does not rotate remotely before reaching projection");
        var target=wolf.getNavigation().getTargetPos();
        h.assertTrue(target!=null&&Math.abs(target.getX()-step.x)<2&&Math.abs(target.getZ()-step.z)<2,"navigation targets airborne breadcrumb projection: "+target);
        wolf.setPos(step.x,wolf.getY(),step.z);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.EAST,"grounded pet replays airborne breadcrumb at its movement-plane projection");
        h.assertFalse(AirChanges.grounded(wolf),"successful replay releases navigation into ballistic fall");
        h.assertTrue(GravityBreadcrumbs.hasPending(wolf),"internal center-aligned replay preserves the next breadcrumb");
        h.assertFalse(goal.canContinueToUse(),"unsupported pet releases FollowOwnerGoal while physics owns the fall");
        goal.stop();GravityBreadcrumbs.clear(owner.getUUID());h.succeed();
    }
    @GameTest(padding=32) public void followGoalRefreshesNavigationAfterGravityChanges(GameTestHelper h) throws Exception {
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));wolf.setNoGravity(true);wolf.setOnGround(false);
        var goal=new FollowOwnerGoal(wolf,1,10,2);var before=wolf.getNavigation();
        h.assertTrue(MobGravity.replay(wolf,Direction.EAST),"fixture reorients pet");wolf.tick();
        var directional=wolf.getNavigation();h.assertTrue(directional!=before,"Gravity Changer replaces navigation after gravity change");
        goal.stop();var field=FollowOwnerGoal.class.getDeclaredField("navigation");field.setAccessible(true);
        h.assertTrue(field.get(goal)==directional,"FollowOwnerGoal refreshes its cached navigation before lifecycle use");h.succeed();
    }
    @GameTest(padding=40,maxTicks=120) public void realWolfAiWalksToAirborneBreadcrumbAndReorients(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        for(int x=3;x<=10;x++)for(int z=3;z<=5;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,9,z)),Blocks.STONE.defaultBlockState());
        wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        Vec3 step=wolf.position().add(4,8,0);owner.setPos(step);owner.setNoGravity(true);GravityBreadcrumbs.record(owner,Direction.EAST);
        h.assertTrue(wolf.distanceToSqr(owner)<100,"fixture keeps owner inside vanilla follow start radius");
        h.runAfterDelay(80,()->{
            h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.EAST,
                "real wolf AI did not walk to the projected airborne breadcrumb and replay it; pos="+wolf.position()
                    +", target="+wolf.getNavigation().getTargetPos()+", done="+wolf.getNavigation().isDone()
                    +", pending="+GravityBreadcrumbs.hasPending(wolf)+", grounded="+AirChanges.grounded(wolf));
            GravityBreadcrumbs.clear(owner.getUUID());h.succeed();
        });
    }
    @GameTest(padding=32) public void effectFreePetKeepsVanillaDeadZoneAndExternalTeleportForgetsRoute(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);
        h.getLevel().setBlockAndUpdate(wolf.blockPosition().below(),Blocks.STONE.defaultBlockState());wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        Vec3 step=wolf.position().add(3,8,0);owner.setPos(step);owner.setNoGravity(true);GravityBreadcrumbs.record(owner,Direction.EAST);
        var vanillaGoal=new FollowOwnerGoal(wolf,1,10,2);
        h.assertFalse(vanillaGoal.canUse(),"effect-free pet must keep vanilla's ten-block follow dead zone");
        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        h.assertTrue(vanillaGoal.canUse(),"effect grants access to pending breadcrumb");vanillaGoal.start();vanillaGoal.tick();
        h.assertTrue(MobGravity.state(wolf).breadcrumbRouteOwned,"breadcrumb pursuit owns the active route before teleport");
        wolf.teleportTo(wolf.getX()+1,wolf.getY(),wolf.getZ());
        h.assertFalse(GravityBreadcrumbs.hasPending(wolf),"external pet teleport invalidates existing breadcrumbs");
        h.assertFalse(MobGravity.state(wolf).breadcrumbRouteOwned,"external pet teleport releases breadcrumb route ownership");
        h.assertTrue(wolf.getNavigation().isDone(),"external pet teleport stops the breadcrumb-authored path");
        vanillaGoal.stop();
        GravityBreadcrumbs.clear(owner.getUUID());h.succeed();
    }
    @GameTest(padding=32) public void sittingPetReleasesAndLaterResumesBreadcrumbRoute(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        for(int x=3;x<=9;x++)for(int z=3;z<=5;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,9,z)),Blocks.STONE.defaultBlockState());
        wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        owner.setPos(wolf.position().add(4,8,0));owner.setNoGravity(true);GravityBreadcrumbs.record(owner,Direction.EAST);
        var goal=new FollowOwnerGoal(wolf,1,10,2);h.assertTrue(goal.canUse(),"standing pet acquires breadcrumb route");goal.start();goal.tick();
        wolf.setOrderedToSit(true);h.assertFalse(goal.canContinueToUse(),"sitting pet releases active breadcrumb goal");goal.stop();
        h.assertTrue(wolf.getNavigation().isDone(),"stopping the sitting pet clears its breadcrumb path");
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"sitting does not remotely replay the turn");
        wolf.setOrderedToSit(false);h.assertTrue(goal.canUse(),"standing again resumes the still-pending breadcrumb");
        goal.stop();GravityBreadcrumbs.clear(owner.getUUID());h.succeed();
    }
    @GameTest public void beaconSecondTierOnly(GameTestHelper h){
        var effect=clinging();
        h.assertTrue(BeaconBlockEntity.BEACON_EFFECTS.get(1).contains(effect),"Clinging in index one");
        h.assertFalse(BeaconBlockEntity.validateEffects(effect,null,1),"tier one rejects");
        h.assertTrue(BeaconBlockEntity.validateEffects(effect,null,2),"tier two accepts");
        h.assertFalse(BeaconBlockEntity.validateEffects(Reorientation.EFFECT,null,4),"Reorientation excluded even at tier four");
        h.assertTrue(io.github.r3neer.clingingreoriented.mixin.BeaconAccess.clinging$valid().contains(effect),"save/load whitelist includes Clinging");
        var menu=new net.minecraft.world.inventory.BeaconMenu(1,h.makeMockServerPlayerInLevel().getInventory());
        menu.setData(0,2);menu.slots.get(0).set(new ItemStack(Items.EMERALD));
        h.assertTrue(menu.updateEffects(java.util.Optional.of(effect),java.util.Optional.empty()) && effect.equals(menu.getPrimaryEffect()) && !menu.hasPayment(),"server menu accepts tier 2 power and consumes payment");h.succeed();
    }
    @GameTest(padding=32) public void elytraHasPriorityInEveryFrame(GameTestHelper h){
        var p=player(h);p.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
        for(var direction:Direction.values()){
            ClingingReoriented.write(p,direction);var s=ClingingReoriented.data(p);s.owned=true;s.selected=direction;p.setOnGround(false);
            h.assertTrue(GravityInput.elytraWins(p),"usable glider wins "+direction);
            h.assertTrue(ClingingReoriented.attempt(p,new Vec3(1,0,0))==ClingingReoriented.Result.BLOCKED,"cannot steal deployment "+direction);
            h.assertTrue(p.tryToStartFallFlying(),"actual glide starts "+direction);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==direction,"glide keeps frame");
            p.stopFallFlying();
        }h.succeed();
    }
}
