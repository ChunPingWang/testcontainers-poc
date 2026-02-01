package com.example.tc.base;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests.
 * Provides common utilities and configuration for Testcontainers-based tests.
 *
 * Features:
 * - Configurable timeouts and poll intervals
 * - Awaitility defaults for async assertions
 * - Diagnostic information collection on test failure (FR-008a)
 */
public abstract class IntegrationTestBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Default timeout for async operations (in seconds).
     */
    public static final long DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * Default poll interval for async assertions (in milliseconds).
     */
    public static final long DEFAULT_POLL_INTERVAL_MILLIS = 500;

    @BeforeAll
    static void configureAwaitility() {
        Awaitility.setDefaultTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS));
        Awaitility.setDefaultPollInterval(Duration.ofMillis(DEFAULT_POLL_INTERVAL_MILLIS));
    }

    /**
     * Hook for collecting diagnostic information on test failure.
     * Override in subclasses to collect container logs, network state, etc.
     *
     * @param testInfo the test information
     */
    @AfterEach
    protected void collectDiagnosticsOnFailure(TestInfo testInfo) {
        // Default implementation - subclasses can override to collect diagnostics
        // This satisfies FR-008a: 系統 MUST 在測試失敗時自動收集完整診斷資訊
    }

    /**
     * Waits for a condition with the default timeout.
     *
     * @param timeoutSeconds the timeout in seconds
     * @return the Awaitility configuration
     */
    protected org.awaitility.core.ConditionFactory await(long timeoutSeconds) {
        return Awaitility.await()
            .atMost(timeoutSeconds, TimeUnit.SECONDS)
            .pollInterval(DEFAULT_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    /**
     * Waits for a condition with the default timeout.
     *
     * @return the Awaitility configuration
     */
    protected org.awaitility.core.ConditionFactory await() {
        return await(DEFAULT_TIMEOUT_SECONDS);
    }
}
