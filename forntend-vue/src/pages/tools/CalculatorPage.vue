<template>
  <div class="calculator-page">
    <div class="calculator">
      <div class="display">
        <div class="expression">{{ expression || '0' }}</div>
        <div class="result">{{ result || '0' }}</div>
      </div>
      <div class="keypad">
        <button
          v-for="btn in buttons"
          :key="btn.value"
          :class="['button', btn.type]"
          @click="handleClick(btn.value)"
        >
          {{ btn.label }}
        </button>
      </div>
    </div>

    <!-- 添加浮动的历史按钮 -->
    <button class="floating-history-btn" @click="toggleHistory">
      <span class="history-icon">📋</span>
      <span class="history-count" v-if="history.length">{{ history.length }}</span>
    </button>

    <div class="history-panel" :class="{ 'history-panel-open': showHistory }">
      <div class="history-header">
        <h3>计算历史</h3>
        <div class="history-actions">
          <button class="history-action-btn" @click="clearHistory" v-if="history.length">
            <span class="action-icon">🗑️</span>
            清空
          </button>
          <button class="history-action-btn" @click="toggleHistory">
            <span class="action-icon">✖️</span>
            关闭
          </button>
        </div>
      </div>
      <div class="history-list" v-if="history.length">
        <div v-for="(item, index) in history"
             :key="index"
             class="history-item"
             @click="loadHistory(item)">
          <div class="history-expression">{{ item.expression }}</div>
          <div class="history-result">= {{ item.result }}</div>
          <div class="history-time">{{ formatTime(item.timestamp) }}</div>
        </div>
      </div>
      <div class="history-empty" v-else>
        暂无计算记录
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface Button {
  label: string
  value: string
  type: 'number' | 'operator' | 'function'
}

interface HistoryItem {
  expression: string
  result: string
  timestamp: number
}

const buttons: Button[] = [
  { label: 'C', value: 'clear', type: 'function' },
  { label: '←', value: 'backspace', type: 'function' },
  { label: '%', value: '%', type: 'operator' },
  { label: '÷', value: '/', type: 'operator' },
  { label: '7', value: '7', type: 'number' },
  { label: '8', value: '8', type: 'number' },
  { label: '9', value: '9', type: 'number' },
  { label: '×', value: '*', type: 'operator' },
  { label: '4', value: '4', type: 'number' },
  { label: '5', value: '5', type: 'number' },
  { label: '6', value: '6', type: 'number' },
  { label: '-', value: '-', type: 'operator' },
  { label: '1', value: '1', type: 'number' },
  { label: '2', value: '2', type: 'number' },
  { label: '3', value: '3', type: 'number' },
  { label: '+', value: '+', type: 'operator' },
  { label: '±', value: 'negate', type: 'function' },
  { label: '0', value: '0', type: 'number' },
  { label: '.', value: '.', type: 'number' },
  { label: '=', value: '=', type: 'operator' },
]

const expression = ref('')
const result = ref('')
const lastOperator = ref('')
const newNumber = ref(true)
const history = ref<HistoryItem[]>([])
const showHistory = ref(false)

// 从本地存储加载历史记录
onMounted(() => {
  const savedHistory = localStorage.getItem('calculatorHistory')
  if (savedHistory) {
    history.value = JSON.parse(savedHistory)
  }
})

// 保存历史记录到本地存储
const saveHistory = () => {
  localStorage.setItem('calculatorHistory', JSON.stringify(history.value))
}

// 添加计算记录
const addToHistory = () => {
  if (expression.value && result.value) {
    history.value.unshift({
      expression: expression.value,
      result: result.value,
      timestamp: Date.now()
    })
    // 限制历史记录数量为20条
    if (history.value.length > 20) {
      history.value.pop()
    }
    saveHistory()
  }
}

// 清空历史记录
const clearHistory = () => {
  history.value = []
  saveHistory()
}

// 加载历史记录
const loadHistory = (item: HistoryItem) => {
  expression.value = item.result
  result.value = item.result
  newNumber.value = true
}

