package com.modernization.customer.event;

import com.modernization.customer.config.CustomerEventsProperties;
import com.modernization.customer.domain.event.CustomerUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Kafka producer replacing the CUSTAUD audit file (BR021). */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCustomerEventRelay {

    static final String EVENT_TYPE_HEADER = "eventType";

    private final KafkaTemplate<String, CustomerUpdatedEvent> kafkaTemplate;
    private final CustomerEventsProperties properties;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCustomerUpdated(CustomerUpdatedEvent event) {
        ProducerRecord<String, CustomerUpdatedEvent> producerRecord =
                new ProducerRecord<>(properties.topic(), String.valueOf(event.customerId()), event);
        producerRecord.headers().add(EVENT_TYPE_HEADER, CustomerUpdatedEvent.EVENT_TYPE.getBytes());
        kafkaTemplate.send(producerRecord).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for customer {}", event.eventId(), event.customerId(), ex);
            } else {
                log.debug("Published {} for customer {} to {}", event.eventId(), event.customerId(),
                        result.getRecordMetadata());
            }
        });
    }
}
