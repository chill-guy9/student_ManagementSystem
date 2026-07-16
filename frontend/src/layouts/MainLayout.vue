<template>
  <div class="main-layout" :class="{ 'sidebar-collapsed': sidebar.collapsed }">
    <Sidebar />
    <div class="main-content">
      <StatusBar />
      <div class="page-container">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
      <ShellPanel />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useSidebarStore } from '@/stores/sidebar'
import Sidebar from './Sidebar.vue'
import StatusBar from './StatusBar.vue'
import ShellPanel from './ShellPanel.vue'

const sidebar = useSidebarStore()
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background: var(--bg-base);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: var(--sidebar-width);
  transition: margin-left var(--transition-base);
}

.sidebar-collapsed .main-content {
  margin-left: var(--sidebar-collapsed-width);
}

.page-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  background: var(--bg-base);
}

.page-enter-active {
  animation: fade-in-up 0.3s ease-out;
}

.page-leave-active {
  animation: fade-in-up 0.2s ease-in reverse;
}
</style>
