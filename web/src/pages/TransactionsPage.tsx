import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  CircleAlert, CircleDollarSign, CreditCard, Hourglass, RefreshCw, Search,
} from 'lucide-react';
import SidebarLayout from '../components/SidebarLayout';
import {
  EmptyState, FilterChips, PageHeader, StatCard, StatusBadge, TableHead, alertClass,
} from '../components/ui';
import { errorMessage } from '../lib/api';
import { formatDateTime, peso } from '../lib/format';
import { statusLabel } from '../lib/status';
import { getAllPayments } from '../services/paymentService';
import type { Payment, PaymentStatus } from '../services/paymentService';

type Filter = 'ALL' | PaymentStatus;
const FILTERS: Filter[] = ['ALL', 'SUCCESS', 'PENDING', 'FAILED'];

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
      <PageHeader
        title="Transactions"
        description="Customer wallet top-ups paid through Stripe (test mode)."
      >
        <button
          onClick={load}
          disabled={loading}
          className="flex items-center gap-2 border border-gray-200 bg-white
                     text-gray-700 px-4 py-2.5 rounded-xl text-sm font-semibold
                     hover:bg-gray-50 transition-colors disabled:opacity-60"
        >
          <RefreshCw size={16} aria-hidden="true"
                     className={loading ? 'animate-spin' : ''} />
          Refresh
        </button>
      </PageHeader>

      <div className="px-8 py-6 space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <StatCard label="Top-ups" value={payments.length}
                    icon={CreditCard} tone="brand" />
          <StatCard label="Collected" value={peso(collected)}
                    icon={CircleDollarSign} tone="accent" />
          <StatCard label="Waiting for Stripe" value={pending}
                    icon={Hourglass} tone="warning" />
        </div>

        {error && (
          <div role="alert" className={alertClass}>
            <CircleAlert size={18} className="shrink-0 mt-px" aria-hidden="true" />
            {error}
          </div>
        )}

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex-1 min-w-60 relative">
            <Search size={16} aria-hidden="true"
                    className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="search"
              placeholder="Search by customer or reference"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search transactions"
              className="w-full pl-10 pr-4 py-2.5 border border-gray-200
                         rounded-xl text-sm text-gray-900 bg-white outline-none
                         focus:border-brand-500 focus:ring-4 focus:ring-brand-50"
            />
          </div>
          <FilterChips
            options={FILTERS}
            value={filter}
            onChange={setFilter}
            labelFor={(f) => (f === 'ALL' ? 'All' : statusLabel(f))}
          />
        </div>

        <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden">
          <table className="w-full">
            <TableHead columns={['Customer', 'Reference', 'Amount', 'Status', 'Date']} />
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-gray-400">
                    Loading transactions…
                  </td>
                </tr>
              ) : visible.length === 0 ? (
                <tr>
                  <td colSpan={5}>
                    <EmptyState
                      icon={CreditCard}
                      title="No transactions match"
                      hint="Top-ups appear here when customers add funds in the app."
                    />
                  </td>
                </tr>
              ) : visible.map((p, i) => (
                <tr key={p.id}
                    style={{ '--i': i } as React.CSSProperties}
                    className="stagger border-b border-gray-50 hover:bg-gray-50
                               transition-colors">
                  <td className="px-6 py-4">
                    <div className="font-semibold text-gray-900 text-sm">
                      {p.username}
                    </div>
                    <div className="text-xs text-gray-500">{p.userEmail}</div>
                  </td>
                  <td className="px-6 py-4 text-xs text-gray-500 tabular">
                    {p.providerReference ?? '—'}
                  </td>
                  <td className="px-6 py-4 text-sm font-semibold text-gray-900 tabular">
                    {peso(p.amount)}
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge status={p.status} />
                  </td>
                  <td className="px-6 py-4 text-sm text-gray-600 tabular">
                    {formatDateTime(p.paidAt ?? p.createdAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {visible.length > 0 && (
            <div className="px-6 py-4 border-t border-gray-100 text-sm text-gray-500">
              Showing {visible.length} of {payments.length} transactions
            </div>
          )}
        </div>
      </div>
    </SidebarLayout>
  );
}
