<template>
  <span class="count-up" :style="{ color: color }">{{ displayValue }}</span>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

const props = withDefaults(defineProps<{
  endValue: number
  duration?: number
  color?: string
  prefix?: string
  suffix?: string
}>(), {
  duration: 1500,
  color: 'var(--text-primary)',
  prefix: '',
  suffix: '',
})

const displayValue = ref('0')

function animateCount() {
  const start = 0
  const end = props.endValue
  const duration = props.duration
  const startTime = performance.now()

  function update(currentTime: number) {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    const current = Math.floor(start + (end - start) * eased)
    displayValue.value = `${props.prefix}${current.toLocaleString()}${props.suffix}`

    if (progress < 1) {
      requestAnimationFrame(update)
    }
  }

  requestAnimationFrame(update)
}

onMounted(() => {
  animateCount()
})

watch(() => props.endValue, () => {
  animateCount()
})
</script>

<style scoped>
.count-up {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 32px;
  letter-spacing: -0.5px;
}
</style>
