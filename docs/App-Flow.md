# App Flow

How users move through LabaLink, screen by screen. For screen layouts see
[UI-UX-Design.md](UI-UX-Design.md). For API details see [TRD](TRD.md).

---

## 1. Customer app (Android)

### 1.1 Navigation map

```mermaid
flowchart TD
    Launch([App launch]) --> HasSession{Valid stored session?}
    HasSession -- yes --> Home
    HasSession -- no --> Login

    Login -->|"Register"| Register
    Register -->|success| Login
    Login -->|"email + password / Google"| RoleCheck{role == CUSTOMER?}
    RoleCheck -- no --> LoginErr[/"Attendants use the web dashboard"/] --> Login
    RoleCheck -- yes --> Home

    Home -->|tap machine| Detail[Machine detail]
    Detail -->|"Scan & Start"| Scanner[QR scanner]
    Scanner -->|success| Detail2[Toast: Booking started] --> Home
    Scanner -->|error| Scanner

    Home -->|nav: Bookings| Bookings
    Bookings -->|nav: Home / back| Home
    Bookings -->|tap PENDING| CancelDlg{Cancel booking?}
    CancelDlg -->|yes| Bookings

    Home -->|nav: Profile| Wallet
    Wallet -->|"Pay securely with Stripe"| Stripe[[Stripe Checkout in browser]]
    Stripe -->|return to app| Wallet
    Wallet -->|Log out| Login

    AnyScreen([Any API call returns 401]) -.-> LoginExpired[Login: Your session expired]
```

### 1.2 First wash (happy path)

```mermaid
journey
    title Ana's first wash
    section Get started
      Install and open app: 4: Customer
      Register or sign in with Google: 4: Customer
    section Fund wallet
      Open Profile / Wallet: 5: Customer
      Choose ₱100, tap Pay with Stripe: 4: Customer
      Pay on Stripe page: 3: Customer, Stripe
      Return, balance shows ₱100: 5: Customer
    section Wash
      Pick an Available washer: 5: Customer
      Tap Scan & Start, scan sticker: 5: Customer
      See "Machine started": 5: Customer
      Attendant activates: 4: Attendant
      Receive completion email: 5: Customer
```

### 1.3 Scan & Start: detailed flow

```mermaid
flowchart TD
    A[Machine detail] --> B{Status AVAILABLE?}
    B -- no --> B1[Button disabled: Not available]
    B -- yes --> C[Tap Scan & Start]
    C --> D{Camera permission?}
    D -- denied --> D1[Toast: camera required] --> A
    D -- granted --> E[Scan sticker]
    E --> F{Looks like a LabaLink code?<br/>non-empty, ≤ 64 chars}
    F -- no --> F1[❌ Not a LabaLink QR code] --> E
    F -- yes --> G[POST /api/bookings<br/>machineId, qrToken, durationMinutes]
    G --> H{Response}
    H -- 201 --> I[✅ Machine started! Booking number shown] --> J[Back to Home]
    H -- 400 --> K[❌ QR does not belong to this machine] --> E
    H -- 402 --> L[❌ Insufficient balance. Top up first] --> E
    H -- 409 --> M[❌ Machine is not available] --> E
    H -- network --> N[❌ Cannot connect to server] --> E
```

### 1.4 Wallet top-up flow

```mermaid
flowchart TD
    W[Wallet screen] --> A{Amount 50–10,000?}
    A -- no --> A1[Error: enter ₱50–₱10,000]
    A -- yes --> B[POST /api/payments/checkout]
    B -->|checkoutUrl| C{URL is https://*.stripe.com?}
    C -- no --> C1[Error: unexpected payment page]
    C -- yes --> D[Save pendingPaymentId<br/>open browser]
    D --> E[[Customer pays or cancels on Stripe]]
    E --> F[Returns to app → onResume]
    F --> G[POST /api/payments/id/sync]
    G --> H{status}
    H -- SUCCESS --> H1[Wallet topped up by ₱X ✓]
    H -- FAILED --> H2[Payment not completed, no money taken]
    H -- PENDING --> H3[Waiting for Stripe to confirm…]
    H1 & H2 & H3 --> R[Reload balance and history]
```

