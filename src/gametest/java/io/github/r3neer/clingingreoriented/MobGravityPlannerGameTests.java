package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** S05-A integration tests for the non-mutating physical transition evaluator. */
public final class MobGravityPlannerGameTests {
    @GameTest(padding=48)
    public void eastWallIsARealSupportTransitionAndEvaluationDoesNotMutate(GameTestHelper h){
        Wolf wolf=wolf(h,true);
        for(int y=5;y<=15;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(13,y,z),Blocks.STONE);
        Vec3 beforePosition=wolf.position();Vec3 beforeVelocity=wolf.getDeltaMovement();
        Direction beforeGravity=GravityDirectionUtil.getOwnGravityDirection(wolf);
        var beforeOwnership=MobGravity.state(wolf).ownership;boolean beforeAirUsed=MobGravity.state(wolf).airUsed;

        var evaluation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,40);
        h.assertTrue(evaluation.accepted(),"real EAST wall should produce an accepted transition, rejection="+evaluation.rejection());
        var transition=evaluation.transition();
        h.assertTrue(transition.targetGravity()==Direction.EAST,"accepted transition lost target gravity");
        h.assertTrue(transition.landingContact().gravity()==Direction.EAST,"landing contact is not frame-bound to EAST");
        h.assertTrue(transition.etaTicks()>0.0D&&transition.etaTicks()<=40.0D,"landing ETA outside bounded horizon");
        h.assertTrue(transition.egressDirections()>0&&transition.robustness()>0.0D,"wall landing has no usable tangent exit");
        h.assertTrue(Double.isFinite(transition.physicalCost()),"physical cost is not finite");

