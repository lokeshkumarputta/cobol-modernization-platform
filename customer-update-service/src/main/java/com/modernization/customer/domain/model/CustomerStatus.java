package com.modernization.customer.domain.model;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;

import java.util.Arrays;

/** CUST-STATUS 88-levels from CUSTREC. */
public enum CustomerStatus {
    ACTIVE("A"),
    INACTIVE("I"),
    SUSPENDED("S"),
    CLOSED("C");

    private final String code;

    CustomerStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Accepts either the legacy one-letter code or the enum name (BR011). */
    public static CustomerStatus parse(String value) {
        if (value == null) {
            throw new BusinessRuleViolationException(BusinessRule.BR011);
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(trimmed) || s.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(BusinessRule.BR011));
    }
}
