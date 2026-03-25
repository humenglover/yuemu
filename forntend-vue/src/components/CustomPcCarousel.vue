<template>
  <div
    class="luxury-carousel"
    @mouseenter="stopTimer"
    @mouseleave="startTimer"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- 沉浸式背景层 (重新引入并优化) -->
    <div class="carousel-bg-layer">
      <transition name="fade-bg">
        <div
          v-if="currentActivity"
          :key="currentActivity.id"
          class="bg-blur-item"
          :style="{ backgroundImage: `url(${currentActivity.coverUrl || defaultActivityImg})` }"
        ></div>
      </transition>
    </div>

    <div class="main-wrapper">
      <div class="slide-view">
        <!-- 核心显示队列 -->
        <div
          v-for="(activity, idx) in displayList"
          :key="activity.id"
          class="carousel-item"
          :class="{ 'is-loaded': imageStatus[activity.id] }"
          :style="{ backgroundImage: imageStatus[activity.id] ? `url(${activity.coverUrl || defaultActivityImg})` : 'none' }"
          @click="handleItemClick(idx)"
        >
          <!-- 预加载隐藏图：用于监听加载事件 -->
          <img :src="activity.coverUrl || defaultActivityImg"
               @load="handleImgLoad(activity.id)"
               class="hidden-preload-img" />

          <!-- 加载中占位骨架屏 -->
          <div v-if="!imageStatus[activity.id]" class="carousel-skeleton">
            <div class="shimmer"></div>
          </div>

          <div class="item-overlay"></div>

          <!-- 内容层：仅首位可见 -->
          <transition name="fade-content">
            <div class="item-content" v-if="idx === 0">
              <h1 class="content-title">{{ activity.title || '灵感发现' }}</h1>
              <p class="content-desc">
                探索数字影像的极致魅力，让每一像素都焕发生命力。
                <br/>悦木图库，为您记录最纯粹的视觉瞬间。
              </p>
              <div class="content-actions">
                <a-button type="primary" class="btn-prime" @click="$emit('activityClick', activity.id)">
                  立即开启 <i class="fas fa-external-link-alt"></i>
                </a-button>
                <div class="timer-badge">
                  <i class="far fa-clock"></i> {{ formatTime(activity.expireTime) }}截止
                </div>
              </div>
            </div>
          </transition>
        </div>
      </div>

      <!-- 高级导航控件 -->
      <div class="nav-controls" v-if="displayList.length > 1">
        <button class="nav-btn prev" @click="prev">
          <i class="fas fa-chevron-left"></i>
        </button>
        <button class="nav-btn next" @click="next">
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>

      <!-- 磁吸进度指示器 -->
      <div class="progress-bar-container">
        <div
          v-for="(_, index) in activities"
          :key="index"
          class="progress-segment"
          :class="{ active: currentRealIndex === index }"
          @click="jumpTo(index)"
        >
          <div class="segment-fill"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, reactive, type Ref } from 'vue'
import { formatTime } from '@/utils/time'
import defaultActivityImg from '@/assets/images/default_activity.png'

interface Activity {
  id: string
  coverUrl: string
  title: string
  isExpired: number
  expireTime: string
}

const props = defineProps<{ activities: Activity[] }>()
const emit = defineEmits<{ (e: 'activityClick', id: string): void }>()

const displayList: Ref<Activity[]> = ref([])
const timer = ref<any>(null)

// 核心：当前展示的项
const currentActivity = computed(() => displayList.value.length > 0 ? displayList.value[0] : null)

// 跟踪真实索引，用于指示器显示
const currentRealIndex = computed(() => {
  if (!props.activities.length || !currentActivity.value) return 0
  const firstId = currentActivity.value.id
  return props.activities.findIndex(a => a.id === firstId)
})

const initDisplayList = () => {
  if (props.activities.length > 0) {
    displayList.value = [...props.activities]
  }
}

const next = () => {
  if (displayList.value.length < 2) return
  const first = displayList.value.shift()
  if (first) displayList.value.push(first)
}

const prev = () => {
  if (displayList.value.length < 2) return
  const last = displayList.value.pop()
  if (last) displayList.value.unshift(last)
}

const jumpTo = (targetRealIndex: number) => {
  // 简单的重新排列 displayList，让 targetRealIndex 项在第一位
  const targetId = props.activities[targetRealIndex].id
  const idxInDisplay = displayList.value.findIndex(a => a.id === targetId)
  if (idxInDisplay > 0) {
    const moved = displayList.value.splice(0, idxInDisplay)
    displayList.value.push(...moved)
  }
}

// 记录已加载完成的图片 ID 状态
const imageStatus = reactive<Record<string, boolean>>({})
const handleImgLoad = (id: string) => {
  imageStatus[id] = true
}

// 处理卡片点击逻辑
const handleItemClick = (idx: number) => {
  if (idx === 0) return // 点击大图不执行切换逻辑，保持原有 emit
  // 点击右侧小图，将其切换至首位
  const moved = displayList.value.splice(0, idx)
  displayList.value.push(...moved)
}

