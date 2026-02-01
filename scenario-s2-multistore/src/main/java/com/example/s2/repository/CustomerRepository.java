package com.example.s2.repository;

import com.example.s2.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Customer persistence operations.
 * Provides CRUD operations for Customer entities in PostgreSQL.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Finds a customer by email address.
     *
     * @param email the email address
     * @return the customer if found
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Finds customers by name containing the given string (case-insensitive).
     *
     * @param name     the name fragment to search for
     * @param pageable pagination information
     * @return page of customers matching the name pattern
     */
    Page<Customer> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Checks if a customer with the given email exists.
     *
     * @param email the email address
     * @return true if a customer with this email exists
     */
    boolean existsByEmail(String email);
}
