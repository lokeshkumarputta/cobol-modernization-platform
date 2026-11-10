package com.modernization.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** REST equivalent of an 'L' transaction (TXN-NEW-LIMIT PIC 9(09)V99). */
public record CreditLimitChangeRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @Schema(example = "7500.00")
        @NotNull @DecimalMin("0.00") @Digits(integer = 9, fraction = 2) BigDecimal newCreditLimit) {
}
