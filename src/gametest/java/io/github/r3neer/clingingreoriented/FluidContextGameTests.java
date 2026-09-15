package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Fluids are interaction contexts, never landing/support surfaces for Clinging presentation. */
public final class FluidContextGameTests {
    private static void assertFluidFence(GameTestHelper h,Block fluid,String label){
        BlockPos fluidPos=h.absolutePos(new BlockPos(4,4,4));
        h.setBlock(new BlockPos(4,3,4),Blocks.STONE);
        h.setBlock(new BlockPos(4,4,4),fluid);

        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(new Vec3(fluidPos.getX()+.5D,fluidPos.getY(),fluidPos.getZ()+.5D));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        var state=ClingingReoriented.data(p);
        state.owned=true;state.selected=Direction.DOWN;state.visualFrameOwned=true;
        state.airborneTicks=37;state.visualBaseKnown=true;state.visualBaseDirection=Direction.UP;
        state.landingCommitted=true;state.landingGravity=Direction.DOWN;state.landingEtaTicks=2.0D;
        state.gravityFallActive=true;state.gravityFallLanding=true;state.gravityFallLandingEtaTicks=2.0D;
        // This HOLD originated before fluid entry. Crossing the fluid boundary must still retire it.
        state.freeFlightVisualHeld=true;state.freeFlightVisualHeldInFluid=false;
        p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);

        h.assertTrue(FluidContext.intersects(p),label+" fixture did not intersect a fluid volume");
        h.assertFalse(AirChanges.grounded(p),label+" let the solid floor under fluid count as grounded support");

        LandingState.tick(p);
        h.assertFalse(state.landingCommitted,label+" did not release committed landing presentation");
        h.assertFalse(state.visualBaseKnown,label+" retained a solid-surface visual base while immersed");
        h.assertFalse(state.freeFlightVisualHeld,label+" retained a pre-fluid camera HOLD across fluid entry");
        h.assertFalse(state.freeFlightVisualHeldInFluid,label+" retained stale fluid-HOLD provenance after transfer clear");

        GravityFallState.tick(p);
        h.assertFalse(state.gravityFallActive||state.gravityFallLanding,label+" did not retire Gravity Fall on fluid entry");
    }

    private static void assertFluidOriginHoldSurvives(GameTestHelper h,Block fluid,String label){
        BlockPos fluidPos=h.absolutePos(new BlockPos(4,4,4));
        h.setBlock(new BlockPos(4,4,4),fluid);

        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(new Vec3(fluidPos.getX()+.5D,fluidPos.getY(),fluidPos.getZ()+.5D));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        var state=ClingingReoriented.data(p);
        state.owned=true;state.selected=Direction.DOWN;state.visualFrameOwned=true;
        state.airborneTicks=23;state.visualBaseKnown=true;state.visualBaseDirection=Direction.UP;
        state.freeFlightVisualHeld=true;state.freeFlightVisualHeldInFluid=true;
        p.setDeltaMovement(Vec3.ZERO);p.setOnGround(false);

        h.assertTrue(FluidContext.intersects(p),label+" fluid-origin HOLD fixture did not intersect fluid");
        h.assertTrue(ClingingReoriented.controlsPhysics(p),label+" fluid-origin HOLD fixture lost physics ownership");

        LandingState.tick(p);
        h.assertTrue(state.freeFlightVisualHeld,label+" cancelled a HOLD created inside the fluid context");
        h.assertTrue(state.freeFlightVisualHeldInFluid,label+" lost provenance for a live fluid-origin HOLD");
        h.assertFalse(state.landingCommitted,label+" retained solid landing state inside fluid");
        h.assertFalse(state.visualBaseKnown,label+" retained a solid visual base inside fluid");
        h.assertTrue(state.airborneTicks==0,label+" kept dry-air landing clock alive inside fluid");
    }

    @GameTest
    public void waterNeverCountsAsLandingSupport(GameTestHelper h){assertFluidFence(h,Blocks.WATER,"water");h.succeed();}

    @GameTest
    public void lavaNeverCountsAsLandingSupport(GameTestHelper h){assertFluidFence(h,Blocks.LAVA,"lava");h.succeed();}

    @GameTest
    public void waterTurnHoldSurvivesWaterContext(GameTestHelper h){assertFluidOriginHoldSurvives(h,Blocks.WATER,"water");h.succeed();}

    @GameTest
    public void lavaTurnHoldSurvivesLavaContext(GameTestHelper h){assertFluidOriginHoldSurvives(h,Blocks.LAVA,"lava");h.succeed();}
}
