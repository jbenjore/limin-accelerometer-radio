package limn.radio.util;

/**
 * A standard injectable clock like you'll find in Java 8.
 *
 * @author Josh ben Jore
 */
public abstract class Clock {
    private static class SystemClock extends Clock {
        public SystemClock() {
        }

        @Override
        public long millis() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Returns a clock using the system clock.
     *
     * @return the global shared clock
     */
    public static Clock systemUTC() {
        return new SystemClock();
    }

    /**
     * The count of milliseconds since the start of the epoch.
     * @return
     */
    public abstract long millis();
}
