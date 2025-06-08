<template>
  <div class="barrage-container">
    <!-- 弹幕显示区域 -->
    <div class="barrage-stage" ref="barrageStage">
      <div
        v-for="barrage in activeBarrages"
        :key="barrage.key"
        class="barrage-item"
        :class="{ 'active': barrage.active }"
        :style="{
          top: `${barrage.lane * 50}px`,
          '--duration': `${barrageSpeed}ms`,
          background: barrage.color
        }"
      >
        <img :src="barrage.avatar" class="barrage-avatar" alt="avatar" />
        <span class="barrage-text">{{ barrage.content }}</span>
      </div>
    </div>

    <!-- 弹幕输入区域 -->
    <div class="message-in" :class="{ 'show': showInput }">
      <div class="input-wrapper">
        <div class="input-container">
          <div class="input-box">
            <a-input
              v-model:value="inputContent"
              placeholder="说点什么吧..."
              :maxLength="60"
              class="message-input"
              @pressEnter="sendBarrage"
              bordered="false"
            >
              <template #prefix>
                <img :src="getDefaultAvatar('Guest')" class="input-avatar" alt="avatar" />
              </template>
              <template #suffix>
                <span class="word-count">{{ inputContent.length }}/60</span>
              </template>
            </a-input>
            <a-button
              type="primary"
              :disabled="!inputContent.trim()"
              @click="sendBarrage"
              class="send-button"
            >
              发射
            </a-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { sendMessageUsingPost, getTop500UsingPost, listMessageByPageUsingPost } from '@/api/messageController'
import { getDefaultAvatar } from '@/utils/userUtils'

const props = defineProps<{
  speed?: number,
  showInput?: boolean
}>()

const emit = defineEmits(['send'])

// 状态
const inputContent = ref('')
const showInput = ref(props.showInput ?? true)
const activeBarrages = ref<any[]>([])
const barrageSpeed = ref(props.speed || 12000)
const maxBarragesCount = 15 // 固定最大弹幕数量
const barrageStage = ref<HTMLElement | null>(null)
const lanes = ref<number[]>([])
const currentPage = ref(1)
const pageSize = ref(50)
const loading = ref(false)
const hasMore = ref(true)
const queryTimer = ref<any>(null)
const messageCache = ref<any[]>([])
const isPageVisible = ref(true)

// 计算查询间隔
const queryInterval = computed(() => {
  // 根据弹幕速度计算查询间隔
  const baseInterval = barrageSpeed.value / 4
  return Math.min(Math.max(baseInterval, 1000), 3000)
})

// 弹幕颜色配置
const barrageColors = [
  'linear-gradient(45deg, #FFB6C1, #FF69B4)', // 粉色系
  'linear-gradient(45deg, #87CEEB, #1E90FF)', // 蓝色系
  'linear-gradient(45deg, #98FB98, #32CD32)', // 绿色系
  'linear-gradient(45deg, #DDA0DD, #9370DB)', // 紫色系
  'linear-gradient(45deg, #F0E68C, #FFD700)', // 金色系
  'linear-gradient(45deg, #FFA07A, #FF6347)', // 橙色系
  'linear-gradient(45deg, #E6E6FA, #9370DB)', // 淡紫色系
  'linear-gradient(45deg, #98FB98, #3CB371)', // 翠绿色系
]

// 获取随机颜色
const getRandomColor = () => {
  return barrageColors[Math.floor(Math.random() * barrageColors.length)]
}

// 初始化轨道
const initLanes = () => {
  const isMobile = window.innerWidth <= 768
  const laneHeight = isMobile ? 40 : 50
  const stageHeight = barrageStage.value?.clientHeight || 0

  // 移动端需要考虑输入框的高度
  const inputBoxHeight = isMobile ? 120 : 0
  const availableHeight = stageHeight - inputBoxHeight
  const laneCount = Math.floor(availableHeight / laneHeight)

  lanes.value = new Array(laneCount).fill(0)
  console.log('初始化轨道数:', laneCount)
}

// 获取可用轨道
const getAvailableLane = () => {
  const now = Date.now()
  const isMobile = window.innerWidth <= 768

  // 移动端使用更大的轨道间距以减少渲染压力
  const laneHeight = isMobile ? 40 : 50
  const stageHeight = barrageStage.value?.clientHeight || 0

  // 移动端需要考虑输入框的高度
  const inputBoxHeight = isMobile ? 120 : 0
  const availableHeight = stageHeight - inputBoxHeight
  const laneCount = Math.floor(availableHeight / laneHeight)

  // 如果轨道数量发生变化，重新初始化轨道
  if (lanes.value.length !== laneCount) {
    lanes.value = new Array(laneCount).fill(0)
  }

  // 随机选择一个轨道，优化分布
  const availableLanes = lanes.value
    .map((time, index) => ({ time, index }))
    .filter(lane => lane.time <= now)

  if (availableLanes.length > 0) {
    const randomIndex = Math.floor(Math.random() * availableLanes.length)
    return availableLanes[randomIndex].index
  }

  // 如果没有空闲轨道，找到最早空闲的轨道
  let earliestLane = 0
  let earliestTime = Infinity

  lanes.value.forEach((time, index) => {
    if (time < earliestTime) {
      earliestTime = time
      earliestLane = index
    }
  })

  return earliestLane
}

