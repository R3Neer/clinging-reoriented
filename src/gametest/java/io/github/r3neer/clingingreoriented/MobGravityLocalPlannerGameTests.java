package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.entity.ai.DirectionalGroundNodeEvaluator;
import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** S05-B integration tests for one-path + bounded-transition local planning. */
public final class MobGravityLocalPlannerGameTests {
    @GameTest(padding=48)
    public void reachableSurfaceGoalReturnsWalkWithoutMutatingLiveNavigation(GameTestHelper h){
        Wolf wolf=walkingWolf(h,true,1,20);
        var navigation=wolf.getNavigation();var beforePath=navigation.getPath();var beforeTarget=navigation.getTargetPos();
        Vec3 beforePosition=wolf.position();Direction beforeGravity=GravityDirectionUtil.getOwnGravityDirection(wolf);
        var beforeOwnership=MobGravity.state(wolf).ownership;boolean beforeAirUsed=MobGravity.state(wolf).airUsed;
        Vec3 focus=wolf.position().add(5.0D,0.0D,0.0D);

        var plan=MobGravityLocalPlanner.plan(wolf,distanceGoal(focus,.8D,null),30);
        h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.WALK,"reachable ordinary target should stay WALK, got "+plan.kind());
        h.assertTrue(plan.walkPath()!=null&&plan.walkPath().canReach(),"WALK did not retain its tactical path");
        h.assertTrue(plan.transitionEvaluations()==0,"WALK unnecessarily evaluated gravity transitions");
        h.assertTrue(navigation.getPath()==beforePath,"planning replaced the live navigation path");
        h.assertTrue(equalsNullable(navigation.getTargetPos(),beforeTarget),"planning changed live navigation target metadata");
        h.assertTrue(wolf.position().equals(beforePosition)&&GravityDirectionUtil.getOwnGravityDirection(wolf)==beforeGravity,"planning moved or reoriented the mob");
        h.assertTrue(MobGravity.state(wolf).ownership==beforeOwnership&&MobGravity.state(wolf).airUsed==beforeAirUsed,"planning mutated gravity capability state");
        h.succeed();
    }

    @GameTest(padding=48)
    public void eastGravityWalkUsesDirectionalNodeEntityPosition(GameTestHelper h){
        clear(h);
        for(int y=4;y<=16;y++)for(int z=0;z<=12;z++)h.setBlock(new BlockPos(6,y,z),Blocks.STONE);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.setNoAi(true);wolf.setNoGravity(true);
        GravityDirectionUtil.setGravityDirection(wolf,Direction.EAST);
        Vec3 start=DirectionalGroundNodeEvaluator.entityPosition(h.absolutePos(new BlockPos(5,10,5)),Direction.EAST);
        Vec3 focus=DirectionalGroundNodeEvaluator.entityPosition(h.absolutePos(new BlockPos(5,10,9)),Direction.EAST);
        wolf.setPos(start.x,start.y,start.z);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);

        var plan=MobGravityLocalPlanner.plan(wolf,distanceGoal(focus,.8D,null),20);
        h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.WALK&&plan.walkPath()!=null&&plan.walkPath().canReach(),
            "EAST wall target was not reachable through directional navigation: "+plan.kind());
        Vec3 expected=DirectionalGroundNodeEvaluator.entityPosition(plan.walkPath().getEndNode().asBlockPos(),Direction.EAST);
        Vec3 naive=Vec3.atCenterOf(plan.walkPath().getEndNode().asBlockPos());
        h.assertTrue(plan.frontier().distanceTo(expected)<1.0E-9D,"lateral frontier did not use Gravity Changer entityPosition convention");
        h.assertTrue(plan.frontier().distanceTo(naive)>.4D,"fixture failed to distinguish directional entity anchor from naive block center");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"planning changed EAST logical gravity");
        h.succeed();
    }

    @GameTest(padding=64)
    public void blockedTargetUsesExactPartialPathFrontierAndOnlyFiveForecasts(GameTestHelper h){
        Wolf wolf=walkingWolf(h,false,1,22);
        for(int y=10;y<=14;y++)for(int z=-5;z<=15;z++)h.setBlock(new BlockPos(11,y,z),Blocks.STONE);
        Vec3 focus=wolf.position().add(12.0D,0.0D,0.0D);
        var navigation=wolf.getNavigation();var beforePath=navigation.getPath();var beforeTarget=navigation.getTargetPos();

        var plan=MobGravityLocalPlanner.plan(wolf,distanceGoal(focus,.8D,null),20);
        h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.NO_PLAN,"mob without gravity capability should not invent a blocked route");
        h.assertTrue(plan.walkPath()!=null&&!plan.walkPath().canReach(),"fixture did not produce the required partial path");
        Vec3 expected=DirectionalGroundNodeEvaluator.entityPosition(plan.walkPath().getEndNode().asBlockPos(),Direction.DOWN);
        h.assertTrue(plan.frontier().distanceTo(expected)<1.0E-9D,"planner did not use exact partial-path endpoint as frontier: "+plan.frontier()+" vs "+expected);
        h.assertTrue(plan.transitionEvaluations()==5,"local planner must evaluate exactly five alternative gravities after blocked WALK, got "+plan.transitionEvaluations());
        h.assertTrue(navigation.getPath()==beforePath&&equalsNullable(navigation.getTargetPos(),beforeTarget),"ephemeral path query leaked into live navigation");
        h.succeed();
    }

    @GameTest(padding=48)
    public void futureFrontierCanSelectPhysicalEastTransitionWithoutMovingMob(GameTestHelper h){
        Wolf wolf=walkingWolf(h,true,1,20);Vec3 before=wolf.position();
        Vec3 focus=wolf.position().add(5.0D,0.0D,0.0D);
        var provider=new LandingSurfaceProvider(){
            private LocalContact contact(){return new LocalContact("east-local",1L,new Vec3(-1,0,0));}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(!query.entity().getUUID().equals(wolf.getUUID())||query.gravity()!=Direction.EAST)return Optional.empty();
                return end.getCenter().x>start.getCenter().x+1.0E-8D?Optional.of(new LocalSweep(contact(),.5D,true)):Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){return query.entity().getUUID().equals(wolf.getUUID())&&query.gravity()==Direction.EAST;}
        };
        var registration=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","local_planner_east"),provider);
        try{
            var plan=MobGravityLocalPlanner.plan(wolf,distanceGoal(focus,.8D,Direction.EAST),20);
            h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.TRANSITION,"gravity-specific goal should choose a physical transition, got "+plan.kind());
            h.assertTrue(plan.terminalGravity()==Direction.EAST&&plan.transition()!=null&&plan.transition().targetGravity()==Direction.EAST,
                "planner chose wrong terminal gravity: "+plan.terminalGravity());
            h.assertTrue(plan.walkPath()!=null&&plan.walkPath().canReach(),"planner lost the ordinary approach path to its launch frontier");
            h.assertTrue(plan.transition().launchPosition().distanceTo(plan.frontier())<1.0D,
                "transition was forecast from current mob position instead of future frontier");
            h.assertTrue(plan.transitionEvaluations()==5,"transition comparison exceeded or skipped the bounded five candidates");
            h.assertTrue(wolf.position().equals(before),"local planning physically moved the wolf");
        }finally{registration.close();}
        h.succeed();
    }

    @GameTest(padding=32)
    public void alreadySatisfiedGoalReturnsZeroCostWalkWithoutPathfinding(GameTestHelper h){
        Wolf wolf=walkingWolf(h,false,1,12);Vec3 here=wolf.position();
        var navigation=wolf.getNavigation();var beforeTarget=navigation.getTargetPos();
        var plan=MobGravityLocalPlanner.plan(wolf,distanceGoal(here,.1D,null),20);
        h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.WALK&&plan.walkPath()==null,"already-satisfied state should be zero-step WALK");
        h.assertTrue(plan.totalCost()==0.0D&&plan.transitionEvaluations()==0,"already-satisfied state spent planning budget");
        h.assertTrue(equalsNullable(navigation.getTargetPos(),beforeTarget),"zero-step planning mutated navigation metadata");
        h.succeed();
    }

    private static MobGravityLocalPlanner.Goal distanceGoal(Vec3 focus,double radius,Direction requiredGravity){
        return new MobGravityLocalPlanner.Goal(){
            @Override public Vec3 focus(){return focus;}
            @Override public boolean satisfied(Vec3 position,Direction gravity){
                return (requiredGravity==null||gravity==requiredGravity)&&position.distanceToSqr(focus)<=radius*radius;
            }
            @Override public double heuristic(Vec3 position,Direction gravity){
                double gravityPenalty=requiredGravity==null||gravity==requiredGravity?0.0D:1000.0D;
                return gravityPenalty+position.distanceTo(focus);
            }
        };
    }

    private static Wolf walkingWolf(GameTestHelper h,boolean reorientation,int floorMinX,int floorMaxX){
        clear(h);
        for(int x=floorMinX;x<=floorMaxX;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        if(reorientation)wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,600));
        return wolf;
    }

    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-6,3,-6)),h.absolutePos(new BlockPos(30,18,16))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }

    private static boolean equalsNullable(Object a,Object b){return a==null?b==null:a.equals(b);}
}
