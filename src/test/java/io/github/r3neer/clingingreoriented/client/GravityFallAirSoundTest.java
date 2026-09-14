package io.github.r3neer.clingingreoriented.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GravityFallAirSoundTest {
    @Test void volumeCurveMatchesVanillaElytraMapping(){
        assertEquals(0.0F,GravityFallAirSound.volumeForSpeedSqr(0.0F),1.0E-6F);
        assertEquals(0.25F,GravityFallAirSound.volumeForSpeedSqr(1.0F),1.0E-6F);
        assertEquals(1.0F,GravityFallAirSound.volumeForSpeedSqr(4.0F),1.0E-6F);
        assertEquals(1.0F,GravityFallAirSound.volumeForSpeedSqr(100.0F),1.0E-6F);
    }

    @Test void pitchOnlyRisesInHighSpeedRegion(){
        assertEquals(1.0F,GravityFallAirSound.pitchForVolume(0.8F),1.0E-6F);
        assertEquals(1.1F,GravityFallAirSound.pitchForVolume(0.9F),1.0E-6F);
        assertEquals(1.2F,GravityFallAirSound.pitchForVolume(1.0F),1.0E-6F);
    }
}
