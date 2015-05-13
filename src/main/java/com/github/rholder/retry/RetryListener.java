/*
 * Copyright 2012-2015 Ray Holder
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

/**
 * A listener that will be called after each retry attempt.
 *
 * The listener will be called no matter what the result is and before the rejection predicate and stop strategies are applied
 */
public interface RetryListener {

    /**
     * @param attempt the current {@link Attempt}
     * @param <V> The type returned by the retryer callable.
     */
    // TODO adjust for 3 phases, refactor Attempt to include attemptNumber
    <V> void onRetry(Attempt<V> attempt);
}
