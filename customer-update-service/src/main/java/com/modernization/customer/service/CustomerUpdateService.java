package com.modernization.customer.service;

import com.modernization.customer.config.CacheConfig;
import com.modernization.customer.domain.event.CustomerUpdatedEvent;
import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.model.TransactionCode;
import com.modernization.customer.domain.port.CustomerEventPublisherPort;
import com.modernization.customer.domain.port.CustomerPersistencePort;
import com.modernization.customer.dto.CustomerDto;
import com.modernization.customer.mapper.CustomerMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Application service for CUSTUPD. Each public method is one unit of work (EXEC SQL COMMIT / ROLLBACK):
 * fetch (2200), apply rules (2300), update (2400) and audit event (2500).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerUpdateService {

    static final String METRIC_NAME = "customer.update.transactions";

    private final CustomerPersistencePort persistence;
    private final CustomerEventPublisherPort eventPublisher;
    private final CustomerMapper mapper;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.CUSTOMER_CACHE, key = "#customerId")
    public CustomerDto getCustomer(long customerId) {
        return mapper.toDto(load(customerId));
    }

    @Transactional
    @CachePut(cacheNames = CacheConfig.CUSTOMER_CACHE, key = "#customerId")
    public CustomerDto updateContact(long customerId, long expectedVersion, ContactUpdate update, String user) {
        return apply(customerId, expectedVersion, TransactionCode.CONTACT_UPDATE, user,
                customer -> customer.updateContact(update));
    }

    @Transactional
    @CachePut(cacheNames = CacheConfig.CUSTOMER_CACHE, key = "#customerId")
    public CustomerDto changeStatus(long customerId, long expectedVersion, String newStatus, String user) {
        return apply(customerId, expectedVersion, TransactionCode.STATUS_CHANGE, user,
                customer -> customer.changeStatus(newStatus));
    }

    @Transactional
    @CachePut(cacheNames = CacheConfig.CUSTOMER_CACHE, key = "#customerId")
    public CustomerDto changeCreditLimit(long customerId, long expectedVersion, BigDecimal newLimit, String user) {
        return apply(customerId, expectedVersion, TransactionCode.CREDIT_LIMIT_CHANGE, user,
                customer -> customer.changeCreditLimit(newLimit));
    }

    private CustomerDto apply(long customerId, long expectedVersion, TransactionCode code, String user,
                              Consumer<Customer> rule) {
        try {
            Customer customer = load(customerId);
            customer.verifyVersion(expectedVersion);
            String oldStatus = customer.getStatus().name();
            BigDecimal oldLimit = customer.getCreditLimit();

            rule.accept(customer);
            customer.stampAudit(user, clock.instant());
            Customer saved = persistence.save(customer);

            eventPublisher.publish(new CustomerUpdatedEvent(UUID.randomUUID(), saved.getId(), code.code(),
                    oldStatus, saved.getStatus().name(), oldLimit, saved.getCreditLimit(),
                    saved.getVersion(), user, saved.getLastUpdatedAt()));
            count(code, "updated", "none");
            log.info("Customer {} updated: txn={} version={} user={}", customerId, code, saved.getVersion(), user);
            return mapper.toDto(saved);
        } catch (BusinessRuleViolationException e) {
            count(code, "rejected", e.getRule().name());
            log.warn("Customer {} rejected: txn={} rule={} code={}", customerId, code, e.getRule(),
                    e.getRule().errorCode());
            throw e;
        }
    }

    /** BR001 + BR003. */
    private Customer load(long customerId) {
        if (customerId <= 0) {
            throw new BusinessRuleViolationException(BusinessRule.BR001);
        }
        return persistence.findById(customerId)
                .orElseThrow(() -> new BusinessRuleViolationException(BusinessRule.BR003));
    }

    private void count(TransactionCode code, String outcome, String rule) {
        meterRegistry.counter(METRIC_NAME, "txn", code.name(), "outcome", outcome, "rule", rule).increment();
    }
}
