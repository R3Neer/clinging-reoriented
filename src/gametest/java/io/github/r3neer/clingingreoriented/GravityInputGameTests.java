package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.init.ModAttributes;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class GravityInputGameTests {
    @GameTest(padding=16) public void sprintLandingReservationOnlyCatchesNearDescendingContact(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        clear(h,p.blockPosition(),8);
        var floor=h.absolutePos(new BlockPos(4,4,4));h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        p.snapTo(new Vec3(floor.getX()+.5,floor.getY()+1.20,floor.getZ()+.5));p.setOnGround(false);p.setSprinting(true);
        p.setDeltaMovement(0,-.15,0);
        h.assertTrue(Math.abs(GravityInput.landingGraceTicks(p)-1.0D)<1.0E-9D,"normal player keeps exactly one landing-grace tick");
        h.assertTrue(GravityInput.sprintLandingJumpReserved(p),"descending sprint within normal next-tick floor contact is reserved");
        p.setDeltaMovement(0,.15,0);
        h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"ascending sprint is never reserved");
        p.snapTo(new Vec3(floor.getX()+.5,floor.getY()+2.0,floor.getZ()+.5));p.setDeltaMovement(0,-.15,0);
        h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"distant descending sprint remains available to Clinging");
        p.snapTo(new Vec3(floor.getX()+.5,floor.getY()+1.20,floor.getZ()+.5));p.setDeltaMovement(0,-.15,0);p.setSprinting(false);
        h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"non-sprint input is never reserved");
        h.succeed();
    }

    @GameTest(padding=16) public void jumpBoostScalesLandingGraceWithoutBlanketLockout(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();clear(h,p.blockPosition(),8);
        var floor=h.absolutePos(new BlockPos(4,4,4));h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        p.snapTo(new Vec3(floor.getX()+.5,floor.getY()+1.65,floor.getZ()+.5));p.setOnGround(false);p.setSprinting(true);p.setDeltaMovement(0,-.35,0);
        double normal=GravityInput.landingGraceTicks(p);
        h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"normal jump power does not reserve a 0.65-block landing gap at this velocity");

        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,200,0));
        double jumpOne=GravityInput.landingGraceTicks(p);
        p.removeEffect(MobEffects.JUMP_BOOST);p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,200,1));
        double jumpTwo=GravityInput.landingGraceTicks(p);
        h.assertTrue(normal<jumpOne&&jumpOne<jumpTwo,"Jump Boost levels increase landing grace monotonically");
        h.assertTrue(GravityInput.sprintLandingJumpReserved(p),"Jump Boost II extends grace enough to reserve the same approaching landing");

        p.setDeltaMovement(0,.35,0);
        h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"boosted ascending sprint remains available to gravity input");
        p.setDeltaMovement(0,-.05,0);p.snapTo(new Vec3(floor.getX()+.5,floor.getY()+4.0,floor.getZ()+.5));
        h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"boosted player far from support is not blanket-reserved");

        p.removeEffect(MobEffects.JUMP_BOOST);p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,200,40));
        h.assertTrue(Math.abs(GravityInput.landingGraceTicks(p)-GravityInput.MAX_LANDING_GRACE_TICKS)<1.0E-9D,"pathological Jump Boost is capped at three grace ticks");
        h.succeed();
    }

    @GameTest(padding=16) public void sidewaysGravityUsesSameSprintLandingPrediction(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();clear(h,p.blockPosition(),8);
        p.snapTo(h.absoluteVec(new Vec3(5.8,8.5,5.5)));ClingingReoriented.write(p,Direction.EAST);
        var support=BlockPos.containing(p.getBoundingBox().maxX+.20,p.getBoundingBox().getCenter().y,p.getBoundingBox().getCenter().z);
        h.getLevel().setBlockAndUpdate(support,Blocks.STONE.defaultBlockState());
        p.setOnGround(false);p.setSprinting(true);p.setDeltaMovement(.15,0,0);
        h.assertTrue(GravityInput.sprintLandingJumpReserved(p),"EAST gravity predicts support along +X rather than world DOWN");
        double normal=GravityInput.landingGraceTicks(p);
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST,200,1));
        h.assertTrue(GravityInput.landingGraceTicks(p)>normal,"sideways gravity uses the same Jump Boost grace scaling");
        h.succeed();
    }

    @GameTest(padding=16) public void gravityStrengthScalesNextTickLandingPrediction(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();clear(h,p.blockPosition(),8);
        var floor=h.absolutePos(new BlockPos(4,4,4));h.getLevel().setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());
        p.snapTo(new Vec3(floor.getX()+.5,floor.getY()+1.25,floor.getZ()+.5));p.setOnGround(false);p.setSprinting(true);p.setDeltaMovement(0,-.15,0);
        var strength=p.getAttribute(ModAttributes.GRAVITY_STRENGTH);h.assertTrue(strength!=null,"gravity strength attribute present");
        strength.setBaseValue(9.8);h.assertFalse(GravityInput.sprintLandingJumpReserved(p),"default gravity does not reach the 0.25-block gap this tick");
        strength.setBaseValue(15.0);h.assertTrue(GravityInput.sprintLandingJumpReserved(p),"strong gravity extends prediction enough to reserve the landing jump");
        h.succeed();
    }

    private static void clear(GameTestHelper h,BlockPos center,int radius){
        for(var pos:BlockPos.betweenClosed(center.offset(-radius,-radius,-radius),center.offset(radius,radius,radius)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
