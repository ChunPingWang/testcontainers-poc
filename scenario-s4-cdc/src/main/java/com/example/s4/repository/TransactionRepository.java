package com.example.s4.repository;

import com.example.s4.domain.Transaction;
import com.example.s4.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for Transaction entity.
 * Provides CRUD operations and custom queries for transactions.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Finds all transactions for a specific account.
     *
     * @param accountId the account identifier
     * @return list of transactions for the account
     */
    List<Transaction> findByAccountIdOrderByCreatedAtDesc(String accountId);

    /**
     * Finds all transactions of a specific type.
     *
     * @param type the transaction type
     * @return list of transactions of the given type
     */
    List<Transaction> findByType(TransactionType type);

    /**
     * Finds all transactions for an account of a specific type.
     *
     * @param accountId the account identifier
     * @param type the transaction type
     * @return list of matching transactions
     */
    List<Transaction> findByAccountIdAndType(String accountId, TransactionType type);
}
