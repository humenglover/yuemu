<template>
  <div class="timer-page">
    <div class="timer-container">
      <div class="mode-switch">
        <button
          :class="['mode-btn', { active: mode === 'stopwatch' }]"
          @click="switchMode('stopwatch')"
        >
          秒表
        </button>
        <button
          :class="['mode-btn', { active: mode === 'countdown' }]"
          @click="switchMode('countdown')"
        >
          倒计时
        </button>
      </div>

      <div class="display">
        <template v-if="mode === 'stopwatch'">
          <div class="time">{{ formatTime(stopwatchTime) }}</div>
          <div class="controls">
            <button class="control-btn" @click="startStopwatch" v-if="!isRunning">开始</button>
            <button class="control-btn" @click="pauseStopwatch" v-else>暂停</button>
            <button class="control-btn" @click="resetStopwatch">重置</button>
            <button class="control-btn" @click="lapStopwatch">计次</button>
          </div>
          <div class="laps" v-if="laps.length">
            <div class="lap" v-for="(lap, index) in laps" :key="index">
              <span>计次 {{ laps.length - index }}</span>
              <span>{{ formatTime(lap) }}</span>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="time">
            <input
              type="number"
              v-model="countdownMinutes"
              class="time-input"
              :disabled="isRunning"
              min="0"
              max="99"
            >:
            <input
              type="number"
              v-model="countdownSeconds"
              class="time-input"
              :disabled="isRunning"
              min="0"
              max="59"
            >
          </div>
          <div class="countdown-display" v-if="isRunning || countdownTime > 0">
            {{ formatTime(countdownTime) }}
          </div>
          <div class="controls">
            <button class="control-btn" @click="startCountdown" v-if="!isRunning">开始</button>
            <button class="control-btn" @click="pauseCountdown" v-else>暂停</button>
            <button class="control-btn" @click="resetCountdown">重置</button>
          </div>
        </template>
      </div>

      <div class="history-panel" v-if="mode === 'countdown'">
        <div class="history-header">
          <h3>历史记录</h3>
          <button class="clear-history-btn" @click="clearHistory" v-if="timerHistory.length">
            清除历史
          </button>
        </div>
        <div class="history-list" v-if="timerHistory.length">
          <div class="history-item" v-for="(item, index) in timerHistory" :key="index">
            <div class="history-time">{{ formatTime(item.duration) }}</div>
            <div class="history-date">{{ formatDate(item.date) }}</div>
            <button class="use-time-btn" @click="useHistoryTime(item)" :disabled="isRunning">
              使用此时间
            </button>
          </div>
        </div>
        <div class="no-history" v-else>
          暂无历史记录
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface TimerHistoryItem {
  duration: number
  date: number
}

const mode = ref<'stopwatch' | 'countdown'>('stopwatch')
const isRunning = ref(false)
const stopwatchTime = ref(0)
const countdownTime = ref(0)
const countdownMinutes = ref(0)
const countdownSeconds = ref(0)
const laps = ref<number[]>([])
const timerHistory = ref<TimerHistoryItem[]>([])
let intervalId: number | null = null

// Load history from localStorage
onMounted(() => {
  const savedHistory = localStorage.getItem('timerHistory')
  if (savedHistory) {
    timerHistory.value = JSON.parse(savedHistory)
  }
})

const saveHistory = () => {
  localStorage.setItem('timerHistory', JSON.stringify(timerHistory.value))
}

const clearHistory = () => {
  timerHistory.value = []
  saveHistory()
}

const addToHistory = (duration: number) => {
  timerHistory.value.unshift({
    duration,
    date: Date.now()
  })
  if (timerHistory.value.length > 10) {
    timerHistory.value.pop()
  }
  saveHistory()
}

const useHistoryTime = (item: TimerHistoryItem) => {
  const minutes = Math.floor(item.duration / 60000)
  const seconds = Math.floor((item.duration % 60000) / 1000)
  countdownMinutes.value = minutes
  countdownSeconds.value = seconds
  countdownTime.value = item.duration
}

