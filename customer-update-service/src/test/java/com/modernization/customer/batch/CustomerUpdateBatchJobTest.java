package com.modernization.customer.batch;

import com.modernization.customer.config.CustomerBatchProperties;
import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.service.CustomerUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerUpdateBatchJobTest {

    @TempDir
    Path inbound;

    @Mock
    private CustomerUpdateService service;

    private CustomerUpdateBatchJob job;

    @BeforeEach
    void setUp() {
        job = new CustomerUpdateBatchJob(service, new CustomerTransactionParser(),
                new CustomerBatchProperties(true, inbound, "0 0 2 * * *"));
    }

    @Test
    void processesFileWritesRejectsAndRenames() throws IOException {
        when(service.changeStatus(1003L, 0L, "C", "BATCH:OPS01"))
                .thenThrow(new BusinessRuleViolationException(BusinessRule.BR012));
        Path file = inbound.resolve("custtxn-20261110.txn");
        Files.write(file, List.of(
                TxnRecordBuilder.txn("0000001001", "U", 3).names("jane", "").user("OPS01").build(),
                TxnRecordBuilder.txn("0000001001", "L", 4).newLimit("00000750000").user("OPS01").build(),
                "",
                TxnRecordBuilder.txn("0000001003", "S", 0).newStatus("C").user("OPS01").build(),
                TxnRecordBuilder.txn("00000ABC01", "S", 0).newStatus("A").build(),
                TxnRecordBuilder.txn("0000001001", "X", 0).build(),
                TxnRecordBuilder.txn("0000001001", "L", 5).build()));

        job.run();

        verify(service).updateContact(eq(1001L), eq(3L), any(ContactUpdate.class), eq("BATCH:OPS01"));
        verify(service).changeCreditLimit(1001L, 4L, new BigDecimal("7500.00"), "BATCH:OPS01");
        assertThat(file).doesNotExist();
        assertThat(inbound.resolve("custtxn-20261110.txn.done")).exists();
        List<String> rejects = Files.readAllLines(inbound.resolve("custtxn-20261110.txn.rej"));
        assertThat(rejects).hasSize(4);
        assertThat(rejects.get(0)).startsWith("0000001003SE012CANNOT CLOSE");
        assertThat(rejects.get(1)).startsWith("00000ABC01SE001");
        assertThat(rejects.get(2)).startsWith("0000001001XE002");
        assertThat(rejects.get(3)).startsWith("0000001001LE015");
    }

    @Test
    void processFileReturnsControlTotals() throws IOException {
        Path file = inbound.resolve("one.txn");
        Files.write(file, List.of(TxnRecordBuilder.txn("0000001001", "S", 1).newStatus("I").build()));

        BatchResult result = job.processFile(file);

        assertThat(result).isEqualTo(new BatchResult(1, 1, 0));
        verify(service).changeStatus(1001L, 1L, "I", "BATCH:BATCH01");
    }

    @Test
    void ignoresNonTransactionFiles() throws IOException {
        Files.writeString(inbound.resolve("readme.md"), "not a txn file");
        job.run();
        assertThat(inbound.resolve("readme.md")).exists();
    }

    @Test
    void missingInboundDirectoryFailsTheRun() {
        CustomerUpdateBatchJob broken = new CustomerUpdateBatchJob(service, new CustomerTransactionParser(),
                new CustomerBatchProperties(true, inbound.resolve("missing"), "0 0 2 * * *"));
        assertThatThrownBy(broken::run).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void unreadableFileFailsTheRun() {
        assertThatThrownBy(() -> job.processFile(inbound.resolve("nope.txn")))
                .isInstanceOf(UncheckedIOException.class);
    }
}
