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

import com.google.common.base.Function;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import org.junit.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WaitStrategiesTest {


    @Test
    public void testNoWait() {
        WaitStrategy noWait = WaitStrategies.noWait();
        assertEquals(0L, noWait.computeSleepTime(failedAttempt(18, 9879L)));
    }

    @Test
    public void testFixedWait() {
        WaitStrategy fixedWait = WaitStrategies.fixedWait(1000L, TimeUnit.MILLISECONDS);
        assertEquals(1000L, fixedWait.computeSleepTime(failedAttempt(12, 6546L)));
    }

    @Test
    public void testIncrementingWait() {
        WaitStrategy incrementingWait = WaitStrategies.incrementingWait(500L, TimeUnit.MILLISECONDS, 100L, TimeUnit.MILLISECONDS);
        assertEquals(500L, incrementingWait.computeSleepTime(failedAttempt(1, 6546L)));
        assertEquals(600L, incrementingWait.computeSleepTime(failedAttempt(2, 6546L)));
        assertEquals(700L, incrementingWait.computeSleepTime(failedAttempt(3, 6546L)));
    }

    @Test
    public void testRandomWait() {
        WaitStrategy randomWait = WaitStrategies.randomWait(1000L, TimeUnit.MILLISECONDS, 2000L, TimeUnit.MILLISECONDS);
        Set<Long> times = Sets.newHashSet();
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        assertTrue(times.size() > 1); // if not, the random is not random
        for (long time : times) {
            assertTrue(time >= 1000L);
            assertTrue(time <= 2000L);
        }
    }

    @Test
    public void testRandomWaitWithoutMinimum() {
        WaitStrategy randomWait = WaitStrategies.randomWait(2000L, TimeUnit.MILLISECONDS);
        Set<Long> times = Sets.newHashSet();
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        times.add(randomWait.computeSleepTime(failedAttempt(1, 6546L)));
        assertTrue(times.size() > 1); // if not, the random is not random
        for (long time : times) {
            assertTrue(time >= 0L);
            assertTrue(time <= 2000L);
        }
    }

    @Test
    public void testExponential() {
        WaitStrategy exponentialWait = WaitStrategies.exponentialWait();
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(1, 0)) == 2);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(2, 0)) == 4);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(3, 0)) == 8);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(4, 0)) == 16);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(5, 0)) == 32);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(6, 0)) == 64);
    }

    @Test
    public void testExponentialWithMaximumWait() {
        WaitStrategy exponentialWait = WaitStrategies.exponentialWait(40, TimeUnit.MILLISECONDS);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(1, 0)) == 2);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(2, 0)) == 4);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(3, 0)) == 8);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(4, 0)) == 16);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(5, 0)) == 32);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(6, 0)) == 40);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(7, 0)) == 40);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0)) == 40);
    }

    @Test
    public void testExponentialWithMultiplierAndMaximumWait() {
        WaitStrategy exponentialWait = WaitStrategies.exponentialWait(1000, 50000, TimeUnit.MILLISECONDS);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(1, 0)) == 2000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(2, 0)) == 4000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(3, 0)) == 8000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(4, 0)) == 16000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(5, 0)) == 32000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(6, 0)) == 50000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(7, 0)) == 50000);
        assertTrue(exponentialWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0)) == 50000);
    }

    @Test
    public void testRandomExponential() {
        WaitStrategy randomExponentialWait = WaitStrategies.randomExponentialWait();
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(1, 0)), 0, 2);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(2, 0)), 0, 4);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(3, 0)), 0, 8);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(4, 0)), 0, 16);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(5, 0)), 0, 32);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(6, 0)), 0, 64);
    }

    @Test
    public void testRandomExponentialWithMaximumWait() {
        WaitStrategy randomExponentialWait = WaitStrategies.randomExponentialWait(40, TimeUnit.MILLISECONDS);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(1, 0)), 0, 2);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(2, 0)), 0, 4);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(3, 0)), 0, 8);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(4, 0)), 0, 16);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(5, 0)), 0, 32);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(6, 0)), 0, 40);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(7, 0)), 0, 40);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0)), 0, 40);
    }

    @Test
    public void testRandomExponentialWithMinimumAndMaximumWait() {
        WaitStrategy randomExponentialWait = WaitStrategies.randomExponentialWait(10, TimeUnit.MILLISECONDS, 40, TimeUnit.MILLISECONDS);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(1, 0)), 10, 10);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(2, 0)), 10, 10);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(3, 0)), 10, 10);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(4, 0)), 10, 16);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(5, 0)), 10, 32);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(6, 0)), 10, 40);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(7, 0)), 10, 40);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0)), 10, 40);
    }

    @Test
    public void testRandomExponentialWithMultiplierAndMinimumAndMaximumWait() {
        WaitStrategy randomExponentialWait = WaitStrategies.randomExponentialWait(1000, 4, TimeUnit.MILLISECONDS, 50000, TimeUnit.MILLISECONDS);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(1, 0)), 4, 2000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(2, 0)), 4, 4000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(3, 0)), 4, 8000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(4, 0)), 4, 16000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(5, 0)), 4, 32000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(6, 0)), 4, 50000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(7, 0)), 4, 50000);
        assertWithin(randomExponentialWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0)), 4, 50000);
    }

    @Test
    public void testRandomExponentialRandomness() {
        WaitStrategy randomExponentialWait = WaitStrategies.randomExponentialWait();
        Set<Long> times = Sets.newHashSet();
        for (int i = 0; i < 16; i++) {
            long time = randomExponentialWait.computeSleepTime(failedAttempt(6, 0));
            assertWithin(time, 0, 64);
            times.add(time);
        }
        assertTrue(times.size() > 1); // if not, the random may not be random
    }

    @Test
    public void testFibonacci() {
        WaitStrategy fibonacciWait = WaitStrategies.fibonacciWait();
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(1, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(2, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(3, 0L)) == 2L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(4, 0L)) == 3L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(5, 0L)) == 5L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(6, 0L)) == 8L);
    }

    @Test
    public void testFibonacciWithMaximumWait() {
        WaitStrategy fibonacciWait = WaitStrategies.fibonacciWait(10L, TimeUnit.MILLISECONDS);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(1, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(2, 0L)) == 1L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(3, 0L)) == 2L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(4, 0L)) == 3L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(5, 0L)) == 5L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(6, 0L)) == 8L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(7, 0L)) == 10L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0L)) == 10L);
    }

    @Test
    public void testFibonacciWithMultiplierAndMaximumWait() {
        WaitStrategy fibonacciWait = WaitStrategies.fibonacciWait(1000L, 50000L, TimeUnit.MILLISECONDS);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(1, 0L)) == 1000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(2, 0L)) == 1000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(3, 0L)) == 2000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(4, 0L)) == 3000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(5, 0L)) == 5000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(6, 0L)) == 8000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(7, 0L)) == 13000L);
        assertTrue(fibonacciWait.computeSleepTime(failedAttempt(Integer.MAX_VALUE, 0L)) == 50000L);
    }

    @Test
    public void testExceptionWait() {
        WaitStrategy exceptionWait = WaitStrategies.exceptionWait(RuntimeException.class, zeroSleepFunction());
        assertEquals(0L, exceptionWait.computeSleepTime(failedAttempt(42, 7227)));

        WaitStrategy oneMinuteWait = WaitStrategies.exceptionWait(RuntimeException.class, oneMinuteSleepFunction());
        assertEquals(3600 * 1000L, oneMinuteWait.computeSleepTime(failedAttempt(42, 7227)));

        WaitStrategy noMatchRetryAfterWait = WaitStrategies.exceptionWait(RetryAfterException.class, customSleepFunction());
        assertEquals(0L, noMatchRetryAfterWait.computeSleepTime(failedAttempt(42, 7227)));

        WaitStrategy retryAfterWait = WaitStrategies.exceptionWait(RetryAfterException.class, customSleepFunction());
        assertEquals(29L, retryAfterWait.computeSleepTime(failedRetryAfterAttempt(42, 7227)));
    }

    public Attempt<Boolean> failedAttempt(long attemptNumber, long delaySinceFirstAttempt) {
        return new Retryer.ExceptionAttempt<Boolean>(new RuntimeException(), attemptNumber, delaySinceFirstAttempt);
    }

    public Attempt<Boolean> failedRetryAfterAttempt(long attemptNumber, long delaySinceFirstAttempt) {
        return new Retryer.ExceptionAttempt<Boolean>(new RetryAfterException(), attemptNumber, delaySinceFirstAttempt);
    }

    public Function<RuntimeException, Long> zeroSleepFunction() {
        return new Function<RuntimeException, Long>() {
            @Override
            public Long apply(RuntimeException input) {
                return 0L;
            }
        };
    }

    public Function<RuntimeException, Long> oneMinuteSleepFunction() {
        return new Function<RuntimeException, Long>() {
            @Override
            public Long apply(RuntimeException input) {
                return 3600 * 1000L;
            }
        };
    }

    public Function<RetryAfterException, Long> customSleepFunction() {
        return new Function<RetryAfterException, Long>() {
            @Override
            public Long apply(RetryAfterException input) {
                return input.getRetryAfter();
            }
        };
    }

    public class RetryAfterException extends RuntimeException {
        private final long retryAfter = 29L;

        public long getRetryAfter() {
            return retryAfter;
        }
    }

    private static void assertWithin(long value, long min, long max) {
        Range<Long> range = Range.closed(min, max);
        assertTrue(String.format("Expected %s to fall within %s", value, range), range.contains(value));
    }
}
