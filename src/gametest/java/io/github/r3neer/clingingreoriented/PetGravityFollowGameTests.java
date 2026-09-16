package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Direct executor and airborne-owner target-filter gates for gravity-aware pet follow. */
public final class PetGravityFollowGameTests {
    @GameTest(padding=64)
    public void blockedOwnerRouteApproachesThenCommitsOnlyAtFrontierAndConfirmsLanding(GameTestHelper h){
        var owner=owner(h,new Vec3(14,10,5));Wolf wolf=wolf(h,owner);
        wall(h,9);
        var state=new PetGravityFollow.State();

        h.assertTrue(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),"blocked owner route did not produce a special transition plan");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.APPROACH,"special wake did not enter APPROACH");
        h.assertTrue(state.plan()!=null&&state.plan().kind()==MobGravityLocalPlanner.Kind.TRANSITION,"wake did not cache a transition plan");
        h.assertTrue(state.plan().terminalGravity()==Direction.EAST,"fixture expected EAST wall transition, got "+state.plan().terminalGravity());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"planning remotely changed pet gravity before reaching frontier");

        Vec3 frontier=state.plan().frontier();
        wolf.setPos(frontier.x,frontier.y,frontier.z);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F),"executor relinquished the tick at commit frontier");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.COMMITTED,"frontier revalidation did not enter COMMITTED: "+state.phase());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"committed executor did not apply EAST gravity");
        h.assertTrue(MobGravity.state(wolf).airUsed,"committed transition did not consume the airborne stage");

        var landing=state.plan().transition().landingBody();
        Vec3 landed=RotationUtil.getCenterAlignedPosition(landing,wolf.getDimensions(wolf.getPose()),Direction.EAST);
        wolf.setPos(landed.x,landed.y,landed.z);wolf.setBoundingBox(landing);wolf.setDeltaMovement(Vec3.ZERO);wolf.setOnGround(true);
        h.assertTrue(PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F),"first landing contact should remain owned for debounce");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.LANDING_CONFIRM,"first support tick did not enter LANDING_CONFIRM");

        owner.snapTo(wolf.position());owner.setOnGround(true);owner.setDeltaMovement(Vec3.ZERO);
        h.assertFalse(PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F),"second stable support should release special ownership when owner is already close");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE,"stable landing did not become a new idle support root");
        h.succeed();
    }

    @GameTest(padding=64)
    public void airborneOwnerBlockedProjectionCanWakeSafeGravityTransition(GameTestHelper h){
        var owner=owner(h,new Vec3(14,13,5));Wolf wolf=wolf(h,owner);wall(h,9);
        owner.setOnGround(false);owner.setDeltaMovement(new Vec3(.2D,0,0));
        var state=new PetGravityFollow.State();

        h.assertTrue(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),
            "airborne owner behind blocked projected route did not wake gravity-aware follow");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.APPROACH||state.phase()==PetGravityFollow.Phase.REVALIDATE,
            "airborne owner woke unexpected phase: "+state.phase());
        h.assertTrue(state.plan()!=null&&state.plan().kind()==MobGravityLocalPlanner.Kind.TRANSITION,
            "airborne owner produced no support-to-support transition");
        h.assertTrue(state.strategicOwnerAirborne(),"airborne owner was not represented as airborne target mode");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,
            "planning toward airborne owner remotely changed pet gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void airborneOwnerJitterDoesNotThrashButMaterialTravelRefreshesAnchor(GameTestHelper h){
        var owner=owner(h,new Vec3(14,13,5));Wolf wolf=wolf(h,owner);wall(h,9);
        owner.setOnGround(false);owner.setDeltaMovement(Vec3.ZERO);
        var state=new PetGravityFollow.State();
        h.assertTrue(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),"fixture produced no airborne follow plan");
        Vec3 firstAnchor=state.strategicOwnerAnchor();var firstPlan=state.plan();

        owner.snapTo(firstAnchor.add(1.0D,0,0));owner.setOnGround(false);
        PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F);
        h.assertTrue(state.strategicOwnerAnchor().distanceToSqr(firstAnchor)<1.0E-8D,
            "one-block airborne jitter moved the strategic anchor");
        h.assertTrue(state.plan()==firstPlan,"sub-threshold owner jitter discarded the current plan");

        Vec3 material=firstAnchor.add(PetFollowTargeting.AIRBORNE_REFRESH_DISTANCE+1.0D,0,0);
        owner.snapTo(material);owner.setOnGround(false);
        PetGravityFollow.tick(wolf,owner,state,1.0D,2.0F);
        h.assertTrue(state.strategicOwnerAnchor().distanceToSqr(material)<1.0E-8D,
            "material airborne travel did not refresh the strategic owner anchor");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,
            "anchor refresh itself committed a remote gravity turn");
        h.succeed();
    }

    @GameTest(padding=48,maxTicks=40)
    public void slowlyDriftingAirborneOwnerCannotLeaveAnchorStaleForever(GameTestHelper h){
        var owner=owner(h,new Vec3(12,14,5));Wolf wolf=wolf(h,owner);
        owner.setOnGround(false);owner.setDeltaMovement(Vec3.ZERO);
        var filter=new PetFollowTargeting.State();
        var first=PetFollowTargeting.observe(wolf,owner,filter);
        h.assertTrue(first!=null&&first.airborne(),"fixture did not enter airborne target mode");
        Vec3 initial=first.anchor();
        owner.snapTo(initial.add(1.5D,0,0));owner.setOnGround(false);
        h.assertTrue(PetFollowTargeting.observe(wolf,owner,filter).anchor().distanceToSqr(initial)<1.0E-8D,
            "small drift refreshed before the hysteresis window elapsed");

        h.runAfterDelay((int)PetFollowTargeting.AIRBORNE_MAX_STALE_TICKS+1,()->{
            var refreshed=PetFollowTargeting.observe(wolf,owner,filter);
            h.assertTrue(refreshed.anchor().distanceToSqr(owner.position())<1.0E-8D,
                "slow airborne drift left the strategic anchor stale beyond its maximum age");
            h.succeed();
        });
    }

    @GameTest(padding=48)
    public void reachableAirborneProjectionDoesNotInventGravityTurn(GameTestHelper h){
        var owner=owner(h,new Vec3(9,20,5));Wolf wolf=wolf(h,owner);
        owner.setOnGround(false);owner.setDeltaMovement(Vec3.ZERO);
        var state=new PetGravityFollow.State();
        h.assertFalse(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),
            "reachable projected airborne owner route incorrectly became a gravity transition");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE&&state.plan()==null,
            "reachable airborne projection left special executor state behind");
        h.assertTrue(state.strategicOwnerAirborne(),"reachable airborne target was not observed");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"projection planning changed gravity");
        h.succeed();
    }

    @GameTest(padding=48)
    public void ordinaryReachableOwnerDoesNotWakeSpecialExecutorInsideVanillaDeadZone(GameTestHelper h){
        var owner=owner(h,new Vec3(9,10,5));Wolf wolf=wolf(h,owner);
        var state=new PetGravityFollow.State();
        h.assertFalse(PetGravityFollow.shouldWake(wolf,owner,state,2.0F),"ordinary same-surface WALK incorrectly became special gravity follow");
        h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE&&state.plan()==null,"ordinary WALK left special executor state behind");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"ordinary planning changed gravity");
        h.succeed();
    }

    private static Wolf wolf(GameTestHelper h,net.minecraft.server.level.ServerPlayer owner){
        clear(h);floor(h);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        return wolf;
    }

    private static net.minecraft.server.level.ServerPlayer owner(GameTestHelper h,Vec3 relative){
        clear(h);floor(h);
        var owner=h.makeMockServerPlayerInLevel();owner.setGameMode(GameType.SURVIVAL);owner.snapTo(h.absoluteVec(relative));
        owner.setNoGravity(true);owner.setOnGround(true);owner.setDeltaMovement(Vec3.ZERO);return owner;
    }

    private static void wall(GameTestHelper h,int x){for(int y=9;y<=16;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void floor(GameTestHelper h){for(int x=0;x<=28;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(32,24,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
