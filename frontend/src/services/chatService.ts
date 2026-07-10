import { apiClient } from './api';

export interface ConversationResponse {
  id: string;
  bookingId: string;
  tenantId: string;
  ownerId: string;
  tenantName: string;
  ownerName: string;
  latestMessage: string | null;
  latestMessageTime: string;
  unreadCount: number;
}

export interface MessageResponse {
  id: string;
  senderId: string;
  content: string;
  read: boolean;
  createdAt: string;
}

export const chatService = {
  getConversations: async (): Promise<ConversationResponse[]> => {
    const response = await apiClient.get('/conversations');
    return response.data.data;
  },

  getMessages: async (conversationId: string): Promise<MessageResponse[]> => {
    const response = await apiClient.get(`/conversations/${conversationId}/messages`);
    return response.data.data;
  },

  sendMessage: async (conversationId: string, content: string): Promise<MessageResponse> => {
    const response = await apiClient.post(`/conversations/${conversationId}/messages`, { content });
    return response.data.data;
  },

  markAsRead: async (conversationId: string): Promise<void> => {
    await apiClient.post(`/conversations/${conversationId}/read`);
  }
};
