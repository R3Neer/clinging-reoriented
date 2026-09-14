package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.RotationUtil;
import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
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

/** S04 visual campaign: every screenshot has a neighboring numerical invariant. */
public final class GravityFallClientGameTest implements FabricClientGameTest {
    private static final double VEC_EPS=3.0E-3D;
    private static final float QUAT_EPS=2.0E-4F;

    @Override public void runTest(ClientGameTestContext context){
        AtomicLong sequence=new AtomicLong(10_000L);
        AtomicReference<Vec3> cameraStart=new AtomicReference<>();
        AtomicReference<Quaternionf> eastBody=new AtomicReference<>();
        AtomicReference<Quaternionf> westBody=new AtomicReference<>();
        AtomicReference<Quaternionf> partialLanding=new AtomicReference<>();

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
            });
            context.waitTicks(2);
            context.runOnClient(mc->{
                cameraStart.set(cameraForward(mc));
                if(GravityFallVisuals.active(mc.player))throw new AssertionError("Gravity Fall active before START");
                if(GravityFallVisuals.body(mc.player,0.0F)!=null)throw new AssertionError("Inactive player already has a Gravity Fall body quaternion");
            });
            context.takeScreenshot("gravity-fall-pre-start");

            // START: begin from the frame actually being displayed, then blend toward head-first DOWN.
            context.runOnClient(mc->{
                mc.player.setDeltaMovement(new Vec3(0,-.20,0));
                send(mc,sequence,GravityFallSync.Phase.START,null,0.0F);
                Quaternionf start=body(mc);
                assertQuat(VisualTransitionsForTest.visual(mc),start,"START did not capture displayed body frame");
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
            });
            context.takeScreenshot("gravity-fall-sustained-down");

            // A trajectory curve rotates the body only when velocity itself changes.
            context.runOnClient(mc->{
                advance(mc,1,new Vec3(.20,0,0));
                Quaternionf body=body(mc);eastBody.set(new Quaternionf(body));
                assertVec(new Vec3(1,0,0),BodyOrientation.bodyUp(body),"90-degree velocity curve");
                assertCamera(cameraStart.get(),mc,"90-degree body curve moved camera");
            });
            context.takeScreenshot("gravity-fall-curve-east");

            // Reserved adversarial holdout: EAST -> zero -> WEST.
            context.runOnClient(mc->{
                advance(mc,5,Vec3.ZERO);
                assertQuat(eastBody.get(),body(mc),"zero-speed crossing changed body twist/orientation");
                assertCamera(cameraStart.get(),mc,"zero-speed hold moved camera");
            });
            context.takeScreenshot("gravity-fall-zero-hold");
            context.runOnClient(mc->{
                advance(mc,1,new Vec3(-.20,0,0));
                Quaternionf west=body(mc);westBody.set(new Quaternionf(west));
                assertVec(new Vec3(-1,0,0),BodyOrientation.bodyUp(west),"WEST direction after zero crossing");
                assertCamera(cameraStart.get(),mc,"180-degree velocity reversal moved camera");
            });
            context.takeScreenshot("gravity-fall-reverse-west");

            // BODY_LANDING captures the currently rendered horizontal body, not the camera/gravity frame.
            context.runOnClient(mc->{
                Quaternionf before=body(mc);
                mc.player.setDeltaMovement(Vec3.ZERO);
                send(mc,sequence,GravityFallSync.Phase.LAND,Direction.DOWN,4.0F);
                if(!GravityFallVisuals.landing(mc.player))throw new AssertionError("LAND did not enter BODY_LANDING");
                assertQuat(before,body(mc),"LAND snapped away from current Gravity Fall body at entry");
            });
            context.takeScreenshot("gravity-fall-landing-begin");
            context.runOnClient(mc->{
                advance(mc,2,Vec3.ZERO);
                Quaternionf mid=body(mc);partialLanding.set(new Quaternionf(mid));
                Quaternionf target=RotationUtil.getEntityRotationQuaternion(Direction.DOWN);
                if(equivalent(mid,target)||equivalent(mid,westBody.get()))throw new AssertionError("BODY_LANDING midpoint collapsed to an endpoint");
            });
            context.takeScreenshot("gravity-fall-landing-mid");

            // Invalidation resumes from the body quaternion currently visible, with no snap-back.
            context.runOnClient(mc->{
                mc.player.setDeltaMovement(new Vec3(-.20,0,0));
                send(mc,sequence,GravityFallSync.Phase.RESUME,null,0.0F);
                if(GravityFallVisuals.landing(mc.player))throw new AssertionError("RESUME left BODY_LANDING active");
                assertQuat(partialLanding.get(),body(mc),"RESUME snapped instead of continuing from current presentation");
                advance(mc,8,new Vec3(-.20,0,0));
                assertVec(new Vec3(-1,0,0),BodyOrientation.bodyUp(body(mc)),"RESUME did not return to velocity transport");
            });
            context.takeScreenshot("gravity-fall-resume-west");

            // A valid touchdown preparation reaches canonical DOWN within the ETA/capped window.
            context.runOnClient(mc->{
                mc.player.setDeltaMovement(Vec3.ZERO);
                send(mc,sequence,GravityFallSync.Phase.LAND,Direction.DOWN,4.0F);
                advance(mc,6,Vec3.ZERO);
                assertQuat(RotationUtil.getEntityRotationQuaternion(Direction.DOWN),body(mc),"BODY_LANDING did not finish at canonical future-floor frame");
                assertCamera(cameraStart.get(),mc,"landing body snap fed back into camera");
            });
            context.takeScreenshot("gravity-fall-landing-final");

            context.runOnClient(mc->{
                send(mc,sequence,GravityFallSync.Phase.RESET,null,0.0F);
                if(GravityFallVisuals.active(mc.player))throw new AssertionError("RESET left Gravity Fall root active");
                if(GravityFallVisuals.body(mc.player,0.0F)!=null)throw new AssertionError("RESET left a stale body quaternion");
                assertCamera(cameraStart.get(),mc,"RESET moved camera");
            });
            context.takeScreenshot("gravity-fall-touchdown-reset");

            context.runOnClient(mc->{GravityFallVisuals.clear();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setDeltaMovement(Vec3.ZERO);});
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setDeltaMovement(Vec3.ZERO);p.setNoGravity(true);});
        }
    }

    private static void send(Minecraft mc,AtomicLong sequence,GravityFallSync.Phase phase,Direction direction,float eta){
        GravityFallVisuals.receive(mc,new GravityFallSync.Visual(
            mc.player.getId(),mc.player.getUUID(),phase.ordinal(),direction==null?-1:direction.get3DDataValue(),eta,sequence.incrementAndGet()
        ));
    }

    /** Advance presentation deterministically without moving the actual player fixture. */
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

    private static boolean equivalent(Quaternionf a,Quaternionf b){
        if(a==null||b==null)return false;
        Quaternionf qa=new Quaternionf(a).normalize(),qb=new Quaternionf(b).normalize();
        return Math.abs(Math.abs(qa.dot(qb))-1.0F)<QUAT_EPS;
    }

    /** Keep the test independent of GravityFallVisuals internals while sampling the real displayed gravity frame. */
    private static final class VisualTransitionsForTest {
        private static Quaternionf visual(Minecraft mc){return io.github.r3neer.clingingreoriented.client.VisualTransitions.current(mc.player);}
    }
}