const startTimer = () => {
  if (props.activities.length <= 1) return
  stopTimer()
  timer.value = setInterval(next, 6000)
}

const stopTimer = () => {
  if (timer.value) clearInterval(timer.value)
}

// 触摸手势处理
const touchStartX = ref(0)
const touchEndX = ref(0)

const handleTouchStart = (e: TouchEvent) => {
  touchStartX.value = e.touches[0].clientX
}

const handleTouchEnd = (e: TouchEvent) => {
  touchEndX.value = e.changedTouches[0].clientX
  const swipeDistance = touchStartX.value - touchEndX.value

  if (Math.abs(swipeDistance) > 50) {
    if (swipeDistance > 0) {
      next() // 左滑 -> 下一张
    } else {
      prev() // 右滑 -> 上一张
    }
  }
}

onMounted(() => {
  initDisplayList()
  startTimer()
})

onUnmounted(stopTimer)

watch(() => props.activities, (newVal) => {
  if (newVal.length > 0) {
    initDisplayList()
    startTimer()
  }
}, { deep: true })

</script>

<style scoped>
.luxury-carousel {
  position: relative;
  width: 100%;
  height: 420px; /* 恢复原高度 */
  background: #f1f5f9; /* 恢复浅色底，防止黑色间隙感 */
  overflow: hidden;
  border-radius: 32px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.05);
  border: 1px solid #fff;
  perspective: 1200px;
  touch-action: pan-y; /* 允许垂直滚动，拦截水平手势用于轮播 */
}

/* 背景虚化层 */
.carousel-bg-layer {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden; /* 修正：确保模糊背景不溢出组件边界 */
}
.bg-blur-item {
  position: absolute;
  inset: -20px;
  background-size: cover;
  background-position: center;
  filter: blur(50px) brightness(0.4) saturate(1.4);
  transform: scale(1.1);
}
.fade-bg-enter-active, .fade-bg-leave-active { transition: opacity 0.8s ease; }
.fade-bg-enter-from, .fade-bg-leave-to { opacity: 0; }

.main-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.carousel-item {
  width: 200px; /* 减小宽度 */
  height: 280px; /* 减小高度 */
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  border-radius: 20px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  transition: all 0.8s cubic-bezier(0.23, 1, 0.32, 1), opacity 0.6s ease;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
  border: 1.5px solid rgba(255, 255, 255, 0.3);
  z-index: 1;
  cursor: pointer;
  opacity: 0;
}

.carousel-item.is-loaded {
  opacity: 1;
}

.hidden-preload-img {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.carousel-skeleton {
  position: absolute;
  inset: 0;
  background: #e2e8f0;
  border-radius: inherit;
  overflow: hidden;
  z-index: -1;
}

.shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0) 0%,
    rgba(255, 255, 255, 0.3) 50%,
    rgba(255, 255, 255, 0) 100%
  );
  animation: luxShimmer 1.8s infinite;
}

@keyframes luxShimmer {
  from { transform: translateX(-100%); }
  to { transform: translateX(100%); }
}

.carousel-item:hover {
  filter: brightness(1.1) contrast(1.1);
  border-color: rgba(255, 255, 255, 0.8);
}

.item-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(0,0,0,0.1) 0%, transparent 60%);
  border-radius: 18px;
}

/* 状态 1：当前聚合背景 (全屏展开) */
.carousel-item:nth-child(1) {
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  border-radius: 0;
  transform: translate(0, 0);
  box-shadow: none;
  border: none;
  filter: none;
}

.carousel-item:nth-child(1) .item-overlay {
  background: linear-gradient(90deg, rgba(0,0,0,0.4) 0%, rgba(0,0,0,0.05) 50%, rgba(255,255,255,0.1) 100%);
  border-radius: 0;
}

/* 状态 2, 3, 4：右侧弹性堆叠卡片 (优化间距) */
.carousel-item:nth-child(2) {
  left: 72%; /* 向右移动，之前是65% */
  z-index: 10;
}

.carousel-item:nth-child(3) {
  left: calc(72% + 140px);
  transform: translateY(-50%) scale(0.9);
  opacity: 0.8;
  z-index: 9;
}

.carousel-item:nth-child(4) {
  left: calc(72% + 260px);
  transform: translateY(-50%) scale(0.8);
  opacity: 0.5;
  z-index: 8;
}

/* 后续隐藏项 */
.carousel-item:nth-child(n + 5) {
  left: calc(72% + 380px);
  opacity: 0;
  z-index: 5;
}

/* 文字内容区域排版优化 */
.item-content {
  position: absolute;
  top: 50%;
  left: 80px;
  width: 520px; /* 稍微拓宽 */
  transform: translateY(-50%);
  color: #fff;
  z-index: 20;
}

