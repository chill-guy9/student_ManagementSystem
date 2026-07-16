import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useAuthStore } from './auth'

export interface ShellLine {
  id: number
  type: 'input' | 'output' | 'error' | 'system'
  content: string
  timestamp: Date
}

export const useShellStore = defineStore('shell', () => {
  const lines = ref<ShellLine[]>([])
  const history = ref<string[]>([])
  const historyIndex = ref(-1)
  const connected = ref(false)
  const visible = ref(false)
  let lineId = 0
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 5
  // Callback for the terminal to receive output in real-time
  let onOutput: ((type: 'output' | 'error' | 'system', content: string) => void) | null = null

  function addLine(type: ShellLine['type'], content: string) {
    lines.value.push({
      id: ++lineId,
      type,
      content,
      timestamp: new Date(),
    })
  }

  function getWsUrl() {
    const auth = useAuthStore()
    const token = auth.token
    // Determine WebSocket URL based on current location
    const loc = window.location
    const protocol = loc.protocol === 'https:' ? 'wss:' : 'ws:'
    // In dev mode, Vite proxy handles /ws -> localhost:8081
    // In prod, Nginx proxies /ws -> backend
    const host = loc.host
    return `${protocol}//${host}/ws/shell?token=${encodeURIComponent(token)}`
  }

  function connect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    const auth = useAuthStore()
    if (!auth.token) {
      addLine('error', '未登录，无法连接终端')
      return
    }

    try {
      ws = new WebSocket(getWsUrl())
    } catch (e) {
      addLine('error', 'WebSocket 连接创建失败')
      return
    }

    ws.onopen = () => {
      connected.value = true
      reconnectAttempts = 0
      addLine('system', '终端已连接')
      onOutput?.('system', '\x1b[32m终端已连接\x1b[0m')
    }

    ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data)
        if (msg.type === 'shell.output') {
          addLine('output', msg.data)
          onOutput?.('output', msg.data)
        } else if (msg.type === 'shell.exit') {
          const code = msg.data
          if (code !== 0) {
            addLine('system', `进程退出码: ${code}`)
          }
          onOutput?.('system', '')
        } else if (msg.type === 'error') {
          addLine('error', msg.data)
          onOutput?.('error', `\x1b[31m${msg.data}\x1b[0m`)
        } else if (msg.type === 'pong') {
          // heartbeat response, ignore
        }
      } catch {
        // Non-JSON message, treat as plain output
        addLine('output', event.data)
        onOutput?.('output', event.data)
      }
    }

    ws.onclose = () => {
      connected.value = false
      addLine('system', '终端连接已断开')
      onOutput?.('system', '\x1b[33m终端连接已断开\x1b[0m')
      attemptReconnect()
    }

    ws.onerror = () => {
      connected.value = false
      addLine('error', '终端连接错误')
    }
  }

  function attemptReconnect() {
    if (reconnectAttempts >= maxReconnectAttempts) {
      addLine('error', '达到最大重连次数，请手动刷新页面')
      return
    }
    reconnectTimer = setTimeout(() => {
      reconnectAttempts++
      addLine('system', `正在重连... (${reconnectAttempts}/${maxReconnectAttempts})`)
      connect()
    }, 2000 * (reconnectAttempts + 1))
  }

  function disconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    reconnectAttempts = maxReconnectAttempts // prevent auto-reconnect
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
  }

  function interrupt() {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'shell.interrupt' }))
    }
  }

  function execute(command: string) {
    if (!command.trim()) return

    history.value.unshift(command)
    if (history.value.length > 100) history.value.pop()
    historyIndex.value = -1

    addLine('input', `$ ${command}`)

    // Handle 'clear' locally (no need to send to server)
    if (command.trim().toLowerCase() === 'clear') {
      lines.value = []
      return
    }

    // Send command to backend via WebSocket
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'shell.input', data: command }))
    } else {
      addLine('error', '终端未连接，请稍后重试')
      onOutput?.('error', '\x1b[31m终端未连接，请稍后重试\x1b[0m')
      // Try to reconnect
      connect()
    }
  }

  function navigateHistory(direction: 'up' | 'down') {
    if (direction === 'up') {
      if (historyIndex.value < history.value.length - 1) {
        historyIndex.value++
        return history.value[historyIndex.value]
      }
    } else {
      if (historyIndex.value > 0) {
        historyIndex.value--
        return history.value[historyIndex.value]
      }
      historyIndex.value = -1
      return ''
    }
    return null
  }

  function toggleVisible() {
    visible.value = !visible.value
  }

  function setOnOutput(cb: (type: 'output' | 'error' | 'system', content: string) => void) {
    onOutput = cb
  }

  // Heartbeat to keep connection alive
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  function startHeartbeat() {
    stopHeartbeat() // Clear any existing timer before creating a new one
    heartbeatTimer = setInterval(() => {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({ type: 'ping' }))
      }
    }, 30000)
  }
  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  return {
    lines, history, historyIndex, connected, visible,
    addLine, execute, navigateHistory, interrupt,
    connect, disconnect, toggleVisible, setOnOutput,
    startHeartbeat, stopHeartbeat,
  }
})
