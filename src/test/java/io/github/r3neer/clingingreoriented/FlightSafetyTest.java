package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class FlightSafetyTest {
    @Test void terminalClampPreservesDirectionAndBoundsMagnitude(){
        Vec3 input=new Vec3(6.0D,-8.0D,3.0D);
        Vec3 clamped=FlightSafety.clampVelocity(input);
        assertEquals(FlightSafety.TERMINAL_SPEED,clamped.length(),1.0E-9D);
        assertTrue(clamped.normalize().distanceTo(input.normalize())<1.0E-9D);
    }

    @Test void subterminalVelocityIsUntouched(){
        Vec3 input=new Vec3(.7D,-1.1D,.3D);
        assertEquals(input,FlightSafety.clampVelocity(input));
    }

    @Test void nonFiniteVelocityFailsClosed(){
        assertEquals(Vec3.ZERO,FlightSafety.clampVelocity(new Vec3(Double.NaN,1,0)));
        assertEquals(Vec3.ZERO,FlightSafety.clampVelocity(new Vec3(Double.POSITIVE_INFINITY,0,0)));
    }

    @Test void frontierSearchReturnsLastAcceptedPoint(){
        Vec3 safe=new Vec3(0,0,0),target=new Vec3(10,0,0);
        Vec3 frontier=FlightSafety.furthestReady(safe,target,p->p.x<=6.25D);
        assertTrue(frontier.x<=6.25D+1.0E-4D,"frontier crossed accepted region: "+frontier);
        assertTrue(frontier.x>=6.24D,"frontier stopped implausibly early: "+frontier);
        assertEquals(0.0D,frontier.y,0.0D);
        assertEquals(0.0D,frontier.z,0.0D);
    }
}
