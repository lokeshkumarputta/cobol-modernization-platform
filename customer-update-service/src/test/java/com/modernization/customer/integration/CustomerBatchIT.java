package com.modernization.customer.integration;

import com.modernization.customer.batch.BatchResult;
import com.modernization.customer.batch.CustomerUpdateBatchJob;
import com.modernization.customer.batch.TxnRecordBuilder;
import com.modernization.customer.repository.CustomerJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "customer.batch.enabled=true",
        "customer.batch.cron=-"
})
class CustomerBatchIT extends AbstractIntegrationTest {

    @Autowired
    private CustomerUpdateBatchJob job;
    @Autowired
    private CustomerJpaRepository repository;

    @Test
    void appliesValidTransactionsAndRejectsInvalidOnes() throws Exception {
        repository.saveAndFlush(TestData.customer(4001L, "A", "100.00", 500));
        repository.saveAndFlush(TestData.customer(4002L, "A", "-50.00", 500));

        Path dir = Files.createTempDirectory("custtxn");
        Path file = dir.resolve("custtxn.txn");
        Files.write(file, List.of(
                TxnRecordBuilder.txn("0000004001", "L", 0).newLimit("00000800000").user("OPS01").build(),
                TxnRecordBuilder.txn("0000004001", "S", 1).newStatus("I").user("OPS01").build(),
                TxnRecordBuilder.txn("0000004002", "L", 0).newLimit("00000900000").build(),
                TxnRecordBuilder.txn("0000004002", "S", 0).newStatus("C").build()));

        BatchResult result = job.processFile(file);

        assertThat(result).isEqualTo(new BatchResult(4, 2, 2));
        var updated = repository.findById(4001L).orElseThrow();
        assertThat(updated.getCreditLimit()).isEqualByComparingTo("8000.00");
        assertThat(updated.getStatus()).isEqualTo("I");
        assertThat(updated.getVersion()).isEqualTo(2L);
        assertThat(updated.getLastUpdatedBy()).isEqualTo("BATCH:OPS01");
        assertThat(Files.readAllLines(dir.resolve("custtxn.txn.rej")))
                .extracting(l -> l.substring(11, 15))
                .containsExactly("E017", "E012");
    }
}
