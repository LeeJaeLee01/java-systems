# java-systems

Java microservices learning workspace.

## Payment Platform

Microservices scaffold based on `payment-system.spec.md`:

```
payment-platform/
├── api-gateway/          (8080)
├── auth-service/         (8081)
├── wallet-service/       (8082)
├── payment-service/      (8083)
├── notification-service/ (8084)
├── fraud-service/        (8085)
├── common-lib/
└── docker-compose.yml    (Postgres, Redis, Kafka)
```

See [payment-platform/README.md](payment-platform/README.md) for run instructions.
