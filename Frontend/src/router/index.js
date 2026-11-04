import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/ai',
    children: [
      {
        path: 'ai',
        name: 'ai',
        component: () => import('../views/AiChat.vue'),
        meta: { title: '智能导诊' }
      },
      {
        path: 'registration',
        name: 'registration',
        component: () => import('../views/Registration.vue'),
        meta: { title: '预约挂号' }
      },
      {
        path: 'appointments',
        name: 'appointments',
        component: () => import('../views/MyAppointments.vue'),
        meta: { title: '我的挂号' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/ai' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录鉴权守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/ai')
  } else {
    next()
  }
})

export default router
