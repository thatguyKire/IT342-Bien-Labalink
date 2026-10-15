# Technical Requirements Document (TRD)

| | |
|---|---|
| **System** | LabaLink 1.0 |
| **Date** | 2026-10-15 |
| **Related** | [PRD](PRD.md) · [SRS](SRS.md) · [SDD](SDD.md) · [Backend Schema](Backend-Schema.md) |

---

## 1. Purpose

The TRD turns the product and software requirements into concrete
technology choices, the API contract, environments and operating limits.
It is written for developers and operators.

## 2. Technology stack

| Layer | Technology | Version |
|---|---|---|
| API runtime | Java (Temurin) | 17 (LTS) |
| API framework | Spring Boot (Web, Security, Data JPA, Validation, WebSocket, Mail, OAuth2 Client, Actuator) | 3.5.11 |
| Auth tokens | JJWT | 0.12.6 |
| Payments | stripe-java | 24.3.0 |
| Database | PostgreSQL | 14+ |
| Migrations | Flyway (core + postgresql) | managed by Spring Boot |
| Build (API) | Maven Wrapper | 3.9 |
| Web | React · TypeScript · Vite · Tailwind CSS | 19 · 5.9 · 7 · 4 |
| Web libraries | axios · react-router-dom 7 · sockjs-client + stompjs · qrcode.react 4.2.0 | — |
| Android | Kotlin · AGP · Gradle | 2.2 · 9.1 · 9.3.1 |
| Android libraries | Retrofit 2.11 · OkHttp 4.12 · Coroutines · ZXing embedded 4.3 · Play Services Auth 21 | — |
| Android SDK | minSdk 24 · target/compile 35 | — |
| Testing | JUnit 5, Spring Boot Test, MockMvc, zonky embedded-postgres 2.1 | — |

## 3. Environments

| Env | API | Web | DB | Stripe | Notes |
|---|---|---|---|---|---|
| Local | `http://localhost:8080` | `http://localhost:5173` | Local PostgreSQL | Test keys + `stripe listen` | Android emulator uses `http://10.0.2.2:8080/` |
| Test (CI) | MockMvc / random port | — | Embedded PostgreSQL | `FakePaymentGateway` | `./mvnw test` |
| Production | `https://api.<domain>` | `https://app.<domain>` | Managed PostgreSQL (TLS) | Live keys + webhook | Single API instance |

## 4. Configuration

All configuration is in `backend/src/main/resources/application.yml`, with
values from environment variables (or `backend/.env`). The full list is in
[`backend/.env.example`](../backend/.env.example).

| Property | Env var | Default | Required |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | `jdbc:postgresql://localhost:5432/labalink` | yes |
| `spring.datasource.username/password` | `DB_USERNAME` / `DB_PASSWORD` | `postgres` / empty | yes |
| `app.jwt.secret` | `JWT_SECRET` | — | **yes (≥ 32 bytes)** |
| `app.jwt.expiration-minutes` | `JWT_EXPIRATION_MINUTES` | 60 | no |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | prod |
| `app.frontend-url` | `FRONTEND_URL` | `http://localhost:5173` | prod |
| `spring.security.oauth2.client.registration.google.*` | `GOOGLE_CLIENT_ID/SECRET` | `not-configured` | for Google web |
| `app.google.client-ids` | `GOOGLE_CLIENT_IDS` | = `GOOGLE_CLIENT_ID` | for Google Android |
| `app.stripe.secret-key` | `STRIPE_SECRET_KEY` | empty (payments off) | for payments |
| `app.stripe.webhook-secret` | `STRIPE_WEBHOOK_SECRET` | empty (webhook off) | prod |
| `app.stripe.success-url/cancel-url` | `STRIPE_SUCCESS_URL/CANCEL_URL` | `${FRONTEND_URL}/payment/...` | no |
| `spring.mail.*` | `MAIL_HOST/PORT/USERNAME/PASSWORD` | Gmail SMTP | for email |
| `app.bootstrap.*` | `BOOTSTRAP_ATTENDANT_EMAIL/PASSWORD/USERNAME` | empty | first run |
| `server.forward-headers-strategy` | `FORWARD_HEADERS_STRATEGY` | `none` | behind a proxy |
| `server.servlet.session.cookie.secure` | `SESSION_COOKIE_SECURE` | `false` | prod: `true` |

Fixed limits (in `application.yml`): top-up ₱50–₱10,000, wallet cap
₱50,000, session 15–240 minutes (default 60), auth rate 20/min/IP, API rate
300/min/IP, lockout after 5 failures for 15 minutes.

**Web:** `VITE_API_URL` (build time).
**Android:** `labalink.apiBaseUrl`, `labalink.apiBaseUrlRelease`,
`labalink.googleWebClientId` in `mobiledev/local.properties` or as `-P`
flags.

## 5. API contract

Base path: `/api`. Content type: `application/json`. Authentication:
`Authorization: Bearer <JWT>`. Timestamps are ISO-8601 local date-times
(`2026-10-15T14:30:00`). Money values are JSON numbers with 2 decimals.

