package com.modernization.customer.domain.port;

import com.modernization.customer.domain.model.Customer;

import java.util.Optional;

/** Outbound port replacing the EXEC SQL SELECT/UPDATE on CUSTOMER_MASTER. */
public interface CustomerPersistencePort {

    Optional<Customer> findById(long customerId);

    /** Persists the customer using optimistic locking and returns it with the incremented version. */
    Customer save(Customer customer);
}
