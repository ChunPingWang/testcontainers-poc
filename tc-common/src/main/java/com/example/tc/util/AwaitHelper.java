package com.example.tc.util;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for async wait operations in integration tests.
 * Provides convenient methods for waiting on conditions.
 */
public final class AwaitHelper {

    private AwaitHelper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Waits until a condition is true.
     *
     * @param condition the condition to wait for
     * @param timeoutSeconds the maximum time to wait
     */
    public static void waitUntil(Callable<Boolean> condition, long timeoutSeconds) {
        Awaitility.await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .until(condition);
    }

    /**
     * Waits until a condition is true with default timeout (30 seconds).
     *
     * @param condition the condition to wait for
     */
    public static void waitUntil(Callable<Boolean> condition) {
        waitUntil(condition, 30);
    }

    /**
     * Creates an Awaitility condition factory with custom timeout.
     *
     * @param timeout the timeout duration
     * @return the condition factory
     */
    public static ConditionFactory awaitWithTimeout(Duration timeout) {
        return Awaitility.await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(500));
    }

    /**
     * Creates an Awaitility condition factory with custom settings.
     *
     * @param timeoutSeconds the timeout in seconds
     * @param pollIntervalMillis the poll interval in milliseconds
     * @return the condition factory
     */
    public static ConditionFactory awaitWith(long timeoutSeconds, long pollIntervalMillis) {
        return Awaitility.await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .pollInterval(pollIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Waits for cache synchronization (SC-010: within 1 second).
     *
     * @param condition the condition to check
     */
    public static void waitForCacheSync(Callable<Boolean> condition) {
        Awaitility.await()
            .atMost(1, TimeUnit.SECONDS)
            .pollInterval(100, TimeUnit.MILLISECONDS)
            .until(condition);
    }

    /**
     * Waits for search index synchronization (SC-011: within 5 seconds).
     *
     * @param condition the condition to check
     */
    public static void waitForIndexSync(Callable<Boolean> condition) {
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .pollInterval(500, TimeUnit.MILLISECONDS)
            .until(condition);
    }

    /**
     * Waits for CDC event propagation (SC-012: within 3 seconds).
     *
     * @param condition the condition to check
     */
    public static void waitForCdcEvent(Callable<Boolean> condition) {
        Awaitility.await()
            .atMost(3, TimeUnit.SECONDS)
            .pollInterval(200, TimeUnit.MILLISECONDS)
            .until(condition);
    }
}
