/*
 * Copyright 2012-2013 Ray Holder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rholder.retry;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import javax.annotation.Nonnull;

/**
 * A builder used to configure and create a {@link Retryer}.
 * @author JB
 * @author Jason Dunkelberger (dirkraft)
 *
 * @param <V> the type of the retryer return value
 */
public class RetryerBuilder<V> {
    private AttemptTimeLimiter<V> attemptTimeLimiter;
    private StopStrategy stopStrategy;
    private WaitStrategy waitStrategy;
    private Predicate<Attempt<V>> rejectionPredicate = Predicates.alwaysFalse();

    private RetryerBuilder() {
    }

    /**
     * Constructs a new builder
     * @return the new builder
     */
    public static <V> RetryerBuilder<V> newBuilder() {
        return new RetryerBuilder<V>();
    }

    /**
     * Sets the wait strategy used to decide how long to sleep between failed attempts.
     * The default strategy is to retry immediately after a failed attempt.
     * @param waitStrategy the strategy used to sleep between failed attempts
     * @return <code>this</code>
     * @throws IllegalStateException if a wait strategy has already been set.
     */
    public RetryerBuilder<V> withWaitStrategy(@Nonnull WaitStrategy waitStrategy) throws IllegalStateException {
        Preconditions.checkNotNull(waitStrategy, "waitStrategy may not be null");
        Preconditions.checkState(this.waitStrategy == null, "a wait strategy has already been set %s", this.waitStrategy);
        this.waitStrategy = waitStrategy;
        return this;
    }

    /**
     * Sets the stop strategy used to decide when to stop retrying. The default strategy
     * is to not stop at all ({@link StopStrategies.NeverStopStrategy}) .
     * @param stopStrategy the strategy used to decide when to stop retrying
     * @return <code>this</code>
     * @throws IllegalStateException if a stop strategy has already been set.
     */
    public RetryerBuilder<V> withStopStrategy(@Nonnull StopStrategy stopStrategy) throws IllegalStateException{
        Preconditions.checkNotNull(stopStrategy, "stopStrategy may not be null");
        Preconditions.checkState(this.stopStrategy == null, "a stop strategy has already been set %s", this.stopStrategy);
        this.stopStrategy = stopStrategy;
        return this;
    }

    /**
     * Configures the retryer to limit the duration of any particular attempt by the given duration.
     * @param attemptTimeLimiter to apply to each attempt
     * @return <code>this</code>
     */
    public RetryerBuilder<V> withAttemptTimeLimiter(@Nonnull AttemptTimeLimiter<V> attemptTimeLimiter) {
        Preconditions.checkNotNull(attemptTimeLimiter);
        this.attemptTimeLimiter = attemptTimeLimiter;
        return this;
    }

    /**
     * Configures the retryer to retry if an exception (i.e. any <code>Exception</code> or subclass
     * of <code>Exception</code>) is thrown by the call.
     * @return <code>this</code>
     */
    public RetryerBuilder<V> retryIfException() {
        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionClassPredicate<V>(Exception.class));
        return this;
    }

    /**
     * Configures the retryer to retry if a runtime exception (i.e. any <code>RuntimeException</code> or subclass
     * of <code>RuntimeException</code>) is thrown by the call.
     * @return <code>this</code>
     */
    public RetryerBuilder<V> retryIfRuntimeException() {
        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionClassPredicate<V>(RuntimeException.class));
        return this;
    }

    /**
     * Configures the retryer to retry if an exception of the given class (or subclass of the given class) is
     * thrown by the call.
     * @param exceptionClass the type of the exception which should cause the retryer to retry
     * @return <code>this</code>
     */
    public RetryerBuilder<V> retryIfExceptionOfType(@Nonnull Class<? extends Throwable> exceptionClass) {
        Preconditions.checkNotNull(exceptionClass, "exceptionClass may not be null");
        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionClassPredicate<V>(exceptionClass));
        return this;
    }

    /**
     * Configures the retryer to retry if an exception satisfying the given predicate is
     * thrown by the call.
     * @param exceptionPredicate the predicate which causes a retry if satisfied
     * @return <code>this</code>
     */
    public RetryerBuilder<V> retryIfException(@Nonnull Predicate<Throwable> exceptionPredicate) {
        Preconditions.checkNotNull(exceptionPredicate, "exceptionPredicate may not be null");
        rejectionPredicate = Predicates.or(rejectionPredicate, new ExceptionPredicate<V>(exceptionPredicate));
        return this;
    }

    /**
     * Configures the retryer to retry if the result satisfies the given predicate.
     * @param resultPredicate a predicate applied to the result, and which causes the retryer
     * to retry if the predicate is satisfied
     * @return <code>this</code>
     */
    public RetryerBuilder<V> retryIfResult(@Nonnull Predicate<V> resultPredicate) {
        Preconditions.checkNotNull(resultPredicate, "resultPredicate may not be null");
        rejectionPredicate = Predicates.or(rejectionPredicate, new ResultPredicate<V>(resultPredicate));
        return this;
    }

    /**
     * Builds the retryer.
     * @return the built retryer.
     */
    public Retryer<V> build() {
        AttemptTimeLimiter<V> theAttemptTimeLimiter = attemptTimeLimiter == null ? AttemptTimeLimiters.<V>noTimeLimit() : attemptTimeLimiter;
        StopStrategy theStopStrategy = stopStrategy == null ? StopStrategies.neverStop() : stopStrategy;
        WaitStrategy theWaitStrategy = waitStrategy == null ? WaitStrategies.noWait() : waitStrategy;

        return new Retryer<V>(theAttemptTimeLimiter, theStopStrategy, theWaitStrategy, rejectionPredicate);
    }

    private static final class ExceptionClassPredicate<V> implements Predicate<Attempt<V>> {

        private Class<? extends Throwable> exceptionClass;

        public ExceptionClassPredicate(Class<? extends Throwable> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        @Override
        public boolean apply(Attempt<V> attempt) {
            if (!attempt.hasException()) {
                return false;
            }
            return exceptionClass.isAssignableFrom(attempt.getExceptionCause().getClass());
        }
    }

    private static final class ResultPredicate<V> implements Predicate<Attempt<V>> {

        private Predicate<V> delegate;

        public ResultPredicate(Predicate<V> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean apply(Attempt<V> attempt) {
            if (!attempt.hasResult()) {
                return false;
            }
            V result = attempt.getResult();
            return delegate.apply(result);
        }
    }

    private static final class ExceptionPredicate<V> implements Predicate<Attempt<V>> {

        private Predicate<Throwable> delegate;

        public ExceptionPredicate(Predicate<Throwable> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean apply(Attempt<V> attempt) {
            if (!attempt.hasException()) {
                return false;
            }
            return delegate.apply(attempt.getExceptionCause());
        }
    }
}