.content-tag {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.4); /* 背景调暗 */
  backdrop-filter: blur(10px);
  padding: 6px 14px;
  border-radius: 20px;
  width: fit-content;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.content-tag .dot {
  width: 6px;
  height: 6px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 10px #10b981;
}

.content-title {
  font-size: 56px;
  font-weight: 900;
  line-height: 1.1;
  margin-bottom: 24px;
  color: #fff;
  text-shadow: 0 4px 30px rgba(0, 0, 0, 0.8), 0 0 10px rgba(0,0,0,0.3);
  letter-spacing: -1px;
}


.content-desc {
  font-size: 17px; /* 字体增大一点 */
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.95);
  margin-bottom: 40px;
  text-shadow: 0 2px 15px rgba(0, 0, 0, 0.9); /* 强化投影 */
  max-width: 420px;
}

.content-actions {
  display: flex;
  align-items: center;
  gap: 24px;
}

.btn-prime {
  height: 56px;
  padding: 0 36px;
  background: #3b82f6; /* 改为主题蓝 */
  color: #fff !important; /* 文字改为白色 */
  border: none;
  font-weight: 700;
  font-size: 16px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 10px 25px rgba(59, 130, 246, 0.3);
}

.btn-prime:hover {
  transform: scale(1.05) translateY(-2px);
  box-shadow: 0 15px 30px rgba(59, 130, 246, 0.4);
  background: #2563eb;
}

.timer-badge {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 导航按钮：极简工业风 */
.nav-controls {
  position: absolute;
  bottom: 40px;
  right: 80px; /* 移至右侧，避免遮挡左侧的操作按钮 */
  display: flex;
  gap: 12px;
  z-index: 30;
}

.nav-btn {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(15, 23, 42, 0.75); /* 改为深色背景 */
  backdrop-filter: blur(12px);
  color: #fff;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.25);
}

.nav-btn:hover {
  background: #fff;
  color: #0f172a;
  border-color: #fff;
  transform: translateY(-2px);
}

/* 进度指示器：磁吸段落感 */
.progress-bar-container {
  position: absolute;
  bottom: 58px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 8px;
  z-index: 40;
}

.progress-segment {
  width: 40px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  cursor: pointer;
  overflow: hidden;
  transition: width 0.4s ease;
}

.progress-segment.active {
  width: 60px;
  background: rgba(255, 255, 255, 0.3);
}

.progress-segment.active .segment-fill {
  width: 100%;
  height: 100%;
  background: #fff;
  animation: progressFill 6s linear forwards;
}

@keyframes progressFill {
  from { width: 0; }
  to { width: 100%; }
}

/* 内容进入动画 (移除位移) */
.fade-content-enter-active {
  transition: opacity 0.6s ease 0.2s;
}
.fade-content-leave-active {
  transition: opacity 0.3s ease;
}
.fade-content-enter-from, .fade-content-leave-to {
  opacity: 0;
}

@media screen and (max-width: 1200px) {
  .item-content { left: 40px; width: 400px; }
  .nav-controls { left: 40px; }
  .content-title { font-size: 42px; }
}

/* 移动端深度优化 (≤ 768px) */
@media screen and (max-width: 768px) {
  .luxury-carousel {
    height: 300px;
    border-radius: 16px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  }

  .carousel-bg-layer {
    display: none; /* 移动端隐藏背景虚化，保持清爽 */
  }

  /* 核心：隐藏移动端的右侧卡片堆叠，只展示当前项 */
  .carousel-item:nth-child(n + 2) {
    display: none;
  }

  .carousel-item:nth-child(1) {
    border-radius: 16px; /* 匹配容器 */
  }

  .carousel-item:nth-child(1) .item-overlay {
    background: linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.4) 40%, transparent 100%);
  }

  /* 移动端文字内容优化 */
  .item-content {
    left: 20px;
    bottom: 40px; /* 底部对齐 */
    top: auto;
    width: calc(100% - 40px);
    transform: none;
  }

  .content-title {
    font-size: 26px;
    margin-bottom: 12px;
    text-shadow: 0 2px 10px rgba(0, 0, 0, 0.8);
  }

  .content-desc {
    font-size: 13px;
    line-height: 1.6;
    margin-bottom: 16px;
    display: -webkit-box;
    -webkit-line-clamp: 2; /* 最多显示2行 */
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: rgba(255, 255, 255, 0.9);
  }

  .content-actions {
    gap: 12px;
  }

  .btn-prime {
    height: 40px;
    padding: 0 20px;
    font-size: 14px;
    border-radius: 20px;
  }

  .timer-badge {
    font-size: 12px;
  }

  /* 控件微调 */
  .nav-controls {
    display: none; /* 移动端靠进度条或自动轮播 */
  }

  .progress-bar-container {
    bottom: 20px;
  }

  .progress-segment {
    width: 24px;
  }

  .progress-segment.active {
    width: 36px;
  }
}
</style>
