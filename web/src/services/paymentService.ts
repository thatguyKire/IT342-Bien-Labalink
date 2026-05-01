import axios from 'axios';

const API_URL = 
  'http://localhost:8080/api/payments';

export interface Payment {
  id: number;
  userEmail: string;
  username: string;
  amount: number;
  providerReference: string;
  clientSecret?: string;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  createdAt: string;
}

export const createPaymentIntent = async (
    userId: number,
    amount: number): Promise<Payment> => {
  const response = await axios.post(
    `${API_URL}/create-intent`,
    { userId, amount });
  return response.data;
};

export const confirmPayment = async (
    paymentIntentId: string): Promise<Payment> => {
  const response = await axios.post(
    `${API_URL}/confirm`,
    { paymentIntentId });
  return response.data;
};

export const getAllPayments = async ():
    Promise<Payment[]> => {
  const response = await axios.get(API_URL);
  return response.data;
};

export const getWalletBalance = async (
    userId: number): Promise<number> => {
  const response = await axios.get(
    `${API_URL}/wallet/${userId}`);
  return response.data.walletBalance;
};