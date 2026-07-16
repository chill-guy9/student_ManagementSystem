<template>
  <div class="shell-page">
    <PageHeader title="终端" subtitle="系统命令行交互">
      <template #actions>
        <el-tag :type="shell.connected ? 'success' : 'danger'" size="small" effect="dark">
          {{ shell.connected ? '已连接' : '未连接' }}
        </el-tag>
        <el-button size="small" @click="reconnect" :disabled="shell.connected">
          重新连接
        </el-button>
      </template>
    </PageHeader>

    <div class="shell-container">
      <!-- Quick Commands Sidebar -->
      <div class="quick-commands">
        <div class="quick-title">
          <el-icon :size="14"><Lightning /></el-icon>
          <span>快捷命令</span>
          <span class="os-badge" v-if="serverOs !== 'unknown'">
            {{ serverOs === 'windows' ? 'Win' : 'Linux' }}
          </span>
        </div>
        <div class="command-list">
          <div
            v-for="cmd in quickCommands"
            :key="cmd.command"
            class="command-item"
            @click="executeQuick(cmd.command)"
          >
            <span class="cmd-name">{{ cmd.name }}</span>
            <span class="cmd-desc">{{ cmd.description }}</span>
          </div>
        </div>
      </div>

      <!-- Terminal Tabs + Content -->
      <div class="terminal-area">
        <div class="terminal-tabs">
          <div
            v-for="(tab, idx) in tabs"
            :key="tab.id"
            class="tab-item"
            :class="{ active: activeTabId === tab.id }"
            @click="activeTabId = tab.id"
          >
            <el-icon :size="12"><Monitor /></el-icon>
            <span>{{ tab.name }}</span>
            <button class="tab-close" @click.stop="closeTab(idx)" v-if="tabs.length > 1">
              <el-icon :size="10"><Close /></el-icon>
            </button>
          </div>
          <button class="tab-add" @click="addTab">
            <el-icon :size="12"><Plus /></el-icon>
          </button>
        </div>

        <div class="terminal-content" ref="terminalContainer">
          <div ref="terminalRef" class="xterm-container"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Monitor, Close, Plus, Lightning } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { useShellStore } from '@/stores/shell'
import { systemApi } from '@/api/system'

const shell = useShellStore()
const terminalRef = ref<HTMLElement>()
const terminalContainer = ref<HTMLElement>()
let term: any = null
let fitAddon: any = null
let resizeObserver: ResizeObserver | null = null

interface Tab {
  id: string
  name: string
}

const tabs = ref<Tab[]>([{ id: '1', name: 'Terminal 1' }])
const activeTabId = ref('1')
let tabCounter = 1

// Detect server OS from system info API
const serverOs = ref<'windows' | 'linux' | 'unknown'>('unknown')

// Quick commands dynamically adjust based on server OS
const quickCommands = computed(() => {
  const common = [
    { name: 'whoami', command: 'whoami', description: '当前用户' },
    { name: 'date', command: 'date', description: '系统日期时间' },
    { name: 'git status', command: 'git status', description: 'Git仓库状态' },
    { name: 'java -version', command: 'java -version', description: 'Java版本' },
    { name: 'clear', command: 'clear', description: '清屏' },
  ]

  if (serverOs.value === 'windows') {
    return [
      { name: 'dir', command: 'dir', description: '列出文件' },
      { name: 'type', command: 'type', description: '查看文件内容' },
      { name: 'hostname', command: 'hostname', description: '主机名' },
      { name: 'ver', command: 'ver', description: '系统版本' },
      { name: 'tree', command: 'tree', description: '目录树' },
      { name: 'where java', command: 'where java', description: '查找Java路径' },
      { name: 'findstr', command: 'findstr', description: '搜索文本' },
      ...common,
    ]
  }

  // Linux / unknown (default to Linux commands)
  return [
    { name: 'pwd', command: 'pwd', description: '当前目录' },
    { name: 'ls', command: 'ls', description: '列出文件' },
    { name: 'uname -a', command: 'uname -a', description: '系统信息' },
    { name: 'ps aux', command: 'ps aux', description: '进程列表' },
    { name: 'df -h', command: 'df -h', description: '磁盘使用' },
    { name: 'netstat -an', command: 'netstat -an', description: '网络连接(需超管)' },
    ...common,
  ]
})

function addTab() {
  tabCounter++
  const id = String(tabCounter)
  tabs.value.push({ id, name: `Terminal ${tabCounter}` })
  activeTabId.value = id
}

function closeTab(idx: number) {
  if (tabs.value.length <= 1) return
  tabs.value.splice(idx, 1)
  if (activeTabId.value === tabs.value[idx]?.id) {
    activeTabId.value = tabs.value[0]?.id || '1'
  }
}

function reconnect() {
  shell.disconnect()
  shell.connect()
}

function executeQuick(command: string) {
  if (!term) return
  if (command === 'clear') {
    term.clear()
    shell.execute('clear')
    return
  }
  term.write(`\r\n$ ${command}\r\n`)
  shell.execute(command)
}

async function detectServerOs() {
  try {
    const res = await systemApi.getInfo()
    const osName = res.data?.osName || ''
    if (osName.toLowerCase().includes('win')) {
      serverOs.value = 'windows'
    } else if (osName.toLowerCase().includes('linux') || osName.toLowerCase().includes('unix') || osName.toLowerCase().includes('mac')) {
      serverOs.value = 'linux'
    } else {
      serverOs.value = 'unknown'
    }
  } catch {
    serverOs.value = 'unknown'
  }
}

