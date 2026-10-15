# UI/UX Design

| | |
|---|---|
| **System** | LabaLink 1.0 (Android app + Attendant dashboard) |
| **Date** | 2026-10-15 |
| **Related** | [App Flow](App-Flow.md) · [PRD](PRD.md) |

---

## 1. Design principles

1. **One obvious next step.** Every screen has a single primary action
   (purple, full-width on mobile): *Scan & Start*, *Pay securely with
   Stripe*, *+ Add New Machine*.
2. **Status at a glance.** Machine and booking states always use the same
   colour and label across both apps.
3. **Money is never ambiguous.** Amounts always show the peso sign and
   2 decimals. The price is shown *before* scanning, and refunds are
   spelled out in the confirmation dialog.
4. **Fail kindly.** Errors use the server's plain-language message
   ("Insufficient wallet balance. Please top up first.") in a red banner
   next to the action, never a stack trace or error code.
5. **Safe by design.** Destructive or irreversible actions (delete
   machine, cancel and refund, new QR code) need confirmation, and the
   button names the consequence ("Cancel & refund").

## 2. Brand and design tokens

### 2.1 Colour

| Token | Hex | Use |
|---|---|---|
| `primary` | `#9333EA` | Primary buttons, links, active nav, brand |
| `primaryDark` | `#7C3AED` | Hover / pressed |
| `primaryLight` | `#EDE9FE` | Active nav background, icon chips, "Running" badge |
| `backgroundApp` | `#F5F3FF` | App and page background (lavender) |
| `surface` | `#FFFFFF` | Cards, tables, modals |
| `textDark` | `#1F2937` | Headings, primary text |
| `textGray` | `#6B7280` | Body and secondary text |
| `textLight` | `#9CA3AF` | Captions, placeholders, footer |
| `border` | `#E5E7EB` | Inputs, dividers |
| `success` | `#10B981` | Available, success messages |
| `error` | `#EF4444` | Errors, destructive buttons |

### 2.2 Status colours (shared by both apps)

| State | Background | Text | Label |
|---|---|---|---|
| Machine `AVAILABLE` | green-100 | green-700 | ● Available |
| Machine `RUNNING` | `#EDE9FE` / `#DBEAFE` | `#9333EA` / `#1D4ED8` | ● Running |
| Machine `OUT_OF_ORDER` | `#FEE2E2` | `#B91C1C` | ● Out of Order |
| Booking `PENDING` | gray-100 | gray-600 | ● PENDING |
| Booking `ACTIVE` | blue-100 | blue-700 | ● ACTIVE |
| Booking `COMPLETED` | green-100 | green-700 | ● COMPLETED |
| Booking `CANCELLED` | red-100 | red-700 | ● CANCELLED |
| Payment `SUCCESS` / `PENDING` / `FAILED` | green / yellow / red 100 | 700 | text |

Status is never shown by colour alone: there is always a text label
(WCAG 1.4.1).

### 2.3 Typography

- **Web:** Plus Jakarta Sans (Google Fonts), 400/500/600/700.
- **Android:** system font (Roboto).

| Role | Web | Android |
|---|---|---|
| Page title | 24 px bold | 20 sp bold |
| Section / card title | 16–18 px bold | 16 sp bold |
| Body | 14 px | 14 sp |
| Label / caption | 12–13 px medium | 11–13 sp |
| Stat number | 30 px bold | 28 sp bold |

### 2.4 Shape, spacing and elevation

- Corner radius: inputs and buttons 12 px (`rounded-xl`); cards 16–20 px;
  modals 24 px; badges full pill.
- Spacing scale: 4 · 8 · 12 · 16 · 24 · 32 px. Page gutter 32 px (web) /
  16–20 dp (mobile).
- Elevation: cards `shadow-sm` + 1 px border; modals `shadow-2xl` on a 50%
  black scrim.

### 2.5 Iconography

