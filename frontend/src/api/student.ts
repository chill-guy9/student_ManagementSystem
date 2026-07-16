import request from './request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { Student, StudentQuery, StudentForm } from '@/types/student'

export const studentApi = {
  getList(params: StudentQuery): Promise<ApiResponse<PageResult<Student>>> {
    return request.get('/students', { params })
  },

  getById(id: string): Promise<ApiResponse<Student>> {
    return request.get(`/students/${id}`)
  },

  create(data: StudentForm): Promise<ApiResponse<Student>> {
    return request.post('/students', data)
  },

  update(id: string, data: StudentForm): Promise<ApiResponse<Student>> {
    return request.put(`/students/${id}`, data)
  },

  delete(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/students/${id}`)
  },

  batchDelete(ids: string[]): Promise<ApiResponse<void>> {
    return request.post('/students/batch-delete', { ids })
  },

  export(params: StudentQuery): Promise<Blob> {
    return request.get('/students/export', { params, responseType: 'blob' })
  },
}
