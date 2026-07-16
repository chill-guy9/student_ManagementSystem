<template>
  <div class="search-box" :class="{ focused: isFocused }">
    <el-icon class="search-icon" :size="16"><Search /></el-icon>
    <input
      ref="inputRef"
      :value="modelValue"
      :placeholder="placeholder"
      class="search-input"
      @focus="isFocused = true"
      @blur="isFocused = false"
      @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @keydown.enter="emit('search', modelValue)"
    />
    <button v-if="modelValue" class="clear-btn" @click="clear">
      <el-icon :size="14"><Close /></el-icon>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search, Close } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
}>(), {
  placeholder: '搜索...',
})

const emit = defineEmits(['update:modelValue', 'search'])
const isFocused = ref(false)
const inputRef = ref<HTMLInputElement>()

function clear() {
  emit('update:modelValue', '')
  emit('search', '')
  inputRef.value?.focus()
}
</script>

<style scoped>
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-input);
  border: 1px solid var(--border-base);
  border-radius: 8px;
  transition: all var(--transition-base);
}

.search-box.focused {
  border-color: var(--accent-cyan);
  box-shadow: 0 0 0 1px var(--accent-cyan) inset, var(--glow-sm);
}

.search-icon {
  color: var(--text-muted);
  flex-shrink: 0;
}

.search-box.focused .search-icon {
  color: var(--accent-cyan);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--text-primary);
  font-size: 13px;
  min-width: 0;
}

.search-input::placeholder {
  color: var(--text-muted);
}

.clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
  border-radius: 4px;
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.clear-btn:hover {
  background: var(--color-error-dim);
  color: var(--color-error);
}
</style>
