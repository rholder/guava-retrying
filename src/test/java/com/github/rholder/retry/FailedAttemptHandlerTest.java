package com.github.rholder.retry;

import com.github.rholder.retry.FailedAttemptEvent.FailureCause;
import com.google.common.base.Predicates;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FailedAttemptHandlerTest {


    @Test
    public void testGetAttemptException() throws ExecutionException {
        final AtomicInteger count = new AtomicInteger();
        FailedAttemptHandler handler = new FailedAttemptHandler() {
            public void handle(FailedAttemptEvent event) {
                assertEquals("expected exception to be the failure cause", FailureCause.EXCEPTION, event.getCause());
                assertTrue(event.getException() instanceof IOException);
                count.incrementAndGet();
            }
        };

        Retryer<Void> r = RetryerBuilder.<Void>newBuilder()
                .retryIfException()
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withFailedAttemptHandler(handler)
                .build();
        try {
            r.call(new Callable<Void>() {
                public Void call() throws Exception {
                    throw new IOException();
                }
            });
        } catch (RetryException ignored) {
        }
        assertEquals(2, count.get());
    }


    @Test
    public void testGetAttemptResult() throws ExecutionException {
        final AtomicInteger count = new AtomicInteger();
        FailedAttemptHandler handler = new FailedAttemptHandler() {
            public void handle(FailedAttemptEvent event) {
                assertEquals("expected result to be the failure cause", FailureCause.RESULT, event.getCause());
                assertTrue("expected result to be \"test\"", "test".equals(event.getResult()));
                count.incrementAndGet();
            }
        };

        Retryer<String> r = RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.equalTo("test"))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withFailedAttemptHandler(handler)
                .build();
        try {
            r.call(new Callable<String>() {
                public String call() throws Exception {
                    return "test";
                }
            });
        } catch (RetryException ignored) {
        }
        assertEquals(2, count.get());
    }


    @Test
    public void testSuccessfulAttempts() throws ExecutionException {
        final AtomicInteger count = new AtomicInteger();
        FailedAttemptHandler handler = new FailedAttemptHandler() {
            public void handle(FailedAttemptEvent event) {
                count.incrementAndGet();
            }
        };

        Retryer<String> r = RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.equalTo("wrong"))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withFailedAttemptHandler(handler)
                .build();
        try {
            r.call(new Callable<String>() {
                public String call() throws Exception {
                    return "right!";
                }
            });
        } catch (RetryException e) {
            fail("All tries should have succeeded");
        }
        assertEquals("FailedAttemptHandler called although no attempts failed", 0, count.get());
    }


    @Test(expected =  RetryException.class)
    public void testGetNextTime() throws ExecutionException, RetryException {
        FailedAttemptHandler handler = new FailedAttemptHandler() {
            public void handle(FailedAttemptEvent event) {
                assertEquals(10L, event.getNextWaitTime());
                assertEquals(10000L, event.getNextWaitTime(TimeUnit.MICROSECONDS));
            }
        };

        Retryer<String> r = RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.equalTo("wrong"))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withWaitStrategy(WaitStrategies.fixedWait(10, TimeUnit.MILLISECONDS))
                .withFailedAttemptHandler(handler)
                .build();
        r.call(new Callable<String>() {
            public String call() throws Exception {
                return "wrong";
            }
        });
    }

    @Test(expected = RetryException.class)
    public void testGetNextTimeWithoutWait() throws ExecutionException, RetryException {
        FailedAttemptHandler handler = new FailedAttemptHandler() {
            public void handle(FailedAttemptEvent event) {
                assertEquals(0L, event.getNextWaitTime());
                assertEquals(0L, event.getNextWaitTime(TimeUnit.MICROSECONDS));
            }
        };

        Retryer<String> r = RetryerBuilder.<String>newBuilder()
                .retryIfResult(Predicates.equalTo("wrong"))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withFailedAttemptHandler(handler)
                .build();
        r.call(new Callable<String>() {
            public String call() throws Exception {
                return "wrong";
            }
        });
    }

}
