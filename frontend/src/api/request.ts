import axios from 'axios'
import type { ApiResponse } from '@/types/common'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
})

request.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response) => {
    // For blob responses (file downloads), skip ApiResponse validation and return raw data
    if (response.config.responseType === 'blob') {
      return response.data as any
    }
    const data = response.data as ApiResponse
    if (data.code !== 0 && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return response.data as any
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const data = error.response.data as ApiResponse | undefined
      if (status === 401) {
        // Only redirect to login if NOT on the login page itself
        // (login failures also return 401 for wrong credentials)
        const currentPath = router.currentRoute.value.path
        if (currentPath !== '/login') {
          const auth = useAuthStore()
          auth.logout()
          router.push({ name: 'Login' })
          ElMessage.error('登录已过期，请重新登录')
        } else {
          // On login page, show the actual error message from backend
          ElMessage.error(data?.message || '用户名或密码错误')
        }
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作')
      } else if (status === 500) {
        ElMessage.error('服务器错误，请稍后重试')
      } else {
        ElMessage.error(data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export default request
