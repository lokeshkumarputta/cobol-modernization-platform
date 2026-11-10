package com.modernization.customer.domain.model;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Customer aggregate (CUSTREC). All CUSTUPD business rules that depend on customer state live here,
 * evaluated in the same order as paragraphs 2200-2330 so rejection codes match the legacy program.
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class Customer {

    private final long id;
    private String firstName;
    private String lastName;
    private final LocalDate dateOfBirth;
    private String email;
    private String phone;
    private Address address;
    private CustomerStatus status;
    private final CustomerType type;
    private BigDecimal creditLimit;
    private final BigDecimal accountBalance;
    private final int riskScore;
    private Instant lastUpdatedAt;
    private String lastUpdatedBy;
    private final long version;

    /** BR004: the caller's expected version must match the stored version. */
    public void verifyVersion(long expectedVersion) {
        if (expectedVersion != version) {
            throw new BusinessRuleViolationException(BusinessRule.BR004);
        }
    }

    /** 2310-CONTACT-RULES: BR005, BR006, BR007, BR008, BR009, BR010. */
    public void updateContact(ContactUpdate update) {
        ensureNotClosed();
        if (status == CustomerStatus.SUSPENDED) {
            throw new BusinessRuleViolationException(BusinessRule.BR006);
        }
        CustomerRules.validateEmail(update.email());
        CustomerRules.validatePhone(update.phone());

        if (!CustomerRules.isBlank(update.firstName())) {
            firstName = update.firstName().trim().toUpperCase(Locale.ROOT);
        }
        if (!CustomerRules.isBlank(update.lastName())) {
            lastName = update.lastName().trim().toUpperCase(Locale.ROOT);
        }
        if (!CustomerRules.isBlank(update.email())) {
            email = update.email().trim().toLowerCase(Locale.ROOT);
        }
        if (!CustomerRules.isBlank(update.phone())) {
            phone = update.phone().trim();
        }
        if (update.address() != null && !CustomerRules.isBlank(update.address().line1())) {
            address = update.address().withDefaultCountry();
        }
    }

    /** 2320-STATUS-RULES: BR005, BR011, BR012, BR013. */
    public void changeStatus(String requestedStatus) {
        ensureNotClosed();
        CustomerStatus newStatus = CustomerStatus.parse(requestedStatus);
        if (newStatus == CustomerStatus.CLOSED && accountBalance.signum() != 0) {
            throw new BusinessRuleViolationException(BusinessRule.BR012);
        }
        if (newStatus == CustomerStatus.ACTIVE && accountBalance.signum() < 0) {
            throw new BusinessRuleViolationException(BusinessRule.BR013);
        }
        status = newStatus;
    }

    /** 2330-LIMIT-RULES: BR005, BR014, BR015, BR016, BR017, BR018. */
    public void changeCreditLimit(BigDecimal newLimit) {
        ensureNotClosed();
        if (status != CustomerStatus.ACTIVE) {
            throw new BusinessRuleViolationException(BusinessRule.BR014);
        }
        if (newLimit.compareTo(CustomerRules.MAX_CREDIT_LIMIT) > 0) {
            throw new BusinessRuleViolationException(BusinessRule.BR015);
        }
        if (accountBalance.signum() > 0 && newLimit.compareTo(accountBalance) < 0) {
            throw new BusinessRuleViolationException(BusinessRule.BR016);
        }
        if (newLimit.compareTo(creditLimit) > 0) {
            if (accountBalance.signum() < 0) {
                throw new BusinessRuleViolationException(BusinessRule.BR017);
            }
            if (riskScore > CustomerRules.MAX_RISK_SCORE_FOR_INCREASE) {
                throw new BusinessRuleViolationException(BusinessRule.BR018);
            }
        }
        creditLimit = newLimit;
    }

    /** BR019: stamp audit columns; the version itself is bumped by the persistence layer. */
    public void stampAudit(String user, Instant timestamp) {
        lastUpdatedBy = user;
        lastUpdatedAt = timestamp;
    }

    /** BR005. */
    private void ensureNotClosed() {
        if (status == CustomerStatus.CLOSED) {
            throw new BusinessRuleViolationException(BusinessRule.BR005);
        }
    }
}
