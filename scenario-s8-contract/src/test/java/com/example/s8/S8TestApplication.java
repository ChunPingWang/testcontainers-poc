package com.example.s8;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * Test configuration for S8 Contract Testing scenario.
 * This scenario uses in-memory storage, so no containers are needed.
 */
@TestConfiguration(proxyBeanMethods = false)
public class S8TestApplication {

    // No additional beans needed for contract testing.
    // The OrderService uses in-memory storage.
}
