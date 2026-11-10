package com.modernization.customer.domain;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.Address;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.model.CustomerStatus;
import com.modernization.customer.support.TestCustomers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CustomerTest {

    private static void assertRule(BusinessRule expected, Executable action) {
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class, action);
        assertThat(ex.getRule()).isEqualTo(expected);
        assertThat(ex.getMessage()).isEqualTo(expected.message());
    }

    @Test
    void verifyVersionRejectsStaleVersion_BR004() {
        Customer customer = TestCustomers.active().build();
        customer.verifyVersion(3L);
        assertRule(BusinessRule.BR004, () -> customer.verifyVersion(2L));
    }

    @Test
    void closedCustomerRejectsEveryTransaction_BR005() {
        Customer customer = TestCustomers.active().status(CustomerStatus.CLOSED).build();
        assertRule(BusinessRule.BR005, () -> customer.updateContact(new ContactUpdate("a", null, null, null, null)));
        assertRule(BusinessRule.BR005, () -> customer.changeStatus("X"));
        assertRule(BusinessRule.BR005, () -> customer.changeCreditLimit(BigDecimal.ONE));
    }

    @Test
    void stampAuditSetsUserAndTimestamp_BR019() {
        Customer customer = TestCustomers.active().build();
        Instant now = Instant.parse("2026-11-10T10:00:00Z");
        customer.stampAudit("jdoe", now);
        assertThat(customer.getLastUpdatedBy()).isEqualTo("jdoe");
        assertThat(customer.getLastUpdatedAt()).isEqualTo(now);
    }

    @Nested
    class ContactUpdateRules {

        @Test
        void suspendedCustomerCannotUpdateContact_BR006() {
            Customer customer = TestCustomers.active().status(CustomerStatus.SUSPENDED).build();
            assertRule(BusinessRule.BR006,
                    () -> customer.updateContact(new ContactUpdate("A", null, null, null, null)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"no-at-sign.example.com", "two@@example.com", "a@b@c"})
        void emailMustContainExactlyOneAt_BR007(String email) {
            Customer customer = TestCustomers.active().build();
            assertRule(BusinessRule.BR007,
                    () -> customer.updateContact(new ContactUpdate(null, null, email, null, null)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"555-123-4567", "(555)1234567", "55512345AB"})
        void phoneMustBeNumeric_BR008(String phone) {
            Customer customer = TestCustomers.active().build();
            assertRule(BusinessRule.BR008,
                    () -> customer.updateContact(new ContactUpdate(null, null, null, phone, null)));
        }

        @Test
        void onlySuppliedFieldsOverwriteWithLegacyCasing_BR009() {
            Customer customer = TestCustomers.active().build();
            customer.updateContact(new ContactUpdate("  jane ", "", "Jane.Doe@Example.COM", " 5550001111 ", null));

            assertThat(customer.getFirstName()).isEqualTo("JANE");
            assertThat(customer.getLastName()).isEqualTo("SMITH");
            assertThat(customer.getEmail()).isEqualTo("jane.doe@example.com");
            assertThat(customer.getPhone()).isEqualTo("5550001111");
            assertThat(customer.getAddress().city()).isEqualTo("AUSTIN");
        }

        @Test
        void lastNameUpperCased_BR009() {
            Customer customer = TestCustomers.active().build();
            customer.updateContact(new ContactUpdate(null, "doe", null, null, null));
            assertThat(customer.getLastName()).isEqualTo("DOE");
            assertThat(customer.getFirstName()).isEqualTo("JOHN");
        }

        @Test
        void addressReplacedOnlyWhenLine1SuppliedAndCountryDefaults_BR010() {
            Customer customer = TestCustomers.active().build();
            customer.updateContact(new ContactUpdate(null, null, null, null,
                    new Address(" ", "x", "y", "ZZ", "1", "CAN")));
            assertThat(customer.getAddress().city()).isEqualTo("AUSTIN");

            customer.updateContact(new ContactUpdate(null, null, null, null,
                    new Address("5 ELM ST", null, "DALLAS", "TX", "75001", "")));
            assertThat(customer.getAddress()).isEqualTo(new Address("5 ELM ST", null, "DALLAS", "TX", "75001", "USA"));

            customer.updateContact(new ContactUpdate(null, null, null, null,
                    new Address("1 QUEEN ST", null, "TORONTO", "ON", "M5H", "CAN")));
            assertThat(customer.getAddress().country()).isEqualTo("CAN");
        }
    }

    @Nested
    class StatusRules {

        @Test
        void invalidStatusCode_BR011() {
            Customer customer = TestCustomers.active().build();
            assertRule(BusinessRule.BR011, () -> customer.changeStatus("X"));
            assertRule(BusinessRule.BR011, () -> customer.changeStatus(null));
        }

        @Test
        void cannotCloseWithOutstandingBalance_BR012() {
            Customer customer = TestCustomers.active().build();
            assertRule(BusinessRule.BR012, () -> customer.changeStatus("CLOSED"));
        }

        @Test
        void canCloseWithZeroBalance() {
            Customer customer = TestCustomers.active().accountBalance(new BigDecimal("0.00")).build();
            customer.changeStatus("C");
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        void cannotActivateWithNegativeBalance_BR013() {
            Customer customer = TestCustomers.active().status(CustomerStatus.INACTIVE)
                    .accountBalance(new BigDecimal("-1.00")).build();
            assertRule(BusinessRule.BR013, () -> customer.changeStatus("ACTIVE"));
        }

        @Test
        void validStatusChangeApplied() {
            Customer customer = TestCustomers.active().build();
            customer.changeStatus("suspended");
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        }
    }

    @Nested
    class CreditLimitRules {

        @Test
        void requiresActiveCustomer_BR014() {
            Customer customer = TestCustomers.active().status(CustomerStatus.INACTIVE).build();
            assertRule(BusinessRule.BR014, () -> customer.changeCreditLimit(new BigDecimal("100.00")));
        }

        @Test
        void cannotExceedMaximum_BR015() {
            Customer customer = TestCustomers.active().build();
            assertRule(BusinessRule.BR015, () -> customer.changeCreditLimit(new BigDecimal("1000000.01")));
        }

        @Test
        void maximumIsAllowed() {
            Customer customer = TestCustomers.active().riskScore(100).build();
            customer.changeCreditLimit(new BigDecimal("1000000.00"));
            assertThat(customer.getCreditLimit()).isEqualByComparingTo("1000000.00");
        }

        @Test
        void cannotGoBelowPositiveBalance_BR016() {
            Customer customer = TestCustomers.active().build();
            assertRule(BusinessRule.BR016, () -> customer.changeCreditLimit(new BigDecimal("1199.99")));
        }

        @Test
        void negativeBalanceBlocksIncrease_BR017() {
            Customer customer = TestCustomers.active().accountBalance(new BigDecimal("-10.00")).build();
            assertRule(BusinessRule.BR017, () -> customer.changeCreditLimit(new BigDecimal("6000.00")));
        }

        @Test
        void negativeBalanceAllowsDecrease() {
            Customer customer = TestCustomers.active().accountBalance(new BigDecimal("-10.00")).build();
            customer.changeCreditLimit(new BigDecimal("100.00"));
            assertThat(customer.getCreditLimit()).isEqualByComparingTo("100.00");
        }

        @Test
        void highRiskBlocksIncrease_BR018() {
            Customer customer = TestCustomers.active().riskScore(701).build();
            assertRule(BusinessRule.BR018, () -> customer.changeCreditLimit(new BigDecimal("6000.00")));
        }

        @Test
        void highRiskAllowsDecrease() {
            Customer customer = TestCustomers.active().riskScore(999).build();
            customer.changeCreditLimit(new BigDecimal("2000.00"));
            assertThat(customer.getCreditLimit()).isEqualByComparingTo("2000.00");
        }

        @Test
        void riskScoreOfExactly700AllowsIncrease() {
            Customer customer = TestCustomers.active().riskScore(700).build();
            customer.changeCreditLimit(new BigDecimal("7500.00"));
            assertThat(customer.getCreditLimit()).isEqualByComparingTo("7500.00");
        }
    }
}
