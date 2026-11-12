import { useState, useEffect, useCallback, useRef } from 'react';
import {
  CircleAlert, CircleDollarSign, Hourglass, ReceiptText, Timer, WashingMachine, Wind,
} from 'lucide-react';
import {
  getAllBookings,
  getBookingStats,
  updateBookingStatus,
} from '../services/bookingService';
import type {
  Booking,
  BookingStats,
  BookingStatus,
} from '../services/bookingService';
import SidebarLayout from '../components/SidebarLayout';
import {
  EmptyState, FilterChips, LiveIndicator, PageHeader, StatCard, StatusBadge, TableHead,
  alertClass, dialogClass, overlayClass,
} from '../components/ui';
import { useWebSocket } from '../hooks/useWebSocket';
import { errorMessage } from '../lib/api';
import { formatDateTime, peso } from '../lib/format';
import { statusLabel } from '../lib/status';

type FilterStatus = 'ALL' | BookingStatus;

const FILTERS: FilterStatus[] = [
  'ALL', 'PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED',
];

const actionButton = 'text-xs px-3 py-1.5 rounded-lg font-semibold border '
  + 'transition-colors duration-150 disabled:opacity-50';

export default function BookingPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [stats, setStats] = useState<BookingStats>({
    totalBookings: 0,
    activeBookings: 0,
    pendingBookings: 0,
    totalRevenue: 0,
  });
  const [filter, setFilter] = useState<FilterStatus>('ALL');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [confirmCancel, setConfirmCancel] = useState<Booking | null>(null);
  // Rows that changed since the last fetch get a short highlight.
  const [changed, setChanged] = useState<Set<number>>(new Set());
  const lastStatus = useRef<Map<number, BookingStatus> | null>(null);

  const fetchData = useCallback(async () => {
    try {
      const [bookingData, statsData] = await Promise.all([
        getAllBookings(filter === 'ALL' ? undefined : filter),
        getBookingStats(),
      ]);
      const previous = lastStatus.current;
      if (previous) {
        setChanged(new Set(bookingData
          .filter((b) => previous.get(b.id) !== b.status)
          .map((b) => b.id)));
      }
      lastStatus.current = new Map(bookingData.map((b) => [b.id, b.status]));
      setBookings(bookingData);
      setStats(statsData);
      setError('');
    } catch (err) {
      setError(errorMessage(err, 'Failed to load bookings'));
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const live = useWebSocket({
    onBookingUpdate: () => fetchData(),
    onMachineUpdate: () => fetchData(),
  });

  const changeFilter = (next: FilterStatus) => {
    setLoading(true);
    lastStatus.current = null; // a new list isn't a live change
    setChanged(new Set());
    setFilter(next);
  };

  const handleStatusUpdate = async (id: number, next: BookingStatus) => {
    try {
      setBusyId(id);
      setError('');
      await updateBookingStatus(id, next);
      await fetchData();
    } catch (err) {
      setError(errorMessage(err, 'Failed to update booking'));
    } finally {
      setBusyId(null);
      setConfirmCancel(null);
    }
  };

  return (
    <SidebarLayout>
      <PageHeader
        title="Bookings"
        description="Activate sessions when customers load their machine, then complete them."
      >
        <LiveIndicator live={live} />
      </PageHeader>

      <div className="px-8 py-6 space-y-6">
        <div className="grid grid-cols-2 xl:grid-cols-4 gap-4">
          <StatCard label="All bookings" value={stats.totalBookings}
                    icon={ReceiptText} tone="brand" />
          <StatCard label="Waiting to start" value={stats.pendingBookings}
                    icon={Hourglass} tone="warning" />
          <StatCard label="Running now" value={stats.activeBookings}
                    icon={Timer} tone="info" />
          <StatCard label="Revenue from completed" value={peso(stats.totalRevenue)}
                    icon={CircleDollarSign} tone="aqua" />
        </div>

        {error && (
          <div role="alert" className={alertClass}>
            <CircleAlert size={18} className="shrink-0 mt-px" aria-hidden="true" />
            {error}
          </div>
        )}

        <FilterChips
          options={FILTERS}
          value={filter}
          onChange={changeFilter}
          labelFor={(f) => (f === 'ALL' ? 'All' : statusLabel(f))}
        />

        <div className="bg-white rounded-2xl border border-gray-100 overflow-hidden">
          <table className="w-full">
            <TableHead columns={[
              'Customer', 'Machine', 'Start', 'End', 'Duration', 'Price',
              'Status', 'Actions']} />
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-gray-400">
                    Loading bookings…
                  </td>
                </tr>
              ) : bookings.length === 0 ? (
                <tr>
                  <td colSpan={8}>
                    <EmptyState
                      icon={ReceiptText}
                      title="No bookings here"
                      hint="Customers start sessions by scanning a machine's QR sticker."
                    />
                  </td>
                </tr>
              ) : bookings.map((booking, i) => (
                <tr
                  key={`${booking.id}-${booking.status}`}
                  style={{ '--i': i } as React.CSSProperties}
                  className={`border-b border-gray-50 hover:bg-gray-50
                              transition-colors text-sm text-gray-700
                              ${changed.has(booking.id) ? 'animate-flash' : 'stagger'}`}
                >
                  <td className="px-6 py-4">
                    <div className="font-semibold text-gray-900">
                      {booking.username}
                    </div>
                    <div className="text-xs text-gray-500">
                      {booking.userEmail}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-2 font-semibold text-gray-900">
                      {booking.machineType === 'WASHER'
                        ? <WashingMachine size={16} className="text-brand-600 shrink-0" aria-hidden="true" />
                        : <Wind size={16} className="text-aqua-700 shrink-0" aria-hidden="true" />}
                      {booking.machineName}
                    </div>
                    <div className="text-xs text-gray-500 tabular">
                      MAC-{String(booking.machineId).padStart(3, '0')}
                    </div>
                  </td>
                  <td className="px-6 py-4 tabular">
                    {formatDateTime(booking.startTime)}
                  </td>
                  <td className="px-6 py-4 tabular">
                    {formatDateTime(booking.endTime)}
                  </td>
                  <td className="px-6 py-4 tabular">
                    {booking.durationMinutes} min
                  </td>
                  <td className="px-6 py-4 font-semibold text-gray-900 tabular">
                    {peso(booking.totalPrice)}
                  </td>
                  <td className="px-6 py-4">
                    <StatusBadge status={booking.status} />
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex gap-2">
                      {booking.status === 'PENDING' && (
                        <button
                          disabled={busyId === booking.id}
                          onClick={() => handleStatusUpdate(booking.id, 'ACTIVE')}
                          className={`${actionButton} border-brand-200 text-brand-600 hover:bg-brand-50`}
                        >
                          Activate
                        </button>
                      )}
                      {booking.status === 'ACTIVE' && (
                        <button
                          disabled={busyId === booking.id}
                          onClick={() => handleStatusUpdate(booking.id, 'COMPLETED')}
                          className={`${actionButton} border-green-200 text-green-700 hover:bg-green-50`}
                        >
                          Complete
                        </button>
                      )}
                      {(booking.status === 'PENDING'
                        || booking.status === 'ACTIVE') && (
                        <button
                          disabled={busyId === booking.id}
                          onClick={() => setConfirmCancel(booking)}
                          className={`${actionButton} border-red-200 text-red-600 hover:bg-red-50`}
                        >
                          Cancel
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {bookings.length > 0 && (
            <div className="px-6 py-4 border-t border-gray-100 text-sm text-gray-500">
              Showing {bookings.length} booking{bookings.length !== 1 ? 's' : ''}
            </div>
          )}
        </div>
      </div>

      {confirmCancel && (
        <div className={overlayClass} role="dialog"
             aria-modal="true" aria-labelledby="cancel-title">
          <div className={`${dialogClass} max-w-md p-8`}>
            <h2 id="cancel-title" className="text-xl font-bold text-gray-900 mb-2">
              Cancel this booking?
            </h2>
            <p className="text-sm text-gray-600 mb-6">
              {confirmCancel.username} gets{' '}
              <strong className="tabular">{peso(confirmCancel.totalPrice)}</strong>{' '}
              back in their wallet, and {confirmCancel.machineName} becomes available.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setConfirmCancel(null)}
                className="flex-1 border border-gray-200 text-gray-700 py-3
                           rounded-xl font-semibold text-sm hover:bg-gray-50"
              >
                Keep booking
              </button>
              <button
                disabled={busyId !== null}
                onClick={() => handleStatusUpdate(confirmCancel.id, 'CANCELLED')}
                className="flex-1 bg-red-600 text-white py-3 rounded-xl
                           font-semibold text-sm hover:bg-red-700
                           active:scale-[0.98] transition-[background-color,transform]
                           disabled:opacity-60"
              >
                Cancel and refund
              </button>
            </div>
          </div>
        </div>
      )}
    </SidebarLayout>
  );
}
