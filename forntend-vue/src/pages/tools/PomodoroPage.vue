<template>
  <div class="pomodoro-page">
    <div class="pomodoro-container">
      <div class="timer-section">
        <div class="mode-switch">
          <a-radio-group v-model:value="currentMode" button-style="solid">
            <a-radio-button value="work" class="work-mode">工作</a-radio-button>
            <a-radio-button value="break" class="break-mode">休息</a-radio-button>
          </a-radio-group>
        </div>

        <div class="timer-display" :class="{ active: isRunning, 'work-mode': currentMode === 'work', 'break-mode': currentMode === 'break' }">
          <div class="time">{{ formatTime }}</div>
          <div class="progress-ring">
            <svg width="300" height="300" viewBox="0 0 300 300">
              <circle
                cx="150"
                cy="150"
                r="140"
                fill="none"
                stroke="#e5e7eb"
                stroke-width="10"
              />
              <circle
                cx="150"
                cy="150"
                r="140"
                fill="none"
                :stroke="currentMode === 'work' ? '#3b82f6' : '#10b981'"
                stroke-width="10"
                stroke-linecap="round"
                :stroke-dasharray="circumference"
                :stroke-dashoffset="dashOffset"
                transform="rotate(-90 150 150)"
                class="progress-circle"
              />
            </svg>
          </div>
        </div>

        <div class="timer-controls">
          <a-button
            type="primary"
            shape="circle"
            size="large"
            @click="toggleTimer"
            :icon="isRunning ? h(PauseOutlined) : h(PlayCircleOutlined)"
            class="control-btn"
          />
          <a-button
            shape="circle"
            size="large"
            @click="resetTimer"
            :icon="h(ReloadOutlined)"
            class="control-btn"
          />
          <a-button
            shape="circle"
            size="large"
            @click="showSettingsModal = true"
            :icon="h(SettingOutlined)"
            class="control-btn"
          />
        </div>

        <div class="session-info">
          <span>已完成 {{ completedSessions }} 个番茄钟</span>
        </div>
      </div>

      <div class="tasks-section">
        <div class="tasks-header">
          <h2>任务列表</h2>
          <a-button type="primary" @click="showAddTaskModal = true" class="add-task-btn">
            <template #icon><PlusOutlined /></template>
            添加任务
          </a-button>
        </div>

        <div class="tasks-list">
          <div
            v-for="task in tasks"
            :key="task.id"
            class="task-item"
            :class="{ completed: task.completed }"
          >
            <a-checkbox
              v-model:checked="task.completed"
              @change="updateTask(task)"
            >
              {{ task.title }}
            </a-checkbox>
            <div class="task-actions">
              <span class="pomodoro-count">{{ task.pomodoroCount }} 个番茄钟</span>
              <a-button
                type="text"
                @click="deleteTask(task.id)"
                :icon="h(DeleteOutlined)"
                class="delete-btn"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 设置弹窗 -->
    <a-modal
      v-model:visible="showSettingsModal"
      title="设置"
      @ok="saveSettings"
      okText="保存"
      cancelText="取消"
      class="settings-modal"
    >
      <div class="settings-form">
        <div class="setting-item">
          <span>工作时长 (分钟)</span>
          <a-input-number v-model:value="settings.workDuration" :min="1" :max="60" />
        </div>
        <div class="setting-item">
          <span>休息时长 (分钟)</span>
          <a-input-number v-model:value="settings.breakDuration" :min="1" :max="30" />
        </div>
        <div class="setting-item">
          <span>声音提醒</span>
          <a-switch v-model:checked="settings.soundEnabled" />
        </div>
      </div>
    </a-modal>

    <!-- 添加任务弹窗 -->
    <a-modal
      v-model:visible="showAddTaskModal"
      title="添加任务"
      @ok="addTask"
      okText="添加"
      cancelText="取消"
      class="task-modal"
    >
      <div class="task-form">
        <a-input v-model:value="newTask.title" placeholder="任务名称" />
        <div class="pomodoro-count-input">
          <span>预计番茄钟数量：</span>
          <a-input-number v-model:value="newTask.pomodoroCount" :min="1" :max="10" />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted, onUnmounted } from 'vue'
