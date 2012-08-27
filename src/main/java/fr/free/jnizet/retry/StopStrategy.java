package fr.free.jnizet.retry;

/**
 * A strategy used to decide if a retryer must stop retrying after a failed attempt or not
 * @author JB
 */
public interface StopStrategy {

    /**
     * Returns <code>true</code> if the retryer should stop retrying.
     * @param previousAttemptNumber the number of the previous attempt (starting from 1)
     * @param delaySinceFirstAttemptInMillis the delay since the start of the first attempt,
     * in milliseconds
     * @return <code>true</code> if the retryer must stop, <code>false</code> otherwise.
     */
    boolean shouldStop(int previousAttemptNumber,
                       long delaySinceFirstAttemptInMillis);
}
