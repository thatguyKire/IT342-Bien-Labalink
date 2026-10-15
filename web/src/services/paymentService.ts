import { api } from '../lib/api';

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

export interface Payment {
  id: number;
  userEmail: string;
  username: string;
  amount: number;
  currency: string;
  providerReference: string | null;
  status: PaymentStatus;
  createdAt: string;
  paidAt?: string;
}

export const getAllPayments = async (): Promise<Payment[]> =>
  (await api.get<Payment[]>('/payments')).data;
