package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.r3neer.clingingreoriented.client.GravityFallVisuals;
import io.github.r3neer.clingingreoriented.client.VisualTransitions;
import org.junit.jupiter.api.Test;

final class LandingPresentationTimingTest {
    @Test void landingPresentationOwnsHalfSecondWindow(){
        assertEquals(10,LandingPrediction.MAX_TICKS);
        assertEquals(10.0F,GravityFallVisuals.LANDING_DURATION_TICKS,0.0F);
        assertEquals(500_000_000L,VisualTransitions.LAND_DURATION_NANOS);
    }

    @Test void ordinaryGravitySnapsRemainFast(){
        assertEquals(180_000_000L,GravityTransition.QUARTER_TURN_NANOS);
        assertEquals(240_000_000L,GravityTransition.HALF_TURN_NANOS);
    }
}
