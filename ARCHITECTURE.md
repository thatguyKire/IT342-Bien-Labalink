# Architecture

This is the high-level view of how LabaLink is put together. Detailed
component, class and sequence designs live in [docs/SDD.md](docs/SDD.md).
The reasons behind each choice are in [DECISIONS.md](DECISIONS.md).

## 1. System context

```mermaid
flowchart TB
    customer(["👤 Customer"])
    attendant(["🧑‍💼 Shop attendant"])

    subgraph LabaLink
        mobile["Android app"]
        web["Web dashboard"]
        api["LabaLink API"]
        db[("PostgreSQL")]
    end

    stripe["Stripe<br/>(Checkout + webhooks)"]
    google["Google Identity"]
    smtp["SMTP provider"]

    customer --> mobile
    attendant --> web
    mobile -->|HTTPS REST| api
    web -->|HTTPS REST + WSS| api
    api --> db
    api -->|create / verify sessions| stripe
    stripe -->|signed webhooks| api
    customer -->|pays on hosted page| stripe
    api -->|verify ID tokens, OAuth2| google
    api -->|transactional email| smtp
```

## 2. Containers

| Container | Tech | Responsibility |
|---|---|---|
| **API** (`backend/`) | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, STOMP, Flyway | Business rules, authN/authZ, payments, real-time events, email |
| **Database** | PostgreSQL 14+ | System of record for users, machines, bookings, payments, email logs |
| **Web dashboard** (`web/`) | React 19, TypeScript, Vite 7, Tailwind 4, Axios, SockJS/STOMP | Attendant operations: machines, bookings, transactions, staff |
| **Android app** (`mobiledev/`) | Kotlin, Views/ViewBinding, Retrofit, OkHttp, ZXing, Google Sign-In | Customer self-service: browse, scan to start, wallet, history |

## 3. Backend layers

```mermaid
flowchart TB
    subgraph web_layer["HTTP / WebSocket edge"]
        RL["RateLimitFilter"]
        JWTF["JwtAuthenticationFilter"]
        WSI["WebSocketAuthInterceptor"]
        CTRL["Controllers<br/>Auth · User · Machine · Booking · Payment"]
        GEH["GlobalExceptionHandler"]
    end
    subgraph service_layer["Services (transactions + rules)"]
        AUTH["AuthService"]
        MACH["MachineService"]
        BOOK["BookingService"]
        PAY["PaymentService"]
        WSS["WebSocketService"]
        MAIL["EmailService (@Async)"]
    end
    subgraph ports["Ports / adapters"]
        GW["PaymentGateway → StripePaymentGateway"]
        GV["GoogleIdTokenVerifier"]
        REPO["Spring Data repositories"]
    end
    DB[("PostgreSQL")]

    RL --> JWTF --> CTRL
    WSI --> WSS
    CTRL --> AUTH & MACH & BOOK & PAY
    AUTH --> GV
    PAY --> GW
    AUTH & MACH & BOOK & PAY --> REPO --> DB
    BOOK & PAY & MACH -->|after commit| WSS
    AUTH & BOOK & PAY -->|after commit| MAIL
    CTRL -.errors.-> GEH
```

**Rules of the road**

- Controllers are thin: validate input (`@Valid`), check the role
  (`@PreAuthorize`), and pass the authenticated `UserPrincipal` to a service.
- Services own transactions and business rules: pricing, state machines,
  ownership, locking.
- Side effects (WebSocket pushes, emails) run **after commit**
  (`AfterCommit`), so clients never see rolled-back changes.
- External providers sit behind interfaces (`PaymentGateway`), so the money
  logic is testable without Stripe.

## 4. Key runtime flows

### Scan-to-start booking

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant App as Android app
    participant API
    participant DB as PostgreSQL
    participant Dash as Web dashboard

    C->>App: Pick machine, tap "Scan & Start"
    App->>App: Scan QR sticker (ZXing)
    App->>API: POST /api/bookings {machineId, qrToken, durationMinutes}<br/>Authorization: Bearer JWT
    API->>DB: SELECT machine FOR UPDATE
    API->>API: constant-time compare qrToken, check AVAILABLE
    API->>API: price = rate × minutes / 60
    API->>DB: SELECT user FOR UPDATE, check balance
    API->>DB: debit wallet, insert booking PENDING, machine RUNNING
    API-->>App: 201 BookingResponse
    API-)Dash: /topic/bookings BOOKING_CREATED (after commit)
    API-)Dash: /topic/machines MACHINE_UPDATED
```

### Wallet top-up

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer
    participant App as Android app
    participant API
    participant S as Stripe
    participant DB as PostgreSQL

    App->>API: POST /api/payments/checkout {amount}
    API->>DB: insert payment PENDING
    API->>S: create Checkout Session (idempotency key)
    API-->>App: {id, checkoutUrl}
    App->>S: open checkoutUrl in browser
    C->>S: enter card, pay
    S-)API: webhook checkout.session.completed (signed)
    API->>API: verify signature
    API->>DB: lock payment, if PENDING and amount matches → SUCCESS, credit wallet
    C->>App: return to app
    App->>API: POST /api/payments/{id}/sync
    API->>S: retrieve session (if still PENDING)
    API-->>App: status SUCCESS, balance updated
```

## 5. Security architecture

Defence in depth, from the outside in:

1. **Edge**: HTTPS, CORS allow-list, security headers, per-IP rate limits.
2. **Identity**: JWT verification on every request; user reloaded from
   the DB (active flag, role, token version).
3. **Authorisation**: URL rules (deny by default), then `@PreAuthorize`
   role checks, then ownership checks in services.
4. **Integrity**: server-side pricing, a state machine, row locks,
   idempotent payment crediting, DB constraints.
5. **Data**: BCrypt passwords, exact decimals, escaped email templates,
   no secrets in the repo.

Full details: [SECURITY.md](SECURITY.md).

## 6. Deployment view

```mermaid
flowchart LR
    subgraph Internet
        U1["Browser"]
        U2["Android device"]
        ST["Stripe"]
    end
    subgraph Hosting
        CDN["Static host / CDN<br/>web/dist + security headers"]
        RP["Reverse proxy (TLS)"]
        APP["Spring Boot JAR<br/>(1 instance)"]
        PG[("Managed PostgreSQL")]
    end
    U1 -->|HTTPS| CDN
    U1 -->|HTTPS / WSS| RP
    U2 -->|HTTPS| RP
    ST -->|webhook HTTPS| RP
    RP --> APP --> PG
```

The API currently runs as a **single instance**: rate limits, lockouts and
OAuth2 codes are in memory, and the STOMP broker is the in-process simple
broker. Scaling out requires Redis (shared counters and codes) and a
broker relay such as RabbitMQ. This is tracked in [ROADMAP.md](ROADMAP.md).

## 7. Cross-cutting concerns

| Concern | Approach |
|---|---|
| Configuration | `application.yml` + environment variables / `backend/.env`; typed `AppProperties` |
| Schema | Flyway (`db/migration`), Hibernate `ddl-auto=validate` |
| Errors | `ApiException` (safe messages) + `GlobalExceptionHandler` (generic 500s) |
| Time | Server clock; JDBC time zone UTC |
| Observability | SLF4J logs (no tokens or PII bodies), `/actuator/health` |
| Testing | MockMvc + embedded PostgreSQL integration tests; fake payment gateway |
