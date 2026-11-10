package com.modernization.customer.domain.model;

import java.util.Arrays;

/** CUST-TYPE 88-levels from CUSTREC. */
public enum CustomerType {
    INDIVIDUAL("I"),
    BUSINESS("B");

    private final String code;

    CustomerType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static CustomerType fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown customer type: " + code));
    }
}
