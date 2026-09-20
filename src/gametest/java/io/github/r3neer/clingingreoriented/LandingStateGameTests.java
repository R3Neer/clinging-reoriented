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
    private static LandingSurfaceProvider planeFixture(ServerPlayer p,AtomicBoolean valid,AtomicBoolean visible,double targetY){
        String id="plane:"+Double.toHexString(targetY);
        return new LandingSurfaceProvider(){
            private LocalContact contact(Query q){return new LocalContact(id,1L,FaceGeometry.vector(q.gravity().getOpposite()));}
            @Override public Optional<LocalContact> currentSupport(Query q){return Optional.empty();}
            @Override public Optional<LocalSweep> sweep(Query q,AABB start,AABB end){
                if(!q.entity().getUUID().equals(p.getUUID())||!valid.get())return Optional.empty();
                if(!visible.get())return Optional.of(new LocalSweep(contact(q),.1D,false));
                if(end.minY>=start.minY || !(start.minY>targetY&&end.minY<=targetY))return Optional.empty();
                double fraction=(start.minY-targetY)/(start.minY-end.minY);
                return Optional.of(new LocalSweep(contact(q),fraction,true));
            }
            @Override public boolean revalidate(Query q,LocalContact c){return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&c!=null&&c.localId().equals(id);}
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
    public void tangentialFeetGrazeDoesNotBecomeLandingCandidate(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);p.setDeltaMovement(new Vec3(1.0D,-.05D,0));
        var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","feet_graze"),fixture(p,valid,supportNow,true));
        try{
            LandingState.tick(p);var s=ClingingReoriented.data(p);
            h.assertTrue(s.landingCandidate==null,"near-tangential feet contact must remain a graze, not a landing candidate");
            h.assertTrue(s.landingGrazeKey!=null&&s.landingGrazeGravity==Direction.DOWN,"graze prediction must retain one-tick contact identity");
            h.assertFalse(s.landingCommitted,"near-tangential feet contact must not lock gravity input");
        }finally{reg.close();}
        h.succeed();
    }


    @GameTest(padding=16)
    public void predictedGrazeSuppressesFirstGroundFlagButPersistentFeetSupportWins(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(true);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","graze_ground_flag"),fixture(p,valid,supportNow,true));
        try{
            p.setOnGround(true);p.setDeltaMovement(new Vec3(1.0D,0,0));
            var contact=LandingSurfaces.currentSupport(p,Direction.DOWN).orElseThrow();
            var s=ClingingReoriented.data(p);long now=p.level().getGameTime();
            s.landingGrazeKey=contact.key();s.landingGrazeGravity=Direction.DOWN;s.landingGrazeUntilTick=now;
            h.assertFalse(AirChanges.grounded(p),"first onGround tick for the predicted graze must not become semantic support");
            h.assertTrue(GravityInput.available(p),"graze-suppressed ground flag must not steal the Reorientation rescue input");
            s.landingGrazeUntilTick=now-1L;
            h.assertTrue(AirChanges.grounded(p),"feet contact that persists beyond graze memory must become real support");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=40)
    public void acquiresLongRangeBeforePresentationAndKeepsSurfaceIdentity(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);p.snapTo(h.absoluteVec(new Vec3(5.5,30,5.5)));p.setDeltaMovement(new Vec3(0,-.25,0));
        double targetY=h.absoluteVec(new Vec3(5.5,12,5.5)).y;var valid=new AtomicBoolean(true);var visible=new AtomicBoolean(true);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","long_acquisition"),planeFixture(p,valid,visible,targetY));
        try{
            LandingState.tick(p);var s=ClingingReoriented.data(p);
            h.assertTrue(s.landingCandidate!=null,"support inside 40-tick acquisition horizon should be retained");
            h.assertTrue(s.landingCandidate.etaTicks()>LandingTiming.PRESENTATION_TICKS,"early acquisition must stay outside visual window");
            h.assertTrue(s.landingCandidate.etaTicks()<=LandingPrediction.ACQUISITION_TICKS,"early acquisition must respect bounded horizon");
            h.assertFalse(s.landingCommitted,"early acquisition must not commit camera/body presentation");
            var key=s.landingCandidate.contact().key();
            p.snapTo(h.absoluteVec(new Vec3(5.5,16,5.5)));p.setDeltaMovement(new Vec3(0,-.25,0));LandingState.tick(p);
            h.assertTrue(s.landingCandidate!=null&&s.landingCandidate.contact().key().equals(key),"entering body-approach window must preserve acquired surface identity");
            h.assertTrue(s.landingCandidateStableTicks>=2,"same acquired surface should accumulate stability");
            h.assertTrue(s.landingCandidateConfirmed,"presentation may use only the current physical prediction");
            h.assertTrue(s.landingCandidate.approach()==LandingPolicy.Approach.CLEAR,"straight feet-first approach should classify as clear");
            h.assertTrue(s.landingCandidate.etaTicks()>LandingPolicy.CLEAR_COMMIT_TICKS&&s.landingCandidate.etaTicks()<=LandingTiming.PRESENTATION_TICKS,
                "clear support should be visible to body anticipation before input lock; eta="+s.landingCandidate.etaTicks());
            h.assertFalse(s.landingCommitted,"clear support must not lock input while still outside the final five ticks");
            p.snapTo(new Vec3(p.getX(),targetY+2.0D,p.getZ()));p.setDeltaMovement(new Vec3(0,-.25,0));LandingState.tick(p);
            h.assertTrue(s.landingCommitted,"clear support should commit once ETA enters the final five ticks");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=40)
    public void oneFarMissIsHysteresisOnlyAndSecondMissClearsCandidate(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);p.snapTo(h.absoluteVec(new Vec3(5.5,30,5.5)));p.setDeltaMovement(new Vec3(0,-.25,0));
        double targetY=h.absoluteVec(new Vec3(5.5,12,5.5)).y;var valid=new AtomicBoolean(true);var visible=new AtomicBoolean(true);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","candidate_hysteresis"),planeFixture(p,valid,visible,targetY));
        try{
            LandingState.tick(p);var s=ClingingReoriented.data(p);h.assertTrue(s.landingCandidate!=null,"fixture should acquire far support");
            visible.set(false);LandingState.tick(p);
            h.assertTrue(s.landingCandidate!=null&&s.landingCandidateMisses==1,"one far miss should retain candidate only as hysteresis");
            h.assertFalse(s.landingCandidateConfirmed,"hysteresis-retained candidate must not masquerade as a current forecast");
            h.assertFalse(s.landingCommitted,"hysteresis alone must never start landing presentation");
            LandingState.tick(p);
            h.assertTrue(s.landingCandidate==null,"second consecutive miss should clear acquired candidate");
            h.assertFalse(s.landingCommitted,"clearing stale acquisition must not manufacture a commitment");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void earlierBlockingContactNeverBecomesLanding(GameTestHelper h){
        var p=managed(h,Direction.DOWN,Direction.EAST);var valid=new AtomicBoolean(true);var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","obstruction"),fixture(p,valid,supportNow,false));
        try{LandingState.tick(p);var s=ClingingReoriented.data(p);h.assertFalse(s.landingCommitted,"first non-support collision stops trajectory instead of seeing through it");h.assertTrue(s.landingCandidate==null,"blocking first contact must prevent even early acquisition of geometry behind it");}
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
            h.assertTrue(s.landingCandidate==null,"touchdown clears predictive acquisition state");
            h.assertTrue(s.visualBaseKnown&&s.visualBaseDirection==Direction.DOWN,"touchdown establishes new canonical visual base");
            h.assertTrue(s.airborneTicks==0,"touchdown resets sustained-airborne clock");
        }finally{reg.close();}
        h.succeed();
    }
}