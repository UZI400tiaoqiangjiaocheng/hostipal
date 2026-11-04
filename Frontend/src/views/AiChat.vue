<template>
  <div class="chat-wrap">
    <!-- 会话列表 -->
    <div class="chat-side">
      <div class="side-header">
        <el-button type="primary" :icon="Plus" size="large" style="width: 100%" @click="newConversation">
          新建对话
        </el-button>
      </div>
      <div class="conversation-list">
        <div
          v-for="(c, i) in conversations"
          :key="c"
          class="conversation-item"
          :class="{ active: c === activeConversationId }"
          @click="selectConversation(c)"
        >
          <span class="conv-label">对话 {{ conversations.length - i }}</span>
          <el-icon class="conv-delete" @click.stop="deleteConversation(c)"><Delete /></el-icon>
        </div>
        <el-empty v-if="!conversations.length" description="暂无对话" :image-size="60" />
      </div>
    </div>

    <!-- 聊天区 -->
    <div class="chat-main">
      <div class="chat-header">
        <span>智能导诊助手</span>
        <span class="chat-tip">请描述您的症状，我将为您推荐合适的科室</span>
      </div>

      <div ref="msgBox" class="chat-body">
        <el-empty
          v-if="!messages.length"
          description="您好，我是医院智能导诊助手，请问有什么可以帮您？"
        />
        <div
          v-for="(m, idx) in messages"
          :key="idx"
          class="msg-row"
          :class="m.role"
        >
          <div class="msg-bubble">{{ m.content || (m.loading ? '正在思考...' : '') }}</div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="input"
          type="textarea"
          :rows="3"
          resize="none"
          placeholder="请描述您的症状，例如：最近总是咳嗽、发热…"
          @keydown.enter.prevent="sendMessage"
        />
        <el-button
          type="primary"
          :icon="Promotion"
          :loading="streaming"
          @click="sendMessage"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Promotion } from '@element-plus/icons-vue'
import { aiHistoryList, aiHistoryMessages, aiHistorySave, aiHistoryDelete } from '../api'

const conversations = ref([])
const activeConversationId = ref('')
const messages = ref([])
const input = ref('')
const streaming = ref(false)
const msgBox = ref()

// 生成一个简短随机的会话 id
const genId = () =>
  Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 8)

const scrollToBottom = () => {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
  })
}

// 归一化后端返回的 Spring AI Message 结构
const normalizeMessage = (m) => {
  const type = String(m.messageType ?? m.role ?? '').toUpperCase()
  let role = 'assistant'
  if (type.includes('USER')) role = 'user'
  else if (type.includes('SYSTEM')) role = 'system'
  else if (type.includes('ASSISTANT')) role = 'assistant'
  return { role, content: m.text ?? m.content ?? '' }
}

const loadConversations = async () => {
  try {
    conversations.value = (await aiHistoryList()) || []
  } catch {
    conversations.value = []
  }
}

const newConversation = async () => {
  const id = genId()
  try {
    await aiHistorySave(id)
  } catch {
    // 保存失败不阻断对话
  }
  conversations.value.unshift(id)
  activeConversationId.value = id
  messages.value = []
  input.value = ''
}

const selectConversation = async (id) => {
  activeConversationId.value = id
  try {
    const list = (await aiHistoryMessages(id)) || []
    messages.value = list.map(normalizeMessage).filter((m) => m.content)
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

const deleteConversation = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该对话吗？', '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await aiHistoryDelete(id)
    conversations.value = conversations.value.filter((c) => c !== id)
    if (activeConversationId.value === id) {
      activeConversationId.value = ''
      messages.value = []
    }
    ElMessage.success('已删除')
  } catch {
    // 错误提示已在拦截器中处理
  }
}

const sendMessage = async () => {
  const text = input.value.trim()
  if (!text || streaming.value) return

  // 若无当前会话，先新建一个
  if (!activeConversationId.value) {
    await newConversation()
  }

  messages.value.push({ role: 'user', content: text })
  const assistantMsg = { role: 'assistant', content: '', loading: true }
  messages.value.push(assistantMsg)
  input.value = ''
  streaming.value = true
  scrollToBottom()

  try {
    const token = localStorage.getItem('token')
    const userId = localStorage.getItem('userId')
    const url = `/ai/chat?prompt=${encodeURIComponent(text)}&conversationId=${encodeURIComponent(activeConversationId.value)}`
    const res = await fetch(url, { headers: { token, userId } })
    if (!res.ok) {
      throw new Error(`AI 服务请求失败（状态码 ${res.status}）`)
    }
    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    assistantMsg.content = ''
    assistantMsg.loading = false
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      assistantMsg.content += decoder.decode(value, { stream: true })
      scrollToBottom()
    }
  } catch (e) {
    assistantMsg.loading = false
    assistantMsg.content = assistantMsg.content || e.message
  } finally {
    streaming.value = false
  }
}

onMounted(() => {
  loadConversations()
})
</script>

<style scoped>
.chat-wrap {
  display: flex;
  height: calc(100vh - 104px);
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chat-side {
  width: 240px;
  border-right: 1px solid #eef0f3;
  display: flex;
  flex-direction: column;
  background: #fafbfc;
}

.side-header {
  padding: 16px;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  transition: all 0.2s;
}

.conversation-item:hover {
  background: #eef1f6;
}

.conversation-item.active {
  background: #e8f0ff;
  color: #2f6fed;
  font-weight: 600;
}

.conv-delete {
  visibility: hidden;
}

.conversation-item:hover .conv-delete {
  visibility: visible;
}

.conv-delete:hover {
  color: #f56c6c;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #eef0f3;
  font-weight: 600;
}

.chat-tip {
  font-weight: 400;
  font-size: 12px;
  color: #a8abb2;
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f7f8fa;
}

.msg-row {
  display: flex;
  margin-bottom: 16px;
}

.msg-row.user {
  justify-content: flex-end;
}

.msg-row.assistant {
  justify-content: flex-start;
}

.msg-bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-row.user .msg-bubble {
  background: #2f6fed;
  color: #fff;
  border-top-right-radius: 4px;
}

.msg-row.assistant .msg-bubble {
  background: #fff;
  color: #303133;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.chat-input {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-top: 1px solid #eef0f3;
  align-items: flex-end;
}

.chat-input .el-button {
  height: 78px;
}
</style>
