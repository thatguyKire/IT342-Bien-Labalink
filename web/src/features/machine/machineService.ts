import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const API_URL = `${API_BASE_URL}/api/machines`;

const getHeaders = () => {
  const token = localStorage.getItem('token');
  return {
    headers: {
      Authorization: `Bearer ${token}`
    }
  };
};

export interface Machine {
  id: number;
  machineName: string;
  machineType: 'WASHER' | 'DRYER';
  status: 'AVAILABLE' | 'RUNNING' | 'OUT_OF_ORDER';
  qrToken: string;
  hourlyRate: number;
  createdAt: string;
}

export interface MachineRequest {
  machineName: string;
  machineType: 'WASHER' | 'DRYER';
  hourlyRate: number;
  status: 'AVAILABLE' | 'RUNNING' | 'OUT_OF_ORDER';
}

export const getAllMachines = async (): 
    Promise<Machine[]> => {
  const response = await axios.get(API_URL, getHeaders());
  return response.data;
};

export const createMachine = async (
    data: MachineRequest): Promise<Machine> => {
  const response = await axios.post(API_URL, data, getHeaders());
  return response.data;
};

export const updateMachine = async (
    id: number, 
    data: MachineRequest): Promise<Machine> => {
  const response = await axios.put(
    `${API_URL}/${id}`, data, getHeaders());
  return response.data;
};

export const deleteMachine = async (
    id: number): Promise<void> => {
  await axios.delete(`${API_URL}/${id}`, getHeaders());
};