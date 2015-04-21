package com.github.rholder.retry;

/**
 * A listener that will be called after each retry attempt.
 *
 * The listener will be called no matter what the result is and before the rejection predicate and stop strategies are applied
 */
public interface RetryListener {

    /**
     * @param attempt The current attempt result
     * @param attemptNumber The current attempt number
     * @param <V> The type returned by the retryer callable.
     */
    <V> void onRetry(Attempt<V> attempt, int attemptNumber);
}