// 切换历史面板
const toggleHistory = () => {
  showHistory.value = !showHistory.value
}

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const calculate = () => {
  try {
    const sanitizedExpression = expression.value
      .replace(/×/g, '*')
      .replace(/÷/g, '/')
    const evalResult = eval(sanitizedExpression)
    result.value = Number.isInteger(evalResult)
      ? evalResult.toString()
      : evalResult.toFixed(8).replace(/\.?0+$/, '')
  } catch (error) {
    result.value = 'Error'
  }
}

const handleClick = (value: string) => {
  switch (value) {
    case 'clear':
      expression.value = ''
      result.value = ''
      lastOperator.value = ''
      newNumber.value = true
      break

    case 'backspace':
      expression.value = expression.value.slice(0, -1)
      if (expression.value) {
        calculate()
      } else {
        result.value = ''
      }
      break

    case 'negate':
      if (expression.value.startsWith('-')) {
        expression.value = expression.value.slice(1)
      } else {
        expression.value = '-' + expression.value
      }
      calculate()
      break

    case '=':
      if (expression.value) {
        calculate()
        addToHistory()
        expression.value = result.value
        newNumber.value = true
      }
      break

    case '+':
    case '-':
    case '*':
    case '/':
    case '%':
      if (expression.value && !newNumber.value) {
        expression.value += value
        lastOperator.value = value
        newNumber.value = true
      } else if (result.value && value !== lastOperator.value) {
        expression.value = result.value + value
        lastOperator.value = value
        newNumber.value = true
      }
      break

    default:
      if (newNumber.value) {
        expression.value += value
        newNumber.value = false
      } else {
        expression.value += value
      }
      calculate()
  }
}
</script>

<style scoped>
.calculator-page {

  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 88vh;

  color: var(--text-primary);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.calculator {
  width: 100%;
  max-width: 400px;
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: visible;
  border: 1px solid rgba(255, 255, 255, 0.2);
  animation: calculatorAppear 0.5s ease-out;
  margin-bottom: 20px;
}

.display {
  background: linear-gradient(180deg, rgba(248, 249, 250, 0.9) 0%, rgba(255, 255, 255, 0.9) 100%);
  padding: 24px;
  text-align: right;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.display::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: scanline 2s linear infinite;
}

.expression {
  font-size: 20px;
  color: #6c757d;
  min-height: 30px;
  margin-bottom: 12px;
  word-break: break-all;
  font-family: 'Monaco', monospace;
  opacity: 0.8;
  transform-origin: right;
  animation: slideIn 0.3s ease-out;
}

.result {
  font-size: 40px;
  color: #345750;
  min-height: 60px;
  word-break: break-all;
  font-family: 'Monaco', monospace;
  font-weight: 600;
  position: relative;
  transform-origin: right;
  animation: slideIn 0.3s ease-out;
}

.keypad {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 2px;
  background: rgba(233, 236, 239, 0.5);
  padding: 2px;
  position: relative;
}

.button {
  padding: 24px;
  font-size: 24px;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: 'Monaco', monospace;
  position: relative;
  overflow: hidden;
  user-select: none;
}

.button::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at center, rgba(255, 255, 255, 0.8), transparent 70%);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.button:hover::before {
  opacity: 1;
}

.button::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100px;
  height: 100px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.8) 0%, transparent 70%);
  transform: translate(-50%, -50%) scale(0);
  opacity: 0;
  transition: transform 0.5s ease, opacity 0.3s ease;
  pointer-events: none;
}

.button:active::after {
  transform: translate(-50%, -50%) scale(3);
  opacity: 1;
}

.button.operator {
  color: #0d6efd;
  font-weight: bold;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 249, 250, 0.95) 100%);
}

.button.function {
  color: #dc3545;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 249, 250, 0.95) 100%);
}

.button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 1;
}

