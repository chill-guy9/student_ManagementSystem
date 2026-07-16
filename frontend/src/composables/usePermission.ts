import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  const isSuperAdmin = computed(() => auth.role === 'super_admin')
  const isAdmin = computed(() => auth.role === 'user_admin' || auth.role === 'super_admin')
  const isLogAuditor = computed(() => auth.role === 'log_auditor')
  const isReadOnly = computed(() => auth.role === 'read_only')

  function canAccess(module: string): boolean {
    if (isSuperAdmin.value) return true
    if (isAdmin.value) return true
    if (isLogAuditor.value) return module === 'log' || module === 'student' || module === 'teacher'
    if (isReadOnly.value) return module !== 'admin' && module !== 'backup' && module !== 'shell'
    return false
  }

  function canPerform(module: string, action: string): boolean {
    if (isSuperAdmin.value) return true
    if (isAdmin.value) {
      if (action === 'delete' && module === 'admin') return false
      if (module === 'shell' || module === 'backup') return false
      return true
    }
    if (isLogAuditor.value) {
      if (module === 'log') return action === 'view' || action === 'edit'
      return action === 'view' || action === 'export'
    }
    if (isReadOnly.value) return action === 'view' || action === 'export'
    return false
  }

  return { isSuperAdmin, isAdmin, isLogAuditor, isReadOnly, canAccess, canPerform }
}
