package com.modernization.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** REST equivalent of an 'S' transaction. */
public record StatusChangeRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @Schema(allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED", "CLOSED"}, example = "INACTIVE")
        @NotBlank String status) {
}