import {
  PlayCircleOutlined,
  PauseOutlined,
  ReloadOutlined,
  SettingOutlined,
  PlusOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

// 类型定义
interface Task {
  id: number
  title: string
  completed: boolean
  pomodoroCount: number
}

interface Settings {
  workDuration: number
  breakDuration: number
  soundEnabled: boolean
}

// 状态管理
const currentMode = ref<'work' | 'break'>('work')
const timeLeft = ref(25 * 60) // 默认25分钟
const isRunning = ref(false)
const timer = ref<number | null>(null)
const completedSessions = ref(0)
const showSettingsModal = ref(false)
const showAddTaskModal = ref(false)
const tasks = ref<Task[]>([])
const circumference = 2 * Math.PI * 140

// 设置
const settings = ref<Settings>({
  workDuration: 25,
  breakDuration: 5,
  soundEnabled: true
})

// 新任务表单
const newTask = ref({
  title: '',
  pomodoroCount: 1
})

// 计算属性
const formatTime = computed(() => {
  const minutes = Math.floor(timeLeft.value / 60)
  const seconds = timeLeft.value % 60
  // 使用String.padStart替代padStart方法以避免TypeScript错误
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const dashOffset = computed(() => {
  const progress = timeLeft.value / (currentMode.value === 'work' ? settings.value.workDuration * 60 : settings.value.breakDuration * 60)
  return circumference * (1 - progress)
})

// 方法
const startTimer = () => {
  if (timer.value) return
  timer.value = window.setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
    } else {
      handleTimerComplete()
    }
  }, 1000)
  isRunning.value = true
}

const pauseTimer = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
  isRunning.value = false
}

const toggleTimer = () => {
  if (isRunning.value) {
    pauseTimer()
  } else {
    startTimer()
  }
}

const resetTimer = () => {
  pauseTimer()
  timeLeft.value = currentMode.value === 'work' ? settings.value.workDuration * 60 : settings.value.breakDuration * 60
}

const handleTimerComplete = () => {
  pauseTimer()
  playSound()

  if (currentMode.value === 'work') {
    completedSessions.value++
    currentMode.value = 'break'
    timeLeft.value = settings.value.breakDuration * 60
    message.success('太棒了！该休息一下了')
  } else {
    currentMode.value = 'work'
    timeLeft.value = settings.value.workDuration * 60
    message.success('休息结束，继续工作吧')
  }
}

const playSound = () => {
  if (settings.value.soundEnabled) {
    const audio = new Audio('/sounds/timer-complete.mp3')
    audio.play().catch(e => console.log("Audio play failed:", e))
  }
}

// 任务管理
const addTask = () => {
  if (!newTask.value.title.trim()) {
    message.warning('请输入任务名称')
    return
  }

  tasks.value.push({
    id: Date.now(),
    title: newTask.value.title,
    completed: false,
    pomodoroCount: newTask.value.pomodoroCount
  })

  newTask.value.title = ''
  newTask.value.pomodoroCount = 1
  showAddTaskModal.value = false
  saveTasks()
}

const updateTask = (task: Task) => {
  const index = tasks.value.findIndex((t: Task) => t.id === task.id)
  if (index !== -1) {
    tasks.value[index] = { ...task }
    saveTasks()
  }
}

const deleteTask = (id: number) => {
  tasks.value = tasks.value.filter((task: Task) => task.id !== id)
  saveTasks()
}

// 数据持久化
const saveTasks = () => {
  localStorage.setItem('pomodoro-tasks', JSON.stringify(tasks.value))
}

const loadTasks = () => {
  const savedTasks = localStorage.getItem('pomodoro-tasks')
  if (savedTasks) {
    tasks.value = JSON.parse(savedTasks)
  }
}

