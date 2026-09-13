package io.github.r3neer.clingingreoriented;

import io.github.r3neer.clingingreoriented.api.LandingSurfaceProvider;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import io.github.r3neer.clingingreoriented.geometry.FaceGeometry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class LandingStateGameTests {
    private static ServerPlayer managed(GameTestHelper h,Direction gravity,Direction visualBase){
        var p=h.makeMockServerPlayerInLevel();p.snapTo(h.absoluteVec(new Vec3(5.5,12,5.5)));p.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));
        ClingingReoriented.write(p,gravity);var s=ClingingReoriented.data(p);s.owned=true;s.selected=gravity;s.visualFrameOwned=true;
        s.visualBaseKnown=true;s.visualBaseDirection=visualBase;p.setOnGround(false);p.setDeltaMovement(new Vec3(0,-.25,0));return p;
    }
    private static LandingSurfaceProvider fixture(ServerPlayer p,AtomicBoolean valid,AtomicBoolean supportNow,boolean sweepSupport){
        return new LandingSurfaceProvider(){
            private LocalContact contact(Query q){return new LocalContact("fixture",1L,FaceGeometry.vector(q.gravity().getOpposite()));}
            @Override public Optional<LocalContact> currentSupport(Query q){return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&supportNow.get()?Optional.of(contact(q)):Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query q,AABB start,AABB end){return q.entity().getUUID().equals(p.getUUID())&&valid.get()?Optional.of(new LocalSweep(contact(q),.5,sweepSupport)):Optional.empty();}
            @Override public boolean revalidate(Query q,LocalContact c){return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&c!=null&&c.localId().equals("fixture");}
        };
    }

    @GameTest(padding=16)
    public void quarterTurnCommitsOnlyInsideSnapWindowAndBlocksFurtherGravity(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","quarter_commit"),fixture(p,valid,supportNow,true));
        try{
            LandingState.tick(p);var s=ClingingReoriented.data(p);
            h.assertTrue(s.landingCommitted,"imminent external support creates commitment");
            h.assertTrue(s.landingKind==GravityTransition.TurnKind.QUARTER,"EAST visual base -> DOWN landing is quarter turn");
            var result=ClingingReoriented.attempt(p,new Vec3(1,0,0),GravityTransition.headingFromYaw(Direction.DOWN,p.getYRot()));
            h.assertTrue(result==ClingingReoriented.Result.LANDING_COMMITTED,"new gravity input is rejected by normal landing lock state");
            valid.set(false);LandingState.tick(p);
            h.assertFalse(s.landingCommitted,"provider invalidation cancels commitment");
            h.assertFalse(s.visualBaseKnown,"canceled visual trajectory is conservatively non-canonical");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void earlierBlockingContactNeverBecomesLanding(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","obstruction"),fixture(p,valid,supportNow,false));
        try{LandingState.tick(p);h.assertFalse(ClingingReoriented.data(p).landingCommitted,"first non-support collision stops trajectory instead of seeing through it");}
        finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void sameVisualFrameNeedsNoArtificialLandingLock(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.DOWN);var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","same_frame"),fixture(p,valid,supportNow,true));
        try{LandingState.tick(p);h.assertFalse(ClingingReoriented.data(p).landingCommitted,"already aligned visual floor needs no snap lock");}
        finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void oppositeVisualFrameUsesHalfTurnAndTouchdownClearsState(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.UP);var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","half_touchdown"),fixture(p,valid,supportNow,true));
        try{
            LandingState.tick(p);var s=ClingingReoriented.data(p);
            h.assertTrue(s.landingCommitted&&s.landingKind==GravityTransition.TurnKind.HALF,"opposite retained frame reserves the 240 ms half-turn window");
            supportNow.set(true);p.setDeltaMovement(Vec3.ZERO);LandingState.tick(p);
            h.assertFalse(s.landingCommitted,"real provider support completes commitment");
            h.assertTrue(s.visualBaseKnown&&s.visualBaseDirection==Direction.DOWN,"touchdown establishes new canonical visual base");
            h.assertTrue(s.airborneTicks==0,"touchdown resets sustained-airborne clock");
        }finally{reg.close();}
        h.succeed();
    }
}
