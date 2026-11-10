package com.modernization.customer.event;

import com.modernization.customer.config.CustomerEventsProperties;
import com.modernization.customer.domain.event.CustomerUpdatedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaCustomerEventRelayTest {

    @Mock
    private KafkaTemplate<String, CustomerUpdatedEvent> kafkaTemplate;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private final CustomerUpdatedEvent event = new CustomerUpdatedEvent(UUID.randomUUID(), 1001L, "S", "ACTIVE",
            "INACTIVE", BigDecimal.TEN, BigDecimal.TEN, 4L, "jdoe", Instant.now());

    @Test
    void sendsEventKeyedByCustomerId() {
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("customer.updated.v1", 0), 0, 0, 0, 0, 0);
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, metadata)));
        KafkaCustomerEventRelay relay = new KafkaCustomerEventRelay(kafkaTemplate,
                new CustomerEventsProperties("customer.updated.v1", 3, (short) 1));

        relay.onCustomerUpdated(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, CustomerUpdatedEvent>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        assertThat(captor.getValue().topic()).isEqualTo("customer.updated.v1");
        assertThat(captor.getValue().key()).isEqualTo("1001");
        assertThat(captor.getValue().value()).isEqualTo(event);
        assertThat(new String(captor.getValue().headers().lastHeader("eventType").value()))
                .isEqualTo(CustomerUpdatedEvent.EVENT_TYPE);
    }

    @Test
    void sendFailureIsLoggedNotThrown() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));
        KafkaCustomerEventRelay relay = new KafkaCustomerEventRelay(kafkaTemplate,
                new CustomerEventsProperties("t", 1, (short) 1));

        relay.onCustomerUpdated(event);

        verify(kafkaTemplate).send(any(ProducerRecord.class));
    }

    @Test
    void springPublisherDelegatesToApplicationEvents() {
        new SpringCustomerEventPublisher(applicationEventPublisher).publish(event);
        verify(applicationEventPublisher).publishEvent(event);
    }
}
