import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notificationApi, type NotificationVO } from '@/api/notification'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const notifications = ref<NotificationVO[]>([])
  const loading = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let pingInterval: ReturnType<typeof setInterval> | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 10

  async function fetchUnreadCount() {
    try {
      const res = await notificationApi.getUnreadCount()
      unreadCount.value = res.data as number
    } catch {}
  }

  async function fetchNotifications(unread = false) {
    loading.value = true
    try {
      const res = await notificationApi.list({ unread, page: 1, pageSize: 20 })
      const pageResult = res.data as any
      notifications.value = pageResult.records || []
    } catch {} finally {
      loading.value = false
    }
  }

  async function markAsRead(notificationId: string) {
    try {
      await notificationApi.markAsRead(notificationId)
      const item = notifications.value.find(n => n.notificationId === notificationId)
      if (item) item.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch {}
  }

  async function markAllAsRead() {
    try {
      await notificationApi.markAllAsRead()
      notifications.value.forEach(n => n.isRead = 1)
      unreadCount.value = 0
    } catch {}
  }

  async function deleteNotification(notificationId: string) {
    try {
      await notificationApi.deleteNotification(notificationId)
      const item = notifications.value.find(n => n.notificationId === notificationId)
      if (item && item.isRead === 0) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      notifications.value = notifications.value.filter(n => n.notificationId !== notificationId)
    } catch {}
  }

  function clearPingInterval() {
    if (pingInterval) {
      clearInterval(pingInterval)
      pingInterval = null
    }
  }

  function connectWebSocket() {
    const token = localStorage.getItem('token')
    if (!token) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = `${protocol}//${host}/ws/notifications?token=${token}`

    ws = new WebSocket(url)

    ws.onopen = () => {
      reconnectAttempts = 0
      // Clear any previous ping interval before creating a new one
      clearPingInterval()
      // Send ping periodically
      pingInterval = setInterval(() => {
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'ping' }))
        } else {
          clearPingInterval()
        }
      }, 30000)
    }

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'notification.new') {
          const data = msg.data
          const newNotif: NotificationVO = {
            notificationId: data.notificationId,
            title: data.title,
            content: data.content,
            type: data.type,
            typeLabel: getTypeLabel(data.type),
            isRead: 0,
            createdAt: data.createdAt,
          }
          notifications.value.unshift(newNotif)
          unreadCount.value++
        }
      } catch {}
    }

    ws.onclose = () => {
      clearPingInterval()
      // Reconnect with backoff
      attemptReconnect()
    }

    ws.onerror = () => {
      ws?.close()
    }
  }

  function attemptReconnect() {
    if (reconnectAttempts >= maxReconnectAttempts) {
      return
    }
    const delay = Math.min(2000 * (reconnectAttempts + 1), 30000) // max 30s
    reconnectTimer = setTimeout(() => {
      reconnectAttempts++
      connectWebSocket()
    }, delay)
  }

  function disconnectWebSocket() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    clearPingInterval()
    reconnectAttempts = maxReconnectAttempts // prevent auto-reconnect
    if (ws) {
      ws.close()
      ws = null
    }
  }

  function getTypeLabel(type: string): string {
    const labels: Record<string, string> = {
      SYSTEM: '系统通知',
      LOGIN: '登录通知',
      OPERATION: '操作通知',
      BACKUP: '备份通知',
      ALERT: '告警通知',
    }
    return labels[type] || type
  }

  return {
    unreadCount,
    notifications,
    loading,
    fetchUnreadCount,
    fetchNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    connectWebSocket,
    disconnectWebSocket,
  }
})
