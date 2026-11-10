package com.modernization.customer.domain;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.CustomerStatus;
import com.modernization.customer.domain.model.CustomerType;
import com.modernization.customer.domain.model.TransactionCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEnumsTest {

    @Test
    void customerStatusParsesCodesAndNames() {
        assertThat(CustomerStatus.parse("A")).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(CustomerStatus.parse(" closed ")).isEqualTo(CustomerStatus.CLOSED);
        assertThat(CustomerStatus.SUSPENDED.code()).isEqualTo("S");
    }

    @Test
    void transactionCodeRejectsUnknown_BR002() {
        assertThat(TransactionCode.fromCode("L")).isEqualTo(TransactionCode.CREDIT_LIMIT_CHANGE);
        assertThat(TransactionCode.CONTACT_UPDATE.code()).isEqualTo("U");
        assertThatThrownBy(() -> TransactionCode.fromCode("X"))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getRule()).isEqualTo(BusinessRule.BR002));
    }

    @Test
    void customerTypeFromCode() {
        assertThat(CustomerType.fromCode("B")).isEqualTo(CustomerType.BUSINESS);
        assertThat(CustomerType.INDIVIDUAL.code()).isEqualTo("I");
        assertThatThrownBy(() -> CustomerType.fromCode("Z")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyRuleHasLegacyErrorCode() {
        for (BusinessRule rule : BusinessRule.values()) {
            assertThat(rule.errorCode()).matches("E\\d{3}");
            assertThat(rule.errorCode().substring(1)).isEqualTo(rule.name().substring(2));
        }
    }
}
