package com.modernization.customer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** JPA mapping of DB2 table CUSTOMER_MASTER (host variables from copybook CUSTREC). */
@Entity
@Table(name = "customer_master")
@Getter
@Setter
@NoArgsConstructor
public class CustomerEntity {

    @Id
    @Column(name = "cust_id")
    private Long id;

    @Column(name = "first_name", length = 30, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 30, nullable = false)
    private String lastName;

    @Column(name = "dob")
    private LocalDate dateOfBirth;

    @Column(name = "email", length = 60)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "addr_line1", length = 40)
    private String addressLine1;

    @Column(name = "addr_line2", length = 40)
    private String addressLine2;

    @Column(name = "city", length = 30)
    private String city;

    @Column(name = "state", length = 2)
    private String state;

    @Column(name = "zip", length = 10)
    private String zip;

    @Column(name = "country", length = 3)
    private String country;

    @Column(name = "status", length = 1, nullable = false)
    private String status;

    @Column(name = "cust_type", length = 1, nullable = false)
    private String type;

    @Column(name = "credit_limit", precision = 11, scale = 2, nullable = false)
    private BigDecimal creditLimit;

    @Column(name = "account_bal", precision = 11, scale = 2, nullable = false)
    private BigDecimal accountBalance;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Column(name = "last_upd_ts")
    private Instant lastUpdatedAt;

    @Column(name = "last_upd_user", length = 64)
    private String lastUpdatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
