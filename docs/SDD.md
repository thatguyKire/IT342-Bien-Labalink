# Software Design Document (SDD)

| | |
|---|---|
| **System** | LabaLink 1.0 |
| **Date** | 2026-10-15 |
| **Standard** | Structure adapted from IEEE 1016 |
| **Related** | [SRS](SRS.md) · [TRD](TRD.md) · [Architecture](../ARCHITECTURE.md) · [Decisions](../DECISIONS.md) |

---

## 1. Introduction

This document describes **how** LabaLink meets the [SRS](SRS.md): its
decomposition, interfaces, data, algorithms and the reasoning behind key
designs. Architectural decisions are recorded as ADRs in
[DECISIONS.md](../DECISIONS.md).

## 2. Design overview

LabaLink is a three-tier, client-server system:

- **Presentation**: Android app (customers) and React SPA (attendants).
- **Application**: a Spring Boot API organised as edge → service →
  repository, plus ports for external providers.
- **Data**: PostgreSQL with Flyway-managed schema.

Design principles:

1. **The server is the authority.** Clients send intent (machine ID,
   scanned code, amount). The server decides identity, price, status and
   money.
2. **Deny by default.** Every endpoint is authenticated unless explicitly
   public. Role and ownership are checked separately.
3. **Money is transactional.** Row locks, exact decimals, idempotent state
   transitions, DB constraints.
4. **Side effects after commit.** Email and WebSocket pushes never announce
   rolled-back state.
5. **Ports for providers.** Stripe sits behind `PaymentGateway`, so business
   logic is testable offline.

## 3. Component design

### 3.1 Backend packages

```mermaid
flowchart TB
    subgraph edu.cit.bien.labalink
        direction TB
        controller["controller<br/>AuthController · UserController<br/>MachineController · BookingController<br/>PaymentController"]
        service["service<br/>AuthService · UserService · MachineService<br/>BookingService · PaymentService<br/>EmailService · WebSocketService<br/>PaymentGateway ⟵ StripePaymentGateway<br/>AfterCommit"]
        security["security<br/>SecurityConfig · JwtProvider<br/>JwtAuthenticationService · JwtAuthenticationFilter<br/>RateLimitFilter · LoginAttemptService<br/>GoogleIdTokenVerifier · OAuth2SuccessHandler<br/>OAuth2CodeStore · UserPrincipal"]
        config["config<br/>AppProperties · PasswordConfig<br/>WebSocketConfig · WebSocketAuthInterceptor<br/>DataInitializer"]
        repository["repository<br/>User · Machine · Booking<br/>Payment · EmailLog repositories"]
        model["model<br/>User · Machine · Booking<br/>Payment · EmailLog · WebSocketMessage"]
        dto["dto<br/>requests / responses"]
        exception["exception<br/>ApiException · GlobalExceptionHandler"]
    end
    controller --> service
    controller --> dto
    service --> repository --> model
    service --> dto
    security --> service
    config --> security
    controller -.throws.-> exception
    service -.throws.-> exception
```

### 3.2 Component responsibilities

