<template>
  <div class="page-header">
    <div class="header-left">
      <h1 class="page-title">{{ title }}</h1>
      <p class="page-subtitle" v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div class="header-right">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.5px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
