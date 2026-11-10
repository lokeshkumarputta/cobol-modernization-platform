package com.modernization.customer.batch;

import com.modernization.customer.domain.model.Address;

import java.math.BigDecimal;

/** One TXN-RECORD from the CUSTTXN file. */
public record CustomerTransaction(
        String customerId,
        String transactionCode,
        long expectedVersion,
        String firstName,
        String lastName,
        String email,
        String phone,
        Address address,
        String newStatus,
        BigDecimal newCreditLimit,
        String userId) {
}
