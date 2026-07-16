import request from './request'
import type { ApiResponse } from '@/types/common'

export interface BackupRecordVO {
  id: number
  fileName: string
  fileSize: number
  status: string
  triggerType: string
  startedAt: string
  finishedAt: string
}

export interface BackupStatusVO {
  autoBackupEnabled: boolean
  backupRetentionDays: number
  latestBackup: BackupRecordVO | null
}

export const backupApi = {
  getStatus(): Promise<ApiResponse<BackupStatusVO>> {
    return request.get('/backup/status')
  },

  create(): Promise<ApiResponse<number>> {
    return request.post('/backup/execute')
  },

  restore(backupId: number): Promise<ApiResponse<void>> {
    return request.post('/backup/restore', { backupId })
  },
}