### 5.0 Conventions

**Error body** (every non-2xx response):
```json
{ "success": false, "message": "Human-readable, safe to display" }
```

| Status | Meaning |
|---|---|
| 400 | Validation failed / malformed request / wrong QR |
| 401 | Missing, invalid, expired or revoked token; bad credentials |
| 402 | Insufficient wallet balance |
| 403 | Authenticated but not allowed (role); disabled account |
| 404 | Not found **or not yours** |
| 409 | Conflict (machine busy, invalid state change, duplicate email) |
| 429 | Rate limited / account locked (`Retry-After` header on rate limit) |
| 503 | Payments or Google sign-in not configured / provider unavailable |

Access legend: 🌐 public · 🔑 any signed-in user · 👤 customer · 🧑‍💼 attendant.

### 5.1 Auth — `/api/auth`

| Method | Path | Access | Body | Response |
|---|---|---|---|---|
| POST | `/register` | 🌐 | `{username, email, password}` | 201 `{success, message}` |
| POST | `/login` | 🌐 | `{email, password}` | 200 `LoginResponse` |
| POST | `/google` | 🌐 | `{idToken}` (Google ID token) | 200 `LoginResponse` |
| POST | `/oauth2/exchange` | 🌐 | `{code}` | 200 `LoginResponse` |
| POST | `/logout` | 🔑 | — | 200; all of the caller's tokens revoked |
| GET | `/oauth2/authorization/google` *(no /api prefix)* | 🌐 | — | 302 to Google (web login) |

```json
// LoginResponse
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "role": "CUSTOMER",
  "email": "ana@example.com",
  "username": "Ana",
  "userId": 42
}
```

Validation: username 3–50 chars `[letters digits space . _ -]`; email ≤ 254
chars; password 8–72 chars, at least 1 letter and 1 digit.

### 5.2 Users — `/api/users`

| Method | Path | Access | Response |
|---|---|---|---|
| GET | `/me` | 🔑 | `UserResponse` |
| GET | `/attendants` | 🧑‍💼 | `UserResponse[]` |
| POST | `/attendants` | 🧑‍💼 | Body as register → 201 `UserResponse` (role `ATTENDANT`) |

```json
// UserResponse
{ "id": 42, "username": "Ana", "email": "ana@example.com", "role": "CUSTOMER",
  "authProvider": "LOCAL", "walletBalance": 155.00, "createdAt": "2026-10-15T09:00:00" }
```

### 5.3 Machines — `/api/machines`

| Method | Path | Access | Body | Response |
|---|---|---|---|---|
| GET | `/` | 🔑 | — | `MachineResponse[]` (`qrToken` only for attendants) |
| GET | `/{id}` | 🔑 | — | `MachineResponse` |
| POST | `/` | 🧑‍💼 | `MachineRequest` | 201 `MachineResponse` (with `qrToken`) |
| PUT | `/{id}` | 🧑‍💼 | `MachineRequest` | `MachineResponse` |
| POST | `/{id}/regenerate-qr` | 🧑‍💼 | — | `MachineResponse` with a new `qrToken` |
| DELETE | `/{id}` | 🧑‍💼 | — | 200, or 409 if it has bookings |

```json
// MachineRequest
{ "machineName": "Washer 01", "machineType": "WASHER", "hourlyRate": 30.00, "status": "AVAILABLE" }
// MachineResponse
{ "id": 1, "machineName": "Washer 01", "machineType": "WASHER", "status": "AVAILABLE",
  "qrToken": "LL-3q2V0yX4c1mJk9Qe7TfWbR8p", "hourlyRate": 30.00, "createdAt": "..." }
```

### 5.4 Bookings — `/api/bookings`

| Method | Path | Access | Body / query | Response |
|---|---|---|---|---|
| GET | `/` | 🧑‍💼 | `?status=PENDING\|ACTIVE\|COMPLETED\|CANCELLED` | `BookingResponse[]` |
| GET | `/me` | 🔑 | — | Caller's `BookingResponse[]` |
| GET | `/stats` | 🧑‍💼 | — | `{totalBookings, activeBookings, pendingBookings, totalRevenue}` |
| GET | `/{id}` | owner or 🧑‍💼 | — | `BookingResponse` (404 otherwise) |
| POST | `/` | 👤 | `{machineId, qrToken, durationMinutes?}` | 201 `BookingResponse`; 400 wrong QR; 402 funds; 409 busy |
| PATCH | `/{id}/status` | 🧑‍💼 | `{status}` | `BookingResponse`; 409 invalid transition |
| POST | `/{id}/cancel` | owner | — | `BookingResponse` (PENDING only; refunded) |

```json
// BookingResponse
{ "id": 7, "userId": 42, "userEmail": "ana@example.com", "username": "Ana",
  "machineId": 1, "machineName": "Washer 01", "machineType": "WASHER",
  "status": "PENDING", "startTime": "2026-10-15T14:30:00", "endTime": "2026-10-15T15:30:00",
  "durationMinutes": 60, "totalPrice": 30.00, "createdAt": "2026-10-15T14:30:00" }
```

