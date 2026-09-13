package io.github.r3neer.clingingreoriented.client;

/** Passive jump-key edge detector. It never consumes or rewrites Vanilla input. */
public final class WaterDoubleTapDetector {
    public static final long WINDOW_MS = 250L;

    private boolean active;
    private boolean previousDown;
    private long firstPressMs = -1L;

    public boolean update(boolean waterContext, boolean jumpDown, long nowMs) {
        if (!waterContext) {
            reset();
            return false;
        }
        if (!active) {
            active = true;
            previousDown = jumpDown;
            firstPressMs = -1L;
            return false;
        }

        boolean risingEdge = jumpDown && !previousDown;
        previousDown = jumpDown;
        if (!risingEdge) return false;

        if (firstPressMs >= 0L && nowMs >= firstPressMs && nowMs - firstPressMs <= WINDOW_MS) {
            firstPressMs = -1L;
            return true;
        }
        firstPressMs = nowMs;
        return false;
    }

    public void reset() {
        active = false;
        previousDown = false;
        firstPressMs = -1L;
    }
}
