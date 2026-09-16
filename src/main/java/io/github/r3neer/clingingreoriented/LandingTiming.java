package io.github.r3neer.clingingreoriented;

/** Shared timing contract for camera/body landing presentation. */
public final class LandingTiming {
    public static final int PRESENTATION_TICKS=10; // 500 ms at 20 TPS
    public static final long TICK_NANOS=50_000_000L;
    public static final long PRESENTATION_NANOS=PRESENTATION_TICKS*TICK_NANOS;

    private LandingTiming() {}

    /** Remaining predicted flight time, clamped to the visual presentation window. */
    public static long presentationNanos(double etaTicks){
        if(!Double.isFinite(etaTicks))return PRESENTATION_NANOS;
        double clamped=Math.max(0.0D,Math.min(PRESENTATION_TICKS,etaTicks));
        return Math.round(clamped*TICK_NANOS);
    }
}