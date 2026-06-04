import axios from 'axios'
import { useAuth } from '../composables/useAuth'

const API_BASE_URL = import.meta.env.PROD
  ? '/api'
  : 'http://localhost:8123/api'

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000
})

http.interceptors.request.use((config) => {
  const { token } = useAuth()
  if (token.value) {
    config.headers.Authorization = `Bearer ${token.value}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const { clearAuth } = useAuth()
      clearAuth()
      if (!window.location.pathname.includes('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(window.location.pathname)}`
      }
    }
    return Promise.reject(error)
  }
)

/**
 * 用户注册。
 */
export const register = (payload) => http.post('/auth/register', payload)

/**
 * 用户登录。
 */
export const login = (payload) => http.post('/auth/login', payload)

/**
 * 创建新会话。
 */
export const createConversation = (scene = 'chef_app') =>
  http.post('/chat/conversations', { scene })

/**
 * 查询会话历史消息。
 */
export const fetchMessages = (conversationId) =>
  http.get(`/chat/conversations/${conversationId}/messages`)

/**
 * 查询用户会话列表。
 */
export const fetchConversations = () => http.get('/chat/conversations')

export const connectSSE = (url, params, onMessage, onError) => {
  const { token } = useAuth()
  const allParams = { ...params, token: token.value }
  const queryString = Object.keys(allParams)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(allParams[key])}`)
    .join('&')
  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  const eventSource = new EventSource(fullUrl)

  eventSource.onmessage = event => {
    if (onMessage) onMessage(event.data)
  }
  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }
  return eventSource
}

export const chatWithChefApp = (message, chatId) => {
  return connectSSE('/ai/chef_app/chat/sse', { message, chatId })
}

export const chatWithChefManus = (message, chatId) => {
  return connectSSE('/ai/chef_manus/chat', { message, chatId })
}

export default {
  register,
  login,
  createConversation,
  fetchMessages,
  fetchConversations,
  chatWithChefApp,
  chatWithChefManus
}
