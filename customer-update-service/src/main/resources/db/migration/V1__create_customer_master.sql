-- DB2 CUSTOMER_MASTER migrated to PostgreSQL. Column sizes follow copybook CUSTREC.
CREATE TABLE customer_master (
    cust_id        BIGINT        PRIMARY KEY CHECK (cust_id > 0),
    first_name     VARCHAR(30)   NOT NULL,
    last_name      VARCHAR(30)   NOT NULL,
    dob            DATE,
    email          VARCHAR(60),
    phone          VARCHAR(15),
    addr_line1     VARCHAR(40),
    addr_line2     VARCHAR(40),
    city           VARCHAR(30),
    state          VARCHAR(2),
    zip            VARCHAR(10),
    country        VARCHAR(3),
    status         VARCHAR(1)    NOT NULL CHECK (status IN ('A', 'I', 'S', 'C')),
    cust_type      VARCHAR(1)    NOT NULL CHECK (cust_type IN ('I', 'B')),
    credit_limit   NUMERIC(11,2) NOT NULL DEFAULT 0,
    account_bal    NUMERIC(11,2) NOT NULL DEFAULT 0,
    risk_score     INTEGER       NOT NULL DEFAULT 0 CHECK (risk_score BETWEEN 0 AND 999),
    last_upd_ts    TIMESTAMP WITH TIME ZONE,
    last_upd_user  VARCHAR(64),
    version        BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX idx_customer_master_status ON customer_master (status);
