# If LabaLink Ever Goes Live

LabaLink is a class project. It runs on Stripe **test mode** only, and the
server refuses to start with a live Stripe key (`sk_live_` / `rk_live_`).
**Nothing on this page is needed for the course.**

This list is for later, if the project ever turns into a real service
that takes real money from real customers in the Philippines. It was
written by developers and is not legal advice. Items marked ⚖️ need a
lawyer's confirmation.

## 1. Business and payments

- [ ] Register the business (DTI or SEC), get a business permit, register
      with the BIR, and issue proper receipts.
- [ ] Pick a payment provider that serves Philippine businesses. Stripe
      generally does not open accounts for them. PayMongo, Xendit or Maya
      Business are options. Payments sit behind the `PaymentGateway`
      interface, so switching means writing one adapter class.
- [ ] Remove the live-key guard in `AppProperties.Stripe` only once all
      of this list is done.
- [ ] ⚖️ Confirm the wallet does not need a BSP licence. Stored-value
      wallets can count as e-money under BSP Circular 1166 (2023). A
      closed-loop wallet usable only at your own shop is treated
      differently.

## 2. Legal documents and privacy (RA 10173)

- [ ] Rewrite the [Terms](Terms-of-Service.md) and
      [Privacy Policy](Privacy-Policy.md) for a real business: real
      operator details, wallet refund rules, laundry damage policy, and
      retention periods for tax records. Have a lawyer review them.
- [ ] Appoint a Data Protection Officer and publish their contact.
- [ ] ⚖️ Check whether NPC registration is required (NPC Circular
      2022-04). Small businesses usually fall below the threshold.
- [ ] Have a breach-response plan (NPC and users notified within 72
      hours).
- [ ] Build an in-app account deletion tool. Google Play requires one for
      apps that let users create accounts.

## 3. App stores and Google sign-in

- [ ] Host the Terms and Privacy Policy at stable public URLs and set
      `labalink.termsUrl` / `labalink.privacyUrl` (Android) and
      `VITE_TERMS_URL` / `VITE_PRIVACY_URL` (web).
- [ ] Fill in Google Play's Data safety form and give the account
      deletion link.
- [ ] Add the policy URLs to the Google OAuth consent screen.

## 4. Security

- [ ] HTTPS everywhere, strong unique production secrets, changed
      bootstrap password, encrypted backups with a tested restore.
- [ ] Ship the 1.1 roadmap items (password reset, email verification, CI
      with dependency scanning). Consider an independent penetration
      test.
