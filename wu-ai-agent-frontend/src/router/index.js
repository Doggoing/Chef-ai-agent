import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '百味智厨 AI 助手', requiresAuth: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录 - 百味智厨', guestOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册 - 百味智厨', guestOnly: true }
  },
  {
    path: '/chef-assistant',
    name: 'ChefAssistant',
    component: () => import('../views/ChefAssistant.vue'),
    meta: { title: '百味智厨 - 烹饪助手', requiresAuth: true }
  },
  {
    path: '/chef-manus',
    name: 'ChefManus',
    component: () => import('../views/ChefManus.vue'),
    meta: { title: '百味智厨 - 智能体', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = to.meta.title
  }

  const { isLoggedIn } = useAuth()

  if (to.meta.requiresAuth && !isLoggedIn.value) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if (to.meta.guestOnly && isLoggedIn.value) {
    next('/')
    return
  }

  next()
})

export default router
