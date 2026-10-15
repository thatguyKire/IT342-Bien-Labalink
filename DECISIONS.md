# Architecture Decision Records

Each record captures one significant decision: the context, what was
decided, and the consequences. Status values: **Accepted**, **Superseded**,
**Proposed**.

| # | Decision | Status |
|---|---|---|
| [001](#adr-001-monorepo-with-backend-web-and-mobile) | Monorepo with backend, web and mobile | Accepted |
| [002](#adr-002-spring-boot-and-postgresql-for-the-api) | Spring Boot and PostgreSQL for the API | Accepted |
| [003](#adr-003-stateless-jwt-bearer-authentication) | Stateless JWT bearer authentication | Accepted |
| [004](#adr-004-token-versioning-for-server-side-revocation) | Token versioning for server-side revocation | Accepted |
| [005](#adr-005-public-sign-up-creates-customers-only) | Public sign-up creates customers only | Accepted |
| [006](#adr-006-web-token-in-sessionstorage) | Web token in sessionStorage | Accepted |
| [007](#adr-007-verified-google-sign-in-for-both-clients) | Verified Google sign-in for both clients | Accepted |
| [008](#adr-008-stripe-checkout-with-webhook-and-sync-confirmation) | Stripe Checkout with webhook and sync confirmation | Accepted |
| [009](#adr-009-in-memory-rate-limiting-and-lockout) | In-memory rate limiting and lockout | Accepted |
| [010](#adr-010-charge-the-wallet-when-the-session-starts) | Charge the wallet when the session starts | Accepted |
| [011](#adr-011-exact-decimals-and-pessimistic-locking-for-money) | Exact decimals and pessimistic locking for money | Accepted |
| [012](#adr-012-flyway-with-an-idempotent-baseline) | Flyway with an idempotent baseline | Accepted |
| [013](#adr-013-qr-token-as-a-presence-secret) | QR token as a presence secret | Accepted |
| [014](#adr-014-stomp-over-sockjs-with-per-user-queues) | STOMP over SockJS with per-user queues | Accepted |
| [015](#adr-015-web-is-attendant-only-android-is-customer-only) | Web is attendant-only, Android is customer-only | Accepted |
| [016](#adr-016-embedded-postgresql-for-integration-tests) | Embedded PostgreSQL for integration tests | Accepted |

---

## ADR-001: Monorepo with backend, web and mobile

**Context.** One small team builds three deliverables that share one API
contract.
**Decision.** Keep `backend/`, `web/` and `mobiledev/` in one repository,
with shared docs at the root.
**Consequences.** Atomic changes across API and clients; a single place for
docs. Each part still has its own build tool (Maven, npm, Gradle).

## ADR-002: Spring Boot and PostgreSQL for the API

**Context.** The course stack is Java. We need transactions, row locking
and strong consistency for money.
**Decision.** Spring Boot 3.5 (Web, Security, Data JPA, WebSocket, Mail,
OAuth2 Client) on PostgreSQL.
**Consequences.** A mature security framework; real `SELECT … FOR UPDATE`;
`NUMERIC` money type.

## ADR-003: Stateless JWT bearer authentication

**Context.** Two very different clients (an SPA and a native app) call the
same API.
**Decision.** Short-lived (60 min) HS256 JWTs sent as
`Authorization: Bearer`. There is no server session for API calls. The
security context is stored per request only.
**Consequences.** One mechanism for both clients. CSRF does not apply,
because no ambient cookie credential exists. Revocation needs extra work
(ADR-004). Users re-authenticate hourly until refresh tokens are added.

## ADR-004: Token versioning for server-side revocation

**Context.** Pure JWTs cannot be revoked before they expire, so logout
would be cosmetic.
**Decision.** Each user has a `token_version`, embedded in the token as
`tv`. The filter reloads the user on every request and rejects mismatches.
Logout increments the version.
**Consequences.** Real logout and instant effect of deactivation or role
changes, at the cost of one indexed primary-key lookup per request. Logout
signs the user out on all devices.

## ADR-005: Public sign-up creates customers only

**Context.** Pre-1.0 registration accepted a `role` field, so anyone could
become an attendant.
**Decision.** `RegisterRequest` has no role. Attendants are created only by
an existing attendant (`POST /api/users/attendants`) or once from
`BOOTSTRAP_ATTENDANT_*` configuration. Google sign-up always creates a
customer.
**Consequences.** Removes the privilege-escalation path. The web app has
no public registration page.

## ADR-006: Web token in sessionStorage

**Context.** Options were localStorage, sessionStorage, memory only, or an
httpOnly cookie.
**Decision.** Store the access token in `sessionStorage`, combined with a
strict CSP, a 60-minute lifetime and server revocation.
**Consequences.** Survives page reloads but not closing the tab. Still
readable by injected script, so XSS prevention (React escaping, CSP
`script-src 'self'`, no `dangerouslySetInnerHTML`) is essential. An
httpOnly-cookie + CSRF-token design is on the roadmap.

## ADR-007: Verified Google sign-in for both clients

**Context.** The old mobile flow posted `{email, name, role}` and the
server trusted it (account takeover). The old web flow put the JWT in a
redirect query string.
**Decision.** *Android*: send Google's **ID token**. The server verifies it
with Nimbus against Google's JWKS (issuer, audience, expiry,
`email_verified`). *Web*: Spring Security OAuth2 login. The success handler
issues a 60-second single-use code in the URL fragment, which the SPA
exchanges for a JWT. The OAuth2 HTTP session is then invalidated.
**Consequences.** No identity is taken on the client's word, and no tokens
appear in URLs or logs. Requires configured Google client IDs.

## ADR-008: Stripe Checkout with webhook and sync confirmation

**Context.** The Android app collected card numbers in its own fields (out
of PCI scope rules) and confirmed payments with a forgeable, replayable
call.
**Decision.** Use **Stripe Checkout** (a hosted page). The server is the
only party that decides a payment succeeded: either from a **signed
webhook** or by retrieving the session from Stripe on
`POST /api/payments/{id}/sync`. Crediting is idempotent (row lock +
`PENDING`-only transition) and checks amount and currency.
**Consequences.** No card data in LabaLink (the lightest PCI SAQ). It works
in local development without webhooks (sync). The web dashboard no longer
processes top-ups; that is a customer action in the app.

## ADR-009: In-memory rate limiting and lockout

**Context.** We need brute-force protection now, without new infrastructure.
**Decision.** A fixed-window per-IP limiter (20/min for `/api/auth/**`,
300/min elsewhere) and a per-email lockout (5 failures → 15 minutes), both
in `ConcurrentHashMap`s.
**Consequences.** Simple and dependency-free, but counters are per instance
and reset on restart. Must move to Redis before running more than one
instance. Behind a proxy, set `FORWARD_HEADERS_STRATEGY` so the real client
IP is used.

## ADR-010: Charge the wallet when the session starts

**Context.** Pre-1.0 wallets could be topped up but were never spent.
Bookings carried a client-supplied price.
**Decision.** When a customer scans to start, the server prices the session
(rate × minutes / 60) and debits the wallet in the same transaction.
Cancelling (customer while `PENDING`, or an attendant any time before
completion) refunds the full amount.
**Consequences.** Prepaid model with no debt. Customers must top up before
washing. Refunds are automatic and auditable.

## ADR-011: Exact decimals and pessimistic locking for money

**Context.** `Double` money and unsynchronised read-modify-write updates
could lose or invent centavos and allowed races.
**Decision.** `BigDecimal` in Java and `NUMERIC(12,2)` in the DB.
`PESSIMISTIC_WRITE` locks on the machine and wallet during booking,
cancellation and crediting, with locked rows re-read
(`EntityManager.refresh`) so stale copies are never used. `CHECK`
constraints act as a last line of defence.
**Consequences.** Correct under concurrency, with slightly more contention
on hot rows. That is negligible at shop scale.

## ADR-012: Flyway with an idempotent baseline

**Context.** Pre-1.0 used `ddl-auto=update`, so existing databases differ
from the new model (types, missing columns).
**Decision.** Flyway owns the schema, and Hibernate only validates. `V1` is
written with `IF NOT EXISTS` / `ALTER … IF NOT EXISTS`, and the app runs
with `baseline-on-migrate` at version 0. The same script creates a fresh
schema and upgrades a legacy one.
**Consequences.** Safe upgrades for existing installs. A dedicated test
proves it. Future changes are new `V2__…` scripts.

## ADR-013: QR token as a presence secret

**Context.** Scanning a sticker should prove the customer is at the
machine. The old app ignored the scan and every client received all tokens.
**Decision.** Tokens are `LL-` plus 144 random bits (`SecureRandom`). They
are returned only to attendants, never broadcast, compared in constant
time, and rotatable from the dashboard (which renders a printable sticker).
**Consequences.** Remote starts require a photo of the sticker. Rotating a
token invalidates the printed sticker.

## ADR-014: STOMP over SockJS with per-user queues

**Context.** Wallet updates went to `/topic/wallet/{email}`, which anyone
could subscribe to.
**Decision.** Authenticate the STOMP `CONNECT` frame with the JWT.
Authorise every `SUBSCRIBE`. Send private events with
`convertAndSendToUser` to `/user/queue/*`. Clients may not `SEND`.
**Consequences.** Real-time updates without leaking data. The simple
in-memory broker limits us to one instance (see ADR-009).

## ADR-015: Web is attendant-only, Android is customer-only

**Context.** The old web app let any logged-in user (customers included)
use the attendant dashboard.
**Decision.** The web app accepts only `ATTENDANT` sessions. The Android app
accepts only `CUSTOMER` sessions. The server enforces roles regardless.
**Consequences.** Clearer UX and a smaller attack surface per client.
Customers who want a web experience would need a separate customer web app
(see the roadmap).

## ADR-016: Embedded PostgreSQL for integration tests

**Context.** H2 does not behave like PostgreSQL (locks, `DO $$` blocks,
partial indexes), and Docker isn't always available on student machines.
**Decision.** Use `io.zonky.test:embedded-postgres` (real PostgreSQL
binaries, no Docker) for `@SpringBootTest` integration tests, with a fake
`PaymentGateway` and a mocked mail sender.
**Consequences.** Tests exercise the real migrations and SQL. The first run
downloads the binaries.
