import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || ''
  }),
  actions: {
    setAuth({ token, userId, username }) {
      this.token = token
      this.userId = String(userId)
      this.username = username || ''
      localStorage.setItem('token', token)
      localStorage.setItem('userId', String(userId))
      localStorage.setItem('username', username || '')
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
    }
  }
})
