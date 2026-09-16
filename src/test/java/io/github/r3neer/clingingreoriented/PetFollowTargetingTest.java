package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class PetFollowTargetingTest {
    private static final Vec3 ANCHOR=new Vec3(10,20,30);

    @Test void airborneDistanceIgnoresOnlyCandidateGravityAxis(){
        Vec3 offset=new Vec3(3,4,12);
        Vec3 p=ANCHOR.add(offset);
        assertEquals(Math.sqrt(160.0D),PetFollowTargeting.goalDistance(p,ANCHOR,Direction.EAST,true),1.0E-9D);
        assertEquals(Math.sqrt(160.0D),PetFollowTargeting.goalDistance(p,ANCHOR,Direction.WEST,true),1.0E-9D);
        assertEquals(Math.sqrt(153.0D),PetFollowTargeting.goalDistance(p,ANCHOR,Direction.UP,true),1.0E-9D);
        assertEquals(Math.sqrt(153.0D),PetFollowTargeting.goalDistance(p,ANCHOR,Direction.DOWN,true),1.0E-9D);
        assertEquals(5.0D,PetFollowTargeting.goalDistance(p,ANCHOR,Direction.NORTH,true),1.0E-9D);
        assertEquals(5.0D,PetFollowTargeting.goalDistance(p,ANCHOR,Direction.SOUTH,true),1.0E-9D);
    }

    @Test void groundedOwnerStillUsesFullThreeDimensionalDistance(){
        Vec3 p=ANCHOR.add(3,4,12);
        assertEquals(13.0D,PetFollowTargeting.goalDistance(p,ANCHOR,Direction.DOWN,false),1.0E-9D);
        assertEquals(13.0D,PetFollowTargeting.goalDistance(p,ANCHOR,Direction.EAST,false),1.0E-9D);
    }
}