// 监听props变化
watch(() => props.speed, (newValue) => {
  if (typeof newValue === 'number') {
    barrageSpeed.value = newValue
  }
}, { immediate: true })

watch(() => props.showInput, (newValue) => {
  if (typeof newValue === 'boolean') {
    showInput.value = newValue
  }
}, { immediate: true })

// 监听页面可见性变化
const handleVisibilityChange = () => {
  isPageVisible.value = document.visibilityState === 'visible'
  if (isPageVisible.value) {
    // 页面变为可见时，重新启动查询
    startQueryTimer()
  } else {
    // 页面不可见时，暂停查询
    stopQueryTimer()
  }
}

// 获取历史弹幕
const fetchHistoryBarrages = async () => {
  // 如果页面不可见或正在加载，则跳过
  if (!isPageVisible.value || loading.value) return

  try {
    loading.value = true

    // 如果缓存中还有数据，直接使用缓存的数据
    if (messageCache.value.length > 0) {
      // 每次最多展示2条，避免突然加载过多
      const batchSize = Math.min(2, messageCache.value.length)
      const batch = messageCache.value.splice(0, batchSize)

      // 为每条弹幕添加随机延迟，避免同时出现
      batch.forEach((msg, index) => {
        setTimeout(() => {
          // 只在未达到最大数量时添加新弹幕
          if (activeBarrages.value.length < maxBarragesCount) {
            addBarrage({
              id: msg.id,
              content: msg.content
            })
          } else {
            // 如果已达到最大数量，将消息放回缓存开头
            messageCache.value.unshift(msg)
          }
        }, index * 800)
      })

      // 如果缓存快用完了，提前获取下一页
      if (messageCache.value.length < 5) {
        await fetchNextPage()
      }
    } else {
      // 缓存为空时，获取新的一页数据
      await fetchNextPage()
    }
  } catch (error) {
    console.error('获取历史弹幕失败:', error)
  } finally {
    loading.value = false
  }
}

// 添加弹幕
const addBarrage = (barrage: any) => {
  const isMobile = window.innerWidth <= 768

  // 移动端减少最大弹幕数量以降低渲染压力
  const currentMaxBarrages = isMobile ? 10 : maxBarragesCount

  // 检查是否超过最大弹幕数量
  if (activeBarrages.value.length >= currentMaxBarrages) {
    // 如果超过，标记最早的弹幕为即将移除
    const oldestBarrage = activeBarrages.value[0]
    oldestBarrage.active = false

    // 延迟移除，等待淡出动画完成
    setTimeout(() => {
      const index = activeBarrages.value.findIndex(b => b.key === oldestBarrage.key)
      if (index !== -1) {
        activeBarrages.value.splice(index, 1)
      }
    }, 300)
  }

  const lane = getAvailableLane()
  const avatar = getDefaultAvatar(barrage.content.substring(0, 2))
  const color = getRandomColor()

  // 更新轨道占用时间
  lanes.value[lane] = Date.now() + barrageSpeed.value

  // 添加弹幕到显示列表
  const newBarrage = {
    ...barrage,
    key: Date.now() + Math.random(),
    lane,
    duration: barrageSpeed.value,
    avatar,
    color,
    active: false
  }

  // 使用 requestAnimationFrame 来确保动画平滑
  requestAnimationFrame(() => {
    activeBarrages.value.push(newBarrage)

    // 确保DOM更新后再激活动画
    requestAnimationFrame(() => {
      setTimeout(() => {
        const index = activeBarrages.value.findIndex(b => b.key === newBarrage.key)
        if (index !== -1) {
          activeBarrages.value[index].active = true
        }
      }, 50)
    })
  })

  // 设置弹幕自动移除
  setTimeout(() => {
    const index = activeBarrages.value.findIndex(b => b.key === newBarrage.key)
    if (index !== -1) {
      // 先将弹幕标记为非活动状态，触发淡出动画
      activeBarrages.value[index].active = false

      // 等待淡出动画完成后移除
      setTimeout(() => {
        const removeIndex = activeBarrages.value.findIndex(b => b.key === newBarrage.key)
        if (removeIndex !== -1) {
          activeBarrages.value.splice(removeIndex, 1)
        }
      }, 300)
    }
  }, barrageSpeed.value - 300) // 提前开始淡出动画
}

