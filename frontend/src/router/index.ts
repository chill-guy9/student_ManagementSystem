import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login/index.vue'),
    meta: { title: '登录', requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/Register/index.vue'),
    meta: { title: '注册', requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/Dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Monitor', group: '概览' },
      },
      {
        path: 'students',
        name: 'Students',
        component: () => import('@/pages/Students/index.vue'),
        meta: { title: '学生管理', icon: 'User', group: '数据管理' },
      },
      {
        path: 'teachers',
        name: 'Teachers',
        component: () => import('@/pages/Teachers/index.vue'),
        meta: { title: '教师管理', icon: 'Reading', group: '数据管理' },
      },
      {
        path: 'admins',
        name: 'Admins',
        component: () => import('@/pages/Admins/index.vue'),
        meta: { title: '管理员管理', icon: 'Key', group: '系统管理', permission: 'admin' },
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/pages/Logs/index.vue'),
        meta: { title: '系统日志', icon: 'Document', group: '系统管理', permission: 'admin' },
      },
      {
        path: 'shell',
        name: 'Shell',
        component: () => import('@/pages/Shell/index.vue'),
        meta: { title: '终端', icon: 'Monitor', group: '系统管理', permission: 'admin' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/pages/Settings/index.vue'),
        meta: { title: '系统设置', icon: 'Setting', group: '系统管理', permission: 'admin' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth !== false && !auth.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  if (to.name === 'Login' && auth.isAuthenticated) {
    next({ name: 'Dashboard' })
    return
  }

  if (to.meta.permission === 'admin' && auth.role !== 'user_admin' && auth.role !== 'super_admin') {
    next({ name: 'Dashboard' })
    return
  }

  next()
})

export default router
