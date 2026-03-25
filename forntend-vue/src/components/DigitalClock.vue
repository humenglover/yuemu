<!-- 数字时钟组件（蓝天白云色系美化版） -->
<template>
  <div class="digital-clock-container">
    <div class="time-section">
      <div class="time">{{ timeString }}</div>
      <div class="time-dots">
        <span class="dot"></span>
        <span class="dot"></span>
      </div>
    </div>
    <div class="date">{{ dateString }}</div>
    <div class="decoration-icons">
      <span class="floating-icon">☁️</span>
      <span class="floating-icon">🌤️</span>
      <span class="floating-icon">☀️</span>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: 'DigitalClock'
})

import { ref, onMounted, onUnmounted } from 'vue'

const timeString = ref('')
const dateString = ref('')
let timer: number | null = null

// 更新时间
const updateTime = () => {
  const now = new Date()

  // 格式化时间
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  timeString.value = `${hours}:${minutes}:${seconds}`

  // 格式化日期
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const date = String(now.getDate()).padStart(2, '0')
  const day = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]
  dateString.value = `${month}月${date}日 ${day}`
}

// 组件挂载时启动定时器
onMounted(() => {
  updateTime() // 立即执行一次
  timer = window.setInterval(updateTime, 1000)
})

// 组件卸载时清除定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.digital-clock-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 24px;
  border-radius: 20px;
  /* 蓝天白云主背景：浅蓝渐变+玻璃拟态 */
  background: linear-gradient(135deg, rgba(187, 222, 251, 0.25), rgba(144, 202, 249, 0.15));
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow:
    0 8px 32px rgba(30, 136, 229, 0.12),
    inset 0 0 0 1px rgba(255, 255, 255, 0.2);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  /* 保留原有高度，不做修改 */
}

.digital-clock-container:hover {
  transform: translateY(-2px);
  box-shadow:
    0 12px 40px rgba(30, 136, 229, 0.18),
    inset 0 0 0 1px rgba(255, 255, 255, 0.3);
}

.time-section {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}

.time {
  font-size: 32px;
  font-weight: 600;
  color: #ffffff;
  font-family: 'Monaco', monospace;
  letter-spacing: 2px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  /* 蓝天白云渐变文字：天蓝到浅蓝 */
  background: linear-gradient(to right, #1976d2, #64b5f6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.time-dots {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-left: 4px;
}

.dot {
  width: 4px;
  height: 4px;
  /* 圆点改为天空蓝 */
  background-color: #2196f3;
  border-radius: 50%;
  animation: pulse 1s ease-in-out infinite;
}

.dot:nth-child(2) {
  animation-delay: 0.5s;
}

.date {
  font-size: 14px;
  /* 日期文字改为浅蓝灰，更贴合蓝天白云 */
  color: rgba(25, 118, 210, 0.85);
  margin-top: 4px;
  font-weight: 500;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.2);
}

.decoration-icons {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  pointer-events: none;
  overflow: hidden;
}

.floating-icon {
  position: absolute;
  font-size: 12px;
  opacity: 0.6;
  animation: float 6s ease-in-out infinite;
  /* 装饰元素改为蓝天白云相关 */
}

.floating-icon:nth-child(1) {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.floating-icon:nth-child(2) {
  top: 60%;
  right: 15%;
  animation-delay: -2s;
}

.floating-icon:nth-child(3) {
  bottom: 20%;
  left: 20%;
  animation-delay: -4s;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.6;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-10px) rotate(10deg);
  }
}
</style>
