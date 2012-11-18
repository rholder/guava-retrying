package com.github.rholder.retry;

import com.github.rholder.retry.Retryer.RetryerCallable;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
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
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
        }
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
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IOException);
        }

        callable = noIllegalStateExceptionAfter5Attempts();
        retryer = RetryerBuilder.<Boolean>newBuilder()
                                .retryIfException()
                                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                                .build();
        try {
            retryer.call(callable);
            fail("RetryException expected");
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IllegalStateException);
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
        }
        catch (ExecutionException e) {
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
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IllegalStateException);
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
        }
        catch (ExecutionException e) {
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
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IOException);
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
        }
        catch (ExecutionException e) {
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
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IOException);
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
        }
        catch (RetryException e) {
            assertEquals(3, e.getNumberOfFailedAttempts());
            assertTrue(e.getLastFailedAttempt().hasResult());
            assertNull(e.getLastFailedAttempt().getResult());
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
        }
        catch (RetryException e) {
            assertTrue(e.getLastFailedAttempt().hasException());
            assertTrue(e.getLastFailedAttempt().getExceptionCause() instanceof IllegalStateException);
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
                }
                else if (counter < 2) {
                    counter++;
                    throw new IOException();
                }
                else if (counter < 5) {
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
                }
                catch (RetryException e) {
                    assertTrue(Thread.currentThread().isInterrupted());
                    result.set(true);
                }
                catch (ExecutionException e) {
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

    private Callable<Boolean> alwaysNull(final CountDownLatch latch) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                latch.countDown();
                return null;
            }
        };
    }
}
