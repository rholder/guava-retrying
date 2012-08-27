package fr.free.jnizet.retry;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StopStrategiesTest {

    @Test
    public void testNeverStop() {
        assertFalse(StopStrategies.neverStop().shouldStop(3, 6546L));
    }

    @Test
    public void testStopAfterAttempt() {
        assertFalse(StopStrategies.stopAfterAttempt(3).shouldStop(2, 6546L));
        assertTrue(StopStrategies.stopAfterAttempt(3).shouldStop(3, 6546L));
        assertTrue(StopStrategies.stopAfterAttempt(3).shouldStop(4, 6546L));
    }

    @Test
    public void testStopAfterDelay() {
        assertFalse(StopStrategies.stopAfterDelay(1000L).shouldStop(2, 999L));
        assertTrue(StopStrategies.stopAfterDelay(3).shouldStop(2, 1000L));
        assertTrue(StopStrategies.stopAfterDelay(3).shouldStop(2, 1001L));
    }

}
