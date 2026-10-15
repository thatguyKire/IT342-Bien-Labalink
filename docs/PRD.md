# Product Requirements Document (PRD)

| | |
|---|---|
| **Product** | LabaLink — Smart Laundry Management System |
| **Version** | 1.0 |
| **Date** | 2026-10-15 |
| **Status** | Approved for 1.0 release |
| **Related** | [SRS](SRS.md) · [TRD](TRD.md) · [UI/UX](UI-UX-Design.md) · [Roadmap](../ROADMAP.md) |

---

## 1. Problem

Self-service laundry shops in the Philippines still run on coins, paper
logbooks and shouting across the room.

- **Customers** walk in without knowing whether a machine is free, queue
  for coins, and have no record of what they paid.
- **Attendants** track which machine is running from memory, settle
  disputes about who paid for what, and reconcile cash at the end of the
  day.
- **Owners** have no data on utilisation or revenue per machine.

## 2. Vision

> *Tap, scan, wash.* LabaLink lets a customer see a free machine, pay from a
> prepaid wallet and start it by scanning a sticker. The attendant sees
> every machine and session update live on one screen.

## 3. Goals and non-goals

### Goals (1.0)

| # | Goal | Measure |
|---|---|---|
| G1 | Customers can start a wash without cash or staff help | ≥ 80% of sessions started via QR scan |
| G2 | Attendants know the state of every machine at a glance | Dashboard reflects changes in < 2 s |
| G3 | Every peso is accounted for | 0 wallet discrepancies between Stripe, wallets and bookings |
| G4 | The system is safe to put real money in | 0 critical/high findings in the security review; all OWASP Top 10 categories addressed |
| G5 | Fast onboarding | New customer goes from install to first wash in < 3 minutes |

### Non-goals (1.0)

- Controlling machines electronically (IoT). Attendants activate sessions
  manually. Planned for 2.0.
- Advance reservations for future time slots.
- Multiple branches or franchise management.
- Cash handling or a point-of-sale system.
- An iOS app or a customer web app.

## 4. Personas

### 👩 Customer — "Ana", 21, university student
- Washes weekly between classes; always on her Android phone.
- Wants to know if a machine is free **before** walking over, and to pay
  without coins.
- Worries about being charged twice or losing money.

### 🧑‍💼 Attendant — "Marco", 34, shop staff
- Manages 8–15 machines alone during a shift, mostly from the counter PC.
- Needs to see what is running, start and finish sessions, and handle
  "the machine didn't start" complaints with refunds.
- Not technical; the UI must be obvious.

### 👔 Owner — "Mrs. Reyes"
- Wants revenue numbers and confidence that staff and customers can't game
  the system. Uses the attendant dashboard.

## 5. User stories

Priority: **M** = must (1.0), **S** = should, **C** = could (later).

### Customer (Android)

| ID | Story | Pri |
|---|---|---|
| C-1 | As a customer, I can register with email and password, or sign in with Google, so I can use the service. | M |
| C-2 | I can see all machines with their type, rate and live status. | M |
| C-3 | I can top up my wallet by card through a secure payment page. | M |
| C-4 | I can start a session by scanning the QR sticker on a free machine; the cost is deducted from my wallet. | M |
| C-5 | I can see my booking history and status. | M |
| C-6 | I can cancel a booking before it is activated and get my money back. | M |
| C-7 | I receive emails for sign-up, session confirmation, completion and top-ups. | M |
| C-8 | I can see my top-up history. | M |
| C-9 | I can log out and know my session is gone. | M |
| C-10 | I can choose the session length. | S |
| C-11 | I get a push notification when my wash is done. | C |

### Attendant (Web)

| ID | Story | Pri |
|---|---|---|
| A-1 | As an attendant, I can sign in with email/password or Google. Customers cannot access the dashboard. | M |
| A-2 | I can add, edit, mark Out of Order and delete machines. | M |
| A-3 | I can view and print a machine's QR sticker, and replace it if it leaks. | M |
| A-4 | I can see all bookings, filter them by status, and Activate, Complete or Cancel (with refund). | M |
| A-5 | I see machines and bookings update live without refreshing. | M |
| A-6 | I can see totals: bookings, active, pending, revenue. | M |
| A-7 | I can see all customer top-ups and their status. | M |
| A-8 | I can add other attendant accounts. | M |
| A-9 | I can export transactions to CSV. | C |
| A-10 | I can see revenue charts per day and machine. | C |

## 6. Functional scope (1.0)

1. **Accounts and access**: email/password, Google, roles (customer,
   attendant), logout everywhere.
2. **Machine management**: CRUD, status, QR sticker generation and rotation.
3. **Scan-to-start sessions**: server-verified QR, server-priced,
   wallet-debited, with a status lifecycle.
4. **Wallet**: Stripe Checkout top-ups, automatic refunds, history.
5. **Live operations**: WebSocket updates of machines and bookings.
6. **Notifications**: transactional email.
7. **Staff management**: attendants create attendants.

Detailed, testable requirements are in the [SRS](SRS.md).

## 7. Business rules

| Rule | Value |
|---|---|
| Session price | `hourly_rate × minutes ÷ 60`, rounded to centavos |
| Session length | 15–240 minutes, default 60 |
| Payment timing | Prepaid: debited when the session starts |
| Refunds | Full refund when cancelled before completion |
| Top-up limits | ₱50 – ₱10,000 per top-up; wallet max ₱50,000 |
| Machine availability | Only `AVAILABLE` machines can be booked; a booking sets it `RUNNING` |
| Attendant accounts | Created only by attendants (or once at install) |

## 8. Non-functional requirements (summary)

- **Security (top priority):** see [SECURITY.md](../SECURITY.md). No card
  data stored or handled; all access authenticated and authorised;
  financial operations atomic and idempotent.
- **Performance:** p95 API latency < 300 ms at 50 concurrent users; live
  updates < 2 s.
- **Availability:** 99% during shop hours.
- **Usability:** a first-time customer completes a wash without help;
  dashboard actions take one or two clicks.
- **Compatibility:** Android 7.0+ (API 24); latest Chrome, Edge and Firefox.
- **Privacy:** collect only name and email; comply with RA 10173 (Data
  Privacy Act).

## 9. Success metrics (first 3 months)

- 70% of active customers top up at least twice.
- Average time from "scan" to "session started" under 5 seconds.
- Fewer than 1% of sessions refunded because of system errors.
- Zero security incidents involving wallets or accounts.

## 10. Release plan

| Release | Contents | Date |
|---|---|---|
| 0.1–0.5 | Prototype: auth, machines, bookings, Stripe, mobile screens | Mar–May 2026 |
| **1.0** | Security overhaul, wallet charging, staff, QR stickers, docs, tests | **2026-10-15** |
| 1.1 | Password reset, email verification, refresh tokens, CI | Next |
| 1.2 | Push notifications, auto-complete, analytics | Later |

## 11. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Customer starts a machine remotely with a photographed QR | Machine taken | Attendant activation step; QR rotation |
| Stripe webhook not delivered | Top-up looks stuck | App-triggered `sync` asks Stripe directly |
| Single-instance limits (in-memory rate limits) | Can't scale out | Redis is on the roadmap; one instance covers one shop easily |
| Attendant forgets to complete sessions | Machine shows RUNNING | Auto-complete is on the roadmap; attendant can edit status |

## 12. Open questions

- Should cancelled ACTIVE sessions be partially refunded by time used?
  (1.0: full refund.)
- Should customers be able to extend a running session?
