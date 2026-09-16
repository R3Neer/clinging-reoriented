package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.entity.ai.DirectionalGroundNodeEvaluator;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
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

/** S05-C: the support graph is explored online by excluding failed local launch edges between replans. */
public final class MobGravityOnlinePlannerGameTests {
    @GameTest(padding=48)
    public void excludingConcreteLaunchesEventuallySelectsNextSafeDirectionWithoutMutation(GameTestHelper h){
        Wolf wolf=wolf(h,true);Vec3 before=wolf.position();
        Vec3 focus=wolf.position().add(4.0D,0.0D,0.0D);
        var goal=new MobGravityLocalPlanner.Goal(){
            @Override public Vec3 focus(){return focus;}
            @Override public boolean satisfied(Vec3 position,Direction gravity){return false;}
            @Override public double heuristic(Vec3 position,Direction gravity){
                return gravity==Direction.EAST?0.0D:gravity==Direction.NORTH?10.0D:1000.0D;
            }
        };
        var provider=new LandingSurfaceProvider(){
            private LocalContact contact(Direction gravity){
                Vec3 normal=gravity==Direction.EAST?new Vec3(-1,0,0):new Vec3(0,0,1);
                return new LocalContact("online-"+gravity.getName(),gravity.ordinal()+1L,normal);
            }
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(!query.entity().getUUID().equals(wolf.getUUID()))return Optional.empty();
                if(query.gravity()==Direction.EAST&&end.getCenter().x>start.getCenter().x+1.0E-8D)
                    return Optional.of(new LocalSweep(contact(Direction.EAST),.5D,true));
                if(query.gravity()==Direction.NORTH&&end.getCenter().z<start.getCenter().z-1.0E-8D)
                    return Optional.of(new LocalSweep(contact(Direction.NORTH),.5D,true));
                return Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){
                return query.entity().getUUID().equals(wolf.getUUID())
                    &&(query.gravity()==Direction.EAST||query.gravity()==Direction.NORTH);
            }
        };
        var registration=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","online_edges"),provider);
        try{
            Set<MobGravityLocalPlanner.ManeuverKey> excluded=new HashSet<>();
            var plan=MobGravityLocalPlanner.plan(wolf,goal,20,excluded);
            h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.TRANSITION&&plan.terminalGravity()==Direction.EAST,
                "lowest-cost safe launch should be EAST, got "+plan.kind()+"/"+plan.terminalGravity());
            h.assertTrue(plan.maneuverKey()!=null,"transition did not expose a stable maneuver key");
            h.assertTrue(plan.transitionEvaluations()>0&&plan.transitionEvaluations()<=MobGravityLocalPlanner.MAX_LAUNCH_SAMPLES*5,
                "local expansion escaped bounded launch-region budget: "+plan.transitionEvaluations());

            int eastExcluded=0;
            while(plan.kind()==MobGravityLocalPlanner.Kind.TRANSITION&&plan.terminalGravity()==Direction.EAST
                &&eastExcluded<MobGravityLocalPlanner.MAX_LAUNCH_SAMPLES){
                h.assertTrue(excluded.add(plan.maneuverKey()),"planner repeated an already-excluded EAST launch key");
                eastExcluded++;
                plan=MobGravityLocalPlanner.plan(wolf,goal,20,Set.copyOf(excluded));
            }
            h.assertTrue(eastExcluded>0,"fixture never exercised concrete EAST exclusions");
            h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.TRANSITION&&plan.terminalGravity()==Direction.NORTH,
                "exhausting bounded EAST launches should expose safe NORTH edge, got "+plan.kind()+"/"+plan.terminalGravity());
            h.assertTrue(plan.maneuverKey()!=null&&!excluded.contains(plan.maneuverKey()),"excluded maneuver key was selected again");
            h.assertTrue(plan.transitionEvaluations()<=MobGravityLocalPlanner.MAX_LAUNCH_SAMPLES*5,
                "replan escaped bounded launch-region budget: "+plan.transitionEvaluations());
            h.assertTrue(wolf.position().equals(before),"online replanning moved the real mob");
        }finally{registration.close();}
        h.succeed();
    }

    @GameTest(padding=32)
    public void maneuverKeyIsStableInsideSameDirectionalNode(GameTestHelper h){
        Vec3 base=DirectionalGroundNodeEvaluator.entityPosition(h.absolutePos(new BlockPos(5,10,5)),Direction.EAST);
        var a=MobGravityLocalPlanner.maneuverKey(Direction.EAST,base,Direction.UP);
        var b=MobGravityLocalPlanner.maneuverKey(Direction.EAST,base.add(0.0D,.2D,-.2D),Direction.UP);
        h.assertTrue(a!=null&&a.equals(b),"sub-block tangent jitter changed maneuver identity: "+a+" vs "+b);
        h.succeed();
    }

    @GameTest(padding=40)
    public void gravityExclusionsNeverBlockOrdinaryWalk(GameTestHelper h){
        Wolf wolf=wolf(h,false);Vec3 focus=wolf.position().add(4.0D,0.0D,0.0D);
        var fake=MobGravityLocalPlanner.maneuverKey(Direction.DOWN,wolf.position(),Direction.EAST);
        var goal=new MobGravityLocalPlanner.Goal(){
            @Override public Vec3 focus(){return focus;}
            @Override public boolean satisfied(Vec3 position,Direction gravity){return position.distanceToSqr(focus)<.7D*.7D;}
            @Override public double heuristic(Vec3 position,Direction gravity){return position.distanceTo(focus);}
        };
        var plan=MobGravityLocalPlanner.plan(wolf,goal,20,Set.of(fake));
        h.assertTrue(plan.kind()==MobGravityLocalPlanner.Kind.WALK&&plan.walkPath()!=null&&plan.walkPath().canReach(),
            "gravity-edge memory interfered with ordinary WALK");
        h.assertTrue(plan.maneuverKey()==null&&plan.transitionEvaluations()==0,"WALK unexpectedly consumed gravity planning budget");
        h.succeed();
    }

    private static Wolf wolf(GameTestHelper h,boolean reorientation){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-6,3,-6)),h.absolutePos(new BlockPos(28,18,16))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        for(int x=0;x<=22;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        if(reorientation)wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,600));
        return wolf;
    }
}
