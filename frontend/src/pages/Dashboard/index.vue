<template>
  <div class="dashboard-page">
    <PageHeader title="仪表盘" subtitle="系统运行概览" />

    <!-- Stats Cards -->
    <div class="stats-grid">
      <GlowCard v-for="stat in stats" :key="stat.label" :color="stat.color">
        <div class="stat-card">
          <div class="stat-icon" :style="{ background: stat.iconBg }">
            <el-icon :size="22" :color="stat.iconColor"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <span class="stat-label">{{ stat.label }}</span>
            <CountUp :end-value="stat.value" :color="stat.iconColor" />
          </div>
        </div>
      </GlowCard>
    </div>

    <!-- Charts Row -->
    <div class="charts-row">
      <GlowCard color="cyan" class="chart-card chart-main">
        <template #header>
          <span class="card-title">用户增长趋势</span>
        </template>
        <v-chart :option="growthOption" autoresize class="chart" />
      </GlowCard>

      <GlowCard color="blue" class="chart-card chart-side">
        <template #header>
          <span class="card-title">日志类型分布</span>
        </template>
        <v-chart :option="logDistOption" autoresize class="chart" />
      </GlowCard>
    </div>

    <!-- System Load -->
    <GlowCard color="green" class="load-card">
      <template #header>
        <span class="card-title">系统负载监控</span>
      </template>
      <v-chart :option="loadOption" autoresize class="chart chart-load" />
    </GlowCard>

    <!-- Recent Activities -->
    <GlowCard color="cyan" class="activity-card">
      <template #header>
        <div class="activity-header">
          <span class="card-title">最近活动</span>
          <router-link :to="{ name: 'Logs' }" class="view-all">查看全部</router-link>
        </div>
      </template>
      <div class="activity-list">
        <div v-for="act in activities" :key="act.logId" class="activity-item">
          <StatusDot :status="act.level === 'ERROR' ? 'error' : act.level === 'WARN' ? 'busy' : 'online'" />
          <div class="activity-info">
            <span class="activity-user">{{ act.operatorName }}</span>
            <span class="activity-op">{{ act.operationTypeLabel || act.operationType }}</span>
            <span class="activity-module">{{ act.targetName || act.targetType }}</span>
          </div>
          <span class="activity-time">{{ formatRelative(act.createdAt) }}</span>
        </div>
      </div>
    </GlowCard>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent,
  LegendComponent, DataZoomComponent,
} from 'echarts/components'
import GlowCard from '@/components/ui/GlowCard.vue'
import CountUp from '@/components/ui/CountUp.vue'
import StatusDot from '@/components/ui/StatusDot.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { dashboardApi, type DashboardStats, type GrowthTrend, type LogTypeDistribution, type SystemLoad, type RecentActivity } from '@/api/dashboard'
import { formatRelative } from '@/utils/formatters'

use([CanvasRenderer, LineChart, PieChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent])

const stats = ref<Array<{
  label: string
  value: number
  icon: string
  color: 'cyan' | 'green' | 'yellow' | 'red' | 'blue'
  iconBg: string
  iconColor: string
}>>([])

const growthData = ref<GrowthTrend[]>([])
const logDistData = ref<LogTypeDistribution[]>([])
const loadData = ref<SystemLoad | null>(null)
const activities = ref<RecentActivity[]>([])

const growthOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#1a2332',
    borderColor: '#1e293b',
    textStyle: { color: '#e2e8f0', fontSize: 12 },
  },
  legend: {
    data: ['学生', '教师', '管理员'],
    textStyle: { color: '#94a3b8', fontSize: 12 },
    top: 0,
  },
  grid: { left: 40, right: 20, top: 40, bottom: 20 },
  xAxis: {
    type: 'category',
    data: growthData.value.map(d => d.date),
    axisLine: { lineStyle: { color: '#1e293b' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#1e293b' } },
    axisLabel: { color: '#64748b', fontSize: 11 },
  },
  series: [
    {
      name: '学生',
      type: 'line',
      smooth: true,
      data: growthData.value.map(d => d.students),
      lineStyle: { color: '#00e5ff', width: 2 },
      itemStyle: { color: '#00e5ff' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0,229,255,0.3)' }, { offset: 1, color: 'rgba(0,229,255,0)' }] } },
    },
    {
      name: '教师',
      type: 'line',
      smooth: true,
      data: growthData.value.map(d => d.teachers),
      lineStyle: { color: '#10b981', width: 2 },
      itemStyle: { color: '#10b981' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.2)' }, { offset: 1, color: 'rgba(16,185,129,0)' }] } },
    },
    {
      name: '管理员',
      type: 'line',
      smooth: true,
      data: growthData.value.map(d => d.admins),
      lineStyle: { color: '#f59e0b', width: 2 },
      itemStyle: { color: '#f59e0b' },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(245,158,11,0.15)' }, { offset: 1, color: 'rgba(245,158,11,0)' }] } },
    },
  ],
}))

// Operation type label mapping
const opTypeLabels: Record<string, string> = {
  LOGIN: '登录', LOGOUT: '登出', CREATE: '新增', UPDATE: '修改', DELETE: '删除',
  EXPORT: '导出', IMPORT: '导入', VIEW: '查看', OTHER: '其他',
}

const logDistOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    backgroundColor: '#1a2332',
    borderColor: '#1e293b',
    textStyle: { color: '#e2e8f0', fontSize: 12 },
  },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    center: ['50%', '50%'],
    data: logDistData.value.map(d => ({ name: opTypeLabels[d.operationType] || d.operationType, value: d.count })),
    label: { color: '#94a3b8', fontSize: 11 },
    itemStyle: {
      borderColor: '#111827',
      borderWidth: 2,
    },
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowColor: 'rgba(0, 229, 255, 0.3)',
      },
    },
    color: ['#00e5ff', '#10b981', '#f59e0b', '#ef4444', '#3b82f6', '#8b5cf6'],
  }],
}))

// System load uses a single data point (gauge/bar style), not time series
const loadOption = computed(() => {
  const d = loadData.value
  if (!d) return {}
  const categories = ['CPU', '内存', '磁盘']
  // cpuUsage/diskUsage are already percentages; memoryUsage may be 0~1 ratio or percentage
  const memPercent = d.memoryUsage > 1 ? d.memoryUsage : d.memoryUsage * 100
  const values = [
    Number(d.cpuUsage.toFixed(1)),
    Number(memPercent.toFixed(1)),
    Number(d.diskUsage.toFixed(1)),
  ]
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#1a2332',
      borderColor: '#1e293b',
      textStyle: { color: '#e2e8f0', fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        return `${p.name}: ${p.value}%`
      },
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: categories,
      axisLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#64748b', fontSize: 12 },
    },
    yAxis: {
      type: 'value',
      max: 100,
      splitLine: { lineStyle: { color: '#1e293b' } },
      axisLabel: { color: '#64748b', fontSize: 11, formatter: '{value}%' },
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({
        value: v,
        itemStyle: {
          color: v > 80 ? '#ef4444' : v > 60 ? '#f59e0b' : ['#00e5ff', '#10b981', '#3b82f6'][i],
          borderRadius: [4, 4, 0, 0],
        },
      })),
      barWidth: 40,
    }],
  }
})

async function loadData_() {
  try {
    const [statsRes, growthRes, logDistRes, loadRes, actRes] = await Promise.allSettled([
      dashboardApi.getStats(),
      dashboardApi.getGrowthTrend(30),
      dashboardApi.getLogTypeDistribution(30),
      dashboardApi.getSystemLoad(),
      dashboardApi.getRecentActivities(8),
    ])

    if (statsRes.status === 'fulfilled') {
      const s = statsRes.value.data as DashboardStats
      stats.value = [
        { label: '总学生', value: s.totalStudents, icon: 'User', color: 'cyan', iconBg: 'rgba(0,229,255,0.1)', iconColor: '#00e5ff' },
        { label: '总教师', value: s.totalTeachers, icon: 'Reading', color: 'green', iconBg: 'rgba(16,185,129,0.1)', iconColor: '#10b981' },
        { label: '今日日志', value: s.todayLogs, icon: 'Document', color: 'yellow', iconBg: 'rgba(245,158,11,0.1)', iconColor: '#f59e0b' },
        { label: '活跃学生', value: s.activeStudents, icon: 'Connection', color: 'red', iconBg: 'rgba(239,68,68,0.1)', iconColor: '#ef4444' },
      ]
    } else {
      useFallbackStats()
    }

    if (growthRes.status === 'fulfilled') growthData.value = growthRes.value.data
    if (logDistRes.status === 'fulfilled') logDistData.value = logDistRes.value.data
    if (loadRes.status === 'fulfilled') loadData.value = loadRes.value.data
    if (actRes.status === 'fulfilled') activities.value = actRes.value.data
  } catch {
    useFallbackStats()
  }
}

function useFallbackStats() {
  stats.value = [
    { label: '总学生', value: 0, icon: 'User', color: 'cyan', iconBg: 'rgba(0,229,255,0.1)', iconColor: '#00e5ff' },
    { label: '总教师', value: 0, icon: 'Reading', color: 'green', iconBg: 'rgba(16,185,129,0.1)', iconColor: '#10b981' },
    { label: '今日日志', value: 0, icon: 'Document', color: 'yellow', iconBg: 'rgba(245,158,11,0.1)', iconColor: '#f59e0b' },
    { label: '活跃学生', value: 0, icon: 'Connection', color: 'red', iconBg: 'rgba(239,68,68,0.1)', iconColor: '#ef4444' },
  ]
}

onMounted(loadData_)
</script>

<style scoped>
.dashboard-page {
  animation: fade-in-up 0.4s ease-out;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: var(--text-muted);
  margin-bottom: 2px;
}

.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.chart-card {
  min-height: 300px;
}

.chart {
  width: 100%;
  height: 260px;
}

.chart-load {
  height: 220px;
}

.load-card {
  margin-bottom: 24px;
}

.activity-card {
  margin-bottom: 24px;
}

.activity-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.view-all {
  font-size: 12px;
  color: var(--accent-cyan);
  cursor: pointer;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-base);
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.activity-user {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.activity-op {
  font-size: 12px;
  color: var(--accent-cyan);
  font-family: var(--font-mono);
}

.activity-module {
  font-size: 12px;
  color: var(--text-muted);
}

.activity-time {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
