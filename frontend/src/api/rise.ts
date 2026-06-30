import client from './client';
import type { ApiResponse, ResumeResponse, SkillGapResponse } from '../types';

export const riseApi = {
  upload: (file: File, onProgress?: (pct: number) => void) => {
    const fd = new FormData();
    fd.append('file', file);
    return client.post<ApiResponse<ResumeResponse>>('/rise/resume/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: e => onProgress && onProgress(Math.round((e.loaded * 100) / (e.total ?? 1))),
    }).then(r => r.data);
  },
  getActive: () => client.get<ApiResponse<ResumeResponse>>('/rise/resume').then(r => r.data),
  reanalyze: () => client.post<ApiResponse<ResumeResponse>>('/rise/resume/reanalyze').then(r => r.data),
  getHistory: () => client.get<ApiResponse<ResumeResponse[]>>('/rise/resume/history').then(r => r.data),
  getSkillGap: () => client.get<ApiResponse<SkillGapResponse>>('/rise/skill-gap').then(r => r.data),
};