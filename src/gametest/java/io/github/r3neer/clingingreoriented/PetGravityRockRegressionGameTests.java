package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.lang.reflect.Field;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Regression for the original "follow the owner, turn, hit a bad rock and suffocate" failure mode. */
public final class PetGravityRockRegressionGameTests {
    @GameTest(padding=64)
    public void blockingOrTrappedIntermediateContactCannotWakeGravityFollow(GameTestHelper h) throws Exception {
        clear(h);floor(h);wall(h,9);
        var owner=h.makeMockServerPlayerInLevel();owner.setGameMode(GameType.SURVIVAL);owner.snapTo(h.absoluteVec(new Vec3(14,10,5)));
        owner.setNoGravity(true);owner.setOnGround(true);owner.setDeltaMovement(Vec3.ZERO);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.tame(owner);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(wolf.distanceToSqr(owner)<100.0D,"fixture must keep vanilla FollowOwnerGoal asleep");

        double[] northLane={Double.NaN,Double.NaN};
        var provider=new LandingSurfaceProvider(){
            private LocalContact blocker(){return new LocalContact("rock-blocker",1L,new Vec3(0,1,0));}
            private LocalContact trap(){return new LocalContact("rock-trap",1L,new Vec3(0,0,1));}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(!query.entity().getUUID().equals(wolf.getUUID()))return Optional.empty();
                Vec3 a=start.getCenter(),b=end.getCenter();
                if(query.gravity()==Direction.EAST&&b.x>a.x+1.0E-8D)
                    return Optional.of(new LocalSweep(blocker(),.05D,false));
                if(query.gravity()!=Direction.NORTH||b.z>=a.z-1.0E-8D)return Optional.empty();
                if(Double.isNaN(northLane[0])){northLane[0]=a.x;northLane[1]=a.y;}
                boolean mainLane=Math.abs(a.x-northLane[0])<1.0E-4D&&Math.abs(a.y-northLane[1])<1.0E-4D;
                return mainLane?Optional.of(new LocalSweep(trap(),.25D,true)):Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){return query.entity().getUUID().equals(wolf.getUUID());}
        };
        var registration=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","pet_rock_regression"),provider);
        try{
            var goal=new FollowOwnerGoal(wolf,1.0D,10.0F,2.0F);
            h.assertFalse(goal.canUse(),"unsafe intermediate contact woke gravity follow despite blocker/trapped landing");
            var state=state(goal);
            h.assertTrue(state.phase()==PetGravityFollow.Phase.IDLE&&state.plan()==null,"rejected rock maneuver left executable follow state");
            h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"unsafe rock forecast remotely changed pet gravity");
            h.assertTrue(MobGravity.state(wolf).visualSequence==0L,"unsafe rock forecast emitted a visual gravity commit");
        }finally{registration.close();}
        h.succeed();
    }

    private static PetGravityFollow.State state(FollowOwnerGoal goal) throws Exception {
        Field field=FollowOwnerGoal.class.getDeclaredField("clinging$gravityFollow");field.setAccessible(true);
        return (PetGravityFollow.State)field.get(goal);
    }

    private static void wall(GameTestHelper h,int x){for(int y=9;y<=16;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);}
    private static void floor(GameTestHelper h){for(int x=0;x<=22;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(26,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
