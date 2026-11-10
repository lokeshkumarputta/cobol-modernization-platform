package com.modernization.customer.batch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTransactionParserTest {

    private final CustomerTransactionParser parser = new CustomerTransactionParser();

    @Test
    void parsesFixedWidthRecord() {
        String line = TxnRecordBuilder.txn("0000001001", "L", 3).newLimit("00000750000").user("OPS01").build();
        assertThat(line).hasSize(CustomerTransactionParser.RECORD_LENGTH);

        CustomerTransaction txn = parser.parse(line);

        assertThat(txn.customerId()).isEqualTo("0000001001");
        assertThat(txn.transactionCode()).isEqualTo("L");
        assertThat(txn.expectedVersion()).isEqualTo(3L);
        assertThat(txn.newCreditLimit()).isEqualByComparingTo("7500.00");
        assertThat(txn.userId()).isEqualTo("OPS01");
    }

    @Test
    void parsesContactFields() {
        String line = TxnRecordBuilder.txn("0000001001", "U", 0)
                .names("jane", "doe").email("jane@example.com").phone("5550001111")
                .address("5 ELM ST", "APT 2", "DALLAS", "TX", "75001", "").build();

        CustomerTransaction txn = parser.parse(line);

        assertThat(txn.firstName()).isEqualTo("jane");
        assertThat(txn.lastName()).isEqualTo("doe");
        assertThat(txn.email()).isEqualTo("jane@example.com");
        assertThat(txn.phone()).isEqualTo("5550001111");
        assertThat(txn.address().line2()).isEqualTo("APT 2");
        assertThat(txn.address().country()).isEmpty();
        assertThat(txn.newCreditLimit()).isNull();
    }

    @Test
    void shortAndNonNumericFieldsAreTolerated() {
        CustomerTransaction txn = parser.parse("ABC");
        assertThat(txn.customerId()).isEqualTo("ABC       ");
        assertThat(txn.expectedVersion()).isEqualTo(-1L);
        assertThat(txn.newCreditLimit()).isNull();
    }

    @Test
    void numericClassTestMatchesCobol() {
        assertThat(CustomerTransactionParser.isNumeric("0012")).isTrue();
        assertThat(CustomerTransactionParser.isNumeric("12 ")).isFalse();
        assertThat(CustomerTransactionParser.isNumeric("")).isFalse();
    }
}
