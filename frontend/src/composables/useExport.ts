import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import type { StudentQuery } from '@/types/student'
import type { TeacherQuery } from '@/types/teacher'
import { studentApi } from '@/api/student'
import { teacherApi } from '@/api/teacher'

export function useExport() {
  async function exportStudents(params: StudentQuery) {
    try {
      const blob = await studentApi.export(params)
      downloadBlob(blob, `学生数据_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
      ElMessage.success('导出成功')
    } catch {
      ElMessage.error('导出失败')
    }
  }

  async function exportTeachers(params: TeacherQuery) {
    try {
      const blob = await teacherApi.export(params)
      downloadBlob(blob, `教师数据_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`)
      ElMessage.success('导出成功')
    } catch {
      ElMessage.error('导出失败')
    }
  }

  function downloadBlob(blob: Blob, filename: string) {
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  }

  return { exportStudents, exportTeachers, downloadBlob }
}
