import request from './request'
import type { ApiResponse } from '@/types/common'

export interface SystemInfoVO {
  appName: string
  version: string
  javaVersion: string
  osName: string
  osArch: string
  uptime: number
  activeConnections: number
  serverTime: string
}

export const systemApi = {
  getInfo(): Promise<ApiResponse<SystemInfoVO>> {
    return request.get('/system/info')
  },
}
