package io.github.r3neer.clingingreoriented.geometry;
import com.moigferdsrte.gravitychanger.util.RotationUtil;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LookDirectionTest {
    @Test void allSixDirectionsInAllSixGravityFrames() {
        for(Direction gravity:Direction.values()) for(Direction local:Direction.values()) {
            var world=RotationUtil.vecPlayerToWorld(FaceGeometry.vector(local),gravity);
            var selected=LookDirection.select(world);
            assertNotNull(selected); assertEquals(1,world.dot(FaceGeometry.vector(selected)),1e-6);
        }
    }
    @Test void nearestCardinalHasDeterministicTies() {
        assertEquals(Direction.EAST,LookDirection.select(new Vec3(1,1,0).normalize()));
        assertEquals(Direction.WEST,LookDirection.select(new Vec3(-1,1,1).normalize()));
        assertEquals(Direction.EAST,LookDirection.select(new Vec3(1,.95,0).normalize()));
        assertEquals(Direction.EAST,LookDirection.select(new Vec3(1,.5,.1).normalize()));
    }
    @Test void malformedNetworkVectorsCannotChooseGravity() {
        for(Vec3 v:new Vec3[]{Vec3.ZERO,new Vec3(Double.NaN,0,0),new Vec3(Double.POSITIVE_INFINITY,0,0),new Vec3(100,0,0)}) assertNull(LookDirection.select(v));
    }
}