| Component | Responsibility | Key collaborators |
|---|---|---|
| `SecurityConfig` | Filter chain, URL rules, CORS, headers, OAuth2 login, JSON 401/403 | `JwtAuthenticationFilter`, `RateLimitFilter`, `OAuth2SuccessHandler` |
| `JwtProvider` | Sign and verify HS256 tokens; enforce secret length | `AppProperties` |
| `JwtAuthenticationService` | Token → `Authentication` (user exists, active, token version matches) | `UserRepository` |
| `RateLimitFilter` | Per-IP fixed-window throttling of `/api/**` | — |
| `LoginAttemptService` | Per-email failure counter and lockout | — |
| `GoogleIdTokenVerifier` | Verify Google ID tokens through JWKS | Nimbus `JwtDecoder` |
| `OAuth2SuccessHandler` / `OAuth2CodeStore` | Web Google login → single-use code handoff | `AuthService` |
| `AuthService` | Register, login, Google, logout, create users | `PasswordEncoder`, `JwtProvider`, `EmailService` |
| `MachineService` | Machine CRUD, QR token generation and rotation | `BookingRepository`, `WebSocketService` |
| `BookingService` | Scan-to-start, state machine, refunds, stats | Machine/User/Booking repositories, `EmailService`, `WebSocketService` |
| `PaymentService` | Checkout creation, sync, webhook handling, idempotent credit | `PaymentGateway`, `EntityManager` |
| `StripePaymentGateway` | Stripe SDK calls with per-request API key and idempotency key | Stripe Java SDK |
| `WebSocketService` | Topic and per-user pushes | `SimpMessagingTemplate` |
| `WebSocketAuthInterceptor` | Authenticate CONNECT, authorise SUBSCRIBE, block SEND | `JwtAuthenticationService` |
| `EmailService` | Async HTML emails with escaping; logs every attempt | `JavaMailSender`, `EmailLogRepository` |
| `GlobalExceptionHandler` | Map exceptions to safe JSON responses | — |
| `DataInitializer` | Bootstrap the first attendant | `AuthService` |

### 3.3 Web dashboard modules

```mermaid
flowchart LR
    App["App.tsx<br/>routes + AttendantRoute guard"] --> Pages
    subgraph Pages
        Login["LoginPage"]
        Cb["OAuth2CallbackPage"]
        M["MachinePage"]
        B["BookingPage"]
        T["TransactionsPage"]
        St["StaffPage"]
        Pay["PaymentResultPage"]
    end
    Pages --> Layout["SidebarLayout"]
    Pages --> Services["services/*<br/>auth · machine · booking · payment · user"]
    Pages --> Hook["hooks/useWebSocket"]
    Services --> Api["lib/api.ts<br/>axios + auth + 401"]
    Api --> Session["lib/session.ts<br/>sessionStorage"]
    Hook --> Session
```

### 3.4 Android modules

```mermaid
flowchart LR
    AppC["LabaLinkApp<br/>(Application)"] --> SM["session/SessionManager"]
    SM --> TC["session/TokenCipher<br/>(Keystore AES-GCM)"]
    AppC --> RC["api/RetrofitClient<br/>auth interceptor · 401 handler"]
    RC --> API["api/LabaLinkApi"]
    subgraph UI
        L["LoginActivity"] --> H["HomeActivity"]
        R["RegisterActivity"]
        H --> D["MachineDetailActivity"] --> Q["QrScannerActivity"]
        H --> W["WalletActivity"]
        H --> BK["BookingsActivity"]
    end
    UI --> API
    UI --> SM
```

## 4. Class design (domain and services)

```mermaid
classDiagram
    direction LR
    class User {
        Long id
        String username
        String email
        String passwordHash
        Role role
        AuthProvider authProvider
        BigDecimal walletBalance
        Boolean isActive
        Integer tokenVersion
        LocalDateTime createdAt
    }
    class Machine {
        Long id
        String machineName
        MachineType machineType
        MachineStatus status
        String qrToken
        BigDecimal hourlyRate
    }
    class Booking {
        Long id
        BookingStatus status
        LocalDateTime startTime
        LocalDateTime endTime
        Integer durationMinutes
        BigDecimal totalPrice
    }
    class Payment {
        Long id
        BigDecimal amount
        String currency
        String stripeCheckoutSessionId
        String stripePaymentIntentId
        String providerReference
        PaymentStatus status
        LocalDateTime paidAt
    }
    class BookingStatus {
        <<enumeration>>
        PENDING
        ACTIVE
        COMPLETED
        CANCELLED
        +isOpen() boolean
        +canTransitionTo(next) boolean
    }
    User "1" --> "*" Booking : places
    Machine "1" --> "*" Booking : used in
    User "1" --> "*" Payment : funds
    Booking --> BookingStatus

    class BookingService {
        +createBooking(caller, request) BookingResponse
        +updateBookingStatus(id, status) BookingResponse
        +cancelOwnBooking(id, caller) BookingResponse
        +getBooking(id, caller) BookingResponse
        +getStats() BookingStats
        -transition(booking, status)
    }
    class PaymentService {
        +createCheckout(caller, amount) PaymentResponse
        +syncPayment(id, caller) PaymentResponse
        +handleWebhook(payload, signature)
        -applySession(id, session)
        -credit(payment, intentId)
        -lockFresh(type, id)
    }
    class PaymentGateway {
        <<interface>>
        +createCheckoutSession(...) CheckoutSession
        +retrieveCheckoutSession(id) CheckoutSession
        +parseWebhook(payload, sig) WebhookEvent
    }
    class StripePaymentGateway
    PaymentGateway <|.. StripePaymentGateway
    PaymentService --> PaymentGateway
    BookingService ..> Booking
    PaymentService ..> Payment
```

