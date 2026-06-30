import client from './client';
import type { ApiResponse, ChatResponse, ConversationDetail, ConversationSummary, CodeReviewResponse } from '../types';

export const mentorApi = {
  chat: (data: { message: string; conversationId?: number }) =>
    client.post<ApiResponse<ChatResponse>>('/mentor/chat', data).then(r => r.data),
  getConversations: () =>
    client.get<ApiResponse<ConversationSummary[]>>('/mentor/conversations').then(r => r.data),
  getConversation: (id: number) =>
    client.get<ApiResponse<ConversationDetail>>(`/mentor/conversations/${id}`).then(r => r.data),
  deleteConversation: (id: number) =>
    client.delete(`/mentor/conversations/${id}`).then(r => r.data),
  reviewCode: (data: { code: string; language: string; problemContext?: string; requestBetterSolution?: boolean }) =>
    client.post<ApiResponse<CodeReviewResponse>>('/mentor/code-review', data).then(r => r.data),
  getCodeReviewHistory: () =>
    client.get<ApiResponse<CodeReviewResponse[]>>('/mentor/code-review/history').then(r => r.data),
};