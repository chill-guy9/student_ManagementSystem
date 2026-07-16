export interface LogEntry {
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
  fieldChanges?: FieldChange[]
}

export interface FieldChange {
  fieldName: string
  fieldLabel: string
  oldValue: string | null
  newValue: string | null
}

export interface LogQuery {
  page: number
  pageSize: number
  keyword?: string
  level?: string[]
  operation?: string
  module?: string
  startTime?: string
  endTime?: string
  username?: string
}

export const LOG_LEVELS = [
  { value: 'info', label: 'INFO' },
  { value: 'warn', label: 'WARN' },
  { value: 'error', label: 'ERROR' },
  { value: 'debug', label: 'DEBUG' },
]

export const LOG_LEVEL_CONFIG: Record<string, { color: string; label: string }> = {
  info: { color: '#3b82f6', label: 'INFO' },
  warn: { color: '#f59e0b', label: 'WARN' },
  error: { color: '#ef4444', label: 'ERROR' },
  debug: { color: '#64748b', label: 'DEBUG' },
}

export const METHOD_COLORS: Record<string, string> = {
  GET: '#10b981',
  POST: '#3b82f6',
  PUT: '#f59e0b',
  DELETE: '#ef4444',
  PATCH: '#8b5cf6',
}
