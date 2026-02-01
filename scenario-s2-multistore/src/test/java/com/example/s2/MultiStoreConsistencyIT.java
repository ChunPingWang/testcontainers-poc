package com.example.s2;

import com.example.s2.domain.Customer;
import com.example.s2.service.CacheService;
import com.example.s2.service.CustomerService;
import com.example.s2.service.SearchService;
import com.example.tc.base.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for multi-store data consistency.
 * Tests that data remains consistent across PostgreSQL, Redis, and Elasticsearch.
 *
 * Validates:
 * - Data consistency after create operations
 * - Data consistency after update operations
 * - Data consistency after delete operations
 * - Eventual consistency across all three stores
 */
@SpringBootTest
@Import(S2TestApplication.class)
@ActiveProfiles("test")
class MultiStoreConsistencyIT extends IntegrationTestBase {

    private static final int CONSISTENCY_TIMEOUT_SECONDS = 5;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        // Clear cache and search index before each test
        cacheService.clearAll();
        searchService.clearAll();
    }

    @Test
    @DisplayName("Create should maintain consistency across all three stores")
    void create_shouldMaintainConsistencyAcrossAllStores() {
        // When
        Customer customer = customerService.createCustomer(
                "Consistency Create Test",
                "consistency.create." + UUID.randomUUID() + "@example.com",
                "123-456-7890",
                "123 Consistency Lane"
        );

        // Then - Verify all three stores have consistent data
        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Database
                    Optional<Customer> fromDb = customerService.findByIdFromDatabase(customer.getId());
                    assertThat(fromDb).isPresent();
                    assertThat(fromDb.get().getName()).isEqualTo("Consistency Create Test");
                    assertThat(fromDb.get().getEmail()).isEqualTo(customer.getEmail());

                    // Cache
                    Optional<Customer> fromCache = cacheService.get(customer.getId());
                    assertThat(fromCache).isPresent();
                    assertThat(fromCache.get().getName()).isEqualTo("Consistency Create Test");
                    assertThat(fromCache.get().getEmail()).isEqualTo(customer.getEmail());

                    // Search Index
                    Optional<SearchService.CustomerSearchResult> fromIndex = searchService.findById(customer.getId());
                    assertThat(fromIndex).isPresent();
                    assertThat(fromIndex.get().name()).isEqualTo("Consistency Create Test");
                    assertThat(fromIndex.get().email()).isEqualTo(customer.getEmail());
                });
    }

    @Test
    @DisplayName("Update should maintain consistency across all three stores")
    void update_shouldMaintainConsistencyAcrossAllStores() {
        // Given
        Customer customer = customerService.createCustomer(
                "Original Name",
                "consistency.update." + UUID.randomUUID() + "@example.com",
                "111-111-1111",
                "Original Address"
        );

        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });

        // When
        customerService.updateCustomer(
                customer.getId(),
                "Updated Name",
                "999-999-9999",
                "Updated Address"
        );

        // Then - Verify all three stores have consistent updated data
        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Database
                    Optional<Customer> fromDb = customerService.findByIdFromDatabase(customer.getId());
                    assertThat(fromDb).isPresent();
                    assertThat(fromDb.get().getName()).isEqualTo("Updated Name");
                    assertThat(fromDb.get().getPhone()).isEqualTo("999-999-9999");
                    assertThat(fromDb.get().getAddress()).isEqualTo("Updated Address");

                    // Cache
                    Optional<Customer> fromCache = cacheService.get(customer.getId());
                    assertThat(fromCache).isPresent();
                    assertThat(fromCache.get().getName()).isEqualTo("Updated Name");
                    assertThat(fromCache.get().getPhone()).isEqualTo("999-999-9999");
                    assertThat(fromCache.get().getAddress()).isEqualTo("Updated Address");

                    // Search Index
                    Optional<SearchService.CustomerSearchResult> fromIndex = searchService.findById(customer.getId());
                    assertThat(fromIndex).isPresent();
                    assertThat(fromIndex.get().name()).isEqualTo("Updated Name");
                    assertThat(fromIndex.get().address()).isEqualTo("Updated Address");
                });
    }

    @Test
    @DisplayName("Delete should maintain consistency across all three stores")
    void delete_shouldMaintainConsistencyAcrossAllStores() {
        // Given
        Customer customer = customerService.createCustomer(
                "Delete Consistency Test",
                "consistency.delete." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        UUID customerId = customer.getId();

        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customerId)).isTrue();
                    assertThat(customerService.isCached(customerId)).isTrue();
                });

        // When
        customerService.deleteCustomer(customerId);

        // Then - Verify customer is removed from all three stores
        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Database
                    Optional<Customer> fromDb = customerService.findByIdFromDatabase(customerId);
                    assertThat(fromDb).isEmpty();

                    // Cache
                    Optional<Customer> fromCache = cacheService.get(customerId);
                    assertThat(fromCache).isEmpty();

                    // Search Index
                    assertThat(searchService.isIndexed(customerId)).isFalse();
                });
    }

    @Test
    @DisplayName("Read-through should populate cache with database data")
    void readThrough_shouldPopulateCacheWithDatabaseData() {
        // Given - Create customer
        Customer customer = customerService.createCustomer(
                "Read Through Test",
                "read.through." + UUID.randomUUID() + "@example.com",
                "222-333-4444",
                "456 Read Lane"
        );

        // Evict from cache to force read-through
        cacheService.evict(customer.getId());
        assertThat(cacheService.exists(customer.getId())).isFalse();

        // When - Read through service (should trigger cache population)
        Optional<Customer> found = customerService.findById(customer.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Read Through Test");

        // Cache should now be populated with database data
        Optional<Customer> fromCache = cacheService.get(customer.getId());
        assertThat(fromCache).isPresent();
        assertThat(fromCache.get().getName()).isEqualTo("Read Through Test");
        assertThat(fromCache.get().getEmail()).isEqualTo(customer.getEmail());
        assertThat(fromCache.get().getPhone()).isEqualTo("222-333-4444");
        assertThat(fromCache.get().getAddress()).isEqualTo("456 Read Lane");
    }

    @Test
    @DisplayName("Cached data should match database data")
    void cachedData_shouldMatchDatabaseData() {
        // Given
        Customer customer = customerService.createCustomer(
                "Cache Match Test",
                "cache.match." + UUID.randomUUID() + "@example.com",
                "555-666-7777",
                "789 Cache Street"
        );

        // When
        Optional<Customer> fromDb = customerService.findByIdFromDatabase(customer.getId());
        Optional<Customer> fromCache = cacheService.get(customer.getId());

        // Then
        assertThat(fromDb).isPresent();
        assertThat(fromCache).isPresent();

        Customer dbCustomer = fromDb.get();
        Customer cachedCustomer = fromCache.get();

        assertThat(cachedCustomer.getId()).isEqualTo(dbCustomer.getId());
        assertThat(cachedCustomer.getName()).isEqualTo(dbCustomer.getName());
        assertThat(cachedCustomer.getEmail()).isEqualTo(dbCustomer.getEmail());
        assertThat(cachedCustomer.getPhone()).isEqualTo(dbCustomer.getPhone());
        assertThat(cachedCustomer.getAddress()).isEqualTo(dbCustomer.getAddress());
    }

    @Test
    @DisplayName("Indexed data should match database data")
    void indexedData_shouldMatchDatabaseData() {
        // Given
        Customer customer = customerService.createCustomer(
                "Index Match Test",
                "index.match." + UUID.randomUUID() + "@example.com",
                "888-999-0000",
                "101 Index Boulevard"
        );

        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });

        // When
        Optional<Customer> fromDb = customerService.findByIdFromDatabase(customer.getId());
        Optional<SearchService.CustomerSearchResult> fromIndex = searchService.findById(customer.getId());

        // Then
        assertThat(fromDb).isPresent();
        assertThat(fromIndex).isPresent();

        Customer dbCustomer = fromDb.get();
        SearchService.CustomerSearchResult indexedCustomer = fromIndex.get();

        assertThat(indexedCustomer.id()).isEqualTo(dbCustomer.getId());
        assertThat(indexedCustomer.name()).isEqualTo(dbCustomer.getName());
        assertThat(indexedCustomer.email()).isEqualTo(dbCustomer.getEmail());
        assertThat(indexedCustomer.address()).isEqualTo(dbCustomer.getAddress());
    }

    @Test
    @DisplayName("Multiple customers should maintain individual consistency")
    void multipleCustomers_shouldMaintainIndividualConsistency() {
        // Given
        Customer customer1 = customerService.createCustomer(
                "Multi Test Customer 1",
                "multi.1." + UUID.randomUUID() + "@example.com",
                "111-111-1111",
                "Address 1"
        );
        Customer customer2 = customerService.createCustomer(
                "Multi Test Customer 2",
                "multi.2." + UUID.randomUUID() + "@example.com",
                "222-222-2222",
                "Address 2"
        );
        Customer customer3 = customerService.createCustomer(
                "Multi Test Customer 3",
                "multi.3." + UUID.randomUUID() + "@example.com",
                "333-333-3333",
                "Address 3"
        );

        // Wait for all to be indexed
        await().atMost(CONSISTENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer1.getId())).isTrue();
                    assertThat(customerService.isIndexed(customer2.getId())).isTrue();
                    assertThat(customerService.isIndexed(customer3.getId())).isTrue();
                });

        // Then - Each customer should be consistent in all stores
        for (Customer customer : new Customer[]{customer1, customer2, customer3}) {
            Optional<Customer> fromDb = customerService.findByIdFromDatabase(customer.getId());
            Optional<Customer> fromCache = cacheService.get(customer.getId());
            Optional<SearchService.CustomerSearchResult> fromIndex = searchService.findById(customer.getId());

            assertThat(fromDb).isPresent();
            assertThat(fromCache).isPresent();
            assertThat(fromIndex).isPresent();

            assertThat(fromCache.get().getName()).isEqualTo(fromDb.get().getName());
            assertThat(fromIndex.get().name()).isEqualTo(fromDb.get().getName());
        }
    }
}
