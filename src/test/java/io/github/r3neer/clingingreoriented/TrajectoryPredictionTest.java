package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class TrajectoryPredictionTest {
    private static LandingSurfaces.SweepHit hit(double fraction,boolean support,Vec3 normal){
        var key=new LandingSurfaces.SurfaceKey(Identifier.fromNamespaceAndPath("test","surface"),"fixture",0L);
        var contact=new LandingSurfaces.Contact(key,Direction.EAST,normal);
        return new LandingSurfaces.SweepHit(contact,fraction,support);
    }

    @Test void findsSubTickFirstContactAndImpactBody(){
        AABB body=new AABB(0,0,0,1,1,1);Vec3 velocity=new Vec3(1,0,0);
        var trace=TrajectoryPrediction.simulate(body,velocity,5,(tick,v)->v,(start,end)->{
            if(start.minX<1.5D&&end.minX>=1.5D)return Optional.of(hit((1.5D-start.minX)/(end.minX-start.minX),true,new Vec3(-1,0,0)));
            return Optional.empty();
        });
        assertTrue(trace.valid());assertTrue(trace.firstContact().isPresent());
        var contact=trace.firstContact().get();
        assertEquals(1.5D,contact.etaTicks(),1.0E-12D);
        assertEquals(1.5D,contact.impactBody().minX,1.0E-12D);
        assertEquals(1.0D,contact.normalImpactSpeed(),1.0E-12D);
        assertTrue(contact.support());
    }

    @Test void retainsNonSupportAsRealFirstContact(){
        var trace=TrajectoryPrediction.simulate(new AABB(0,0,0,1,1,1),new Vec3(0,-2,0),2,(tick,v)->v,
            (start,end)->Optional.of(hit(.25D,false,new Vec3(1,0,0))));
        assertTrue(trace.valid());assertTrue(trace.firstContact().isPresent());assertFalse(trace.firstContact().get().support());
        assertEquals(.25D,trace.firstContact().get().etaTicks(),1.0E-12D);
    }

    @Test void motionStepperOwnsFutureVelocityLaw(){
        var trace=TrajectoryPrediction.simulate(new AABB(0,0,0,1,1,1),Vec3.ZERO,3,
            (tick,v)->v.add(0,-1,0),(start,end)->Optional.empty());
        assertTrue(trace.valid());assertTrue(trace.firstContact().isEmpty());
        assertEquals(-3.0D,trace.endBody().minY,1.0E-12D);
        assertEquals(new Vec3(0,-3,0),trace.endVelocity());
        assertEquals(3,trace.completedTicks());
    }

    @Test void malformedFutureMotionFailsClosed(){
        var trace=TrajectoryPrediction.simulate(new AABB(0,0,0,1,1,1),new Vec3(1,0,0),3,
            (tick,v)->new Vec3(Double.NaN,0,0),(start,end)->Optional.empty());
        assertFalse(trace.valid());assertTrue(trace.firstContact().isEmpty());
    }
}
