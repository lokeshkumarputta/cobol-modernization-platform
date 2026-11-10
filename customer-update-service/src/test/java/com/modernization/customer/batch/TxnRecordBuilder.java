package com.modernization.customer.batch;

/** Builds 300-byte TXN-RECORD lines for tests. */
public final class TxnRecordBuilder {

    private final String custId;
    private final String code;
    private final String version;
    private String first = "";
    private String last = "";
    private String email = "";
    private String phone = "";
    private String l1 = "";
    private String l2 = "";
    private String city = "";
    private String state = "";
    private String zip = "";
    private String country = "";
    private String newStatus = "";
    private String newLimit = "";
    private String user = "BATCH01";

    private TxnRecordBuilder(String custId, String code, long version) {
        this.custId = custId;
        this.code = code;
        this.version = String.format("%09d", version);
    }

    public static TxnRecordBuilder txn(String custId, String code, long version) {
        return new TxnRecordBuilder(custId, code, version);
    }

    public TxnRecordBuilder names(String first, String last) {
        this.first = first;
        this.last = last;
        return this;
    }

    public TxnRecordBuilder email(String email) {
        this.email = email;
        return this;
    }

    public TxnRecordBuilder phone(String phone) {
        this.phone = phone;
        return this;
    }

    public TxnRecordBuilder address(String l1, String l2, String city, String state, String zip, String country) {
        this.l1 = l1;
        this.l2 = l2;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.country = country;
        return this;
    }

    public TxnRecordBuilder newStatus(String newStatus) {
        this.newStatus = newStatus;
        return this;
    }

    public TxnRecordBuilder newLimit(String elevenDigits) {
        this.newLimit = elevenDigits;
        return this;
    }

    public TxnRecordBuilder user(String user) {
        this.user = user;
        return this;
    }

    public String build() {
        return pad(custId, 10) + pad(code, 1) + version + pad(first, 30) + pad(last, 30) + pad(email, 60)
                + pad(phone, 15) + pad(l1, 40) + pad(l2, 40) + pad(city, 30) + pad(state, 2) + pad(zip, 10)
                + pad(country, 3) + pad(newStatus, 1) + pad(newLimit, 11) + pad(user, 8);
    }

    private static String pad(String value, int width) {
        return String.format("%-" + width + "." + width + "s", value);
    }
}
