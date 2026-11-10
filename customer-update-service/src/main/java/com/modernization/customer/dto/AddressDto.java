package com.modernization.customer.dto;

import jakarta.validation.constraints.Size;

public record AddressDto(
        @Size(max = 40) String line1,
        @Size(max = 40) String line2,
        @Size(max = 30) String city,
        @Size(max = 2) String state,
        @Size(max = 10) String zip,
        @Size(max = 3) String country) {
}
