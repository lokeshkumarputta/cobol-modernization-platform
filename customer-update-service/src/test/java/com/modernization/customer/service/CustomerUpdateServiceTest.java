package com.modernization.customer.service;

import com.modernization.customer.domain.event.CustomerUpdatedEvent;
import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.model.CustomerStatus;
import com.modernization.customer.domain.port.CustomerEventPublisherPort;
import com.modernization.customer.domain.port.CustomerPersistencePort;
import com.modernization.customer.dto.CustomerDto;
import com.modernization.customer.mapper.CustomerMapper;
import com.modernization.customer.support.TestCustomers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerUpdateServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T10:30:00Z");

    @Mock
    private CustomerPersistencePort persistence;
    @Mock
    private CustomerEventPublisherPort eventPublisher;

    private SimpleMeterRegistry meterRegistry;
    private CustomerUpdateService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new CustomerUpdateService(persistence, eventPublisher, new CustomerMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC), meterRegistry);
    }

    private void persistenceBumpsVersion() {
        when(persistence.save(any())).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            return c.toBuilder().version(c.getVersion() + 1).build();
        });
    }

    @Test
    void getCustomerReturnsDto() {
        when(persistence.findById(1001L)).thenReturn(Optional.of(TestCustomers.active().build()));
        CustomerDto dto = service.getCustomer(1001L);
        assertThat(dto.customerId()).isEqualTo(1001L);
        assertThat(dto.status()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void rejectsNonPositiveId_BR001() {
        assertThatThrownBy(() -> service.getCustomer(0L))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getRule()).isEqualTo(BusinessRule.BR001));
        verifyNoInteractions(persistence);
    }

    @Test
    void rejectsUnknownCustomer_BR003() {
        when(persistence.findById(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.changeStatus(42L, 0L, "I", "jdoe"))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getRule()).isEqualTo(BusinessRule.BR003));
        assertThat(meterRegistry.get(CustomerUpdateService.METRIC_NAME)
                .tags("outcome", "rejected", "rule", "BR003").counter().count()).isEqualTo(1.0);
    }

    @Test
    void rejectsStaleVersionBeforeApplyingRules_BR004() {
        when(persistence.findById(1001L)).thenReturn(Optional.of(TestCustomers.active().build()));
        assertThatThrownBy(() -> service.changeCreditLimit(1001L, 2L, new BigDecimal("1.00"), "jdoe"))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getRule()).isEqualTo(BusinessRule.BR004));
        verify(persistence, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void contactUpdatePersistsStampsAuditAndPublishesEvent() {
        when(persistence.findById(1001L)).thenReturn(Optional.of(TestCustomers.active().build()));
        persistenceBumpsVersion();

        CustomerDto dto = service.updateContact(1001L, 3L,
                new ContactUpdate("jane", null, null, null, null), "jdoe");

        assertThat(dto.firstName()).isEqualTo("JANE");
        assertThat(dto.version()).isEqualTo(4L);
        assertThat(dto.lastUpdatedBy()).isEqualTo("jdoe");
        assertThat(dto.lastUpdatedAt()).isEqualTo(NOW);

        ArgumentCaptor<CustomerUpdatedEvent> event = ArgumentCaptor.forClass(CustomerUpdatedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().transactionCode()).isEqualTo("U");
        assertThat(event.getValue().newVersion()).isEqualTo(4L);
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
        assertThat(meterRegistry.get(CustomerUpdateService.METRIC_NAME)
                .tags("txn", "CONTACT_UPDATE", "outcome", "updated").counter().count()).isEqualTo(1.0);
    }

    @Test
    void statusChangeEventCarriesOldAndNewStatus() {
        when(persistence.findById(1001L)).thenReturn(Optional.of(TestCustomers.active().build()));
        persistenceBumpsVersion();

        service.changeStatus(1001L, 3L, "INACTIVE", "jdoe");

        ArgumentCaptor<CustomerUpdatedEvent> event = ArgumentCaptor.forClass(CustomerUpdatedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().oldStatus()).isEqualTo("ACTIVE");
        assertThat(event.getValue().newStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void creditLimitChangeEventCarriesOldAndNewLimit() {
        when(persistence.findById(1001L)).thenReturn(Optional.of(TestCustomers.active().build()));
        persistenceBumpsVersion();

        CustomerDto dto = service.changeCreditLimit(1001L, 3L, new BigDecimal("7500.00"), "jdoe");

        assertThat(dto.creditLimit()).isEqualByComparingTo("7500.00");
        ArgumentCaptor<CustomerUpdatedEvent> event = ArgumentCaptor.forClass(CustomerUpdatedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertThat(event.getValue().oldCreditLimit()).isEqualByComparingTo("5000.00");
        assertThat(event.getValue().newCreditLimit()).isEqualByComparingTo("7500.00");
        assertThat(event.getValue().transactionCode()).isEqualTo("L");
    }

    @Test
    void ruleViolationDoesNotPersistOrPublish() {
        when(persistence.findById(1001L)).thenReturn(Optional.of(
                TestCustomers.active().status(CustomerStatus.CLOSED).build()));

        assertThatThrownBy(() -> service.changeStatus(1001L, 3L, "A", "jdoe"))
                .isInstanceOfSatisfying(BusinessRuleViolationException.class,
                        e -> assertThat(e.getRule()).isEqualTo(BusinessRule.BR005));
        verify(persistence, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