## 5. Dynamic design

### 5.1 Request authentication pipeline

```mermaid
sequenceDiagram
    participant C as Client
    participant RL as RateLimitFilter
    participant JF as JwtAuthenticationFilter
    participant JS as JwtAuthenticationService
    participant DB as users table
    participant AZ as Authorization (URL rules + @PreAuthorize)
    participant CT as Controller

    C->>RL: HTTP request
    alt over the per-IP limit
        RL-->>C: 429 Too Many Requests
    end
    RL->>JF: continue
    JF->>JS: authenticate(bearer token)
    JS->>JS: verify signature, issuer, expiry
    JS->>DB: findById(sub)
    JS->>JS: active? tv == token_version?
    JS-->>JF: Authentication(UserPrincipal, ROLE_x) or empty
    JF->>AZ: continue
    alt anonymous on a protected URL
        AZ-->>C: 401 {"success":false,"message":"Authentication required"}
    else wrong role
        AZ-->>C: 403 Access denied
    end
    AZ->>CT: invoke with @AuthenticationPrincipal
```

### 5.2 Password login

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthService
    participant L as LoginAttemptService
    participant DB

    C->>A: POST /api/auth/login {email, password}
    A->>L: isLocked(email)?
    alt locked
        A-->>C: 429 Try again in 15 minutes
    end
    A->>DB: findByEmail (LOCAL accounts only)
    A->>A: BCrypt.matches(password, hash or dummyHash)
    alt mismatch or unknown
        A->>L: recordFailure(email)
        A-->>C: 401 Invalid email or password
    else ok
        A->>L: recordSuccess(email)
        A-->>C: 200 {accessToken, expiresIn, role, userId, ...}
    end
```

### 5.3 Web Google login (code handoff)

```mermaid
sequenceDiagram
    participant B as Browser (SPA)
    participant API
    participant G as Google

    B->>API: GET /oauth2/authorization/google
    API-->>B: 302 to Google (state saved in a short-lived session)
    B->>G: consent
    G-->>B: 302 /login/oauth2/code/google?code&state
    B->>API: callback
    API->>G: exchange code, fetch userinfo (OIDC)
    API->>API: email_verified? find or create CUSTOMER
    API->>API: issue a 60 s single-use code, invalidate the HTTP session
    API-->>B: 302 to FRONTEND/oauth2/callback with code in URL fragment
    B->>B: read the fragment, wipe the URL
    B->>API: POST /api/auth/oauth2/exchange {code}
    API-->>B: {accessToken, role, ...}
    B->>B: role == ATTENDANT ? dashboard : "use the mobile app"
```

### 5.4 Android Google login

```mermaid
sequenceDiagram
    participant App
    participant G as Google Play services
    participant API
    participant J as Google JWKS

    App->>G: signIn(requestIdToken(WEB_CLIENT_ID))
    G-->>App: GoogleSignInAccount.idToken
    App->>API: POST /api/auth/google {idToken}
    API->>J: fetch signing keys (cached)
    API->>API: verify signature, iss, aud, exp, email_verified
    API-->>App: {accessToken, role=CUSTOMER, ...}
    App->>App: encrypt token with Keystore, save
