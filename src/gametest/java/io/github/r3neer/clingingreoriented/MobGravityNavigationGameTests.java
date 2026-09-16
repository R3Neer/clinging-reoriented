package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** S06-D gates for the generic PathNavigation intent bridge on a non-pet mob. */
public final class MobGravityNavigationGameTests {
    @GameTest(padding=64)
    public void blockedZombiePositionIntentActivatesGravityPlannerWithoutRemoteTurn(GameTestHelper h){
        Zombie zombie=zombie(h,true);wall(h,9);
        Vec3 target=h.absoluteVec(new Vec3(14,10,5));
        boolean accepted=zombie.getNavigation().moveTo(target.x,target.y,target.z,1.0D);
        var state=MobGravityNavigation.state(zombie);

        h.assertTrue(accepted,"gravity bridge should accept an otherwise unreachable navigation intent");
        h.assertTrue(MobGravityNavigation.active(zombie),"blocked powered zombie did not enter gravity navigation");
        h.assertTrue(state.phase()==MobGravityNavigation.Phase.APPROACH||state.phase()==MobGravityNavigation.Phase.REVALIDATE,
            "gravity bridge entered unexpected phase: "+state.phase());
        h.assertTrue(state.plan()!=null&&state.plan().kind()==MobGravityLocalPlanner.Kind.TRANSITION,
            "blocked zombie did not cache a transition plan");
        h.assertTrue(state.plan().terminalGravity()==Direction.EAST,
            "fixture expected EAST wall transition, got "+state.plan().terminalGravity());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,
            "recording navigation intent remotely changed zombie gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void transientVanillaApproachJumpKeepsGenericPlanUntilGroundedCommit(GameTestHelper h){
        Zombie zombie=zombie(h,true);wall(h,9);Vec3 target=h.absoluteVec(new Vec3(14,10,5));
        h.assertTrue(zombie.getNavigation().moveTo(target.x,target.y,target.z,1.0D),"blocked intent was not accepted");
        var state=MobGravityNavigation.state(zombie);var plan=state.plan();
        h.assertTrue(plan!=null&&state.phase()==MobGravityNavigation.Phase.APPROACH,"fixture did not begin in APPROACH");
        MobGravityNavigation.tick(zombie);

        zombie.setOnGround(false);zombie.setDeltaMovement(new Vec3(0,.2D,0));
        MobGravityNavigation.tick(zombie);
        h.assertTrue(state.phase()==MobGravityNavigation.Phase.APPROACH&&state.plan()==plan,
            "short vanilla navigation jump discarded generic approach state");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,
            "generic approach jump committed a gravity turn while unsupported");

        Vec3 frontier=plan.frontier();
        zombie.setPos(frontier.x,frontier.y,frontier.z);zombie.setOnGround(true);zombie.setDeltaMovement(Vec3.ZERO);
        MobGravityNavigation.tick(zombie);
        h.assertTrue(state.phase()==MobGravityNavigation.Phase.COMMITTED,"generic approach did not commit after support returned: "+state.phase());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.EAST,"generic resumed approach did not execute EAST transition");
        h.succeed();
    }

    @GameTest(padding=64)
    public void fleePositionIntentCanUseWallWhenItImprovesSeparation(GameTestHelper h){
        Zombie zombie=zombie(h,true);wall(h,9);
        Vec3 threat=h.absoluteVec(new Vec3(3,10,5));
        Vec3 escape=h.absoluteVec(new Vec3(14,10,5));
        double before=zombie.position().distanceTo(threat);

        h.assertTrue(MobGravityNavigation.requestPositionAfterVanilla(zombie,escape,1.2D,false),
            "blocked flee intent was not accepted by generic gravity locomotion");
        var state=MobGravityNavigation.state(zombie);var plan=state.plan();
        h.assertTrue(plan!=null&&plan.kind()==MobGravityLocalPlanner.Kind.TRANSITION,
            "flee intent did not obtain a gravity transition");
        h.assertTrue(plan.terminalGravity()==Direction.EAST,
            "fixture expected escape via EAST wall, got "+plan.terminalGravity());
        Vec3 landing=RotationUtil.getCenterAlignedPosition(plan.transition().landingBody(),zombie.getDimensions(zombie.getPose()),plan.terminalGravity());
        h.assertTrue(landing.distanceTo(threat)>before,
            "gravity escape did not improve separation from threat: before="+before+" landing="+landing.distanceTo(threat));
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,
            "planning an escape changed gravity before the launch frontier");
        h.succeed();
    }

    @GameTest(padding=48)
    public void reachableZombiePositionIntentStaysVanilla(GameTestHelper h){
        Zombie zombie=zombie(h,true);Vec3 target=zombie.position().add(4,0,0);
        boolean accepted=zombie.getNavigation().moveTo(target.x,target.y,target.z,1.0D);
        h.assertTrue(accepted,"reachable vanilla navigation unexpectedly failed");
        h.assertFalse(MobGravityNavigation.active(zombie),"reachable path was stolen by gravity navigation");
        h.assertTrue(zombie.getNavigation().getPath()!=null&&zombie.getNavigation().getPath().canReach(),
            "fixture did not produce a reachable vanilla path");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,"reachable path changed gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void effectFreeBlockedZombieDoesNotGainGravityNavigation(GameTestHelper h){
        Zombie zombie=zombie(h,false);wall(h,9);Vec3 target=h.absoluteVec(new Vec3(14,10,5));
        zombie.getNavigation().moveTo(target.x,target.y,target.z,1.0D);
        h.assertFalse(MobGravityNavigation.active(zombie),"effect-free zombie gained gravity navigation");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(zombie)==Direction.DOWN,"effect-free query changed gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void repeatedIntentCannotOverwriteOwnedApproachRoute(GameTestHelper h){
        Zombie zombie=zombie(h,true);wall(h,9);Vec3 target=h.absoluteVec(new Vec3(14,10,5));
        h.assertTrue(zombie.getNavigation().moveTo(target.x,target.y,target.z,1.0D),"first blocked intent was not accepted");
        var state=MobGravityNavigation.state(zombie);var plan=state.plan();
        h.assertTrue(plan!=null,"first intent produced no special plan");
        MobGravityNavigation.tick(zombie);
        var ownedPath=zombie.getNavigation().getPath();
        h.assertTrue(ownedPath!=null,"planner did not author an approach path");

        h.assertTrue(zombie.getNavigation().moveTo(target.x,target.y,target.z,1.2D),"repeated owned intent should report accepted");
        h.assertTrue(state.plan()==plan,"repeated identical intent discarded the active plan");
        h.assertTrue(zombie.getNavigation().getPath()==ownedPath,
            "repeated vanilla request overwrote the planner-owned approach path");
        h.succeed();
    }

    private static Zombie zombie(GameTestHelper h,boolean powered){
        clear(h);floor(h);
        Zombie zombie=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,10,5));
        zombie.setNoAi(true);zombie.setNoGravity(true);zombie.setOnGround(true);zombie.setDeltaMovement(Vec3.ZERO);
        if(powered)zombie.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        return zombie;
    }

    private static void wall(GameTestHelper h,int x){for(int y=9;y<=16;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void floor(GameTestHelper h){for(int x=0;x<=22;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(26,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
