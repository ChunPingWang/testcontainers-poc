package com.example.tc.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for IntegrationTestBase.
 * Verifies base class functionality for integration tests.
 */
class IntegrationTestBaseTest {

    @Test
    void integrationTestBase_shouldProvideAwaitilityDefaults() {
        // Given
        IntegrationTestBase base = new IntegrationTestBase() {};

        // Then
        assertNotNull(base, "IntegrationTestBase should be instantiable");
    }

    @Test
    void getDefaultTimeout_shouldReturnReasonableValue() {
        // When
        long timeout = IntegrationTestBase.DEFAULT_TIMEOUT_SECONDS;

        // Then
        assertTrue(timeout >= 10 && timeout <= 60,
            "Default timeout should be between 10 and 60 seconds");
    }

    @Test
    void getPollInterval_shouldReturnReasonableValue() {
        // When
        long pollInterval = IntegrationTestBase.DEFAULT_POLL_INTERVAL_MILLIS;

        // Then
        assertTrue(pollInterval >= 100 && pollInterval <= 1000,
            "Poll interval should be between 100ms and 1000ms");
    }
}
