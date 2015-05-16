/*
 * Copyright 2012-2015 Stavros Lekkas
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

import org.junit.Assert;
import org.junit.Test;

import java.util.Timer;
import java.util.TimerTask;

public class BlockStrategiesTest {

    public static final long blockingTimeMs = 1000L;

    @Test(timeout= 2 * BlockStrategiesTest.blockingTimeMs)
    public void testThreadSleepStrategyBlock() {
        final Timer timer = new Timer(true);
        try {
            final Thread current = Thread.currentThread();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    current.interrupt();
                }
            }, blockingTimeMs - 1);
            BlockStrategies.threadSleepStrategy().block(blockingTimeMs);
            Assert.fail("Block strategy did not block");
        } catch (final InterruptedException ignored) {
        } finally {
            timer.cancel();
        }
    }
}