        h.assertTrue(wolf.position().equals(beforePosition),"evaluation moved the mob");
        h.assertTrue(wolf.getDeltaMovement().equals(beforeVelocity),"evaluation changed velocity");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==beforeGravity,"evaluation changed gravity");
        h.assertTrue(MobGravity.state(wolf).ownership==beforeOwnership,"evaluation changed ownership");
        h.assertTrue(MobGravity.state(wolf).airUsed==beforeAirUsed,"evaluation consumed an air change");
        h.succeed();
    }

    @GameTest(padding=48)
    public void groundedFutureLaunchUsesFutureSupportWithoutMutating(GameTestHelper h){
        Wolf wolf=wolf(h,true);
        Vec3 future=wolf.position().add(3.0D,0.0D,0.0D);
        BlockPos feet=BlockPos.containing(future);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)h.getLevel().setBlockAndUpdate(feet.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        int wallX=feet.getX()+6;
        for(int y=feet.getY()-4;y<=feet.getY()+5;y++)for(int z=feet.getZ()-5;z<=feet.getZ()+5;z++)
            h.getLevel().setBlockAndUpdate(new BlockPos(wallX,y,z),Blocks.STONE.defaultBlockState());

        Vec3 beforePosition=wolf.position(),beforeVelocity=wolf.getDeltaMovement();
        Direction beforeGravity=GravityDirectionUtil.getOwnGravityDirection(wolf);
        var beforeOwnership=MobGravity.state(wolf).ownership;boolean beforeAirUsed=MobGravity.state(wolf).airUsed;
        var evaluation=MobGravityPlanner.evaluateGroundedLaunch(wolf,future,Direction.EAST,40);

        h.assertTrue(evaluation.accepted(),"future supported launch should reach the EAST wall, rejection="+evaluation.rejection());
        h.assertTrue(evaluation.transition().launchPosition().distanceTo(future)<1.0D,
            "hypothetical launch did not originate at the future support: "+evaluation.transition().launchPosition()+" vs "+future);
        h.assertTrue(wolf.position().equals(beforePosition)&&wolf.getDeltaMovement().equals(beforeVelocity),"future launch query mutated motion");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==beforeGravity&&MobGravity.state(wolf).ownership==beforeOwnership,
            "future launch query mutated gravity ownership");
        h.assertTrue(MobGravity.state(wolf).airUsed==beforeAirUsed,"future grounded launch query consumed capacity");
        h.succeed();
    }

    @GameTest(padding=32)
    public void groundedFutureLaunchWithoutSupportFailsClosed(GameTestHelper h){
        Wolf wolf=wolf(h,true);Vec3 unsupported=wolf.position().add(4.0D,0.0D,0.0D);
        Vec3 before=wolf.position();
        var evaluation=MobGravityPlanner.evaluateGroundedLaunch(wolf,unsupported,Direction.EAST,20);
        h.assertTrue(!evaluation.accepted()&&evaluation.rejection()==MobGravityPlanner.Rejection.STALE_CONTACT,
            "unsupported future frontier should fail closed, got "+evaluation.rejection());
        h.assertTrue(wolf.position().equals(before),"rejected future support query moved the mob");
        h.succeed();
    }

    @GameTest(padding=32)
    public void firstNonSupportContactRejectsTheManeuver(GameTestHelper h){
        Wolf wolf=wolf(h,true);
        var provider=new LandingSurfaceProvider(){
            private LocalContact contact(){return new LocalContact("blocker",1L,new Vec3(0,0,1));}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                return query.entity().getUUID().equals(wolf.getUUID())?Optional.of(new LocalSweep(contact(),.25D,false)):Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){return true;}
        };
        var registration=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","planner_blocker"),provider);
        try{
            var evaluation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,20);
            h.assertTrue(!evaluation.accepted()&&evaluation.rejection()==MobGravityPlanner.Rejection.BLOCKING_CONTACT,
                "first non-support contact must veto later landing, got "+evaluation.rejection());
        }finally{registration.close();}
        h.succeed();
    }

    @GameTest(padding=32)
    public void supportWithoutAnyStableTangentExitIsRejectedAsTrapped(GameTestHelper h){
        Wolf wolf=wolf(h,true);
        AABB eastBody=RotationUtil.makeBoxFromDimensions(wolf.getDimensions(wolf.getPose()),Direction.EAST,wolf.position()).deflate(1.0E-7D);
        Vec3 mainCenter=eastBody.getCenter();
        var provider=new LandingSurfaceProvider(){
            private LocalContact contact(){return new LocalContact("trap",1L,new Vec3(-1,0,0));}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(!query.entity().getUUID().equals(wolf.getUUID()))return Optional.empty();
                Vec3 a=start.getCenter(),b=end.getCenter();
                boolean mainLane=Math.abs(a.y-mainCenter.y)<1.0E-4D&&Math.abs(a.z-mainCenter.z)<1.0E-4D;
                boolean movingEast=b.x>a.x+1.0E-8D;
                return mainLane&&movingEast?Optional.of(new LocalSweep(contact(),.25D,true)):Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){return query.entity().getUUID().equals(wolf.getUUID());}
        };
        var registration=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","planner_trap"),provider);
        try{
            var evaluation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,20);
            h.assertTrue(!evaluation.accepted()&&evaluation.rejection()==MobGravityPlanner.Rejection.TRAPPED_LANDING,
                "support with zero stable tangent exits was accepted: "+evaluation.rejection());
        }finally{registration.close();}
        h.succeed();
    }

    @GameTest(padding=32)
    public void spentClingingCannotInventASecondAirTurnButReorientationCan(GameTestHelper h){
        Wolf wolf=wolf(h,false);wolf.addEffect(new MobEffectInstance(clinging(),600));wolf.setOnGround(false);
        MobGravity.state(wolf).airUsed=true;
        var spent=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,5);
        h.assertTrue(spent.rejection()==MobGravityPlanner.Rejection.CAPABILITY_SPENT,"spent Clinging was allowed to plan another air turn: "+spent.rejection());

        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,600));
        var reorientation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,5);
        h.assertTrue(reorientation.rejection()!=MobGravityPlanner.Rejection.CAPABILITY_SPENT,"Reorientation was incorrectly reduced to Clinging semantics");
        h.succeed();
    }

    @GameTest(padding=32)
    public void externalGravityOwnershipIsNeverClaimedByPlanner(GameTestHelper h){
        Wolf wolf=wolf(h,true);
        GravityDirectionUtil.setGravityDirection(wolf,Direction.UP);
        var state=MobGravity.state(wolf);state.ownership=MobGravity.Ownership.EXTERNAL;state.ownedDirection=Direction.UP;
        var evaluation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,10);
        h.assertTrue(!evaluation.accepted()&&evaluation.rejection()==MobGravityPlanner.Rejection.FOREIGN_GRAVITY,
            "planner tried to steal external gravity ownership: "+evaluation.rejection());
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.UP,"rejected planning mutated external gravity");
        h.succeed();
    }

    @GameTest(padding=32)
    public void openAirEndsAsBoundedNoLandingRatherThanSuccess(GameTestHelper h){
        Wolf wolf=wolf(h,true);
        var evaluation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,5);
        h.assertTrue(!evaluation.accepted()&&evaluation.rejection()==MobGravityPlanner.Rejection.NO_LANDING_IN_HORIZON,
            "open air should stay an explicit bounded miss, got "+evaluation.rejection());
        h.succeed();
    }

    @GameTest(padding=32)
    public void trajectoryLeavingWorldBoundsFailsClosedAndDoesNotMutate(GameTestHelper h){
        Wolf wolf=wolf(h,true);wolf.setDeltaMovement(new Vec3(0,1000,0));
        Vec3 beforePosition=wolf.position(),beforeVelocity=wolf.getDeltaMovement();
        Direction beforeGravity=GravityDirectionUtil.getOwnGravityDirection(wolf);
        var beforeOwnership=MobGravity.state(wolf).ownership;
        var evaluation=MobGravityPlanner.evaluateImmediate(wolf,Direction.EAST,2);
        h.assertTrue(!evaluation.accepted()&&evaluation.rejection()==MobGravityPlanner.Rejection.UNKNOWN_GEOMETRY,
            "trajectory outside build height was treated as known air: "+evaluation.rejection());
        h.assertTrue(wolf.position().equals(beforePosition)&&wolf.getDeltaMovement().equals(beforeVelocity),"failed unknown-geometry query mutated motion state");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==beforeGravity&&MobGravity.state(wolf).ownership==beforeOwnership,
            "failed unknown-geometry query mutated gravity ownership");
        h.succeed();
    }

    private static Wolf wolf(GameTestHelper h,boolean reorientation){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(0,3,0)),h.absolutePos(new BlockPos(20,18,12))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(false);wolf.setDeltaMovement(Vec3.ZERO);
        if(reorientation)wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,600));
        return wolf;
    }

    private static Holder<MobEffect> clinging(){return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();}
}
