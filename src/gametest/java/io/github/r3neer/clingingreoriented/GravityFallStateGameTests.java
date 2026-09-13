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

public final class GravityFallStateGameTests {
    private static ServerPlayer managed(GameTestHelper h,int airborneTicks){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(5.5,12,5.5)));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,400));
        ClingingReoriented.write(p,Direction.DOWN);
        var s=ClingingReoriented.data(p);
        s.owned=true;s.selected=Direction.DOWN;s.visualFrameOwned=true;s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;
        s.airborneTicks=airborneTicks;
        p.setOnGround(false);p.setDeltaMovement(Vec3.ZERO);
        return p;
    }

    private static LandingSurfaceProvider fixture(ServerPlayer p,AtomicBoolean valid,AtomicBoolean supportNow,double fraction){
        return new LandingSurfaceProvider(){
            private LocalContact contact(Query q){return new LocalContact("gravity_fall_fixture",1L,FaceGeometry.vector(q.gravity().getOpposite()));}
            @Override public Optional<LocalContact> currentSupport(Query q){
                return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&supportNow.get()?Optional.of(contact(q)):Optional.empty();
            }
            @Override public Optional<LocalSweep> sweep(Query q,AABB start,AABB end){
                return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&!supportNow.get()?Optional.of(new LocalSweep(contact(q),fraction,true)):Optional.empty();
            }
            @Override public boolean revalidate(Query q,LocalContact c){
                return q.entity().getUUID().equals(p.getUUID())&&valid.get()&&c!=null&&c.localId().equals("gravity_fall_fixture");
            }
        };
    }

    @GameTest(padding=16)
    public void startsAtTwelveAirborneTicksOnlyUnderOwnedPhysics(GameTestHelper h){
        var p=managed(h,GravityFallState.START_AIRBORNE_TICKS-1);var s=ClingingReoriented.data(p);
        GravityFallState.tick(p);
        h.assertFalse(s.gravityFallActive,"Gravity Fall must not start before the sustained-airborne threshold");
        s.airborneTicks=GravityFallState.START_AIRBORNE_TICKS;
        GravityFallState.tick(p);
        h.assertTrue(s.gravityFallActive,"owned sustained airborne physics should start Gravity Fall at tick 12");
        h.assertFalse(s.gravityFallLanding,"open-air start must begin in sustained transport, not landing phase");
        h.succeed();
    }

    @GameTest(padding=16)
    public void potionWithoutPhysicsOwnershipNeverStartsGravityFall(GameTestHelper h){
        var p=managed(h,GravityFallState.START_AIRBORNE_TICKS+10);var s=ClingingReoriented.data(p);
        s.owned=false;
        GravityFallState.tick(p);
        h.assertFalse(s.gravityFallActive,"merely carrying Reorientation must not opt an ordinary fall into Gravity Fall presentation");
        h.succeed();
    }

    @GameTest(padding=16)
    public void bodyLandingUsesPredictorEvenWhenCameraNeedsNoCommit(GameTestHelper h){
        var p=managed(h,GravityFallState.START_AIRBORNE_TICKS+3);var s=ClingingReoriented.data(p);
        s.gravityFallActive=true;s.gravityFallLanding=false;s.landingCommitted=false;
        // Camera is already canonical DOWN, so S02/S03 have no reason to lock or rotate it.
        s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;
        var valid=new AtomicBoolean(true);
        var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","gravity_fall_body_land"),fixture(p,valid,supportNow,.4D));
        try{
            GravityFallState.tick(p);
            h.assertFalse(s.landingCommitted,"body landing must not manufacture a camera commitment");
            h.assertTrue(s.gravityFallLanding,"body should prepare for the same imminent support independently of camera state");
            h.assertTrue(s.gravityFallLandingGravity==Direction.DOWN,"body landing target must be the predicted gravity-relative floor");
            h.assertTrue(Math.abs(s.gravityFallLandingEtaTicks-.4D)<1.0E-6D,"body landing must retain predictor ETA for visual timing");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void invalidatedBodyLandingResumesFromCurrentPresentation(GameTestHelper h){
        var p=managed(h,GravityFallState.START_AIRBORNE_TICKS+3);var s=ClingingReoriented.data(p);
        s.gravityFallActive=true;
        var valid=new AtomicBoolean(true);
        var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","gravity_fall_resume"),fixture(p,valid,supportNow,.35D));
        try{
            GravityFallState.tick(p);
            h.assertTrue(s.gravityFallLanding,"fixture should first enter body landing");
            valid.set(false);
            GravityFallState.tick(p);
            h.assertTrue(s.gravityFallActive,"invalidating a landing target should resume sustained Gravity Fall, not reset the whole presentation");
            h.assertFalse(s.gravityFallLanding,"invalidated landing target must leave BODY_LANDING");
            h.assertTrue(s.gravityFallLandingEtaTicks==0.0D,"resume must clear stale touchdown timing");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void realSupportResetsActiveGravityFall(GameTestHelper h){
        var p=managed(h,GravityFallState.START_AIRBORNE_TICKS+3);var s=ClingingReoriented.data(p);
        s.gravityFallActive=true;s.gravityFallLanding=true;s.gravityFallLandingGravity=Direction.DOWN;s.gravityFallLandingEtaTicks=.2D;
        var valid=new AtomicBoolean(true);
        var supportNow=new AtomicBoolean(true);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","gravity_fall_touchdown"),fixture(p,valid,supportNow,.2D));
        try{
            GravityFallState.tick(p);
            h.assertFalse(s.gravityFallActive,"real support should terminate Gravity Fall immediately");
            h.assertFalse(s.gravityFallLanding,"touchdown must clear landing phase");
            h.assertTrue(s.gravityFallLandingEtaTicks==0.0D,"touchdown must clear landing ETA");
        }finally{reg.close();}
        h.succeed();
    }

    @GameTest(padding=16)
    public void imminentSupportAtThresholdDoesNotFlashStart(GameTestHelper h){
        var p=managed(h,GravityFallState.START_AIRBORNE_TICKS);var s=ClingingReoriented.data(p);
        var valid=new AtomicBoolean(true);
        var supportNow=new AtomicBoolean(false);
        var reg=LandingSurfaces.register(Identifier.fromNamespaceAndPath("clinging_reoriented_test","gravity_fall_no_flash"),fixture(p,valid,supportNow,.25D));
        try{
            GravityFallState.tick(p);
            h.assertFalse(s.gravityFallActive,"first eligible frame already inside landing horizon must stay normal instead of flashing START");
        }finally{reg.close();}
        h.succeed();
    }
}
