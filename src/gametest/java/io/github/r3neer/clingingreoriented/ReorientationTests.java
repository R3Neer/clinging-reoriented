package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;

public final class ReorientationTests {
    @GameTest public void spentChargeSurvivesSaveLoadAndDeathResets(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();ClingingReoriented.data(p).airChangeUsed=true;
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        p.saveWithoutId(output);
        var input=net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult());
        var loaded=h.makeMockServerPlayerInLevel();loaded.load(input);
        h.assertTrue(ClingingReoriented.data(loaded).airChangeUsed,"spent charge persists on disk");
        var respawn=h.makeMockServerPlayerInLevel();respawn.restoreFrom(p,false);
        h.assertFalse(ClingingReoriented.data(respawn).airChangeUsed,"death gives fresh charge");h.succeed();
    }
    @GameTest(padding=24) public void chargeSurvivesEffectRefreshAndFalseGround(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(3,8,3)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-4,-4),p.blockPosition().offset(4,4,4)))h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
        p.addEffect(new MobEffectInstance(clinging,200));
        h.assertTrue(ClingingReoriented.attempt(p,Vec3.ZERO)==ClingingReoriented.Result.AMBIGUOUS,"failure before use");
        h.assertFalse(ClingingReoriented.data(p).airChangeUsed,"failure does not spend");
        var first=ClingingReoriented.attempt(p,new Vec3(1,0,0));
        h.assertTrue(first==ClingingReoriented.Result.SUCCESS,"first air change: "+first);
        p.removeEffect(clinging);p.addEffect(new MobEffectInstance(clinging,200));p.setOnGround(true);
        var refreshed=ClingingReoriented.attempt(p,new Vec3(0,0,1));
        h.assertTrue(refreshed==ClingingReoriented.Result.AIR_CHANGE_USED && ClingingReoriented.data(p).airChangeUsed,"refresh and forged ground do not replenish or become semantic support: result="+refreshed+", used="+ClingingReoriented.data(p).airChangeUsed+", ground="+p.onGround());
        p.setOnGround(false);
        p.removeAllEffects();p.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));
        for(int i=0;i<12;i++)h.assertTrue(ClingingReoriented.attempt(p,i%2==0?new Vec3(0,0,1):new Vec3(0,0,-1))==ClingingReoriented.Result.SUCCESS,"Reorientation alone unlimited");
        p.addEffect(new MobEffectInstance(clinging,200));p.removeEffect(Reorientation.EFFECT);
        h.assertTrue(ClingingReoriented.attempt(p,new Vec3(1,0,0))==ClingingReoriented.Result.AIR_CHANGE_USED,"downgrading midair retains spent charge");
        h.succeed();
    }
    @GameTest(padding=24) public void actualCardinalLandingRestoresCharge(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();var origin=h.absoluteVec(new Vec3(4,8,4));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));
        for(Direction gravity:Direction.values()) {
            p.snapTo(origin);ClingingReoriented.write(p,gravity);
            var s=ClingingReoriented.data(p);s.owned=true;s.selected=gravity;s.airChangeUsed=true;
            var box=p.getBoundingBox();var n=io.github.r3neer.clingingreoriented.geometry.FaceGeometry.vector(gravity);
            var center=box.getCenter();var axis=gravity.getAxis();
            double plane=gravity.getAxisDirection()==Direction.AxisDirection.POSITIVE?box.max(axis):box.min(axis);
            var target=new Vec3(axis==Direction.Axis.X?plane:center.x,axis==Direction.Axis.Y?plane:center.y,axis==Direction.Axis.Z?plane:center.z);
            var block=BlockPos.containing(target.add(n.scale(.01)));
            double face=gravity.getAxisDirection()==Direction.AxisDirection.POSITIVE?block.get(axis):block.get(axis)+1;
            p.setPos(p.position().add(n.scale((face-plane)*gravity.getAxisDirection().getStep())));
            h.getLevel().setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());p.setOnGround(true);p.setDeltaMovement(Vec3.ZERO);
            ClingingReoriented.reconcile(p);
            h.assertFalse(s.airChangeUsed,"real landing replenishes "+gravity);
            s.airChangeUsed=true;p.jumpFromGround();
            h.assertFalse(s.airChangeUsed,"immediate jump replenishes before departure "+gravity);
            h.getLevel().setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
    @GameTest(padding=24) public void spentClingingRejectsEveryVoluntaryTurnIncludingDown(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(4,12,4)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-4,-4),p.blockPosition().offset(4,4,4)))h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
        p.addEffect(new MobEffectInstance(clinging,200));
        ClingingReoriented.write(p,Direction.EAST);
        var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;s.airChangeUsed=true;
        p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);
        var down=ClingingReoriented.attempt(p,new Vec3(0,-1,0));
        h.assertTrue(down==ClingingReoriented.Result.AIR_CHANGE_USED,"spent Clinging rejects voluntary DOWN: "+down);
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.EAST,"rejected DOWN leaves gravity unchanged");
        h.assertTrue(s.airChangeUsed,"rejected turn keeps charge spent");
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));
        var unlimited=ClingingReoriented.attempt(p,new Vec3(0,-1,0));
        h.assertTrue(unlimited==ClingingReoriented.Result.SUCCESS,"Reorientation permits the same DOWN turn: "+unlimited);
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.DOWN,"Reorientation reaches DOWN");
        h.succeed();
    }
    @GameTest(padding=24) public void oldFloorDoesNotFalselyRejectCenteredSidewaysTurn(GameTestHelper h) {
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(5.5,5,5.5)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-3,-4),p.blockPosition().offset(4,5,4)))h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
        p.addEffect(new MobEffectInstance(clinging,200));
        h.getLevel().setBlockAndUpdate(p.blockPosition().below(),Blocks.STONE.defaultBlockState());
        p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);
        var dimensions=p.getDimensions(p.getPose());var before=p.getBoundingBox();var beforeCenter=before.getCenter();
        var direct=com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(dimensions,Direction.EAST,p.position());
        h.assertFalse(h.getLevel().noCollision(p,direct.deflate(1e-7)),"fixture proves old-feet pivot clips the old floor");
        var centeredPosition=com.moigferdsrte.gravitychanger.util.RotationUtil.getCenterAlignedPosition(before,dimensions,Direction.EAST);
        var centeredBox=com.moigferdsrte.gravitychanger.util.RotationUtil.makeBoxFromDimensions(dimensions,Direction.EAST,centeredPosition);
        h.assertTrue(h.getLevel().noCollision(p,centeredBox.deflate(1e-7)),"center-preserving rotation has enough room");
        var result=ClingingReoriented.attempt(p,new Vec3(1,0,0));
        h.assertTrue(result==ClingingReoriented.Result.SUCCESS,"clear centered turn succeeds: "+result);
        h.assertTrue(p.position().distanceToSqr(centeredPosition)<1e-10,"turn uses the validated centered placement");
        h.assertTrue(p.getBoundingBox().getCenter().distanceToSqr(beforeCenter)<1e-10,"physical body center is preserved");
        h.succeed();
    }
    @GameTest public void realBrewingRecipes(GameTestHelper h) {
        var brewing=h.getLevel().potionBrewing();
        for(boolean extended:new boolean[]{false,true})for(var bottle:new Item[]{Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION}) {
            var base=BuiltInRegistries.POTION.get(Identifier.parse(extended?"alexsmobs:long_clinging":"alexsmobs:clinging")).orElseThrow();
            var baseStack=PotionContents.createItemStack(bottle,base);
            var result=brewing.mix(new ItemStack(GravityCharges.ITEM),baseStack);
            var potion=result.get(DataComponents.POTION_CONTENTS).potion().orElseThrow();
            h.assertTrue(potion.equals(extended?Reorientation.LONG_POTION:Reorientation.POTION),"Gravity Charge recipe retains duration and bottle");
            h.assertTrue(result.is(bottle),"bottle unchanged");
            h.assertTrue(potion.value().getEffects().size()==1 && potion.value().getEffects().getFirst().getEffect().equals(Reorientation.EFFECT),"single infusible effect");
            h.assertTrue(potion.value().getEffects().getFirst().getDuration()==(extended?9600:3600),"duration");
            var obsolete=brewing.mix(new ItemStack(Items.SHULKER_SHELL),baseStack.copy());
            h.assertTrue(obsolete.get(DataComponents.POTION_CONTENTS).potion().orElseThrow().equals(base),"Shulker Shell no longer brews Reorientation");
        }
        var normal=PotionContents.createItemStack(Items.POTION,Reorientation.POTION);
        var longer=brewing.mix(new ItemStack(Items.REDSTONE),normal);
        h.assertTrue(longer.get(DataComponents.POTION_CONTENTS).potion().orElseThrow().equals(Reorientation.LONG_POTION),"redstone extends");
        var splash=brewing.mix(new ItemStack(Items.GUNPOWDER),normal);
        h.assertTrue(brewing.mix(new ItemStack(Items.DRAGON_BREATH),splash).is(Items.LINGERING_POTION),"vanilla splash and lingering routes");h.succeed();
    }
}
