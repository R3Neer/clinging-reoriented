package io.github.r3neer.clingingreoriented.compat;

import io.github.r3neer.clingingreoriented.ClingingReoriented;
import io.github.r3neer.clingingreoriented.Reorientation;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class AlchemicalCompatibilityGameTests {
    @GameTest public void compatibilityResourcesArePackaged(GameTestHelper h){
        var loader=AlchemicalCompatibilityGameTests.class.getClassLoader();
        h.assertTrue(loader.getResource("data/alexsmobs/alchemical_leather/wear_rules/clinging.json")!=null,"Clinging wear rule packaged");
        h.assertTrue(loader.getResource("data/clinging_reoriented/alchemical_leather/effect_slots/reorientation.json")!=null,"Reorientation slot rule packaged");
        h.assertTrue(loader.getResource("data/clinging_reoriented/alchemical_leather/wear_rules/reorientation.json")!=null,"Reorientation wear rule packaged");
        h.succeed();
    }

    @GameTest public void turnEffectRequiresAndPrefersRealActiveEffect(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        h.assertTrue(AlchemicalLeatherCompat.turnEffect(p)==null,"no potion effect produces no semantic owner");
        var clinging=BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();
        p.addEffect(new MobEffectInstance(clinging,200));
        h.assertTrue(AlchemicalLeatherCompat.turnEffect(p).equals(clinging),"Clinging owns a successful turn when it is the active gravity effect");
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));
        h.assertTrue(AlchemicalLeatherCompat.turnEffect(p).equals(Reorientation.EFFECT),"Reorientation takes precedence when both effects are present");
        p.removeAllEffects();
        h.assertTrue(AlchemicalLeatherCompat.turnEffect(p)==null,"removing effects removes semantic ownership immediately");
        h.succeed();
    }

    @GameTest(padding=16) public void controlledFlightExcludesForeignAndPassiveContexts(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(4,10,4)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-4,-4,-4),p.blockPosition().offset(4,4,4)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));
        ClingingReoriented.write(p,Direction.DOWN);
        var state=ClingingReoriented.data(p);
        state.owned=true;
        state.selected=Direction.DOWN;
        state.groundedOnSurface=false;
        p.setOnGround(false);
        p.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(AlchemicalLeatherCompat.controlledFlightEligible(p),"owned airborne Reorientation self-locomotion is eligible");

        state.groundedOnSurface=true;
        h.assertFalse(AlchemicalLeatherCompat.controlledFlightEligible(p),"moving/support surface state is passive transport and cannot accrue continuous wear");
        state.groundedOnSurface=false;

        BlockPos fluidPos=p.blockPosition();
        h.getLevel().setBlockAndUpdate(fluidPos,Blocks.WATER.defaultBlockState());
        h.assertFalse(AlchemicalLeatherCompat.controlledFlightEligible(p),"swimming/fluid context is not Reorientation-controlled airborne flight");
        h.getLevel().setBlockAndUpdate(fluidPos,Blocks.AIR.defaultBlockState());

        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
        h.assertTrue(p.tryToStartFallFlying(),"holdout fixture enters real Elytra flight");
        h.assertFalse(AlchemicalLeatherCompat.controlledFlightEligible(p),"Elytra owns locomotion and suppresses continuous Reorientation wear");
        p.stopFallFlying();
        p.setItemSlot(EquipmentSlot.CHEST,ItemStack.EMPTY);

        p.getAbilities().flying=true;
        h.assertFalse(AlchemicalLeatherCompat.controlledFlightEligible(p),"independent player flight is not Reorientation-controlled work");
        p.getAbilities().flying=false;

        p.removeEffect(Reorientation.EFFECT);
        h.assertFalse(AlchemicalLeatherCompat.controlledFlightEligible(p),"flight without Reorientation cannot accrue Reorientation wear");
        h.succeed();
    }
}
