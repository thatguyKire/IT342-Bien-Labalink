import axios from 'axios';

const API_URL = 'http://localhost:8080/api/auth';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  role: string;
  email: string;
  username: string;
}

export const register = async (
  data: RegisterRequest
) => {
  const response = await axios.post(
    `${API_URL}/register`, 
    data
  );
  return response.data;
};

export const login = async (
  data: LoginRequest
): Promise<LoginResponse> => {
  const response = await axios.post(
    `${API_URL}/login`, 
    data
  );
  return response.data;
};