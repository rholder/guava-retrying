package fr.free.jnizet.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.Sets;

public class WaitStrategiesTest {

    @Test
    public void testNoWait() {
        WaitStrategy noWait = WaitStrategies.noWait();
        assertEquals(0L, noWait.computeSleepTime(18, 9879L));
    }

    @Test
    public void testFixedWait() {
        WaitStrategy fixedWait = WaitStrategies.fixedWait(1000L, TimeUnit.MILLISECONDS);
        assertEquals(1000L, fixedWait.computeSleepTime(12, 6546L));
    }

    @Test
    public void testIncrementingWait() {
        WaitStrategy incrementingWait = WaitStrategies.incrementingWait(500L, TimeUnit.MILLISECONDS, 100L, TimeUnit.MILLISECONDS);
        assertEquals(500L, incrementingWait.computeSleepTime(1, 6546L));
        assertEquals(600L, incrementingWait.computeSleepTime(2, 6546L));
        assertEquals(700L, incrementingWait.computeSleepTime(3, 6546L));
    }

    @Test
    public void testRandomWait() {
        WaitStrategy randomWait = WaitStrategies.randomWait(1000L, TimeUnit.MILLISECONDS, 2000L, TimeUnit.MILLISECONDS);
        Set<Long> times = Sets.newHashSet();
        times.add(randomWait.computeSleepTime(1, 6546L));
        times.add(randomWait.computeSleepTime(1, 6546L));
        times.add(randomWait.computeSleepTime(1, 6546L));
        times.add(randomWait.computeSleepTime(1, 6546L));
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
        times.add(randomWait.computeSleepTime(1, 6546L));
        times.add(randomWait.computeSleepTime(1, 6546L));
        times.add(randomWait.computeSleepTime(1, 6546L));
        times.add(randomWait.computeSleepTime(1, 6546L));
        assertTrue(times.size() > 1); // if not, the random is not random
        for (long time : times) {
            assertTrue(time >= 0L);
            assertTrue(time <= 2000L);
        }
    }
}
