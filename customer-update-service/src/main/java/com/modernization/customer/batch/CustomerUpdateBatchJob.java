package com.modernization.customer.batch;

import com.modernization.customer.config.CustomerBatchProperties;
import com.modernization.customer.domain.exception.BusinessRuleViolationException;
import com.modernization.customer.domain.model.BusinessRule;
import com.modernization.customer.domain.model.ContactUpdate;
import com.modernization.customer.domain.model.TransactionCode;
import com.modernization.customer.service.CustomerUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scheduled replacement for the nightly CUSTUPD JCL step. Every transaction is its own unit of work;
 * rejects are written to {@code <file>.rej} (former CUSTREJ) and do not stop the run. Unexpected
 * failures propagate and stop the run, like the legacy U999 ABEND.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "customer.batch", name = "enabled", havingValue = "true")
public class CustomerUpdateBatchJob {

    static final String INBOUND_SUFFIX = ".txn";
    static final String DONE_SUFFIX = ".done";
    static final String REJECT_SUFFIX = ".rej";
    static final String BATCH_USER_PREFIX = "BATCH:";

    private final CustomerUpdateService service;
    private final CustomerTransactionParser parser;
    private final CustomerBatchProperties properties;

    @Scheduled(cron = "${customer.batch.cron:0 0 2 * * *}")
    public void run() {
        try (Stream<Path> files = Files.list(properties.inboundDir())) {
            files.filter(p -> p.getFileName().toString().endsWith(INBOUND_SUFFIX))
                    .sorted()
                    .forEach(this::processFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read inbound directory " + properties.inboundDir(), e);
        }
    }

    public BatchResult processFile(Path file) {
        List<String> rejects = new ArrayList<>();
        int read = 0;
        int updated = 0;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                read++;
                CustomerTransaction txn = parser.parse(line);
                try {
                    apply(txn);
                    updated++;
                } catch (BusinessRuleViolationException e) {
                    rejects.add(rejectRecord(txn, e.getRule()));
                    log.warn("CUSTUPD rejected {} {}", txn.customerId(), e.getRule().errorCode());
                }
            }
            Files.write(file.resolveSibling(file.getFileName() + REJECT_SUFFIX), rejects, StandardCharsets.UTF_8);
            Files.move(file, file.resolveSibling(file.getFileName() + DONE_SUFFIX));
        } catch (IOException e) {
            throw new UncheckedIOException("Batch file processing failed for " + file, e);
        }
        BatchResult result = new BatchResult(read, updated, rejects.size());
        log.info("CUSTUPD {} - records read: {}, updated: {}, rejected: {}",
                file.getFileName(), result.read(), result.updated(), result.rejected());
        return result;
    }

    /** 2100-VALIDATE-TXN (BR001, BR002) then dispatch to the transactional service. */
    void apply(CustomerTransaction txn) {
        if (!CustomerTransactionParser.isNumeric(txn.customerId())) {
            throw new BusinessRuleViolationException(BusinessRule.BR001);
        }
        long customerId = Long.parseLong(txn.customerId());
        TransactionCode code = TransactionCode.fromCode(txn.transactionCode());
        String user = BATCH_USER_PREFIX + txn.userId();
        switch (code) {
            case CONTACT_UPDATE -> service.updateContact(customerId, txn.expectedVersion(),
                    new ContactUpdate(txn.firstName(), txn.lastName(),
                            txn.email(), txn.phone(), txn.address()), user);
            case STATUS_CHANGE -> service.changeStatus(customerId, txn.expectedVersion(), txn.newStatus(), user);
            case CREDIT_LIMIT_CHANGE -> {
                if (txn.newCreditLimit() == null) {
                    throw new BusinessRuleViolationException(BusinessRule.BR015);
                }
                service.changeCreditLimit(customerId, txn.expectedVersion(), txn.newCreditLimit(), user);
            }
        }
    }

    /** REJ-RECORD layout: cust-id(10) txn-code(1) reason-code(4) reason-text(60). */
    private static String rejectRecord(CustomerTransaction txn, BusinessRule rule) {
        return String.format("%-10.10s%-1.1s%-4.4s%-60.60s",
                txn.customerId(), txn.transactionCode(), rule.errorCode(), rule.message().toUpperCase());
    }
}
