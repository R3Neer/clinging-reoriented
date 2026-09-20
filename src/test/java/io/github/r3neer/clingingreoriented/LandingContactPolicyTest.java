package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class LandingContactPolicyTest {
    @Test void clearFeetApproachHasImmediateLandingConfidence(){
        var a=LandingContactPolicy.assess(new Vec3(.1,-.3,.05),new Vec3(0,1,0));
        assertEquals(LandingContactPolicy.ContactKind.CLEAR,a.kind());
        assertEquals(.3D,a.normalSpeed(),1.0E-12D);
        assertTrue(a.normalFraction()>LandingContactPolicy.CLEAR_MIN_NORMAL_FRACTION);
    }

    @Test void fastNearlyTangentialFeetContactIsAGraze(){
        var a=LandingContactPolicy.assess(new Vec3(1.0,-.02,0),new Vec3(0,1,0));
        assertEquals(LandingContactPolicy.ContactKind.GRAZE,a.kind());
        assertTrue(a.normalFraction()<LandingContactPolicy.GRAZE_MAX_NORMAL_FRACTION);
    }

    @Test void intermediateApproachIsAmbiguousInsteadOfCommittingImmediately(){
        var a=LandingContactPolicy.assess(new Vec3(.35,-.05,0),new Vec3(0,1,0));
        assertEquals(LandingContactPolicy.ContactKind.AMBIGUOUS,a.kind());
    }

    @Test void gentleContactNeedsPersistenceRatherThanBeingDiscardedAsAGraze(){
        var a=LandingContactPolicy.assess(new Vec3(.04,-.01,0),new Vec3(0,1,0));
        assertEquals(LandingContactPolicy.ContactKind.AMBIGUOUS,a.kind());
    }
}
