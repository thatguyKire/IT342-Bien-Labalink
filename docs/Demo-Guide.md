# Demo Guide

LabaLink runs entirely on one computer: the API and database locally, the
attendant dashboard in a browser, and the customer app in the Android
emulator. This guide covers running it that way, filling it with sample
data, and capturing screenshots or video. No phone or deployment needed.

## 1. Turn on demo data

Add these lines to `backend/.env`:

```properties
DEMO_DATA=true
DEMO_PASSWORD=pick-a-password-1
```

On startup, an **empty** database gets:

- 8 machines: 4 washers and 4 dryers. Three are running, one is out of
  order, and the rest are available.
- 3 customers with wallet balances, top-up history (one failed top-up
  included) and nine days of completed and cancelled sessions.
- 2 active sessions and 1 pending session that started just now, so the
  dashboard has something to activate and complete.

If the database already has any machines, the seeder does nothing, so it
can't mix sample data into real data. Leaving `DEMO_DATA=true` on is
harmless after the first start.

## 2. Accounts

| Where | Email | Password |
|---|---|---|
| Web dashboard (attendant) | `BOOTSTRAP_ATTENDANT_EMAIL` | `BOOTSTRAP_ATTENDANT_PASSWORD` |
| Android app (customer) | `maria@labalink.test` | `DEMO_PASSWORD` |
| Other customers | `juan@labalink.test`, `ana@labalink.test` | `DEMO_PASSWORD` |

The `.test` domain is reserved, so these addresses can never receive mail.

## 3. Start everything

```powershell
cd backend; .\mvnw.cmd spring-boot:run     # API on http://localhost:8080
cd web; npm run dev                         # dashboard on http://localhost:5173
```

## 4. Run the app in the emulator

1. In Android Studio, open **Device Manager**, click **+**, pick a phone
   (for example Pixel 8) and an API 36 system image.
2. Open `mobiledev/` and press **Run**.

The emulator reaches your computer at `10.0.2.2`, which is the app's
default API address, so `local.properties` is optional.

## 5. Phone-only features

**Scanning a QR code.** Print or open a machine's sticker from the
dashboard (**Machines → View / Print**), then use one of these:

- **Webcam:** in Device Manager, edit the device and set the back camera to
  `Webcam0`. Hold the sticker up to your laptop camera.
- **Virtual scene:** on the emulator toolbar, open **⋯ → Camera** and set
  the wall image to a screenshot of the QR code. Open the scanner, hold
  **Alt**, and use the mouse and **W/A/S/D** to walk to the wall.

**Wallet top-up.** This needs a Stripe test key
(`STRIPE_SECRET_KEY=sk_test_...` in `backend/.env`). Pay with
`4242 4242 4242 4242`, any future date and any CVC. Stripe then redirects
to `localhost:5173`, which doesn't load inside the emulator. That's
expected: press Back, and the app confirms the payment when it returns.
The demo customers already have balances, so you can skip this step.

**Google sign-in** needs OAuth client setup in Google Cloud. Use email and
password for demos.

## 6. Capture screenshots and video

- **App screenshots:** the camera icon on the emulator toolbar.
- **App video:** **⋯ → Record and Playback**. It saves WebM or GIF.
- **Dashboard:** Win+Shift+S for screenshots, Win+Alt+R (Xbox Game Bar) to
  record a window.

The most telling clip shows both apps side by side: start a session on the
phone and watch it appear on the dashboard without a refresh, then
activate and complete it.

## 7. Start over

Sessions marked "now" are timestamped when the data loads. For fresh times
before a recording, delete and recreate the `labalink` database (in pgAdmin
or with `dropdb` / `createdb`), then start the backend again.
