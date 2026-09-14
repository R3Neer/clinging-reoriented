package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class ClimbablePolicyTest {
    @Test void onlyWorldVerticalGravitiesParticipate(){
        assertTrue(ClimbablePolicy.participates(Direction.DOWN));
        assertTrue(ClimbablePolicy.participates(Direction.UP));
        assertFalse(ClimbablePolicy.participates(Direction.EAST));
        assertFalse(ClimbablePolicy.participates(Direction.WEST));
        assertFalse(ClimbablePolicy.participates(Direction.NORTH));
        assertFalse(ClimbablePolicy.participates(Direction.SOUTH));
    }

    @Test void upsideDownClampMirrorsVanillaWorldYRules(){
        Vec3 clamped=ClimbablePolicy.clampForUpGravity(new Vec3(.8,.9,-.7),false,false);
        assertEquals(.15,clamped.x,1.0E-8);
        assertEquals(.15,clamped.y,1.0E-8);
        assertEquals(-.15,clamped.z,1.0E-8);

        Vec3 upward=ClimbablePolicy.clampForUpGravity(new Vec3(0,-.9,0),false,false);
        assertEquals(-.9,upward.y,1.0E-8,"movement toward world -Y must remain uncapped just as vanilla preserves +Y climb");
    }

    @Test void upsideDownSneakSuppressesSlidingTowardGravity(){
        assertEquals(0.0,ClimbablePolicy.clampForUpGravity(new Vec3(0,.1,0),true,false).y,1.0E-8);
        assertEquals(.1,ClimbablePolicy.clampForUpGravity(new Vec3(0,.1,0),true,true).y,1.0E-8,"scaffolding keeps vanilla exception");
    }

    @Test void climbImpulseMirrorsOnlyForUpGravity(){
        assertEquals(-.2,ClimbablePolicy.climbImpulse(Direction.UP,.2),1.0E-8);
        assertEquals(.2,ClimbablePolicy.climbImpulse(Direction.DOWN,.2),1.0E-8);
        assertEquals(.2,ClimbablePolicy.climbImpulse(Direction.EAST,.2),1.0E-8);
    }
}