### 5.5 Payments — `/api/payments`

| Method | Path | Access | Body | Response |
|---|---|---|---|---|
| POST | `/checkout` | 👤 | `{amount}` (50–10000) | 201 `PaymentResponse` with `checkoutUrl` |
| POST | `/{id}/sync` | owner or 🧑‍💼 | — | `PaymentResponse` (credits if Stripe says paid) |
| GET | `/me` | 🔑 | — | Caller's `PaymentResponse[]` |
| GET | `/wallet` | 🔑 | — | `{walletBalance}` |
| GET | `/` | 🧑‍💼 | — | All `PaymentResponse[]` |
| POST | `/webhook` | Stripe (signature) | Raw Stripe event | 200; 400 bad signature |

```json
// PaymentResponse
{ "id": 12, "userEmail": "ana@example.com", "username": "Ana", "amount": 150.00,
  "currency": "php", "providerReference": "LL-12-AB12CD34", "status": "PENDING",
  "createdAt": "...", "checkoutUrl": "https://checkout.stripe.com/c/pay/cs_test_..." }
```

Handled Stripe events: `checkout.session.completed`,
`checkout.session.async_payment_succeeded`,
`checkout.session.async_payment_failed`, `checkout.session.expired`.

### 5.6 Health

`GET /actuator/health` → `{"status":"UP"}` (🌐, no details).

### 5.7 WebSocket (STOMP)

- Endpoint: `/ws` (SockJS). The CONNECT frame must include the
  `Authorization: Bearer <JWT>` header.
- Message envelope:
  `{"type": "...", "message": "...", "data": {...}, "timestamp": "..."}`

| Destination | Who may subscribe | `type` values | `data` |
|---|---|---|---|
| `/topic/machines` | 🔑 | `MACHINE_UPDATED` | `MachineResponse` without `qrToken`, `{id,status}` or `{id,deleted:true}` |
| `/topic/bookings` | 🧑‍💼 | `BOOKING_CREATED`, `BOOKING_UPDATED` | `BookingResponse` |
| `/user/queue/bookings` | owner | `BOOKING_CREATED`, `BOOKING_UPDATED` | `BookingResponse` |
| `/user/queue/wallet` | owner | `WALLET_UPDATED` | `{newBalance}` |

Clients may not send to `/app/**`. Max message size 16 KB.

## 6. Security requirements (technical)

Implemented as described in [SECURITY.md](../SECURITY.md):

| Area | Requirement |
|---|---|
| Tokens | HS256, ≥ 256-bit key, 60 min, claims `sub, role, tv, iss, jti, iat, exp`; 30 s clock skew |
| Passwords | BCrypt cost 12; ≤ 72 bytes |
| Transport | TLS 1.2+ in prod; HSTS 1 year; Android release cleartext disabled |
| Headers | `Content-Security-Policy: default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'` on the API |
| CORS | Origins from config; methods `GET POST PUT PATCH DELETE OPTIONS`; headers `Authorization, Content-Type`; no credentials; max-age 1 h |
| Rate limits | 20/min/IP auth, 300/min/IP API; 429 + `Retry-After` |
| Lockout | 5 failures → 15 min per email |
| Payments | Stripe Checkout; idempotency key `labalink-topup-{paymentId}`; session expiry 30 min |

## 7. Performance and capacity

| Item | Target |
|---|---|
| Concurrent users (one shop) | 50 |
| Machines per shop | ≤ 100 |
| p95 read latency | < 300 ms |
| p95 booking / checkout latency | < 600 ms (excluding Stripe) |
| WebSocket fan-out delay | < 2 s |
| DB connections (Hikari default) | 10 |

List endpoints are not yet paginated. That is acceptable at shop scale,
and pagination is planned for 1.2.

## 8. Logging and monitoring

- SLF4J/Logback to stdout. Security events (rejected webhooks, unhandled
  errors, amount mismatches) are logged at WARN/ERROR.
- **Never logged:** passwords, tokens, card data, request bodies.
- Email attempts recorded in `email_logs` with status and truncated error.
- Liveness: `/actuator/health`.

## 9. Build and run

| Part | Build | Run | Test |
|---|---|---|---|
| API | `./mvnw package` → `target/labalink-1.0.0.jar` | `java -jar target/labalink-1.0.0.jar` | `./mvnw test` |
| Web | `npm ci && npm run build` → `dist/` | Any static host (SPA fallback to `index.html`) | `npm run lint` |
| Android | `./gradlew assembleRelease` | Install APK / upload AAB | `./gradlew lintDebug` |

## 10. Compatibility and versioning

- API changes that break clients require a new major version or a parallel
  endpoint. 1.0 intentionally broke the pre-1.0 contract for security (see
  [CHANGELOG](../CHANGELOG.md)).
- The schema evolves only through new Flyway migrations (`V2__…`).

## 11. Dependencies and licences

All third-party libraries are open source (Apache-2.0 / MIT). Stripe and
Google are external services governed by their own terms.
