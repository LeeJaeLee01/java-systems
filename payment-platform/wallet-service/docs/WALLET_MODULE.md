# Wallet Module — Deposit / Withdraw Implementation

**Service:** `wallet-service` (port `8082`)  
**Spec reference:** `payment-system.spec.md` §2.2 Database Architecture & Balance Data Consistency

---

## 1. Overview

Wallet module manages user balances using a **ledger pattern**:
- `wallets` stores current balance
- `wallet_transactions` stores immutable audit trail (`balance_before`, `balance_after`)

All balance mutations use a **two-layer locking strategy**:
1. **Redisson `RLock`** (Redis) — cluster-wide serialization per wallet, reduces DB connection contention
2. **`PESSIMISTIC_WRITE`** (PostgreSQL) — authoritative balance + ledger, guarantees no negative balance

---

## 2. Redisson Distributed Lock (`RLock`)

**Purpose:** Handle high concurrency (e.g. 10,000 req/s hitting the same wallet) by queueing at Redis before touching PostgreSQL.

```java
RLock lock = redissonClient.getLock("lock:wallet:" + walletId);
lock.tryLock(waitTimeMs, leaseTimeMs, TimeUnit.MILLISECONDS);
```

| Config | Default | Description |
|--------|---------|-------------|
| `wallet.lock.key-prefix` | `lock:wallet:` | Redis lock key prefix |
| `wallet.lock.wait-time-ms` | `3000` | Max wait to acquire lock |
| `wallet.lock.lease-time-ms` | `10000` | Auto-release if worker crashes |

**Flow (deposit/withdraw/debit/credit):**
```
Request → RLock acquire (Redis)
        → ensure balance cache loaded
        → [debit] Lua atomic pre-check on Redis cache
        → @Transactional DB: SELECT FOR UPDATE + update + ledger
        → sync Redis cache from DB balance
        → RLock release
```

If lock not acquired → `409 Wallet is busy, please retry`.

---

## 3. Redis Balance Cache + Lua Script

Fast-path debit check (spec §2.3) using atomic Lua on Redis **inside RLock**:

```lua
-- Returns new balance or -1 if insufficient
local current_balance = tonumber(redis.call('get', wallet_key) or "0")
if current_balance >= tx_amount then
    redis.call('set', wallet_key, tostring(current_balance - tx_amount))
    return new_balance
else
    return -1
end
```

| Key | Example |
|-----|---------|
| Distributed lock | `lock:wallet:{uuid}` |
| Balance cache | `wallet:balance:{uuid}` |

- **Debit/withdraw:** Lua pre-check fails fast → `400 Insufficient balance` without DB round-trip
- **DB remains source of truth:** `SELECT FOR UPDATE` + balance check after Lua
- **On DB failure:** cache rolled back from DB via `rollbackCache()`

Config: `wallet.cache.enabled=true`, `wallet.cache.balance-key-prefix=wallet:balance:`

---

## 4. Pessimistic Locking (PostgreSQL)

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
Optional<Wallet> findByIdForUpdate(@Param("walletId") UUID walletId);
```

**Flow:**
1. Begin transaction
2. Lock wallet row with `findByIdForUpdate`
3. Validate wallet status + balance (for withdraw)
4. Update balance + insert ledger entry
5. Commit → release lock

Concurrent requests on the same wallet are serialized at DB level → no lost updates.

---

## 5. API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/wallets` | POST | Create wallet |
| `/api/wallets/{walletId}` | GET | Get wallet by ID |
| `/api/wallets/users/{userId}` | GET | Get wallet by user |
| `/api/wallets/{walletId}/deposit` | POST | **Nạp tiền** |
| `/api/wallets/{walletId}/withdraw` | POST | **Rút tiền** |
| `/api/wallets/{walletId}/transactions` | GET | Lịch sử ledger |
| `/api/wallets/{walletId}/credit` | POST | Internal credit (payment-service) |
| `/api/wallets/{walletId}/debit` | POST | Internal debit (payment-service) |

### Deposit request

```json
POST /api/wallets/{walletId}/deposit
{
  "referenceId": "550e8400-e29b-41d4-a716-446655440000",
  "amount": 100.00,
  "description": "Bank transfer top-up"
}
```

### Withdraw request

```json
POST /api/wallets/{walletId}/withdraw
{
  "referenceId": "660e8400-e29b-41d4-a716-446655440001",
  "amount": 50.00,
  "description": "Cash withdrawal"
}
```

---

## 6. Business Rules

| Rule | Behavior |
|------|----------|
| Wallet must be `ACTIVE` | `403` if `FROZEN` or `CLOSED` |
| Withdraw amount ≤ balance | `400 Insufficient balance` |
| Duplicate `referenceId` per type | Idempotent — returns current wallet state |
| Ledger immutability | Every change writes `wallet_transactions` row |
| Unique index | `(wallet_id, reference_id, transaction_type)` |

---

## 7. Transaction Types

| Type | Use case |
|------|----------|
| `DEPOSIT` | User top-up via public API |
| `WITHDRAW` | User withdrawal via public API |
| `CREDIT` | Internal credit from payment flow |
| `DEBIT` | Internal debit from payment flow |

---

## 8. Example Flow

```bash
# 1. Create wallet
curl -X POST http://localhost:8082/api/wallets \
  -H "Content-Type: application/json" \
  -d '{"userId":"USER_UUID"}'

# 2. Deposit 100
curl -X POST http://localhost:8082/api/wallets/WALLET_UUID/deposit \
  -H "Content-Type: application/json" \
  -d '{"referenceId":"REF_UUID_1","amount":100.00}'

# 3. Withdraw 30
curl -X POST http://localhost:8082/api/wallets/WALLET_UUID/withdraw \
  -H "Content-Type: application/json" \
  -d '{"referenceId":"REF_UUID_2","amount":30.00}'

# 4. View ledger
curl http://localhost:8082/api/wallets/WALLET_UUID/transactions
```

Via API Gateway (`8080`): replace host/port with `http://localhost:8080`.

---

## 9. Database Migrations

- `V1__init_wallet_schema.sql` — wallets + wallet_transactions
- `V2__add_wallet_tx_idempotency_index.sql` — unique index for idempotent operations

---

## 10. Tests

```bash
./gradlew :wallet-service:test
```

- `WalletBalanceServiceTest` — DB logic, pessimistic lock, cache sync
- `WalletDistributedLockServiceTest` — RLock acquire/release

---

## 11. File Map

```
wallet-service/src/main/java/com/paymentsystem/wallet/
├── controller/WalletController.java
├── service/
│   ├── WalletService.java                 # RLock orchestration
│   ├── WalletBalanceService.java          # DB + pessimistic lock
│   ├── WalletDistributedLockService.java  # Redisson RLock
│   └── RedisWalletBalanceCache.java       # Lua atomic debit
├── config/
│   ├── WalletLockProperties.java
│   └── WalletCacheProperties.java
├── repository/
│   ├── WalletRepository.java              # @Lock PESSIMISTIC_WRITE
│   └── WalletTransactionRepository.java
```
