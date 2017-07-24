package com.github.rholder.retry;

import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class ExceptionAttempt<R> implements Attempt<R> {
    private final ExecutionException e;
    private final long attemptNumber;
    private final long delaySinceFirstAttempt;

    public ExceptionAttempt(Throwable cause, long attemptNumber, long delaySinceFirstAttempt) {
        this.e = new ExecutionException(cause);
        this.attemptNumber = attemptNumber;
        this.delaySinceFirstAttempt = delaySinceFirstAttempt;
    }

    @Override
    public R get() throws ExecutionException {
        throw e;
    }

    @Override
    public boolean hasResult() {
        return false;
    }

    @Override
    public boolean hasException() {
        return true;
    }

    @Override
    public R getResult() throws IllegalStateException {
        throw new IllegalStateException("The attempt resulted in an exception, not in a result");
    }

    @Override
    public Throwable getExceptionCause() throws IllegalStateException {
        return e.getCause();
    }

    @Override
    public long getAttemptNumber() {
        return attemptNumber;
    }

    @Override
    public long getDelaySinceFirstAttempt() {
        return delaySinceFirstAttempt;
    }
}
