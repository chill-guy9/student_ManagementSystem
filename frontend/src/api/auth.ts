import request from './request'
import type { ApiResponse, LoginParams, LoginResult, UserInfo } from '@/types/common'

export interface RegisterParams {
  username: string
  realName: string
  email: string
  phone: string
  password: string
}

export interface ChangePasswordParams {
  oldPassword: string
  newPassword: string
}

export const authApi = {
  login(data: LoginParams): Promise<ApiResponse<LoginResult>> {
    return request.post('/auth/login', data)
  },

  register(data: RegisterParams): Promise<ApiResponse<void>> {
    return request.post('/auth/register', data)
  },

  getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return request.get('/auth/user')
  },

  logout(): Promise<ApiResponse<void>> {
    return request.post('/auth/logout')
  },

  changePassword(data: ChangePasswordParams): Promise<ApiResponse<void>> {
    return request.put('/auth/change-password', data)
  },
}
