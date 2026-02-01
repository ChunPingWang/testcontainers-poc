package com.example.s2;

import com.example.s2.domain.Customer;
import com.example.s2.service.CustomerService;
import com.example.s2.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for Elasticsearch search operations.
 * Tests search index synchronization with the database.
 *
 * Validates SC-011: Search index synchronization within 5 seconds
 *
 * Tests:
 * - Index creation on customer create
 * - Index update on customer update
 * - Index deletion on customer delete
 * - Search by name
 * - Search by email
 * - Full-text search
 */
@SpringBootTest
@ActiveProfiles("test")
class ElasticsearchSyncIT extends S2IntegrationTestBase {

    private static final int SYNC_TIMEOUT_SECONDS = 5;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SearchService searchService;

    @BeforeEach
    void setUp() {
        // Clear search index before each test
        searchService.clearAll();
    }

    @Test
    @DisplayName("SC-011: Creating customer should index within 5 seconds")
    void createCustomer_shouldIndexWithin5Seconds() {
        // When
        Customer customer = customerService.createCustomer(
                "Indexed Customer",
                "indexed." + UUID.randomUUID() + "@example.com",
                "555-123-4567",
                "100 Search Lane"
        );

        // Then - Should be indexed within 5 seconds (SC-011)
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });
    }

    @Test
    @DisplayName("Search by name should return matching customers")
    void searchByName_shouldReturnMatchingCustomers() {
        // Given
        Customer customer1 = customerService.createCustomer(
                "Alice Smith",
                "alice.smith." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        Customer customer2 = customerService.createCustomer(
                "Bob Smith",
                "bob.smith." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        Customer customer3 = customerService.createCustomer(
                "Charlie Brown",
                "charlie.brown." + UUID.randomUUID() + "@example.com",
                null,
                null
        );

        // Wait for indexing
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(searchService.count()).isGreaterThanOrEqualTo(3);
                });

        // When
        List<SearchService.CustomerSearchResult> results = customerService.searchByName("Smith");

        // Then
        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.stream().map(SearchService.CustomerSearchResult::name))
                .allMatch(name -> name.contains("Smith"));
    }

    @Test
    @DisplayName("Search by email should return exact match")
    void searchByEmail_shouldReturnExactMatch() {
        // Given
        String uniqueEmail = "unique.email." + UUID.randomUUID() + "@example.com";
        Customer customer = customerService.createCustomer(
                "Email Test User",
                uniqueEmail,
                null,
                null
        );

        // Wait for indexing
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });

        // When
        List<SearchService.CustomerSearchResult> results = searchService.searchByEmail(uniqueEmail);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).email()).isEqualTo(uniqueEmail);
        assertThat(results.get(0).id()).isEqualTo(customer.getId());
    }

    @Test
    @DisplayName("Full-text search should search across multiple fields")
    void fullTextSearch_shouldSearchAcrossFields() {
        // Given
        Customer customer = customerService.createCustomer(
                "Search Test User",
                "fulltext." + UUID.randomUUID() + "@example.com",
                null,
                "123 Searchable Street"
        );

        // Wait for indexing
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });

        // When - Search by address
        List<SearchService.CustomerSearchResult> results = customerService.fullTextSearch("Searchable");

        // Then
        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
        assertThat(results.stream().anyMatch(r -> r.id().equals(customer.getId()))).isTrue();
    }

    @Test
    @DisplayName("SC-011: Updating customer should update index within 5 seconds")
    void updateCustomer_shouldUpdateIndexWithin5Seconds() {
        // Given
        Customer customer = customerService.createCustomer(
                "Original Name",
                "update.index." + UUID.randomUUID() + "@example.com",
                null,
                "Original Address"
        );

        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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

        // Then - Index should be updated within 5 seconds
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Optional<SearchService.CustomerSearchResult> result = searchService.findById(customer.getId());
                    assertThat(result).isPresent();
                    assertThat(result.get().name()).isEqualTo("Updated Name");
                    assertThat(result.get().address()).isEqualTo("Updated Address");
                });
    }

    @Test
    @DisplayName("SC-011: Deleting customer should remove from index within 5 seconds")
    void deleteCustomer_shouldRemoveFromIndexWithin5Seconds() {
        // Given
        Customer customer = customerService.createCustomer(
                "Delete Index Test",
                "delete.index." + UUID.randomUUID() + "@example.com",
                null,
                null
        );

        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });

        // When
        customerService.deleteCustomer(customer.getId());

        // Then - Should be removed from index within 5 seconds
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isFalse();
                });
    }

    @Test
    @DisplayName("Find by ID in search index should return indexed document")
    void findByIdInIndex_shouldReturnIndexedDocument() {
        // Given
        Customer customer = customerService.createCustomer(
                "Find By Id Test",
                "find.by.id." + UUID.randomUUID() + "@example.com",
                "111-222-3333",
                "456 Index Ave"
        );

        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(customerService.isIndexed(customer.getId())).isTrue();
                });

        // When
        Optional<SearchService.CustomerSearchResult> result = searchService.findById(customer.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(customer.getId());
        assertThat(result.get().name()).isEqualTo("Find By Id Test");
        assertThat(result.get().address()).isEqualTo("456 Index Ave");
    }

    @Test
    @DisplayName("Index count should match number of created customers")
    void indexCount_shouldMatchCreatedCustomers() {
        // Given
        long initialCount = searchService.count();

        customerService.createCustomer(
                "Count Test 1",
                "count.test.1." + UUID.randomUUID() + "@example.com",
                null,
                null
        );
        customerService.createCustomer(
                "Count Test 2",
                "count.test.2." + UUID.randomUUID() + "@example.com",
                null,
                null
        );

        // Then
        await().atMost(SYNC_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(searchService.count()).isEqualTo(initialCount + 2);
                });
    }
}
