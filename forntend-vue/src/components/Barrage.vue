<template>
  <div class="barrage-container">
    <!-- 弹幕全屏显示区域 -->
    <div class="barrage-stage" ref="barrageStage">
      <div
        v-for="barrage in activeBarrages"
        :key="barrage.key"
        class="barrage-item"
        :class="{ 'active': barrage.active, 'paused': barrage.paused, 'current-user': barrage.isCurrentUser }"
        :style="{
          top: `${barrage.top}px`,
          transform: `translateX(${barrage.currentX}px)`,
          '--duration': `${barrageSpeed}ms`
        }"
        @click="handleBarrageHover(barrage)"
        @mouseenter="handleBarrageHover(barrage)"
        @mouseleave="handleBarrageLeave(barrage)"
        @touchstart="handleBarrageHover(barrage)"
      >
        <img :src="barrage.avatar" class="barrage-avatar" alt="avatar" />
        <span class="barrage-text">{{ barrage.content }}</span>
      </div>
    </div>

    <!-- 输入框：屏幕中间位置（弹幕可从下方滚动穿过） -->
    <div class="message-in" :class="{ 'show': showInput }">
      <div class="input-mask"> <!-- 遮罩层防止弹幕穿透交互 + 呼吸边框容器 -->
        <div class="input-box">
          <input
            v-model="inputContent"
            placeholder="写下你的想法..."
            maxlength="60"
            class="message-input"
            @keyup.enter="sendBarrage"
          />
          <button
            @click="sendBarrage"
            :disabled="!inputContent.trim()"
            class="send-btn"
            :class="{ disabled: !inputContent.trim() }"
          >
            发送
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { sendMessageUsingPost, listMessageByPageUsingPost } from '@/api/messageController'
import { getDefaultAvatar } from '@/utils/userUtils'

const props = defineProps<{
  speed?: number,
  showInput?: boolean
}>()

const emit = defineEmits(['send'])

// 核心状态
const inputContent = ref('')
const showInput = ref(props.showInput ?? true)
const activeBarrages = ref<any[]>([])
const barrageSpeed = ref(props.speed || 8000)
const maxBarragesCount = 100 // 增加弹幕数量，提升全屏密度
const barrageStage = ref<HTMLElement | null>(null)
const currentPage = ref(1)
const pageSize = ref(100)
const loading = ref(false)
const hasMore = ref(true)
const queryTimer = ref<any>(null)
const messageCache = ref<any[]>([])
const isPageVisible = ref(true)

// 暂停状态 - 现在改为单个弹幕的暂停状态
const pauseDuration = 3000 // 3秒后自动恢复

// 动画控制相关
let animationFrameId: number | null = null

// 视窗尺寸（实时更新）
const viewportHeight = ref(window.innerHeight)
const viewportWidth = ref(window.innerWidth)

// 计算查询间隔（更频繁生成弹幕）
const queryInterval = computed(() => {
  const baseInterval = barrageSpeed.value / 10
  return Math.min(Math.max(baseInterval, 500), 1200)
})

// 获取随机垂直位置（全屏范围，允许覆盖输入框下方）
const getRandomTopPosition = () => {
  // 弹幕垂直范围：20px ~ 视窗高度-60px（仅留底部少量余量）
  const top = 20 + Math.random() * (viewportHeight.value - 80)
  return top
}

// 监听窗口大小变化
const handleResize = () => {
  viewportHeight.value = window.innerHeight
  viewportWidth.value = window.innerWidth
}

// 监听页面可见性（切后台暂停弹幕加载）
const handleVisibilityChange = () => {
  isPageVisible.value = document.visibilityState === 'visible'
  if (isPageVisible.value) {
    startQueryTimer()
  } else {
    stopQueryTimer()
  }
}

