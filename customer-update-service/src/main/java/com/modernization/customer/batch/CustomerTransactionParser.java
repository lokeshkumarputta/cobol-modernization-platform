package com.modernization.customer.batch;

import com.modernization.customer.domain.model.Address;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Fixed-width reader for the 300-byte TXN-RECORD layout declared in CUSTUPD's FD CUSTTXN-FILE. */
@Component
public class CustomerTransactionParser {

    public static final int RECORD_LENGTH = 300;

    private static final int[] WIDTHS = {10, 1, 9, 30, 30, 60, 15, 40, 40, 30, 2, 10, 3, 1, 11, 8};

    public CustomerTransaction parse(String line) {
        String record = String.format("%-" + RECORD_LENGTH + "s", line);
        String[] f = new String[WIDTHS.length];
        int pos = 0;
        for (int i = 0; i < WIDTHS.length; i++) {
            f[i] = record.substring(pos, pos + WIDTHS[i]);
            pos += WIDTHS[i];
        }
        return new CustomerTransaction(
                f[0],
                f[1],
                isNumeric(f[2]) ? Long.parseLong(f[2]) : -1L,
                f[3].trim(),
                f[4].trim(),
                f[5].trim(),
                f[6].trim(),
                new Address(f[7].trim(), f[8].trim(), f[9].trim(), f[10].trim(), f[11].trim(), f[12].trim()),
                f[13].trim(),
                isNumeric(f[14]) ? new BigDecimal(new BigInteger(f[14]), 2) : null,
                f[15].trim());
    }

    /** COBOL NUMERIC class test: every byte is a digit (spaces are not numeric). */
    public static boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(c -> c >= '0' && c <= '9');
    }
}
