package com.modernization.customer.support;

import com.modernization.customer.domain.model.Address;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.model.CustomerStatus;
import com.modernization.customer.domain.model.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class TestCustomers {

    private TestCustomers() {
    }

    public static Customer.CustomerBuilder active() {
        return Customer.builder()
                .id(1001L)
                .firstName("JOHN")
                .lastName("SMITH")
                .dateOfBirth(LocalDate.of(1980, 4, 12))
                .email("john.smith@example.com")
                .phone("5551234567")
                .address(new Address("1 MAIN ST", null, "AUSTIN", "TX", "73301", "USA"))
                .status(CustomerStatus.ACTIVE)
                .type(CustomerType.INDIVIDUAL)
                .creditLimit(new BigDecimal("5000.00"))
                .accountBalance(new BigDecimal("1200.00"))
                .riskScore(640)
                .version(3L);
    }
}
