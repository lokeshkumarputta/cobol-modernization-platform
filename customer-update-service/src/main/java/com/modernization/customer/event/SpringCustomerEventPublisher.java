package com.modernization.customer.event;

import com.modernization.customer.domain.event.CustomerUpdatedEvent;
import com.modernization.customer.domain.port.CustomerEventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Hands the event to Spring so it is only relayed to Kafka once the DB transaction commits. */
@Component
@RequiredArgsConstructor
public class SpringCustomerEventPublisher implements CustomerEventPublisherPort {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(CustomerUpdatedEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