const saveSettings = () => {
  localStorage.setItem('pomodoro-settings', JSON.stringify(settings.value))
  showSettingsModal.value = false
  resetTimer()
  message.success('设置已保存')
}

const loadSettings = () => {
  const savedSettings = localStorage.getItem('pomodoro-settings')
  if (savedSettings) {
    settings.value = JSON.parse(savedSettings)
    timeLeft.value = settings.value.workDuration * 60
  }
}

// 生命周期钩子
onMounted(() => {
  loadSettings()
  loadTasks()
})

onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value)
  }
})
</script>

<style scoped>
.pomodoro-page {
  max-height: 88vh;
  background: #f5f5f7;
  color: #1d1d1f;
  overflow: hidden;
  margin: -24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
}

.pomodoro-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
  padding: 32px;
  background: #ffffff;
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05);
  height: calc(100vh - 100px);
  overflow: hidden;
}

.timer-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 20px;
  height: 100%;
}

.mode-switch {
  margin-bottom: 16px;
}

.mode-switch :deep(.ant-radio-button) {
  border: none;
  border-radius: 10px;
  padding: 6px 20px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.mode-switch :deep(.ant-radio-button-checked) {
  color: white !important;
}

.mode-switch :deep(.ant-radio-button:first-child) {
  border-radius: 10px 0 0 10px;
}

.mode-switch :deep(.ant-radio-button:last-child) {
  border-radius: 0 10px 10px 0;
}

.mode-switch .work-mode {
  background-color: #ff3b30;
}

.mode-switch .break-mode {
  background-color: #34c759;
}

.timer-display {
  position: relative;
  width: 300px;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 20px 0;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.timer-display.work-mode {
  box-shadow: 0 4px 20px rgba(255, 59, 48, 0.1);
}

.timer-display.break-mode {
  box-shadow: 0 4px 20px rgba(52, 199, 89, 0.1);
}

.timer-display.active {
  transform: scale(1.02);
}

.time {
  position: absolute;
  font-size: 54px;
  font-weight: 600;
  color: #1d1d1f;
  z-index: 1;
  font-family: 'SF Mono', 'Monaco', 'Courier New', monospace;
}

.progress-ring {
  position: absolute;
  transform: rotate(-90deg);
}

.progress-circle {
  transition: stroke-dashoffset 1s linear;
}

.timer-controls {
  display: flex;
  gap: 20px;
  margin-top: 30px;
}

.control-btn {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border: none;
  font-size: 20px;
}

.control-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.control-btn:active {
  transform: scale(0.98);
}

.control-btn.work-mode {
  background-color: #ff3b30;
  color: white;
}

.control-btn.break-mode {
  background-color: #34c759;
  color: white;
}

.reset-btn {
  background-color: #f5f5f7;
  color: #1d1d1f;
}

.session-info {
  margin-top: 16px;
  color: #86868b;
  font-size: 14px;
  background: #f5f5f7;
  padding: 8px 16px;
  border-radius: 20px;
  font-weight: 500;
}

.tasks-section {
  padding: 24px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  border: 1px solid #f5f5f7;
}

.tasks-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.tasks-header h2 {
  margin: 0;
  font-size: 24px;
  color: #1d1d1f;
  font-weight: 600;
}

.add-task-btn {
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
  background: #007aff;
  border: none;
  color: white;
  font-weight: 500;
  padding: 6px 16px;
}

.add-task-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

.tasks-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 8px;
  margin-right: -8px;
  scrollbar-width: thin;
  scrollbar-color: #d1d5db transparent;
}

.tasks-list::-webkit-scrollbar {
  width: 6px;
}

.tasks-list::-webkit-scrollbar-track {
  background: transparent;
}

.tasks-list::-webkit-scrollbar-thumb {
  background-color: #d1d5db;
  border-radius: 3px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 12px;
  background: #f5f5f7;
  transition: all 0.2s ease;
  border: 1px solid #e5e5ea;
}

.task-item:hover {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-color: #d1d1d6;
}

.task-item.completed {
  opacity: 0.7;
  background: #f5f5f7;
  text-decoration: line-through;
}

.task-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pomodoro-count {
  font-size: 12px;
  color: #86868b;
  background: #ffffff;
  padding: 4px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.delete-btn {
  color: #ff3b30;
  transition: all 0.2s ease;
  font-size: 16px;
}

.delete-btn:hover {
  color: #ff3b30;
  transform: scale(1.1);
}

.settings-form,
.task-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #e5e5ea;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-item span {
  font-size: 16px;
  color: #1d1d1f;
  font-weight: 500;
}

.pomodoro-count-input {
  margin-top: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.pomodoro-count-input span {
  font-size: 16px;
  color: #1d1d1f;
  font-weight: 500;
}

.settings-modal .ant-modal-content,
.task-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.settings-modal :deep(.ant-modal-header),
.task-modal :deep(.ant-modal-header) {
  background: #ffffff;
  border-bottom: 1px solid #e5e5ea;
  padding: 16px 24px;
}

.settings-modal :deep(.ant-modal-title),
.task-modal :deep(.ant-modal-title) {
  color: #1d1d1f;
  font-size: 20px;
  font-weight: 600;
}

.settings-modal :deep(.ant-modal-footer),
.task-modal :deep(.ant-modal-footer) {
  background: #f5f5f7;
  border-top: 1px solid #e5e5ea;
  padding: 12px 16px;
}

.settings-modal :deep(.ant-btn),
.task-modal :deep(.ant-btn) {
  border-radius: 10px;
  font-weight: 500;
}

.settings-modal :deep(.ant-btn-primary),
.task-modal :deep(.ant-btn-primary) {
  background: #007aff;
  border-color: #007aff;
}

@media (max-width: 768px) {
  .pomodoro-page {
    margin: -24px;
    padding: 0;
    height: 100vh;
    max-height: calc(100vh - 60px);
    align-items: flex-start;
  }

  .pomodoro-container {
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 16px;
    height: 100%;
    max-height: 100%;
    border-radius: 0;
    display: flex;
    flex-direction: column;
  }

  .timer-section {
    padding: 0;
    gap: 12px;
    flex-shrink: 0;
    height: auto;
    max-height: 45vh;
    min-height: 320px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .mode-switch {
    margin-bottom: 8px;
  }

  .timer-display {
    width: min(60vw, 280px);
    height: min(60vw, 280px);
    margin: 0 auto;
    background: #ffffff;
    border-radius: 50%;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }

  .progress-ring {
    display: none;
  }

  .time {
    font-size: min(8vw, 42px);
    position: static;
  }

  .timer-controls {
    gap: 16px;
    margin-top: 12px;
  }

  .control-btn {
    width: 50px;
    height: 50px;
  }

  .session-info {
    margin-top: 8px;
    font-size: 13px;
  }

  .tasks-section {
    padding: 12px;
    flex: 1;
    min-height: 0;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    margin: 0 4px;
  }

  .tasks-header {
    margin-bottom: 12px;
  }

  .tasks-header h2 {
    font-size: 18px;
  }

  .tasks-list {
    flex: 1;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 60px;
  }

  .task-item {
    padding: 12px;
    margin-bottom: 8px;
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .pomodoro-page {
    margin: -24px;
  }

  .pomodoro-container {
    padding: 12px 8px;
    gap: 12px;
  }

  .timer-section {
    padding: 0;
    max-height: 40vh;
    min-height: 280px;
  }

  .timer-display {
    width: min(70vw, 240px);
    height: min(70vw, 240px);
  }

  .time {
    font-size: min(9vw, 36px);
  }

  .control-btn {
    width: 44px;
    height: 44px;
  }

  .tasks-section {
    padding: 10px;
    margin: 0 2px;
  }

  .task-item {
    padding: 10px;
    margin-bottom: 6px;
  }

  .task-actions {
    gap: 8px;
  }

  .pomodoro-count {
    font-size: 12px;
  }
}
</style>
