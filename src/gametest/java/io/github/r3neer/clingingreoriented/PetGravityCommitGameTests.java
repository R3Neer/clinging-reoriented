package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
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

/** S06-A gates for the physical commit seam between pure planning and real mob state. */
public final class PetGravityCommitGameTests {
    @GameTest(padding=48)
    public void groundedClingingCommitConsumesTheAirborneStageAndMatchesForecast(GameTestHelper h){
        Wolf wolf=groundedWolf(h);wolf.addEffect(new MobEffectInstance(clinging(),600));
        for(int y=5;y<=15;y++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(13,y,z),Blocks.STONE);
        wolf.setDeltaMovement(new Vec3(.08D,0.0D,.04D));

        var preview=MobGravityPlanner.evaluateGroundedLaunch(wolf,wolf.position(),Direction.EAST,40);
        h.assertTrue(preview.accepted(),"fixture must preview a safe EAST wall landing: "+preview.rejection());
        var committed=MobGravity.executePlannedTransition(wolf,Direction.EAST,40);
        h.assertTrue(committed!=null,"revalidated grounded Clinging transition did not commit");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.EAST,"commit did not change gravity to EAST");
        h.assertTrue(MobGravity.state(wolf).ownership==MobGravity.Ownership.OWNED_EFFECT,"commit did not acquire effect ownership");
        h.assertTrue(MobGravity.state(wolf).airUsed,"grounded Clinging commit failed to consume the resulting airborne stage");
        h.assertTrue(wolf.getDeltaMovement().equals(Vec3.ZERO),"commit retained navigation momentum despite zero-velocity forecast: "+wolf.getDeltaMovement());
        h.assertTrue(committed.launchPosition().distanceTo(preview.transition().launchPosition())<1.0E-9D,
            "commit launch no longer matches the immediately previewed launch");
        var second=MobGravityPlanner.evaluateImmediate(wolf,Direction.NORTH,5);
        h.assertTrue(!second.accepted()&&second.rejection()==MobGravityPlanner.Rejection.CAPABILITY_SPENT,
            "Clinging received a second midair gravity plan after grounded commit: "+second.rejection());
        h.succeed();
    }

    @GameTest(padding=40)
    public void commitFailsClosedWhenPreviouslySafeLandingDisappears(GameTestHelper h){
        Wolf wolf=groundedWolf(h);wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,600));
        var provider=new LandingSurfaceProvider(){
            private LocalContact contact(){return new LocalContact("temporary-east",1L,new Vec3(-1,0,0));}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(!query.entity().getUUID().equals(wolf.getUUID())||query.gravity()!=Direction.EAST)return Optional.empty();
                return end.getCenter().x>start.getCenter().x+1.0E-8D?Optional.of(new LocalSweep(contact(),.5D,true)):Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){return query.entity().getUUID().equals(wolf.getUUID())&&query.gravity()==Direction.EAST;}
        };
        var registration=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","temporary_commit_support"),provider);
        var preview=MobGravityPlanner.evaluateGroundedLaunch(wolf,wolf.position(),Direction.EAST,20);
        h.assertTrue(preview.accepted(),"temporary provider did not establish safe preview: "+preview.rejection());
        registration.close();

        Vec3 beforePosition=wolf.position();Vec3 beforeVelocity=new Vec3(.03D,0.0D,-.02D);wolf.setDeltaMovement(beforeVelocity);
        var beforeOwnership=MobGravity.state(wolf).ownership;boolean beforeAirUsed=MobGravity.state(wolf).airUsed;
        var committed=MobGravity.executePlannedTransition(wolf,Direction.EAST,20);
        h.assertTrue(committed==null,"stale safe preview committed after its landing disappeared");
        h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.DOWN,"failed revalidation changed gravity");
        h.assertTrue(wolf.position().equals(beforePosition)&&wolf.getDeltaMovement().equals(beforeVelocity),"failed revalidation changed motion");
        h.assertTrue(MobGravity.state(wolf).ownership==beforeOwnership&&MobGravity.state(wolf).airUsed==beforeAirUsed,
            "failed revalidation changed ownership/capability state");
        h.succeed();
    }

    private static Wolf groundedWolf(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(0,3,0)),h.absolutePos(new BlockPos(20,18,12))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        for(int x=1;x<=18;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        return wolf;
    }

    private static Holder<MobEffect> clinging(){return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();}
}
