<template>
  <span class="pulse-badge" :class="[`badge-${type}`]">
    <span class="badge-dot"></span>
    <span class="badge-text"><slot></slot></span>
  </span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  type?: 'success' | 'warning' | 'error' | 'info' | 'cyan'
}>(), {
  type: 'cyan',
})
</script>

<style scoped>
.pulse-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
}

.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.badge-cyan {
  background: var(--accent-cyan-dim);
  color: var(--accent-cyan);
}
.badge-cyan .badge-dot {
  background: var(--accent-cyan);
  box-shadow: 0 0 6px var(--accent-cyan);
  animation: pulse-dot 2s ease-in-out infinite;
}

.badge-success {
  background: var(--color-success-dim);
  color: var(--color-success);
}
.badge-success .badge-dot {
  background: var(--color-success);
  box-shadow: 0 0 6px var(--color-success);
  animation: pulse-dot 2s ease-in-out infinite;
}

.badge-warning {
  background: var(--color-warning-dim);
  color: var(--color-warning);
}
.badge-warning .badge-dot {
  background: var(--color-warning);
  box-shadow: 0 0 6px var(--color-warning);
  animation: pulse-dot 2s ease-in-out infinite;
}

.badge-error {
  background: var(--color-error-dim);
  color: var(--color-error);
}
.badge-error .badge-dot {
  background: var(--color-error);
  box-shadow: 0 0 6px var(--color-error);
  animation: pulse-dot 2s ease-in-out infinite;
}

.badge-info {
  background: var(--color-info-dim);
  color: var(--color-info);
}
.badge-info .badge-dot {
  background: var(--color-info);
  box-shadow: 0 0 6px var(--color-info);
  animation: pulse-dot 2s ease-in-out infinite;
}

.badge-text {
  white-space: nowrap;
}
</style>
