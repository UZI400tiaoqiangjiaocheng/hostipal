import request from './request'

// ============ 用户 ============
export const login = (data) => request.post('/user/login', data)
export const register = (data) => request.post('/user/register', data)

// ============ 预约挂号 ============
// 查询排班（科室信息）
export const queryDepartment = () => request.get('/registration/queryDepartment')
// 生成支付订单，返回 { orderNo, qrCode } 或 { error }
export const generateOrder = (schedule) => request.post('/registration/generateOrder', schedule)
// 支付后生成挂号记录
export const addAppointment = (orderNo) => request.post('/registration/addAppointment', { orderNo })
// 查询我的挂号记录
export const queryAppointment = () => request.get('/registration/queryAppointment')
// 取消预约并退款
export const cancelAppointment = (data) => request.put('/registration/cancelAppointment', data)

// ============ AI 智能导诊 ============
// 会话 id 列表
export const aiHistoryList = () => request.get('/ai/history/list')
// 某个会话的消息记录
export const aiHistoryMessages = (conversationId) =>
  request.get('/ai/history/messages', { params: { conversationId } })
// 保存会话 id（后端返回 void）
export const aiHistorySave = (conversationId) =>
  request.get('/ai/history/save', { params: { conversationId } })
// 删除会话（后端返回 void）
export const aiHistoryDelete = (conversationId) =>
  request.delete('/ai/history/delete', { params: { conversationId } })
