import { apiClient } from './api';

export interface InAppNotification {
  id: string;
  userId: string;
  title: string;
  message: string;
  status: 'UNREAD' | 'READ';
  notificationType: string;
  createdAt: string;
}

export const notificationService = {
  getNotifications: async (): Promise<InAppNotification[]> => {
    const response = await apiClient.get('/notifications/inbox');
    return response.data;
  },

  markAsRead: async (id: string): Promise<void> => {
    await apiClient.post(`/notifications/inbox/${id}/read`);
  }
};
