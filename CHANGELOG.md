# Changelog

All notable changes to LabaLink are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and the project uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- [Terms of Service](docs/legal/Terms-of-Service.md) and
  [Privacy Policy](docs/legal/Privacy-Policy.md) for the student demo
  (Data Privacy Act of 2012), plus a
  [checklist](docs/legal/Launch-Checklist.md) for a future real launch.
- The backend refuses to start with a live Stripe key (`sk_live_` /
  `rk_live_`), so the demo can never charge real money.
- Android: registration requires agreeing to the Terms and Privacy Policy
  (age confirmation included). The login screen links to both.
- Web: Terms and Privacy links in the login footer. URLs are configurable
  with `VITE_TERMS_URL` / `VITE_PRIVACY_URL` (web) and
  `labalink.termsUrl` / `labalink.privacyUrl` (Android).
- Demo data: `DEMO_DATA=true` fills an empty database with sample
  machines, customers, bookings and top-ups for demos and screenshots.
  Wallet balances are derived from the seeded history.
- [Demo Guide](docs/Demo-Guide.md): run everything on one computer with
  the Android emulator, including QR scanning without a phone.
- [Third-party notices](THIRD_PARTY_NOTICES.md) for the bundled font and
  icons.

### Changed

- New visual design for both apps (see
  [UI/UX Design](docs/UI-UX-Design.md)): a "Pandan & Mango" palette of
  pandan green with a mango-yellow accent; Plus Jakarta Sans throughout; Lucide icons in
  place of every emoji; a new logo, launcher icon and favicon.
- Android: a Material 3 design layer (colour, type and component styles)
  replaces per-screen hard-coded colours. Home, Bookings and Wallet share
  one bottom bar; Wallet is now a tab, and the balance pill on Home opens
  it. Status labels and colours come from one mapping (`ui/Status.kt`).
- Android motion: screen and tab transitions, cards that rise in on first
  load, press feedback, a live indicator, balances that count up, a
  scanning line in the QR scanner and an animated success check. All of it
  follows the system "Remove animations" setting.
- Android: the machine screen explains the three steps and shows the real
  session price, replacing placeholder "Last used" and "Total sessions"
  values.
- Web: palette and font are Tailwind theme tokens; shared page header,
  stat card, status badge, filter chips, empty state and live indicator
  (`components/ui.tsx`); a deep pandan-green sidebar; a two-column sign-in page.
  Dialogs pop in, table rows rise in, and bookings that change over the
  live feed flash mango. Animations stop under `prefers-reduced-motion`.
- Web: the font is self-hosted, so the dashboard no longer loads anything
  from Google Fonts.

### Fixed

- Web: the live feed never connected in browsers ("Connecting…" forever).
  SockJS sends `/ws/info` with credentials, and the CORS rule for `/ws/**`
  refused them. `/ws/**` now allows credentials for the configured origins
  only; `/api/**` is unchanged.
- Android 15+: the status bar no longer covers the top of every screen.
  Screens draw edge to edge and pad their bars and forms for the system
  bars, display cutout and keyboard (`ui/EdgeToEdge.kt`). The navigation
  bar is no longer hidden, which removes the "Viewing full screen" prompt.
- Tests no longer load the developer's `backend/.env`. A blank
  `GOOGLE_CLIENT_ID=` there made every Spring test fail to start.
- `.env.example` keeps optional settings commented out. Blank `KEY=`
  lines override built-in defaults, which stopped the app from starting
  without Google credentials.

## [1.0.0] - 2026-10-15

Security overhaul and stabilisation release. **Upgrade notes:** the backend
now needs `JWT_SECRET` and the other settings in `backend/.env.example`.
Existing databases are migrated automatically by Flyway. All users must
sign in again. Web self-registration was removed, so create the first
attendant with `BOOTSTRAP_ATTENDANT_EMAIL` / `BOOTSTRAP_ATTENDANT_PASSWORD`.

### Security

- Enforced JWT authentication on every API endpoint. Previously all
  endpoints were `permitAll` and no token was ever checked.
- Removed `POST /api/auth/google-login`, which issued a token for any
  email the client sent. Replaced it with `POST /api/auth/google`, which
  verifies Google ID tokens (signature, issuer, audience, expiry,
  `email_verified`).
- Web Google login now hands off through a single-use 60-second code in the
  URL fragment instead of putting the JWT in the query string. The OAuth2
  session is invalidated after login.
- Registration can no longer set a role. Attendants are created only by
  attendants or bootstrap config.
- Added role checks (`@PreAuthorize`) and ownership checks. Customers can
  only see their own bookings, payments and wallet; non-owners get `404`.
- Bookings: the user comes from the token and the price is computed by the
  server. The scanned QR code must match the machine (constant-time
  compare).
- QR tokens are now 144-bit random secrets, hidden from customers and from
  broadcasts, and rotatable.
- Payments: replaced the replayable `/confirm` endpoint with Stripe
  Checkout, a signature-verified webhook and an idempotent `sync`. The
  wallet is credited once, only for the exact paid amount and currency.
- Added per-IP rate limiting (auth 20/min, API 300/min) and a per-account
  lockout (5 failures → 15 minutes). Unknown-email and wrong-password
  responses are identical, with equalised timing.
