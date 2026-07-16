import { useAuthStore } from '@/stores/auth'

export function hasPermission(permission: string): boolean {
  const auth = useAuthStore()
  if (auth.role === 'super_admin') return true
  if (auth.role === 'user_admin') {
    return permission !== 'super_admin'
  }
  return false
}

export function isAdmin(): boolean {
  const auth = useAuthStore()
  return auth.role === 'user_admin' || auth.role === 'super_admin'
}

export function isSuperAdmin(): boolean {
  const auth = useAuthStore()
  return auth.role === 'super_admin'
}

export function canView(module: string): boolean {
  return hasPermission(`${module}:view`)
}

export function canCreate(module: string): boolean {
  return hasPermission(`${module}:create`)
}

export function canEdit(module: string): boolean {
  return hasPermission(`${module}:edit`)
}

export function canDelete(module: string): boolean {
  return hasPermission(`${module}:delete`)
}
