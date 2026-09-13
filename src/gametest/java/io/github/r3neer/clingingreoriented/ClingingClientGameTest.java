package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class ClingingClientGameTest implements FabricClientGameTest {
    public static final java.util.List<net.minecraft.sounds.SoundEvent> SOUNDS=new java.util.concurrent.CopyOnWriteArrayList<>();
    private static void tapJump(ClientGameTestContext context){context.getInput().holdKey(options->options.keyJump);context.waitTicks(2);context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);}
    private static Vec3 cameraForward(net.minecraft.client.Minecraft mc){
        var forward=mc.gameRenderer.mainCamera().rotation().transform(new org.joml.Vector3f(0,0,-1));
        return new Vec3(forward.x,forward.y,forward.z).normalize();
    }
    @Override public void runTest(ClientGameTestContext context){
        SOUNDS.clear();
        try(var world=context.worldBuilder().create()){
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-5,79,-5),new BlockPos(5,86,5)))level.setBlockAndUpdate(pos,pos.getY()==79?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                var p=server.getPlayerList().getPlayers().getFirst();p.setGameMode(GameType.SURVIVAL);
                p.teleport(new TeleportTransition(level,new Vec3(.5,82,.5),Vec3.ZERO,-90,0,TeleportTransition.DO_NOTHING));
                p.setNoGravity(true);
                p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow(),1200));
            });
            context.waitFor(mc->mc.player!=null && ClingingReoriented.hasEffect(mc.player) && Math.abs(mc.player.getY()-82)<.1);
            context.waitTicks(3);
            var firstView=new AtomicReference<Vec3>();context.runOnClient(mc->firstView.set(cameraForward(mc)));
            context.getInput().holdKey(options->options.keyJump);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.EAST);
            context.waitFor(mc->VisualTransitions.holding(mc.player));
            long first=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            context.waitTicks(30);
            long held=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).lastRequest);
            if(first<0 || held!=first)throw new AssertionError("Held jump produced repeated requests");
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);
            context.runOnClient(mc->{
                if(cameraForward(mc).distanceTo(firstView.get())>2.0E-3)throw new AssertionError("Free-flight gravity turn moved the rendered camera");
                if(!VisualTransitions.holding(mc.player))throw new AssertionError("Open-air EAST turn did not retain the visual frame");
            });
            if(!SOUNDS.contains(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME))throw new AssertionError("Missing success sound");
            context.takeScreenshot("gamefeel-held-east-open-air");

            // Once Clinging's one-turn budget is spent, a precisely requested DOWN turn is rejected.
            var spentResult=world.getServer().computeOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                return ClingingReoriented.attempt(p,new Vec3(0,-1,0),GravityTransition.headingFromYaw(Direction.EAST,p.getYRot()));
            });
            if(spentResult!=ClingingReoriented.Result.BLOCKED)throw new AssertionError("Spent Clinging accepted a second airborne turn: "+spentResult);
            context.runOnClient(mc->{if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("Spent Clinging returned to DOWN voluntarily");});
            boolean stillSpent=world.getServer().computeOnServer(server->ClingingReoriented.data(server.getPlayerList().getPlayers().getFirst()).airChangeUsed);
            if(!stillSpent)throw new AssertionError("Rejected DOWN turn refunded Clinging charge");

            // Reorientation removes the budget. Drive the authoritative request with an exact world-space
            // direction so this test measures camera ownership rather than a physical-frame look conversion.
            world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().addEffect(new MobEffectInstance(Reorientation.EFFECT,1200)));
            context.waitFor(mc->mc.player.hasEffect(Reorientation.EFFECT));
            var beforeDown=new AtomicReference<Vec3>();context.runOnClient(mc->beforeDown.set(cameraForward(mc)));
            var downResult=world.getServer().computeOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                return ClingingReoriented.attempt(p,new Vec3(0,-1,0),GravityTransition.headingFromYaw(Direction.EAST,p.getYRot()));
            });
            if(downResult!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("Reorientation DOWN request failed: "+downResult);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.DOWN);
            context.waitFor(mc->VisualTransitions.holding(mc.player));
            context.runOnClient(mc->{if(cameraForward(mc).distanceTo(beforeDown.get())>2.0E-3)throw new AssertionError("Open-air Reorientation DOWN moved camera before landing");});
            context.takeScreenshot("gamefeel-held-down-open-air");

            // A second authoritative airborne turn also changes physics without accumulating camera roll.
            var beforeSouth=new AtomicReference<Vec3>();context.runOnClient(mc->beforeSouth.set(cameraForward(mc)));
            var southResult=world.getServer().computeOnServer(server->{
                var p=server.getPlayerList().getPlayers().getFirst();
                return ClingingReoriented.attempt(p,new Vec3(0,0,1),GravityTransition.headingFromYaw(Direction.DOWN,p.getYRot()));
            });
            if(southResult!=ClingingReoriented.Result.SUCCESS)throw new AssertionError("Reorientation SOUTH request failed: "+southResult);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.SOUTH);
            context.waitFor(mc->VisualTransitions.holding(mc.player));
            context.runOnClient(mc->{if(cameraForward(mc).distanceTo(beforeSouth.get())>2.0E-3)throw new AssertionError("Second open-air Reorientation turn accumulated a camera snap");});
            world.getServer().runOnServer(server->server.getPlayerList().getPlayers().getFirst().removeAllEffects());
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.DOWN);
            context.waitFor(mc->Math.abs(((com.moigferdsrte.gravitychanger.client.GravityAnimationEntity)mc.player).gravitychanger$getVisualGravityRotation(Direction.DOWN).dot(com.moigferdsrte.gravitychanger.util.RotationUtil.getEntityRotationQuaternion(Direction.DOWN)))>.99999f);
            context.takeScreenshot("clinging-restored-down");

            // Real client integration: jump, choose a wall five blocks away, fall,
            // acquire it only shortly before contact, finish the landing snap at the wall,
            // then jump away in the new physical frame.
            world.getServer().runOnServer(server->{
                var level=server.overworld();var p=server.getPlayerList().getPlayers().getFirst();
                for(var pos:BlockPos.betweenClosed(new BlockPos(-5,79,-5),new BlockPos(6,96,5)))level.setBlockAndUpdate(pos,pos.getY()==79 || pos.getX()==6?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState());
                p.teleport(new TeleportTransition(level,new Vec3(1,80,.5),Vec3.ZERO,-90,0,TeleportTransition.DO_NOTHING));
                p.setNoGravity(false);p.setDeltaMovement(Vec3.ZERO);
                p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow(),1200));
                var s=ClingingReoriented.data(p);s.airChangeUsed=false;s.clearLandingCommit();s.visualBaseKnown=true;s.visualBaseDirection=Direction.DOWN;s.freeFlightVisualHeld=false;
            });
            context.waitFor(mc->mc.player.onGround() && Math.abs(mc.player.getX()-1)<.1 && ClingingReoriented.hasEffect(mc.player));
            context.waitTicks(3);
            context.getInput().holdKey(options->options.keyJump);
            context.waitFor(mc->mc.player.getY()>80.65);
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);
            var beforeWallTurn=new AtomicReference<Vec3>();context.runOnClient(mc->beforeWallTurn.set(cameraForward(mc)));
            tapJump(context);
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.EAST);
            context.waitFor(mc->VisualTransitions.holding(mc.player));
            context.takeScreenshot("gamefeel-wall-approach-held");
            context.waitFor(mc->mc.player.onGround() && mc.player.getX()>5.9);
            context.waitFor(mc->Math.abs(((com.moigferdsrte.gravitychanger.client.GravityAnimationEntity)mc.player).gravitychanger$getVisualGravityRotation(Direction.EAST).dot(com.moigferdsrte.gravitychanger.util.RotationUtil.getEntityRotationQuaternion(Direction.EAST)))>.9999f);
            context.runOnClient(mc->{if(mc.player.getBoundingBox().maxX>6.00001)throw new AssertionError("Client penetrated five-metre wall");});
            context.takeScreenshot("gamefeel-wall-touchdown-east");
            context.waitTicks(90);
            context.getInput().holdKey(options->options.keyJump);
            context.waitFor(mc->mc.player.getX()<5.7);
            context.getInput().releaseKey(options->options.keyJump);context.waitTicks(2);
            context.runOnClient(mc->{if(GravityDirectionUtil.getOwnGravityDirection(mc.player)!=Direction.EAST)throw new AssertionError("Jump detached gravity");});
            context.takeScreenshot("clinging-five-metre-wall-jump");
            world.getServer().runOnServer(server->{var p=server.getPlayerList().getPlayers().getFirst();p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.removeAllEffects();});
            context.waitFor(mc->GravityDirectionUtil.getOwnGravityDirection(mc.player)==Direction.DOWN);
            if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("firstperson")) {
                world.getServer().runCommand("fill -30 100 -10 -10 120 10 air");
                world.getServer().runCommand("tp @a -20 110 0 0 80");
                world.getServer().runCommand("clear @a");
                context.waitTicks(10);
                try { Class.forName("io.github.r3neer.clingingreoriented.FirstPersonChecks").getMethod("run", ClientGameTestContext.class).invoke(null, context); }
                catch (ReflectiveOperationException e) { throw new AssertionError("First Person integration check failed", e); }
            }
        }
    }
}