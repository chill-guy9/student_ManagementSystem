<template>
  <div class="sidebar" :class="{ collapsed: sidebar.collapsed }">
    <div class="sidebar-header">
      <div class="logo" v-show="!sidebar.collapsed">
        <div class="logo-icon">S</div>
        <div class="logo-text">
          <span class="logo-title">SMS</span>
          <span class="logo-sub">Management</span>
        </div>
      </div>
      <div class="logo-mini" v-show="sidebar.collapsed">S</div>
      <button class="sidebar-toggle" @click="sidebar.toggle()" title="收起/展开">
        <el-icon :size="14">
          <Fold v-if="!sidebar.collapsed" />
          <Expand v-else />
        </el-icon>
      </button>
    </div>

    <nav class="sidebar-nav">
      <template v-for="group in navGroups" :key="group.label">
        <div class="nav-group" v-show="!sidebar.collapsed">
          <span class="nav-group-label">{{ group.label }}</span>
        </div>
        <router-link
          v-for="item in group.items"
          :key="item.name"
          :to="{ name: item.name }"
          class="nav-item"
          :class="{ active: isActive(item.name) }"
        >
          <span class="nav-label">{{ item.label }}</span>
          <span class="nav-glow" v-if="isActive(item.name)"></span>
        </router-link>
      </template>
    </nav>

    <div class="sidebar-footer" v-show="!sidebar.collapsed">
      <div class="user-info">
        <div class="user-avatar">{{ userInitial }}</div>
        <div class="user-details">
          <span class="user-name">{{ auth.user?.realName || auth.user?.username }}</span>
          <span class="user-role">{{ roleLabel }}</span>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSidebarStore } from '@/stores/sidebar'
import { Fold, Expand } from '@element-plus/icons-vue'

const route = useRoute()
const auth = useAuthStore()
const sidebar = useSidebarStore()

interface NavItem {
  name: string
  label: string
  icon: string
}

interface NavGroup {
  label: string
  items: NavItem[]
}

const navGroups = computed<NavGroup[]>(() => {
  const groups: NavGroup[] = [
    {
      label: '概览',
      items: [{ name: 'Dashboard', label: '仪表盘', icon: 'Monitor' }],
    },
    {
      label: '数据管理',
      items: [
        { name: 'Students', label: '学生管理', icon: 'User' },
        { name: 'Teachers', label: '教师管理', icon: 'Reading' },
      ],
    },
    {
      label: '系统管理',
      items: [
        { name: 'Admins', label: '管理员', icon: 'Key' },
        { name: 'Logs', label: '系统日志', icon: 'Document' },
        { name: 'Shell', label: '终端', icon: 'Monitor' },
        { name: 'Settings', label: '系统设置', icon: 'Setting' },
      ],
    },
  ]

  if (auth.role !== 'user_admin' && auth.role !== 'super_admin') {
    // log_auditor can see Logs page
    if (auth.role === 'log_auditor') {
      groups[2].items = groups[2].items.filter(i => i.name === 'Logs')
    } else {
      groups[2].items = groups[2].items.filter(i => i.name !== 'Admins' && i.name !== 'Shell' && i.name !== 'Logs' && i.name !== 'Settings')
    }
  }

  return groups
})

const isActive = (name: string) => route.name === name

const userInitial = computed(() => {
  const name = auth.user?.realName || auth.user?.username || 'U'
  return name.charAt(0).toUpperCase()
})

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    super_admin: '超级管理员',
    user_admin: '用户管理员',
    log_auditor: '日志审计员',
    read_only: '只读用户',
  }
  return map[auth.role] || auth.role
})
</script>

<style scoped>
.sidebar {
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  width: var(--sidebar-width);
  background: var(--bg-card);
  border-right: 1px solid var(--border-base);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-base);
  z-index: 100;
  overflow: hidden;
}

.sidebar.collapsed {
  width: var(--sidebar-collapsed-width);
}

.sidebar.collapsed .sidebar-header {
  flex-direction: column;
  gap: 8px;
  padding: 12px 8px;
}

.sidebar.collapsed .logo-mini {
  margin: 0;
}

.sidebar.collapsed .sidebar-toggle {
  margin-top: 4px;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-base);
  min-height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: var(--accent-cyan-dim);
  border: 1px solid var(--accent-cyan);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-cyan);
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 18px;
  box-shadow: var(--glow-sm);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-title {
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 16px;
  color: var(--text-primary);
  letter-spacing: 2px;
}

.logo-sub {
  font-size: 10px;
  color: var(--text-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
}

.logo-mini {
  width: 36px;
  height: 36px;
  background: var(--accent-cyan-dim);
  border: 1px solid var(--accent-cyan);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-cyan);
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 18px;
  box-shadow: var(--glow-sm);
  margin: 0 auto;
}

.sidebar-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.nav-group {
  padding: 16px 20px 6px;
}

.nav-group-label {
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--text-muted);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  margin: 2px 8px;
  border-radius: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--transition-base);
  position: relative;
  cursor: pointer;
}

.nav-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--accent-cyan-dim);
  color: var(--accent-cyan);
}

.nav-glow {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: var(--accent-cyan);
  border-radius: 0 3px 3px 0;
  box-shadow: var(--glow-sm);
}

.nav-label {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border-base);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--accent-cyan-dim);
  border: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--accent-cyan);
  font-family: var(--font-mono);
  font-weight: 600;
  font-size: 14px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.user-role {
  font-size: 11px;
  color: var(--text-muted);
}

.sidebar-toggle {
  width: 24px;
  height: 24px;
  background: transparent;
  border: 1px solid var(--border-light);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-muted);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.sidebar-toggle:hover {
  background: var(--accent-cyan-dim);
  border-color: var(--accent-cyan);
  color: var(--accent-cyan);
}
</style>
