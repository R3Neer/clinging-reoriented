package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import io.github.r3neer.clingingreoriented.geometry.SweptAabb;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

final class SweptAabbTest {
    private static final AABB BODY=new AABB(0,1,0,1,2,1);
    @Test void downwardFloorContactReportsUpNormal(){
        var hit=SweptAabb.hitForNormal(BODY,BODY.move(0,-2,0),new AABB(-2,-1,-2,3,0,3),Direction.UP).orElseThrow();
        assertEquals(.5,hit.fraction(),1.0E-9);assertEquals(Direction.UP,hit.normal());
    }
    @Test void lateralWallIsNotAValidDownGravityLanding(){
        var wall=new AABB(2,-2,-2,3,3,3);
        assertTrue(SweptAabb.hit(BODY,BODY.move(2,0,0),wall).isPresent());
        assertTrue(SweptAabb.hitForNormal(BODY,BODY.move(2,0,0),wall,Direction.UP).isEmpty());
        assertEquals(Direction.WEST,SweptAabb.hit(BODY,BODY.move(2,0,0),wall).orElseThrow().normal());
    }
    @Test void cornerTieCanAcknowledgeFeetContact(){
        var corner=new AABB(2,-1,-1,3,0,2);
        var end=BODY.move(2,-2,0);
        assertTrue(SweptAabb.hitForNormal(BODY,end,corner,Direction.UP).isPresent());
        assertTrue(SweptAabb.hitForNormal(BODY,end,corner,Direction.WEST).isPresent());
    }
    @Test void parallelSeparatedAxisCannotCollide(){
        var remote=new AABB(0,0,3,1,1,4);
        assertTrue(SweptAabb.hit(BODY,BODY.move(0,-2,0),remote).isEmpty());
    }
}
