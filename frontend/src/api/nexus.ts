import client from './client';
import type { ApiResponse, GitHubProfileResponse, LeetCodeProfileResponse, LeetGitStats, LeetGitSyncResponse } from '../types';

export const nexusApi = {
  connectGitHub: (code: string) =>
    client.post<ApiResponse<GitHubProfileResponse>>('/nexus/github/token', { token }).then(r => r.data),
  getGitHub: () =>
    client.get<ApiResponse<GitHubProfileResponse>>('/nexus/github').then(r => r.data),
  syncGitHub: () =>
    client.post<ApiResponse<GitHubProfileResponse>>('/nexus/github/sync').then(r => r.data),
  disconnectGitHub: () =>
    client.delete('/nexus/github/disconnect').then(r => r.data),
  connectLeetCode: (username: string) =>
    client.post<ApiResponse<LeetCodeProfileResponse>>('/nexus/leetcode/connect', { username }).then(r => r.data),
  getLeetCode: () =>
    client.get<ApiResponse<LeetCodeProfileResponse>>('/nexus/leetcode').then(r => r.data),
  syncLeetCode: () =>
    client.post<ApiResponse<LeetCodeProfileResponse>>('/nexus/leetcode/sync').then(r => r.data),
  disconnectLeetCode: () =>
    client.delete('/nexus/leetcode/disconnect').then(r => r.data),
  
  // ===== LEETGIT (renamed from LeetHub+) =====
  enableLeetGit: (repoName: string) =>
    client.post('/nexus/leetgit/enable', { repoName }).then(r => r.data),
  syncSolution: (data: {
    problemTitle: string;
    problemSlug: string;
    problemDescription?: string;
    language: string;
    difficulty: string;
    code: string;
  }) =>
    client.post<ApiResponse<LeetGitSyncResponse>>('/nexus/leetgit/sync', data).then(r => r.data),
  getLeetGitStats: () =>
    client.get<ApiResponse<LeetGitStats>>('/nexus/leetgit/stats').then(r => r.data),
  getLeetGitHistory: () =>
    client.get<ApiResponse<LeetGitSyncResponse[]>>('/nexus/leetgit/history').then(r => r.data),
};
