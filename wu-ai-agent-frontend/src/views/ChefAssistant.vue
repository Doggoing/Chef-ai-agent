<template>
  <div class="chef-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">百味智厨 · 烹饪助手</h1>
      <div class="user-info">{{ username }} · 会话: {{ chatId }}</div>
    </div>

    <div class="chat-area">
      <ChatRoom
        :messages="messages"
        :connection-status="connectionStatus"
        ai-type="chef"
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
import { chatWithChefApp, createConversation, fetchMessages } from '../api'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { username } = useAuth()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

const addMessage = (content, isUser) => {
  messages.value.push({ content, isUser, time: Date.now() })
}

const loadHistory = async (conversationId) => {
  try {
    const { data } = await fetchMessages(conversationId)
    messages.value = data.map(item => ({
      content: item.content,
      isUser: item.role === 'user',
      time: new Date(item.createdAt).getTime()
    }))
  } catch (err) {
    console.warn('加载历史消息失败', err)
  }
}

const initConversation = async () => {
  const { data } = await createConversation('chef_app')
  chatId.value = data.conversationId
  await loadHistory(chatId.value)
  if (messages.value.length === 0) {
    addMessage('你好，我是百味智厨烹饪助手。可以问我菜谱、食材搭配、烹饪技巧，有什么想做的菜？', false)
  }
}

const sendMessage = (message) => {
  addMessage(message, true)
  if (eventSource) eventSource.close()

  const aiIndex = messages.value.length
  addMessage('', false)
  connectionStatus.value = 'connecting'
  eventSource = chatWithChefApp(message, chatId.value)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      if (aiIndex < messages.value.length) {
        messages.value[aiIndex].content += data
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
    await initConversation()
  } catch (err) {
    console.error('初始化会话失败', err)
    router.push('/login')
  }
})

onBeforeUnmount(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.chef-container {
  min-height: 100vh;
  background: #fffaf5;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #e67e22;
  color: white;
}

.back-button {
  cursor: pointer;
}

.back-button:before {
  content: '← ';
}

.title {
  margin: 0;
  font-size: 1.2rem;
}

.user-info {
  font-size: 0.85rem;
  opacity: 0.9;
}

.chat-area {
  flex: 1;
  padding: 16px;
}
</style>
