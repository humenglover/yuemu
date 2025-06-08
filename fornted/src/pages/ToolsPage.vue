<template>
  <div class="tools-page">
    <div class="tools-container">
      <div class="tools-header">
        <h1>实用工具</h1>
        <p>这里提供了一些实用的小工具，希望能帮助到你~</p>
      </div>
      <div class="tools-grid">
        <div v-for="tool in tools" :key="tool.key" class="tool-card" @click="handleToolClick(tool)">
          <div class="tool-icon">
            <component :is="tool.icon" />
          </div>
          <div class="tool-info">
            <h3>{{ tool.name }}</h3>
            <p>{{ tool.description }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端九宫格布局 -->
    <div class="mobile-tools-grid">
      <div v-for="tool in tools" :key="tool.key" class="mobile-tool-card" @click="handleToolClick(tool)">
        <div class="mobile-tool-icon">
          <component :is="tool.icon" />
        </div>
        <div class="mobile-tool-name">{{ tool.name }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { h } from '@vue/runtime-dom'
import { useRouter } from 'vue-router'
import {
  CalculatorOutlined,
  ClockCircleOutlined,
  BookOutlined,
  CoffeeOutlined,
  FileTextOutlined,
  FieldTimeOutlined,
  NumberOutlined,
  RetweetOutlined,
  BgColorsOutlined,
  VideoCameraOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()

interface Tool {
  key: string
  name: string
  description: string
  icon: any
  path: string
}

const tools: Tool[] = [
  {
    key: 'reminder',
    name: '记事本',
    description: '记录待办事项',
    icon: () => h(BookOutlined),
    path: '/reminder'
  },
  // {
  //   key: 'video-compressor',
  //   name: '视频压缩',
  //   description: '在线视频压缩工具',
  //   icon: () => h(VideoCameraOutlined),
  //   path: '/tools/video-compressor'
  // },
  {
    key: 'calculator',
    name: '计算器',
    description: '简单好用的计算器',
    icon: () => h(CalculatorOutlined),
    path: '/tools/calculator'
  },
  {
    key: 'timer',
    name: '计时器',
    description: '倒计时/秒表工具',
    icon: () => h(ClockCircleOutlined),
    path: '/tools/timer'
  },
  {
    key: 'food-wheel',
    name: '今天吃什么',
    description: '随机推荐美食',
    icon: () => h(CoffeeOutlined),
    path: '/tools/food-wheel'
  },
  {
    key: 'sticky-wall',
    name: '便签墙',
    description: '记录灵感与待办',
    icon: () => h(FileTextOutlined),
    path: '/tools/sticky-wall'
  },
  {
    key: 'pomodoro',
    name: '番茄钟',
    description: '专注时间管理',
    icon: () => h(FieldTimeOutlined),
    path: '/tools/pomodoro'
  },
  {
    key: 'random',
    name: '随机数生成器',
    description: '生成随机数字',
    icon: () => h(NumberOutlined),
    path: '/tools/random'
  },
  {
    key: 'base-converter',
    name: '进制转换器',
    description: '数字进制转换',
    icon: () => h(RetweetOutlined),
    path: '/tools/base-converter'
  },
  {
    key: 'color-picker',
    name: '颜色选择器',
    description: '颜色选择与转换',
    icon: () => h(BgColorsOutlined),
    path: '/tools/color-picker'
  },
]

const handleToolClick = (tool: Tool) => {
  router.push(tool.path)
}
</script>

<style scoped>
.tools-page {
  min-height: 86vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  position: relative;
  overflow: hidden;
  margin-left: -20px;
  margin-right: -20px;
  margin-top: -20px;
}

.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(52, 87, 80, 0.05), rgba(58, 143, 183, 0.05));
  animation: gradientMove 10s ease infinite;
}

@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.tools-container {
  position: relative;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 40px 20px;
}

.tools-header {
  text-align: center;
  margin-bottom: 40px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  backdrop-filter: blur(10px);
}

.tools-header h1 {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.tools-header p {
  font-size: 16px;
  color: #666;
  font-weight: 400;
  opacity: 0.9;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 0;
}

.tool-card {
  background: white;
  border-radius: 16px;
  padding: 0;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  position: relative;
}

.tool-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: rgba(0, 0, 0, 0.08);
}

.tool-icon {
  width: 100%;
  height: 0;
  padding-bottom: 100%;
  position: relative;
  border-radius: 16px 16px 0 0;
  overflow: hidden;
}

.tool-icon > div {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: inherit;
}

/* 工具卡片背景色 */
.tool-card:nth-child(1) .tool-icon {
  background: linear-gradient(135deg, #FF9D6C, #FF5E62);
}

.tool-card:nth-child(2) .tool-icon {
  background: linear-gradient(135deg, #edc22e, #f2b179);
}

.tool-card:nth-child(3) .tool-icon {
  background: linear-gradient(135deg, #00c6fb, #005bea);
}

.tool-card:nth-child(4) .tool-icon {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
}

.tool-card:nth-child(5) .tool-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
}

.tool-card:nth-child(6) .tool-icon {
  background: linear-gradient(135deg, #f0d9b5, #b58863);
}

.tool-card:nth-child(7) .tool-icon {
  background: linear-gradient(135deg, #00c6fb, #005bea);
}

.tool-card:nth-child(8) .tool-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
}

.tool-card:nth-child(9) .tool-icon {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
}

.tool-card:nth-child(10) .tool-icon {
  background: linear-gradient(135deg, #9C27B0, #E91E63);
}

.tool-icon :deep(svg) {
  width: 42px;
  height: 42px;
  color: #fff;
  transition: all 0.3s ease;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.tool-card:hover .tool-icon :deep(svg) {
  transform: translate(-50%, -50%) scale(1.1);
  color: #fff;
}

.tool-info {
  padding: 16px;
  background: #fff;
}

.tool-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tool-info p {
  font-size: 14px;
  color: #666;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  height: 40px;
}

/* 移动端九宫格样式 */
.mobile-tools-grid {
  display: none;
  padding: 20px;
  gap: 16px;
  grid-template-columns: repeat(3, 1fr);
  animation: fadeInUp 0.6s ease;
}

.mobile-tool-card {
  aspect-ratio: 1;
  background: white;
  border-radius: 16px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}

.mobile-tool-card:active {
  transform: scale(0.95);
  background: rgba(255, 255, 255, 0.95);
}

.mobile-tool-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  margin-bottom: 4px;
  position: relative;
}

.mobile-tool-icon :deep(svg) {
  width: 24px;
  height: 24px;
  color: #fff;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 移动端图标颜色 */
.mobile-tool-card:nth-child(1) .mobile-tool-icon {
  background: linear-gradient(135deg, #FF9D6C, #FF5E62);
}

.mobile-tool-card:nth-child(2) .mobile-tool-icon {
  background: linear-gradient(135deg, #edc22e, #f2b179);
}

.mobile-tool-card:nth-child(3) .mobile-tool-icon {
  background: linear-gradient(135deg, #00c6fb, #005bea);
}

.mobile-tool-card:nth-child(4) .mobile-tool-icon {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
}

.mobile-tool-card:nth-child(5) .mobile-tool-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
}

.mobile-tool-card:nth-child(6) .mobile-tool-icon {
  background: linear-gradient(135deg, #f0d9b5, #b58863);
}

.mobile-tool-card:nth-child(7) .mobile-tool-icon {
  background: linear-gradient(135deg, #00c6fb, #005bea);
}

.mobile-tool-card:nth-child(8) .mobile-tool-icon {
  background: linear-gradient(135deg, #FF6B6B, #FF8E53);
}

.mobile-tool-card:nth-child(9) .mobile-tool-icon {
  background: linear-gradient(135deg, #4CAF50, #8BC34A);
}

.mobile-tool-card:nth-child(10) .mobile-tool-icon {
  background: linear-gradient(135deg, #9C27B0, #E91E63);
}

.mobile-tool-name {
  font-size: 13px;
  color: #333;
  text-align: center;
  font-weight: 500;
  line-height: 1.3;
}

/* 响应式布局 */
@media screen and (max-width: 1200px) {
  .tools-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media screen and (max-width: 992px) {
  .tools-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 768px) {
  .tools-page {
    min-height: 94vh;
    display: flex;
    flex-direction: column;
    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
    margin-left: -20px;
    margin-right: -20px;
    border-radius: 8px;
  }

  .tools-container {
    display: none;
  }

  .mobile-tools-grid {
    display: grid;
    flex: 1;
    width: 100%;
    margin: 0;
    padding: 20px;
    align-content: center;
  }

  .mobile-tool-card {
    padding: 10px;
    border-radius: 14px;
  }

  .mobile-tool-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
  }

  .mobile-tool-icon :deep(svg) {
    width: 22px;
    height: 22px;
  }

  .mobile-tool-name {
    font-size: 12px;
    margin-top: 2px;
  }
}

@media screen and (max-width: 360px) {
  .mobile-tools-grid {
    padding: 12px;
    gap: 12px;
  }

  .mobile-tool-card {
    padding: 8px;
    border-radius: 12px;
  }

  .mobile-tool-icon {
    width: 36px;
    height: 36px;
  }

  .mobile-tool-icon :deep(svg) {
    width: 20px;
    height: 20px;
  }

  .mobile-tool-name {
    font-size: 11px;
  }
}
</style>
