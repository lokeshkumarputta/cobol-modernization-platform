package com.modernization.customer.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Mirrors AUD-RECORD written by 2500-WRITE-AUDIT. */
public record CustomerUpdatedEvent(
        UUID eventId,
        long customerId,
        String transactionCode,
        String oldStatus,
        String newStatus,
        BigDecimal oldCreditLimit,
        BigDecimal newCreditLimit,
        long newVersion,
        String updatedBy,
        Instant occurredAt) {

    public static final String EVENT_TYPE = "CustomerUpdated";
}
