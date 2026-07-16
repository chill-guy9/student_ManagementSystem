import request from './request'
import type { ApiResponse, PageResult } from '@/types/common'

export interface NotificationVO {
  notificationId: string
  title: string
  content: string
  type: string
  typeLabel: string
  isRead: number
  createdAt: string
}

export const notificationApi = {
  list(params: { unread?: boolean; page?: number; pageSize?: number }): Promise<ApiResponse<PageResult<NotificationVO>>> {
    return request.get('/notifications', { params })
  },

  getUnreadCount(): Promise<ApiResponse<number>> {
    return request.get('/notifications/unread-count')
  },

  markAsRead(notificationId: string): Promise<ApiResponse<void>> {
    return request.put(`/notifications/${notificationId}/read`)
  },

  markAllAsRead(): Promise<ApiResponse<void>> {
    return request.put('/notifications/read-all')
  },

  deleteNotification(notificationId: string): Promise<ApiResponse<void>> {
    return request.delete(`/notifications/${notificationId}`)
  },
}
