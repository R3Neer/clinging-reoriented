package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.Vec3;

/** Real mace holdouts: distance is geometric and a gravity-axis change starts a fresh segment. */
public final class DirectionalMaceGameTests {
    @GameTest
    public void maceUsesOnlyCurrentDirectionalFallSegment(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(5.5D,10.0D,5.5D)));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        var s=ClingingReoriented.data(p);
        s.owned=true;s.selected=Direction.EAST;s.visualFrameOwned=true;
        ClingingReoriented.write(p,Direction.EAST);
        p.setNoGravity(true);p.setOnGround(false);

        DirectionalMaceFall.tick(p); // establish EAST origin
        Vec3 eastStart=p.position();
        p.snapTo(eastStart.add(4.0D,0.0D,0.0D));
        p.setDeltaMovement(new Vec3(1.0D,0.0D,0.0D));
        DirectionalMaceFall.tick(p);
        h.assertTrue(Math.abs(DirectionalMaceFall.value(p,0.0F)-4.0F)<1.0E-4F,"EAST geometric height was not four blocks");
        h.assertTrue(MaceItem.canSmashAttack(p),"four-block EAST fall did not arm mace smash");

        // Deliberately poison vanilla fallDistance. The mace must still follow our new SOUTH
        // segment rather than inheriting EAST history or this unrelated/scaled vanilla value.
        p.fallDistance=40.0F;
        ClingingReoriented.write(p,Direction.SOUTH);s.selected=Direction.SOUTH;
        DirectionalMaceFall.tick(p);
        h.assertTrue(DirectionalMaceFall.value(p,p.fallDistance)<1.0E-4F,"gravity turn inherited previous-axis mace height");
        h.assertFalse(MaceItem.canSmashAttack(p),"mace inherited old/scaled fallDistance after gravity turn");

        Vec3 southStart=p.position();
        p.snapTo(southStart.add(0.0D,0.0D,1.0D));
        p.setDeltaMovement(new Vec3(0.0D,0.0D,1.0D));
        DirectionalMaceFall.tick(p);
        h.assertFalse(MaceItem.canSmashAttack(p),"one-block SOUTH fall incorrectly armed mace smash");

        p.snapTo(p.position().add(0.0D,0.0D,1.0D));
        DirectionalMaceFall.tick(p);
        h.assertTrue(Math.abs(DirectionalMaceFall.value(p,p.fallDistance)-2.0F)<1.0E-4F,"SOUTH segment did not accumulate literal distance");
        h.assertTrue(MaceItem.canSmashAttack(p),"two-block SOUTH fall should pass vanilla 1.5-block smash threshold");

        GravityDirectionUtil.setGravityStrength(p,15.0D);
        h.assertTrue(Math.abs(DirectionalMaceFall.value(p,p.fallDistance)-2.0F)<1.0E-4F,
            "gravity strength scaled geometric mace height");
        h.succeed();
    }
}
