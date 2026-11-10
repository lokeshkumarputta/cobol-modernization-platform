package com.modernization.customer.controller;

import com.modernization.customer.dto.ContactUpdateRequest;
import com.modernization.customer.dto.CreditLimitChangeRequest;
import com.modernization.customer.dto.CustomerDto;
import com.modernization.customer.dto.StatusChangeRequest;
import com.modernization.customer.mapper.CustomerMapper;
import com.modernization.customer.service.CustomerUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer maintenance (former CUSTUPD transactions U, S, L)")
@ApiResponse(responseCode = "400", description = "Invalid request (BR001/BR002 or bean validation)")
@ApiResponse(responseCode = "404", description = "Customer not found (BR003)")
@ApiResponse(responseCode = "409", description = "Version conflict (BR004)")
@ApiResponse(responseCode = "422", description = "Business rule violation (BR005-BR018)")
public class CustomerController {

    private static final int MAX_USER_LENGTH = 64;

    private final CustomerUpdateService service;
    private final CustomerMapper mapper;

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a customer")
    public CustomerDto getCustomer(@PathVariable long customerId) {
        return service.getCustomer(customerId);
    }

    @PutMapping("/{customerId}/contact")
    @Operation(summary = "Update contact details and address (TXN-CODE 'U')")
    public CustomerDto updateContact(@PathVariable long customerId,
                                     @Valid @RequestBody ContactUpdateRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return service.updateContact(customerId, request.expectedVersion(), mapper.toContactUpdate(request),
                userOf(jwt));
    }

    @PutMapping("/{customerId}/status")
    @Operation(summary = "Change customer status (TXN-CODE 'S')")
    public CustomerDto changeStatus(@PathVariable long customerId,
                                    @Valid @RequestBody StatusChangeRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        return service.changeStatus(customerId, request.expectedVersion(), request.status(), userOf(jwt));
    }

    @PutMapping("/{customerId}/credit-limit")
    @Operation(summary = "Change credit limit (TXN-CODE 'L')")
    public CustomerDto changeCreditLimit(@PathVariable long customerId,
                                         @Valid @RequestBody CreditLimitChangeRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        return service.changeCreditLimit(customerId, request.expectedVersion(), request.newCreditLimit(),
                userOf(jwt));
    }

    private static String userOf(Jwt jwt) {
        String user = jwt.getClaimAsString("preferred_username");
        if (user == null || user.isBlank()) {
            user = jwt.getSubject();
        }
        return user.length() > MAX_USER_LENGTH ? user.substring(0, MAX_USER_LENGTH) : user;
    }
}
