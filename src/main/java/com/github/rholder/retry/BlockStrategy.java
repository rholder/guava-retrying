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
 * This is a strategy used to decide how a retryer should block between retry
 * attempts. Normally this is just a Thread.sleep(), but implementations can be
 * something more elaborate if desired.
 */
public interface BlockStrategy {

    /**
     * Attempt to block for the designated amount of time. Implementations
     * that don't block or otherwise delay the processing from within this
     * method for the given sleep duration can significantly modify the behavior
     * of any configured {@link com.github.rholder.retry.WaitStrategy}. Caution
     * is advised when generating your own implementations.
     *
     * @param sleepTime the computed sleep duration in milliseconds
     * @throws InterruptedException
     */
    void block(long sleepTime) throws InterruptedException;
}