# UI/UX Design

| | |
|---|---|
| **System** | LabaLink 1.0 (Android app + Attendant dashboard) |
| **Date** | 2026-11-12 (redesign) |
| **Related** | [App Flow](App-Flow.md) · [PRD](PRD.md) |

---

## 1. Design principles

1. **One obvious next step.** Every screen has a single primary action
   (ocean-blue, full-width on mobile): *Scan and pay*, *Pay securely with
   Stripe*, *Add machine*.
2. **Status at a glance.** Machine, booking and payment states use the
   same label and colour in both apps (one mapping each:
   `ui/Status.kt` on Android, `lib/status.ts` on the web).
3. **Money is never ambiguous.** Amounts always show the peso sign and
   2 decimals in tabular figures. The price is shown *before* scanning, and
   refunds are spelled out in the confirmation dialog.
4. **Fail kindly.** Errors use the server's plain-language message
   ("Insufficient wallet balance. Please top up first.") in a red banner
   next to the action, never a stack trace or error code.
5. **Safe by design.** Destructive or irreversible actions (delete
   machine, cancel and refund, new QR code) need confirmation, and the
   button names the consequence ("Cancel and refund").

## 2. Brand and design tokens

The look is "clean water": deep and ocean blues with an aqua accent on
cool, near-white surfaces. Android defines the tokens in
`res/values/colors.xml`, `type.xml`, `styles.xml` and `themes.xml`; the web
defines the same values as Tailwind theme tokens in `src/index.css`.

### 2.1 Colour

| Token (Android / web) | Hex | Use |
|---|---|---|
| `brand_deep` / `brand-900` | `#0B3B5C` | Web sidebar, start of the brand gradient |
| `brand` / `brand-600` | `#0E6BA8` | Primary buttons, links, active nav |
| `brand_pressed` / `brand-700` | `#0B5689` | Hover / pressed |
| `brand_tint` / `brand-50` | `#E3F1FA` | Icon tiles, nav indicator, wallet pill |
| `brand_aqua` / `aqua-400` | `#22C3C9` | Accent: live dot, scan frame, gradient end |
| `brand_aqua_deep` / `aqua-700` | `#0B7C82` | Aqua text on light surfaces |
| `ink` / `gray-900` | `#0F2233` | Headings, primary text |
| `ink_secondary` / `gray-600` | `#4E6275` | Body and secondary text |
| `ink_tertiary` / `gray-400` | `#8597A8` | Captions, placeholders |
| `background` / `gray-50` | `#F3F7FA` | App and page background |
| `surface` | `#FFFFFF` | Cards, tables, dialogs |
| `outline` / `gray-200`, `divider` / `gray-100` | `#D6E2EB`, `#E8EFF4` | Inputs, hairline card borders, dividers |

On the web, Tailwind's `gray` scale is redefined to these cool neutrals, so
existing `gray-*` classes follow the palette.

The brand gradient (`brand_deep` → `brand` → `aqua`, 135°) is used only for
hero surfaces: the Android home and wallet cards, the auth headers, the
machine illustration and the web sign-in panel.

### 2.2 Status colours (shared by both apps)

| Status | Label | Tone (text on tint) |
|---|---|---|
| Machine `AVAILABLE` | Available | success `#0A7A55` on `#DCF5EA` |
| Machine `RUNNING` | Running | info `#1559B3` on `#DFEBFB` |
| Machine `OUT_OF_ORDER` | Out of order | danger `#B42318` on `#FDE6E4` |
| Booking `PENDING` | Pending | warning `#A35A00` on `#FDF0D5` |
| Booking `ACTIVE` | Active | info |
| Booking `COMPLETED` | Completed | success |
| Booking `CANCELLED` | Cancelled | neutral `#4E6275` on `#EAF1F6` |
| Payment `SUCCESS` / `PENDING` / `FAILED` | Paid / Pending / Failed | success / warning / danger |

Status is never shown by colour alone: there is always a text label
(WCAG 1.4.1).

### 2.3 Typography

**Plus Jakarta Sans** on both platforms (SIL Open Font License). Android
bundles the static weights in `res/font/`; the web self-hosts the variable
font through `@fontsource-variable/plus-jakarta-sans`, so no request goes to
Google Fonts. Money, times and counts use tabular figures.

| Role | Android | Web |
|---|---|---|
| Display (hero, brand) | 30 sp ExtraBold | 30–42 px ExtraBold |
| Headline (screen title) | 22 sp Bold | 24 px Bold |
| Title (section, card) | 17 sp Bold | 18 px Bold |
| Body | 15 sp Regular | 14 px Regular |
| Label / small title | 13–15 sp SemiBold | 14 px SemiBold |
| Caption | 12 sp Medium | 12 px Medium |
| Amount | 34 sp ExtraBold, tabular | 30 px ExtraBold, tabular |

