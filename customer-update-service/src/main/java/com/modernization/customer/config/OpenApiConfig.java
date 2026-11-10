package com.modernization.customer.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_JWT = "bearer-jwt";

    @Bean
    OpenAPI customerUpdateOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Customer Update Service")
                        .version("v1")
                        .description("REST replacement for mainframe batch program CUSTUPD (CUSTOMER_UPDATE.cbl). "
                                + "Business rule violations are returned as RFC 7807 problems with ruleId/errorCode."))
                .components(new Components().addSecuritySchemes(BEARER_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_JWT));
    }
}
