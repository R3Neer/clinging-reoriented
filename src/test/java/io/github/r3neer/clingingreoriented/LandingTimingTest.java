package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class LandingTimingTest {
    @Test void presentationDurationTracksRemainingEtaAndClampsToWindow(){
        assertEquals(0L,LandingTiming.presentationNanos(0.0D));
        assertEquals(150_000_000L,LandingTiming.presentationNanos(3.0D));
        assertEquals(475_000_000L,LandingTiming.presentationNanos(9.5D));
        assertEquals(LandingTiming.PRESENTATION_NANOS,LandingTiming.presentationNanos(10.0D));
        assertEquals(LandingTiming.PRESENTATION_NANOS,LandingTiming.presentationNanos(30.0D));
        assertEquals(LandingTiming.PRESENTATION_NANOS,LandingTiming.presentationNanos(Double.NaN));
    }
}