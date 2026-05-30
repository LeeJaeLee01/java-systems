# Auth Module — Implementation Notes

**Service:** `auth-service` (port `8081`)  
**Spec reference:** `payment-system.spec.md` §2.1 Authentication & Security  
**Status:** Phase 1 implemented (MVP + rate limiting)

---

## 1. Overview

Auth module handles user registration, login, password hashing, JWT access tokens, and **IP-based rate limiting** for auth endpoints using **Redis** (Spring Data Redis).

```
Client → API Gateway (8080) → auth-service (8081)
                                    ├── PostgreSQL (auth_db)
                                    └── Redis (rate limit keys)
```

---

## 2. Implemented Features

### 2.1 Spring Security Filter Chain

| Feature | Status | Implementation |
|---------|--------|----------------|
| Stateless API (no session) | Done | `SessionCreationPolicy.STATELESS` |
| CSRF disabled | Done | Appropriate for JWT/stateless REST |
| Public auth endpoints | Done | `/api/auth/**`, `/actuator/**` |
| CORS policy | Done | Configurable via `auth.cors.*` / env `AUTH_CORS_ALLOWED_ORIGINS` |
| Security headers | Done | `X-Content-Type-Options`, `X-Frame-Options: DENY`, CSP |
| Rate limit filter | Done | `AuthRateLimitFilter` before auth processing |

**File:** `config/SecurityConfig.java`

### 2.2 Registration & Login

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Create user, return JWT |
| `/api/auth/login` | POST | Validate credentials, return JWT |
| `/api/auth/health` | GET | Service health check |

**Files:**
- `controller/AuthController.java`
- `service/AuthService.java`
- `dto/RegisterRequest.java`, `LoginRequest.java`, `AuthResponse.java`

### 2.3 Password Hashing

| Spec target | Current implementation |
|-------------|------------------------|
| Argon2id | **BCrypt** (MVP; Argon2id planned) |

**File:** `SecurityConfig.passwordEncoder()` → `BCryptPasswordEncoder`

### 2.4 JWT Access Token

| Feature | Status |
|---------|--------|
| Access token generation | Done |
| HS256 signing (dev) | Done |
| RS256 asymmetric | Not yet |
| Refresh token + rotation | Not yet |

**Config:** `application.yml` → `auth.jwt.*`  
**File:** `security/JwtService.java`

---

## 3. Redis Rate Limiter (Implemented)

Aligned with spec: **5 requests / minute / IP** for login & register, **HTTP 429**, **15-minute IP block** when limit exceeded.

### 3.1 Algorithm (Fixed Window + Block)

1. Resolve client IP (`X-Forwarded-For` or `remoteAddr`)
2. Check Redis key `auth:block:{ip}` → if exists → **429 blocked**
3. Increment `auth:rate:{ip}` with **1 minute TTL**
4. If count **> 5** → set `auth:block:{ip}` for **15 minutes** → **429**

### 3.2 Components

| Class | Role |
|-------|------|
| `AuthRateLimitFilter` | Servlet filter on `POST /api/auth/login`, `/register` |
| `AuthRateLimiterService` | Redis counter + block logic |
| `ClientIpResolver` | Extract IP (supports Gateway `X-Forwarded-For`) |
| `AuthRateLimitProperties` | Configurable limits |
| `RateLimitExceededException` | Domain exception |
| `AuthExceptionHandler` | JSON error responses |

### 3.3 Configuration

```yaml
auth:
  rate-limit:
    max-requests-per-minute: 5
    block-duration-minutes: 15
    window-key-prefix: "auth:rate:"
    block-key-prefix: "auth:block:"
  cors:
    allowed-origins: ${AUTH_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:8080}
    allowed-methods: GET,POST,PUT,DELETE,OPTIONS
    allowed-headers: "*"
    allow-credentials: true
```

**Environment override example:**

```bash
export AUTH_CORS_ALLOWED_ORIGINS="http://localhost:3000,https://app.example.com"
./gradlew :auth-service:bootRun
```

### 3.4 Response on Rate Limit

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60   # or 900 when IP is blocked
Content-Type: application/json

{
  "success": false,
  "message": "Too many auth requests from this IP",
  "data": null
}
```

---

## 4. Database Schema

**Database:** `auth_db` (PostgreSQL)

```sql
users (
  id UUID PK,
  email UNIQUE,
  password_hash,
  full_name,
  status,
  created_at,
  updated_at
)
```

**Migration:** `src/main/resources/db/migration/V1__init_auth_schema.sql`

---

## 5. How to Run & Test

### Prerequisites

```bash
cd payment-platform
docker compose up -d   # Postgres + Redis
./gradlew :auth-service:bootRun
```

### Manual test (rate limit)

```bash
# Send 6 login attempts quickly from same machine
for i in {1..6}; do
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost:8081/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@example.com","password":"wrong"}'
done
# Expected: first 5 → 401, 6th → 429
```

### Unit tests

```bash
./gradlew :auth-service:test
```

Includes `AuthRateLimiterServiceTest` (Redis logic mocked).

---

## 6. Not Yet Implemented (Future)

| Spec item | Notes |
|-----------|-------|
| Argon2id password hashing | Replace BCrypt |
| JWT RS256 + refresh token rotation | Dual-token pattern |
| Rate limit at API Gateway (Bucket4j) | Currently in auth-service |
| Change password endpoint | Spec includes in rate limit group |
| JWT validation filter for protected routes | Other services / gateway |
| MFA / account lock policies | Fraud integration |

---

## 7. File Map

```
auth-service/src/main/java/com/paymentsystem/auth/
├── AuthServiceApplication.java
├── config/
│   ├── SecurityConfig.java
│   ├── AuthRateLimitProperties.java
│   └── AuthCorsProperties.java
├── controller/
│   └── AuthController.java
├── service/
│   ├── AuthService.java
│   └── AuthRateLimiterService.java
├── security/
│   ├── JwtService.java
│   ├── ClientIpResolver.java
│   └── AuthRateLimitFilter.java
├── exception/
│   ├── RateLimitExceededException.java
│   └── AuthExceptionHandler.java
├── domain/User.java
├── repository/UserRepository.java
└── dto/
```
