# cobol-modernization-platform

Mainframe batch program **CUSTUPD** (`CUSTOMER_UPDATE.cbl` + `CUSTOMER_RECORD.cpy`) converted to a
Java 21 / Spring Boot 3.4 microservice: REST + PostgreSQL/JPA + Kafka events + Redis cache, secured with OAuth2/JWT,
instrumented with OpenTelemetry/Prometheus/Grafana, packaged for Docker and Kubernetes.

> `legacy/` contains a **representative sample** COBOL program and copybook written for this conversion.
> Replace them with the production sources and re-run the rule extraction to diff against `docs/business-rules.json`.

## Layout

```
legacy/cobol/CUSTOMER_UPDATE.cbl      sample source program (CUSTUPD)
legacy/copybook/CUSTOMER_RECORD.cpy   sample copybook (CUSTOMER-RECORD)
docs/business-rules.json              BR001–BR022 extracted rules -> Java location
docs/cobol-to-java-mapping.md         paragraph / file / field mapping
docs/api/...-openapi.yaml             generated OpenAPI 3.1 spec
customer-update-service/              Spring Boot service (hexagonal: domain / ports / adapters)
deploy/k8s/                           Kustomize manifests (API Deployment, batch Deployment, HPA, PDB, Ingress)
deploy/observability/                 Prometheus scrape config, Grafana datasources + dashboard
deploy/keycloak/                      local-only OAuth2 realm (demo users reader/writer)
docker-compose.yml                    full local stack
```

## API

| COBOL TXN-CODE | Endpoint | Role |
|---|---|---|
| read (`2200-FETCH-CUSTOMER`) | `GET /api/v1/customers/{id}` | `CUSTOMER_READ` or `CUSTOMER_WRITE` |
| `U` contact/address | `PUT /api/v1/customers/{id}/contact` | `CUSTOMER_WRITE` |
| `S` status | `PUT /api/v1/customers/{id}/status` | `CUSTOMER_WRITE` |
| `L` credit limit | `PUT /api/v1/customers/{id}/credit-limit` | `CUSTOMER_WRITE` |

Every mutation carries `expectedVersion` (the COBOL `CUST-VERSION` optimistic lock). Rule violations return
RFC 7807 `application/problem+json` with `ruleId` / `errorCode` (`E001`…): 400 validation, 404 not found,
409 version conflict, 422 business rule. Successful updates publish `CustomerUpdated` (the old `CUSTAUD`
audit record) to Kafka topic `customer.updated.v1` **after commit**, keyed by customer id.

Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Build & test

```bash
cd customer-update-service
mvn verify        # unit tests + Testcontainers ITs (PostgreSQL, Kafka, Redis) + JaCoCo 80% line gate
```

## Run locally

```bash
docker compose up --build
TOKEN=$(curl -s localhost:8180/realms/mainframe/protocol/openid-connect/token \
  -d grant_type=password -d client_id=customer-api -d username=writer -d password=writer | jq -r .access_token)
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/customers/1001
curl -X PUT -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"expectedVersion":0,"newCreditLimit":7500.00}' localhost:8080/api/v1/customers/1001/credit-limit
```

| UI | URL |
|---|---|
| Grafana (dashboard "Customer Update Service") | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| Jaeger traces | http://localhost:16686 |
| Keycloak (admin/admin) | http://localhost:8180 |

The `local` profile seeds six customers (1001–1006) covering active, closed, suspended, negative-balance and
high-risk cases. Drop a fixed-width 300-byte `CUSTTXN` file named `*.txn` into `legacy/data/` to exercise the
batch job (runs every 5 minutes in compose; rejects go to `*.txn.rej`, processed files are renamed `*.done`).

## Batch replacement

`CustomerUpdateBatchJob` replaces the nightly JCL step. Enable with `CUSTOMER_BATCH_ENABLED=true`
(`CUSTOMER_BATCH_CRON`, `CUSTOMER_BATCH_INBOUND_DIR`). Each record runs in its own transaction through the same
domain rules as the REST API, so REST and batch behaviour cannot drift.

## Kubernetes

```bash
kubectl apply -k deploy/k8s     # create the Secret first; see deploy/k8s/secret.example.yaml
```

## Configuration

| Env var | Default |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/customers` / `customer` / `customer` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | `localhost` / `6379` / empty |
| `JWT_JWK_SET_URI` | local Keycloak `mainframe` realm |
| `OTEL_EXPORTER_OTLP_TRACES_ENDPOINT` | `http://localhost:4318/v1/traces` |
| `CUSTOMER_EVENTS_TOPIC` | `customer.updated.v1` |
