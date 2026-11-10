package com.modernization.customer.domain.exception;

import com.modernization.customer.domain.model.BusinessRule;

public class BusinessRuleViolationException extends RuntimeException {

    private final transient BusinessRule rule;

    public BusinessRuleViolationException(BusinessRule rule) {
        super(rule.message());
        this.rule = rule;
    }

    public BusinessRule getRule() {
        return rule;
    }
}
