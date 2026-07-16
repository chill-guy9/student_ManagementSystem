export interface Teacher {
  teacherId: string
  name: string
  gender: number
  genderLabel: string
  birthDate: string
  phone: string
  email: string
  department: string
  title: string
  avatar: string | null
  status: number
  createdAt: string
  updatedAt: string
  courses: TeacherCourse[]
}

export interface TeacherCourse {
  id: number
  courseCode: string
  courseName: string
  semester: string
  hours: number
}

export interface TeacherQuery {
  page: number
  pageSize: number
  keyword?: string
  department?: string
  title?: string
  status?: string
}

export interface TeacherForm {
  name: string
  gender: number
  birthDate: string
  phone: string
  email: string
  department: string
  title: string
  status: number
}