// 发送弹幕
const sendBarrage = async () => {
  if (!inputContent.value.trim()) return

  try {
    const res = await sendMessageUsingPost({
      content: inputContent.value
    })

    if (res.data.code === 0) {
      addBarrage({
        id: Date.now(),
        content: inputContent.value
      })
      inputContent.value = ''
    } else {
      message.error('发送失败')
    }
  } catch (error) {
    message.error('发送失败')
  }
}

// 获取下一页数据
const fetchNextPage = async () => {
  try {
    const res = await listMessageByPageUsingPost({
      current: currentPage.value,
      pageSize: pageSize.value,
      sortField: 'createTime',
      sortOrder: 'descend'
    })

    if (res.data.data?.records) {
      const messages = res.data.data.records
      console.log('获取到历史弹幕:', messages.length, '条')

      // 更新是否还有更多数据
      hasMore.value = messages.length === pageSize.value

      // 将新消息添加到缓存
      messageCache.value.push(...messages)

      // 更新页码
      if (hasMore.value) {
        currentPage.value++
      } else {
        // 如果没有更多数据，重置到第一页继续循环
        currentPage.value = 1
      }
    }
  } catch (error) {
    console.error('获取下一页弹幕失败:', error)
  }
}

// 启动定时查询
const startQueryTimer = () => {
  stopQueryTimer()
  // 确保立即执行一次
  fetchHistoryBarrages()
  // 然后开始定时查询
  queryTimer.value = setInterval(fetchHistoryBarrages, queryInterval.value)
}

// 停止定时查询
const stopQueryTimer = () => {
  if (queryTimer.value) {
    clearInterval(queryTimer.value)
    queryTimer.value = null
  }
}

// 组件挂载时启动定时查询
onMounted(() => {
  initLanes()
  window.addEventListener('resize', initLanes)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  startQueryTimer()
})

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', initLanes)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  stopQueryTimer()
})

// 暴露方法给父组件
defineExpose({
  addBarrage,
  updateSpeed: (speed: number) => barrageSpeed.value = speed,
  updateShowInput: (show: boolean) => showInput.value = show
})
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

.barrage-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
  user-select: none;
  animation: hideToShow 2s;
  padding: 60px 0 160px;
  font-family: 'Noto Sans SC', sans-serif;
}

.barrage-stage {
  position: absolute;
  top: 60px;
  left: 0;
  right: 0;
  bottom: 160px;
  pointer-events: none;
}

.barrage-item {
  position: absolute;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: var(--gradientBG);
  background-size: 200% 200%;
  border-radius: 24px;
  color: var(--white);
  font-size: 16px;
  white-space: nowrap;
  will-change: transform;
  transform: translateX(calc(100vw + 100%));
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  gap: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  letter-spacing: 0.5px;
  font-weight: 500;
  transition: transform var(--duration) linear;
  pointer-events: none;
  opacity: 0;
}

.barrage-item.active {
  opacity: 1;
  transform: translateX(-100%);
}

.barrage-enter-active {
  opacity: 1;
  transition: transform var(--duration) linear, opacity 0.3s ease;
}

.barrage-enter-from {
  opacity: 0;
  transform: translateX(calc(100vw + 100%));
}

.barrage-enter-to {
  opacity: 1;
  transform: translateX(-100%);
}

.barrage-leave-active {
  position: absolute;
  transition: opacity 0.3s ease;
}

.barrage-leave-to {
  opacity: 0;
}

.barrage-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.barrage-text {
  line-height: 1.4;
}

.message-in {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  color: var(--white);
  animation: hideToShow 2.5s;
  width: 600px;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.message-in.show {
  opacity: 1;
}

.input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.input-container {
  /* 降低背景不透明度 */
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(20px);
  border-radius: 30px;
  padding: 6px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

.input-container:hover {
  /* 悬停时稍微增加不透明度 */
  background: rgba(0, 0, 0, 0.3);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.2);
  transform: translateY(-1px);
}

.input-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 6px;
}

.input-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  margin-right: 4px;
}

.message-input {
  flex: 1;
}

.message-input :deep(.ant-input) {
  height: 44px;
  background: transparent;
  border: none;
  padding: 0 16px;
  color: var(--white);
  font-size: 16px;
  transition: all 0.3s ease;
}

.message-input :deep(.ant-input-affix-wrapper) {
  background: transparent;
  border: none;
  padding: 0;
}

.message-input :deep(.ant-input-affix-wrapper:hover),
.message-input :deep(.ant-input-affix-wrapper:focus) {
  background: transparent;
  border: none;
  box-shadow: none;
}

.message-input :deep(.ant-input::placeholder) {
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
}

