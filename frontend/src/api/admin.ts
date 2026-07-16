import request from './request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { Admin, AdminQuery, AdminForm, Permission } from '@/types/admin'

export const adminApi = {
  getList(params: AdminQuery): Promise<ApiResponse<PageResult<Admin>>> {
    return request.get('/admins', { params })
  },

  getById(id: string): Promise<ApiResponse<Admin>> {
    return request.get(`/admins/${id}`)
  },

  create(data: AdminForm): Promise<ApiResponse<Admin>> {
    return request.post('/admins', data)
  },

  update(id: string, data: AdminForm): Promise<ApiResponse<Admin>> {
    return request.put(`/admins/${id}`, data)
  },

  delete(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/admins/${id}`)
  },

  toggleStatus(id: string): Promise<ApiResponse<void>> {
    return request.put(`/admins/${id}/toggle-status`)
  },

  getPermissions(role: string): Promise<ApiResponse<Permission[]>> {
    return request.get('/admins/permissions', { params: { role } })
  },

  updatePermissions(role: string, permissions: Permission[]): Promise<ApiResponse<void>> {
    return request.put('/admins/permissions', { role, permissions })
  },
}
