package com.modernization.customer.integration;

import com.modernization.customer.entity.CustomerEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

final class TestData {

    private TestData() {
    }

    static CustomerEntity customer(long id, String status, String balance, int riskScore) {
        CustomerEntity e = new CustomerEntity();
        e.setId(id);
        e.setFirstName("JOHN");
        e.setLastName("SMITH");
        e.setDateOfBirth(LocalDate.of(1980, 4, 12));
        e.setEmail("john@example.com");
        e.setPhone("5551234567");
        e.setAddressLine1("1 MAIN ST");
        e.setCity("AUSTIN");
        e.setState("TX");
        e.setZip("73301");
        e.setCountry("USA");
        e.setStatus(status);
        e.setType("I");
        e.setCreditLimit(new BigDecimal("5000.00"));
        e.setAccountBalance(new BigDecimal(balance));
        e.setRiskScore(riskScore);
        return e;
    }
}
