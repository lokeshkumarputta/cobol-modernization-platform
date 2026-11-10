package com.modernization.customer.repository;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.port.CustomerPersistencePort;
import com.modernization.customer.mapper.CustomerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Replaces the EXEC SQL SELECT / UPDATE ... WHERE VERSION = :v on CUSTOMER_MASTER. */
@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerPersistencePort {

    private final CustomerJpaRepository repository;
    private final CustomerMapper mapper;

    @Override
    public Optional<Customer> findById(long customerId) {
        return repository.findById(customerId).map(mapper::toDomain);
    }

    @Override
    public Customer save(Customer customer) {
        try {
            return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(customer)));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BusinessRuleViolationException(BusinessRule.BR004);
        }
    }
}
