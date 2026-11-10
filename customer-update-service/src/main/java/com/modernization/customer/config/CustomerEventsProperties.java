package com.modernization.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "customer.events")
public record CustomerEventsProperties(
        @DefaultValue("customer.updated.v1") String topic,
        @DefaultValue("3") int partitions,
        @DefaultValue("1") short replicas) {
}
