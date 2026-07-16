import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/common'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<UserInfo | null>(null)
  const role = ref<string>(localStorage.getItem('role') || '')

  const isAuthenticated = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    // Backend returns flat structure: { token, adminId, username, realName, role, avatar }
    // Frontend expects nested structure: { token, user: { id, username, realName, role, avatar } }
    const data = res.data as any
    const userInfo: UserInfo = {
      id: data.adminId,
      username: data.username,
      realName: data.realName,
      role: data.role,
      avatar: data.avatar,
    }
    token.value = data.token
    role.value = data.role
    user.value = userInfo
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
  }

  async function fetchUser() {
    try {
      const res = await authApi.getUserInfo()
      // Backend returns flat structure, map to UserInfo
      const data = res.data as any
      const userInfo: UserInfo = {
        id: data.adminId,
        username: data.username,
        realName: data.realName,
        role: data.role,
        avatar: data.avatar,
      }
      user.value = userInfo
      role.value = data.role
    } catch {
      // Token may be expired or invalid - clear auth state
      logout()
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
  }

  return { token, user, role, isAuthenticated, login, fetchUser, logout }
})
