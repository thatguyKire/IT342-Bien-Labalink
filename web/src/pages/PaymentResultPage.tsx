import { oauthStyles as s } from '../styles/OAuth2CallbackPage.styles';

/**
 * Public landing page Stripe Checkout returns to after a mobile wallet
 * top-up. It shows a message only: the wallet is credited server-side once
 * Stripe confirms the payment, never because this page was visited.
 */
export default function PaymentResultPage({
  outcome,
}: {
  outcome: 'success' | 'cancel';
}) {
  const success = outcome === 'success';
  return (
    <div className={s.page}>
      <div className={s.card}>
        <div className={s.icon}>{success ? '💜' : '↩️'}</div>
        <h1 className={s.title}>
          {success ? 'Payment received' : 'Payment cancelled'}
        </h1>
        <p className={s.subtitle}>
          {success
            ? 'Return to the LabaLink app. Your wallet updates as soon as '
              + 'the payment is confirmed.'
            : 'No money was taken. You can return to the LabaLink app.'}
        </p>
      </div>
    </div>
  );
}
