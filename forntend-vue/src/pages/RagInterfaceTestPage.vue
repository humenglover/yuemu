<template>
  <div class="rag-test-container">
    <h1>RAG接口测试页面</h1>

    <div class="test-section">
      <h2>普通对话测试</h2>
      <div class="input-area">
        <a-textarea
          v-model:value="normalQuestion"
          placeholder="请输入问题"
          :rows="3"
          style="margin-bottom: 10px;"
        />
        <a-button
          type="primary"
          @click="testNormalChat"
          :loading="normalLoading"
        >
          发送问题
        </a-button>
      </div>

      <div class="response-area" v-if="normalResponse">
        <h3>响应结果：</h3>
        <a-card class="response-card">
          <p><strong>答案：</strong>{{ normalResponse.answer }}</p>
          <p><strong>是否命中缓存：</strong>{{ normalResponse.cacheHit ? '是' : '否' }}</p>
        </a-card>
      </div>
    </div>

    <div class="test-section">
      <h2>流式对话测试</h2>
      <div class="input-area">
        <a-textarea
          v-model:value="streamQuestion"
          placeholder="请输入问题"
          :rows="3"
          style="margin-bottom: 10px;"
        />
        <a-button
          type="primary"
          @click="testStreamChat"
          :loading="streamLoading"
        >
          发送问题（流式）
        </a-button>
      </div>

      <div class="response-area" v-if="streamResponse">
        <h3>流式响应：</h3>
        <a-card class="response-card">
          <p>{{ streamResponse }}</p>
        </a-card>
      </div>
    </div>

    <div class="test-section">
      <h2>清除上下文测试</h2>
      <a-button
        type="primary"
        danger
        @click="testClearContext"
        :loading="clearContextLoading"
      >
        清除对话上下文
      </a-button>

      <div class="response-area" v-if="clearContextResponse !== null">
        <h3>响应结果：</h3>
        <a-card class="response-card">
          <p><strong>清除结果：</strong>{{ clearContextResponse ? '成功' : '失败' }}</p>
        </a-card>
      </div>
    </div>

    <div class="test-section">
      <h2>接口调用日志</h2>
      <a-card class="log-card">
        <div
          v-for="(log, index) in logs"
          :key="index"
          :class="['log-item', log.type]"
        >
          <span class="log-time">[{{ log.time }}]</span>
          <span class="log-type">[{{ log.type.toUpperCase() }}]</span>
          <span class="log-content">{{ log.content }}</span>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { chatUsingPost, chatStreamUsingGet, clearContextUsingPost } from '@/api/ragController'

// 定义日志类型
interface LogEntry {
  time: string
  type: 'info' | 'success' | 'error'
  content: string
}

// 定义响应类型
interface RagChatResponse {
  answer: string
  cacheHit: boolean
}

// 定义全局类型
interface Window {
  EventSource: any
}

declare const EventSourcePolyfill: any

// 普通对话相关
const normalQuestion = ref('')
const normalResponse = ref(null)
const normalLoading = ref(false)

// 流式对话相关
const streamQuestion = ref('')
const streamResponse = ref('')
const streamLoading = ref(false)

// 清除上下文相关
const clearContextResponse = ref<boolean | null>(null)
const clearContextLoading = ref(false)

// 日志相关
const logs: { value: LogEntry[] } = ref<LogEntry[]>([])

// 添加日志
const addLog = (type: 'info' | 'success' | 'error', content: string) => {
  const now = new Date().toLocaleTimeString()
  logs.value.push({
    time: now,
    type,
    content
  })

  // 限制日志数量，防止过多占用内存
  if (logs.value.length > 50) {
    logs.value.shift()
  }
}

// 测试普通对话
const testNormalChat = async () => {
  if (!normalQuestion.value.trim()) {
    message.warning('请输入问题')
    return
  }

  normalLoading.value = true
  addLog('info', `发送普通对话请求: ${normalQuestion.value}`)

  try {
    const response = await chatUsingPost({ question: normalQuestion.value })
    // 后端返回格式为 { code: 0, data: { answer, cacheHit }, message: 'ok' }
    normalResponse.value = response.data.data as RagChatResponse
    addLog('success', `普通对话响应: ${response.data.data?.answer?.substring(0, 50)}...`)
    message.success('普通对话请求成功')
  } catch (error: any) {
    console.error('普通对话请求失败:', error)
    addLog('error', `普通对话请求失败: ${error.message || error}`)
    message.error('普通对话请求失败: ' + (error.message || error))
  } finally {
    normalLoading.value = false
  }
}

// 测试流式对话
const testStreamChat = async () => {
  if (!streamQuestion.value.trim()) {
    message.warning('请输入问题')
    return
  }

  streamLoading.value = true
  streamResponse.value = ''
  addLog('info', `发送流式对话请求: ${streamQuestion.value}`)

  try {
    // 由于SSE流的特殊性，我们直接使用fetch API处理流式响应
    const token = localStorage.getItem('token')
    const url = `/api/rag/chat/stream?question=${encodeURIComponent(streamQuestion.value)}`

    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token || ''}`
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('无法读取响应流')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || '' // 保留未完成的行

      for (const line of lines) {
        if (line.indexOf('data: ') === 0) {
          const data = line.substring(6) // 移除 'data: ' 前缀
          if (data === '[DONE]') {
            break
          }
          try {
            const parsed = JSON.parse(data)
            streamResponse.value += parsed
          } catch (e) {
            // 如果不是JSON格式，直接添加
            if (data.trim()) {
              streamResponse.value += data
            }
          }
        }
      }
    }

    reader.releaseLock()

    addLog('success', `流式对话完成，响应长度: ${streamResponse.value.length}`)
    message.success('流式对话请求完成')
  } catch (error: any) {
    console.error('流式对话请求失败:', error)
    addLog('error', `流式对话请求失败: ${error.message || error}`)
    message.error('流式对话请求失败: ' + (error.message || error))
  } finally {
    streamLoading.value = false
  }
}

// 测试清除上下文
const testClearContext = async () => {
  clearContextLoading.value = true
  addLog('info', '发送清除上下文请求')

  try {
    const response = await clearContextUsingPost()
    // 后端返回格式为 { code: 0, data: boolean, message: 'ok' }
    clearContextResponse.value = response.data.data
    addLog('success', `清除上下文结果: ${response.data.data}`)
    message.success('清除上下文请求成功')
  } catch (error: any) {
    console.error('清除上下文请求失败:', error)
    addLog('error', `清除上下文请求失败: ${error.message || error}`)
    message.error('清除上下文请求失败: ' + (error.message || error))
  } finally {
    clearContextLoading.value = false
  }
}
</script>

<style scoped>
.rag-test-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.test-section {
  margin-bottom: 40px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

.test-section h2 {
  margin-top: 0;
  color: #303133;
}

.input-area {
  margin-bottom: 20px;
}

.response-area {
  margin-top: 20px;
}

.response-card {
  background-color: #f5f7fa;
}

.log-card {
  max-height: 400px;
  overflow-y: auto;
}

.log-item {
  padding: 8px 0;
  border-bottom: 1px solid #ebeef5;
  font-family: monospace;
  font-size: 14px;
}

.log-item.info {
  color: #409eff;
}

.log-item.success {
  color: #67c23a;
}

.log-item.error {
  color: #f56c6c;
}

.log-time {
  color: #909399;
  margin-right: 10px;
}

.log-type {
  font-weight: bold;
  margin-right: 10px;
}

.log-content {
  color: #606266;
}
</style>
