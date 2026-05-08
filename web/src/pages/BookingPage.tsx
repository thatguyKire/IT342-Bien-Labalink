import { useState, useEffect, useCallback } from 'react';
import {
  getAllBookings,
  getBookingStats,
  updateBookingStatus,
} from '../services/bookingService';
import type {
  Booking,
  BookingStats,
} from '../services/bookingService';
import { bookingStyles as s } from
  '../styles/BookingPage.styles';

  import SidebarLayout from '../components/SidebarLayout';

  import { useWebSocket } from '../hooks/useWebSocket';

type FilterStatus = 
  'ALL' | 'PENDING' | 'ACTIVE' | 
  'COMPLETED' | 'CANCELLED';

export default function BookingPage() {
  const [bookings, setBookings] = 
    useState<Booking[]>([]);
  const [stats, setStats] = 
    useState<BookingStats>({
      totalBookings: 0,
      activeBookings: 0,
      totalRevenue: 0,
    });
  const [filter, setFilter] = 
    useState<FilterStatus>('ALL');
  const [loading, setLoading] = useState(true);

  // WebSocket — auto refresh on booking updates
useWebSocket({
  onBookingUpdate: () => {
    fetchData();
  },
  onMachineUpdate: () => {
    fetchData();
  },
});

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [bookingData, statsData] = 
        await Promise.all([
          getAllBookings(
            filter === 'ALL' 
              ? undefined 
              : filter),
          getBookingStats(),
        ]);
      setBookings(bookingData);
      setStats(statsData);
    } catch (err) {
      console.error('Failed to load bookings', err);
    } finally {
      setLoading(false);
    }
  }, [filter]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleStatusUpdate = async (
      id: number,
      newStatus: string) => {
    try {
      await updateBookingStatus(id, newStatus);
      fetchData();
    } catch (err) {
      console.error('Failed to update status', err);
    }
  };

  const getStatusBadge = (
      status: Booking['status']) => {
    switch (status) {
      case 'PENDING':
        return <span className={s.badgePending}>
          ● PENDING
        </span>;
      case 'ACTIVE':
        return <span className={s.badgeActive}>
          ● ACTIVE
        </span>;
      case 'COMPLETED':
        return <span className={s.badgeCompleted}>
          ● COMPLETED
        </span>;
      case 'CANCELLED':
        return <span className={s.badgeCancelled}>
          ● CANCELLED
        </span>;
    }
  };

  const formatDateTime = (dateStr: string | null) => {
    if (!dateStr) return '—';
    return new Date(dateStr)
      .toLocaleString('en-PH', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
  };

  const filters: FilterStatus[] = [
    'ALL', 'PENDING', 'ACTIVE',
    'COMPLETED', 'CANCELLED'
  ];

  return (
  <SidebarLayout>
    <div className={s.page}>

      {/* Header */}
      <div className={s.header}>
        <div>
          <h1 className={s.headerTitle}>
            Bookings & Transactions
          </h1>
          <p className={s.headerSubtitle}>
            Monitor and manage all laundry sessions
          </p>
        </div>
      </div>

      {/* Stats */}
      <div className={s.statsRow}>
        <div className={s.statCard}>
          <div className={s.statLabel}>
            Total Bookings
          </div>
          <div className={s.statValue}>
            {stats.totalBookings}
          </div>
        </div>
        <div className={s.statCard}>
          <div className={s.statLabel}>
            Active Sessions
          </div>
          <div className="text-3xl font-bold 
                          text-blue-600 mt-1">
            {stats.activeBookings}
          </div>
        </div>
        <div className={s.statCard}>
          <div className={s.statLabel}>
            Total Revenue
          </div>
          <div className="text-3xl font-bold 
                          text-[#9333EA] mt-1">
            ₱{stats.totalRevenue.toFixed(2)}
          </div>
        </div>
      </div>

      {/* Filter Buttons */}
      <div className={s.filterRow}>
        {filters.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={
              filter === f
                ? s.filterButtonActive
                : s.filterButton
            }
          >
            {f}
          </button>
        ))}
      </div>

      {/* Booking Records Table */}
      <div className={s.tableContainer}>
        <div className={s.tableTitle}>
          Booking Records
          <span className="text-sm font-normal 
                           text-gray-400 ml-2">
            Live monitoring of laundry sessions
          </span>
        </div>
        <table className={s.table}>
          <thead className={s.tableHead}>
            <tr>
              <th className={s.tableHeadCell}>
                User
              </th>
              <th className={s.tableHeadCell}>
                Machine
              </th>
              <th className={s.tableHeadCell}>
                Type
              </th>
              <th className={s.tableHeadCell}>
                Start Time
              </th>
              <th className={s.tableHeadCell}>
                End Time
              </th>
              <th className={s.tableHeadCell}>
                Total Price
              </th>
              <th className={s.tableHeadCell}>
                Status
              </th>
              <th className={s.tableHeadCell}>
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8}
                    className="text-center 
                               py-12 text-gray-400">
                  Loading bookings...
                </td>
              </tr>
            ) : bookings.length === 0 ? (
              <tr>
                <td colSpan={8}>
                  <div className={s.emptyState}>
                    <div className={s.emptyIcon}>
                      🫧
                    </div>
                    <div className={s.emptyText}>
                      No bookings found
                    </div>
                  </div>
                </td>
              </tr>
            ) : (
              bookings.map((booking) => (
                <tr key={booking.id}
                    className={s.tableRow}>
                  <td className={s.tableCell}>
                    <div className="font-medium 
                                    text-gray-800">
                      {booking.username}
                    </div>
                    <div className="text-xs 
                                    text-gray-400">
                      {booking.userEmail}
                    </div>
                  </td>
                  <td className={s.tableCell}>
                    <span className="text-[#9333EA] 
                                     font-semibold">
                      MAC-{String(booking.machineId)
                        .padStart(3, '0')}
                    </span>
                    <div className="text-xs 
                                    text-gray-400">
                      {booking.machineName}
                    </div>
                  </td>
                  <td className={s.tableCell}>
                    {booking.machineType}
                  </td>
                  <td className={s.tableCell}>
                    {formatDateTime(booking.startTime)}
                  </td>
                  <td className={s.tableCell}>
                    {formatDateTime(booking.endTime)}
                  </td>
                  <td className={s.tableCell}>
                    <span className="font-semibold 
                                     text-gray-800">
                      ₱{booking.totalPrice
                        .toFixed(2)}
                    </span>
                  </td>
                  <td className={s.tableCell}>
                    {getStatusBadge(booking.status)}
                  </td>
                  <td className={s.tableCell}>
                    <div className="flex gap-2">
                      {booking.status === 
                          'PENDING' && (
                        <button
                          onClick={() =>
                            handleStatusUpdate(
                              booking.id, 'ACTIVE')}
                          className={`${s.actionButton}
                            border-blue-200 
                            text-blue-600 
                            hover:bg-blue-50`}
                        >
                          Activate
                        </button>
                      )}
                      {booking.status === 
                          'ACTIVE' && (
                        <button
                          onClick={() =>
                            handleStatusUpdate(
                              booking.id, 
                              'COMPLETED')}
                          className={`${s.actionButton}
                            border-green-200
                            text-green-600
                            hover:bg-green-50`}
                        >
                          Complete
                        </button>
                      )}
                      {(booking.status === 
                            'PENDING' ||
                        booking.status === 
                            'ACTIVE') && (
                        <button
                          onClick={() =>
                            handleStatusUpdate(
                              booking.id,
                              'CANCELLED')}
                          className={`${s.actionButton}
                            border-red-200
                            text-red-500
                            hover:bg-red-50`}
                        >
                          Cancel
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination info */}
        {bookings.length > 0 && (
          <div className="px-6 py-4 border-t 
                          border-gray-100">
            <span className="text-sm text-gray-500">
              Showing {bookings.length} record
              {bookings.length !== 1 ? 's' : ''}
            </span>
          </div>
        )}
      </div>

  </div>
  </SidebarLayout>
);
}    

