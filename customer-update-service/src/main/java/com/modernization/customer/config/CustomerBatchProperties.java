package com.modernization.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

/** Replaces the CUSTTXN DD statement and the nightly JCL schedule. */
@ConfigurationProperties(prefix = "customer.batch")
public record CustomerBatchProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("/data/custtxn") Path inboundDir,
        @DefaultValue("0 0 2 * * *") String cron) {
}
