<template>
  <div class="home-container">
    <div class="header">
      <h1 class="title">百味智厨 AI 助手</h1>
      <p class="subtitle">你的智能烹饪顾问 · 菜谱 · 搭配 · 膳食规划</p>
      <div class="user-bar">
        <span>你好，{{ username }}</span>
        <button class="link-btn" @click="logout">退出</button>
      </div>
    </div>

    <div class="apps-container">
      <div class="app-card" @click="navigateTo('/chef-assistant')">
        <div class="app-icon">🍳</div>
        <div class="app-title">烹饪助手</div>
        <div class="app-desc">多轮对话，解答做菜问题，支持流式回复与历史记录</div>
        <div class="app-button">立即体验 →</div>
      </div>

      <div class="app-card" @click="navigateTo('/chef-manus')">
        <div class="app-icon">👨‍🍳</div>
        <div class="app-title">智厨智能体</div>
        <div class="app-desc">ReAct 自主规划，可搜索菜谱、生成 PDF 等</div>
        <div class="app-button">立即体验 →</div>
      </div>
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppFooter from '../components/AppFooter.vue'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { username, clearAuth } = useAuth()

const navigateTo = (path) => router.push(path)

const logout = () => {
  clearAuth()
  router.push('/login')
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff8f0 0%, #ffe8cc 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
}

.header {
  text-align: center;
  margin-bottom: 48px;
}

.title {
  font-size: 2.5rem;
  color: #d35400;
  margin: 0 0 12px;
}

.subtitle {
  color: #666;
  font-size: 1.1rem;
}

.user-bar {
  margin-top: 16px;
  color: #555;
}

.user-bar a {
  color: #d35400;
  text-decoration: none;
  font-weight: 600;
}

.divider {
  margin: 0 8px;
}

.link-btn {
  margin-left: 12px;
  border: none;
  background: none;
  color: #d35400;
  cursor: pointer;
  font-weight: 600;
}

.apps-container {
  display: flex;
  flex-wrap: wrap;
  gap: 32px;
  justify-content: center;
  max-width: 900px;
  flex: 1;
}

.app-card {
  width: 320px;
  background: white;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(211, 84, 0, 0.12);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  text-align: center;
}

.app-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(211, 84, 0, 0.2);
}

.app-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}

.app-title {
  font-size: 1.4rem;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.app-desc {
  color: #666;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 20px;
}

.app-button {
  color: #d35400;
  font-weight: 600;
}
</style>