Labels are sentence case; there are no all-caps eyebrows. On Android each
text style points at one weight file, so weights render identically on
every Android version.

### 2.4 Shape, spacing and elevation

- Corner radius: badges full pill; inputs 14 dp / 12 px; buttons 16 dp /
  12 px; cards 20 dp / 16 px; hero cards and dialogs 24 dp / 24 px.
- Spacing scale: 4 · 8 · 12 · 16 · 20 · 24 · 32. Page gutter 16 dp
  (Android) / 32 px (web).
- Elevation: flat. Cards are white with a 1 px hairline border; only the
  auth logo tile and dialogs cast a shadow. Dialogs sit on a blurred
  `brand-900` 50% scrim.

### 2.5 Iconography

[Lucide](https://lucide.dev) icons (ISC licence) on both platforms: 24 px
grid, 2 px round strokes. The web uses `lucide-react`; Android uses vector
drawables generated from the same SVGs by
`mobiledev/scripts/lucide-to-vector.mjs`. Washers use `washing-machine`
(brand blue tile), dryers use `wind` (aqua tile). The LabaLink mark is the
`droplets` icon on the brand gradient (`logo_mark.xml`, `LogoMark.tsx`, the
launcher icon and the favicon). Decorative icons are hidden from screen
readers.

### 2.6 Motion

Motion explains what changed; it is never decoration for its own sake.
Durations follow 100–150 ms for presses, 200–400 ms for entrances and
screens, and never more than 600 ms for anything the user waits on. Only
opacity and transforms are animated.

| Moment | Android | Web |
|---|---|---|
| Screen change | Slide + fade (300 ms); tabs cross-fade (200 ms) | — |
| First load of a list | Cards rise in, 35 ms apart | Table rows rise in, 35 ms apart |
| Press | Cards, chips and buttons shrink 3% | Buttons shrink 2% |
| Live feed | "Live" dot with an expanding halo | Same; a booking whose status just changed flashes aqua |
| Money | Wallet balances count up to the new value | — |
| Dialogs and messages | Banners rise in | Dialogs pop in over a fading scrim; banners rise in |
| QR scanner | Aqua line sweeps the scan frame; success draws a check mark | — |

Everything respects the system setting: Android checks the animator
duration scale ("Remove animations"); the web disables animations under
`prefers-reduced-motion`.

## 3. Components

| Component | Spec |
|---|---|
| **Primary button** | Brand fill, white 15 sp/14 px bold, 56 dp / 44 px tall, optional leading icon, press scale, disabled state shows a progress label ("Signing in…") |
| **Secondary button** | White, 1 px outline, ink text |
| **Destructive button** | Red outline (Android "Log out", web inline) or red fill (confirm dialogs) |
| **Input** | Outlined, 14 dp radius, leading Lucide icon, label as hint (Android) or visible label (web), brand focus ring, password visibility toggle |
| **Status badge** | Pill, 11–12 bold, tone per §2.2 |
| **Icon tile** | 40–44 dp rounded square, tinted per machine type or status |
| **Stat card** (web) | White card, sentence-case label, 30 px tabular number, tinted icon tile |
| **Data table** (web) | Gray-50 header with sentence-case column titles, hover row, loading and empty rows |
| **Filter chips** | Single choice; selected chip is brand-filled |
| **Dialog** | Title, explanation, two buttons (keep left, action right); `role="dialog"`, `aria-modal` |
| **Banner** | Red (error, `role="alert"`) or green (success, `role="status"`) with a leading icon |
| **Live indicator** | Aqua dot with halo, "Live" / gray "Connecting…" |
| **Bottom nav** (Android) | Material 3: Home, Bookings, Wallet; pill indicator in `brand_tint` |
| **Sidebar** (web) | 256 px, deep-blue `brand-900`, logo, nav with aqua active bar, profile card with log out |

## 4. Screens

### 4.1 Android (customer)

| Screen | Purpose | Key elements |
|---|---|---|
| **Login** | Sign in | Gradient header with logo, floating form card (email, password with toggle), *Sign in*, *Continue with Google* (shown only when configured), legal notice, link to Register; "session expired" banner |
| **Register** | Create account | Same shell; name, email, password with helper text, age/terms checkbox, *Create account* |
| **Home** | Find a machine | Top bar with logo and wallet pill (opens Wallet), gradient greeting card with live available/running counts, "Machines" header with live dot, 2-column machine cards, bottom nav |
| **Machine detail** | Decide and start | Gradient illustration, name, rate, type and status badges, "How it works" (scan, pay from wallet with the real price, attendant starts it), **Scan and pay ₱ 60.00** (disabled and relabelled when the machine is busy) |
| **QR scanner** | Prove presence | Full-bleed camera, aqua scan frame with sweeping line, result card (working / success check / error), bottom panel with instructions and *Cancel* |
| **Bookings** | History | Filter chips (All, Pending, Active, Completed), booking cards (machine tile, type and duration, start/end, total, status); pending cards show "Tap to cancel and get a refund" |
| **Wallet** | Balance and top-up | Gradient balance card (counts up), amount chips ₱50/100/200/500 or a custom amount, Stripe explainer, **Pay securely with Stripe**, "Stripe test mode" note, recent activity, **Log out** |

### 4.2 Web (attendant)

| Screen | Purpose | Key elements |
|---|---|---|
| **Sign in** | Attendant access | Brand panel (logo, headline, three facts) beside the form card: email, password with toggle, Google button, note that attendants are created by an existing attendant, error banners |
| **Machines** | Manage the floor | Header with live indicator and *Add machine*; search + status filter; 4 stat cards; table (ID, machine with type icon, status, **QR sticker → View / Print**, rate, edit/delete) |
| **Machine dialog** | Add / edit | Name, type, hourly rate (₱, step 0.01), status; inline validation |
| **QR sticker dialog** | Print / rotate | Machine name, QR code, ID and rate, warning "Treat this code like a key", *Close* · *New code* · *Print*. Print CSS prints only the sticker |
| **Bookings** | Run sessions | Live indicator, 4 stats (all, waiting to start, running now, revenue), status chips, table with Activate / Complete / Cancel; changed rows flash; cancel dialog shows the refund |
| **Transactions** | Audit top-ups | 3 stats (top-ups, collected, waiting for Stripe), search, status chips, table (customer, reference, amount, status, date) |
| **Staff** | Manage attendants | Staff table (name, email, sign-in method, since) + "Add an attendant" form with password rules |
| **Payment result** (public) | Stripe return page | Check icon "Payment received, return to the app" / undo icon "Payment cancelled, no money was taken" |

### 4.3 Wireframes

**Android: machine detail**
```
┌──────────────────────────────┐
│ ←          Machine           │
├──────────────────────────────┤
│ ╭──────────────────────────╮ │
│ │   [washer icon]   ░drops │ │  brand gradient
│ │   Washing machine        │ │
│ ╰──────────────────────────╯ │
│ Washer 1          ₱ 60.00/hr │
│ (Washer) (Available)         │
│ How it works                 │
│ ┌──────────────────────────┐ │
│ │ [scan]  Scan the sticker │ │
│ │ [wallet] Pay from wallet │ │
│ │ [spark] Attendant starts │ │
│ └──────────────────────────┘ │
├──────────────────────────────┤
│ [ ⌗  Scan and pay ₱ 60.00  ] │
└──────────────────────────────┘
```

**Web: bookings**
```
┌──────────┬────────────────────────────────────────────────────┐
│ [logo]   │ Bookings                                  ● Live   │
│ LabaLink │ Activate sessions when customers load…             │
│          ├────────────────────────────────────────────────────┤
│▌Machines │ [All 13] [Waiting 1] [Running 2] [Revenue ₱492.50] │
│ Bookings │ (All)(Pending)(Active)(Completed)(Cancelled)       │
│ Transact.│ ┌────────────────────────────────────────────────┐ │
│ Staff    │ │Customer│Machine│Start│End│Dur│Price│Status│Act.│ │
│          │ │Ana Cruz│Washer 4│…   │…  │60 │₱80 │Pending│[Activate][Cancel]│
│ [M] Admin│ └────────────────────────────────────────────────┘ │
└──────────┴────────────────────────────────────────────────────┘
  deep-blue sidebar
```

## 5. Interaction details

| Situation | Behaviour |
|---|---|
| Loading | Tables show "Loading…" rows; buttons switch to a progress label and disable to prevent double submits |
| Empty | Icon tile, a plain statement and the next step ("Use Add machine to create one") |
| Live update | Dashboard tables refetch silently on WebSocket events; changed booking rows flash aqua instead of a spinner |
| Session expiry | Any 401 → back to sign-in with "Your session expired" |
| Wrong role | Customer on web → "This dashboard is for shop attendants…"; attendant on Android → "Attendants use the LabaLink web dashboard" |
| Payment return | Wallet screen shows "Confirming your payment…" → success or failure; a still-pending payment shows "Waiting for Stripe…" |
| Busy machine | Detail button disabled and relabelled ("Running right now"); the server also rejects with 409 |
| Edge to edge | Android draws behind the status and navigation bars; bars and forms are padded so nothing sits under them or the keyboard |

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
