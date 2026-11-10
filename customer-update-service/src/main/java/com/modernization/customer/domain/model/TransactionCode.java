package com.modernization.customer.domain.model;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;

import java.util.Arrays;

/** TXN-CODE 88-levels from the CUSTTXN transaction record. */
public enum TransactionCode {
    CONTACT_UPDATE("U"),
    STATUS_CHANGE("S"),
    CREDIT_LIMIT_CHANGE("L");

    private final String code;

    TransactionCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TransactionCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(t -> t.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleViolationException(BusinessRule.BR002));
    }
}
