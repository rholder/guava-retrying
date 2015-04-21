package com.github.rholder.retry;

/**
 * @author squall
 */
public interface FailedAttemptHandler {
    void handle(FailedAttemptEvent event);
}