- Token revocation through per-user token versions (`POST /api/auth/logout`).
- Locked CORS and WebSocket origins to an allow-list. STOMP `CONNECT` is
  authenticated and subscriptions are authorised. Wallet events moved to
  private `/user/queue/wallet`.
- Added security headers: CSP, X-Frame-Options, nosniff, Referrer-Policy,
  HSTS, Permissions-Policy and COOP.
- Errors no longer leak messages or stack traces.
- Usernames are HTML-escaped in email templates.
- BCrypt cost raised to 12. The password policy now requires 8–72
  characters with a letter and a digit.
- Android: token encrypted with an Android Keystore AES-GCM key. Removed
  global cleartext traffic (debug-only now). Disabled backups of session
  data. Request logging is debug-only, `BASIC` level, with the
  `Authorization` header redacted. R8 minification is on for release.
- Android: removed in-app card number, expiry and CVC fields. Card entry now
  happens only on Stripe's hosted page.
- Fixed 12 vulnerable npm dependencies (1 critical, 9 high).
- Secrets moved to environment variables. `.gitignore` now blocks `.env`,
  keystores and `local.properties`.

### Added

- Flyway migrations (`V1__init_schema.sql`) that create a fresh schema or
  upgrade databases created by earlier builds.
- Wallet is charged when a session starts and refunded when it is
  cancelled. Customers can cancel their own pending booking.
- Booking duration (15–240 minutes, default 60) with server-side pricing.
- Booking state machine: `PENDING → ACTIVE → COMPLETED`, or `CANCELLED`.
- Attendant **Staff** page and `POST /api/users/attendants`.
- Attendant **Transactions** page (read-only view of all top-ups).
- Printable QR sticker with QR code rendering and a "New code" rotation in
  the machine dashboard. Status filter and search.
- `GET /api/users/me`, `GET /api/bookings/me`, `GET /api/payments/me` and
  `GET /api/payments/wallet`.
- `/payment/success` and `/payment/cancel` landing pages for Stripe.
- `/actuator/health` endpoint.
- Integration test suite (32 tests) on embedded PostgreSQL.
- Documentation set: README, PRD, SRS, SDD, TRD, UI/UX, App Flow, Backend
  Schema, Implementation Plan, Architecture, Decisions, Roadmap, Security.

### Changed

- Money is now `BigDecimal` / `NUMERIC(12,2)` everywhere, with DB `CHECK`
  constraints. Machine and wallet rows are locked during bookings and
  payments.
- Emails and WebSocket events are sent only after the transaction commits.
- The web dashboard is attendant-only. The Android app is customer-only.
- The web API client is centralised (`lib/api.ts`) with an auth header,
  `401` handling and server error messages. The session lives in
  `sessionStorage` with an expiry.
- Android: one `LabaLinkApi` Retrofit interface with an auth interceptor.
  The base URL and Google client ID come from `local.properties`.
- `useWebSocket` keeps handlers in a ref instead of reconnecting on every
  render, and sends the JWT on connect.
- Hourly rate validation (₱1–₱10,000, 2 decimals). Machines with booking
  history can no longer be deleted (mark them Out of Order).

### Fixed

- Android: starting a wash always failed with "No machine selected"
  because the machine ID was never passed to the detail screen.
- Android: the wallet sent the local payment row ID to Stripe as if it were
  a PaymentIntent ID.
- Android: the bookings screen downloaded every customer's bookings and
  filtered them on the device.
- Android: added the missing `INTERNET` permission. The date parser no
  longer uses `java.time`, which crashed on Android 7 (minSdk 24).
- Web: modal overlays were solid black (Tailwind v4 removed
  `bg-opacity-*`).
- Web: `PATCH` was missing from CORS, so attendants could not change
  booking status.
- Web: the wallet page used a hard-coded `userId = 1`.
- Backend: payment lookup scanned the entire payments table. Revenue was
  computed in memory.
- The Gradle wrapper JAR was ignored by git, so `./gradlew` failed after
  cloning.

### Removed

- Web self-registration page (attendants are provisioned).
- Web wallet top-up and Stripe Elements (top-ups are a customer action in
  the app).
- Unused `DashboardPage`, the `OAuth2Controller` role-hint redirects, and
  the `/api/payments/confirm`, `/api/payments/user/{email}` and
  `/api/payments/wallet/{userId}` endpoints.

## [0.5.0] - 2026-05-08

### Added
- Mobile home screen, machine detail, bookings, wallet top-up and QR
  scanner.

## [0.4.0] - 2026-05-01

### Added
- Stripe payment gateway and wallet top-up (web).

## [0.3.0] - 2026-04-29

### Added
- Google sign-in.
- Bookings and transactions page on the web dashboard.

## [0.2.0] - 2026-04-11

### Added
- Machine management (CRUD, QR token, status).
- CORS configuration.

## [0.1.0] - 2026-03-07

### Added
- Initial project setup: backend, web and mobile folders.
- User registration and login with JWT.

[Unreleased]: https://github.com/thatguyKire/IT342-Bien-Labalink/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/thatguyKire/IT342-Bien-Labalink/releases/tag/v1.0.0
