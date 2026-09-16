package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class MobFlightReactorTest {
    @Test void targetOnlyCorrectionMayNotIncreasePredictedRisk(){
        assertTrue(MobFlightReactor.targetRiskAcceptable(0.0D,0.0D));
        assertTrue(MobFlightReactor.targetRiskAcceptable(12.0D,8.0D));
        assertTrue(MobFlightReactor.targetRiskAcceptable(12.0D,12.0D));
        assertFalse(MobFlightReactor.targetRiskAcceptable(0.0D,.01D));
        assertFalse(MobFlightReactor.targetRiskAcceptable(12.0D,12.1D));
    }

    @Test void malformedRiskFailsClosed(){
        assertFalse(MobFlightReactor.targetRiskAcceptable(Double.NaN,0.0D));
        assertFalse(MobFlightReactor.targetRiskAcceptable(0.0D,Double.POSITIVE_INFINITY));
        assertFalse(MobFlightReactor.targetRiskAcceptable(-1.0D,0.0D));
        assertFalse(MobFlightReactor.targetRiskAcceptable(0.0D,-1.0D));
    }
}
