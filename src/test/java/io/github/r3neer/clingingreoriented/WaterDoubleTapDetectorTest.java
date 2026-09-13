package io.github.r3neer.clingingreoriented;

import static org.junit.jupiter.api.Assertions.*;

import io.github.r3neer.clingingreoriented.client.WaterDoubleTapDetector;
import org.junit.jupiter.api.Test;

final class WaterDoubleTapDetectorTest {
    @Test
    void detectsOnlySecondRisingEdgeWithinWindow() {
        var d=new WaterDoubleTapDetector();
        assertFalse(d.update(true,false,0));
        assertFalse(d.update(true,true,10));
        assertFalse(d.update(true,true,100));
        assertFalse(d.update(true,false,120));
        assertTrue(d.update(true,true,200));
        assertFalse(d.update(true,true,220));
    }

    @Test
    void lateSecondPressBecomesNewFirstPress() {
        var d=new WaterDoubleTapDetector();
        d.update(true,false,0);
        assertFalse(d.update(true,true,10));
        d.update(true,false,20);
        assertFalse(d.update(true,true,WaterDoubleTapDetector.WINDOW_MS+11));
        d.update(true,false,WaterDoubleTapDetector.WINDOW_MS+20);
        assertTrue(d.update(true,true,WaterDoubleTapDetector.WINDOW_MS+100));
    }

    @Test
    void detectedPairIsConsumedBeforeThirdPress() {
        var d=new WaterDoubleTapDetector();
        d.update(true,false,0);
        d.update(true,true,10);
        d.update(true,false,20);
        assertTrue(d.update(true,true,30));
        d.update(true,false,40);
        assertFalse(d.update(true,true,50));
    }

    @Test
    void leavingContextOrEnteringWhileHeldCannotSynthesizeTap() {
        var d=new WaterDoubleTapDetector();
        assertFalse(d.update(false,true,0));
        assertFalse(d.update(true,true,10));
        assertFalse(d.update(true,false,20));
        assertFalse(d.update(true,true,30));
        assertFalse(d.update(false,false,40));
        assertFalse(d.update(true,false,50));
        assertFalse(d.update(true,true,60));
    }
}
