package com.github.rholder.retry;

import java.util.concurrent.Callable;

/**
 * A rule to wrap any single attempt in a time limit, where it will possibly be interrupted if the limit is exceeded.
 * @param <V> return type of Callable
 *
 * @author Jason Dunkelberger (dirkraft)
 */
public interface AttemptTimeLimiter<V> {
    /**
     * @param callable to subject to the time limit
     * @return the return of the given callable
     */
    V call(Callable<V> callable) throws Exception;
}