// 获取历史弹幕
const fetchHistoryBarrages = async () => {
  if (!isPageVisible.value || loading.value) return

  try {
    loading.value = true

    if (messageCache.value.length > 0) {
      // 每次加载12条，缩短间隔提升密度
      const batchSize = Math.min(12, messageCache.value.length)
      const batch = messageCache.value.splice(0, batchSize)

      batch.forEach((msg, index) => {
        setTimeout(() => {
          if (activeBarrages.value.length < maxBarragesCount) {
            addBarrage({
              id: msg.id,
              content: msg.content
            })
          } else {
            messageCache.value.unshift(msg)
          }
        }, index * 250)
      })

      // 缓存不足时提前加载下一页
      if (messageCache.value.length < 20) {
        await fetchNextPage()
      }
    } else {
      await fetchNextPage()
    }
  } catch (error) {
    console.error('获取历史弹幕失败:', error)
  } finally {
    loading.value = false
  }
}

// 添加弹幕（核心逻辑：全屏随机位置，允许穿过输入框下方）
const addBarrage = (barrage: any) => {
  // 超出最大数量时，直接移除最早的弹幕（无消失动画）
  if (activeBarrages.value.length >= maxBarragesCount) {
    activeBarrages.value.shift() // 直接删除第一条，无动画
  }

  const top = getRandomTopPosition()
  const avatar = getDefaultAvatar(barrage.content.substring(0, 2))

  // 构建弹幕项（移除随机颜色，统一用样式里的蓝色）
  const newBarrage = {
    ...barrage,
    key: Date.now() + Math.random(),
    top,
    duration: barrageSpeed.value,
    avatar,
    active: false,
    paused: false,
    isCurrentUser: !!barrage.isCurrentUser, // 标识是否为当前用户发送的消息
    startX: viewportWidth.value + 100, // 初始X位置（屏幕右侧外）
    currentX: viewportWidth.value + 100, // 当前X位置
    targetX: -200, // 目标X位置（屏幕左侧外）
    speed: (viewportWidth.value + 200) / (barrageSpeed.value / 1000) // 像素/秒
  }

  // 平滑添加弹幕（避免卡顿）
  requestAnimationFrame(() => {
    activeBarrages.value.push(newBarrage)
    requestAnimationFrame(() => {
      setTimeout(() => {
        const index = activeBarrages.value.findIndex(b => b.key === newBarrage.key)
        if (index !== -1) {
          // 如果当前处于暂停状态，新弹幕也要添加暂停类
          activeBarrages.value[index].active = true
        }
      }, 50)
    })
  })

  // 注意：现在使用JavaScript动画控制，不需要定时移除弹幕
  // 原来的自动移除逻辑已被JavaScript动画循环替代
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
        content: inputContent.value,
        isCurrentUser: true
      })
      inputContent.value = ''
      message.success('发送成功')
    } else {
      message.error('发送失败：' + (res.data.msg || '服务器错误'))
    }
  } catch (error) {
    console.error('发送弹幕失败:', error)
    message.error('发送失败：网络异常')
  }
}

// 获取下一页弹幕数据
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
      hasMore.value = messages.length === pageSize.value
      messageCache.value.push(...messages)

      // 无更多数据时重置页码，循环加载
      if (hasMore.value) {
        currentPage.value++
      } else {
        currentPage.value = 1
      }
    }
  } catch (error) {
    console.error('获取下一页弹幕失败:', error)
  }
}

// 启动定时查询弹幕
const startQueryTimer = () => {
  stopQueryTimer()
  fetchHistoryBarrages() // 立即执行一次
  queryTimer.value = setInterval(fetchHistoryBarrages, queryInterval.value)
}

// 停止定时查询
const stopQueryTimer = () => {
  if (queryTimer.value) {
    clearInterval(queryTimer.value)
    queryTimer.value = null
  }
}

