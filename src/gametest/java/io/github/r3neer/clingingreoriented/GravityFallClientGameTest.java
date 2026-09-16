package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** S04/S05 visual campaign: every screenshot has a neighboring numerical invariant. */
public final class GravityFallClientGameTest implements FabricClientGameTest {
    private static final double VEC_EPS=3.0E-3D;
    private static final float QUAT_EPS=2.0E-4F;
    private static final double BODY_STEP_EPS=Math.toRadians(.5D);
    private static final String FA_PACK="FreshAnimations_v1.10.5.zip";
    private static final String FA_PLAYER_PACK="FA+Player-v1.1.zip";

    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(10_000L);
        AtomicReference<Vec3> cameraStart=new AtomicReference<>();
        AtomicReference<Quaternionf> curvedBody=new AtomicReference<>();
        FreshAnimationsFixture fresh=FreshAnimationsFixture.enableIfPresent(context);

        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-8,79,-8),new BlockPos(8,79,8)))
                    level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                var player=server.getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.SURVIVAL);
                player.teleport(new TeleportTransition(level,new Vec3(.5,84,.5),Vec3.ZERO,180.0F,0.0F,TeleportTransition.DO_NOTHING));
                player.setNoGravity(true);player.setDeltaMovement(Vec3.ZERO);
            });
            context.waitFor(mc->mc.player!=null&&Math.abs(mc.player.getY()-84.0D)<.25D);
            context.runOnClient(mc->{
                GravityFallVisuals.clear();
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.player.setDeltaMovement(Vec3.ZERO);

                // Entity ids are reusable. A packet for the old UUID must never attach to the new
                // entity merely because its numerical id now matches.
                GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                    mc.player.getId(),java.util.UUID.randomUUID(),GravityFallSync.Phase.START.ordinal(),-1,0.0F,999L));
                if(GravityFallVisuals.active(mc.player))throw new AssertionError("stale UUID attached Gravity Fall to a reused entity id");
                GravityFallVisuals.clear();
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                cameraStart.set(cameraForward(mc));
                if(GravityFallVisuals.active(mc.player))throw new AssertionError("Gravity Fall active before START");
                if(GravityFallVisuals.body(mc.player,0.0F)!=null)throw new AssertionError("Inactive player already has a Gravity Fall body quaternion");
                fresh.assertStillSelected(mc);
            });
            context.takeScreenshot(fresh.active()?"fresh-animations-gravity-fall-pre-start":"gravity-fall-pre-start");

            context.runOnClient(mc->{
                mc.player.setDeltaMovement(new Vec3(0,-.20,0));
                send(mc,sequence,GravityFallSync.Phase.START,null,0.0F);
                Quaternionf start=body(mc);
                assertQuat(VisualTransitionsForTest.visual(mc),start,"START did not capture displayed body frame");
                // A delayed packet from the same UUID but an older connection epoch must not undo
                // the new START. Sequence ordering is the second half of the respawn/tracking fence.
                GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
                    mc.player.getId(),mc.player.getUUID(),GravityFallSync.Phase.RESET.ordinal(),-1,0.0F,sequence.get()-1));
                if(!GravityFallVisuals.active(mc.player))throw new AssertionError("stale RESET overrode a newer Gravity Fall START");
                mc.player.setDeltaMovement(Vec3.ZERO);
            });
            context.takeScreenshot("gravity-fall-blend-start");
            context.runOnClient(mc->{
                advance(mc,2,new Vec3(0,-.20,0));
                float progress=GravityFallVisuals.blendProgress(mc.player,0.0F);
                if(progress<=0.0F||progress>=1.0F)throw new AssertionError("Expected in-progress 6-tick blend, got "+progress);
                Vec3 up=BodyOrientation.bodyUp(body(mc));
                if(Math.abs(up.y)>.98D)throw new AssertionError("Mid-blend body already looks fully canonical/velocity-aligned: "+up);
            });
            context.takeScreenshot("gravity-fall-blend-mid");
            context.runOnClient(mc->{
                advance(mc,8,new Vec3(0,-.20,0));
                assertVec(new Vec3(0,-1,0),BodyOrientation.bodyUp(body(mc)),"sustained DOWN velocity body axis");
                assertCamera(cameraStart.get(),mc,"body blend fed back into camera");
                if(GravityFallVisuals.extraRoot(mc.player,0.0F)==null)throw new AssertionError("active Gravity Fall has no avatar root transform");
                fresh.assertStillSelected(mc);
            });
            context.takeScreenshot(fresh.active()?"fresh-animations-gravity-fall-sustained-down":"gravity-fall-sustained-down");

            context.runOnClient(mc->{
                Quaternionf before=body(mc);
                advance(mc,1,new Vec3(.20,0,0));
                Quaternionf after=body(mc);curvedBody.set(new Quaternionf(after));
                assertFiniteNormalized(after,"90-degree velocity curve body");
                assertBoundedBodyStep(before,after,"90-degree velocity curve snapped body");
                Vec3 up=BodyOrientation.bodyUp(after);
                if(up.x<=0.0D||up.y>=-.90D)
                    throw new AssertionError("90-degree velocity curve did not begin gradual EAST weathercock without snapping: "+up);
                assertCamera(cameraStart.get(),mc,"90-degree body curve moved camera");
            });
            context.takeScreenshot("gravity-fall-curve-east");

            context.runOnClient(mc->{
                advance(mc,5,Vec3.ZERO);
                assertFiniteNormalized(body(mc),"zero-speed body state");
                assertCamera(cameraStart.get(),mc,"zero-speed hold moved camera");
            });
            context.takeScreenshot("gravity-fall-zero-hold");
            context.runOnClient(mc->{
                Quaternionf before=body(mc);
                advance(mc,1,new Vec3(-.20,0,0));
                Quaternionf after=body(mc);
                assertFiniteNormalized(after,"velocity reversal body");
                assertBoundedBodyStep(before,after,"velocity reversal manufactured an instantaneous head/feet flip");
                if(equivalent(after,RotationUtil.getEntityRotationQuaternion(Direction.WEST)))
                    throw new AssertionError("velocity reversal snapped body to canonical WEST instead of preserving attitude");
                assertCamera(cameraStart.get(),mc,"velocity reversal moved camera");
            });
            context.takeScreenshot("gravity-fall-reverse-west");

            // BODY_LANDING entry must begin exactly from the currently visible body.
            context.runOnClient(mc->{
                Quaternionf before=body(mc);
                mc.player.setDeltaMovement(Vec3.ZERO);
                send(mc,sequence,GravityFallSync.Phase.LAND,Direction.DOWN,4.0F);
                if(!GravityFallVisuals.landing(mc.player))throw new AssertionError("LAND did not enter BODY_LANDING");
                assertQuat(before,body(mc),"LAND snapped away from current Gravity Fall body at entry");
            });
            context.takeScreenshot("gravity-fall-landing-begin");

            // The screenshot above is allowed to consume real client ticks. Resume from whatever
            // is visible now, drive the persistent body for a few ticks, then create a fresh LAND
            // epoch for a deterministic one-tick midpoint assertion before screenshots can advance it.
            context.runOnClient(mc->{
                Quaternionf beforeResume=body(mc);
                send(mc,sequence,GravityFallSync.Phase.RESUME,null,0.0F);
                if(GravityFallVisuals.landing(mc.player))throw new AssertionError("post-begin RESUME left BODY_LANDING active");
                assertQuat(beforeResume,body(mc),"post-begin RESUME snapped current body");
                Quaternionf beforeDrive=body(mc);
                advance(mc,8,new Vec3(-.20,0,0));
                Quaternionf afterDrive=body(mc);
                assertFiniteNormalized(afterDrive,"persistent body before midpoint landing");
                if(equivalent(beforeDrive,afterDrive))throw new AssertionError("RESUME did not restart persistent body attitude integration");

                mc.player.setDeltaMovement(Vec3.ZERO);
                Quaternionf landStart=body(mc);
                send(mc,sequence,GravityFallSync.Phase.LAND,Direction.DOWN,4.0F);
                GravityFallVisuals.tick(mc);
                Quaternionf mid=body(mc);
                Quaternionf target=RotationUtil.getEntityRotationQuaternion(Direction.DOWN);
                if(equivalent(mid,target)||equivalent(mid,landStart))throw new AssertionError("BODY_LANDING one-tick midpoint collapsed to an endpoint");
            });
            context.takeScreenshot(fresh.active()?"fresh-animations-gravity-fall-landing-mid":"gravity-fall-landing-mid");

            // Separate fresh partial-LAND epoch for the RESUME continuity invariant. Keeping LAND
            // and RESUME in one client callback prevents screenshot/render ticks from changing the
            // reference frame between the two samples.
            context.runOnClient(mc->{
                Quaternionf current=body(mc);
                send(mc,sequence,GravityFallSync.Phase.RESUME,null,0.0F);
                assertQuat(current,body(mc),"midpoint cleanup RESUME snapped current body");
                advance(mc,8,new Vec3(-.20,0,0));
                mc.player.setDeltaMovement(Vec3.ZERO);
                Quaternionf landStart=body(mc);
                send(mc,sequence,GravityFallSync.Phase.LAND,Direction.DOWN,4.0F);
                GravityFallVisuals.tick(mc);
                Quaternionf beforeResume=body(mc);
                if(equivalent(beforeResume,RotationUtil.getEntityRotationQuaternion(Direction.DOWN))||equivalent(beforeResume,landStart))
                    throw new AssertionError("RESUME holdout did not reach a partial landing state");
                send(mc,sequence,GravityFallSync.Phase.RESUME,null,0.0F);
                if(GravityFallVisuals.landing(mc.player))throw new AssertionError("RESUME left BODY_LANDING active");
                assertQuat(beforeResume,body(mc),"RESUME snapped instead of continuing from current partial presentation");
                Quaternionf beforeDrive=body(mc);
                advance(mc,8,new Vec3(-.20,0,0));
                Quaternionf afterDrive=body(mc);
                assertFiniteNormalized(afterDrive,"RESUME persistent body state");
                if(equivalent(beforeDrive,afterDrive))throw new AssertionError("RESUME did not return to persistent body attitude integration");
            });
            context.takeScreenshot("gravity-fall-resume-west");

            context.runOnClient(mc->{
                mc.player.setDeltaMovement(Vec3.ZERO);
                send(mc,sequence,GravityFallSync.Phase.LAND,Direction.DOWN,4.0F);
                advance(mc,6,Vec3.ZERO);
                assertQuat(RotationUtil.getEntityRotationQuaternion(Direction.DOWN),body(mc),"BODY_LANDING did not finish at canonical future-floor frame");
                assertCamera(cameraStart.get(),mc,"landing body snap fed back into camera");
                fresh.assertStillSelected(mc);
            });
            context.takeScreenshot(fresh.active()?"fresh-animations-gravity-fall-landing-final":"gravity-fall-landing-final");

            context.runOnClient(mc->{
                send(mc,sequence,GravityFallSync.Phase.RESET,null,0.0F);
                if(GravityFallVisuals.active(mc.player))throw new AssertionError("RESET left Gravity Fall root active");
                if(GravityFallVisuals.body(mc.player,0.0F)!=null)throw new AssertionError("RESET left a stale body quaternion");
                assertCamera(cameraStart.get(),mc,"RESET moved camera");
            });
            context.takeScreenshot("gravity-fall-touchdown-reset");

            context.runOnClient(mc->{GravityFallVisuals.clear();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setDeltaMovement(Vec3.ZERO);});
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);});
        }finally{
            fresh.restore(context);
        }
    }

    private static void send(Minecraft mc,AtomicLong sequence,GravityFallSync.Phase phase,Direction direction,float eta){
        GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
            mc.player.getId(),mc.player.getUUID(),phase.ordinal(),direction==null?-1:direction.get3DDataValue(),eta,sequence.incrementAndGet()
        ));
    }

    private static void advance(Minecraft mc,int ticks,Vec3 velocity){
        for(int i=0;i<ticks;i++){
            mc.player.setDeltaMovement(velocity);
            GravityFallVisuals.tick(mc);
        }
        mc.player.setDeltaMovement(Vec3.ZERO);
    }

    private static Quaternionf body(Minecraft mc){
        Quaternionf body=GravityFallVisuals.body(mc.player,0.0F);
        if(body==null)throw new AssertionError("missing active Gravity Fall body quaternion");
        return body;
    }

    private static Vec3 cameraForward(Minecraft mc){
        Vector3f forward=mc.gameRenderer.mainCamera().rotation().transform(new Vector3f(0,0,-1));
        return new Vec3(forward.x,forward.y,forward.z).normalize();
    }

    private static void assertCamera(Vec3 expected,Minecraft mc,String label){assertVec(expected,cameraForward(mc),label);}

    private static void assertVec(Vec3 expected,Vec3 actual,String label){
        if(expected.distanceTo(actual)>VEC_EPS)throw new AssertionError(label+": expected "+expected+" got "+actual);
    }

    private static void assertQuat(Quaternionf expected,Quaternionf actual,String label){
        if(!equivalent(expected,actual))throw new AssertionError(label+": expected "+expected+" got "+actual);
    }

    private static void assertFiniteNormalized(Quaternionf value,String label){
        if(value==null||!Float.isFinite(value.x())||!Float.isFinite(value.y())||!Float.isFinite(value.z())||!Float.isFinite(value.w()))
            throw new AssertionError(label+" is non-finite: "+value);
        float lengthSquared=value.x()*value.x()+value.y()*value.y()+value.z()*value.z()+value.w()*value.w();
        if(Math.abs(lengthSquared-1.0F)>1.0E-3F)throw new AssertionError(label+" is not normalized: length^2="+lengthSquared+" value="+value);
    }

    private static void assertBoundedBodyStep(Quaternionf before,Quaternionf after,String label){
        assertFiniteNormalized(before,label+" before");
        assertFiniteNormalized(after,label+" after");
        Quaternionf a=new Quaternionf(before).normalize(),b=new Quaternionf(after).normalize();
        double dot=Math.max(-1.0D,Math.min(1.0D,Math.abs(a.dot(b))));
        double radians=2.0D*Math.acos(dot);
        double max=GravityFallAerodynamics.MAX_STABILIZE_RADIANS_PER_TICK+GravityFallAerodynamics.MAX_FOLLOW_RADIANS_PER_TICK+BODY_STEP_EPS;
        if(radians>max)throw new AssertionError(label+": rotated "+Math.toDegrees(radians)+" degrees in one tick; max="+Math.toDegrees(max));
    }

    private static boolean equivalent(Quaternionf a,Quaternionf b){
        if(a==null||b==null)return false;
        Quaternionf qa=new Quaternionf(a).normalize(),qb=new Quaternionf(b).normalize();
        return Math.abs(Math.abs(qa.dot(qb))-1.0F)<QUAT_EPS;
    }

    private static final class VisualTransitionsForTest {
        private static Quaternionf visual(Minecraft mc){return io.github.r3neer.clingingreoriented.client.VisualTransitions.current(mc.player);}
    }

    /** Activates the exact VanillaPlus-26.2 FA fixture only when EMF is present in the lane. */
    private static final class FreshAnimationsFixture {
        private final boolean active;
        private final AtomicReference<List<String>> previous=new AtomicReference<>();
        private final AtomicReference<CompletableFuture<Void>> reload=new AtomicReference<>();
        private final AtomicReference<String> freshId=new AtomicReference<>();
        private final AtomicReference<String> playerId=new AtomicReference<>();

        private FreshAnimationsFixture(boolean active){this.active=active;}
        private boolean active(){return active;}

        private static FreshAnimationsFixture enableIfPresent(ClientGameTestContext context){
            boolean emf=FabricLoader.getInstance().isModLoaded("entity_model_features");
            if(!emf)return new FreshAnimationsFixture(false);
            if(!FabricLoader.getInstance().isModLoaded("entity_texture_features"))
                throw new AssertionError("Fresh Animations lane loaded EMF without ETF");
            var fixture=new FreshAnimationsFixture(true);
            context.runOnClient(mc->{
                var repo=mc.getResourcePackRepository();repo.reload();
                fixture.previous.set(List.copyOf(repo.getSelectedIds()));
                fixture.freshId.set(findPack(repo.getAvailableIds(),FA_PACK));
                fixture.playerId.set(findPack(repo.getAvailableIds(),FA_PLAYER_PACK));
                var selected=new ArrayList<>(fixture.previous.get());
                if(!selected.contains(fixture.freshId.get()))selected.add(fixture.freshId.get());
                if(!selected.contains(fixture.playerId.get()))selected.add(fixture.playerId.get());
                repo.setSelected(selected);
                fixture.reload.set(mc.reloadResourcePacks());
            });
            context.waitFor(mc->fixture.reload.get()!=null&&fixture.reload.get().isDone()&&!fixture.reload.get().isCompletedExceptionally());
            context.waitFor(mc->mc.gui.overlay()==null);
            context.waitTicks(20);
            context.runOnClient(fixture::assertStillSelected);
            return fixture;
        }

        private static String findPack(java.util.Collection<String> available,String fileName){
            return available.stream().filter(id->id.toLowerCase(java.util.Locale.ROOT).contains(fileName.toLowerCase(java.util.Locale.ROOT)))
                .findFirst().orElseThrow(()->new AssertionError("Fresh Animations fixture not discovered in resourcepacks: "+fileName+" available="+available));
        }

        private void assertStillSelected(Minecraft mc){
            if(!active)return;
            var selected=mc.getResourcePackRepository().getSelectedIds();
            if(!selected.contains(freshId.get())||!selected.contains(playerId.get()))
                throw new AssertionError("Fresh Animations packs stopped being selected: "+selected);
        }

        private void restore(ClientGameTestContext context){
            if(!active||previous.get()==null)return;
            context.runOnClient(mc->{
                mc.getResourcePackRepository().setSelected(previous.get());
                reload.set(mc.reloadResourcePacks());
            });
            context.waitFor(mc->reload.get()!=null&&reload.get().isDone()&&!reload.get().isCompletedExceptionally());
            context.waitFor(mc->mc.gui.overlay()==null);
        }
    }
}