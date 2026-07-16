<template>
  <div class="shell-panel" :class="{ visible: shell.visible }" :style="shell.visible ? { height: panelHeight + 'px' } : {}">
    <div class="shell-header" @mousedown="startResize">
      <div class="shell-drag-bar">
        <span class="drag-dot"></span>
        <span class="drag-dot"></span>
        <span class="drag-dot"></span>
      </div>
      <div class="shell-title">
        <el-icon :size="14"><Monitor /></el-icon>
        <span>Terminal</span>
      </div>
      <div class="shell-actions">
        <button class="shell-btn" @click="closePanel" title="关闭">
          <el-icon :size="12"><Close /></el-icon>
        </button>
      </div>
    </div>
    <div class="shell-body" ref="shellBody">
      <div class="shell-lines">
        <div
          v-for="line in shell.lines"
          :key="line.id"
          class="shell-line"
          :class="line.type"
        >
          <span class="line-content">{{ line.content }}</span>
        </div>
      </div>
      <div class="shell-input-line">
        <span class="prompt">$</span>
        <input
          v-model="inputCommand"
          class="shell-input"
          @keydown.enter="executeCommand"
          @keydown.up.prevent="navigateUp"
          @keydown.down.prevent="navigateDown"
          @keydown.ctrl.c.prevent="handleCtrlC"
          ref="inputRef"
          placeholder="输入命令..."
        />
        <span class="cursor-blink">▊</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { useShellStore } from '@/stores/shell'
import { Monitor, Close } from '@element-plus/icons-vue'

const shell = useShellStore()
const inputCommand = ref('')
const inputRef = ref<HTMLInputElement>()
const shellBody = ref<HTMLDivElement>()
const panelHeight = ref(280)

// Auto-connect when panel becomes visible
watch(() => shell.visible, (val) => {
  if (val && !shell.connected) {
    shell.connect()
    shell.startHeartbeat()
  }
})

function closePanel() {
  shell.visible = false
}

function executeCommand() {
  if (!inputCommand.value.trim()) return
  shell.execute(inputCommand.value)
  inputCommand.value = ''
  scrollToBottom()
}

function navigateUp() {
  const result = shell.navigateHistory('up')
  if (result !== null) inputCommand.value = result
}

function navigateDown() {
  const result = shell.navigateHistory('down')
  if (result !== null) inputCommand.value = result
  else inputCommand.value = ''
}

function handleCtrlC() {
  inputCommand.value = ''
  shell.interrupt()
}

function scrollToBottom() {
  nextTick(() => {
    if (shellBody.value) {
      shellBody.value.scrollTop = shellBody.value.scrollHeight
    }
  })
}

function startResize(e: MouseEvent) {
  // Don't resize when clicking action buttons
  if ((e.target as HTMLElement).closest('.shell-btn')) return

  const startY = e.clientY
  const startHeight = panelHeight.value

  function onMouseMove(ev: MouseEvent) {
    const diff = startY - ev.clientY
    panelHeight.value = Math.max(120, Math.min(600, startHeight + diff))
  }

  function onMouseUp() {
    document.removeEventListener('mousemove', onMouseMove)
    document.removeEventListener('mouseup', onMouseUp)
  }

  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

watch(() => shell.lines.length, () => scrollToBottom())
</script>

<style scoped>
.shell-panel {
  height: 0;
  background: var(--bg-base);
  border-top: 1px solid var(--border-base);
  display: flex;
  flex-direction: column;
  transition: height 0.25s ease;
  overflow: hidden;
  flex-shrink: 0;
}

.shell-panel.visible {
  /* height is set via inline style for resize support */
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-base);
  cursor: ns-resize;
  user-select: none;
  flex-shrink: 0;
}

.shell-drag-bar {
  display: flex;
  gap: 3px;
}

.drag-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--text-muted);
}

.shell-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--accent-cyan);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 500;
}

.shell-actions {
  display: flex;
  gap: 4px;
}

.shell-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.shell-btn:hover {
  background: var(--color-error-dim);
  color: var(--color-error);
}

.shell-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  font-family: var(--font-mono);
  font-size: 13px;
  line-height: 1.6;
}

.shell-line {
  padding: 1px 0;
}

.shell-line.input .line-content {
  color: var(--accent-cyan);
}

.shell-line.output .line-content {
  color: var(--text-primary);
}

.shell-line.error .line-content {
  color: var(--color-error);
}

.shell-line.system .line-content {
  color: var(--text-muted);
  font-style: italic;
}

.shell-input-line {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
}

.prompt {
  color: var(--accent-cyan);
  font-weight: 600;
  flex-shrink: 0;
}

.shell-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 13px;
  caret-color: transparent;
}

.shell-input::placeholder {
  color: var(--text-muted);
}

.cursor-blink {
  color: var(--accent-cyan);
  animation: blink-cursor 1s step-end infinite;
}
</style>