// 处理弹幕点击/悬停事件 - 暂停弹幕
const handleBarrageHover = (barrage: any) => {
  // 暂停弹幕
  barrage.paused = true;

  // 设置定时器，3秒后恢复滚动
  setTimeout(() => {
    // 检查弹幕是否仍然存在，然后恢复滚动
    const index = activeBarrages.value.findIndex(b => b.key === barrage.key);
    if (index !== -1 && activeBarrages.value[index].paused) {
      activeBarrages.value[index].paused = false;
    }
  }, pauseDuration);
}

// 处理弹幕离开事件 - 恢复滚动
const handleBarrageLeave = (barrage: any) => {
  // 恢复弹幕滚动
  setTimeout(() => {
    // 检查弹幕是否仍然存在，然后恢复滚动
    const index = activeBarrages.value.findIndex(b => b.key === barrage.key);
    if (index !== -1 && activeBarrages.value[index].paused) {
      activeBarrages.value[index].paused = false;
    }
  }, pauseDuration);
}

// 记录上一次更新的时间
let lastUpdateTime = Date.now();

// 更新弹幕位置
const updateBarragePositions = () => {
  const now = Date.now();
  const deltaTime = (now - lastUpdateTime) / 1000; // 转换为秒
  lastUpdateTime = now;

  activeBarrages.value.forEach(barrage => {
    if (barrage.active && !barrage.paused) {
      // 根据速度和时间增量更新位置
      barrage.currentX -= barrage.speed * deltaTime;
    }

    // 检查弹幕是否已经移动到屏幕左侧外，从数组中移除（无论是否暂停）
    if (barrage.active && barrage.currentX <= -200) {
      const index = activeBarrages.value.findIndex(b => b.key === barrage.key);
      if (index !== -1) {
        activeBarrages.value.splice(index, 1);
      }
    }
  });

  // 继续动画循环
  animationFrameId = requestAnimationFrame(updateBarragePositions);
};

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

// 组件生命周期
onMounted(() => {
  handleResize() // 初始化视窗尺寸
  window.addEventListener('resize', handleResize)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  startQueryTimer()

  // 初始化时间并启动动画循环
  lastUpdateTime = Date.now();
  animationFrameId = requestAnimationFrame(updateBarragePositions);
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  stopQueryTimer()

  // 停止动画循环
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId);
  }
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

/* 全屏弹幕容器 */
.barrage-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  user-select: none;
  animation: hideToShow 2s;
  overflow: hidden;
  font-family: 'Noto Sans SC', sans-serif;
  z-index: 10;
}

/* 弹幕舞台：全屏覆盖 */
.barrage-stage {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

/* 弹幕项样式（统一蓝色，使用JS控制滚动动画） */
.barrage-item {
  position: absolute;
  display: flex;
  align-items: center;
  padding: 4px 16px;
  /* 统一蓝色背景（非渐变） */
  background: #6eb1f8;
  border-radius: 24px;
  color: #0a0a0a;
  font-size: 16px;
  white-space: nowrap;
  will-change: transform;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  gap: 12px;
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  letter-spacing: 0.5px;
  font-weight: 500;
  pointer-events: auto;
  opacity: 1; /* 固定不透明，无渐变 */
  max-height: 50px;
  box-sizing: border-box;
  z-index: 1; /* 低于输入框遮罩 */
}

/* 当前用户发送的消息 - 绿色背景 */
.barrage-item.current-user {
  background: #52c41a; /* 绿色背景 */
  color: #ffffff;
}

/* 暂停的弹幕 - 橙色背景 */
.barrage-item.paused {
  background: #fa8c16; /* 橙色背景 */
  color: #ffffff;
}

/* 定义弹幕滚动动画 */
@keyframes barrageMove {
  from {
    transform: translateX(calc(100vw + 100%));
  }
  to {
    transform: translateX(-100%);
  }
}

/* 暂停时的样式 */
.barrage-item.active.paused {
  animation-play-state: paused;
}

/* 激活状态：应用动画 */
.barrage-item.active {
  transition: none; /* 使用JS控制动画，不使用CSS过渡 */
}

/* 移除交替透明度，统一显示效果 */
.barrage-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
  flex-shrink: 0;
}