.button:active {
  transform: translateY(1px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

@keyframes calculatorAppear {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scanline {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(100%);
  }
}

@keyframes buttonPress {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(0.95);
  }
  100% {
    transform: scale(1);
  }
}

@media (max-width: 480px) {
  .calculator-page {
    height: 92vh;
  }
  .calculator {
    max-width: 100%;
  }

  .display {
    padding: 20px;
  }

  .expression {
    font-size: 18px;
  }

  .result {
    font-size: 36px;
  }

  .button {
    padding: 20px;
    font-size: 22px;
  }
}

/* 添加按键反馈的动画类 */
.button-pressed {
  animation: buttonPress 0.15s ease-in-out;
}

/* 为不同类型的按钮添加不同的光晕效果 */
.button.operator::before {
  background: radial-gradient(circle at center, rgba(13, 110, 253, 0.1), transparent 70%);
}

.button.function::before {
  background: radial-gradient(circle at center, rgba(220, 53, 69, 0.1), transparent 70%);
}

/* 添加玻璃态悬浮效果 */
.calculator:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
  transition: all 0.3s ease;
}

/* 添加结果变化的动画 */
.result.changed {
  animation: resultChange 0.3s ease-out;
}

@keyframes resultChange {
  0% {
    transform: scale(1.1);
    color: #0d6efd;
  }
  100% {
    transform: scale(1);
    color: #345750;
  }
}

/* 添加历史记录面板样式 */
.history-panel {
  position: fixed;
  right: 80px; /* 调整位置，避免与浮动按钮重叠 */
  top: 50%;
  transform: translate(100%, -50%);
  width: 300px;
  max-height: 80vh;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  z-index: 999;
}

.history-panel-open {
  transform: translate(0, -50%);
}

.history-header {
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: inherit;
  border-radius: 16px 16px 0 0;
  backdrop-filter: blur(10px);
}

.history-header h3 {
  margin: 0;
  color: #345750;
  font-size: 18px;
}

.history-actions {
  display: flex;
  gap: 8px;
}

.history-action-btn {
  background: none;
  border: none;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s ease;
}

.history-action-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.action-icon {
  font-size: 16px;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  margin: 0;
  max-height: calc(100vh - 240px); /* 确保列表不会太长 */
}

.history-item {
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 8px;
  background: rgba(248, 249, 250, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.history-item:last-child {
  margin-bottom: 0;
}

.history-item:hover {
  background: rgba(248, 249, 250, 0.9);
  transform: translateX(-4px);
}

.history-expression {
  color: #6c757d;
  font-size: 14px;
  margin-bottom: 4px;
  font-family: 'Monaco', monospace;
}

.history-result {
  color: #345750;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
  font-family: 'Monaco', monospace;
}

.history-time {
  color: #adb5bd;
  font-size: 12px;
}

.history-empty {
  padding: 24px;
  text-align: center;
  color: #adb5bd;
}

/* 自定义滚动条 */
.history-list::-webkit-scrollbar {
  width: 6px;
}

.history-list::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
}

.history-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.history-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

@media (max-width: 768px) {
  .floating-history-btn {
    right: 16px;
    bottom: 80px; /* 避免被底部导航栏遮挡 */
    top: auto;
    transform: none;
  }

  .history-panel {
    right: 0;
    top: 0;
    width: 100%;
    height: 100vh;
    max-height: none;
    transform: translateX(100%);
    border-radius: 0;
  }

  .history-panel-open {
    transform: translateX(0);
  }

  .history-header {
    padding-top: env(safe-area-inset-top);
  }

  .history-list {
    padding-bottom: env(safe-area-inset-bottom);
  }
}

/* 添加动画效果 */
@keyframes buttonPulse {
  0% {
    transform: translateY(-50%) scale(1);
  }
  50% {
    transform: translateY(-50%) scale(1.05);
  }
  100% {
    transform: translateY(-50%) scale(1);
  }
}

.floating-history-btn {
  animation: buttonPulse 2s infinite;
}

.floating-history-btn:hover {
  animation: none;
}

/* 添加浮动历史按钮样式 */
.floating-history-btn {
  position: fixed;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
  width: 50px;
  height: 50px;
  border-radius: 25px;
  background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(13, 110, 253, 0.2);
  transition: all 0.3s ease;
  z-index: 1000;
}

.floating-history-btn:hover {
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 16px rgba(13, 110, 253, 0.3);
}

.history-icon {
  font-size: 24px;
}

.history-count {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #dc3545;
  color: white;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 12px;
  min-width: 20px;
  text-align: center;
}
</style>
