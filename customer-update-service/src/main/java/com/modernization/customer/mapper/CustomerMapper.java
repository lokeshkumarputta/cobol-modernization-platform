package com.modernization.customer.mapper;

import com.modernization.customer.domain.model.Address;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.domain.model.CustomerStatus;
import com.modernization.customer.domain.model.CustomerType;
import com.modernization.customer.dto.AddressDto;
import com.modernization.customer.dto.ContactUpdateRequest;
import com.modernization.customer.dto.CustomerDto;
import com.modernization.customer.entity.CustomerEntity;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toDomain(CustomerEntity e) {
        return Customer.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .dateOfBirth(e.getDateOfBirth())
                .email(e.getEmail())
                .phone(e.getPhone())
                .address(new Address(e.getAddressLine1(), e.getAddressLine2(), e.getCity(),
                        e.getState(), e.getZip(), e.getCountry()))
                .status(CustomerStatus.parse(e.getStatus()))
                .type(CustomerType.fromCode(e.getType()))
                .creditLimit(e.getCreditLimit())
                .accountBalance(e.getAccountBalance())
                .riskScore(e.getRiskScore())
                .lastUpdatedAt(e.getLastUpdatedAt())
                .lastUpdatedBy(e.getLastUpdatedBy())
                .version(e.getVersion())
                .build();
    }

    public CustomerEntity toEntity(Customer c) {
        CustomerEntity e = new CustomerEntity();
        e.setId(c.getId());
        e.setFirstName(c.getFirstName());
        e.setLastName(c.getLastName());
        e.setDateOfBirth(c.getDateOfBirth());
        e.setEmail(c.getEmail());
        e.setPhone(c.getPhone());
        Address a = c.getAddress();
        if (a != null) {
            e.setAddressLine1(a.line1());
            e.setAddressLine2(a.line2());
            e.setCity(a.city());
            e.setState(a.state());
            e.setZip(a.zip());
            e.setCountry(a.country());
        }
        e.setStatus(c.getStatus().code());
        e.setType(c.getType().code());
        e.setCreditLimit(c.getCreditLimit());
        e.setAccountBalance(c.getAccountBalance());
        e.setRiskScore(c.getRiskScore());
        e.setLastUpdatedAt(c.getLastUpdatedAt());
        e.setLastUpdatedBy(c.getLastUpdatedBy());
        e.setVersion(c.getVersion());
        return e;
    }

    public CustomerDto toDto(Customer c) {
        Address a = c.getAddress();
        AddressDto address = a == null ? null
                : new AddressDto(a.line1(), a.line2(), a.city(), a.state(), a.zip(), a.country());
        return new CustomerDto(c.getId(), c.getFirstName(), c.getLastName(), c.getDateOfBirth(), c.getEmail(),
                c.getPhone(), address, c.getStatus(), c.getType(), c.getCreditLimit(), c.getAccountBalance(),
                c.getRiskScore(), c.getLastUpdatedAt(), c.getLastUpdatedBy(), c.getVersion());
    }

    public ContactUpdate toContactUpdate(ContactUpdateRequest r) {
        AddressDto a = r.address();
        Address address = a == null ? null
                : new Address(a.line1(), a.line2(), a.city(), a.state(), a.zip(), a.country());
        return new ContactUpdate(r.firstName(), r.lastName(), r.email(), r.phone(), address);
    }
}
