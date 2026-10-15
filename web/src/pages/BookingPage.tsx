import { useState, useEffect, useCallback } from 'react';
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
import { bookingStyles as s } from '../styles/BookingPage.styles';
import SidebarLayout from '../components/SidebarLayout';
import { useWebSocket } from '../hooks/useWebSocket';
import { errorMessage } from '../lib/api';
import { formatDateTime, peso } from '../lib/format';

type FilterStatus = 'ALL' | BookingStatus;

const FILTERS: FilterStatus[] = [
  'ALL', 'PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED',
];

const BADGES: Record<BookingStatus, string> = {
  PENDING: s.badgePending,
  ACTIVE: s.badgeActive,
  COMPLETED: s.badgeCompleted,
  CANCELLED: s.badgeCancelled,
};

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

  const fetchData = useCallback(async () => {
    try {
      const [bookingData, statsData] = await Promise.all([
        getAllBookings(filter === 'ALL' ? undefined : filter),
        getBookingStats(),
      ]);
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
      <div className={s.page}>

        {/* Header */}
        <div className={s.header}>
          <div>
            <h1 className={s.headerTitle}>Bookings &amp; Sessions</h1>
            <p className={s.headerSubtitle}>
              Monitor and manage all laundry sessions
            </p>
          </div>
          <span className="flex items-center gap-2 text-xs text-gray-400">
            <span className={`w-2 h-2 rounded-full ${live
              ? 'bg-green-500 animate-pulse' : 'bg-gray-300'}`} />
            {live ? 'Live' : 'Connecting...'}
          </span>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 px-8 py-6">
          <div className={s.statCard}>
            <div className={s.statLabel}>Total Bookings</div>
            <div className={s.statValue}>{stats.totalBookings}</div>
          </div>
          <div className={s.statCard}>
            <div className={s.statLabel}>Awaiting Activation</div>
            <div className="text-3xl font-bold text-gray-600 mt-1">
              {stats.pendingBookings}
            </div>
          </div>
          <div className={s.statCard}>
            <div className={s.statLabel}>Active Sessions</div>
            <div className="text-3xl font-bold text-blue-600 mt-1">
              {stats.activeBookings}
            </div>
          </div>
          <div className={s.statCard}>
            <div className={s.statLabel}>Completed Revenue</div>
            <div className="text-3xl font-bold text-[#9333EA] mt-1">
              {peso(stats.totalRevenue)}
            </div>
          </div>
        </div>

        {error && (
          <div role="alert" className="mx-8 mb-4 bg-red-50 border
                                       border-red-200 text-red-600 text-sm
                                       rounded-xl px-4 py-3">
            {error}
          </div>
        )}

        {/* Filter Buttons */}
        <div className={s.filterRow}>
          {FILTERS.map((f) => (
            <button
              key={f}
              onClick={() => {
                setLoading(true);
                setFilter(f);
              }}
              aria-pressed={filter === f}
              className={filter === f ? s.filterButtonActive : s.filterButton}
            >
              {f}
            </button>
          ))}
        </div>

        {/* Booking Records Table */}
        <div className={s.tableContainer}>
          <div className={s.tableTitle}>
            Booking Records
            <span className="text-sm font-normal text-gray-400 ml-2">
              Updates automatically
            </span>
          </div>
          <table className={s.table}>
            <thead className={s.tableHead}>
              <tr>
                {['Customer', 'Machine', 'Start', 'End', 'Duration', 'Price',
                  'Status', 'Actions'].map((h) => (
                  <th key={h} className={s.tableHeadCell}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="text-center py-12 text-gray-400">
                    Loading bookings...
                  </td>
                </tr>
              ) : bookings.length === 0 ? (
                <tr>
                  <td colSpan={8}>
                    <div className={s.emptyState}>
                      <div className={s.emptyIcon}>🫧</div>
                      <div className={s.emptyText}>No bookings found</div>
                    </div>
                  </td>
                </tr>
              ) : bookings.map((booking) => (
                <tr key={booking.id} className={s.tableRow}>
                  <td className={s.tableCell}>
                    <div className="font-medium text-gray-800">
                      {booking.username}
                    </div>
                    <div className="text-xs text-gray-400">
                      {booking.userEmail}
                    </div>
                  </td>
                  <td className={s.tableCell}>
                    <span className="text-[#9333EA] font-semibold">
                      MAC-{String(booking.machineId).padStart(3, '0')}
                    </span>
                    <div className="text-xs text-gray-400">
                      {booking.machineName} · {booking.machineType}
                    </div>
                  </td>
                  <td className={s.tableCell}>
                    {formatDateTime(booking.startTime)}
                  </td>
                  <td className={s.tableCell}>
                    {formatDateTime(booking.endTime)}
                  </td>
                  <td className={s.tableCell}>
                    {booking.durationMinutes} min
                  </td>
                  <td className={s.tableCell}>
                    <span className="font-semibold text-gray-800">
                      {peso(booking.totalPrice)}
                    </span>
                  </td>
                  <td className={s.tableCell}>
                    <span className={BADGES[booking.status]}>
                      ● {booking.status}
                    </span>
                  </td>
                  <td className={s.tableCell}>
                    <div className="flex gap-2">
                      {booking.status === 'PENDING' && (
                        <button
                          disabled={busyId === booking.id}
                          onClick={() =>
                            handleStatusUpdate(booking.id, 'ACTIVE')}
                          className={`${s.actionButton} border-blue-200
                            text-blue-600 hover:bg-blue-50
                            disabled:opacity-50`}
                        >
                          Activate
                        </button>
                      )}
                      {booking.status === 'ACTIVE' && (
                        <button
                          disabled={busyId === booking.id}
                          onClick={() =>
                            handleStatusUpdate(booking.id, 'COMPLETED')}
                          className={`${s.actionButton} border-green-200
                            text-green-600 hover:bg-green-50
                            disabled:opacity-50`}
                        >
                          Complete
                        </button>
                      )}
                      {(booking.status === 'PENDING'
                        || booking.status === 'ACTIVE') && (
                        <button
                          disabled={busyId === booking.id}
                          onClick={() => setConfirmCancel(booking)}
                          className={`${s.actionButton} border-red-200
                            text-red-500 hover:bg-red-50
                            disabled:opacity-50`}
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
            <div className="px-6 py-4 border-t border-gray-100">
              <span className="text-sm text-gray-500">
                Showing {bookings.length} record
                {bookings.length !== 1 ? 's' : ''}
              </span>
            </div>
          )}
        </div>
      </div>

      {confirmCancel && (
        <div className="fixed inset-0 bg-black/50 flex items-center
                        justify-center z-50 px-4" role="dialog"
             aria-modal="true" aria-labelledby="cancel-title">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md p-8">
            <h2 id="cancel-title" className="text-xl font-bold text-gray-800
                                             mb-2">
              Cancel booking?
            </h2>
            <p className="text-sm text-gray-500 mb-6">
              {confirmCancel.username} will be refunded{' '}
              <strong>{peso(confirmCancel.totalPrice)}</strong> to their
              wallet and {confirmCancel.machineName} becomes available.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setConfirmCancel(null)}
                className="flex-1 border border-gray-200 text-gray-600 py-3
                           rounded-xl font-semibold text-sm hover:bg-gray-50"
              >
                Keep booking
              </button>
              <button
                disabled={busyId !== null}
                onClick={() =>
                  handleStatusUpdate(confirmCancel.id, 'CANCELLED')}
                className="flex-1 bg-red-500 text-white py-3 rounded-xl
                           font-semibold text-sm hover:bg-red-600
                           disabled:opacity-60"
              >
                Cancel &amp; refund
              </button>
            </div>
          </div>
        </div>
      )}
    </SidebarLayout>
  );
}