const formatDate = (timestamp: number) => {
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const switchMode = (newMode: 'stopwatch' | 'countdown') => {
  if (isRunning.value) {
    stopTimer()
  }
  mode.value = newMode
  resetTimer()
}

const formatTime = (ms: number) => {
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  const centiseconds = Math.floor((ms % 1000) / 10)

  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.${centiseconds.toString().padStart(2, '0')}`
}

const startTimer = (callback: () => void) => {
  if (!isRunning.value) {
    isRunning.value = true
    intervalId = window.setInterval(callback, 10)
  }
}

const stopTimer = () => {
  if (intervalId !== null) {
    clearInterval(intervalId)
    intervalId = null
  }
  isRunning.value = false
}

const resetTimer = () => {
  stopTimer()
  if (mode.value === 'stopwatch') {
    stopwatchTime.value = 0
    laps.value = []
  } else {
    countdownTime.value = countdownMinutes.value * 60000 + countdownSeconds.value * 1000
  }
}

// Stopwatch functions
const startStopwatch = () => {
  startTimer(() => {
    stopwatchTime.value += 10
  })
}

const pauseStopwatch = () => {
  stopTimer()
}

const resetStopwatch = () => {
  resetTimer()
}

const lapStopwatch = () => {
  if (isRunning.value) {
    laps.value.unshift(stopwatchTime.value)
  }
}

// Countdown functions
const startCountdown = () => {
  if (countdownTime.value === 0) {
    const duration = countdownMinutes.value * 60000 + countdownSeconds.value * 1000
    countdownTime.value = duration
    addToHistory(duration)
  }
  if (countdownTime.value > 0) {
    startTimer(() => {
      if (countdownTime.value > 0) {
        countdownTime.value -= 10
      } else {
        stopTimer()
        // Play sound when countdown reaches zero
        const audio = new Audio('/sounds/timer-end.mp3')
        audio.play()
      }
    })
  }
}

const pauseCountdown = () => {
  stopTimer()
}

const resetCountdown = () => {
  resetTimer()
}

onUnmounted(() => {
  if (intervalId !== null) {
    clearInterval(intervalId)
  }
})
</script>

<style scoped>
.timer-page {
  margin: -20px;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 88vh;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  padding: 20px;
}

.timer-container {
  width: 100%;
  max-width: 400px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.mode-switch {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.mode-btn {
  flex: 1;
  padding: 12px;
  border: none;
  border-radius: 12px;
  background: rgba(248, 249, 250, 0.8);
  color: #6c757d;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
  font-weight: 500;
}

.mode-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.mode-btn.active {
  background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
  color: #fff;
  box-shadow: 0 4px 12px rgba(13, 110, 253, 0.2);
}

.display {
  text-align: center;
  padding: 20px;
  background: rgba(248, 249, 250, 0.8);
  border-radius: 16px;
  margin-bottom: 20px;
}

.time {
  font-size: 56px;
  font-family: 'Monaco', monospace;
  margin: 24px 0;
  color: #345750;
  font-weight: 600;
  letter-spacing: 2px;
}

.time-input {
  width: 70px;
  font-size: 56px;
  font-family: 'Monaco', monospace;
  text-align: center;
  border: none;
  border-bottom: 2px solid rgba(222, 226, 230, 0.5);
  color: #345750;
  background: transparent;
  font-weight: 600;
  transition: all 0.3s ease;
}

.time-input:focus {
  outline: none;
  border-bottom-color: #0d6efd;
}

.countdown-display {
  font-size: 28px;
  color: #6c757d;
  margin: 16px 0;
  font-family: 'Monaco', monospace;
}

.controls {
  display: flex;
  gap: 12px;
  margin: 24px 0;
}

.control-btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 16px;
  font-weight: 500;
  position: relative;
  overflow: hidden;
}

.control-btn::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  transform: translate(-50%, -50%) scale(0);
  opacity: 0;
  transition: transform 0.5s ease, opacity 0.3s ease;
}

.control-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(13, 110, 253, 0.2);
}

.control-btn:active {
  transform: translateY(0);
}

.control-btn:active::after {
  transform: translate(-50%, -50%) scale(2);
  opacity: 1;
}

.laps {
  max-height: 200px;
  overflow-y: auto;
  border-radius: 12px;
  background: rgba(248, 249, 250, 0.8);
  margin-top: 24px;
}

.lap {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(222, 226, 230, 0.5);
  color: #6c757d;
  font-family: 'Monaco', monospace;
  transition: background-color 0.2s ease;
}

.lap:hover {
  background-color: rgba(255, 255, 255, 0.5);
}

.lap:last-child {
  border-bottom: none;
}

@media (max-width: 480px) {
  .timer-page {
    min-height: 94vh;
  }

  .timer-container {
    max-width: 100%;
    padding: 20px;
  }

  .time {
    font-size: 44px;
  }

  .time-input {
    font-size: 44px;
    width: 55px;
  }

  .control-btn {
    padding: 10px 20px;
    font-size: 15px;
  }

  .mode-btn {
    padding: 10px;
    font-size: 15px;
  }
}

/* Custom scrollbar for the laps container */
.laps::-webkit-scrollbar {
  width: 8px;
}

.laps::-webkit-scrollbar-track {
  background: rgba(222, 226, 230, 0.3);
  border-radius: 4px;
}

.laps::-webkit-scrollbar-thumb {
  background: rgba(108, 117, 125, 0.3);
  border-radius: 4px;
  transition: background-color 0.3s ease;
}

.laps::-webkit-scrollbar-thumb:hover {
  background: rgba(108, 117, 125, 0.5);
}

.history-panel {
  margin-top: 24px;
  background: rgba(248, 249, 250, 0.8);
  border-radius: 16px;
  padding: 20px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.history-header h3 {
  margin: 0;
  color: #345750;
  font-size: 18px;
  font-weight: 600;
}

.clear-history-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  background: rgba(220, 53, 69, 0.1);
  color: #dc3545;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.clear-history-btn:hover {
  background: rgba(220, 53, 69, 0.2);
}

.history-list {
  max-height: 300px;
  overflow-y: auto;
  padding-right: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 12px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.history-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateX(4px);
}

.history-time {
  font-family: 'Monaco', monospace;
  font-size: 16px;
  color: #345750;
  font-weight: 500;
}

.history-date {
  color: #6c757d;
  font-size: 14px;
}

.use-time-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 8px;
  background: rgba(13, 110, 253, 0.1);
  color: #0d6efd;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
}

.use-time-btn:hover:not(:disabled) {
  background: rgba(13, 110, 253, 0.2);
}

.use-time-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.no-history {
  text-align: center;
  color: #6c757d;
  padding: 24px;
  font-size: 14px;
}

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
  background: rgba(0, 0, 0, 0.15);
}

@media (max-width: 480px) {
  .timer-container {
    padding: 16px;
  }

  .time {
    font-size: 48px;
  }

  .time-input {
    width: 60px;
    font-size: 48px;
  }

  .history-item {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }

  .use-time-btn {
    width: 100%;
  }
}
</style>
