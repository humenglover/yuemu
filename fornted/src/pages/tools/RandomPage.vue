<template>
  <div class="random-page">
    <div class="random-container">
      <div class="mode-section">
        <a-radio-group v-model:value="currentMode" button-style="solid">
          <a-radio-button value="single">
            <span class="mode-icon"><number-outlined /></span>
            <span class="mode-text">单个数字</span>
          </a-radio-button>
          <a-radio-button value="range">
            <span class="mode-icon"><swap-outlined /></span>
            <span class="mode-text">范围随机</span>
          </a-radio-button>
          <a-radio-button value="multiple">
            <span class="mode-icon"><bars-outlined /></span>
            <span class="mode-text">生成多个</span>
          </a-radio-button>
          <a-radio-button value="dice">
            <span class="mode-icon"><block-outlined /></span>
            <span class="mode-text">骰子模式</span>
          </a-radio-button>
        </a-radio-group>
      </div>

      <div class="display-section">
        <div class="result-display" :class="{ 'animate': isAnimating }">
          <template v-if="currentMode === 'dice'">
            <div class="dice-face">
              {{ result || '?' }}
            </div>
          </template>
          <template v-else>
            <div class="number-result">
              {{ displayResult }}
            </div>
          </template>
        </div>
      </div>

      <div class="controls-section">
        <!-- 单个数字模式 -->
        <div v-if="currentMode === 'single'" class="control-group">
          <div class="control-item">
            <span>最大值：</span>
            <a-input-number v-model:value="singleMax" :min="1" :max="9999" />
          </div>
        </div>

        <!-- 范围随机模式 -->
        <div v-if="currentMode === 'range'" class="control-group">
          <div class="control-item">
            <span>最小值：</span>
            <a-input-number v-model:value="rangeMin" :max="rangeMax - 1" />
          </div>
          <div class="control-item">
            <span>最大值：</span>
            <a-input-number v-model:value="rangeMax" :min="rangeMin + 1" />
          </div>
        </div>

        <!-- 生成多个模式 -->
        <div v-if="currentMode === 'multiple'" class="control-group">
          <div class="control-item">
            <span>最小值：</span>
            <a-input-number v-model:value="multipleMin" :max="multipleMax - 1" />
          </div>
          <div class="control-item">
            <span>最大值：</span>
            <a-input-number v-model:value="multipleMax" :min="multipleMin + 1" />
          </div>
          <div class="control-item">
            <span>生成数量：</span>
            <a-input-number v-model:value="multipleCount" :min="1" :max="10" />
          </div>
          <div class="control-item">
            <span>是否允许重复：</span>
            <a-switch v-model:checked="allowDuplicate" />
          </div>
        </div>

        <!-- 骰子模式 -->
        <div v-if="currentMode === 'dice'" class="control-group">
          <div class="control-item">
            <span>骰子面数：</span>
            <a-input-number v-model:value="diceFaces" :min="2" :max="100" />
          </div>
        </div>

        <div class="action-buttons">
          <a-button type="primary" @click="generate" :loading="isAnimating">
            生成随机数
          </a-button>
          <a-button @click="clearHistory">
            清空历史
          </a-button>
        </div>
      </div>

      <div class="history-section">
        <h3>生成历史</h3>
        <div class="history-list">
          <div v-for="(item, index) in history" :key="index" class="history-item">
            <span class="history-mode">{{ getModeLabel(item.mode) }}</span>
            <span class="history-result">{{ item.result }}</span>
            <span class="history-time">{{ formatTime(item.time) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import {
  NumberOutlined,
  SwapOutlined,
  BarsOutlined,
  BlockOutlined
} from '@ant-design/icons-vue'

// 状态定义
const currentMode = ref<'single' | 'range' | 'multiple' | 'dice'>('single')
const result = ref<number | number[] | null>(null)
const isAnimating = ref(false)

// 单个数字模式
const singleMax = ref(100)

// 范围随机模式
const rangeMin = ref(1)
const rangeMax = ref(100)

// 生成多个模式
const multipleMin = ref(1)
const multipleMax = ref(100)
const multipleCount = ref(5)
const allowDuplicate = ref(true)

// 骰子模式
const diceFaces = ref(6)

// 历史记录
interface HistoryItem {
  mode: string
  result: number | number[]
  time: number
}

const history = ref<HistoryItem[]>([])

// 计算属性
const displayResult = computed(() => {
  if (result.value === null) return '?'
  if (Array.isArray(result.value)) return result.value.join(', ')
  return result.value
})

// 添加watch监听器
watch(currentMode, () => {
  // 重置结果
  result.value = null
  // 重置动画状态
  isAnimating.value = false

  // 重置各模式的特定状态
  switch (currentMode.value) {
    case 'single':
      singleMax.value = 100
      break
    case 'range':
      rangeMin.value = 1
      rangeMax.value = 100
      break
    case 'multiple':
      multipleMin.value = 1
      multipleMax.value = 100
      multipleCount.value = 5
      allowDuplicate.value = true
      break
    case 'dice':
      diceFaces.value = 6
      break
  }
})

// 方法
const generate = async () => {
  if (isAnimating.value) return
  isAnimating.value = true

  // 动画效果
  let tempResult: number | number[] = 0
  for (let i = 0; i < 20; i++) {
    await new Promise(resolve => setTimeout(resolve, 50))
    if (currentMode.value === 'multiple') {
      tempResult = Array(multipleCount.value).fill(0).map(() =>
        Math.floor(Math.random() * (multipleMax.value - multipleMin.value + 1)) + multipleMin.value
      )
    } else {
      tempResult = Math.floor(Math.random() * 100)
    }
    result.value = tempResult
  }

  // 生成最终结果
  switch (currentMode.value) {
    case 'single':
      result.value = Math.floor(Math.random() * singleMax.value) + 1
      break
    case 'range':
      result.value = Math.floor(Math.random() * (rangeMax.value - rangeMin.value + 1)) + rangeMin.value
      break
    case 'multiple':
      if (allowDuplicate.value) {
        result.value = Array(multipleCount.value).fill(0).map(() =>
          Math.floor(Math.random() * (multipleMax.value - multipleMin.value + 1)) + multipleMin.value
        )
      } else {
        const numbers = new Set<number>()
        const range = multipleMax.value - multipleMin.value + 1
        if (multipleCount.value > range) {
          multipleCount.value = range
        }
        while (numbers.size < multipleCount.value) {
          numbers.add(Math.floor(Math.random() * range) + multipleMin.value)
        }
        result.value = Array.from(numbers)
      }
      break
    case 'dice':
      result.value = Math.floor(Math.random() * diceFaces.value) + 1
      break
  }

  // 添加到历史记录
  history.value.unshift({
    mode: currentMode.value,
    result: result.value,
    time: Date.now()
  })

  // 限制历史记录数量
  if (history.value.length > 10) {
    history.value = history.value.slice(0, 10)
  }

  isAnimating.value = false
}

const clearHistory = () => {
  history.value = []
}

const getModeLabel = (mode: string) => {
  const modeMap = {
    single: '单个数字',
    range: '范围随机',
    multiple: '生成多个',
    dice: '骰子模式'
  }
  return modeMap[mode as keyof typeof modeMap]
}

const formatTime = (timestamp: number) => {
  return dayjs(timestamp).format('HH:mm:ss')
}
</script>

<style scoped>
.random-page {
  height: 88vh;
  margin: -24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
  padding: 24px;
  min-height: calc(96vh - 60px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.random-container {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  padding: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: 24px;
  max-height: calc(88vh - 48px);
  overflow: hidden;
}

.mode-section {
  text-align: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.mode-section :deep(.ant-radio-group) {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.mode-section :deep(.ant-radio-button-wrapper) {
  margin: 0;
  border-radius: 8px !important;
  border: 1px solid #e5e7eb !important;
  padding: 4px 16px;
  height: 36px;
  line-height: 26px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 4px;
}

.mode-section :deep(.ant-radio-button-wrapper:not(:first-child)::before) {
  display: none;
}

.mode-section .mode-icon {
  font-size: 16px;
}

.mode-section .mode-text {
  margin-left: 4px;
}

.display-section {
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
}

.result-display {
  width: 280px;
  min-height: 220px;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  background: white;
  border-radius: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.result-display::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(16, 185, 129, 0.1));
  opacity: 0.5;
}

.result-display.animate {
  animation: pulse 0.5s ease-in-out;
}

.number-result {
  font-size: 48px;
  font-weight: bold;
  background: linear-gradient(135deg, #3b82f6, #10b981);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1;
  word-wrap: break-word;
  word-break: break-all;
  text-align: center;
  max-width: 100%;
  line-height: 1.4;
}

.dice-face {
  font-size: 96px;
  font-weight: bold;
  background: linear-gradient(135deg, #3b82f6, #10b981);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 1;
}

.controls-section {
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 24px;
  justify-content: center;
}

.control-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: white;
  padding: 8px 16px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.control-item span {
  color: #4b5563;
  font-size: 14px;
  white-space: nowrap;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}

.action-buttons .ant-btn {
  min-width: 120px;
  height: 40px;
  border-radius: 20px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.action-buttons .ant-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.history-section {
  padding: 20px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  max-height: 300px;
}

.history-section h3 {
  margin: 0 0 16px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  margin: -4px;
  padding: 4px;
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}

.history-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-track {
  background: transparent;
}

.history-list::-webkit-scrollbar-thumb {
  background-color: #d1d5db;
  border-radius: 2px;
}

.history-item {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  margin-bottom: 8px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.history-item:last-child {
  margin-bottom: 0;
}

.history-mode {
  color: #6b7280;
  font-size: 13px;
  padding: 4px 8px;
  background: #f3f4f6;
  border-radius: 6px;
  white-space: nowrap;
}

.history-result {
  font-weight: 600;
  color: #1f2937;
  font-size: 16px;
  text-align: center;
}

.history-time {
  color: #9ca3af;
  font-size: 13px;
  white-space: nowrap;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

@media (max-width: 768px) {
  .random-page {
    height: 94vh !important;
  }

  .random-container {
    padding: 20px;
    gap: 16px;
    max-height: calc(100vh - 84px);
    grid-template-rows: auto auto auto;
  }

  .mode-section {
    padding: 12px;
    background: transparent;
    box-shadow: none;
  }

  .mode-section :deep(.ant-radio-group) {
    gap: 12px;
  }

  .mode-section :deep(.ant-radio-button-wrapper) {
    padding: 8px;
    height: 40px;
    width: 40px;
    border-radius: 12px !important;
    display: flex;
    align-items: center;
    justify-content: center;
    background: white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  }

  .mode-section :deep(.ant-radio-button-wrapper-checked) {
    color: #1890ff;
    background: #e6f7ff;
    border-color: #1890ff !important;
  }

  .mode-section .mode-icon {
    font-size: 20px;
  }

  .mode-section .mode-text {
    display: none;
  }

  .display-section {
    padding: 10px 0;
  }

  .result-display {
    width: 240px;
    min-height: 180px;
    padding: 16px;
  }

  .number-result {
    font-size: 36px;
    line-height: 1.3;
  }

  .controls-section {
    padding: 16px;
    gap: 12px;
  }

  .history-section {
    display: none;
  }

  .action-buttons button:last-child {
    display: none;
  }

  .action-buttons .ant-btn {
    min-width: 140px;
  }
}
</style>
