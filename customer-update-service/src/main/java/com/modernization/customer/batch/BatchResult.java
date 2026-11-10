package com.modernization.customer.batch;

/** The control totals CUSTUPD DISPLAYed in 9000-TERMINATE. */
public record BatchResult(int read, int updated, int rejected) {
}
