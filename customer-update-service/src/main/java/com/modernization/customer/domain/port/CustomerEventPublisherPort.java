package com.modernization.customer.domain.port;

import com.modernization.customer.domain.event.CustomerUpdatedEvent;

/** Outbound port replacing the CUSTAUD audit file write (BR021). */
public interface CustomerEventPublisherPort {

    void publish(CustomerUpdatedEvent event);
}
