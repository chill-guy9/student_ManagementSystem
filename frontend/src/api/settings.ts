import request from './request'
import type { ApiResponse } from '@/types/common'

export interface NotificationSettings {
  configs: Record<string, string>
}

export const settingsApi = {
  getNotificationSettings(): Promise<ApiResponse<NotificationSettings>> {
    return request.get('/settings/notifications')
  },

  updateNotificationConfig(data: Record<string, string>): Promise<ApiResponse<void>> {
    return request.put('/settings/notifications', { configs: data })
  },
}
