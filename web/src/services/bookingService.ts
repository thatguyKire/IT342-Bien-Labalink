import axios from 'axios';

const API_URL = 'http://localhost:8080/api/bookings';

export interface Booking {
  id: number;
  userEmail: string;
  username: string;
  machineId: number;
  machineName: string;
  machineType: string;
  status: 'PENDING' | 'ACTIVE' | 
          'COMPLETED' | 'CANCELLED';
  startTime: string;
  endTime: string | null;
  totalPrice: number;
  createdAt: string;
}

export interface BookingStats {
  totalBookings: number;
  activeBookings: number;
  totalRevenue: number;
}

export const getAllBookings = async (
    status?: string): Promise<Booking[]> => {
  const url = status
    ? `${API_URL}?status=${status}`
    : API_URL;
  const response = await axios.get(url);
  return response.data;
};

export const updateBookingStatus = async (
    id: number,
    status: string): Promise<Booking> => {
  const response = await axios.patch(
    `${API_URL}/${id}/status`,
    { status });
  return response.data;
};

export const getBookingStats = async ():
    Promise<BookingStats> => {
  const response = await axios.get(
    `${API_URL}/stats`);
  return response.data;
};