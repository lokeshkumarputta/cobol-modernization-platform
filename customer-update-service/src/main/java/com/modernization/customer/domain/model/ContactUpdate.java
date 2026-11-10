package com.modernization.customer.domain.model;

/** Payload of a 'U' transaction. Blank/null fields leave the existing value unchanged (BR009). */
public record ContactUpdate(String firstName, String lastName, String email, String phone, Address address) {
}