### 1.5 Google sign-in (Android)

1. The *Continue with Google* button is visible only if
   `labalink.googleWebClientId` is configured.
2. Account picker → Google returns an **ID token**.
3. App → `POST /api/auth/google {idToken}` → the server verifies it with
   Google.
4. On success the token is encrypted with the Keystore key and saved →
   Home.

## 2. Attendant dashboard (Web)

### 2.1 Navigation map

```mermaid
flowchart TD
    Visit([Open dashboard URL]) --> Sess{Valid attendant session?}
    Sess -- no --> Login[Sign in]
    Sess -- yes --> Machines

    Login -->|email + password| Check{role}
    Login -->|Continue with Google| Google[[Google consent]]
    Google --> Callback["/oauth2/callback<br/>(one-time code in URL fragment)"]
    Callback -->|exchange code| Check
    Check -- ATTENDANT --> Machines
    Check -- CUSTOMER --> NotAtt[/"This dashboard is for shop attendants"/] --> Login

    subgraph Sidebar
        Machines[Machines]
        Bookings[Bookings]
        Transactions[Transactions]
        Staff[Staff]
    end
    Machines <--> Bookings <--> Transactions <--> Staff

    Machines --> AddEdit[Add / Edit modal]
    Machines --> QR[QR sticker modal → Print / New code]
    Machines --> Del[Delete confirm]
    Bookings --> Act[Activate / Complete]
    Bookings --> Cancel[Cancel & refund confirm]
    Staff --> NewStaff[Add attendant form]

    Sidebar -->|Log out| Login
    Any([401 from API]) -.-> Expired[Sign in: session expired]
```

### 2.2 Daily operations flow

```mermaid
sequenceDiagram
    actor M as Attendant
    participant D as Dashboard
    participant API
    actor C as Customer

    M->>D: Sign in (opens on Machines)
    C->>API: Scan & Start (Android)
    API-->>D: live BOOKING_CREATED + MACHINE_UPDATED
    D->>D: Bookings table shows new PENDING row
    M->>D: Load laundry, click Activate
    D->>API: PATCH status ACTIVE
    API-->>C: Email "Booking confirmed"
    Note over M,C: …wash runs…
    M->>D: Click Complete
    D->>API: PATCH status COMPLETED
    API-->>D: machine AVAILABLE (live)
    API-->>C: Email "Session completed"
```

### 2.3 Machine setup flow

1. **Machines → + Add New Machine** → name, type, rate, status → *Add
   Machine*.
2. The QR sticker modal opens automatically → **Print** → stick it on the
   machine.
3. If a sticker is damaged or photographed: **View / Print → New code** →
   confirm → reprint. The old code stops working immediately.
4. To retire a machine that has history, set it **Out of Order** (delete
   is blocked with an explanation).

### 2.4 Staff onboarding flow

1. First install: the server creates the attendant from
   `BOOTSTRAP_ATTENDANT_*`.
2. That attendant signs in → **Staff** → *Add Attendant* (name, email,
   temporary password) → shares the password privately.
3. The new attendant signs in with that password, or with Google if their
   Google email matches.

## 3. Cross-cutting flows

### 3.1 Session lifecycle

```mermaid
stateDiagram-v2
    [*] --> SignedOut
    SignedOut --> SignedIn: login / Google (JWT, 60 min)
    SignedIn --> SignedIn: API calls (Bearer)
    SignedIn --> SignedOut: Log out (server revokes all tokens)
    SignedIn --> SignedOut: token expires / 401
    SignedIn --> SignedOut: tab closed (web, sessionStorage)
```

### 3.2 Error handling pattern (both apps)

| Server says | App shows |
|---|---|
| 400 / 402 / 409 with `message` | That message in a red banner or toast |
| 401 (signed in) | Session cleared → login with "session expired" |
| 403 | "Access denied" (should not happen through normal UI) |
| 429 | "Too many attempts. Try again in 15 minutes." / "Please slow down." |
| Network error | "Cannot connect to server." |
