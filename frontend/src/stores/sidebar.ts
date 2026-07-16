import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useSidebarStore = defineStore('sidebar', () => {
  const collapsed = ref(false)

  function toggle() {
    collapsed.value = !collapsed.value
  }

  function setCollapsed(val: boolean) {
    collapsed.value = val
  }

  return { collapsed, toggle, setCollapsed }
})
