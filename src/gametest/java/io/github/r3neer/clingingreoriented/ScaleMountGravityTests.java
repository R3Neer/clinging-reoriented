package io.github.r3neer.clingingreoriented;
import io.github.r3neer.scalebrews.mount.*;
import com.moigferdsrte.gravitychanger.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
public final class ScaleMountGravityTests {
    private ServerPlayer rider(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();p.getAttribute(Attributes.SCALE).setBaseValue(.28);p.refreshDimensions();
        p.snapTo(h.absoluteVec(new Vec3(5,12,5)));p.addEffect(new MobEffectInstance(Reorientation.EFFECT,500));
        for(var b:BlockPos.betweenClosed(p.blockPosition().offset(-6,-6,-6),p.blockPosition().offset(6,8,6)))h.getLevel().setBlockAndUpdate(b,Blocks.AIR.defaultBlockState());return p;
    }
    @GameTest(padding=32) public void beeLookFlightAndScaledSeatsUseMountFrame(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=rider(h);var bee=h.spawn(EntityTypes.BEE,new BlockPos(5,12,5));bee.setNoAi(true);bee.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(io.github.r3neer.scalebrews.item.ScaleItems.FLOWER_ON_A_STICK));
        h.assertTrue(p.startRiding(bee,true,true)&&TinyMounts.controller(bee)==p,"scaled bee controlled");
        p.setYRot(0);p.setXRot(-30);
        MobGravity.turn(bee,Direction.DOWN,true);Vec3 vanilla=TinyMounts.flightVelocity(p,TinyMounts.definition(bee));
        for(var d:Direction.values()){
            MobGravity.turn(bee,d,true);bee.positionRider(p);
            h.assertTrue(TinyMounts.flightVelocity(p,TinyMounts.definition(bee)).distanceTo(RotationUtil.vecPlayerToWorld(vanilla,d))<1e-6,"bee flight relative "+d);
            h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==d && Double.isFinite(p.getBoundingBox().getSize()) && p.position().distanceTo(bee.position())<3,"finite coherent scaled passenger seat "+d);
        }p.stopRiding();h.succeed();
    }
    @GameTest(padding=32) public void wolfChargeReleaseStillPouncesInItsFrame(GameTestHelper h){
        if(!ScaleBridge.PRESENT){h.succeed();return;}
        var p=rider(h);var wolf=h.spawn(EntityTypes.WOLF,new BlockPos(5,12,5));wolf.tame(p);wolf.setNoAi(true);wolf.setItemSlot(EquipmentSlot.SADDLE,new ItemStack(Items.SADDLE));
        h.assertTrue(p.startRiding(wolf,true,true)&&TinyMounts.controller(wolf)==p,"scaled wolf controlled");
        MobGravity.turn(wolf,Direction.EAST,true);p.setYRot(0);p.setXRot(-30);wolf.setOnGround(true);
        var jump=new Input(false,false,false,false,true,false,false);
        for(int tick=0;tick<6;tick++){p.setLastClientInput(jump);WolfMount.tick(wolf);}
        p.setLastClientInput(Input.EMPTY);WolfMount.tick(wolf);
        var local=RotationUtil.vecWorldToPlayer(wolf.getDeltaMovement(),Direction.EAST);
        h.assertTrue(local.y>0 && local.z>0,"release pounces forward and locally up");p.stopRiding();h.succeed();
    }
}
