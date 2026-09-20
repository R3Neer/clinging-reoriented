package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import io.github.r3neer.clingingreoriented.LandingPolicy.Approach;
import io.github.r3neer.clingingreoriented.api.LandingSurfaces;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class LandingPolicyTest {
    private static LandingPrediction.Candidate candidate(Approach approach,double eta){
        var key=new LandingSurfaces.SurfaceKey(Identifier.fromNamespaceAndPath("test","landing_policy"),"fixture",0L);
        var contact=new LandingSurfaces.Contact(key,Direction.DOWN,new Vec3(0,1,0));
        return new LandingPrediction.Candidate(contact,Direction.DOWN,eta,approach,.2D,1.0D);
    }

    @Test void nearTangentialFeetContactIsGraze(){
        assertEquals(Approach.GRAZE,LandingPolicy.classify(new Vec3(2.0D,-.10D,0),new Vec3(0,1,0)));
    }

    @Test void decisiveNormalApproachIsClear(){
        assertEquals(Approach.CLEAR,LandingPolicy.classify(new Vec3(.50D,-.40D,0),new Vec3(0,1,0)));
    }

    @Test void middleAngleAndVeryLowSpeedRequireConfirmation(){
        assertEquals(Approach.AMBIGUOUS,LandingPolicy.classify(new Vec3(1.0D,-.20D,0),new Vec3(0,1,0)));
        assertEquals(Approach.AMBIGUOUS,LandingPolicy.classify(new Vec3(0,-.08D,0),new Vec3(0,1,0)));
    }

    @Test void commitWindowKeepsRescueTimeAndMakesAmbiguousContactPersist(){
        assertFalse(LandingPolicy.shouldCommit(candidate(Approach.CLEAR,5.01D),1));
        assertTrue(LandingPolicy.shouldCommit(candidate(Approach.CLEAR,5.0D),1));
        assertFalse(LandingPolicy.shouldCommit(candidate(Approach.AMBIGUOUS,2.5D),1));
        assertTrue(LandingPolicy.shouldCommit(candidate(Approach.AMBIGUOUS,3.0D),2));
        assertFalse(LandingPolicy.shouldCommit(candidate(Approach.AMBIGUOUS,3.01D),2));
    }

    @Test void bodyAnticipationIsBroaderForClearThanAmbiguousContact(){
        assertTrue(LandingPolicy.bodyApproachReady(candidate(Approach.CLEAR,10.0D),1));
        assertFalse(LandingPolicy.bodyApproachReady(candidate(Approach.AMBIGUOUS,4.0D),1));
        assertTrue(LandingPolicy.bodyApproachReady(candidate(Approach.AMBIGUOUS,4.0D),2));
    }
}
