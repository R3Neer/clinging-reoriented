package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class DirectionalMaceFallTest {
    @Test void projectionMeasuresLiteralBlocksAlongGravityAxis(){
        Vec3 delta=new Vec3(3.0D,-4.0D,5.0D);
        assertEquals(4.0D,DirectionalMaceFall.alongGravity(delta,Direction.DOWN),1.0E-12D);
        assertEquals(-4.0D,DirectionalMaceFall.alongGravity(delta,Direction.UP),1.0E-12D);
        assertEquals(3.0D,DirectionalMaceFall.alongGravity(delta,Direction.EAST),1.0E-12D);
        assertEquals(-3.0D,DirectionalMaceFall.alongGravity(delta,Direction.WEST),1.0E-12D);
        assertEquals(5.0D,DirectionalMaceFall.alongGravity(delta,Direction.SOUTH),1.0E-12D);
        assertEquals(-5.0D,DirectionalMaceFall.alongGravity(delta,Direction.NORTH),1.0E-12D);
    }
}