async function initTerminal() {
  await nextTick()
  if (!terminalRef.value) return

  try {
    const { Terminal } = await import('@xterm/xterm')
    const { FitAddon } = await import('@xterm/addon-fit')
    await import('@xterm/xterm/css/xterm.css')

    term = new Terminal({
      theme: {
        background: '#0a0e17',
        foreground: '#e2e8f0',
        cursor: '#00e5ff',
        cursorAccent: '#0a0e17',
        selectionBackground: 'rgba(0, 229, 255, 0.3)',
        black: '#0a0e17',
        red: '#ef4444',
        green: '#10b981',
        yellow: '#f59e0b',
        blue: '#3b82f6',
        magenta: '#8b5cf6',
        cyan: '#00e5ff',
        white: '#e2e8f0',
        brightBlack: '#64748b',
        brightRed: '#f87171',
        brightGreen: '#34d399',
        brightYellow: '#fbbf24',
        brightBlue: '#60a5fa',
        brightMagenta: '#a78bfa',
        brightCyan: '#22d3ee',
        brightWhite: '#f1f5f9',
      },
      fontFamily: "'JetBrains Mono', 'Cascadia Code', monospace",
      fontSize: 14,
      lineHeight: 1.5,
      cursorBlink: true,
      cursorStyle: 'block',
      allowTransparency: true,
    })

    fitAddon = new FitAddon()
    term.loadAddon(fitAddon)
    term.open(terminalRef.value)
    fitAddon.fit()

    // Set up real-time output callback from shell store
    shell.setOnOutput((type, content) => {
      if (!term) return
      if (type === 'output') {
        term.write(`${content}\r\n`)
      } else if (type === 'error') {
        term.write(`\x1b[31m${content}\x1b[0m\r\n`)
      } else if (type === 'system') {
        if (content) {
          term.write(`${content}\r\n`)
        }
      }
    })

    term.write('\x1b[36m  Student Management System - Terminal v1.0.0\x1b[0m\r\n')
    term.write('\x1b[90m  Connecting to server...\x1b[0m\r\n\r\n')

    let currentLine = ''
    term.write('$ ')

    term.onData((data: string) => {
      if (data === '\r') {
        term.write('\r\n')
        if (currentLine.trim()) {
          if (currentLine.trim() === 'clear') {
            term.clear()
          }
          shell.execute(currentLine)
        }
        currentLine = ''
        term.write('$ ')
      } else if (data === '\x7f') {
        if (currentLine.length > 0) {
          currentLine = currentLine.slice(0, -1)
          term.write('\b \b')
        }
      } else if (data === '\x1b[A') {
        const result = shell.navigateHistory('up')
        if (result !== null) {
          term.write('\r\x1b[K$ ' + result)
          currentLine = result
        }
      } else if (data === '\x1b[B') {
        const result = shell.navigateHistory('down')
        const val = result || ''
        term.write('\r\x1b[K$ ' + val)
        currentLine = val
      } else if (data === '\x03') {
        // Ctrl+C — send interrupt to backend to kill running process
        term.write('^C\r\n$ ')
        currentLine = ''
        shell.interrupt()
      } else if (data >= ' ') {
        currentLine += data
        term.write(data)
      }
    })

    resizeObserver = new ResizeObserver(() => {
      try { fitAddon.fit() } catch {}
    })
    resizeObserver.observe(terminalContainer.value!)

    // Connect to backend WebSocket shell
    shell.connect()
    shell.startHeartbeat()
  } catch (e) {
    console.warn('xterm.js not available, using fallback shell')
  }
}

onMounted(() => {
  detectServerOs()
  initTerminal()
})

onUnmounted(() => {
  shell.setOnOutput(null)
  shell.stopHeartbeat()
  shell.disconnect()
  term?.dispose()
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped>
.shell-page {
  animation: fade-in-up 0.4s ease-out;
}

.shell-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 160px);
  min-height: 400px;
}

.quick-commands {
  width: 200px;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  border-radius: 12px;
  padding: 16px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.quick-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-cyan);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.os-badge {
  font-size: 10px;
  background: var(--accent-cyan-dim);
  color: var(--accent-cyan);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: var(--font-mono);
  letter-spacing: 0;
}

.command-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  overflow-y: auto;
}

.command-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.command-item:hover {
  background: var(--accent-cyan-dim);
}

.cmd-name {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 500;
  color: var(--accent-cyan);
}

.cmd-desc {
  font-size: 11px;
  color: var(--text-muted);
}

.terminal-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  border-radius: 12px;
  overflow: hidden;
}

.terminal-tabs {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 8px 12px;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-base);
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  color: var(--text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.tab-item:hover {
  background: var(--bg-hover);
  color: var(--text-secondary);
}

.tab-item.active {
  background: var(--accent-cyan-dim);
  color: var(--accent-cyan);
}

.tab-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 3px;
  margin-left: 4px;
}

.tab-close:hover {
  background: var(--color-error-dim);
  color: var(--color-error);
}

.tab-add {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px dashed var(--border-light);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 6px;
  transition: all var(--transition-fast);
}

.tab-add:hover {
  border-color: var(--accent-cyan);
  color: var(--accent-cyan);
  background: var(--accent-cyan-dim);
}

.terminal-content {
  flex: 1;
  padding: 8px;
  overflow: hidden;
}

.xterm-container {
  width: 100%;
  height: 100%;
}
</style>
