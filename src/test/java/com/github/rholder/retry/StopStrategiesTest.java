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

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class StopStrategiesTest {

    @Test
    public void testNeverStop() {
        assertFalse(StopStrategies.neverStop().shouldStop(failedAttempt(3, 6546L)));
    }

    @Test
    public void testStopAfterAttempt() {
        assertFalse(StopStrategies.stopAfterAttempt(3).shouldStop(failedAttempt(2, 6546L)));
        assertTrue(StopStrategies.stopAfterAttempt(3).shouldStop(failedAttempt(3, 6546L)));
        assertTrue(StopStrategies.stopAfterAttempt(3).shouldStop(failedAttempt(4, 6546L)));
    }

    @Test
    public void testStopAfterDelayWithMilliseconds() {
        assertFalse(StopStrategies.stopAfterDelay(1, SECONDS).shouldStop(failedAttempt(2, 999L)));
        assertTrue(StopStrategies.stopAfterDelay(1, SECONDS).shouldStop(failedAttempt(2, 1000L)));
        assertTrue(StopStrategies.stopAfterDelay(1, SECONDS).shouldStop(failedAttempt(2, 1001L)));
    }

    @Test
    public void testStopAfterDelayWithTimeUnit() {
        assertFalse(StopStrategies.stopAfterDelay(1, SECONDS).shouldStop(failedAttempt(2, 999L)));
        assertTrue(StopStrategies.stopAfterDelay(1, SECONDS).shouldStop(failedAttempt(2, 1000L)));
        assertTrue(StopStrategies.stopAfterDelay(1, SECONDS).shouldStop(failedAttempt(2, 1001L)));
    }

    public Attempt<Boolean> failedAttempt(long attemptNumber, long delaySinceFirstAttempt) {
        return new Retryer.ExceptionAttempt<Boolean>(new RuntimeException(), attemptNumber, delaySinceFirstAttempt);
    }
}
