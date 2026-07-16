import { ref, onMounted, onUnmounted } from 'vue'

export function useWebSocket(path: string) {
  const ws = ref<WebSocket | null>(null)
  const isConnected = ref(false)
  const lastMessage = ref<any>(null)
  const error = ref<Event | null>(null)
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let reconnectAttempts = 0
  const maxReconnectAttempts = 5

  function connect() {
    const token = localStorage.getItem('token')
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const url = `${protocol}//${host}${path}?token=${encodeURIComponent(token || '')}`

    ws.value = new WebSocket(url)

    ws.value.onopen = () => {
      isConnected.value = true
      reconnectAttempts = 0
    }

    ws.value.onmessage = (event) => {
      try {
        lastMessage.value = JSON.parse(event.data)
      } catch {
        lastMessage.value = event.data
      }
    }

    ws.value.onclose = () => {
      isConnected.value = false
      attemptReconnect()
    }

    ws.value.onerror = (e) => {
      error.value = e
      isConnected.value = false
    }
  }

  function attemptReconnect() {
    if (reconnectAttempts >= maxReconnectAttempts) return
    reconnectTimer = setTimeout(() => {
      reconnectAttempts++
      connect()
    }, 2000 * (reconnectAttempts + 1)) // Backoff: 2s, 4s, 6s, 8s, 10s
  }

  function send(data: any) {
    if (ws.value && isConnected.value) {
      ws.value.send(typeof data === 'string' ? data : JSON.stringify(data))
    }
  }

  function close() {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectAttempts = maxReconnectAttempts // prevent auto-reconnect
    ws.value?.close()
  }

  onMounted(() => connect())
  onUnmounted(() => close())

  return { ws, isConnected, lastMessage, error, send, close }
}
