package com.modernization.customer.domain.model;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;

import java.math.BigDecimal;

/** Constants and stateless field rules ported from CUSTUPD WORKING-STORAGE and 2310-CONTACT-RULES. */
public final class CustomerRules {

    /** WS-MAX-CREDIT-LIMIT. */
    public static final BigDecimal MAX_CREDIT_LIMIT = new BigDecimal("1000000.00");
    /** WS-MAX-RISK-FOR-INCREASE. */
    public static final int MAX_RISK_SCORE_FOR_INCREASE = 700;

    private CustomerRules() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** BR007: exactly one '@' when supplied. */
    public static void validateEmail(String email) {
        if (isBlank(email)) {
            return;
        }
        long atCount = email.chars().filter(c -> c == '@').count();
        if (atCount != 1) {
            throw new BusinessRuleViolationException(BusinessRule.BR007);
        }
    }

    /** BR008: digits only (after trimming) when supplied. */
    public static void validatePhone(String phone) {
        if (isBlank(phone)) {
            return;
        }
        if (!phone.trim().chars().allMatch(Character::isDigit)) {
            throw new BusinessRuleViolationException(BusinessRule.BR008);
        }
    }
}
