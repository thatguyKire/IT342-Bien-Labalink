import { api } from '../lib/api';

export type MachineType = 'WASHER' | 'DRYER';
export type MachineStatus = 'AVAILABLE' | 'RUNNING' | 'OUT_OF_ORDER';

export interface Machine {
  id: number;
  machineName: string;
  machineType: MachineType;
  status: MachineStatus;
  qrToken?: string;
  hourlyRate: number;
  createdAt: string;
}

export interface MachineRequest {
  machineName: string;
  machineType: MachineType;
  hourlyRate: number;
  status: MachineStatus;
}

export const getAllMachines = async (): Promise<Machine[]> =>
  (await api.get<Machine[]>('/machines')).data;

export const createMachine = async (
  data: MachineRequest,
): Promise<Machine> => (await api.post<Machine>('/machines', data)).data;

export const updateMachine = async (
  id: number,
  data: MachineRequest,
): Promise<Machine> =>
  (await api.put<Machine>(`/machines/${id}`, data)).data;

export const regenerateQr = async (id: number): Promise<Machine> =>
  (await api.post<Machine>(`/machines/${id}/regenerate-qr`)).data;

export const deleteMachine = async (id: number): Promise<void> => {
  await api.delete(`/machines/${id}`);
};
