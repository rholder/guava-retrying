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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

/**
 * An exception indicating that none of the attempts of the retryer succeeded.
 * @author JB
 */
@Immutable
public final class RetryException extends Exception {

    private final int numberOfFailedAttempts;
    private final Attempt<?> lastFailedAttempt;

    /**
     * Constructor
     * @param lastFailedAttempt the last failed attempt
     */
    public RetryException(int numberOfFailedAttempts, @Nonnull Attempt<?> lastFailedAttempt) {
        Preconditions.checkNotNull(lastFailedAttempt);
        this.numberOfFailedAttempts = numberOfFailedAttempts;
        this.lastFailedAttempt = lastFailedAttempt;
    }

    /**
     * Returns the number of failed attempts
     */
    public int getNumberOfFailedAttempts() {
        return numberOfFailedAttempts;
    }

    /**
     * Returns the last failed attempt
     */
    public Attempt<?> getLastFailedAttempt() {
        return lastFailedAttempt;
    }
}
