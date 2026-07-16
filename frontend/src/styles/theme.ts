export const theme = {
  colors: {
    bg: {
      base: '#0a0e17',
      card: '#111827',
      elevated: '#1a2332',
      input: '#0f172a',
      hover: '#1e293b',
    },
    border: {
      base: '#1e293b',
      light: '#334155',
      glow: '#00e5ff',
    },
    text: {
      primary: '#e2e8f0',
      secondary: '#94a3b8',
      muted: '#64748b',
      accent: '#00e5ff',
    },
    accent: {
      cyan: '#00e5ff',
      cyanDim: 'rgba(0, 229, 255, 0.15)',
      cyanGlow: 'rgba(0, 229, 255, 0.4)',
    },
    status: {
      success: '#10b981',
      warning: '#f59e0b',
      error: '#ef4444',
      info: '#3b82f6',
    },
  },
  fonts: {
    mono: "'JetBrains Mono', 'Cascadia Code', monospace",
    display: "'Outfit', 'Inter', system-ui, sans-serif",
    body: "system-ui, -apple-system, 'Segoe UI', sans-serif",
  },
} as const

export type Theme = typeof theme