Emoji icons keep the build light and friendly: 🫧 washer / brand, 💨 dryer,
📋 bookings, 💳 transactions, 👥 staff, ✅ ▶️ ⚠️ status stats. Decorative
icons are marked `aria-hidden`.

## 3. Components

| Component | Spec |
|---|---|
| **Primary button** | Purple fill, white 14–15 px semibold, 48–56 px tall on mobile, `active:scale-95`, disabled 60% opacity with a progress label ("Signing in…") |
| **Secondary button** | White, 1 px gray border, gray text |
| **Destructive button** | Red fill (confirm dialogs) or red outline (inline) |
| **Input** | 52 dp / 48 px tall, gray-50 fill, 1 px border, purple focus ring, leading emoji icon, visible label above |
| **Status badge** | Pill, 12 px medium, coloured per §2.2, leading "●" |
| **Stat card** | White card, uppercase 12 px caption, 30 px number, round icon chip |
| **Data table** (web) | Gray-50 header, uppercase 12 px column titles, row hover lavender, empty and loading rows |
| **Modal** | Centred card, title + explanation + two buttons (cancel left, action right); `role="dialog"`, `aria-modal` |
| **Banner** | Red (error, `role="alert"`) or green (success, `role="status"`) rounded box above the form |
| **Live indicator** | 8 px dot: green pulsing "Live" / gray "Connecting…" |
| **Bottom nav** (Android) | Home · Bookings · Profile (wallet) |
| **Sidebar** (web) | 240 px, logo, Main Menu (Machines, Bookings, Transactions, Staff), profile footer with logout |

## 4. Screens

### 4.1 Android (customer)

| Screen | Purpose | Key elements |
|---|---|---|
| **Login** | Sign in | Logo, email, password (show/hide), *Login*, *Continue with Google* (shown only when configured), link to Register; "session expired" banner |
| **Register** | Create account | Username, email, password with inline rules; server errors shown verbatim |
| **Home** | Find a machine | Greeting + wallet pill, machine count, 2-column grid of machine cards (icon, type, name, status badge, ₱/hr), bottom nav |
| **Machine detail** | Decide and start | Large type illustration, name, rate, status, "Scan the sticker on the machine", primary **Scan & Start · ₱30.00 / 60 min** (disabled + "Not available" when busy) |
| **QR scanner** | Prove presence | Full-screen camera with frame, instruction text, live result line ("Starting machine…", "❌ This QR code does not belong…"), cancel |
| **Bookings** | History | Filter chips (All, Pending, Active, Completed), booking cards (machine, type · duration, start/end, price, status); tap a pending one → cancel dialog with refund amount |
| **Wallet** | Balance and top-up | Balance card, quick amounts ₱50/100/200/500, custom amount, explainer "You'll finish paying on Stripe's secure page", **Pay securely with Stripe**, transaction history, **Log out** |

### 4.2 Web (attendant)

| Screen | Purpose | Key elements |
|---|---|---|
| **Sign in** | Attendant access | Card with email/password, Google button, note "Attendant accounts are created by an existing attendant", error banners for cancelled Google, not-attendant, expired session |
| **Machines** | Manage the floor | Header + *Add New Machine*; search (name or MAC-001) + status filter; 4 stat cards; table (ID, machine, status, **QR Sticker → View/Print**, rate, edit/delete) |
| **Machine modal** | Add / edit | Name, type, hourly rate (₱, step 0.01), status; inline validation |
| **QR sticker modal** | Print / rotate | Machine name, QR code (208 px), "MAC-001 · ₱30.00/hr", warning "Treat this code like a key", *Close* · *New code* · *Print*. Print CSS prints only the sticker |
| **Bookings** | Run sessions | Live indicator, 4 stats (total, awaiting activation, active, completed revenue), status filters, table with Activate / Complete / Cancel; cancel dialog shows the refund |
| **Transactions** | Audit top-ups | 3 stats (count, collected, awaiting confirmation), search, status filter, table (customer, reference, amount, status, date) |
| **Staff** | Manage attendants | Staff table (name, email, sign-in method, since) + "Add Attendant" form with password rules |
| **Payment result** (public) | Stripe return page | 💜 "Payment received — return to the app" / ↩️ "Payment cancelled — no money was taken" |

