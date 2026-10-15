# Security

LabaLink holds personal data (names, emails) and **money** (wallet
balances funded by real card payments). Security is treated as the
top non-functional requirement. This document describes the threat model,
the controls in place, how to deploy safely, and how to report a problem.

- [Reporting a vulnerability](#reporting-a-vulnerability)
- [Threat model](#threat-model)
- [Controls](#controls)
- [Vulnerabilities fixed in 1.0.0](#vulnerabilities-fixed-in-100)
- [Production deployment checklist](#production-deployment-checklist)
- [Known limitations](#known-limitations)

---

## Reporting a vulnerability

Please **do not open a public GitHub issue** for security problems.
Use GitHub's private vulnerability reporting (**Security → Report a
vulnerability**) on this repository instead. Include steps to reproduce
and the impact you observed. We aim to acknowledge reports within 72 hours.

## Threat model

### Assets

| Asset | Why it matters |
|---|---|
| Wallet balances | Spendable value; inflating one is direct theft from the shop |
| Accounts and roles | An attacker-controlled attendant account controls the shop |
| Customer PII (name, email, booking history) | Privacy; Data Privacy Act of 2012 (RA 10173) |
| Machine QR tokens | Prove physical presence; a leaked token lets someone start a machine remotely |
| Secrets (JWT key, Stripe key, DB password) | Each one fully compromises its system |

### Actors

- **Anonymous internet user**: can reach the public API and web app.
- **Authenticated customer**: has a valid account and may be malicious
  (tries to read others' data, get free washes, inflate their wallet).
- **Stolen-device holder**: has physical access to a customer's phone.
- **Network attacker**: on the same Wi-Fi as a customer or attendant.

### Top risks (STRIDE) and mitigations

| Threat | Example | Mitigation |
|---|---|---|
| **Spoofing** | Log in as someone else via Google | Server verifies Google ID token signature, issuer, audience, expiry and `email_verified` |
| **Spoofing** | Brute-force passwords | Per-IP rate limit + per-account lockout; BCrypt cost 12 |
| **Tampering** | Change price or user ID in a booking request | Server ignores client price/user; derives the user from the JWT and the price from the machine rate |
| **Tampering** | Replay a payment confirmation to multiply wallet credit | Payment row locked; only `PENDING → SUCCESS` credits; Stripe is the source of truth |
| **Tampering** | Forge a Stripe webhook | HMAC signature verification with the webhook secret |
| **Repudiation** | "I never booked that" | Booking, payment and email-log rows with timestamps |
| **Information disclosure** | Read other users' bookings and payments (IDOR) | Ownership checks; non-owners get `404`; customer endpoints scoped to `/me` |
| **Information disclosure** | Subscribe to another user's wallet feed | STOMP CONNECT requires a JWT; per-user `/user/queue/*`; `/topic/bookings` is attendants-only |
| **Information disclosure** | Stack traces or SQL in errors | Global error handler; `include-message/stacktrace: never` |
| **Denial of service** | Hammer login or the API | Rate limiting; WebSocket message size limits |
| **Elevation of privilege** | Register as `ATTENDANT` | No role field on sign-up; attendants are created only by attendants or bootstrap config |

## Controls

### Authentication

- **JWT (HS256)** access tokens, 60 minutes by default, signed with a key of
  at least 32 bytes. The app refuses to start without one.
- Tokens carry `sub` (user ID), `role`, `tv` (token version), `iss`, `jti`,
  `iat` and `exp`. Every request reloads the user, so **deactivation and
  role changes take effect immediately**.
- **Server-side revocation**: `POST /api/auth/logout` increments the user's
  token version, which invalidates every token issued before.
- Passwords: **BCrypt (cost 12)**, 8–72 characters, must contain a letter
  and a digit. Input longer than 72 bytes is rejected rather than silently
  truncated.
- **Login hardening**: identical response for "unknown email" and "wrong
  password"; a dummy BCrypt comparison equalises timing; 5 failures lock the
  account for 15 minutes.
- **Google sign-in**
  - *Android*: the app sends a Google **ID token**. The server verifies it
    against Google's JWKS (signature, issuer, audience = our client IDs,
    expiry, `email_verified`).
  - *Web*: standard OAuth2 authorization-code flow with Spring Security
    (the `state` anti-CSRF parameter is handled by the framework). After success the server
    issues a **single-use, 60-second code** in the URL *fragment*; the SPA
    swaps it for a JWT. Tokens never appear in query strings, server logs or
    `Referer` headers.

### Authorisation

- Deny-by-default rules: every `/api/**` route requires authentication
  except register, login, Google, OAuth2 code exchange and the Stripe
  webhook. Unknown paths are denied.
- Method-level `@PreAuthorize("hasRole('ATTENDANT')")` or `'CUSTOMER'` on
  each sensitive endpoint.
- **Ownership** enforced in services (bookings and payments). Non-owners get
  `404` so record IDs cannot be enumerated.
- Customers never receive machine **QR tokens**. Neither does the public
  `/topic/machines` broadcast.

### Business-logic integrity (money)

- All money is `NUMERIC(12,2)` / `BigDecimal`. No floating-point arithmetic.
- **Price is computed by the server** from the machine's hourly rate and the
  requested duration (15–240 minutes).
- **Pessimistic row locks** (`SELECT … FOR UPDATE`) on the machine and wallet
  during booking, cancellation and crediting. Two customers can't take the
  same machine, and one wallet can't be spent twice at the same time.
- DB `CHECK` constraints: wallet ≥ 0, price ≥ 0, rate > 0, payment > 0.
- Booking state machine enforced server-side
  (`PENDING → ACTIVE → COMPLETED`, `PENDING/ACTIVE → CANCELLED`). A cancelled
  booking can't be refunded twice.
- **Stripe**: the server creates Checkout Sessions with an idempotency key.
  It credits a wallet only after Stripe reports `status=complete` and
  `payment_status=paid`, **and** the amount and currency match our record.
  Any mismatch is flagged `FAILED` for manual review. Top-ups are limited to
  ₱50–₱10,000 per transaction and a ₱50,000 wallet cap.
- QR tokens: 144 random bits from `SecureRandom`, compared in **constant
  time**, rotatable by attendants.

### Transport and browser

- **CORS**: explicit origin allow-list from `CORS_ALLOWED_ORIGINS`; no
  wildcards; no credentials (bearer tokens only).
- **CSRF**: not applicable to the API. No cookie authenticates API calls,
  and the OAuth2 session cookie is invalidated right after login.
- API security headers: `Content-Security-Policy: default-src 'none'`,
  `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: no-referrer`, HSTS (on HTTPS), `Permissions-Policy`,
  `Cross-Origin-Opener-Policy`.
- Web app headers: strict CSP (`script-src 'self'`), frame-ancestors none.
  See `web/vite.config.ts` and `web/public/_headers`.
- Web tokens live in `sessionStorage` (cleared when the tab closes). They
  are short-lived and revocable. React escapes all rendered data. There is
  no `dangerouslySetInnerHTML`.

### WebSocket

- STOMP `CONNECT` must carry `Authorization: Bearer <jwt>`; validated with
  the same rules as HTTP.
- Subscriptions are authorised: `/topic/machines` (any user, no secrets),
  `/topic/bookings` (attendants), `/user/queue/*` (the user's own queue).
  Clients cannot `SEND`.
- Allowed origins come from the same allow-list as CORS. Message size is
  capped at 16 KB.

### Android

- Access token encrypted with **AES-256-GCM** using a non-exportable key in
  the **Android Keystore**.
- `allowBackup=false` and backup/data-extraction rules exclude preferences.
- **Release builds are HTTPS-only** (network security config plus a runtime
  check). Cleartext HTTP is allowed only in the debug build, for local
  development.
- HTTP logging only in debug builds, at `BASIC` level, with `Authorization`
  redacted. R8 minification and shrinking are enabled for release.
- Card details are entered only on **Stripe's hosted Checkout page**. The
  app opens only `https://*.stripe.com` URLs.
- A `401` from the server wipes the local session immediately.

### Data protection and operations

- Secrets come only from environment variables or `backend/.env`. The
  `.gitignore` blocks `.env`, keystores and `local.properties`.
- `server.error.*` hides messages, traces and exceptions. Unexpected errors
  are logged server-side and answered with a generic message.
- User-supplied text is HTML-escaped before it goes into email templates.
- Only `/actuator/health` is exposed, without details.
- Dependencies: `npm audit` is clean at release. Keep Maven and Gradle
  dependencies patched (see the roadmap for automated scanning).

## Vulnerabilities fixed in 1.0.0

These issues existed in pre-1.0 builds and were fixed during the security
overhaul (details in [CHANGELOG.md](CHANGELOG.md)):

| Severity | Issue |
|---|---|
| Critical | Every API endpoint was public (`permitAll`). No JWT was ever checked |
| Critical | `POST /api/auth/google-login` issued a token for **any email** sent by the client (account takeover) |
| Critical | `POST /api/payments/confirm` could be replayed to credit the same payment repeatedly |
| Critical | Self-registration accepted `"role": "ATTENDANT"` |
| High | Booking price and user ID came from the client (free washes, booking as others) |
| High | Any user could read every booking, every payment and any wallet by ID or email |
| High | QR scan result was ignored, and QR tokens were sent to every client |
| High | WebSocket accepted any origin, needed no auth, and broadcast wallet updates on guessable topics |
| High | CORS allowed every origin with credentials |
| High | JWT placed in OAuth redirect query string |
| High | Android app collected raw card numbers in its own form |
| Medium | Android allowed cleartext HTTP globally and logged full request bodies |
| Medium | HTML injection in emails through usernames |
| Medium | Floating-point money; no DB constraints; no concurrency control |
| Medium | 12 vulnerable npm packages (one critical) |

## Production deployment checklist

- [ ] Serve the API and web app over **HTTPS only**. Set
      `SESSION_COOKIE_SECURE=true`. Set `FORWARD_HEADERS_STRATEGY=native`
      only when behind a trusted proxy.
- [ ] Generate a fresh `JWT_SECRET` (`openssl rand -base64 48`). Never reuse
      the dev one.
- [ ] Use **live** Stripe keys only in production. Configure the webhook
      endpoint in the Stripe dashboard with the events
      `checkout.session.completed`,
      `checkout.session.async_payment_succeeded`,
      `checkout.session.async_payment_failed` and
      `checkout.session.expired`.
- [ ] Set `CORS_ALLOWED_ORIGINS` and `FRONTEND_URL` to the real dashboard
      origin.
- [ ] Use a dedicated PostgreSQL user with least privilege. Enable TLS to
      the database and turn on backups.
- [ ] Remove `BOOTSTRAP_ATTENDANT_PASSWORD` after the first start, and
      change that password.
- [ ] Mirror the CSP in `web/public/_headers` on your static host and put
      your API origin in `connect-src`.
- [ ] Build the Android release with `labalink.apiBaseUrlRelease` set to the
      HTTPS API, and sign it with a keystore kept outside the repo.
- [ ] Turn on GitHub secret scanning and Dependabot.

## Known limitations

- Rate-limit counters, login lockouts and OAuth2 handoff codes are kept
  **in memory**. They are per instance and reset on restart. Run a single
  instance, or move them to Redis before scaling out (see
  [ROADMAP.md](ROADMAP.md)).
- There are no refresh tokens. Users sign in again after 60 minutes.
  Logout revokes tokens on **all** of a user's devices.
- Per-account lockout can be abused to temporarily lock a known victim's
  account (15 minutes). This is an accepted trade-off against password
  guessing.
- Registration reveals whether an email is already registered (`409`),
  which most sign-up flows accept.
