package com.modernization.customer.controller;

import com.modernization.customer.config.SecurityConfig;
import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.dto.CustomerDto;
import com.modernization.customer.mapper.CustomerMapper;
import com.modernization.customer.service.CustomerUpdateService;
import com.modernization.customer.support.TestCustomers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, CustomerMapper.class})
class CustomerControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomerUpdateService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private final CustomerDto dto = new CustomerMapper().toDto(TestCustomers.active().build());

    private static RequestPostProcessor writer() {
        return jwt().jwt(j -> j.subject("sub-1").claim("preferred_username", "jdoe"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_WRITE"));
    }

    private static RequestPostProcessor reader() {
        return jwt().jwt(j -> j.subject("reader-sub"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_READ"));
    }

    @Test
    void getRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/customers/1001")).andExpect(status().isUnauthorized());
    }

    @Test
    void readerCanGetCustomer() throws Exception {
        when(service.getCustomer(1001L)).thenReturn(dto);
        mvc.perform(get("/api/v1/customers/1001").with(reader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1001))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void readerCannotUpdate() throws Exception {
        mvc.perform(put("/api/v1/customers/1001/status").with(reader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":3,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void updateContactUsesPreferredUsername() throws Exception {
        when(service.updateContact(eq(1001L), eq(3L), any(ContactUpdate.class), eq("jdoe"))).thenReturn(dto);
        mvc.perform(put("/api/v1/customers/1001/contact").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":3,"firstName":"jane",
                                 "address":{"line1":"5 ELM ST","city":"DALLAS","state":"TX","zip":"75001"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1001));
    }

    @Test
    void changeStatusFallsBackToSubject() throws Exception {
        when(service.changeStatus(1001L, 3L, "INACTIVE", "reader-sub")).thenReturn(dto);
        mvc.perform(put("/api/v1/customers/1001/status")
                        .with(jwt().jwt(j -> j.subject("reader-sub"))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_WRITE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":3,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        verify(service).changeStatus(1001L, 3L, "INACTIVE", "reader-sub");
    }

    @Test
    void changeCreditLimit() throws Exception {
        when(service.changeCreditLimit(1001L, 3L, new BigDecimal("7500.00"), "jdoe")).thenReturn(dto);
        mvc.perform(put("/api/v1/customers/1001/credit-limit").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":3,\"newCreditLimit\":7500.00}"))
                .andExpect(status().isOk());
    }

    @Test
    void beanValidationFailureIs400() throws Exception {
        mvc.perform(put("/api/v1/customers/1001/credit-limit").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newCreditLimit\":-1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void notFoundMapsTo404WithRuleId() throws Exception {
        when(service.getCustomer(9L)).thenThrow(new BusinessRuleViolationException(BusinessRule.BR003));
        mvc.perform(get("/api/v1/customers/9").with(reader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ruleId").value("BR003"))
                .andExpect(jsonPath("$.errorCode").value("E003"));
    }

    @Test
    void staleVersionMapsTo409() throws Exception {
        when(service.changeStatus(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new BusinessRuleViolationException(BusinessRule.BR004));
        mvc.perform(put("/api/v1/customers/1001/status").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("E004"));
    }

    @Test
    void closedCustomerMapsTo422() throws Exception {
        when(service.changeStatus(anyLong(), anyLong(), anyString(), anyString()))
                .thenThrow(new BusinessRuleViolationException(BusinessRule.BR005));
        mvc.perform(put("/api/v1/customers/1002/status").with(writer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.ruleId").value("BR005"))
                .andExpect(jsonPath("$.detail").value(BusinessRule.BR005.message()));
    }

    @Test
    void invalidIdMapsTo400() throws Exception {
        when(service.getCustomer(0L)).thenThrow(new BusinessRuleViolationException(BusinessRule.BR001));
        mvc.perform(get("/api/v1/customers/0").with(reader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("E001"));
    }

    @Test
    void unexpectedErrorMapsTo500() throws Exception {
        when(service.getCustomer(1001L)).thenThrow(new IllegalStateException("db down"));
        mvc.perform(get("/api/v1/customers/1001").with(reader()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("U999"));
    }
}