```

### 5.5 Attendant cancels with refund

```mermaid
sequenceDiagram
    participant D as Dashboard
    participant API as BookingService
    participant DB
    participant WS as WebSocket

    D->>API: PATCH /api/bookings/{id}/status {CANCELLED}
    API->>DB: SELECT booking FOR UPDATE
    API->>API: canTransitionTo(CANCELLED)?
    API->>DB: SELECT machine FOR UPDATE → AVAILABLE (unless OUT_OF_ORDER)
    API->>DB: SELECT user FOR UPDATE → wallet += price
    API->>DB: booking CANCELLED, end_time = now
    API-->>D: 200 BookingResponse
    API-)WS: /topic/bookings, /topic/machines, /user/queue/wallet (after commit)
```

The scan-to-start and top-up sequences are in
[ARCHITECTURE.md §4](../ARCHITECTURE.md#4-key-runtime-flows).

## 6. Data design

The relational model, constraints, indexes and migration strategy are in
[Backend-Schema.md](Backend-Schema.md). Highlights:

- `NUMERIC(12,2)` for balances and amounts, `NUMERIC(10,2)` for rates and
  prices.
- Unique: `users.email`, `machines.qr_token`, and the Stripe IDs on
  `payments` (partial unique indexes).
- `CHECK` constraints prevent negative wallets and non-positive amounts.
- Enumerations stored as `VARCHAR`; valid values enforced by JPA enums.

## 7. Algorithms and rules

### 7.1 Pricing
```
price = round_half_up(hourly_rate × duration_minutes ÷ 60, 2)
duration_minutes ∈ [15, 240], default 60
```

### 7.2 Idempotent crediting
```
lock payment row FOR UPDATE (refresh from DB)
if payment.status ≠ PENDING          → return            # already settled
if session.id ≠ payment.session_id   → log, return
if session.complete and paid:
    if session.amount_total ≠ payment.amount×100
       or currency mismatch          → status FAILED, return
    status SUCCESS, paid_at = now
    lock user row, wallet += amount
    after commit: email + /user/queue/wallet
elif session.expired                 → status FAILED
```

### 7.3 QR token
```
token = "LL-" + base64url(SecureRandom 18 bytes)     # 144 bits
compare: MessageDigest.isEqual(expected, scanned)   # constant time
```

### 7.4 Rate limiting
Fixed 60-second windows keyed by `auth:<ip>` (20 requests) or
`api:<ip>` (300 requests). Stale windows are evicted when more than 50,000
clients are tracked.

## 8. Interface design

- REST endpoints, payloads and error format: [TRD §5](TRD.md#5-api-contract).
- WebSocket destinations: [TRD §5.7](TRD.md#57-websocket-stomp).
- UI design and screens: [UI-UX-Design.md](UI-UX-Design.md).

## 9. Error handling design

| Source | Handling | Client sees |
|---|---|---|
| `ApiException` (business) | Status from the exception | `{"success":false,"message":"<safe message>"}` |
| Bean validation | 400, joined field messages | Specific validation messages |
| Malformed JSON or path type | 400 | "Malformed request" |
| Spring Security | 401 / 403 JSON | "Authentication required" / "Access denied" |
| Unique constraint | 409 | "Request conflicts with existing data" |
| Anything else | Logged with stack trace; 500 | "Something went wrong. Please try again." |

Clients show the `message` field (`errorMessage()` helpers in the web and
Android apps).

## 10. Deployment design

See [ARCHITECTURE.md §6](../ARCHITECTURE.md#6-deployment-view). The API is
one fat JAR (`./mvnw package`). The web app is static files (`npm run
build`). The Android app is an APK/AAB signed outside the repo.

## 11. Design for testability

- `PaymentGateway` interface, with `FakePaymentGateway` in tests.
- Embedded PostgreSQL ([ADR-016](../DECISIONS.md#adr-016-embedded-postgresql-for-integration-tests))
  so tests run the real migrations and locks.
- `@MockitoBean JavaMailSender` so email has no network dependency.
- Test suites: `SecurityIntegrationTest`, `BookingIntegrationTest`,
  `PaymentIntegrationTest`, `WebSocketSecurityTest`, `RateLimitFilterTest`,
  `LegacySchemaUpgradeTest`.
