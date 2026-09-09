package io.github.r3neer.clingingreoriented;

import com.github.alexthe666.alexsmobs.effect.EffectClinging;
import com.moigferdsrte.gravitychanger.util.*;
import com.moigferdsrte.gravitychanger.init.ModAttributes;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class ClingingGameTests {
    /** Runs only with the new coordinated Scale artifact; old optional dependency stays loadable. */
    @GameTest(padding=24) public void sharedAnatomyBridgeUsesPiecesAndUpstreamGravity(GameTestHelper h) throws Exception {
        Class<?> core;
        try { core=Class.forName("io.github.r3neer.scalebrews.platform.anatomy.AnatomyMovement"); }
        catch(ClassNotFoundException oldScale) { h.succeed(); return; }
        var levelType=net.minecraft.world.level.Level.class;
        var p=player(h);
        var cow=h.spawn(EntityTypes.COW,new BlockPos(3,7,3));cow.setNoAi(true);cow.setNoGravity(true);
        cow.getAttribute(Attributes.SCALE).setBaseValue(6);cow.refreshDimensions();
        h.assertTrue(ScaleBridge.eligible(p,cow),"fixture respects existing eligibility");
        core.getMethod("activate",levelType).invoke(null,h.getLevel());
        try {
            h.assertTrue(AnatomyBridge.active(p),"optional bridge resolves new core");
            var frames=Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GravityFrames");
            var frameType=Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GravityFrame");
            for(Direction direction:Direction.values()) {
                ClingingReoriented.write(p,direction);
                var frame=frames.getMethod("get",Entity.class).invoke(null,p);
                h.assertTrue(frameType.getMethod("down").invoke(frame)==direction,"upstream gravity adapter "+direction);
            }
            ClingingReoriented.write(p,Direction.DOWN);
            var center=cow.position().add(0,2,0);
            var solid=new AABB(center.x-.5,center.y-.5,center.z-.5,center.x+.5,center.y+.5,center.z+.5);
            var boxType=Class.forName("io.github.r3neer.scalebrews.platform.anatomy.ConvexBox");
            var vertices=new java.util.ArrayList<Vec3>();
            for(int i=0;i<8;i++)vertices.add(new Vec3((i&1)==0?solid.minX:solid.maxX,(i&2)==0?solid.minY:solid.maxY,(i&4)==0?solid.minZ:solid.maxZ));
            var piece=boxType.getConstructor(java.util.List.class).newInstance(vertices);
            var providerType=Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GeometryProvider");
            var snapshotType=Class.forName("io.github.r3neer.scalebrews.platform.anatomy.GeometryProvider$Snapshot");
            var snapshot=snapshotType.getConstructor(long.class,java.util.Map.class).newInstance(1L,java.util.Map.of("body",piece));
            var provider=java.lang.reflect.Proxy.newProxyInstance(providerType.getClassLoader(),new Class<?>[]{providerType},(proxy,method,args)->{
                if(method.isDefault())return java.lang.reflect.InvocationHandler.invokeDefault(proxy,method,args);
                if(method.getName().equals("sample"))return java.util.Optional.of(snapshot);
                throw new UnsupportedOperationException(method.toString());
            });
            core.getMethod("register",LivingEntity.class,providerType).invoke(null,cow,provider);
            h.assertFalse(AnatomyBridge.spaceClear(p,solid.deflate(.1)),"actual anatomy obstructs clearance");
            var hole=solid.move(1.3,0,0).deflate(.2);
            h.assertTrue(cow.getBoundingBox().intersects(hole),"hole is inside legacy global box");
            h.assertTrue(AnatomyBridge.spaceClear(p,hole),"global box does not fill anatomical holes");
            var state=ClingingReoriented.data(p);state.owned=true;
            for(Direction direction:Direction.values()) {
                AnatomyBridge.clear(p);ClingingReoriented.write(p,direction);state.selected=direction;
                h.assertTrue(ClingingReoriented.controlsPhysics(p),"fixture exercises managed Clinging "+direction);
                p.setPos(center);p.setDeltaMovement(Vec3.ZERO);
                var down=io.github.r3neer.clingingreoriented.geometry.FaceGeometry.vector(direction);
                double extent=(p.getBoundingBox().max(direction.getAxis())-p.getBoundingBox().min(direction.getAxis()))*.5;
                var targetCenter=center.subtract(down.scale(1.5+extent));
                p.setPos(p.position().add(targetCenter.subtract(p.getBoundingBox().getCenter())));
                var requested=down.scale(3);
                var clipped=io.github.r3neer.scalebrews.platform.PlatformPhysics.collide(p,requested,requested);
                h.assertTrue(clipped.dot(down)<2,"Clinging no longer cancels anatomical collision "+direction);
                p.setPos(p.position().add(clipped));
                io.github.r3neer.scalebrews.platform.PlatformPhysics.afterMove(p);
                h.assertTrue(AnatomyBridge.supported(p),"real piece contact is shared "+direction);
                h.assertTrue(AnatomyBridge.parent(p)==cow,"shared contact identifies support "+direction);
                MovingSurface.afterMove(p);
                var before=p.position();cow.setPos(cow.position().add(.1,0,0));MovingSurface.carry(p);
                h.assertTrue(p.position().equals(before),"legacy carry does not run beside core "+direction);
                cow.setPos(cow.position().add(-.1,0,0));
                state.lastTransport=new Vec3(7,0,0);
                p.jumpFromGround();
                h.assertFalse(AnatomyBridge.supported(p),"jump releases actual contact "+direction);
                h.assertTrue(p.getDeltaMovement().length()<2,"jump does not inherit stale legacy transport "+direction);
            }
        } finally {
            core.getMethod("deactivate",levelType).invoke(null,h.getLevel());cow.discard();
            ClingingReoriented.write(p,Direction.DOWN);
        }
        h.succeed();
    }
    private static void anchorTick(ServerPlayer p){
        try {var m=com.moigferdsrte.gravitychanger.init.ModEvents.class.getDeclaredMethod("tickGravityAnchor",ServerPlayer.class);m.setAccessible(true);m.invoke(null,p);}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static void check(boolean condition,String message){if(!condition)throw new GameTestAssertException(Component.literal(message),0);}
    private static ServerPlayer player(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel(); p.snapTo(h.absoluteVec(new Vec3(2.5,3,2.5)));
        for(var pos:BlockPos.betweenClosed(p.blockPosition().offset(-8,-2,-8),p.blockPosition().offset(8,12,8)))h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
        p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow(),200));
        return p;
    }
    @GameTest(padding=24) public void actualAabbMatchesPreflightForThirtyTransitions(GameTestHelper h){
        var p=player(h);
        for(float scale:new float[]{.28f,1f,3.88f}){
            p.getAttribute(Attributes.SCALE).setBaseValue(scale);p.refreshDimensions();
            for(Direction from:Direction.values())for(Direction to:Direction.values()){
                if(from==to)continue;
                ClingingReoriented.write(p,from);
                var expected=RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),to,p.position());
                ClingingReoriented.write(p,to);
                check(expected.equals(p.getBoundingBox()),"preflight equals actual box "+from+" -> "+to+" scale "+scale);
                // setPos forces Gravity Changer's own makeBoundingBox injection.
                p.setPos(p.position());check(expected.equals(p.getBoundingBox()),"upstream refresh matches preflight");
            }
        }
        h.succeed();
    }
    @GameTest(padding=24) public void legacyTickDoesNotMutatePlayerPhysics(GameTestHelper h){
        var p=player(h);p.setNoGravity(true);p.fallDistance=7;p.setDeltaMovement(.1,.2,.3);
        var effect=(EffectClinging)BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow().value();
        check(effect.applyEffectTick(h.getLevel(),p,0),"effect kept");
        check(p.isNoGravity() && p.fallDistance==7 && p.getDeltaMovement().equals(new Vec3(.1,.2,.3)),"legacy physics untouched");
        check(!EffectClinging.isUpsideDown(p),"legacy flip disabled");h.succeed();
    }
    @GameTest(padding=24) public void blockSelectionPersistsAndExpires(GameTestHelper h){
        var p=player(h);var wall=p.blockPosition().east();
        h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        p.setYRot(-90);p.setXRot(0);
        check(ClingingReoriented.attempt(p)==ClingingReoriented.Result.SUCCESS,"wall selection");
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.EAST,"east gravity");
        p.setPos(p.position().add(-3,3,0));ClingingReoriented.reconcile(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.EAST,"air keeps gravity");
        p.removeAllEffects();ClingingReoriented.reconcile(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.DOWN,"expiry restores DOWN");h.succeed();
    }
    @GameTest(padding=24) public void rejectedClearanceDoesNotMutate(GameTestHelper h){
        var p=player(h);var wall=p.blockPosition().east();
        h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(p.blockPosition().west(),Blocks.STONE.defaultBlockState());
        p.setYRot(-90);p.setXRot(0);
        var before=p.position();var box=p.getBoundingBox();
        var result=ClingingReoriented.attempt(p);
        check(result==ClingingReoriented.Result.NO_SPACE,"selected wall rejected by rotated box: "+result);
        check(!ClingingReoriented.data(p).airChangeUsed,"blocked AABB does not spend charge");
        check(p.position().equals(before)&&p.getBoundingBox().equals(box)&&GravityDirectionUtil.getOwnGravityDirection(p)==Direction.DOWN,"failure has no mutation");h.succeed();
    }
    @GameTest(padding=24) public void foreignGravityIsPreserved(GameTestHelper h){
        var p=player(h);GravityDirectionUtil.setGravityDirection(p,Direction.NORTH);
        check(ClingingReoriented.attempt(p)==ClingingReoriented.Result.FOREIGN_GRAVITY,"cannot claim foreign direction");
        p.removeAllEffects();ClingingReoriented.reconcile(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.NORTH,"doesn't reset foreign source");h.succeed();
    }
    @GameTest(padding=24) public void scaleEligibilityAndEntityVolume(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
        check(ScaleBridge.eligible(p,cow)==io.github.r3neer.scalebrews.platform.Platforms.eligible(p,cow),"upstream eligibility exact");
        check(ScaleBridge.eligible(p,cow),"tiny player eligible");
        check(!ClingingReoriented.fits(p,cow.getBoundingBox(),cow),"surface interior never fits");
        p.getAttribute(Attributes.SCALE).setBaseValue(10);p.refreshDimensions();
        check(!ScaleBridge.eligible(p,cow),"giant player not eligible on cow");h.succeed();
    }
    @GameTest(padding=24) public void selectedEntityAddsPhysicalCollision(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
        MovingSurface.bind(p,cow);ClingingReoriented.data(p).owned=true;
        var shapes=p.level().getEntityCollisions(p,cow.getBoundingBox());
        check(shapes.stream().anyMatch(s->s.bounds().equals(cow.getBoundingBox())),"entity contributes real collider");h.succeed();
    }
    @GameTest(padding=24) public void surfaceTranslationAndAirborneSeparation(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
        p.setPos(cow.getX(),cow.getBoundingBox().maxY,cow.getZ());p.setDeltaMovement(Vec3.ZERO);
        MovingSurface.bind(p,cow);ClingingReoriented.data(p).owned=true;
        var before=p.position();cow.setPos(cow.position().add(.2,0,0));MovingSurface.carry(p);
        check(p.position().distanceTo(before.add(.2,0,0))<1e-6,"inherits translation once");
        MovingSurface.carry(p);check(p.position().distanceTo(before.add(.2,0,0))<1e-6,"no double carry");
        p.setPos(p.position().add(0,1,0));before=p.position();cow.setPos(cow.position().add(.2,0,0));MovingSurface.carry(p);
        check(p.position().equals(before),"airborne not dragged");check(ClingingReoriented.data(p).support!=null,"reference survives air");h.succeed();
    }
    @GameTest(padding=24) public void sixEntityFacesCollideAndTranslate(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,5,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
        for(Direction gravity:Direction.values()){
            var normal=gravity.getOpposite();var box=cow.getBoundingBox();var center=box.getCenter();
            double plane=normal.getAxisDirection()==Direction.AxisDirection.POSITIVE?box.max(normal.getAxis()):box.min(normal.getAxis());
            Vec3 n=io.github.r3neer.clingingreoriented.geometry.FaceGeometry.vector(normal);
            double x=normal.getAxis()==Direction.Axis.X?plane:center.x;
            double y=normal.getAxis()==Direction.Axis.Y?plane:center.y;
            double z=normal.getAxis()==Direction.Axis.Z?plane:center.z;
            ClingingReoriented.write(p,gravity);p.setPos(new Vec3(x,y,z).add(n.scale(.1)));
            var s=ClingingReoriented.data(p);s.owned=true;s.selected=gravity;MovingSurface.bind(p,cow);p.setDeltaMovement(Vec3.ZERO);
            p.move(MoverType.SELF,n.scale(-.5));
            check(io.github.r3neer.clingingreoriented.geometry.FaceGeometry.touching(p.getBoundingBox(),cow.getBoundingBox(),gravity),"solid entity face "+normal);
            Vec3 translation=normal.getAxis()==Direction.Axis.X?new Vec3(0,0,.2):new Vec3(.2,0,0);
            var before=p.position();cow.setPos(cow.position().add(translation));MovingSurface.carry(p);
            check(p.position().distanceTo(before.add(translation))<1e-6,"translation on "+normal);
            check(p.onGround(),"moving support remains jumpable on "+normal);
            MovingSurface.clear(p);
        }
        h.succeed();
    }
    @GameTest(padding=24) public void anchorCannotRestoreExpiredClinging(GameTestHelper h){
        var p=player(h);ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(com.moigferdsrte.gravitychanger.init.ModItems.GRAVITY_ANCHOR_NORTH));
        anchorTick(p);check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.NORTH && s.anchorBorrowed,"anchor takes temporary control");
        p.removeAllEffects();ClingingReoriented.reconcile(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.NORTH,"active anchor preserved");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);anchorTick(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.DOWN && !s.owned,"stale EAST not restored");h.succeed();
    }
    @GameTest(padding=24) public void foreignAnchorHistorySurvives(GameTestHelper h){
        var p=player(h);GravityDirectionUtil.setGravityDirection(p,Direction.WEST);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(com.moigferdsrte.gravitychanger.init.ModItems.GRAVITY_ANCHOR_NORTH));anchorTick(p);
        p.removeAllEffects();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);anchorTick(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.WEST,"foreign anchor history preserved");h.succeed();
    }
    @GameTest(padding=24) public void retirementFindsClearDownBox(GameTestHelper h){
        var p=player(h);ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;
        h.getLevel().setBlockAndUpdate(p.blockPosition().above(),Blocks.STONE.defaultBlockState());
        check(!ClingingReoriented.fits(p,RotationUtil.makeBoxFromDimensions(p.getDimensions(p.getPose()),Direction.DOWN,p.position()),null),"DOWN blocked before retirement");
        p.removeAllEffects();ClingingReoriented.reconcile(p);
        check(GravityDirectionUtil.getOwnGravityDirection(p)==Direction.DOWN && h.getLevel().noCollision(p,p.getBoundingBox().deflate(1e-7)),"retirement relocated to validated box");h.succeed();
    }
    @GameTest(padding=24) public void forgedMovingReferenceCannotReposition(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();p.setPos(cow.getX(),cow.getBoundingBox().maxY,cow.getZ());
        MovingSurface.bind(p,cow);var s=ClingingReoriented.data(p);s.owned=true;s.groundedOnSurface=true;
        var raw=p.position();s.pendingMove=new Payloads.MoveReference(cow.getId(),s.revision,raw,cow.position().add(2,0,0));
        check(MovingSurface.resolveMovement(p,raw).equals(raw),"unknown frame rejected");
        check(s.pendingMove==null,"reference consumed once");h.succeed();
    }
    @GameTest(padding=24) public void respawnDoesNotCopyOwnedBaseGravity(GameTestHelper h){
        var old=player(h);ClingingReoriented.write(old,Direction.EAST);var s=ClingingReoriented.data(old);s.owned=true;s.selected=Direction.EAST;
        old.setHealth(0);ClingingReoriented.reconcile(old);
        var replacement=h.makeMockServerPlayerInLevel();replacement.restoreFrom(old,false);
        check(GravityDirectionUtil.getOwnGravityDirection(replacement)==Direction.DOWN,"death-copy base direction reset");
        check(!ClingingReoriented.data(replacement).owned,"respawn not owned");h.succeed();
    }
    @GameTest(padding=24) public void livingTransferPreservesGravityOwnership(GameTestHelper h){
        var old=player(h);ClingingReoriented.write(old,Direction.SOUTH);var s=ClingingReoriented.data(old);s.owned=true;s.selected=Direction.SOUTH;
        s.airChangeUsed=true;
        var replacement=h.makeMockServerPlayerInLevel();replacement.snapTo(old.position());replacement.restoreFrom(old,true);
        check(GravityDirectionUtil.getOwnGravityDirection(replacement)==Direction.SOUTH && ClingingReoriented.data(replacement).owned,"alive copy preserves own direction");
        check(ClingingReoriented.data(replacement).airChangeUsed,"alive copy preserves spent air change");
        replacement.removeAllEffects();ClingingReoriented.reconcile(replacement);
        check(GravityDirectionUtil.getOwnGravityDirection(replacement)==Direction.DOWN,"copied ownership retires");h.succeed();
    }
    @GameTest(padding=24) public void movingEntityTeleportDropsBindingNotGravity(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();MovingSurface.bind(p,cow);
        ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;
        var before=p.position();cow.teleportTo(cow.getX()+.2,cow.getY(),cow.getZ());
        check(s.support==null && p.position().equals(before) && GravityDirectionUtil.getOwnGravityDirection(p)==Direction.EAST,"small teleport doesn't carry or reset");h.succeed();
    }
    @GameTest(padding=24) public void playerTeleportInvalidatesBindingAndInputEpoch(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();MovingSurface.bind(p,cow);
        var s=ClingingReoriented.data(p);int before=s.revision;
        p.teleportTo(p.getX()+.2,p.getY(),p.getZ());
        check(s.support==null && s.revision>before,"ServerPlayer override clears binding and stale input");h.succeed();
    }
    @GameTest(padding=24) public void freeLookIgnoresWallsAndMomentum(GameTestHelper h){
        var p=player(h);p.setDeltaMovement(-.7,.42,.3);var before=p.position();var velocity=p.getDeltaMovement();
        check(ClingingReoriented.attempt(p,new Vec3(1,0,0))==ClingingReoriented.Result.SUCCESS,"free-air EAST accepted");
        check(p.position().equals(before) && p.getDeltaMovement().equals(velocity),"turn preserves position and world momentum");
        check(!p.onGround(),"old support flags cleared");
        check(ClingingReoriented.attempt(p,Vec3.ZERO)==ClingingReoriented.Result.AMBIGUOUS,"invalid vector rejected");
        check(GravityDirectionUtil.getGravityDirection(p)==Direction.EAST,"ambiguous input leaves direction");
        check(ClingingReoriented.attempt(p,new Vec3(0,0,-1))==ClingingReoriented.Result.AIR_CHANGE_USED,"Clinging cannot redirect twice in air");
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,200));
        check(ClingingReoriented.attempt(p,new Vec3(0,0,-1))==ClingingReoriented.Result.SUCCESS,"Reorientation overrides spent Clinging");
        check(p.position().equals(before) && p.getDeltaMovement().equals(velocity),"second turn preserves world momentum");h.succeed();
    }
    @GameTest(padding=24) public void fiveMetreFallHitsWallAndCanJumpAway(GameTestHelper h){
        var p=player(h);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(Math.floor(p.getX()),p.getY()+3,p.getZ());var origin=p.position();int wallX=(int)origin.x+5;
        for(int y=(int)origin.y-3;y<(int)origin.y+8;y++)for(int z=(int)origin.z-3;z<(int)origin.z+4;z++)h.getLevel().setBlock(new BlockPos(wallX,y,z),Blocks.STONE.defaultBlockState(),3);
        p.setDeltaMovement(0,.42,0);
        check(ClingingReoriented.attempt(p,new Vec3(1,0,0))==ClingingReoriented.Result.SUCCESS,"wall five metres away accepted");
        for(int tick=0;tick<45;tick++)p.travel(Vec3.ZERO);
        check(p.getX()>origin.x+4.9,"actual travel reaches distant wall: "+p.position().subtract(origin));
        check(p.getBoundingBox().maxX<=wallX+1e-6 && p.onGround(),"wall clips travel and acts as floor");
        p.jumpFromGround();check(p.getDeltaMovement().x<0,"jump leaves wall against EAST gravity");h.succeed();
    }
    @GameTest(padding=24) public void allEntityFacesCatchUnboundArrivals(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,5,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
        for(Direction gravity:Direction.values()){
            var box=cow.getBoundingBox();var normal=gravity.getOpposite();var center=box.getCenter();
            double plane=normal.getAxisDirection()==Direction.AxisDirection.POSITIVE?box.max(normal.getAxis()):box.min(normal.getAxis());
            var n=io.github.r3neer.clingingreoriented.geometry.FaceGeometry.vector(normal);
            var point=new Vec3(normal.getAxis()==Direction.Axis.X?plane:center.x,normal.getAxis()==Direction.Axis.Y?plane:center.y,normal.getAxis()==Direction.Axis.Z?plane:center.z);
            ClingingReoriented.write(p,gravity);p.setPos(point.add(n.scale(2)));p.setDeltaMovement(n.scale(-.2));
            var s=ClingingReoriented.data(p);s.owned=true;s.selected=gravity;s.unbind();
            p.move(MoverType.SELF,n.scale(-3));
            check(io.github.r3neer.clingingreoriented.geometry.FaceGeometry.touching(p.getBoundingBox(),box,gravity),"unbound arrival collides on "+gravity);
            check(cow.getUUID().equals(s.support),"arrival acquires moving reference on "+gravity);
            s.airChangeUsed=true;p.setDeltaMovement(Vec3.ZERO);p.setOnGround(true);AirChanges.refresh(p);
            check(!s.airChangeUsed,"entity landing restores charge on "+gravity);
            var before=p.position();var delta=gravity.getAxis()==Direction.Axis.X?new Vec3(0,0,.2):new Vec3(.2,0,0);
            cow.setPos(cow.position().add(delta));p.setDeltaMovement(Vec3.ZERO);MovingSurface.carry(p);
            check(p.position().distanceTo(before.add(delta))<1e-6,"new reference carries on "+gravity);
        }
        h.succeed();
    }
    @GameTest(padding=24) public void fallingTimerDoesNotDamageOwnedGravity(GameTestHelper h){
        var fixture=player(h);
        // Vanilla's GameTest mock overrides gameMode() to CREATIVE even after setGameMode.
        // Use a real ServerPlayer so the upstream survival-only timeout is exercised.
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"fall-survival"),net.minecraft.server.level.ClientInformation.createDefault());
        p.connection=fixture.connection;p.connection.player=p;p.snapTo(fixture.position());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        check(!p.isCreative(),"timeout fixture must actually be survival");
        p.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.get(Identifier.parse("alexsmobs:clinging")).orElseThrow(),1200));
        var origin=p.position();ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;
        float health=p.getHealth();
        for(int i=0;i<240;i++){p.setPos(origin);p.setOnGround(false);p.setDeltaMovement(.2,0,0);p.tick();p.doTick();}
        check(p.getHealth()==health,"no artificial sideways-time damage while owned");
        s.owned=false;
        for(int i=0;i<220 && p.isAlive();i++){p.setPos(origin);p.setOnGround(false);p.setDeltaMovement(.2,0,0);p.tick();p.doTick();}
        check(p.getHealth()<health,"foreign gravity retains upstream timeout damage: health="+p.getHealth()+" noGravity="+p.isNoGravity()+" ground="+p.onGround()+" velocity="+p.getDeltaMovement()+" creative="+p.isCreative());h.succeed();
    }
    @GameTest(padding=24) public void stoppedPlatformDoesNotLaunchPlayer(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=player(h);var cow=h.spawn(EntityTypes.COW,new BlockPos(5,3,5));cow.setNoAi(true);
        p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();p.setPos(cow.getX(),cow.getBoundingBox().maxY,cow.getZ());p.setDeltaMovement(Vec3.ZERO);
        var s=ClingingReoriented.data(p);s.owned=true;MovingSurface.bind(p,cow);s.lastTransport=new Vec3(.2,0,0);s.transportTick=p.level().getGameTime()-1;
        MovingSurface.carry(p);check(s.lastTransport.equals(Vec3.ZERO),"stopped carrier clears stale departure momentum");
        p.jumpFromGround();check(Math.abs(p.getDeltaMovement().x)<1e-6,"jump does not inherit old platform velocity");h.succeed();
    }
    @GameTest(padding=24) public void lateralFallIsNotVanillaFlight(GameTestHelper h){
        var p=player(h);ClingingReoriented.write(p,Direction.EAST);var s=ClingingReoriented.data(p);s.owned=true;s.selected=Direction.EAST;
        try {
            var method=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredMethod("noBlocksAround",Entity.class);method.setAccessible(true);
            check(!(boolean)method.invoke(p.connection,p),"owned lateral gravity bypasses only world-Y floating predicate");
            s.owned=false;check((boolean)method.invoke(p.connection,p),"foreign sources retain original predicate");
        } catch(ReflectiveOperationException e){throw new RuntimeException(e);}h.succeed();
    }
}
