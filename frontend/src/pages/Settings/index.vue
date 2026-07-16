<template>
  <div class="settings-page">
    <PageHeader title="系统设置" subtitle="配置系统参数与偏好" />

    <div class="settings-grid">
      <!-- System Info -->
      <GlowCard color="cyan" class="settings-card">
        <template #header>
          <div class="card-header-row">
            <span class="card-title">系统信息</span>
            <el-button link type="primary" size="small" @click="refreshInfo">
              <el-icon><Refresh /></el-icon>
            </el-button>
          </div>
        </template>
        <div class="info-grid" v-if="sysInfo">
          <div class="info-item" v-for="item in infoItems" :key="item.label">
            <span class="info-label">{{ item.label }}</span>
            <span class="info-value" :class="{ mono: item.mono }">{{ item.value }}</span>
          </div>
        </div>
        <div v-else class="loading-info">加载中...</div>
      </GlowCard>

      <!-- Appearance -->
      <GlowCard color="blue" class="settings-card">
        <template #header>
          <span class="card-title">外观设置</span>
        </template>
        <div class="setting-list">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">暗色模式</span>
              <span class="setting-desc">使用深色背景主题</span>
            </div>
            <el-switch v-model="isDark" @change="toggleTheme" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">侧边栏折叠</span>
              <span class="setting-desc">默认折叠侧边栏</span>
            </div>
            <el-switch v-model="sidebar.collapsed" />
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">终端面板</span>
              <span class="setting-desc">显示底部终端面板</span>
            </div>
            <el-switch :model-value="shell.visible" @change="shell.visible = $event" />
          </div>
        </div>
      </GlowCard>

      <!-- Notification Config -->
      <GlowCard color="green" class="settings-card">
        <template #header>
          <span class="card-title">通知配置</span>
        </template>
        <div class="setting-list">
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">邮件通知</span>
              <span class="setting-desc">系统事件邮件推送</span>
            </div>
            <div class="setting-control">
              <el-switch v-model="notifConfig.email.enabled" />
              <el-select v-model="notifConfig.email.frequency" size="small" style="width: 100px; margin-left: 8px" :disabled="!notifConfig.email.enabled">
                <el-option v-for="f in NOTIFICATION_FREQUENCIES" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
            </div>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">站内通知</span>
              <span class="setting-desc">浏览器内消息提醒</span>
            </div>
            <div class="setting-control">
              <el-switch v-model="notifConfig.inApp.enabled" />
              <el-select v-model="notifConfig.inApp.frequency" size="small" style="width: 100px; margin-left: 8px" :disabled="!notifConfig.inApp.enabled">
                <el-option v-for="f in NOTIFICATION_FREQUENCIES" :key="f.value" :label="f.label" :value="f.value" />
              </el-select>
            </div>
          </div>
          <div class="setting-item">
            <div class="setting-info">
              <span class="setting-name">Webhook</span>
              <span class="setting-desc">推送到外部服务</span>
            </div>
            <div class="setting-control">
              <el-switch v-model="notifConfig.webhook.enabled" />
            </div>
          </div>
          <div class="setting-item" v-if="notifConfig.webhook.enabled">
            <div class="setting-info">
              <span class="setting-name">Webhook URL</span>
            </div>
            <el-input v-model="notifConfig.webhook.url" placeholder="https://..." size="small" style="flex: 1; margin-left: 16px" />
          </div>
        </div>
        <template #footer>
          <el-button type="primary" size="small" @click="saveNotifications">保存通知配置</el-button>
        </template>
      </GlowCard>

      <!-- Data Backup -->
      <GlowCard color="yellow" class="settings-card">
        <template #header>
          <span class="card-title">数据备份</span>
        </template>
        <div class="backup-status" v-if="latestBackup">
          <div class="backup-info">
            <StatusDot :status="latestBackup.status === 'SUCCESS' ? 'online' : latestBackup.status === 'FAILED' ? 'error' : 'away'" />
            <div>
              <span class="backup-label">最近备份</span>
              <span class="backup-time">{{ latestBackup.startedAt }}</span>
            </div>
          </div>
          <span class="backup-size">{{ formatFileSize(latestBackup.fileSize) }}</span>
        </div>
        <div class="backup-actions">
          <el-button type="primary" @click="createBackup" :loading="backupLoading" :icon="Download">
            手动备份
          </el-button>
          <el-button v-if="latestBackup" @click="restoreBackup(latestBackup)" :icon="FolderOpened">
            恢复最近备份
          </el-button>
        </div>
      </GlowCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Refresh, Download, FolderOpened } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/common/PageHeader.vue'
