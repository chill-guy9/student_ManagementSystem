<template>
  <div class="register-page">
    <div class="bg-grid"></div>
    <div class="bg-particles">
      <span v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></span>
    </div>

    <div class="register-card" :class="{ shake: registerError }">
      <div class="card-glow"></div>
      <div class="card-content">
        <div class="register-header">
          <div class="register-logo">
            <span class="logo-char">S</span>
          </div>
          <h1 class="register-title">用户注册</h1>
          <p class="register-subtitle">Create Your Account</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" class="register-form" @submit.prevent="handleRegister">
          <el-form-item prop="username">
            <div class="input-wrapper">
              <el-icon class="input-icon"><User /></el-icon>
              <el-input v-model="form.username" placeholder="用户名" size="large" />
            </div>
          </el-form-item>

          <el-form-item prop="realName">
            <div class="input-wrapper">
              <el-icon class="input-icon"><UserFilled /></el-icon>
              <el-input v-model="form.realName" placeholder="真实姓名" size="large" />
            </div>
          </el-form-item>

          <el-form-item prop="email">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Message /></el-icon>
              <el-input v-model="form.email" placeholder="邮箱" size="large" />
            </div>
          </el-form-item>

          <el-form-item prop="phone">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Phone /></el-icon>
              <el-input v-model="form.phone" placeholder="手机号" size="large" />
            </div>
          </el-form-item>

          <el-form-item prop="password">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
            </div>
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <div class="input-wrapper">
              <el-icon class="input-icon"><Lock /></el-icon>
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="确认密码"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </div>
          </el-form-item>

          <el-form-item>
            <button type="submit" class="register-btn" :disabled="loading" @click.prevent="handleRegister">
              <span v-if="loading" class="btn-loader"></span>
              <span v-else>注 册</span>
              <span class="btn-glow"></span>
            </button>
          </el-form-item>
        </el-form>

        <div class="register-link">
          已有账号？
          <router-link :to="{ name: 'Login' }" class="link-text">返回登录</router-link>
        </div>

        <div class="register-footer">
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
import { useRouter } from 'vue-router'
import { User, UserFilled, Lock, Message, Phone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '@/api/auth'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)
const registerError = ref(false)

const form = reactive({
  username: '',
  realName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' },
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
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

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  registerError.value = false

  try {
    await authApi.register({
      username: form.username,
      realName: form.realName,
      email: form.email,
      phone: form.phone,
      password: form.password,
    })
    ElMessage.success('注册成功，请登录')
    router.push({ name: 'Login' })
  } catch {
    registerError.value = true
    setTimeout(() => { registerError.value = false }, 500)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
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

.register-card {
  position: relative;
  width: 440px;
  border-radius: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-base);
  z-index: 1;
}

.register-card.shake {
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

.register-card:hover .card-glow {
  box-shadow: var(--glow-lg);
}

.card-content {
  position: relative;
  z-index: 1;
  padding: 36px 36px 28px;
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.register-logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 14px;
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
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-cyan);
}

.register-title {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
  letter-spacing: -0.5px;
}

.register-subtitle {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 6px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.register-form {
  margin-top: 4px;
}

.register-form :deep(.el-form-item) {
  margin-bottom: 18px;
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

.register-btn {
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

.register-btn:hover {
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.25), rgba(0, 229, 255, 0.1));
  box-shadow: var(--glow-md);
  transform: translateY(-1px);
}

.register-btn:active {
  transform: translateY(0);
}

.register-btn:disabled {
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

.register-link {
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

.register-footer {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
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
