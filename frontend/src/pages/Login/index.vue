<template>
  <div class="login-page">
    <div class="bg-grid"></div>
    <div class="bg-particles">
      <span v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>

    <div class="login-card" :class="{ shake: loginError }">
      <div class="card-glow"></div>
      <div class="card-content">
        <div class="login-header">
          <div class="login-logo">
            <span class="logo-char">S</span>
          </div>
          <h1 class="login-title">Student Management</h1>
          <p class="login-subtitle">System Control Panel</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="login-form" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <div class="input-wrapper">
              <el-icon class="input-icon"><User /></el-icon>
              <el-input
                v-model="form.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="undefined"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </div>
          </el-form-item>

          <el-form-item>
            <button type="submit" class="login-btn" :disabled="loading" @click.prevent="handleLogin">
              <span v-if="loading" class="btn-loader"></span>
              <span v-else>登 录</span>
              <span class="btn-glow"></span>
            </button>
          </el-form-item>
        </el-form>

        <div class="login-link">
          还没有账号？
          <router-link :to="{ name: 'Register' }" class="link-text">注册账号</router-link>
        </div>

        <div class="login-footer">
          <span class="footer-line"></span>
          <span class="footer-text">SMS v1.0.0</span>
          <span class="footer-line"></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginError = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function particleStyle(i: number) {
  const x = Math.random() * 100
  const y = Math.random() * 100
  const size = Math.random() * 3 + 1
  const delay = Math.random() * 5
  const duration = Math.random() * 10 + 10
  return {
    left: `${x}%`,
    top: `${y}%`,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
  }
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  loginError.value = false

  try {
    await auth.login(form.username, form.password)
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
    ElMessage.success('登录成功')
  } catch {
    loginError.value = true
    setTimeout(() => { loginError.value = false }, 500)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-base);
  position: relative;
  overflow: hidden;
}

.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 229, 255, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 229, 255, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

.bg-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  background: var(--accent-cyan);
  border-radius: 50%;
  opacity: 0.3;
  animation: float linear infinite;
}

.login-card {
  position: relative;
  width: 400px;
  border-radius: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  z-index: 1;
}

.login-card.shake {
  animation: shake 0.5s ease-in-out;
}

.card-glow {
  position: absolute;
  inset: -1px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.1), transparent 50%, rgba(0, 229, 255, 0.05));
  pointer-events: none;
  z-index: 0;
}

.login-card:hover .card-glow {
  box-shadow: var(--glow-lg);
}

.card-content {
  position: relative;
  z-index: 1;
  padding: 40px 36px;
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  border-radius: 16px;
  background: var(--accent-cyan-dim);
  border: 1px solid rgba(0, 229, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--glow-md);
  animation: pulse-glow 3s ease-in-out infinite;
}

.logo-char {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 700;
  color: var(--accent-cyan);
}

.login-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.5px;
}

.login-subtitle {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.login-form {
  margin-top: 8px;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-muted);
  z-index: 1;
}

.input-wrapper :deep(.el-input__wrapper) {
  padding-left: 40px;
  border-radius: 10px;
  background: var(--bg-input);
  transition: all var(--transition-base);
}

.input-wrapper :deep(.el-input__wrapper.is-focus) {
  border-color: var(--accent-cyan);
  box-shadow: 0 0 0 1px var(--accent-cyan) inset, var(--glow-sm);
}

.login-btn {
  width: 100%;
  height: 44px;
  border: 1px solid var(--accent-cyan);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.15), rgba(0, 229, 255, 0.05));
  color: var(--accent-cyan);
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all var(--transition-base);
}

.login-btn:hover {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.25), rgba(0, 229, 255, 0.1));
  box-shadow: var(--glow-md);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-glow {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 229, 255, 0.2), transparent);
  animation: scan-line 3s linear infinite;
}

.btn-loader {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(0, 229, 255, 0.3);
  border-top-color: var(--accent-cyan);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-link {
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 4px;
}

.link-text {
  color: var(--accent-cyan);
  text-decoration: none;
  font-weight: 500;
  transition: all var(--transition-fast);
}

.link-text:hover {
  text-shadow: var(--glow-sm);
}

.login-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

.footer-line {
  flex: 1;
  height: 1px;
  background: var(--border-base);
}

.footer-text {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
}
</style>
