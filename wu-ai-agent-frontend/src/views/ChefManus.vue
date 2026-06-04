<template>
  <div class="manus-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">百味智厨 · 智能体</h1>
      <div class="user-info">{{ username }}</div>
    </div>

    <div class="chat-area">
      <ChatRoom
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="manus"
        @send-message="sendMessage"
      />
    </div>

    <AppFooter />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithChefManus, createConversation } from '../api'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { username } = useAuth()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

const addMessage = (content, isUser, type = '') => {
  messages.value.push({ content, isUser, type, time: Date.now() })
}

const sendMessage = (message) => {
  addMessage(message, true)
  if (eventSource) eventSource.close()

  connectionStatus.value = 'connecting'
  let buffer = []

  eventSource = chatWithChefManus(message, chatId.value)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      buffer.push(data)
      const text = buffer.join('\n')
      if (messages.value.length > 0 && !messages.value[messages.value.length - 1].isUser) {
        messages.value[messages.value.length - 1].content = text
      } else {
        addMessage(text, false, 'ai-answer')
      }
    }
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }

  eventSource.onerror = () => {
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

const goBack = () => router.push('/')

onMounted(async () => {
  try {
    const { data } = await createConversation('chef_manus')
    chatId.value = data.conversationId
  } catch (err) {
    console.error('初始化会话失败', err)
    router.push('/login')
    return
  }
  addMessage('我是智厨智能体，可以帮你查菜谱、整理购物清单、生成食谱 PDF 等复杂任务。请描述你的需求。', false)
})

onBeforeUnmount(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.manus-container {
  min-height: 100vh;
  background: #f5f9ff;
  display: flex;
  flex-direction: column;
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 16px 24px;
  background: #2c3e50;
  color: white;
}

.back-button {
  cursor: pointer;
  justify-self: start;
}

.back-button:before {
  content: '← ';
}

.title {
  margin: 0;
  font-size: 1.2rem;
  text-align: center;
}

.user-info {
  justify-self: end;
  font-size: 0.85rem;
  opacity: 0.9;
}

.chat-area {
  flex: 1;
  padding: 16px;
}
</style>
