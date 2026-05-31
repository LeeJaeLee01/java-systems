# Notification Service — Kafka + ThreadPool Backpressure

## Overview

`notification-service` consumes **`payment-completed`** payloads from Kafka topic `payment-events`, deduplicates via an inbox table, then dispatches Email (Amazon SES) and SMS (Amazon SNS) on a bounded `ThreadPoolTaskExecutor`.

External API calls never block the Kafka poll loop unless the internal queue is full (backpressure).

## Flow

```
Kafka (payment-events)
  → PaymentCompletedEventListener (@KafkaListener)
  → PaymentNotificationService
       ├─ NotificationInboxService (@Transactional dedup)
       └─ NotificationDispatchService.dispatchAsync()
            → ThreadPoolTaskExecutor (bounded queue)
                 ├─ EmailSender (Logging / SES)
                 └─ SmsSender (Logging / SNS)
```

## Backpressure

| Setting | Default | Role |
|---------|---------|------|
| `notification.executor.core-pool-size` | 4 | Worker threads |
| `notification.executor.max-pool-size` | 8 | Burst capacity |
| `notification.executor.queue-capacity` | 100 | Bounded internal queue |

When the queue is full, `CallerRunsPolicy` runs the task on the **Kafka listener thread**, slowing consumption until workers catch up. This prevents unbounded memory growth and protects SES/SNS rate limits.

## Configuration

```yaml
notification:
  executor:
    core-pool-size: 4
    max-pool-size: 8
    queue-capacity: 100
  ses:
    enabled: false          # true → Amazon SES
    region: us-east-1
    from-email: noreply@example.com
  sms:
    enabled: false          # true → Amazon SNS SMS
    region: us-east-1
    default-country-code: "+1"
```

### AWS credentials

Uses the default AWS SDK credential chain (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, IAM role, etc.) when SES/SNS are enabled.

## Local development

With `ses.enabled=false` and `sms.enabled=false`, `LoggingEmailSender` and `LoggingSmsSender` log notifications instead of calling AWS.

## Idempotency

`inbox_events.message_id` stores `PaymentCompletedEvent.eventId()`. Duplicate or concurrent deliveries are skipped before dispatch.

## Related

- Outbox publisher: `payment-service/docs/OUTBOX.md`
- Kafka topic constant: `KafkaTopics.PAYMENT_EVENTS` (`payment-events`)
