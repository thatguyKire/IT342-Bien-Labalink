import { useCallback, useEffect, useMemo, useState } from 'react';
import SidebarLayout from '../components/SidebarLayout';
import { errorMessage } from '../lib/api';
import { formatDateTime, peso } from '../lib/format';
import { getAllPayments } from '../services/paymentService';
import type { Payment, PaymentStatus } from '../services/paymentService';

type Filter = 'ALL' | PaymentStatus;
const FILTERS: Filter[] = ['ALL', 'SUCCESS', 'PENDING', 'FAILED'];

const BADGE: Record<PaymentStatus, string> = {
  SUCCESS: 'bg-green-100 text-green-700 border-green-200',
  PENDING: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  FAILED: 'bg-red-100 text-red-600 border-red-200',
};

/** Read-only view of every customer wallet top-up (attendants only). */
export default function TransactionsPage() {
  const [payments, setPayments] = useState<Payment[]>([]);
  const [filter, setFilter] = useState<Filter>('ALL');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      setPayments(await getAllPayments());
    } catch (err) {
      setError(errorMessage(err, 'Failed to load transactions'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const visible = useMemo(() => payments.filter((p) =>
    (filter === 'ALL' || p.status === filter)
    && (p.userEmail.toLowerCase().includes(search.toLowerCase())
      || p.username.toLowerCase().includes(search.toLowerCase())
      || (p.providerReference ?? '').toLowerCase()
        .includes(search.toLowerCase()))), [payments, filter, search]);

  const collected = payments
    .filter((p) => p.status === 'SUCCESS')
    .reduce((sum, p) => sum + p.amount, 0);
  const pending = payments.filter((p) => p.status === 'PENDING').length;

  return (
    <SidebarLayout>
      <div className="bg-white border-b border-gray-100 px-8 py-5 shadow-sm
                      flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Transactions</h1>
          <p className="text-sm text-gray-400 mt-1">
            Customer wallet top-ups processed through Stripe.
          </p>
        </div>
        <button
          onClick={load}
          className="border border-gray-200 bg-white text-gray-600 px-4
                     py-2.5 rounded-xl text-sm font-medium hover:bg-gray-50"
        >
          ↻ Refresh
        </button>
      </div>

      <div className="px-8 py-6">
        <div className="grid grid-cols-3 gap-4 mb-6">
          <Stat label="Total Top-Ups" value={String(payments.length)} />
          <Stat label="Collected" value={peso(collected)}
                tone="text-[#9333EA]" />
          <Stat label="Awaiting Confirmation" value={String(pending)}
                tone="text-yellow-600" />
        </div>

        {error && (
          <div role="alert" className="bg-red-50 border border-red-200
                                       text-red-600 text-sm rounded-xl
                                       px-4 py-3 mb-4">
            {error}
          </div>
        )}

        <div className="flex flex-wrap items-center gap-3 mb-4">
          <input
            type="search"
            placeholder="Search by customer or reference..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Search transactions"
            className="flex-1 min-w-60 px-4 py-2.5 border border-gray-200
                       rounded-xl text-sm text-gray-700 bg-white outline-none
                       focus:border-[#9333EA] focus:ring-2
                       focus:ring-purple-100"
          />
          {FILTERS.map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              aria-pressed={filter === f}
              className={filter === f
                ? 'px-4 py-2 rounded-xl text-sm font-semibold bg-[#9333EA] '
                  + 'text-white'
                : 'px-4 py-2 rounded-xl text-sm font-medium bg-white '
                  + 'border border-gray-200 text-gray-500 hover:bg-gray-50'}
            >
              {f}
            </button>
          ))}
        </div>

        <div className="bg-white rounded-2xl shadow-sm border border-gray-100
                        overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b border-gray-100">
              <tr>
                {['Customer', 'Reference', 'Amount', 'Status', 'Date']
                  .map((h) => (
                    <th key={h} className="px-6 py-4 text-left text-xs
                                           font-semibold text-gray-500
                                           uppercase tracking-wide">
                      {h}
                    </th>
                  ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-gray-400">
                    Loading transactions...
                  </td>
                </tr>
              ) : visible.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-16 text-gray-400">
                    <div className="text-5xl mb-4">💳</div>
                    No transactions found
                  </td>
                </tr>
              ) : visible.map((p) => (
                <tr key={p.id} className="border-b border-gray-50
                                          hover:bg-purple-50">
                  <td className="px-6 py-4">
                    <div className="font-medium text-gray-800 text-sm">
                      {p.username}
                    </div>
                    <div className="text-xs text-gray-400">{p.userEmail}</div>
                  </td>
                  <td className="px-6 py-4 font-mono text-xs text-gray-500">
                    {p.providerReference ?? '—'}
                  </td>
                  <td className="px-6 py-4 text-sm font-semibold
                                 text-gray-800">
                    {peso(p.amount)}
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex px-3 py-1 rounded-full
                                      text-xs font-medium border
                                      ${BADGE[p.status]}`}>
                      {p.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-500">
                    {formatDateTime(p.paidAt ?? p.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {visible.length > 0 && (
            <div className="px-6 py-4 border-t border-gray-100 text-sm
                            text-gray-500">
              Showing {visible.length} of {payments.length} transactions
            </div>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}

function Stat({
  label,
  value,
  tone = 'text-gray-800',
}: {
  label: string;
  value: string;
  tone?: string;
}) {
  return (
    <div className="bg-white rounded-2xl p-5 shadow-sm border border-gray-100">
      <p className="text-xs text-gray-400 uppercase tracking-wide font-medium">
        {label}
      </p>
      <p className={`text-3xl font-bold mt-1 ${tone}`}>{value}</p>
    </div>
  );
}
