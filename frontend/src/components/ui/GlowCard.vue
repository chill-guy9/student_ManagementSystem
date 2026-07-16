<template>
  <div class="glow-card" :class="[`glow-${color}`, { hoverable }]" @click="$emit('click')">
    <div class="card-border"></div>
    <div class="card-content">
      <div class="card-header" v-if="title || $slots.header">
        <slot name="header">
          <span class="card-title">{{ title }}</span>
        </slot>
      </div>
      <div class="card-body">
        <slot></slot>
      </div>
      <div class="card-footer" v-if="$slots.footer">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  color?: 'cyan' | 'green' | 'yellow' | 'red' | 'blue'
  hoverable?: boolean
}>()

defineEmits(['click'])
</script>

<style scoped>
.glow-card {
  position: relative;
  background: var(--bg-card);
  border-radius: 12px;
  overflow: hidden;
  transition: all var(--transition-base);
}

.card-border {
  position: absolute;
  inset: 0;
  border-radius: 12px;
  border: 1px solid var(--border-base);
  transition: all var(--transition-base);
  pointer-events: none;
  z-index: 1;
}

.glow-card.hoverable {
  cursor: pointer;
}

.glow-card.hoverable:hover .card-border {
  border-color: var(--accent-cyan);
  box-shadow: var(--glow-md);
}

.glow-cyan .card-border {
  border-color: rgba(0, 229, 255, 0.3);
}

.glow-cyan.hoverable:hover .card-border,
.glow-cyan .card-border:hover {
  border-color: var(--accent-cyan);
  box-shadow: var(--glow-lg);
}

.glow-green .card-border {
  border-color: rgba(16, 185, 129, 0.3);
}

.glow-green.hoverable:hover .card-border {
  border-color: var(--color-success);
  box-shadow: 0 0 16px rgba(16, 185, 129, 0.3);
}

.glow-yellow .card-border {
  border-color: rgba(245, 158, 11, 0.3);
}

.glow-yellow.hoverable:hover .card-border {
  border-color: var(--color-warning);
  box-shadow: 0 0 16px rgba(245, 158, 11, 0.3);
}

.glow-red .card-border {
  border-color: rgba(239, 68, 68, 0.3);
}

.glow-red.hoverable:hover .card-border {
  border-color: var(--color-error);
  box-shadow: 0 0 16px rgba(239, 68, 68, 0.3);
}

.glow-blue .card-border {
  border-color: rgba(59, 130, 246, 0.3);
}

.glow-blue.hoverable:hover .card-border {
  border-color: var(--color-info);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.3);
}

.card-content {
  position: relative;
  z-index: 2;
  padding: 20px;
}

.card-header {
  margin-bottom: 12px;
}

.card-title {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-base);
}
</style>
