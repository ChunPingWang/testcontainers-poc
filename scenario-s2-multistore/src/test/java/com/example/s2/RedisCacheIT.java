package com.example.s2;

import com.example.s2.domain.Customer;
import com.example.s2.service.CacheService;
import com.example.s2.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis cache operations.
 * Tests cache read-through and write-through patterns.
 *
 * Validates:
 * - Cache hit scenarios
 * - Cache miss scenarios
 * - Write-through cache population
 * - Read-through cache population
 * - Cache eviction
 */
@SpringBootTest
@ActiveProfiles("test")
class RedisCacheIT extends S2IntegrationTestBase {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        cacheService.clearAll();
    }

    @Test
    @DisplayName("Write-through: Creating customer should populate cache")
    void createCustomer_shouldPopulateCache() {
        // When
        Customer customer = customerService.createCustomer(
                "John Doe",
                "john.doe." + UUID.randomUUID() + "@example.com",
                "123-456-7890",
                "123 Main St"
        );

        // Then
        assertThat(customerService.isCached(customer.getId())).isTrue();

        Optional<Customer> cached = cacheService.get(customer.getId());
        assertThat(cached).isPresent();
        assertThat(cached.get().getName()).isEqualTo("John Doe");
        assertThat(cached.get().getEmail()).isEqualTo(customer.getEmail());
    }

    @Test
    @DisplayName("Read-through: Cache hit should return cached data without DB query")
    void findById_cacheHit_shouldReturnCachedData() {
        // Given
        Customer customer = customerService.createCustomer(
                "Jane Doe",
                "jane.doe." + UUID.randomUUID() + "@example.com",
                "098-765-4321",
                "456 Oak Ave"
        );
        assertThat(customerService.isCached(customer.getId())).isTrue();

        // When - First read (should hit cache)
        Optional<Customer> found = customerService.findById(customer.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(customer.getId());
        assertThat(found.get().getName()).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("Read-through: Cache miss should fetch from DB and populate cache")
    void findById_cacheMiss_shouldFetchFromDbAndPopulateCache() {
        // Given
        Customer customer = customerService.createCustomer(
                "Cache Miss User",
                "cache.miss." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        UUID customerId = customer.getId();

        // Evict from cache to simulate cache miss
        customerService.evictFromCache(customerId);
        assertThat(customerService.isCached(customerId)).isFalse();

        // When
        Optional<Customer> found = customerService.findById(customerId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Cache Miss User");

        // Cache should now be populated
        assertThat(customerService.isCached(customerId)).isTrue();
    }

    @Test
    @DisplayName("Cache eviction should remove customer from cache")
    void evictFromCache_shouldRemoveFromCache() {
        // Given
        Customer customer = customerService.createCustomer(
                "Eviction Test",
                "eviction." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        assertThat(customerService.isCached(customer.getId())).isTrue();

        // When
        customerService.evictFromCache(customer.getId());

        // Then
        assertThat(customerService.isCached(customer.getId())).isFalse();
    }

    @Test
    @DisplayName("Update customer should update cache")
    void updateCustomer_shouldUpdateCache() {
        // Given
        Customer customer = customerService.createCustomer(
                "Update Test",
                "update.test." + UUID.randomUUID() + "@example.com",
                "111-222-3333",
                "Old Address"
        );

        // When
        Customer updated = customerService.updateCustomer(
                customer.getId(),
                "Updated Name",
                "999-888-7777",
                "New Address"
        );

        // Then
        Optional<Customer> cached = cacheService.get(customer.getId());
        assertThat(cached).isPresent();
        assertThat(cached.get().getName()).isEqualTo("Updated Name");
        assertThat(cached.get().getPhone()).isEqualTo("999-888-7777");
        assertThat(cached.get().getAddress()).isEqualTo("New Address");
    }

    @Test
    @DisplayName("Delete customer should evict from cache")
    void deleteCustomer_shouldEvictFromCache() {
        // Given
        Customer customer = customerService.createCustomer(
                "Delete Test",
                "delete.test." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        assertThat(customerService.isCached(customer.getId())).isTrue();

        // When
        customerService.deleteCustomer(customer.getId());

        // Then
        assertThat(customerService.isCached(customer.getId())).isFalse();
    }

    @Test
    @DisplayName("Non-existent customer should not be in cache")
    void findById_nonExistent_shouldNotBeInCache() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When
        Optional<Customer> found = customerService.findById(nonExistentId);

        // Then
        assertThat(found).isEmpty();
        assertThat(customerService.isCached(nonExistentId)).isFalse();
    }

    @Test
    @DisplayName("Cache should have TTL")
    void cachePut_shouldHaveTtl() {
        // Given
        Customer customer = customerService.createCustomer(
                "TTL Test",
                "ttl.test." + UUID.randomUUID() + "@example.com",
                null,
                null
        );

        // Then
        var ttl = cacheService.getTtl(customer.getId());
        assertThat(ttl).isPresent();
        assertThat(ttl.get().toMinutes()).isGreaterThan(0);
    }
}
