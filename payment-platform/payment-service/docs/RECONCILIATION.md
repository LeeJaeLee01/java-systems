# Automated Reconciliation (Stripe ↔ Internal Ledger)

## Overview

Daily Spring Batch job downloads Stripe PaymentIntent reports and performs **two-way matching** against internal `payments` ledger records, detecting discrepancies to maintain eventual consistency with the payment partner.

## Flow

```
@Scheduled (02:00 UTC daily)
  → JobLauncher.run(stripeReconciliationJob)
       → ReconciliationTasklet
            → ReconciliationJobService.reconcile(runDate)
                 ├─ StripePaymentIntentReportFetcher (Stripe API, paginated)
                 ├─ PaymentTransactionRepository (internal SUCCESS rows)
                 ├─ ReconciliationMatcher (two-way match)
                 └─ Persist run stats + discrepancies + partner snapshots
```

## Two-Way Matching

| Direction | Discrepancy type | Condition |
|-----------|------------------|-----------|
| Internal → Partner | `MISSING_IN_PARTNER` | Internal SUCCESS with no Stripe match |
| Partner → Internal | `MISSING_IN_INTERNAL` | Stripe `succeeded` with no internal match |
| Both matched | `AMOUNT_MISMATCH` | Amount differs |
| Both matched | `STATUS_MISMATCH` | Internal SUCCESS vs Stripe non-succeeded (or vice versa) |

**Primary match key:** `payments.stripe_payment_intent_id`  
**Fallback:** `user_id` + `amount`

## Schema (Flyway V4)

- `reconciliation_runs` — one row per calendar day (idempotent)
- `stripe_reconciliation_records` — partner snapshot per run
- `reconciliation_discrepancies` — detected mismatches
- `payments.stripe_payment_intent_id` — optional link to Stripe PI

## Configuration

```yaml
spring:
  batch:
    job:
      enabled: false          # only launched by scheduler
    jdbc:
      initialize-schema: always

payment:
  reconciliation:
    enabled: true
    cron: "0 0 2 * * *"       # UTC
    lookback-days: 1          # reconcile yesterday
    default-currency: usd
```

Requires `STRIPE_API_KEY` for partner report fetch. When unset, partner side is empty (internal-only reconciliation).

## Idempotency

A completed run for a given `run_date` is skipped on subsequent launches. Failed runs are retried and overwrite prior snapshots/discrepancies for that date.

## Related

- Stripe integration: `STRIPE.md`
- Internal ledger / outbox: `OUTBOX.md`
