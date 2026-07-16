export interface ApiResponse<T = any> {
  code: number
  data: T
  message: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

export interface PageQuery {
  page: number
  pageSize: number
  keyword?: string
}

export interface UserInfo {
  id: string
  username: string
  realName: string
  role: string
  avatar?: string
  email?: string
  lastLoginAt?: string
}

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  user: UserInfo
}

export interface SelectOption {
  label: string
  value: string | number
}
