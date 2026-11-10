package com.modernization.customer.dto;

import com.modernization.customer.domain.model.CustomerStatus;
import com.modernization.customer.domain.model.CustomerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CustomerDto(
        long customerId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String email,
        String phone,
        AddressDto address,
        CustomerStatus status,
        CustomerType type,
        BigDecimal creditLimit,
        BigDecimal accountBalance,
        int riskScore,
        Instant lastUpdatedAt,
        String lastUpdatedBy,
        long version) {
}
