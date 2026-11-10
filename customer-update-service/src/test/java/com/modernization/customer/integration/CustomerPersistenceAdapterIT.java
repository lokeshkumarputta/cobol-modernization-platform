package com.modernization.customer.integration;

import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.port.CustomerPersistencePort;
import com.modernization.customer.entity.CustomerEntity;
import com.modernization.customer.repository.CustomerJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerPersistenceAdapterIT extends AbstractIntegrationTest {

    @Autowired
    private CustomerPersistencePort persistence;
    @Autowired
    private CustomerJpaRepository repository;

    @Test
    void saveIncrementsVersion_BR019() {
        repository.saveAndFlush(TestData.customer(3001L, "A", "0.00", 500));
        Customer customer = persistence.findById(3001L).orElseThrow();
        customer.changeStatus("I");
        customer.stampAudit("it", Instant.now());

        Customer saved = persistence.save(customer);

        assertThat(saved.getVersion()).isEqualTo(1L);
        assertThat(repository.findById(3001L).orElseThrow().getStatus()).isEqualTo("I");
    }

    @Test
    void concurrentUpdateIsDetected_BR004() {
        repository.saveAndFlush(TestData.customer(3002L, "A", "0.00", 500));
        Customer stale = persistence.findById(3002L).orElseThrow();

        CustomerEntity concurrent = repository.findById(3002L).orElseThrow();
        concurrent.setFirstName("OTHER");
        repository.saveAndFlush(concurrent);

        stale.changeStatus("I");
        assertThatThrownBy(() -> persistence.save(stale))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getRule()).isEqualTo(BusinessRule.BR004));
    }
}
