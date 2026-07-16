export interface Admin {
  adminId: string
  username: string
  realName: string
  role: string
  roleLabel: string
  email: string
  phone: string
  avatar: string | null
  status: number
  lastLoginAt: string
  lastLoginIp: string
  createdAt: string
}

export interface AdminQuery {
  page: number
  pageSize: number
  keyword?: string
  role?: string
  status?: string
}

export interface AdminForm {
  username: string
  realName: string
  email: string
  phone: string
  role: string
  password?: string
}

export interface Permission {
  module: string
  actions: {
    view: boolean
    create: boolean
    edit: boolean
    delete: boolean
  }
}

export const ROLE_LABELS: Record<string, string> = {
  super_admin: '超级管理员',
  user_admin: '用户管理员',
  log_auditor: '日志审计员',
  read_only: '只读用户',
}

export const ROLE_COLORS: Record<string, string> = {
  super_admin: '#ef4444',
  user_admin: '#00e5ff',
  log_auditor: '#f59e0b',
  read_only: '#64748b',
}
