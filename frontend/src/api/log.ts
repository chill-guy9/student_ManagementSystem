import request from './request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { LogEntry, LogQuery } from '@/types/log'

export interface LogStats {
  levelDistribution: Array<{ level: string; cnt: number }>
  hourlyDistribution: Array<{ hour: number; cnt: number }>
  topOperators: Array<{ operator_name: string; cnt: number }>
}

export const logApi = {
  getList(params: LogQuery): Promise<ApiResponse<PageResult<LogEntry>>> {
    // Convert level array to comma-separated string for backend
    const query: Record<string, any> = { ...params }
    if (Array.isArray(query.level) && query.level.length > 0) {
      query.level = query.level.join(',')
    } else {
      delete query.level
    }
    return request.get('/logs', { params: query })
  },

  getById(id: string): Promise<ApiResponse<LogEntry>> {
    return request.get(`/logs/${id}`)
  },

  getRelatedLogs(id: string): Promise<ApiResponse<LogEntry[]>> {
    return request.get(`/logs/${id}/related`)
  },

  getStats(startDate?: string, endDate?: string): Promise<ApiResponse<LogStats>> {
    const params: Record<string, string> = {}
    if (startDate) params.startDate = startDate
    if (endDate) params.endDate = endDate
    return request.get('/logs/stats', { params })
  },

  clean(days: number): Promise<ApiResponse<void>> {
    return request.delete('/logs/cleanup', { params: { retentionDays: days } })
  },

  export(params: LogQuery): Promise<Blob> {
    const query: Record<string, any> = { ...params }
    if (Array.isArray(query.level) && query.level.length > 0) {
      query.level = query.level.join(',')
    } else {
      delete query.level
    }
    return request.get('/logs/export', { params: query, responseType: 'blob' })
  },
}