### 4.3 Wireframes

**Android: machine detail**
```
┌──────────────────────────────┐
│ ←  Machine Details           │
├──────────────────────────────┤
│        ┌──────────────┐      │
│        │     🫧        │      │
│        │Washing Machine│     │
│        └──────────────┘      │
│ Washer 01            WASHER  │
│ ₱ 30.00 / hr                 │
│ Status        ● AVAILABLE    │
│ QR    Scan the sticker on    │
│       the machine            │
├──────────────────────────────┤
│ [ Scan & Start · ₱30.00/60m ]│
└──────────────────────────────┘
```

**Web: bookings**
```
┌────────┬──────────────────────────────────────────────────────┐
│🫧 Laba │ Bookings & Sessions                        ● Live    │
│        ├──────────────────────────────────────────────────────┤
│Machines│ [Total 128] [Awaiting 2] [Active 5] [Revenue ₱4,320] │
│Bookings│ (ALL)(PENDING)(ACTIVE)(COMPLETED)(CANCELLED)          │
│Transact│ ┌────────────────────────────────────────────────┐   │
│Staff   │ │Customer │Machine │Start│End│Dur│Price│Status│Act│ │
│        │ │Ana      │MAC-001 │2:30 │3:30│60 │₱30 │PEND. │[Activate][Cancel]│
│ Marco ↪│ └────────────────────────────────────────────────┘   │
└────────┴──────────────────────────────────────────────────────┘
```

## 5. Interaction details

| Situation | Behaviour |
|---|---|
| Loading | Tables show "Loading…" rows; buttons switch to a progress label and disable to prevent double submits |
| Empty | Friendly empty state with emoji and next step ("Click + Add New Machine to get started") |
| Live update | Dashboard tables refetch silently on WebSocket events; no flashing spinner |
| Session expiry | Any 401 → back to sign-in with "Your session expired" |
| Wrong role | Customer on web → "This dashboard is for shop attendants…"; attendant on Android → "Attendants use the web dashboard" |
| Payment return | Wallet screen shows "Confirming your payment…" → success or failure; a still-pending payment shows a gentle "Waiting for Stripe…" |
| Busy machine | Detail button disabled and relabelled; the server also rejects with 409 |

## 6. Accessibility (WCAG 2.1 AA targets)

- Labels on every form control (`<label htmlFor>`, Android `hint` plus a
  visible label).
- `role="alert"` for errors, `role="status"` for success, `aria-pressed` on
  filter toggles, `aria-current="page"` in the sidebar, `aria-label` on
  icon-only buttons (edit, delete, logout, show password).
- Dialogs: `role="dialog"`, `aria-modal="true"`, `aria-labelledby`.
- Contrast: body text `#374151` / `#6B7280` on white meets AA. Status
  badges pair 700-weight text with 100-weight backgrounds.
- Touch targets ≥ 48 dp on Android. Keyboard reachable with visible focus
  rings on web.
- Status is never colour-only.

## 7. Content style

- Sentence case, friendly and short: "Machine started!", "Top up first".
- Currency: `₱30.00` (web uses `Intl.NumberFormat('en-PH', PHP)`).
- Dates: `Oct 15, 02:30 PM` (en-PH).
- Machine IDs: `MAC-001` (zero-padded).

## 8. Security UX

- Card details are entered only on Stripe's page, and the app says so.
- The QR sticker modal warns attendants to treat the code like a key.
  *New code* explains that the old sticker stops working.
- Login errors never reveal whether an email exists.
- The password show/hide toggle is a real button with an accessible name.
- Logout is explicit on both apps and revokes the session on the server.