.barrage-text {
  line-height: 1.4;
  max-width: calc(100vw - 120px);
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 输入框容器：屏幕正中间 */
.message-in {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 100; /* 最高层级 */
  width: min(90vw, 600px);
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
}

.message-in.show {
  opacity: 1;
  pointer-events: auto;
}

/* 输入框遮罩层：防止弹幕穿透交互 + 呼吸边框效果 */
.input-mask {
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 32px;
  padding: 4px;
  pointer-events: auto;
  /* 呼吸边框相关样式 */
  position: relative;
  overflow: hidden;
}

/* 输入框呼吸边框动画 - 仅作用于边框 */
.input-mask::before {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 34px; /* 比input-mask大2px，匹配圆角 */
  background: linear-gradient(90deg, #1E90FF, #87CEEB, #1E90FF);
  background-size: 200% 200%;
  z-index: -1;
  /* 呼吸脉搏动画 */
  animation: breathBorder 3s ease-in-out infinite;
}

/* 输入框样式 */
.input-box {
  width: 100%;
  height: 60px;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(20px);
  border-radius: 28px;
  padding: 8px 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

.input-box:hover {
  background: rgba(0, 0, 0, 0.85);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
  transform: translateY(-1px);
}

.input-box:focus-within {
  background: rgba(0, 0, 0, 0.9);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2), 0 12px 40px rgba(0, 0, 0, 0.25);
  border-color: rgba(255, 255, 255, 0.2);
}

.message-input {
  flex: 1;
  height: 44px;
  background: transparent;
  border: none;
  padding: 0 12px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  font-weight: 400;
  transition: all 0.3s ease;
  letter-spacing: 0.3px;
  line-height: 1.5;
  outline: none;
  box-sizing: border-box;
}

.message-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
  font-size: 15px;
  letter-spacing: 0.3px;
  font-weight: 400;
}

.send-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  background: rgba(30, 144, 255, 0.3); /* 匹配弹幕的蓝色系 */
  border: none;
  color: #ffffff;
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  letter-spacing: 0.5px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  text-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
  cursor: pointer;
}

.send-btn:not(.disabled):hover {
  transform: translateY(-1px);
  background: rgba(30, 144, 255, 0.5);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.25);
}

.send-btn:not(.disabled):active {
  transform: translateY(0);
  background: rgba(30, 144, 255, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.send-btn.disabled {
  background: rgba(30, 144, 255, 0.1);
  color: rgba(255, 255, 255, 0.3);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 淡入动画 */
@keyframes hideToShow {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

/* 输入框呼吸脉搏边框动画 */
@keyframes breathBorder {
  0% {
    background-position: 0% 50%;
    opacity: 0.4;
  }
  50% {
    background-position: 100% 50%;
    opacity: 1;
  }
  100% {
    background-position: 0% 50%;
    opacity: 0.4;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .barrage-item {
    padding: 6px 12px;
    font-size: 14px;
    max-height: 45px;
    gap: 8px;
  }

  .barrage-avatar {
    width: 24px;
    height: 24px;
  }

  .barrage-text {
    max-width: calc(100vw - 80px);
  }

  .input-box {
    height: 50px;
    padding: 6px 10px;
  }

  .message-input {
    height: 36px;
    font-size: 15px;
    padding: 0 8px;
  }

  .send-btn {
    height: 32px;
    padding: 0 16px;
    font-size: 14px;
  }

  /* 移动端呼吸边框动画调整 */
  @keyframes breathBorder {
    0% {
      background-position: 0% 50%;
      opacity: 0.3;
    }
    50% {
      background-position: 100% 50%;
      opacity: 0.8;
    }
    100% {
      background-position: 0% 50%;
      opacity: 0.3;
    }
  }
}
</style>
