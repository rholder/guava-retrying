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

/**
 * A strategy used to decide how long to sleep before retrying, after a failed attempt.
 * @author JB
 */
public interface WaitStrategy {

    /**
     * Returns the time, in milliseconds, to sleep before retrying.
     * @param previousAttemptNumber the number, starting from 1, of the previous (failed) attempt
     * @param delaySinceFirstAttemptInMillis the delay since the start of the first attempt, in milliseconds
     */
    long computeSleepTime(int previousAttemptNumber, long delaySinceFirstAttemptInMillis);
}
