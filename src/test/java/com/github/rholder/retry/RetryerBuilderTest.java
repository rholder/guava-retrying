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

import com.github.rholder.retry.Retryer.RetryerCallable;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RetryerBuilderTest {

    @Test
    public void testWithWaitStrategy() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.fixedWait(50L, TimeUnit.MILLISECONDS))
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        long start = System.currentTimeMillis();
        boolean result = retryer.call(callable);
        assertTrue(System.currentTimeMillis() - start >= 250L);
        assertTrue(result);
    }

    @Test
    public void testWithMoreThanOneWaitStrategyOneBeingFixed() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.join(
                        WaitStrategies.fixedWait(50L, TimeUnit.MILLISECONDS),
                        WaitStrategies.fibonacciWait(10, Long.MAX_VALUE, TimeUnit.MILLISECONDS)))
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        long start = System.currentTimeMillis();
        boolean result = retryer.call(callable);
        assertTrue(System.currentTimeMillis() - start >= 370L);
        assertTrue(result);
    }

    @Test
    public void testWithMoreThanOneWaitStrategyOneBeingIncremental() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withWaitStrategy(WaitStrategies.join(
                        WaitStrategies.incrementingWait(10L, TimeUnit.MILLISECONDS, 10L, TimeUnit.MILLISECONDS),
                        WaitStrategies.fibonacciWait(10, Long.MAX_VALUE, TimeUnit.MILLISECONDS)))
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        long start = System.currentTimeMillis();
        boolean result = retryer.call(callable);
        assertTrue(System.currentTimeMillis() - start >= 270L);
        assertTrue(result);
    }

    private Callable<Boolean> notNullAfter5Attempts() {
        return new Callable<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() throws Exception {
                if (counter < 5) {
                    counter++;
                    return null;
                }
                return true;
            }
        };
    }

    @Test
    public void testWithStopStrategy() throws ExecutionException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
        }
    }

    @Test
    public void testWithBlockStrategy() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        final AtomicInteger counter = new AtomicInteger();
        BlockStrategy blockStrategy = new BlockStrategy() {
            @Override
            public void block(long sleepTime) throws InterruptedException {
                counter.incrementAndGet();
            }
        };

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withBlockStrategy(blockStrategy)
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        final int retryCount = 5;
        boolean result = retryer.call(callable);
        assertTrue(result);
        assertEquals(counter.get(), retryCount);
    }

    @Test
    public void testRetryIfException() throws ExecutionException, RetryException {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfException()
                .build();
        boolean result = retryer.call(callable);
        assertTrue(result);

        callable = noIOExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IOException);
            assertTrue(e.getCause() instanceof IOException);
        }

        callable = noIllegalStateExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IllegalStateException);
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    private Callable<Boolean> noIllegalStateExceptionAfter5Attempts() {
        return new Callable<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() throws Exception {
                if (counter < 5) {
                    counter++;
                    throw new IllegalStateException();
                }
                return true;
            }
        };
    }

    private Callable<Boolean> noIOExceptionAfter5Attempts() {
        return new Callable<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() throws IOException {
                if (counter < 5) {
                    counter++;
                    throw new IOException();
                }
                return true;
            }
        };
    }

    @Test
    public void testRetryIfRuntimeException() throws ExecutionException, RetryException {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfRuntimeException()
                .build();
        try {
            retryer.call(callable);
            fail("ExecutionException expected");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IOException);
        }

        callable = noIllegalStateExceptionAfter5Attempts();
        assertTrue(retryer.call(callable));

        callable = noIllegalStateExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IllegalStateException);
            assertTrue(e.getCause() instanceof IllegalStateException);
        }
    }

    @Test
    public void testRetryIfExceptionOfType() throws RetryException, ExecutionException {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .build();
        assertTrue(retryer.call(callable));

        callable = noIllegalStateExceptionAfter5Attempts();
        try {
            retryer.call(callable);
            fail("ExecutionException expected");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        callable = noIOExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfExceptionOfType(IOException.class)
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IOException);
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testRetryIfExceptionWithPredicate() throws RetryException, ExecutionException {
        Callable<Boolean> callable = noIOExceptionAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(Throwable t) {
                        return t instanceof IOException;
                    }
                })
                .build();
        assertTrue(retryer.call(callable));

        callable = noIllegalStateExceptionAfter5Attempts();
        try {
            retryer.call(callable);
            fail("ExecutionException expected");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        callable = noIOExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(Throwable t) {
                        return t instanceof IOException;
                    }
                })
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IOException);
            assertTrue(e.getCause() instanceof IOException);
        }
    }

    @Test
    public void testRetryIfResult() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        assertTrue(retryer.call(callable));

        callable = notNullAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasResult());
            assertNull(e.getLastFailedAttempt().getResult());
            assertNull(e.getCause());
        }
    }

    @Test
    public void testMultipleRetryConditions() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullResultOrIOExceptionOrRuntimeExceptionAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IllegalStateException);
            assertTrue(e.getCause() instanceof IllegalStateException);
        }

        callable = notNullResultOrIOExceptionOrRuntimeExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .retryIfExceptionOfType(IOException.class)
                .retryIfRuntimeException()
                .build();
        assertTrue(retryer.call(callable));
    }

    private Callable<Boolean> notNullResultOrIOExceptionOrRuntimeExceptionAfter5Attempts() {
        return new Callable<Boolean>() {
            int counter = 0;

            @Override
            public Boolean call() throws IOException {
                if (counter < 1) {
                    counter++;
                    return null;
                } else if (counter < 2) {
                    counter++;
                    throw new IOException();
                } else if (counter < 5) {
                    counter++;
                    throw new IllegalStateException();
                }
                return true;
            }
        };
    }

    @Test
    public void testInterruption() throws InterruptedException, ExecutionException {
        final AtomicBoolean result = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                        .withWaitStrategy(WaitStrategies.fixedWait(1000L, TimeUnit.MILLISECONDS))
                        .retryIfResult(Predicates.<Boolean>isNull())
                        .build();
                try {
                    retryer.call(alwaysNull(latch));
                    fail("RetryException expected");
                } catch (RetryException e) {
                    assertTrue(!e.getLastFailedAttempt().hasException());
                    assertNull(e.getCause());
                    assertTrue(Thread.currentThread().isInterrupted());
                    result.set(true);
                } catch (ExecutionException e) {
                    fail("RetryException expected");
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
        latch.countDown();
        t.interrupt();
        t.join();
        assertTrue(result.get());
    }

    @Test
    public void testWrap() throws ExecutionException, RetryException {
        Callable<Boolean> callable = notNullAfter5Attempts();
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .build();
        RetryerCallable<Boolean> wrapped = retryer.wrap(callable);
        assertTrue(wrapped.call());
    }

    @Test
    public void testWhetherBuilderFailsForNullStopStrategy() {
        try {
            RetryerBuilder.<Void>newBuilder()
                    .withStopStrategy(null)
                    .build();
            fail("Exepcted to fail for null stop strategy");
        } catch (NullPointerException exception) {
            assertTrue(exception.getMessage().contains("stopStrategy may not be null"));
        }
    }

    @Test
    public void testWhetherBuilderFailsForNullWaitStrategy() {
        try {
            RetryerBuilder.<Void>newBuilder()
                    .withWaitStrategy(null)
                    .build();
            fail("Exepcted to fail for null wait strategy");
        } catch (NullPointerException exception) {
            assertTrue(exception.getMessage().contains("waitStrategy may not be null"));
        }
    }

    @Test
    public void testWhetherBuilderFailsForNullWaitStrategyWithCompositeStrategies() {
        try {
            RetryerBuilder.<Void>newBuilder()
                    .withWaitStrategy(WaitStrategies.join(null, null))
                    .build();
            fail("Exepcted to fail for null wait strategy");
        } catch (IllegalStateException exception) {
            assertTrue(exception.getMessage().contains("Cannot have a null wait strategy"));
        }
    }

    @Test
    public void testRetryListener_SuccessfulAttempt() throws Exception {
        final Map<Long, Attempt> attempts = new HashMap<Long, Attempt>();

        RetryListener listener = new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                attempts.put(attempt.getAttemptNumber(), attempt);
            }
        };

        Callable<Boolean> callable = notNullAfter5Attempts();

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .withRetryListener(listener)
                .build();
        assertTrue(retryer.call(callable));

        assertEquals(6, attempts.size());

        assertResultAttempt(attempts.get(1L), true, null);
        assertResultAttempt(attempts.get(2L), true, null);
        assertResultAttempt(attempts.get(3L), true, null);
        assertResultAttempt(attempts.get(4L), true, null);
        assertResultAttempt(attempts.get(5L), true, null);
        assertResultAttempt(attempts.get(6L), true, true);
    }

    @Test
    public void testRetryListener_WithException() throws Exception {
        final Map<Long, Attempt> attempts = new HashMap<Long, Attempt>();

        RetryListener listener = new RetryListener() {
            @Override
            public <V> void onRetry(Attempt<V> attempt) {
                attempts.put(attempt.getAttemptNumber(), attempt);
            }
        };

        Callable<Boolean> callable = noIOExceptionAfter5Attempts();

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(Predicates.<Boolean>isNull())
                .retryIfException()
                .withRetryListener(listener)
                .build();
        assertTrue(retryer.call(callable));

        assertEquals(6, attempts.size());

        assertExceptionAttempt(attempts.get(1L), true, IOException.class);
        assertExceptionAttempt(attempts.get(2L), true, IOException.class);
        assertExceptionAttempt(attempts.get(3L), true, IOException.class);
        assertExceptionAttempt(attempts.get(4L), true, IOException.class);
        assertExceptionAttempt(attempts.get(5L), true, IOException.class);
        assertResultAttempt(attempts.get(6L), true, true);
    }

    @Test
    public void testMultipleRetryListeners() throws Exception {
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return true;
            }
        };

        final AtomicBoolean listenerOne = new AtomicBoolean(false);
        final AtomicBoolean listenerTwo = new AtomicBoolean(false);

        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        listenerOne.set(true);
                    }
                })
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        listenerTwo.set(true);
                    }
                })
                .build();

        assertTrue(retryer.call(callable));
        assertTrue(listenerOne.get());
        assertTrue(listenerTwo.get());
    }

    private void assertResultAttempt(Attempt actualAttempt, boolean expectedHasResult, Object expectedResult) {
        assertFalse(actualAttempt.hasException());
        assertEquals(expectedHasResult, actualAttempt.hasResult());
        assertEquals(expectedResult, actualAttempt.getResult());
    }

    private void assertExceptionAttempt(Attempt actualAttempt, boolean expectedHasException, Class<?> expectedExceptionClass) {
        assertFalse(actualAttempt.hasResult());
        assertEquals(expectedHasException, actualAttempt.hasException());
        assertTrue(expectedExceptionClass.isInstance(actualAttempt.getExceptionCause()));
    }

    private Callable<Boolean> alwaysNull(final CountDownLatch latch) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                latch.countDown();
                return null;
            }
        };
    }

    @Test
    public void testRunnable() throws Exception {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                throw new NullPointerException();
            }
        };

        Retryer retryer = RetryerBuilder.newBuilder()
            .retryIfRuntimeException()
            .withStopStrategy(StopStrategies.stopAfterAttempt(3))
            .retryIfRuntimeException()
            .build();

        try {
            retryer.run(runnable);
            fail("RetryException expected");
        } catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof NullPointerException);
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }
}
