package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class ImpactPhysicsTest {
    @Test void absorbedVelocityUsesOnlyClippedComponents(){
        assertEquals(new Vec3(1,0,0),ImpactPhysics.absorbedVelocity(new Vec3(1,-.4,.5),new Vec3(0,-.4,.5)));
        assertEquals(Vec3.ZERO,ImpactPhysics.absorbedVelocity(new Vec3(1,0,.5),new Vec3(1,.25,.5)),"step-up on an unrequested axis is not impact energy");
        assertEquals(new Vec3(1,-1,0),ImpactPhysics.absorbedVelocity(new Vec3(1,-1,.5),new Vec3(0,0,.5)));
    }
    @Test void diagonalImpactIsOneVectorMagnitude(){
        assertEquals(Math.sqrt(2.0),ImpactPhysics.impactSpeed(new Vec3(1,-1,0),Vec3.ZERO),1.0E-9);
    }
    @Test void vanillaEquivalentDistanceIsMonotonicAndCalibrated(){
        double previous=0;
        for(double speed=0.0;speed<=4.5;speed+=.05){double distance=ImpactPhysics.vanillaEquivalentFallDistance(speed);assertTrue(distance>=previous);previous=distance;}
        assertTrue(ImpactPhysics.vanillaEquivalentFallDistance(.73)>3.5&&ImpactPhysics.vanillaEquivalentFallDistance(.73)<5.0,"~0.73 b/t is roughly a four-block vanilla fall");
        assertTrue(ImpactPhysics.vanillaEquivalentFallDistance(1.16)>9.0&&ImpactPhysics.vanillaEquivalentFallDistance(1.16)<13.0,"~1.16 b/t is roughly a ten-block vanilla fall");
    }
    @Test void malformedInputsFailHarmlessly(){
        assertEquals(Vec3.ZERO,ImpactPhysics.absorbedVelocity(new Vec3(Double.NaN,0,0),Vec3.ZERO));
        assertEquals(0.0,ImpactPhysics.vanillaEquivalentFallDistance(Double.NaN));
    }
}
