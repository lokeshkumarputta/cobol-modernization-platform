package com.modernization.customer.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.modernization.customer.config.CacheConfig;
import com.modernization.customer.entity.CustomerEntity;
import com.modernization.customer.repository.CustomerJpaRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
class CustomerApiIT extends AbstractIntegrationTest {

    private static KafkaConsumer<String, String> consumer;

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CustomerJpaRepository repository;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void subscribe() {
        consumer = new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of("customer.updated.v1"));
    }

    @AfterAll
    static void close() {
        consumer.close();
    }

    private static RequestPostProcessor writer() {
        return jwt().jwt(j -> j.claim("preferred_username", "it-user"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_WRITE"));
    }

    @Test
    void contactUpdatePersistsBumpsVersionCachesAndPublishesEvent() throws Exception {
        repository.saveAndFlush(TestData.customer(2001L, "A", "100.00", 500));

        mvc.perform(get("/api/v1/customers/2001").with(writer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));

        mvc.perform(put("/api/v1/customers/2001/contact").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"firstName":"jane","email":"Jane@Example.com",
                                 "address":{"line1":"5 ELM ST","city":"DALLAS","state":"TX","zip":"75001"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("JANE"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.address.country").value("USA"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.lastUpdatedBy").value("it-user"));

        CustomerEntity stored = repository.findById(2001L).orElseThrow();
        assertThat(stored.getFirstName()).isEqualTo("JANE");
        assertThat(stored.getCity()).isEqualTo("DALLAS");
        assertThat(stored.getVersion()).isEqualTo(1L);
        assertThat(stored.getLastUpdatedAt()).isNotNull();

        assertThat(cacheManager.getCache(CacheConfig.CUSTOMER_CACHE).get(2001L)).isNotNull();
        mvc.perform(get("/api/v1/customers/2001").with(writer()))
                .andExpect(jsonPath("$.firstName").value("JANE"))
                .andExpect(jsonPath("$.version").value(1));

        JsonNode event = awaitEvent("2001");
        assertThat(event.get("transactionCode").asText()).isEqualTo("U");
        assertThat(event.get("newVersion").asLong()).isEqualTo(1L);
        assertThat(event.get("updatedBy").asText()).isEqualTo("it-user");
        assertThat(event.get("occurredAt").isTextual()).isTrue();
    }

    @Test
    void staleVersionIsRejectedWith409() throws Exception {
        repository.saveAndFlush(TestData.customer(2002L, "A", "0.00", 500));

        mvc.perform(put("/api/v1/customers/2002/status").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":7,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ruleId").value("BR004"));
    }

    @Test
    void closedCustomerIsRejectedAndNothingChanges() throws Exception {
        repository.saveAndFlush(TestData.customer(2003L, "C", "0.00", 500));

        mvc.perform(put("/api/v1/customers/2003/credit-limit").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"newCreditLimit\":100.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("E005"));

        assertThat(repository.findById(2003L).orElseThrow().getVersion()).isZero();
    }

    @Test
    void creditLimitIncreaseBlockedForHighRisk() throws Exception {
        repository.saveAndFlush(TestData.customer(2004L, "A", "0.00", 750));

        mvc.perform(put("/api/v1/customers/2004/credit-limit").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"newCreditLimit\":9000.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.ruleId").value("BR018"));
    }

    @Test
    void unknownCustomerIs404() throws Exception {
        mvc.perform(get("/api/v1/customers/999999").with(writer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ruleId").value("BR003"));
    }

    @Test
    void openApiAndActuatorArePublic() throws Exception {
        String spec = mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(spec).contains("/api/v1/customers/{customerId}/credit-limit");
        Files.writeString(Path.of("target", "openapi.yaml"), spec);

        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    private JsonNode awaitEvent(String key) {
        List<JsonNode> found = new ArrayList<>();
        await().atMost(Duration.ofSeconds(30)).until(() -> {
            for (ConsumerRecord<String, String> rec : consumer.poll(Duration.ofMillis(500))) {
                if (key.equals(rec.key())) {
                    found.add(objectMapper.readTree(rec.value()));
                }
            }
            return !found.isEmpty();
        });
        return found.get(0);
    }
}
