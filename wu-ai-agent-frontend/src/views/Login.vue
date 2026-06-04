<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="title">登录 · 百味智厨</h1>
      <p class="subtitle">登录后即可保存对话记录</p>

      <form class="form" @submit.prevent="handleLogin">
        <label>
          用户名
          <input v-model="form.username" type="text" placeholder="请输入用户名" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </label>
        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>
        <button type="submit" class="submit-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <p class="switch-link">
        还没有账号？
        <router-link to="/register">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../api'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const route = useRoute()
const { setAuth } = useAuth()

const form = reactive({
  username: '',
  password: ''
})
const loading = ref(false)
const errorMsg = ref('')

const handleLogin = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const { data } = await login(form)
    setAuth(data)
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff8f0 0%, #ffe8cc 100%);
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 12px 32px rgba(211, 84, 0, 0.15);
}

.title {
  margin: 0 0 8px;
  color: #d35400;
  text-align: center;
}

.subtitle {
  margin: 0 0 24px;
  color: #666;
  text-align: center;
}

.form label {
  display: block;
  margin-bottom: 16px;
  color: #333;
  font-size: 0.95rem;
}

.form input {
  width: 100%;
  margin-top: 6px;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-sizing: border-box;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: #e67e22;
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error {
  color: #c0392b;
  font-size: 0.9rem;
}

.switch-link {
  text-align: center;
  margin-top: 16px;
  color: #666;
}

.switch-link a {
  color: #d35400;
  text-decoration: none;
}
</style>
