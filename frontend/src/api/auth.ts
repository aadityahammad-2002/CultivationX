import client from './client';
import type { ApiResponse, AuthResponse, LoginRequest, RegisterRequest, SetupRequest, User } from '../types';

export const authApi = {
  register: (data: RegisterRequest) =>
    client.post<ApiResponse<AuthResponse>>('/auth/register', data).then(r => r.data.data!),
  login: (data: LoginRequest) =>
    client.post<ApiResponse<AuthResponse>>('/auth/login', data).then(r => r.data.data!),
  getMe: () =>
    client.get<ApiResponse<User>>('/auth/me').then(r => r.data.data!),
  setup: (data: SetupRequest) =>
    client.post<ApiResponse<User>>('/auth/setup', data).then(r => r.data.data!),
  updateProfile: (data: Partial<User>) =>
    client.put<ApiResponse<User>>('/auth/profile', data).then(r => r.data.data!),
  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    client.post<ApiResponse<void>>('/auth/change-password', data).then(r => r.data.data),
};
