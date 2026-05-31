# Stripe Integration

Stripe Java SDK integration for PaymentIntent creation and secure webhook handling.

## Environment variables

| Variable | Description |
|----------|-------------|
| `STRIPE_API_KEY` | Secret key (`sk_test_...` or `sk_live_...`) |
| `STRIPE_WEBHOOK_SECRET` | Webhook signing secret (`whsec_...`) |
| `STRIPE_WEBHOOK_TOLERANCE_SECONDS` | Replay tolerance (default `300`) |

Copy `payment-platform/.env.example` and export variables before starting the service:

```bash
export STRIPE_API_KEY=sk_test_...
export STRIPE_WEBHOOK_SECRET=whsec_...
./gradlew :payment-service:bootRun
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/stripe/payment-intents` | Create PaymentIntent, returns `clientSecret` (requires `Idempotency-Key`) |
| `POST` | `/api/stripe/webhook` | Stripe webhook receiver (raw body + signature verification) |

### Create PaymentIntent

```bash
curl -X POST http://localhost:8083/api/stripe/payment-intents \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "660e8400-e29b-41d4-a716-446655440001",
    "amount": 49.99,
    "currency": "usd"
  }'
```

Response includes `clientSecret` for Stripe.js / mobile SDK — card data never touches this backend (PCI-DSS).

## Webhook security

`StripeWebhookVerifier` uses `Webhook.constructEvent(payload, Stripe-Signature, whsec, tolerance)` to:

1. Verify HMAC signature
2. Reject replayed events outside tolerance window
3. Parse into Stripe `Event`

`StripeWebhookService` deduplicates by `event.id` in `processed_stripe_events` (Flyway `V2`).

Handled event types:

- `payment_intent.succeeded`
- `payment_intent.payment_failed`

## Local testing with Stripe CLI

```bash
# Terminal 1 — payment-service on port 8083
./gradlew :payment-service:bootRun

# Terminal 2 — forward webhooks (copy whsec_... to STRIPE_WEBHOOK_SECRET)
stripe listen --forward-to localhost:8083/api/stripe/webhook

# Terminal 3 — trigger test events
stripe trigger payment_intent.succeeded
stripe trigger payment_intent.payment_failed
```

## Configuration

```yaml
stripe:
  api-key: ${STRIPE_API_KEY:}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET:}
  webhook-tolerance-seconds: ${STRIPE_WEBHOOK_TOLERANCE_SECONDS:300}
```

If secrets are missing, PaymentIntent returns `503` and webhook returns `503` with a clear message.
