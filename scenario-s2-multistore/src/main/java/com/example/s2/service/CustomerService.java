package com.example.s2.service;

import com.example.s2.domain.Customer;
import com.example.s2.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for Customer operations.
 * Orchestrates database, cache, and search operations.
 *
 * Implements:
 * - Write-through cache: writes to DB and cache simultaneously
 * - Read-through cache: reads from cache first, falls back to DB
 * - Search sync: updates search index on data changes
 */
@Service
@Transactional
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CacheService cacheService;
    private final SearchService searchService;

    public CustomerService(
            CustomerRepository customerRepository,
            CacheService cacheService,
            SearchService searchService) {
        this.customerRepository = customerRepository;
        this.cacheService = cacheService;
        this.searchService = searchService;
    }

    /**
     * Creates a new customer.
     * Writes to database, cache, and search index.
     *
     * @param name    customer name
     * @param email   customer email (must be unique)
     * @param phone   customer phone (optional)
     * @param address customer address (optional)
     * @return the created customer
     * @throws IllegalArgumentException if email already exists
     */
    public Customer createCustomer(String name, String email, String phone, String address) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Customer with email already exists: " + email);
        }

        Customer customer = new Customer(name, email);
        customer.setPhone(phone);
        customer.setAddress(address);

        // Write to database
        Customer saved = customerRepository.save(customer);
        log.info("Created customer in database: {}", saved.getId());

        // Write-through to cache
        cacheService.put(saved);
        log.debug("Cached customer: {}", saved.getId());

        // Sync to search index
        searchService.index(saved);
        log.debug("Indexed customer: {}", saved.getId());

        return saved;
    }

    /**
     * Finds a customer by ID.
     * Implements read-through cache pattern.
     *
     * @param id the customer ID
     * @return the customer if found
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findById(UUID id) {
        // Try cache first
        Optional<Customer> cached = cacheService.get(id);
        if (cached.isPresent()) {
            log.debug("Customer found in cache: {}", id);
            return cached;
        }

        // Cache miss - read from database
        Optional<Customer> fromDb = customerRepository.findById(id);
        fromDb.ifPresent(customer -> {
            // Populate cache for next read
            cacheService.put(customer);
            log.debug("Customer loaded from database and cached: {}", id);
        });

        return fromDb;
    }

    /**
     * Finds a customer by ID directly from database, bypassing cache.
     *
     * @param id the customer ID
     * @return the customer if found
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByIdFromDatabase(UUID id) {
        return customerRepository.findById(id);
    }

    /**
     * Finds a customer by email.
     *
     * @param email the email address
     * @return the customer if found
     */
    @Transactional(readOnly = true)
    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    /**
     * Updates a customer.
     * Updates database, cache, and search index.
     *
     * @param id      the customer ID
     * @param name    new name (optional)
     * @param phone   new phone (optional)
     * @param address new address (optional)
     * @return the updated customer
     * @throws IllegalArgumentException if customer not found
     */
    public Customer updateCustomer(UUID id, String name, String phone, String address) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        if (name != null) {
            customer.setName(name);
        }
        if (phone != null) {
            customer.setPhone(phone);
        }
        if (address != null) {
            customer.setAddress(address);
        }

        // Update database
        Customer saved = customerRepository.save(customer);
        log.info("Updated customer in database: {}", saved.getId());

        // Write-through to cache
        cacheService.put(saved);
        log.debug("Updated customer in cache: {}", saved.getId());

        // Sync to search index
        searchService.index(saved);
        log.debug("Re-indexed customer: {}", saved.getId());

        return saved;
    }

    /**
     * Deletes a customer.
     * Removes from database, cache, and search index.
     *
     * @param id the customer ID
     * @throws IllegalArgumentException if customer not found
     */
    public void deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }

        // Delete from database
        customerRepository.deleteById(id);
        log.info("Deleted customer from database: {}", id);

        // Evict from cache
        cacheService.evict(id);
        log.debug("Evicted customer from cache: {}", id);

        // Remove from search index
        searchService.delete(id);
        log.debug("Removed customer from search index: {}", id);
    }

    /**
     * Searches customers by name using Elasticsearch.
     *
     * @param name the name to search for
     * @return list of matching search results
     */
    @Transactional(readOnly = true)
    public List<SearchService.CustomerSearchResult> searchByName(String name) {
        return searchService.searchByName(name);
    }

    /**
     * Full-text search across customer fields.
     *
     * @param searchTerm the term to search for
     * @return list of matching search results
     */
    @Transactional(readOnly = true)
    public List<SearchService.CustomerSearchResult> fullTextSearch(String searchTerm) {
        return searchService.fullTextSearch(searchTerm);
    }

    /**
     * Gets all customers from the database.
     *
     * @return list of all customers
     */
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    /**
     * Checks if a customer is cached.
     *
     * @param id the customer ID
     * @return true if cached
     */
    public boolean isCached(UUID id) {
        return cacheService.exists(id);
    }

    /**
     * Checks if a customer is indexed in Elasticsearch.
     *
     * @param id the customer ID
     * @return true if indexed
     */
    public boolean isIndexed(UUID id) {
        return searchService.isIndexed(id);
    }

    /**
     * Evicts a customer from cache.
     *
     * @param id the customer ID
     */
    public void evictFromCache(UUID id) {
        cacheService.evict(id);
    }

    /**
     * Gets the count of indexed customers.
     *
     * @return the count
     */
    public long getIndexedCount() {
        return searchService.count();
    }
}
