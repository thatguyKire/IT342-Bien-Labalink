# LabaLink Privacy Policy

**Version 1.0 · Effective 18 October 2026**

> **LabaLink is a student project for the IT342 course, not a commercial
> service.** It is used for development, testing and class demonstrations.
> No real money is charged.

This policy explains what personal data LabaLink collects, why, and the
rights you have under the **Data Privacy Act of 2012 (Republic Act No.
10173)**. It covers the LabaLink Android app, the attendant web dashboard
and the LabaLink server (together, "LabaLink"), which are built and run
by the LabaLink project developer ("we", "us").

## 1. What we collect and why

| Data | When we get it | Why |
|---|---|---|
| **Account:** username, email address, password, sign-in method (email or Google), account role and status, date created | When you register or sign in | To create your account and let you sign in |
| **Google sign-in:** your Google email address, whether Google has verified it, and your name | When you choose "Continue with Google" | To create or find your account without a separate password |
| **Test top-ups:** amount, status, dates, and Stripe test-mode reference numbers | When you make a test top-up | To show how the wallet works |
| **Demo sessions:** machine, start and end time, length, price and status | When you start, cancel or finish a session | To show how bookings work |
| **Email log:** your email address, subject, email type and whether it was delivered | When the app sends you an email | To check that emails are delivered |
| **Security data:** your IP address and failed sign-in counts | Each time you use LabaLink | To block password-guessing and abuse |

We process this data because it is needed to run the service you signed
up for and to keep it secure.

**Your password** is never stored as written. We keep only a one-way
BCrypt hash, so nobody, including us, can read it.

**Card details.** Payments run in Stripe's test mode. Card details are
entered only on Stripe's page, never on LabaLink, and must be Stripe test
cards. Please never enter a real card number.

**Camera.** The app uses your camera only to read a machine's QR code.
The image is processed on your phone and is not saved or sent anywhere.
Only the text of the scanned code is sent to the server.

**What we do not collect.** We do not collect your location, contacts,
photos, government ID numbers or any sensitive personal information.
LabaLink has no advertising, no analytics or tracking tools, and no
tracking cookies. We never sell or rent your data.

## 2. Data stored on your device

- **Android app:** your sign-in token is stored on your phone, encrypted
  with a key kept in the Android Keystore. It is deleted when you sign out
  and is excluded from device backups.
- **Web dashboard (staff only):** the sign-in token is kept in the
  browser's session storage and is removed when you close the tab, sign
  out, or it expires. A temporary cookie is used only while signing in
  with Google and is deleted right after.

## 3. Emails

LabaLink sends only emails that are part of the demo: a welcome email,
session confirmation and completion, and test top-up receipts. It never
sends marketing emails.

## 4. Who can see your data

| Who | What | Why |
|---|---|---|
| **Attendant accounts** in the demo | Your username, email, sessions and test top-ups | To demonstrate the attendant dashboard |
| **Stripe** (test mode) | Test top-up amount, your email address, LabaLink reference numbers | To process test payments |
| **Google** | Only what is needed to confirm a Google sign-in, plus outgoing emails if Gmail is used to send them | Sign-in and email delivery |
| **Course instructors and evaluators** | What appears on screen during a demo | To grade the project |

Stripe and Google may process data outside the Philippines under their
own privacy policies. The LabaLink server and database run on the
developer's own computer or a test server used for the project.

## 5. How long we keep data

Data is kept only while the project is active. Demo data may be wiped at
any time, and **all personal data is deleted when the project ends**. You
can also ask us to delete your account sooner (see section 8).

## 6. How we protect your data

LabaLink uses encrypted connections in release builds, password hashing,
role-based access, checks that stop users from reading each other's data,
rate limiting and account lockout, and encrypted token storage on phones.
See our [security policy](../../SECURITY.md) for details.

No system is perfectly secure. Please use a password you don't use
anywhere else. If a breach affecting your data ever happens, we will tell
you as soon as we can.

## 7. Your rights

Under the Data Privacy Act you have the right to be informed, to access
your data, to correct it, to object to its processing, to have it erased
or blocked, to get a copy of it, to claim damages, and to file a complaint
with the National Privacy Commission
([privacy.gov.ph](https://privacy.gov.ph)).

You can see your profile, sessions and test top-ups in the app at any
time.

<a id="delete-your-account"></a>
## 8. Requests and account deletion

To ask for a copy of your data, a correction, or deletion of your
account:

1. Open an issue titled **"Privacy request"** in the
   [GitHub repository](https://github.com/thatguyKire/IT342-Bien-Labalink/issues).
   **Do not put your email address or other personal data in the
   issue.**
2. The developer will reply to arrange the request privately and confirm
   that the account is yours.
3. Deletion is completed within 30 days. You do not need to have the app
   installed to ask.

## 9. Children

LabaLink is meant for people aged 18 and over, or younger users whose
parent or guardian has agreed to the Terms for them.

## 10. Changes to this policy

If we change what LabaLink collects, we will update this policy. The
version and date at the top show when it last changed.
