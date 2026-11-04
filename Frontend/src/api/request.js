import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/',
  timeout: 30000
})

// 请求拦截器：附加 token 与 userId 请求头
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  const userId = localStorage.getItem('userId')
  if (token) config.headers['token'] = token
  if (userId) config.headers['userId'] = userId
  return config
})

// 响应拦截器：统一处理后端 Result{code,msg,data}
request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 后端统一返回 Result 结构（code 1 成功 / 0 失败）
    if (data && typeof data === 'object' && 'code' in data) {
      if (data.code === 1) {
        return data.data
      }
      const err = new Error(data.msg || '请求失败')
      err.code = data.code
      return Promise.reject(err)
    }
    return data
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      ElMessage.error('登录已过期，请重新登录')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else {
      const msg = error.message || '网络错误'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request
