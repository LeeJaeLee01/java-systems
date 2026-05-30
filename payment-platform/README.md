# Payment Platform (Microservices)

Gradle multi-module microservices scaffold based on `payment-system.spec.md`.

## Modules

| Module | Port | Responsibility |
|--------|------|----------------|
| `api-gateway` | 8080 | Route requests to services |
| `auth-service` | 8081 | Register/login, JWT |
| `wallet-service` | 8082 | Wallet ledger, pessimistic lock |
| `payment-service` | 8083 | Transfer, idempotency, outbox |
| `notification-service` | 8084 | Kafka consumer, inbox pattern |
| `fraud-service` | 8085 | Kafka consumer, velocity check |
| `common-lib` | - | Shared DTOs/events |

## Infrastructure

```bash
cd payment-platform
docker compose up -d
```

Services:
- PostgreSQL: `localhost:5432` (databases: `auth_db`, `wallet_db`, `payment_db`, `notification_db`, `fraud_db`)
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

## Run services (separate terminals)

```bash
cd payment-platform
source ~/.bashrc

./gradlew :api-gateway:bootRun
./gradlew :auth-service:bootRun
./gradlew :wallet-service:bootRun
./gradlew :payment-service:bootRun
./gradlew :notification-service:bootRun
./gradlew :fraud-service:bootRun
```

## Build all

```bash
./gradlew build
```

## Example flow

1. Register user via gateway:
   `POST http://localhost:8080/api/auth/register`
2. Create wallet:
   `POST http://localhost:8080/api/wallets`
3. Credit wallet:
   `POST http://localhost:8080/api/wallets/{walletId}/credit`
4. Transfer:
   `POST http://localhost:8080/api/payments/transfer`
   Header: `Idempotency-Key: <uuid>`

## Notes

- Each service owns its PostgreSQL database (microservice data isolation).
- `payment-service` writes outbox events and publishes to Kafka topic `payment-events`.
- `notification-service` and `fraud-service` consume the same topic with inbox deduplication.
- **Auth module docs:** [auth-service/docs/AUTH_MODULE.md](auth-service/docs/AUTH_MODULE.md) (Spring Security + Redis rate limiter)
- **Wallet module docs:** [wallet-service/docs/WALLET_MODULE.md](wallet-service/docs/WALLET_MODULE.md) (Deposit/Withdraw + pessimistic lock)
