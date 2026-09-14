package io.github.r3neer.clingingreoriented;

import com.moigferdsrte.gravitychanger.util.GravityDirectionUtil;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative holdouts for terminal speed and hard-world rescue. */
public final class FlightSafetyGameTests {
    private static ServerPlayer managed(GameTestHelper h){
        var p=h.makeMockServerPlayerInLevel();
        p.snapTo(h.absoluteVec(new Vec3(8.5,14.0,8.5)));
        p.addEffect(new MobEffectInstance(Reorientation.EFFECT,1200));
        ClingingReoriented.write(p,Direction.DOWN);
        var s=ClingingReoriented.data(p);
        s.owned=true;s.selected=Direction.DOWN;s.visualFrameOwned=true;
        p.setOnGround(false);p.setNoGravity(true);
        return p;
    }

    @GameTest
    public void terminalSpeedCapsWorldVectorWithoutChangingDirection(GameTestHelper h){
        var p=managed(h);
        Vec3 huge=new Vec3(12.0D,-16.0D,5.0D);
        p.setDeltaMovement(huge);
        FlightSafety.capture(p);
        FlightSafety.tick(p);
        Vec3 actual=p.getDeltaMovement();
        h.assertTrue(Math.abs(actual.length()-FlightSafety.TERMINAL_SPEED)<1.0E-8D,
            "terminal speed did not cap world vector: "+actual.length());
        h.assertTrue(actual.normalize().distanceTo(huge.normalize())<1.0E-8D,
            "terminal cap changed movement direction: "+actual+" vs "+huge);
        h.succeed();
    }

    @GameTest
    public void hardCeilingRescueWorksWithoutPreviousSafeAnchor(GameTestHelper h){
        var p=managed(h);var s=ClingingReoriented.data(p);
        s.clearFlightSafety();
        double illegalY=p.level().getMaxY()+40.0D;
        p.snapTo(new Vec3(p.getX(),illegalY,p.getZ()));
        p.setDeltaMovement(new Vec3(0.0D,2.0D,0.0D));
        h.assertTrue(p.getBoundingBox().maxY>p.level().getMaxY()+1.0D,
            "fixture did not start beyond hard world ceiling");

        FlightSafety.tick(p);

        h.assertTrue(p.getBoundingBox().maxY<=p.level().getMaxY()+1.0D+1.0E-6D,
            "safety rescue left player above hard world ceiling: "+p.getBoundingBox());
        h.assertTrue(p.getDeltaMovement().lengthSqr()<1.0E-12D,
            "hard-boundary rescue retained outward velocity: "+p.getDeltaMovement());
        h.assertTrue(ImpactState.state(p).start.equals(Vec3.ZERO),"rescue left stale impact origin");
        h.assertTrue(GravityDirectionUtil.getGravityDirection(p)==Direction.DOWN,"rescue changed physical gravity");
        h.succeed();
    }
}
