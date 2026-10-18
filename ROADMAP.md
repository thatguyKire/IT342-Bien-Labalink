# Roadmap

Where LabaLink goes after 1.0. Items are grouped by milestone and ordered
by priority within each group. Security items come first in every
milestone.

**Legend:** 🔒 security · ✨ feature · 🛠️ engineering · 📈 scale

---

## ✅ 1.0 — Secure foundation (done, 2026-10-15)

- 🔒 JWT auth everywhere, role and ownership checks, verified Google
  sign-in, rate limiting, account lockout, token revocation
- 🔒 Stripe Checkout with signed webhooks and idempotent crediting
- 🔒 Keystore-encrypted tokens on Android, HTTPS-only release builds
- ✨ Wallet charged on scan-to-start, refunds on cancel, booking durations
- ✨ Staff management, transactions view, printable and rotatable QR stickers
- 🛠️ Flyway migrations, BigDecimal money, row locking, 32 integration tests
- 🛠️ Full documentation set

## 1.1 — Account essentials (next)

- 🔒 **Password reset by email**: single-use, hashed, 30-minute tokens
- 🔒 **Email verification** for email/password sign-ups
- 🔒 **Refresh tokens**: short access tokens (15 min) plus rotating
  refresh tokens with reuse detection; per-device logout
- 🔒 **Change password** in both apps (bumps the token version)
- 🔒 **Account deletion**: customer-requested delete that anonymises
  booking and payment records kept for tax law (required by the Privacy
  Policy and Google Play)
- ✨ Edit profile (display name)
- ✨ Attendants can deactivate or reactivate customer and staff accounts
- 🛠️ **CI pipeline** (GitHub Actions): `mvn test`, `npm run lint && build`,
  `gradlew assembleDebug lint`, on every PR
- 🛠️ **Dependabot** plus OWASP Dependency-Check / `npm audit` gates in CI

## 1.2 — Better operations

- ✨ **Push notifications** (FCM): "Your wash is done", "Session activated"
- ✨ Live customer updates in Android (STOMP `/user/queue/*` already exists
  on the server)
- ✨ Auto-complete sessions when `end_time` passes, plus an
  "overdue" flag for attendants
- ✨ Booking duration picker in the app (server supports 15–240 minutes)
- ✨ Pagination and date filters for bookings and transactions
- ✨ Revenue dashboard: daily, weekly and per-machine charts
- ✨ CSV export of transactions for accounting
- 🛠️ OpenAPI / Swagger UI generated from controllers (attendant-only in prod)

## 1.3 — Scale and resilience

- 📈 Move rate limits, login lockouts and OAuth2 codes to **Redis**
- 📈 External STOMP broker relay (RabbitMQ) for multi-instance WebSockets
- 📈 Containerise (Dockerfile + docker-compose for local dev)
- 🔒 Web auth via **httpOnly, SameSite=Strict cookies** + CSRF tokens
  (replaces sessionStorage, see ADR-006)
- 🔒 Certificate pinning in the Android app for the production API host
- 🔒 Audit log table for attendant actions (who changed what, when)
- 🛠️ Structured JSON logging, request IDs, Prometheus metrics

## 2.0 — Smart laundry

- ✨ **IoT integration**: machines report real status and start or stop
  through an MQTT bridge, replacing manual "Activate/Complete"
- ✨ Reservations: book a machine for a future time slot
- ✨ Multiple branches, each with its own staff, machines and pricing
- ✨ Loyalty points and promo codes
- ✨ Customer web app (separate from the attendant dashboard)
- ✨ GCash / Maya payment methods through Stripe or a local gateway

---

Have an idea? Open an issue with the `enhancement` label.