import GlowCard from '@/components/ui/GlowCard.vue'
import StatusDot from '@/components/ui/StatusDot.vue'
import { useSidebarStore } from '@/stores/sidebar'
import { useShellStore } from '@/stores/shell'
import { useTheme } from '@/composables/useTheme'
import { systemApi, type SystemInfoVO } from '@/api/system'
import { settingsApi } from '@/api/settings'
import { backupApi, type BackupStatusVO, type BackupRecordVO } from '@/api/backup'
import { formatDate, formatFileSize } from '@/utils/formatters'
import { NOTIFICATION_FREQUENCIES } from '@/utils/constants'

const sidebar = useSidebarStore()
const shell = useShellStore()
const { isDark, toggleTheme } = useTheme()
const sysInfo = ref<SystemInfoVO | null>(null)
const showBackupList = ref(false)
const backupLoading = ref(false)
const backupStatus = ref<BackupStatusVO | null>(null)
const latestBackup = computed(() => backupStatus.value?.latestBackup || null)

interface NotifChannelConfig {
  enabled: boolean
  frequency: string
  url?: string
}

interface NotifConfig {
  email: NotifChannelConfig
  inApp: NotifChannelConfig
  webhook: NotifChannelConfig & { url: string }
}

const notifConfig = reactive<NotifConfig>({
  email: { enabled: true, frequency: 'realtime' },
  inApp: { enabled: true, frequency: 'realtime' },
  webhook: { enabled: false, url: '' },
})

const infoItems = computed(() => {
  if (!sysInfo.value) return []
  const info = sysInfo.value
  return [
    { label: '应用名称', value: info.appName },
    { label: '系统版本', value: info.version, mono: true },
    { label: 'Java版本', value: info.javaVersion, mono: true },
    { label: '操作系统', value: `${info.osName} ${info.osArch}` },
    { label: '运行时间', value: formatUptime(info.uptime) },
    { label: '活跃连接', value: `${info.activeConnections}` },
    { label: '服务器时间', value: info.serverTime },
  ]
})

function formatUptime(ms: number): string {
  const hours = Math.floor(ms / 3600000)
  const days = Math.floor(hours / 24)
  if (days > 0) return `${days}天 ${hours % 24}小时`
  return `${hours}小时`
}

async function refreshInfo() {
  try {
    const res = await systemApi.getInfo()
    sysInfo.value = res.data
  } catch {}
}

async function loadBackups() {
  try {
    const res = await backupApi.getStatus()
    backupStatus.value = res.data
  } catch {}
}

async function createBackup() {
  backupLoading.value = true
  try {
    await backupApi.create()
    ElMessage.success('备份创建成功')
    loadBackups()
  } catch {
    ElMessage.error('备份创建失败')
  } finally {
    backupLoading.value = false
  }
}

async function restoreBackup(backup: BackupRecordVO) {
  try {
    await ElMessageBox.confirm('恢复此备份将覆盖当前数据，确定继续吗？', '恢复确认', {
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await backupApi.restore(backup.id)
    ElMessage.success('恢复成功')
  } catch {}
}

async function saveNotifications() {
  try {
    const configs: Record<string, string> = {}
    configs['login_notification'] = String(notifConfig.email.enabled)
    configs['operation_notification'] = String(notifConfig.inApp.enabled)
    configs['backup_notification'] = String(notifConfig.webhook.enabled)
    configs['system_alert'] = String(notifConfig.inApp.enabled)
    await settingsApi.updateNotificationConfig(configs)
    ElMessage.success('通知配置已保存')
  } catch {}
}

onMounted(() => {
  refreshInfo()
  loadBackups()
})
</script>

<style scoped>
.settings-page {
  animation: fade-in-up 0.4s ease-out;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.settings-card {
  min-height: 200px;
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 11px;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 14px;
  color: var(--text-primary);
}

.info-value.mono {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--accent-cyan);
}

.loading-info {
  text-align: center;
  padding: 20px;
  color: var(--text-muted);
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.setting-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.setting-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.setting-desc {
  font-size: 12px;
  color: var(--text-muted);
}

.setting-control {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.backup-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: var(--bg-elevated);
  border-radius: 8px;
  margin-bottom: 16px;
}

.backup-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.backup-label {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.backup-time {
  display: block;
  font-size: 13px;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.backup-size {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--accent-cyan);
}

.backup-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.backup-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.backup-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-elevated);
  border-radius: 6px;
}

.backup-item-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.backup-filename {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-primary);
}

.backup-item-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 1200px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
