export interface Student {
  studentId: string
  name: string
  gender: number
  genderLabel: string
  birthDate: string
  phone: string
  email: string
  grade: string
  major: string
  className: string
  status: string
  statusLabel: string
  enrollmentDate: string
  address: string
  avatar: string | null
  createdAt: string
  updatedAt: string
}

export interface StudentQuery {
  page: number
  pageSize: number
  keyword?: string
  grade?: string
  major?: string
  status?: string
}

export interface StudentForm {
  name: string
  gender: number
  birthDate: string
  phone: string
  email: string
  grade: string
  major: string
  className: string
  status: string
  enrollmentDate: string
  address: string
}
