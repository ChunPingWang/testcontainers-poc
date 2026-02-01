package com.example.s4.domain;

/**
 * Enum representing types of financial transactions.
 */
public enum TransactionType {
    /**
     * Deposit transaction - money added to account.
     */
    DEPOSIT,

    /**
     * Withdrawal transaction - money removed from account.
     */
    WITHDRAWAL,

    /**
     * Transfer transaction - money moved between accounts.
     */
    TRANSFER
}
