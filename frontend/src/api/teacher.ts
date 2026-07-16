import request from './request'
import type { ApiResponse, PageResult } from '@/types/common'
import type { Teacher, TeacherQuery, TeacherForm, TeacherCourse } from '@/types/teacher'

export const teacherApi = {
  getList(params: TeacherQuery): Promise<ApiResponse<PageResult<Teacher>>> {
    return request.get('/teachers', { params })
  },

  getById(id: string): Promise<ApiResponse<Teacher>> {
    return request.get(`/teachers/${id}`)
  },

  create(data: TeacherForm): Promise<ApiResponse<Teacher>> {
    return request.post('/teachers', data)
  },

  update(id: string, data: TeacherForm): Promise<ApiResponse<Teacher>> {
    return request.put(`/teachers/${id}`, data)
  },

  delete(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/teachers/${id}`)
  },

  getCourses(teacherId: string): Promise<ApiResponse<TeacherCourse[]>> {
    return request.get(`/teachers/${teacherId}/courses`)
  },

  addCourse(teacherId: string, data: Partial<TeacherCourse>): Promise<ApiResponse<TeacherCourse>> {
    return request.post(`/teachers/${teacherId}/courses`, data)
  },

  removeCourse(teacherId: string, courseId: number): Promise<ApiResponse<void>> {
    return request.delete(`/teachers/${teacherId}/courses/${courseId}`)
  },

  export(params: TeacherQuery): Promise<Blob> {
    return request.get('/teachers/export', { params, responseType: 'blob' })
  },
}
