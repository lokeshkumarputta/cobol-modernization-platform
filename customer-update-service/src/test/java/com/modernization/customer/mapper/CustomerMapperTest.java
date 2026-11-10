package com.modernization.customer.mapper;

import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.domain.model.Customer;
import com.modernization.customer.dto.AddressDto;
import com.modernization.customer.dto.ContactUpdateRequest;
import com.modernization.customer.dto.CustomerDto;
import com.modernization.customer.entity.CustomerEntity;
import com.modernization.customer.support.TestCustomers;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    void entityRoundTripPreservesAllFields() {
        Customer original = TestCustomers.active().lastUpdatedBy("jdoe").build();
        CustomerEntity entity = mapper.toEntity(original);

        assertThat(entity.getStatus()).isEqualTo("A");
        assertThat(entity.getType()).isEqualTo("I");
        assertThat(entity.getCity()).isEqualTo("AUSTIN");

        Customer roundTripped = mapper.toDomain(entity);
        assertThat(roundTripped).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void entityWithoutAddress() {
        CustomerEntity entity = mapper.toEntity(TestCustomers.active().address(null).build());
        assertThat(entity.getAddressLine1()).isNull();
        CustomerDto dto = mapper.toDto(TestCustomers.active().address(null).build());
        assertThat(dto.address()).isNull();
    }

    @Test
    void toDtoCopiesFields() {
        CustomerDto dto = mapper.toDto(TestCustomers.active().build());
        assertThat(dto.customerId()).isEqualTo(1001L);
        assertThat(dto.address().zip()).isEqualTo("73301");
        assertThat(dto.version()).isEqualTo(3L);
    }

    @Test
    void toContactUpdateMapsAddress() {
        ContactUpdate update = mapper.toContactUpdate(new ContactUpdateRequest(1L, "a", "b", "c@d", "1",
                new AddressDto("l1", "l2", "city", "TX", "1", null)));
        assertThat(update.address().line1()).isEqualTo("l1");
        assertThat(mapper.toContactUpdate(new ContactUpdateRequest(1L, null, null, null, null, null)).address())
                .isNull();
    }
}
