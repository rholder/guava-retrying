package com.github.rholder.retry;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * A retryer, which executes a call, and retries it until it succeeds, or
 * a stop strategy decides to stop retrying. A wait strategy is used to sleep
 * between attempts. The strategy to decide if the call succeeds or not is
 * also configurable.
 * <p>
 * A retryer can also wrap the callable into a RetryerCallable, which can be submitted to an executor.
 * <p>
 * Retryer instances are better constructed with a {@link RetryerBuilder}. A retryer
 * is thread-safe, provided the arguments passed to its constructor are thread-safe.
 *
 * @author JB
 * @author Jason Dunkelberger (dirkraft)
 * @param <V> the type of the call return value
 */
public final class Retryer<V> {
    private final StopStrategy stopStrategy;
    private final WaitStrategy waitStrategy;
    private final AttemptTimeLimiter<V> attemptTimeLimiter;
    private final Predicate<Attempt<V>> rejectionPredicate;

    /**
     * Constructor
     * @param stopStrategy the strategy used to decide when the retryer must stop retrying
     * @param waitStrategy the strategy used to decide how much time to sleep between attempts
     * @param rejectionPredicate the predicate used to decide if the attempt must be rejected
     * or not. If an attempt is rejected, the retryer will retry the call, unless the stop
     * strategy indicates otherwise or the thread is interrupted.
     */
    public Retryer(@Nonnull StopStrategy stopStrategy,
                   @Nonnull WaitStrategy waitStrategy,
                   @Nonnull Predicate<Attempt<V>> rejectionPredicate) {

        this(AttemptTimeLimiters.<V>noTimeLimit(), stopStrategy, waitStrategy, rejectionPredicate);
    }

    /**
     * Constructor
     * @param attemptTimeLimiter to prevent from any single attempt from spinning infinitely
     * @param stopStrategy the strategy used to decide when the retryer must stop retrying
     * @param waitStrategy the strategy used to decide how much time to sleep between attempts
     * @param rejectionPredicate the predicate used to decide if the attempt must be rejected
     * or not. If an attempt is rejected, the retryer will retry the call, unless the stop
     * strategy indicates otherwise or the thread is interrupted.
     */
    public Retryer(@Nonnull AttemptTimeLimiter<V> attemptTimeLimiter,
                   @Nonnull StopStrategy stopStrategy,
                   @Nonnull WaitStrategy waitStrategy,
                   @Nonnull Predicate<Attempt<V>> rejectionPredicate) {
        Preconditions.checkNotNull(attemptTimeLimiter, "timeLimiter may not be null");
        Preconditions.checkNotNull(stopStrategy, "stopStrategy may not be null");
        Preconditions.checkNotNull(waitStrategy, "waitStrategy may not be null");
        Preconditions.checkNotNull(rejectionPredicate, "waitStrategy may not be null");

        this.attemptTimeLimiter = attemptTimeLimiter;
        this.stopStrategy = stopStrategy;
        this.waitStrategy = waitStrategy;
        this.rejectionPredicate = rejectionPredicate;
    }

    /**
     * Executes the given callable. If the rejection predicate
     * accepts the attempt, the stop strategy is used to decide if a new attempt
     * must be made. Then the wait strategy is used to decide how must time to sleep,
     * and a new attempt is made.
     * @throws ExecutionException if the given callable throws an exception, and the
     * rejection predicate considers the attempt as successful. The original exception
     * is wrapped into an ExecutionException.
     * @throws RetryException if all the attempts failed before the stop strategy decided
     * to abort, or the thread was interrupted. Note that if the thread is interrupted,
     * this exception is thrown and the thread's interrupt status is set.
     */
    public V call(Callable<V> callable) throws ExecutionException, RetryException {
        long startTime = System.currentTimeMillis();
        for (int attemptNumber = 1; ; attemptNumber++) {
            Attempt<V> attempt;
            try {
                V result = attemptTimeLimiter.call(callable);
                attempt = new ResultAttempt<V>(result);
            }
            catch (Throwable t) {
                attempt = new ExceptionAttempt<V>(t);
            }
            if (!rejectionPredicate.apply(attempt)) {
                return attempt.get();
            }
            long delaySinceFirstAttemptInMillis = System.currentTimeMillis() - startTime;
            if (stopStrategy.shouldStop(attemptNumber, delaySinceFirstAttemptInMillis)) {
                throw new RetryException(attemptNumber, attempt);
            }
            else {
                long sleepTime = waitStrategy.computeSleepTime(attemptNumber, System.currentTimeMillis() - startTime);
                try {
                    Thread.sleep(sleepTime);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RetryException(attemptNumber, attempt);
                }
            }
        }
    }

    /**
     * Wraps the given callable into a {@link RetryerCallable}, which can be submitted to an executor.
     * The returned callable will use this retryer to call the given callable
     * @param callable the callable to wrap
     */
    public RetryerCallable<V> wrap(Callable<V> callable) {
        return new RetryerCallable<V>(this, callable);
    }

    @Immutable
    private static final class ResultAttempt<R> implements Attempt<R> {
        private final R result;
        public ResultAttempt(R result) {
            this.result = result;
        }

        @Override
        public R get() throws ExecutionException {
            return result;
        }

        @Override
        public boolean hasResult() {
            return true;
        }

        @Override
        public boolean hasException() {
            return false;
        }

        @Override
        public R getResult() throws IllegalStateException {
            return result;
        }

        @Override
        public Throwable getExceptionCause() throws IllegalStateException {
            throw new IllegalStateException("The attempt resulted in a result, not in an exception");
        }
    }

    @Immutable
    private static final class ExceptionAttempt<R> implements Attempt<R> {
        private final ExecutionException e;

        public ExceptionAttempt(Throwable cause) {
            this.e = new ExecutionException(cause);
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
    }

    /**
     * A Callable which wraps another callable in order to make it call by the enclosing retryer.
     * @author JB
     */
    public static class RetryerCallable<X> implements Callable<X> {
        private Retryer<X> retryer;
        private Callable<X> callable;

        private RetryerCallable(Retryer<X> retryer,
                                Callable<X> callable) {
            this.retryer = retryer;
            this.callable = callable;
        }

        /**
         * Makes the enclosing retryer call the wrapped callable.
         * @see Retryer#call(Callable)
         */
        @Override
        public X call() throws ExecutionException, RetryException {
            return retryer.call(callable);
        }
    }
}
