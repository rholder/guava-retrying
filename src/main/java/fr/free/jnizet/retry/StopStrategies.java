package fr.free.jnizet.retry;

import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * Factory class for {@link StopStrategy} instances.
 * @author JB
 */
public final class StopStrategies {
    private static final StopStrategy NEVER_STOP = new NeverStopStrategy();

    private StopStrategies() {
    }

    /**
     * Returns a stop strategy which consists in never stopping retrying
     */
    public static StopStrategy neverStop() {
        return NEVER_STOP;
    }

    /**
     * Returns a stop strategy which consists in stopping after N failed attempts
     * @param attemptNumber the number of failed attempts before stopping
     */
    public static StopStrategy stopAfterAttempt(int attemptNumber) {
        return new StopAfterAttemptStrategy(attemptNumber);
    }

    /**
     * Returns a stop strategy which consists in stopping after a given delay
     * @param delayInMillis the delay, in milliseconds, starting with the start of the first attempt.
     */
    public static StopStrategy stopAfterDelay(long delayInMillis) {
        return new StopAfterDelayStrategy(delayInMillis);
    }

    @Immutable
    private static final class NeverStopStrategy implements StopStrategy {
        @Override
        public boolean shouldStop(int previousAttemptNumber, long delaySinceFirstAttemptInMillis) {
            return false;
        }
    }

    @Immutable
    private static final class StopAfterAttemptStrategy implements StopStrategy {
        private final int maxAttemptNumber;

        public StopAfterAttemptStrategy(int maxAttemptNumber) {
            Preconditions.checkArgument(maxAttemptNumber >= 1, "maxAttemptNumber must be >= 1 but is %d", maxAttemptNumber);
            this.maxAttemptNumber = maxAttemptNumber;
        }

        @Override
        public boolean shouldStop(int previousAttemptNumber, long delaySinceFirstAttemptInMillis) {
            return previousAttemptNumber >= maxAttemptNumber;
        }
    }

    @Immutable
    private static final class StopAfterDelayStrategy implements StopStrategy {
        private final long maxDelay;

        public StopAfterDelayStrategy(long maxDelay) {
            Preconditions.checkArgument(maxDelay >= 0L, "maxDelay must be >= 0 but is %d", maxDelay);
            this.maxDelay = maxDelay;
        }

        @Override
        public boolean shouldStop(int previousAttemptNumber, long delaySinceFirstAttemptInMillis) {
            return delaySinceFirstAttemptInMillis >= maxDelay;
        }
    }
}
