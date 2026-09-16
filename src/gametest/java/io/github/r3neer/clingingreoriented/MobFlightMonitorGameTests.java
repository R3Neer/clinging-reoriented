package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** S07-A gates: observe committed ballistic geometry without replanning or mutating gravity. */
public final class MobFlightMonitorGameTests {
    @GameTest(padding=48)
    public void monitorDistinguishesExpectedChangedAndBlockingFirstContact(GameTestHelper h){
        clear(h);floor(h);
        Zombie mob=h.spawn(EntityTypes.ZOMBIE,new BlockPos(5,10,5));
        mob.setNoAi(true);mob.setNoGravity(true);mob.setOnGround(true);mob.setDeltaMovement(Vec3.ZERO);
        mob.addEffect(new MobEffectInstance(Reorientation.EFFECT,600));
        AtomicInteger mode=new AtomicInteger();

        var provider=new LandingSurfaceProvider(){
            private LocalContact contact(){
                return mode.get()==0?new LocalContact("expected-east",1L,new Vec3(-1,0,0))
                    :new LocalContact("changed-east",2L,new Vec3(-1,0,0));
            }
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(query.entity()!=mob||query.gravity()!=Direction.EAST)return Optional.empty();
                if(end.getCenter().x<=start.getCenter().x+1.0E-8D)return Optional.empty();
                return Optional.of(new LocalSweep(contact(),.5D,mode.get()!=2));
            }
            @Override public boolean revalidate(Query query,LocalContact contact){
                return query.entity()==mob&&query.gravity()==Direction.EAST;
            }
        };
        var registration=LandingSurfaces.register(
            Identifier.fromNamespaceAndPath("clinging_reoriented_test","s07_flight_monitor"),provider);
        try{
            var committed=MobGravity.executePlannedTransition(mob,Direction.EAST,30);
            h.assertTrue(committed!=null,"fixture failed to commit deterministic EAST transition");
            h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(mob)==Direction.EAST,"commit did not enter EAST gravity");
            long sequence=MobGravity.state(mob).visualSequence;

            var expected=MobFlightMonitor.observe(mob,committed,20);
            h.assertTrue(expected.status()==MobFlightMonitor.Status.EXPECTED_SUPPORT,
                "unchanged committed contact was not recognized: "+expected.status());
            h.assertFalse(expected.danger(),"expected support was classified as danger");

            mode.set(1);
            var changed=MobFlightMonitor.observe(mob,committed,20);
            h.assertTrue(changed.status()==MobFlightMonitor.Status.SAFE_CHANGED_SUPPORT,
                "changed safe first contact was not material: "+changed.status());
            h.assertTrue(changed.material()&&!changed.danger(),"safe changed support should be material but not dangerous");

            mode.set(2);
            var blocking=MobFlightMonitor.observe(mob,committed,20);
            h.assertTrue(blocking.status()==MobFlightMonitor.Status.BLOCKING_CONTACT,
                "non-support first contact was not classified as blocking: "+blocking.status());
            h.assertTrue(blocking.danger(),"blocking first contact did not become danger");

            h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(mob)==Direction.EAST,
                "flight observation changed logical gravity");
            h.assertTrue(MobGravity.state(mob).visualSequence==sequence,"flight observation emitted a gravity turn/visual event");
        }finally{registration.close();}
        h.succeed();
    }

    private static void floor(GameTestHelper h){for(int x=0;x<=12;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(18,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
