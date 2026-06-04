import { ref, computed } from 'vue'

const TOKEN_KEY = 'wu_token'
const USER_ID_KEY = 'wu_userId'
const USERNAME_KEY = 'wu_username'

const token = ref(localStorage.getItem(TOKEN_KEY) || '')
const userId = ref(localStorage.getItem(USER_ID_KEY) || '')
const username = ref(localStorage.getItem(USERNAME_KEY) || '')

/**
 * 登录态管理。
 */
export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  const setAuth = (auth) => {
    token.value = auth.token
    userId.value = String(auth.userId)
    username.value = auth.username
    localStorage.setItem(TOKEN_KEY, auth.token)
    localStorage.setItem(USER_ID_KEY, String(auth.userId))
    localStorage.setItem(USERNAME_KEY, auth.username)
  }

  const clearAuth = () => {
    token.value = ''
    userId.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_ID_KEY)
    localStorage.removeItem(USERNAME_KEY)
  }

  return { token, userId, username, isLoggedIn, setAuth, clearAuth }
}
