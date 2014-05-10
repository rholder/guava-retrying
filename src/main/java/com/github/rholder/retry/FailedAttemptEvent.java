package com.github.rholder.retry;

import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;


/**
 * @author squall
 */
public class FailedAttemptEvent<V> {

    private V result;
    private Throwable exception;
    private final long nextWaitTime;
    private final FailureCause cause;


    private FailedAttemptEvent(long nextWaitTime, FailureCause cause) {
        Preconditions.checkArgument(nextWaitTime >= 0, "nextWaitTime may not be negative");
        this.nextWaitTime = nextWaitTime;
        this.cause = Preconditions.checkNotNull(cause, "cause may not be null");
    }

    FailedAttemptEvent(long nextWaitTime, @Nonnull FailureCause cause, @Nonnull Throwable exception) {
        this(nextWaitTime, cause);
        this.exception = Preconditions.checkNotNull(exception, "exception may not be null");
    }

    FailedAttemptEvent(long nextWaitTime, @Nonnull FailureCause cause, @Nullable V result) {
        this(nextWaitTime, cause);
        this.result = result;
    }

    /**
     * @return the result that caused the attempt to fail
     *
     * @throws IllegalStateException if the attempt failed because of an exception
     */
    @Nullable
    public V getResult() {
        if (cause == FailureCause.EXCEPTION)
            throw new IllegalStateException("The attempt failed because of an exception, not because of a result");
        return result;
    }

    /**
     * @return the exception that caused the attempt to fail
     *
     * @throws IllegalStateException if the attempt failed because of a result
     */
    @Nonnull
    public Throwable getException() {
        if (cause == FailureCause.RESULT)
            throw new IllegalStateException("The attempt failed because of the result, not because of an exception");
        return exception;
    }


    /**
     * @return wait time until the next attempt is executed in milliseconds
     */
    public long getNextWaitTime() {
        return nextWaitTime;
    }

    /**
     * @param targetUnit time unit of the returned time
     * @return wait time until the next attempt is executed in the supplied time unit
     */
    public long getNextWaitTime(@Nonnull TimeUnit targetUnit) {
        Preconditions.checkNotNull(targetUnit, "targetUnit may not be null");
        return targetUnit.convert(nextWaitTime, TimeUnit.MILLISECONDS);
    }

    /**
     * @return cause of the failed attempt
     */
    public FailureCause getCause() {
        return cause;
    }


    public enum FailureCause {
        EXCEPTION,
        RESULT
    }
}
