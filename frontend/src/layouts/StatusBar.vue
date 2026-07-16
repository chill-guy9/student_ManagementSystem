<template>
  <div class="status-bar">
    <div class="status-left">
      <div class="breadcrumb">
        <span class="breadcrumb-icon">
          <el-icon :size="14"><Location /></el-icon>
        </span>
        <span class="breadcrumb-text">{{ currentTitle }}</span>
      </div>
    </div>

    <div class="status-right">
      <div class="online-indicator" :class="{ online: isOnline }">
        <span class="dot"></span>
        <span class="status-text">{{ isOnline ? '在线' : '离线' }}</span>
      </div>
      <div class="clock">{{ currentTime }}</div>

      <!-- Notification Bell -->
      <el-popover
        placement="bottom-end"
        :width="360"
        trigger="click"
        :offset="8"
        popper-class="notification-popover"
      >
        <template #reference>
          <div class="action-btn bell-trigger">
            <Bell :size="16" />
            <span class="bell-badge" v-if="notificationStore.unreadCount > 0">
              {{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}
            </span>
          </div>
        </template>
        <NotificationPanel />
      </el-popover>

      <!-- Fullscreen Toggle -->
      <div class="action-btn fullscreen-trigger" @click="toggleFullscreen" :title="isFullscreen ? '退出全屏' : '全屏'">
        <Minimize2 :size="15" v-if="isFullscreen" />
        <Maximize2 :size="15" v-else />
      </div>

      <!-- User Avatar Dropdown -->
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-trigger">
          <div class="user-avatar-sm">{{ userInitial }}</div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="password">修改密码</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- Profile Dialog -->
    <el-dialog v-model="profileVisible" title="个人信息" width="480px" :append-to-body="true">
      <div class="profile-content" v-if="auth.user">
        <div class="profile-header">
          <div class="profile-avatar">{{ userInitial }}</div>
          <div class="profile-identity">
            <h3>{{ auth.user.realName }}</h3>
            <span class="profile-username">@{{ auth.user.username }}</span>
          </div>
        </div>
        <div class="profile-grid">
          <div class="profile-item">
            <span class="profile-label">用户名</span>
            <span class="profile-value">{{ auth.user.username }}</span>
          </div>
          <div class="profile-item">
            <span class="profile-label">姓名</span>
            <span class="profile-value">{{ auth.user.realName }}</span>
          </div>
          <div class="profile-item">
            <span class="profile-label">角色</span>
            <span class="profile-value">
              <el-tag size="small" effect="dark" :color="roleColor" style="border: none; color: #fff">
                {{ roleLabel }}
              </el-tag>
            </span>
          </div>
          <div class="profile-item">
            <span class="profile-label">邮箱</span>
            <span class="profile-value">{{ auth.user.email || '—' }}</span>
          </div>
          <div class="profile-item">
            <span class="profile-label">最后登录</span>
            <span class="profile-value">{{ auth.user.lastLoginAt || '—' }}</span>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- Change Password Dialog -->
    <el-dialog v-model="passwordVisible" title="修改密码" width="440px" :append-to-body="true">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（6-20位）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="pwdLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notification'
import { Location } from '@element-plus/icons-vue'
import { Bell, Maximize2, Minimize2 } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { authApi } from '@/api/auth'
import NotificationPanel from '@/components/common/NotificationPanel.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const notificationStore = useNotificationStore()

const currentTime = ref('')
const isOnline = ref(navigator.onLine)
const isFullscreen = ref(false)
const profileVisible = ref(false)
const passwordVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref<FormInstance>()

const ROLE_LABELS: Record<string, string> = {
  super_admin: '超级管理员',
  admin: '管理员',
  read_only: '只读用户',
}

const ROLE_COLORS: Record<string, string> = {
  super_admin: '#ef4444',
  admin: '#00e5ff',
  read_only: '#64748b',
}

const roleLabel = computed(() => ROLE_LABELS[auth.user?.role || ''] || auth.user?.role || '未知')
const roleColor = computed(() => ROLE_COLORS[auth.user?.role || ''] || '#64748b')

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== pwdForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const currentTitle = computed(() => {
  return (route.meta.title as string) || '仪表盘'
})

const userInitial = computed(() => {
  const name = auth.user?.realName || auth.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

// ===== Fullscreen =====
function toggleFullscreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

function handleFullscreenChange() {
  isFullscreen.value = !!document.fullscreenElement
}

// ===== Command Handler =====
async function handleCommand(command: string) {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      notificationStore.disconnectWebSocket()
      auth.logout()
      router.push({ name: 'Login' })
    } catch {}
  } else if (command === 'profile') {
    profileVisible.value = true
  } else if (command === 'password') {
    Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
    passwordVisible.value = true
    nextTick(() => pwdFormRef.value?.clearValidate())
  }
}

async function handleChangePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return

  pwdLoading.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    auth.logout()
    router.push({ name: 'Login' })
  } catch {
    // error handled by interceptor
  } finally {
    pwdLoading.value = false
  }
}

// ===== Clock & Online =====
let timer: ReturnType<typeof setInterval>
function updateClock() {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

function handleOnline() { isOnline.value = true }
function handleOffline() { isOnline.value = false }

onMounted(() => {
  updateClock()
  timer = setInterval(updateClock, 1000)
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
  document.addEventListener('fullscreenchange', handleFullscreenChange)

  // Initialize notifications
  notificationStore.fetchUnreadCount()
  notificationStore.connectWebSocket()
})

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  document.removeEventListener('fullscreenchange', handleFullscreenChange)
  notificationStore.disconnectWebSocket()
})
</script>

<style scoped>
.status-bar {
  height: var(--statusbar-height);
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-base);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.status-left, .status-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 6px;
}

.breadcrumb-icon {
  color: var(--accent-cyan);
}

.breadcrumb-text {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.online-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}

.online-indicator.online .dot {
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
  animation: pulse-dot 2s ease-in-out infinite;
}

.status-text {
  color: var(--text-muted);
  font-size: 12px;
}

.clock {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
  min-width: 70px;
}

/* Action Buttons (Bell, Fullscreen) */
.action-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  position: relative;
}

.action-btn:hover {
  color: var(--accent-cyan);
  background: var(--accent-cyan-dim);
}

/* Bell Badge */
.bell-trigger {
  position: relative;
}

.bell-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: var(--color-error);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.4);
}

/* User Avatar */
.user-trigger {
  cursor: pointer;
}

.user-avatar-sm {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: var(--accent-cyan-dim);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-cyan);
  font-family: var(--font-mono);
  font-weight: 600;
  font-size: 12px;
  transition: all var(--transition-fast);
}

.user-avatar-sm:hover {
  border-color: var(--accent-cyan);
  box-shadow: var(--glow-sm);
}

/* Profile Dialog */
.profile-content {
  padding: 0 4px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-base);
}

.profile-avatar {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: var(--accent-cyan-dim);
  border: 1px solid rgba(0, 229, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-cyan);
  font-size: 22px;
  font-weight: 700;
  flex-shrink: 0;
}

.profile-identity h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.profile-username {
  font-size: 13px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.profile-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.profile-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.profile-value {
  font-size: 14px;
  color: var(--text-primary);
}

/* Animations */
@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}
</style>
