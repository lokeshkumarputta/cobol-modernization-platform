package com.modernization.customer.domain.model;

/**
 * Business rules extracted from CUSTUPD (see docs/business-rules.json).
 * The error code is the reason code CUSTUPD wrote to the CUSTREJ reject file.
 */
public enum BusinessRule {
    BR001("E001", "Customer id must be numeric and greater than zero"),
    BR002("E002", "Invalid transaction code"),
    BR003("E003", "Customer not found"),
    BR004("E004", "Record changed by another user"),
    BR005("E005", "Customer is closed - update rejected"),
    BR006("E006", "Customer suspended - contact update not allowed"),
    BR007("E007", "Invalid email address"),
    BR008("E008", "Invalid phone number"),
    BR011("E011", "Invalid status code"),
    BR012("E012", "Cannot close - outstanding balance"),
    BR013("E013", "Cannot activate - negative balance"),
    BR014("E014", "Credit limit change requires active customer"),
    BR015("E015", "Credit limit exceeds maximum"),
    BR016("E016", "Credit limit below current balance"),
    BR017("E017", "Negative balance - limit increase rejected"),
    BR018("E018", "Risk score too high - limit increase rejected");

    private final String errorCode;
    private final String message;

    BusinessRule(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }

    public String errorCode() {
        return errorCode;
    }

    public String message() {
        return message;
    }
}
