import { useState, useEffect, useCallback } from 'react';
import { loadStripe } from '@stripe/stripe-js';
import {
  Elements,
  CardElement,
  useStripe,
  useElements,
} from '@stripe/react-stripe-js';
import {
  createPaymentIntent,
  confirmPayment,
  getAllPayments,
  getWalletBalance,
} from '../services/paymentService';
import type {
  Payment,
} from '../services/paymentService';
import { walletStyles as s } from
  '../styles/WalletPage.styles';
import SidebarLayout from '../components/SidebarLayout';


// Replace with your Stripe publishable key
const stripePromise = loadStripe(
  'pk_test_51TRXNtGngruIuuDNkOQFUjnF1YZr1jYXm5gilrUayGlLXV0UxEJo2dL8dzOYWOuNNUIqzLqXnhwkwDnArXDxXZnV000orDVAgI'
);

const QUICK_AMOUNTS = [50, 100, 200, 500];

function CheckoutForm({
  userId,
  amount,
  onSuccess,
  onError,
}: {
  userId: number;
  amount: number;
  onSuccess: () => void;
  onError: (msg: string) => void;
}) {
  const stripe = useStripe();
  const elements = useElements();
  const [processing, setProcessing] = 
    useState(false);

  const handleSubmit = async () => {
    if (!stripe || !elements) return;

    const cardElement = 
      elements.getElement(CardElement);
    if (!cardElement) return;

    try {
      setProcessing(true);

      // Step 1 — Create payment intent
      const paymentData = 
        await createPaymentIntent(userId, amount);

      if (!paymentData.clientSecret) {
        onError('Failed to create payment');
        return;
      }

      // Step 2 — Confirm card payment
      const { error, paymentIntent } =
        await stripe.confirmCardPayment(
          paymentData.clientSecret,
          {
            payment_method: {
              card: cardElement,
            },
          }
        );

      if (error) {
        onError(error.message || 
          'Payment failed');
        return;
      }

      if (paymentIntent?.status === 'succeeded') {
        // Step 3 — Confirm on backend
        await confirmPayment(paymentIntent.id);
        onSuccess();
      }
    } catch (err) {
      console.error('Payment error', err);
      onError('Payment failed. Try again.');
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div>
      <div className="border border-gray-200 
                      rounded-xl p-4 bg-gray-50 
                      mb-4">
        <CardElement
          options={{
            style: {
              base: {
                fontSize: '16px',
                color: '#374151',
                '::placeholder': {
                  color: '#9CA3AF',
                },
              },
            },
          }}
        />
      </div>
      <button
        onClick={handleSubmit}
        disabled={
          !stripe || processing || amount <= 0}
        className={s.payButton}
      >
        {processing
          ? 'Processing...'
          : `Top Up ₱${amount.toFixed(2)}`}
      </button>
      <p className={s.poweredBy}>
        🔒 Powered by Stripe • Sandbox Mode
      </p>
    </div>
  );
}

export default function WalletPage() {
  const [amount, setAmount] = useState(100);
  const [customAmount, setCustomAmount] = 
    useState('');
  const [walletBalance, setWalletBalance] = 
    useState(0);
  const [payments, setPayments] = 
    useState<Payment[]>([]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(true);

  const userId = Number(
    localStorage.getItem('userId') || 1);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [balance, paymentData] = 
        await Promise.all([
          getWalletBalance(userId),
          getAllPayments(),
        ]);
      setWalletBalance(balance);
      setPayments(paymentData);
    } catch (err) {
      console.error('Failed to load wallet', err);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSuccess = async () => {
    setSuccess(
      `Successfully topped up ₱${amount.toFixed(2)}!`
    );
    setError('');
    await fetchData();
    setTimeout(() => setSuccess(''), 5000);
  };

  const handleError = (msg: string) => {
    setError(msg);
    setSuccess('');
  };

  const handleCustomAmount = (
      value: string) => {
    setCustomAmount(value);
    const parsed = parseFloat(value);
    if (!isNaN(parsed) && parsed > 0) {
      setAmount(parsed);
    }
  };

  const getStatusBadge = (
      status: Payment['status']) => {
    switch (status) {
      case 'SUCCESS':
        return <span className={s.badgeSuccess}>
          SUCCESS
        </span>;
      case 'FAILED':
        return <span className={s.badgeFailed}>
          FAILED
        </span>;
      default:
        return <span className={s.badgePending}>
          PENDING
        </span>;
    }
  };

  return (
      <SidebarLayout>
    <div className={s.page}>

      {/* Header */}
      <div className={s.header}>
        <div>
          <h1 className={s.headerTitle}>
            Wallet Top-Up
          </h1>
          <p className={s.headerSubtitle}>
            Add funds to your LabaLink wallet
          </p>
        </div>
      </div>

      <div className={s.content}>

        {/* Balance Card */}
        <div className={s.balanceCard}>
          <div className={s.balanceLabel}>
            CURRENT BALANCE
          </div>
          <div className={s.balanceAmount}>
            ₱ {loading
              ? '...'
              : walletBalance.toFixed(2)}
          </div>
          <div className={s.balanceSubtext}>
            LabaLink Digital Wallet
          </div>
        </div>

        {/* Top Up Card */}
        <div className={s.topUpCard}>
          <h2 className={s.topUpTitle}>
            Add Funds
          </h2>

          {error && (
            <div className={s.errorBox}>
              {error}
            </div>
          )}
          {success && (
            <div className={s.successBox}>
              {success}
            </div>
          )}

          {/* Quick amounts */}
          <div className="mb-4">
            <label className={s.inputLabel}>
              Select Amount
            </label>
            <div className={s.quickAmounts}>
              {QUICK_AMOUNTS.map((a) => (
                <div
                  key={a}
                  onClick={() => {
                    setAmount(a);
                    setCustomAmount('');
                  }}
                  className={
                    amount === a && !customAmount
                      ? s.quickAmountBtnActive
                      : s.quickAmountBtn
                  }
                >
                  ₱{a}
                </div>
              ))}
            </div>
          </div>

          {/* Custom amount */}
          <div className="mb-4">
            <label className={s.inputLabel}>
              Or Enter Custom Amount
            </label>
            <input
              type="number"
              placeholder="Enter amount (min ₱50)"
              value={customAmount}
              onChange={(e) =>
                handleCustomAmount(e.target.value)}
              className={s.input}
              min="50"
            />
          </div>

          {/* Stripe Card Form */}
          <Elements stripe={stripePromise}>
            <CheckoutForm
              userId={userId}
              amount={amount}
              onSuccess={handleSuccess}
              onError={handleError}
            />
          </Elements>
        </div>

        {/* Transaction History */}
        <div className={s.historyCard}>
          <div className={s.historyTitle}>
            Transaction History
          </div>
          {payments.length === 0 ? (
            <div className="text-center 
                            py-8 text-gray-400 
                            text-sm">
              No transactions yet
            </div>
          ) : (
            payments.slice(0, 10).map((p) => (
              <div key={p.id}
                   className={s.historyItem}>
                <div>
                  <div className={s.historyAmount}>
                    ₱{p.amount.toFixed(2)}
                  </div>
                  <div className={s.historyRef}>
                    {p.providerReference}
                  </div>
                </div>
                <div className="text-right">
                  {getStatusBadge(p.status)}
                  <div className={s.historyDate}>
                    {new Date(p.createdAt)
                      .toLocaleDateString('en-PH')}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

      </div>
    </div>
    </SidebarLayout>
  );
}