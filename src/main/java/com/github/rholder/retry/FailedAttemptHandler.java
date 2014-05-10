package com.github.rholder.retry;

/**
 * @author squall
 */
public interface FailedAttemptHandler {
    public void handle(FailedAttemptEvent event);
}
