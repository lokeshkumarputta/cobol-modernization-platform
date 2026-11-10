package com.modernization.customer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Bean
    NewTopic customerUpdatedTopic(CustomerEventsProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(properties.partitions())
                .replicas(properties.replicas())
                .build();
    }

    /** Serialize events with the application ObjectMapper so timestamps are ISO-8601, matching the REST API. */
    @Bean
    @SuppressWarnings("unchecked")
    DefaultKafkaProducerFactoryCustomizer jsonValueSerializerCustomizer(ObjectMapper objectMapper) {
        return factory -> ((DefaultKafkaProducerFactory<Object, Object>) factory)
                .setValueSerializerSupplier(() -> new JsonSerializer<>(objectMapper));
    }
}
