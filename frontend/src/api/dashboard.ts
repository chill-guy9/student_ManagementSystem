import request from './request'
import type { ApiResponse } from '@/types/common'

export interface DashboardStats {
  totalStudents: number
  totalTeachers: number
  totalAdmins: number
  totalLogs: number
  todayLogs: number
  activeStudents: number
  activeTeachers: number
}

export interface GrowthTrend {
  date: string
  students: number
  teachers: number
  admins: number
}

export interface LogTypeDistribution {
  operationType: string
  count: number
}

export interface SystemLoad {
  cpuUsage: number
  memoryUsage: number
  totalMemory: number
  usedMemory: number
  freeMemory: number
  diskTotal: number
  diskUsed: number
  diskFree: number
  diskUsage: number
}

export interface RecentActivity {
  logId: string
  operatorId: string
  operatorName: string
  operationType: string
  operationTypeLabel: string
  targetType: string
  targetId: string
  targetName: string
  detail: string
  ip: string
  level: string
  createdAt: string
}

export const dashboardApi = {
  getStats(): Promise<ApiResponse<DashboardStats>> {
    return request.get('/dashboard/stats')
  },

  getGrowthTrend(days?: number): Promise<ApiResponse<GrowthTrend[]>> {
    return request.get('/dashboard/user-growth', { params: { days } })
  },

  getLogTypeDistribution(days?: number): Promise<ApiResponse<LogTypeDistribution[]>> {
    return request.get('/dashboard/log-distribution', { params: { days } })
  },

  getSystemLoad(): Promise<ApiResponse<SystemLoad>> {
    return request.get('/dashboard/system-load')
  },

  getRecentActivities(limit?: number): Promise<ApiResponse<RecentActivity[]>> {
    return request.get('/dashboard/recent-logs', { params: { limit } })
  },
}
