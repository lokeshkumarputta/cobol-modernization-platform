package com.modernization.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** REST equivalent of a 'U' transaction. Null/blank fields are left unchanged. */
public record ContactUpdateRequest(
        @Schema(description = "Version last read by the client (optimistic lock, BR004)", example = "0")
        @NotNull @PositiveOrZero Long expectedVersion,
        @Size(max = 30) String firstName,
        @Size(max = 30) String lastName,
        @Size(max = 60) String email,
        @Size(max = 15) String phone,
        @Valid AddressDto address) {
}