.word-count {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  margin: 0 12px;
  user-select: none;
}

.send-button {
  height: 44px;
  padding: 0 24px;
  border-radius: 22px;
  background: var(--gradientBG);
  background-size: 200% 200%;
  animation: gradientBG 10s ease infinite;
  border: none;
  color: white;
  font-size: 16px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  letter-spacing: 2px;
  white-space: nowrap;
}

.send-button:not(:disabled):hover {
  transform: translateY(-1px) scale(1.02);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
  background-position: 100% 50%;
}

.send-button:disabled {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
}

@keyframes hideToShow {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

@keyframes gradientBG {
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

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .barrage-container {
    padding: 60px 0 120px; /* 减小底部padding，为输入框留出空间 */
  }

  .barrage-stage {
    position: absolute;
    top: 60px;
    left: 0;
    right: 0;
    bottom: 120px; /* 确保弹幕区域不会与底部输入框重叠 */
    pointer-events: none;
  }

  .barrage-item {
    /* 优化移动端样式 */
    padding: 8px 16px;
    font-size: 15px;
    line-height: 1.4;
    background: rgba(0, 0, 0, 0.6) !important; /* 半透明黑色背景 */
    color: rgba(255, 255, 255, 0.95); /* 略微柔和的白色文字 */
    backdrop-filter: none;
    border: 1px solid rgba(255, 255, 255, 0.1); /* 微弱的边框 */
    border-radius: 20px; /* 更圆润的边角 */
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); /* 轻微的阴影 */
    text-shadow: none;
    min-height: 36px; /* 确保最小高度 */
    display: flex;
    align-items: center;
    gap: 10px;

    /* 优化动画性能和衔接 */
    transform: translate3d(100vw, 0, 0);
    backface-visibility: hidden;
    perspective: 1000;
    -webkit-backface-visibility: hidden;
    -webkit-perspective: 1000;
    will-change: transform;

    /* 使用硬件加速 */
    -webkit-transform-style: preserve-3d;
    transform-style: preserve-3d;

    /* 确保动画平滑过渡 */
    transition: transform var(--duration) linear, opacity 0.3s ease;
    opacity: 0;
  }

  .barrage-item.active {
    transform: translate3d(-100%, 0, 0);
    opacity: 1;
  }

  .barrage-avatar {
    width: 28px; /* 稍微增大头像尺寸 */
    height: 28px;
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  }

  .barrage-text {
    font-weight: normal;
    color: rgba(255, 255, 255, 0.95);
    padding: 2px 0;
    letter-spacing: 0.3px;
  }

  /* 优化动画过渡 */
  .barrage-enter-active {
    transition: transform var(--duration) linear, opacity 0.3s ease;
  }

  .barrage-leave-active {
    transition: transform var(--duration) linear, opacity 0.3s ease;
    position: absolute;
  }

  .barrage-enter-from {
    transform: translate3d(100vw, 0, 0);
    opacity: 0;
  }

  .barrage-enter-to {
    transform: translate3d(-100%, 0, 0);
    opacity: 1;
  }

  .barrage-leave-to {
    opacity: 0;
  }

  .message-in {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    top: auto;
    transform: none;
    width: 100%;
    padding: 12px;
    margin-bottom: env(safe-area-inset-bottom);
    z-index: 99;
    backdrop-filter: blur(10px);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
  }

  .input-container {
    background: rgba(255, 255, 255, 0.9); /* 修改为浅色背景 */
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 16px;
    padding: 6px;
  }

  .input-box {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 4px;
  }

  .input-avatar {
    width: 32px;
    height: 32px;
    border: 1px solid rgba(255, 255, 255, 0.2);
  }

  .message-input :deep(.ant-input) {
    height: 40px;
    font-size: 14px;
    padding: 0 12px;
    background: transparent;
    color: #333; /* 修改文字颜色为深色 */
  }

  .message-input :deep(.ant-input::placeholder) {
    color: rgba(0, 0, 0, 0.45); /* 修改占位符颜色为深色 */
  }

  .word-count {
    color: rgba(0, 0, 0, 0.45); /* 修改字数统计颜色为深色 */
  }

  .send-button {
    height: 40px;
    padding: 0 16px;
    font-size: 14px;
    min-width: 70px;
    background: rgba(0, 0, 0, 0.6);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.9);
  }

  .send-button:hover {
    background: rgba(0, 0, 0, 0.8);
    border-color: rgba(255, 255, 255, 0.2);
  }

  .send-button:disabled {
    background: rgba(0, 0, 0, 0.3);
    border-color: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.3);
  }
}

/* 当控制面板显示时，调整输入框位置 */
.control-panel.show ~ .message-in {
  transform: translateY(-100%);
  opacity: 0;
  pointer-events: none;
}
</style>
