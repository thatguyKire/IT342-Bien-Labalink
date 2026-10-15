import { api } from '../lib/api';

export type BookingStatus = 'PENDING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Booking {
  id: number;
  userId: number;
  userEmail: string;
  username: string;
  machineId: number;
  machineName: string;
  machineType: string;
  status: BookingStatus;
  startTime: string;
  endTime: string | null;
  durationMinutes: number;
  totalPrice: number;
  createdAt: string;
}

export interface BookingStats {
  totalBookings: number;
  activeBookings: number;
  pendingBookings: number;
  totalRevenue: number;
}

export const getAllBookings = async (
  status?: BookingStatus,
): Promise<Booking[]> =>
  (await api.get<Booking[]>('/bookings', {
    params: status ? { status } : undefined,
  })).data;

export const updateBookingStatus = async (
  id: number,
  status: BookingStatus,
): Promise<Booking> =>
  (await api.patch<Booking>(`/bookings/${id}/status`, { status })).data;

export const getBookingStats = async (): Promise<BookingStats> =>
  (await api.get<BookingStats>('/bookings/stats')).data;
