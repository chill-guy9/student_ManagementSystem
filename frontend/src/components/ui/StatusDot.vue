<template>
  <span class="status-dot" :class="[`dot-${status}`]" :title="title">
    <span class="dot-inner"></span>
    <span class="dot-pulse" v-if="pulse"></span>
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  status: 'online' | 'offline' | 'busy' | 'away' | 'error'
  title?: string
  pulse?: boolean
}>(), {
  pulse: true,
})
</script>

<style scoped>
.status-dot {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 10px;
  height: 10px;
}

.dot-inner {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  z-index: 1;
}

.dot-pulse {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  animation: pulse-dot 2s ease-in-out infinite;
  z-index: 0;
}

.dot-online .dot-inner {
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
}
.dot-online .dot-pulse {
  background: var(--color-success);
}

.dot-offline .dot-inner {
  background: var(--text-muted);
}
.dot-offline .dot-pulse {
  display: none;
}

.dot-busy .dot-inner {
  background: var(--color-error);
  box-shadow: 0 0 6px var(--color-error);
}
.dot-busy .dot-pulse {
  background: var(--color-error);
}

.dot-away .dot-inner {
  background: var(--color-warning);
  box-shadow: 0 0 6px var(--color-warning);
}
.dot-away .dot-pulse {
  background: var(--color-warning);
}

.dot-error .dot-inner {
  background: var(--color-error);
  box-shadow: 0 0 6px var(--color-error);
}
.dot-error .dot-pulse {
  background: var(--color-error);
}
</style>
