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
    @GameTest(padding=40,maxTicks=100) public void actualSplashEffectsExpireOnPassiveAnimals(GameTestHelper h){
        player(h);var animals=new java.util.ArrayList<LivingEntity>();
        for(var type:new EntityType[]{EntityTypes.HORSE,EntityTypes.WOLF,EntityTypes.PIG})for(var effect:new Holder[]{clinging(),Reorientation.EFFECT}){
            var mob=(Mob)h.spawn(type,new BlockPos(4,10,4));mob.setNoAi(true);mob.setNoGravity(true);
            var stack=new ItemStack(Items.SPLASH_POTION);
            stack.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(java.util.Optional.empty(),java.util.Optional.empty(),java.util.List.of(new MobEffectInstance(effect,40)),java.util.Optional.empty()));
            var splash=new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion(h.getLevel(),mob.getX(),mob.getY(),mob.getZ(),stack);
            splash.onHitAsPotion(h.getLevel(),stack,new net.minecraft.world.phys.EntityHitResult(mob));
            h.assertTrue(mob.hasEffect(effect),"real splash delivery to "+type);
            h.assertTrue(MobGravity.turn(mob,Direction.UP,false),"external direction applied");MobGravity.tick(mob);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.UP,"splash retains external direction");animals.add(mob);
        }
        h.runAfterDelay(55,()->{for(var mob:animals)h.assertTrue(!ClingingReoriented.hasEffect(mob) && GravityDirectionUtil.getGravityDirection(mob)==Direction.DOWN,"natural expiry restores DOWN");h.succeed();});
    }
    @GameTest(padding=40) public void breadcrumbQueueBoundExpiryAndDimension(GameTestHelper h) throws Exception {
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        for(int i=0;i<100;i++)GravityBreadcrumbs.record(owner,Direction.EAST);
        var field=GravityBreadcrumbs.class.getDeclaredField("TRAILS");field.setAccessible(true);
        var trails=(java.util.Map<java.util.UUID,java.util.ArrayDeque<GravityBreadcrumbs.Step>>)field.get(null);
        var trail=trails.get(owner.getUUID());h.assertTrue(trail.size()==GravityBreadcrumbs.LIMIT,"trail has a hard bound");
        long sequence=trail.getLast().sequence();trail.clear();
        trail.add(new GravityBreadcrumbs.Step(sequence,owner.level().dimension(),wolf.position(),Direction.EAST,owner.level().getGameTime()-GravityBreadcrumbs.TTL-1));
        GravityBreadcrumbs.follow(wolf);h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"expired step ignored");
        trail.add(new GravityBreadcrumbs.Step(sequence+1,net.minecraft.world.level.Level.NETHER,wolf.position(),Direction.EAST,owner.level().getGameTime()));
        GravityBreadcrumbs.follow(wolf);h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"other dimension ignored");
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
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.EAST,"own Clinging retains mount orientation");
        horse.removeAllEffects();MobGravity.tick(horse);
        h.assertTrue(GravityDirectionUtil.getGravityDirection(horse)==Direction.DOWN,"last own effect removed resets mount");h.succeed();
    }
    @GameTest(padding=32) public void passiveEffectsNeverPickGravity(GameTestHelper h){
        player(h);
        for(var type:new EntityType[]{EntityTypes.HORSE,EntityTypes.WOLF,EntityTypes.PIG}){
            var mob=(Mob)h.spawn(type,new BlockPos(4,10,4));mob.setNoAi(true);
            for(var effect:new Holder[]{clinging(),Reorientation.EFFECT}){
                mob.addEffect(new MobEffectInstance(effect,200));MobGravity.tick(mob);
                h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.DOWN,"effect alone makes no decision");
                MobGravity.turn(mob,Direction.UP,true);MobGravity.tick(mob);
                h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.UP,"passive effect retains external orientation");
                mob.removeAllEffects();MobGravity.tick(mob);
                h.assertTrue(GravityDirectionUtil.getGravityDirection(mob)==Direction.DOWN,"effect removal restores down");
            }
            mob.discard();
        }h.succeed();
    }
    @GameTest(padding=40) public void followerReplaysAtThePlaceAndRespectsItsCharge(GameTestHelper h){
        var owner=player(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(4,10,4));wolf.tame(owner);
        wolf.addEffect(new MobEffectInstance(clinging(),500));wolf.setOnGround(false);
        var place=owner.position();GravityBreadcrumbs.record(owner,Direction.EAST);
        owner.setPos(place.add(11,0,0));wolf.setPos(place.add(-5,0,0));
        var goal=new FollowOwnerGoal(wolf,1,10,2);h.assertTrue(goal.canUse(),"real follow goal selects owner");goal.start();goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.DOWN,"no remote simultaneous rotation");
        wolf.setPos(place);goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.EAST,"running follow goal replays nearby step");
        owner.setPos(place);GravityBreadcrumbs.record(owner,Direction.NORTH);owner.setPos(place.add(11,0,0));goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.EAST,"Clinging pet cannot replay second air turn");
        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.NORTH,"own Reorientation can replay pending step");
        owner.setPos(place);GravityBreadcrumbs.record(owner,Direction.SOUTH);owner.setPos(place.add(11,0,0));wolf.setOrderedToSit(true);goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.NORTH,"sitting blocks replay");
        wolf.setOrderedToSit(false);wolf.teleportTo(place.x,place.y,place.z);goal.tick();
        h.assertTrue(GravityDirectionUtil.getGravityDirection(wolf)==Direction.NORTH,"pet teleport invalidates old trail");
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
