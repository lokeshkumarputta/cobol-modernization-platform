# CUSTUPD → customer-update-service mapping

| COBOL artefact | Java / platform artefact |
|---|---|
| `CUSTOMER_RECORD.cpy` (`CUSTOMER-RECORD`) | `domain.model.Customer` (aggregate), `entity.CustomerEntity` (JPA), `dto.CustomerDto` |
| DB2 table `CUSTOMER_MASTER` | PostgreSQL `customer_master` (`V1__create_customer_master.sql`) |
| `CUST-VERSION` + `UPDATE ... WHERE VERSION = :v` | JPA `@Version` → `BR004` / HTTP 409 |
| `PIC S9(09)V99 COMP-3` | `BigDecimal` / `NUMERIC(11,2)` |
| 88-levels `CUST-STATUS`, `CUST-TYPE` | `CustomerStatus`, `CustomerType` enums (legacy code preserved) |
| `TXN-CODE` `U` / `S` / `L` | `PUT /contact`, `PUT /status`, `PUT /credit-limit` + `TransactionCode` enum |
| `2100-VALIDATE-TXN` | Bean Validation on request DTOs + `CustomerTransactionParser` |
| `2200-FETCH-CUSTOMER` | `CustomerPersistencePort.findById` (+ Redis `@Cacheable`) |
| `2300-APPLY-BUSINESS-RULES` (`2310/2320/2330`) | `Customer.updateContact / changeStatus / changeCreditLimit` |
| `2400-UPDATE-CUSTOMER` | `CustomerUpdateService` `@Transactional` save (+ `@CachePut`) |
| `2500-WRITE-AUDIT` → `CUSTAUD` | `CustomerUpdatedEvent` → Kafka `customer.updated.v1` (after commit) |
| `2600-WRITE-REJECT` → `CUSTREJ` | RFC 7807 `ProblemDetail` (`ruleId`, `errorCode`) for REST; `.rej` file for batch |
| `7000-COMMIT` every 100 records | One transaction per record (no partial-batch rollback) |
| `9000-TERMINATE` control totals | `BatchResult(read, updated, rejected)` + log line |
| JCL nightly step, `CUSTTXN` DD | `CustomerUpdateBatchJob` (`@Scheduled`, `customer.batch.*`), `deploy/k8s/batch-deployment.yaml` |
| `LAST-UPD-USER` (SY user / batch id) | JWT `preferred_username` / `BATCH:<TXN-USER>` |

All rules BR001–BR022 with conditions, error codes and Java locations: [`business-rules.json`](business-rules.json).
