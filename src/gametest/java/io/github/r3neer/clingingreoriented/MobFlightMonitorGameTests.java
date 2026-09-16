package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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

    @GameTest(padding=48,maxTicks=80)
    public void reorientationCorrectsAfterReactionDelayButSpentClingingCannotSecondTurn(GameTestHelper h){
        clear(h);
        Wolf agile=flightWolf(h,new BlockPos(5,10,5),Reorientation.EFFECT);
        Wolf clinging=flightWolf(h,new BlockPos(5,10,9),clinging());
        long agileSequence=MobGravity.state(agile).visualSequence,clingingSequence=MobGravity.state(clinging).visualSequence;

        var provider=new LandingSurfaceProvider(){
            private boolean ours(Query query){return query.entity()==agile||query.entity()==clinging;}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(!ours(query))return Optional.empty();
                Vec3 a=start.getCenter(),b=end.getCenter();
                if(query.gravity()==Direction.EAST&&b.x>a.x+1.0E-8D)
                    return Optional.of(new LocalSweep(new LocalContact("east-blocker",1L,new Vec3(0,0,1)),.35D,false));
                if(query.gravity()==Direction.NORTH&&b.z<a.z-1.0E-8D)
                    return Optional.of(new LocalSweep(new LocalContact("north-safe",1L,new Vec3(0,0,1)),.5D,true));
                return Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){return ours(query);}
        };
        var registration=LandingSurfaces.register(
            Identifier.fromNamespaceAndPath("clinging_reoriented_test","s07_air_reaction"),provider);

        h.runAfterDelay(20,()->{
            try{
                h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(agile)==Direction.NORTH,
                    "Reorientation mob did not correct persistent EAST danger after its reaction delay");
                h.assertTrue(MobGravity.state(agile).visualSequence>agileSequence,
                    "Reorientation correction emitted no owned visual transition");
                h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(clinging)==Direction.EAST,
                    "spent Clinging invented a second airborne gravity turn");
                h.assertTrue(MobGravity.state(clinging).visualSequence==clingingSequence,
                    "spent Clinging emitted a phantom second-turn visual");
            }finally{registration.close();}
            h.succeed();
        });
    }

    @GameTest(padding=64,maxTicks=100)
    public void disappearingFarCommittedLandingIsDetectedBeforeShortMonitorCouldRediscoverIt(GameTestHelper h){
        clear(h);floor(h);
        Wolf wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,10,5));
        wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(true);wolf.setDeltaMovement(Vec3.ZERO);
        wolf.addEffect(new MobEffectInstance(Reorientation.EFFECT,800));
        AtomicBoolean eastValid=new AtomicBoolean(true);
        double farX=wolf.getX()+25.0D;

        var provider=new LandingSurfaceProvider(){
            private LocalContact east(){return new LocalContact("far-east",1L,new Vec3(-1,0,0));}
            @Override public Optional<LocalContact> currentSupport(Query query){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query query,AABB start,AABB end){
                if(query.entity()!=wolf)return Optional.empty();
                Vec3 a=start.getCenter(),b=end.getCenter();
                if(query.gravity()==Direction.EAST&&b.x>a.x+1.0E-8D){
                    if(a.x>=farX-1.0E-7D)return Optional.of(new LocalSweep(east(),0.0D,true));
                    if(b.x>=farX){
                        double fraction=Math.clamp((farX-a.x)/(b.x-a.x),0.0D,1.0D);
                        return Optional.of(new LocalSweep(east(),fraction,true));
                    }
                }
                if(query.gravity()==Direction.NORTH&&b.z<a.z-1.0E-8D)
                    return Optional.of(new LocalSweep(new LocalContact("north-rescue",1L,new Vec3(0,0,1)),.5D,true));
                return Optional.empty();
            }
            @Override public boolean revalidate(Query query,LocalContact contact){
                if(query.entity()!=wolf)return false;
                return !"far-east".equals(contact.localId())||eastValid.get();
            }
        };
        var registration=LandingSurfaces.register(
            Identifier.fromNamespaceAndPath("clinging_reoriented_test","s07_far_committed"),provider);
        var evaluation=MobGravityPlanner.evaluateGroundedLaunch(wolf,wolf.position(),Direction.EAST,80);
        h.assertTrue(evaluation.accepted(),"far EAST fixture rejected before commit: "+evaluation.rejection());
        h.assertTrue(evaluation.transition().etaTicks()>MobFlightMonitor.DEFAULT_HORIZON_TICKS,
            "fixture landing is not actually beyond short monitor horizon: eta="+evaluation.transition().etaTicks());
        var committed=MobGravity.executePlannedTransition(wolf,Direction.EAST,80);
        h.assertTrue(committed!=null,"far EAST evaluation passed but commit seam rejected the same fixture");
        eastValid.set(false);

        h.runAfterDelay(20,()->{
            try{
                h.assertTrue(GravityDirectionUtil.getOwnGravityDirection(wolf)==Direction.NORTH,
                    "reactor lost committed far landing identity and failed to take NORTH rescue after it disappeared");
            }finally{registration.close();}
            h.succeed();
        });
    }

    private static Wolf flightWolf(GameTestHelper h,BlockPos relative,Holder<MobEffect> effect){
        Wolf wolf=h.spawn(EntityTypes.WOLF,relative);wolf.setNoAi(true);wolf.setNoGravity(true);wolf.setOnGround(false);wolf.setDeltaMovement(Vec3.ZERO);
        wolf.addEffect(new MobEffectInstance(effect,600));GravityDirectionUtil.setGravityDirection(wolf,Direction.EAST);
        var state=MobGravity.state(wolf);state.ownership=MobGravity.Ownership.OWNED_EFFECT;state.ownedDirection=Direction.EAST;state.airUsed=true;state.retryAt=0;
        MobFlightReactor.clear(wolf);return wolf;
    }

    private static Holder<MobEffect> clinging(){return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow();}
    private static void floor(GameTestHelper h){for(int x=0;x<=12;x++)for(int z=0;z<=10;z++)h.setBlock(new BlockPos(x,9,z),Blocks.STONE);}
    private static void clear(GameTestHelper h){
        for(var pos:BlockPos.betweenClosed(h.absolutePos(new BlockPos(-4,3,-4)),h.absolutePos(new BlockPos(38,20,14))))
            h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
    }
}
