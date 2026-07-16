<template>
  <div class="notification-panel">
    <div class="panel-header">
      <div class="header-left">
        <span class="header-title">通知</span>
        <span class="unread-badge" v-if="notificationStore.unreadCount > 0">
          {{ notificationStore.unreadCount }}
        </span>
      </div>
      <el-button
        link
        type="primary"
        size="small"
        @click="notificationStore.markAllAsRead()"
        :disabled="notificationStore.unreadCount === 0"
      >
        全部已读
      </el-button>
    </div>

    <div class="panel-body" v-loading="notificationStore.loading">
      <div v-if="notificationStore.notifications.length === 0" class="empty-state">
        <span class="empty-text">暂无通知</span>
      </div>
      <div
        v-for="item in notificationStore.notifications"
        :key="item.notificationId"
        class="notification-item"
        :class="{ unread: item.isRead === 0 }"
        @click="handleItemClick(item)"
      >
        <div class="item-indicator" :class="`type-${item.type.toLowerCase()}`"></div>
        <div class="item-content">
          <div class="item-header">
            <span class="item-type">{{ item.typeLabel }}</span>
            <span class="item-time">{{ formatTime(item.createdAt) }}</span>
          </div>
          <div class="item-title">{{ item.title }}</div>
          <div class="item-desc" v-if="item.content">{{ item.content }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import type { NotificationVO } from '@/api/notification'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const notificationStore = useNotificationStore()

function formatTime(time: string): string {
  return dayjs(time).fromNow()
}

function handleItemClick(item: NotificationVO) {
  if (item.isRead === 0) {
    notificationStore.markAsRead(item.notificationId)
  }
}

onMounted(() => {
  notificationStore.fetchNotifications()
})
</script>

<style scoped>
.notification-panel {
  width: 360px;
  max-height: 440px;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-base);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-family: var(--font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.unread-badge {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: var(--color-error);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.panel-body::-webkit-scrollbar {
  width: 4px;
}

.panel-body::-webkit-scrollbar-thumb {
  background: var(--border-light);
  border-radius: 2px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
}

.empty-text {
  font-size: 13px;
  color: var(--text-muted);
}

.notification-item {
  display: flex;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background var(--transition-fast);
  border-bottom: 1px solid var(--border-base);
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--bg-hover);
}

.notification-item.unread {
  background: rgba(0, 229, 255, 0.03);
}

.item-indicator {
  width: 3px;
  border-radius: 2px;
  flex-shrink: 0;
  margin-top: 2px;
  margin-bottom: 2px;
}

.notification-item.unread .item-indicator {
  background: var(--accent-cyan);
  box-shadow: 0 0 6px var(--accent-cyan);
}

.notification-item:not(.unread) .item-indicator {
  background: var(--border-light);
}

.type-system .item-indicator { background: var(--color-info); }
.type-login .item-indicator { background: var(--color-success); }
.type-operation .item-indicator { background: var(--accent-cyan); }
.type-backup .item-indicator { background: var(--color-warning); }
.type-alert .item-indicator { background: var(--color-error); }

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.item-type {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.item-time {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
  flex-shrink: 0;
}

.item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-item:not(.unread) .item-title {
  color: var(--text-secondary);
}

.item-desc {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
