package com.modernization.customer.domain.model;

/** CUST-ADDRESS group item from CUSTREC. */
public record Address(String line1, String line2, String city, String state, String zip, String country) {

    public static final String DEFAULT_COUNTRY = "USA";

    /** BR010: a blank country defaults to USA. */
    public Address withDefaultCountry() {
        return CustomerRules.isBlank(country)
                ? new Address(line1, line2, city, state, zip, DEFAULT_COUNTRY)
                : this;
    }
}
