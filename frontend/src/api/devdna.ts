import client from './client';
import type { ApiResponse, DevDnaReportResponse, WeeklyReportResponse } from '../types';

export const devDnaApi = {
  getLatest: () =>
    client.get<ApiResponse<DevDnaReportResponse>>('/devdna').then(r => r.data),
  generate: () =>
    client.post<ApiResponse<DevDnaReportResponse>>('/devdna/generate').then(r => r.data),
  getWeekly: () =>
    client.get<ApiResponse<WeeklyReportResponse>>('/devdna/weekly').then(r => r.data),
  getHistory: () =>
    client.get('/devdna/history').then(r => r.data),
};