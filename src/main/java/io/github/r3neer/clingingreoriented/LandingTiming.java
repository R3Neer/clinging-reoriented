package io.github.r3neer.clingingreoriented;

/** Shared timing contract for camera/body landing presentation. */
public final class LandingTiming {
    public static final int PRESENTATION_TICKS=10; // 500 ms at 20 TPS
    public static final long PRESENTATION_NANOS=500_000_000L;

    private LandingTiming() {}
}
