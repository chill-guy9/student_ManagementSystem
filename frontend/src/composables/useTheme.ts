import { ref, watch } from 'vue'

type ThemeMode = 'dark' | 'light'

const STORAGE_KEY = 'sms-theme'

const currentTheme = ref<ThemeMode>((localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'dark')

function applyTheme(theme: ThemeMode) {
  const root = document.documentElement
  if (theme === 'dark') {
    root.removeAttribute('data-theme')
    root.classList.add('dark')
  } else {
    root.setAttribute('data-theme', 'light')
    root.classList.remove('dark')
  }
}

// Apply on init
applyTheme(currentTheme.value)

export function useTheme() {
  const isDark = ref(currentTheme.value === 'dark')

  function toggleTheme() {
    currentTheme.value = currentTheme.value === 'dark' ? 'light' : 'dark'
    isDark.value = currentTheme.value === 'dark'
    applyTheme(currentTheme.value)
    localStorage.setItem(STORAGE_KEY, currentTheme.value)
  }

  function setTheme(theme: ThemeMode) {
    currentTheme.value = theme
    isDark.value = theme === 'dark'
    applyTheme(theme)
    localStorage.setItem(STORAGE_KEY, theme)
  }

  watch(currentTheme, (val) => {
    isDark.value = val === 'dark'
  })

  return { isDark, currentTheme, toggleTheme, setTheme }
}
