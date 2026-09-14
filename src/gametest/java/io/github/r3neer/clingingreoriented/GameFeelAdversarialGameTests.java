package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Cross-sprint holdouts for GF-S05. These deliberately combine otherwise-tested subsystems. */
public final class GameFeelAdversarialGameTests {
    private static ServerPlayer managed(GameTestHelper h,Direction gravity){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(8.5,14.0,8.5)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-12,-10,-12),p.blockPosition().offset(12,12,12)))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        ClingingReoriented.write(p,gravity);
        var s=ClingingReoriented.data(p);
        s.owned=true;s.selected=gravity;s.visualFrameOwned=true;
        s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;
        s.airborneTicks=GravityFallState.START_AIRBORNE_TICKS+8;
        p.setOnGround(false);p.setNoGravity(true);
        return p;
    }

    private static Vec3 direction(Direction direction){
        return new Vec3(direction.getStepX(),direction.getStepY(),direction.getStepZ());
    }

    private static LandingSurfaceProvider fixture(ServerPlayer p,AtomicBoolean valid){
        return new LandingSurfaceProvider(){
            private LocalContact contact(Query q){return new LocalContact("s05_surface",17L,FaceGeometry.vector(q.gravity().getOpposite()));}
            @Override public Optional<LocalContact> currentSupport(Query q){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query q,AABB start,AABB end){
                return q.entity().getUUID().equals(p.getUUID())&&valid.get()?Optional.of(new LocalSweep(contact(q),.35D,true)):Optional.empty();
            }
            @Override public boolean revalidate(Query q,LocalContact c){
                return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&c!=null&&"s05_surface".equals(c.localId());
            }
        };
    }

    @GameTest(padding=36)
    public void sixTurnReorientationPreservesMomentumAndAirborneClock(GameTestHelper h){
        var p=managed(h,Direction.DOWN);var s=ClingingReoriented.data(p);
        Vec3 momentum=new Vec3(.37,-.81,.29);p.setDeltaMovement(momentum);p.fallDistance=14.0F;
        int airborne=s.airborneTicks;
        Direction current=Direction.DOWN;
        Direction[] sequence={Direction.EAST,Direction.UP,Direction.NORTH,Direction.DOWN,Direction.WEST,Direction.UP};
        for(Direction target:sequence){
            Vec3 heading=GravityTransition.headingFromYaw(current,p.getYRot());
            var result=ClingingReoriented.attempt(p,direction(target),heading);
            h.assertTrue(result==ClingingReoriented.Result.SUCCESS,"six-turn sequence rejected "+current+" -> "+target+": "+result);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==target,"six-turn sequence selected wrong gravity for "+target);
            h.assertTrue(p.getDeltaMovement().equals(momentum),"gravity turn mutated world momentum at "+target+": "+p.getDeltaMovement());
            h.assertTrue(s.airborneTicks==airborne,"gravity turn segmented sustained-airborne clock at "+target);
            h.assertTrue(p.fallDistance==14.0F,"gravity turn segmented vanilla fall history at "+target);
            h.assertTrue(s.owned&&s.visualFrameOwned&&s.selected==target,"ownership drifted during six-turn sequence at "+target);
            current=target;
        }
        h.succeed();
    }

    @GameTest(padding=24)
    public void committedInputIsDiscardedAndNeverReplayedAfterInvalidation(GameTestHelper h){
        var p=managed(h,Direction.DOWN);var s=ClingingReoriented.data(p);
        s.visualBaseKnown=true;s.visualBaseDirection=Direction.EAST;
        p.setDeltaMovement(new Vec3(0,-.25,0));
        var valid=new AtomicBoolean(true);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","s05_discard"),fixture(p,valid));
        try{
            LandingState.tick(p);
            h.assertTrue(s.landingCommitted,"fixture never entered LANDING_COMMITTED");
            var rejected=ClingingReoriented.attempt(p,direction(Direction.WEST),GravityTransition.headingFromYaw(Direction.DOWN,p.getYRot()));
            h.assertTrue(rejected==ClingingReoriented.Result.LANDING_COMMITTED,"committed input was not discarded at authority boundary: "+rejected);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==Direction.DOWN,"rejected committed input changed gravity immediately");

            valid.set(false);LandingState.tick(p);
            h.assertFalse(s.landingCommitted,"surface invalidation did not cancel commitment");
            // Run the normal ownership/landing machinery again with no second input. A queued request
            // would now reveal itself as a delayed WEST turn.
            ClingingReoriented.reconcile(p);LandingState.tick(p);GravityFallState.tick(p);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==Direction.DOWN,"discarded committed input replayed after cancellation");
            h.assertTrue(s.selected==Direction.DOWN,"discarded committed input mutated selected gravity after cancellation");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=24)
    public void elytraStartingDuringBodyLandingResetsPresentationWithoutChangingGravity(GameTestHelper h){
        var p=managed(h,Direction.WEST);var s=ClingingReoriented.data(p);
        s.gravityFallActive=true;s.gravityFallLanding=true;s.gravityFallLandingGravity=Direction.DOWN;s.gravityFallLandingEtaTicks=2.0D;
        p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.ELYTRA));
        p.setDeltaMovement(new Vec3(.4,-.7,.1));
        Vec3 momentum=p.getDeltaMovement();
        h.assertTrue(p.tryToStartFallFlying(),"usable Elytra did not enter fall_flying from active Gravity Fall");
        h.assertTrue(p.isFallFlying(),"fixture failed to establish actual Elytra state");

        GravityFallState.tick(p);
        h.assertFalse(s.gravityFallActive,"Elytra did not cancel Gravity Fall root ownership");
        h.assertFalse(s.gravityFallLanding,"Elytra left BODY_LANDING active");
        h.assertTrue(s.gravityFallLandingEtaTicks==0.0D,"Elytra left stale body-landing ETA");
        h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==Direction.WEST,"presentation reset changed physical gravity");
        h.assertTrue(p.getDeltaMovement().equals(momentum),"presentation reset changed world momentum");
        h.succeed();
    }
}
