import { api } from '../lib/api';

export interface UserSummary {
  id: number;
  username: string;
  email: string;
  role: 'CUSTOMER' | 'ATTENDANT';
  authProvider: 'LOCAL' | 'GOOGLE';
  createdAt: string;
}

export interface NewAttendant {
  username: string;
  email: string;
  password: string;
}

export const getAttendants = async (): Promise<UserSummary[]> =>
  (await api.get<UserSummary[]>('/users/attendants')).data;

export const createAttendant = async (
  data: NewAttendant,
): Promise<UserSummary> =>
  (await api.post<UserSummary>('/users/attendants', data)).data;
