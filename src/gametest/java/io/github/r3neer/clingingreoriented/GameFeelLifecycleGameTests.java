package io.github.r3neer.clingingreoriented;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/** Lifecycle boundaries discovered during GF-S05 adversarial review. */
public final class GameFeelLifecycleGameTests {
    private static ServerPlayer managed(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(6.5,12,6.5)));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        ClingingReoriented.write(p,Direction.EAST);
        var s=ClingingReoriented.data(p);
        s.owned=true;s.visualFrameOwned=true;s.selected=Direction.EAST;
        p.setOnGround(false);p.setNoGravity(true);p.setDeltaMovement(new Vec3(.2,-.3,.1));
        return p;
    }

    @GameTest(padding=20)
    public void playerReplacementPreservesGravityFallEpochButNotTransientPresentation(GameTestHelper h){
        var old=managed(h);var before=ClingingReoriented.data(old);
        before.revision=21;before.visualSequence=34;before.gravityFallSequence=55;
        before.gravityFallActive=true;before.gravityFallLanding=true;before.gravityFallLandingGravity=Direction.DOWN;before.gravityFallLandingEtaTicks=2.0D;
        before.landingCommitted=true;before.airborneTicks=37;before.freeFlightVisualHeld=true;before.visualBaseKnown=true;

        var replacement=h.makeMockServerPlayerInLevel();
        ClingingReoriented.copyPlayerState(old,replacement,true);
        var after=ClingingReoriented.data(replacement);

        h.assertTrue(after.gravityFallSequence==55,"COPY_FROM reset the UUID-scoped Gravity Fall sequence epoch");
        h.assertTrue(after.visualSequence==34,"COPY_FROM reset the retained-camera visual sequence epoch");
        h.assertTrue(after.revision==22,"COPY_FROM did not advance replicated state revision");
        h.assertTrue(after.owned&&after.visualFrameOwned&&after.selected==Direction.EAST,"alive replacement lost durable gravity ownership");
        h.assertFalse(after.gravityFallActive||after.gravityFallLanding,"alive replacement copied transient Gravity Fall presentation");
        h.assertFalse(after.landingCommitted||after.freeFlightVisualHeld||after.visualBaseKnown,"alive replacement copied transient landing/camera presentation");
        h.assertTrue(after.airborneTicks==0,"alive replacement copied the old airborne presentation clock");

        GravityFallSync.publish(replacement,GravityFallSync.Phase.START,null);
        h.assertTrue(after.gravityFallSequence==56,"first post-replacement Gravity Fall event did not continue monotonically");
        h.succeed();
    }

    @GameTest(padding=20)
    public void sameDimensionTeleportReleasesLandingAndGravityFallPresentation(GameTestHelper h){
        var p=managed(h);var s=ClingingReoriented.data(p);
        s.airborneTicks=40;s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;
        s.landingCommitted=true;s.landingGravity=Direction.EAST;s.landingKind=GravityTransition.TurnKind.QUARTER;s.landingEtaTicks=3.0D;
        s.freeFlightVisualHeld=true;s.visualSequence=70;
        s.gravityFallActive=true;s.gravityFallLanding=true;s.gravityFallLandingGravity=Direction.EAST;s.gravityFallLandingEtaTicks=3.0D;s.gravityFallSequence=90;
        Vec3 momentum=p.getDeltaMovement();

        p.teleport(new TeleportTransition(p.level(),p.position().add(1.0D,2.0D,-1.0D),momentum,p.getYRot(),p.getXRot(),TeleportTransition.DO_NOTHING));

        h.assertFalse(s.gravityFallActive||s.gravityFallLanding,"teleport leaked Gravity Fall/BODY_LANDING presentation");
        h.assertTrue(s.gravityFallLandingEtaTicks==0.0D,"teleport left stale Gravity Fall landing ETA");
        h.assertTrue(s.gravityFallSequence==91,"teleport did not publish a monotonic Gravity Fall RESET epoch");
        h.assertFalse(s.landingCommitted||s.freeFlightVisualHeld||s.visualBaseKnown,"teleport leaked retained camera/landing ownership");
        h.assertTrue(s.airborneTicks==0,"teleport preserved an unrelated airborne presentation clock");
        h.assertTrue(s.visualSequence==71,"teleport did not publish exactly one retained-camera cancellation epoch");
        h.assertTrue(p.getDeltaMovement().equals(momentum),"presentation cleanup changed teleport momentum");
        h.succeed();
    }

    @GameTest(padding=20)
    public void teleportClearsLandingVisualEvenWhenNoFreeFlightHoldFlagRemains(GameTestHelper h){
        var p=managed(h);var s=ClingingReoriented.data(p);
        s.landingCommitted=true;s.freeFlightVisualHeld=false;s.visualBaseKnown=true;s.visualSequence=120;
        // This models a LAND animation that exists independently of a retained HOLD flag.
        p.teleport(new TeleportTransition(p.level(),p.position().add(0.0D,1.0D,0.0D),p.getDeltaMovement(),p.getYRot(),p.getXRot(),TeleportTransition.DO_NOTHING));
        h.assertFalse(s.landingCommitted||s.visualBaseKnown,"teleport retained standalone landing presentation state");
        h.assertTrue(s.visualSequence==121,"standalone landing presentation was not explicitly cancelled on teleport");
        h.succeed();
    }

    @GameTest(padding=20)
    public void losingPhysicsOwnershipReleasesFreeFlightHoldInsteadOfFreezingIt(GameTestHelper h){
        var p=managed(h);var s=ClingingReoriented.data(p);
        s.airborneTicks=25;s.freeFlightVisualHeld=true;s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;s.visualSequence=200;
        // Effect expiry leaves the entity alive/airborne, so the only relevant boundary is that
        // Clinging no longer controls physics. This used to leave HOLD active indefinitely.
        p.removeAllEffects();
        LandingState.tick(p);
        h.assertFalse(s.freeFlightVisualHeld||s.visualBaseKnown,"physics ownership loss froze the retained camera instead of releasing it");
        h.assertTrue(s.airborneTicks==0,"physics ownership loss preserved stale airborne presentation time");
        h.assertTrue(s.visualSequence==201,"physics ownership loss did not publish a camera release");
        h.succeed();
    }

    @GameTest(padding=20)
    public void losingPhysicsOwnershipDuringCommitReleasesRatherThanResumesLanding(GameTestHelper h){
        var p=managed(h);var s=ClingingReoriented.data(p);
        s.landingCommitted=true;s.freeFlightVisualHeld=true;s.visualBaseKnown=true;s.visualSequence=240;
        p.removeAllEffects();
        LandingState.tick(p);
        h.assertFalse(s.landingCommitted||s.freeFlightVisualHeld||s.visualBaseKnown,"ownership loss preserved a committed/resumable landing frame");
        h.assertTrue(s.visualSequence==241,"ownership loss should emit one release, not retain current LAND frame");
        h.succeed();
    }
}
