<template>
  <div
    id="globalSider"
    class="sider-container"
    :class="{ 'minimized': isMinimized, 'is-dragging': isDragging }"
    :style="{
      left: isDragging ? `${dragLeft}px` : undefined,
      top: isDragging ? `${dragTop}px` : undefined,
      right: isDragging ? 'auto' : undefined,
      transform: isDragging ? 'none' : 'translateY(-50%)'
    }"
  >
    <!-- 侧边栏主体内容 - 胶囊形态 (仅在未收起时存在) -->
    <div v-if="!isMinimized" class="sider-wrapper" @mousedown="handleMouseDown">
      <!-- 固定顶部：头像 -->
      <div class="fixed-header">
        <a-tooltip :placement="tooltipPlacement" title="个人中心">
          <router-link to="/my" class="avatar-link">
            <div class="avatar-wrapper">
              <a-avatar :src="userAvatar" :size="32" />
              <div class="status-indicator"></div>
            </div>
          </router-link>
        </a-tooltip>
      </div>

      <!-- 可滚动中段：菜单导航 -->
      <div class="scrollable-content">
        <div class="item-list">
          <a-tooltip :placement="tooltipPlacement" title="首页">
            <router-link to="/" class="nav-item" :class="{ 'active': route.path === '/' }">
              <div class="icon-box"><i class="fa-solid fa-house"></i></div>
            </router-link>
          </a-tooltip>

          <a-tooltip :placement="tooltipPlacement" title="论坛">
            <router-link to="/forum" class="nav-item" :class="{ 'active': route.path === '/forum' }">
              <div class="icon-box"><i class="fa-solid fa-file-lines"></i></div>
            </router-link>
          </a-tooltip>

          <a-tooltip :placement="tooltipPlacement" title="聊天">
            <router-link :to="chatRoute" class="nav-item" :class="{ 'active': route.path === chatRoute }">
              <div class="icon-box"><i class="fa-solid fa-comments"></i></div>
            </router-link>
          </a-tooltip>

          <a-tooltip :placement="tooltipPlacement" title="消息">
            <router-link to="/message-center" class="nav-item" :class="{ 'active': route.path === '/message-center' }">
              <div class="icon-box">
                <i class="fa-solid fa-bell"></i>
                <div v-if="unreadCount > 0" class="mini-badge"></div>
              </div>
            </router-link>
          </a-tooltip>

          <a-tooltip :placement="tooltipPlacement" title="游艺场">
            <router-link to="/games" class="nav-item" :class="{ 'active': route.path === '/games' }">
              <div class="icon-box"><i class="fa-solid fa-rocket"></i></div>
            </router-link>
          </a-tooltip>

          <a-tooltip :placement="tooltipPlacement" title="百宝箱">
            <router-link to="/tools" class="nav-item" :class="{ 'active': route.path === '/tools' }">
              <div class="icon-box"><i class="fa-solid fa-cubes"></i></div>
            </router-link>
          </a-tooltip>
        </div>
      </div>

      <!-- 固定底部：控制 -->
      <div class="fixed-footer">
        <a-tooltip :placement="tooltipPlacement" title="收起">
          <button class="nav-item control-btn" @click.stop="toggleMinimize">
            <div class="icon-box">
              <i class="fa-solid" :class="layoutStore.siderSide === 'left' ? 'fa-chevron-left' : 'fa-chevron-right'"></i>
            </div>
          </button>
        </a-tooltip>
      </div>
    </div>

    <!-- “趴在边缘”的极简唤回按钮 -->
    <a-tooltip :placement="tooltipPlacement" title="展开侧边栏">
      <div v-show="isMinimized" class="sticky-edge-trigger" @click="toggleMinimize">
        <i class="fa-solid sticky-icon" :class="layoutStore.siderSide === 'left' ? 'fa-angle-double-right' : 'fa-angle-double-left'"></i>
      </div>
    </a-tooltip>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useMessageCenterStore } from '@/stores/useMessageCenterStore'
import { useLayoutStore } from '@/stores/useLayoutStore'
import { getDefaultAvatar } from '@/utils/userUtils.ts'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

interface Props {
  // Define any props here if needed
}

const loginUserStore = useLoginUserStore()
const messageCenterStore = useMessageCenterStore()
const layoutStore = useLayoutStore()
const route = useRoute()
const props = defineProps<Props>()

const isMinimized = ref(getDeviceType() === DEVICE_TYPE_ENUM.MOBILE) // 移动端默认收起
const isDragging = ref(false)
const dragLeft = ref(0) // 拖拽时的绝对左偏移
const dragTop = ref(0) // 拖拽时的绝对顶偏移
const offsetX = ref(0) // 鼠标相对于组件左边缘的偏移
const offsetY = ref(0) // 鼠标相对于组件顶边缘的偏移
const menuListRef = ref<HTMLElement | null>(null)

const userAvatar = computed(() => loginUserStore.loginUser?.userAvatar || getDefaultAvatar(loginUserStore.loginUser?.userName || ''))
const unreadCount = computed(() => messageCenterStore.unreadTotal)
const chatRoute = computed(() => getDeviceType() === DEVICE_TYPE_ENUM.MOBILE ? '/chat-list' : '/pc-chat')

// 动态 Tooltip 方向
const tooltipPlacement = computed(() => layoutStore.siderSide === 'left' ? 'right' : 'left')

const toggleMinimize = () => {
  isMinimized.value = !isMinimized.value
}

// 拖拽逻辑
const handleMouseDown = (e: MouseEvent) => {
  if (getDeviceType() === DEVICE_TYPE_ENUM.MOBILE) return
  isDragging.value = true

  // 获取当前组件的矩形信息
  const rect = (e.currentTarget as HTMLElement).parentElement?.getBoundingClientRect()
  if (rect) {
    offsetX.value = e.clientX - rect.left
    offsetY.value = e.clientY - rect.top
    dragLeft.value = rect.left
    dragTop.value = rect.top
  }

  window.addEventListener('mousemove', handleMouseMove)
  window.addEventListener('mouseup', handleMouseUp)
  e.preventDefault()
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return
  const currentX = e.clientX
  const currentY = e.clientY
  const screenWidth = window.innerWidth

  // 更新绝对坐标
  dragLeft.value = currentX - offsetX.value
  dragTop.value = currentY - offsetY.value

  // 仅根据位置逻辑更新侧边状态（触发镜像）
  if (currentX < screenWidth / 2) {
    if (layoutStore.siderSide !== 'left') {
      layoutStore.setSiderSide('left')
    }
  } else {
    if (layoutStore.siderSide !== 'right') {
      layoutStore.setSiderSide('right')
    }
  }
}

const handleMouseUp = () => {
  isDragging.value = false
  window.removeEventListener('mousemove', handleMouseMove)
  window.removeEventListener('mouseup', handleMouseUp)
}

onMounted(() => {
  // 可以在这里初始化消息数量
})
</script>

<style scoped>
/* 统一容器模型，防止宽度计算偏差导致的圆角裁减 */
* {
  box-sizing: border-box;
}

.sider-container {
  height: min(400px, 60vh); /* 缩小高度 */
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1); /* 强化弹性系数 */
  width: 56px;
  z-index: 1000;
  background: transparent !important;
}

.sider-container.is-dragging {
  transition: none !important;
  z-index: 10001; /* 拖拽时置顶 */
}

.sider-container.minimized {
  width: 0;
}

/* 靠左样式 */
.is-left {
  left: 0;
}

/* 靠右样式 */
.is-right {
  right: 0;
  left: auto;
}

.sider-container:hover.minimized.is-left {
  left: 0;
}

.sider-container:hover.minimized.is-right {
  right: 0;
}

/* 核心“智慧胶囊” */
.sider-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--header-background);
  backdrop-filter: blur(28px) saturate(190%);
  -webkit-backdrop-filter: blur(28px) saturate(190%);
  border: 1.5px solid var(--header-border);
  border-radius: 32px; /* 大圆角胶囊感 */
  box-shadow: 0 12px 48px var(--header-shadow);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  padding: 12px 0;
  cursor: grab;
}

.sider-wrapper:active {
  cursor: grabbing;
}

.sider-container.minimized .sider-wrapper {
  opacity: 0;
  visibility: hidden; /* 强制彻底隐藏 */
  transform: translateX(var(--minimized-x, -100%)) scale(0.9);
  pointer-events: none;
  display: none; /* 移动端背景残留的最强杀手锏 */
}

.is-left .sider-wrapper {
  --minimized-x: -100%;
}

.is-right .sider-wrapper {
  --minimized-x: 100%;
}

/* 固定顶部 */
.fixed-header {
  width: 100%;
  padding: 8px 0 16px 0;
  display: flex;
  justify-content: center;
  border-bottom: 1px solid var(--header-border);
  margin-bottom: 8px;
}

.avatar-wrapper {
  position: relative;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.avatar-link:hover .avatar-wrapper {
  transform: scale(1.15) rotate(5deg);
}

.status-indicator {
  position: absolute;
  bottom: 0px;
  right: 0px;
  width: 8px;
  height: 8px;
  background: #27c93f;
  border: 1.5px solid white;
  border-radius: 50%;
}

/* 内部滚动区 */
.scrollable-content {
  width: 100%;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 隐藏滚动条 */
.scrollable-content::-webkit-scrollbar {
  width: 0px;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  align-items: center;
}

/* 菜单项 */
.nav-item {
  display: flex;
  align-items: center;
  justify-content: center; /* 默认居中 */
  height: 38px;
  width: 38px; /* 固定宽度确保圆形感 */
  border-radius: 19px;
  color: var(--header-text);
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: none;
  background: transparent;
  cursor: pointer;
  position: relative;
  padding: 0;
}

.nav-item:hover {
  background: var(--nav-item-hover);
  color: var(--nav-item-active-text);
  transform: translateX(var(--hv-x, 2px));
}

.is-left .nav-item:hover { --hv-x: 2px; }
.is-right .nav-item:hover { --hv-x: -2px; }

.nav-item.active {
  background: #2563eb;
  color: white;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.4);
}

.icon-box {
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
}


/* 固定底部 */
.fixed-footer {
  width: 100%;
  margin-top: 8px;
  padding: 10px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  justify-content: center;
}

.control-btn {
  width: 38px;
  height: 38px;
}

/* 消息中心徽章 */
.mini-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  background: #ef4444;
  border-radius: 50%;
  border: 1px solid white;
}

/* “趴在边缘”的极简唤回按钮 (替代主体) */
.sticky-edge-trigger {
  position: fixed;
  top: 50%;
  left: 0;
  transform: translateY(-50%);
  width: 22px; /* 增加宽度，确保可见 */
  height: min(120px, 20vh);
  background: linear-gradient(135deg, #2563eb, #70a1ff);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 6px 0 20px rgba(37, 99, 235, 0.4);
  transition: all 0.6s cubic-bezier(0.34, 1.56, 0.64, 1); /* 唤回按钮也增加弹性 */
  z-index: 1001;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.is-left .sticky-edge-trigger {
  left: 0;
  border-radius: 0 16px 16px 0;
}

.is-right .sticky-edge-trigger {
  right: 0;
  left: auto;
  border-radius: 16px 0 0 16px;
  box-shadow: -6px 0 20px rgba(37, 99, 235, 0.4);
}

.sticky-edge-trigger:hover {
  width: 38px;
  background: #2563eb;
}

.sticky-icon {
  color: white;
  font-size: 16px; /* 增大图标 */
  opacity: 1; /* 取消低透明度，确保看清 */
  transition: all 0.3s;
}

.sticky-edge-trigger:hover .sticky-icon {
  opacity: 1;
  transform: scale(1.3) translateX(var(--icon-x, 2px));
}

.is-left .sticky-edge-trigger:hover .sticky-icon { --icon-x: 2px; }
.is-right .sticky-edge-trigger:hover .sticky-icon { --icon-x: -2px; }

/* 移动端精致适配 (Mobile Refinement) */
@media (max-width: 768px) {
  .sider-container {
    height: min(360px, 60vh); /* 移动端再压缩高度，更轻量 */
    width: 52px; /* 基础宽度缩小 */
  }

  .sider-wrapper {
    border-radius: 24px;
    padding: 8px 0;
  }

  .nav-item {
    height: 38px;
    width: 38px;
    border-radius: 19px;
  }

  .icon-box {
    font-size: 17px;
    min-width: 28px;
  }

  .fixed-header {
    padding: 4px 0 12px 0;
  }

  /* 移动端贴边按钮极致瘦身 */
  .sticky-edge-trigger {
    width: 4px; /* 极细，不打扰视觉 */
    height: 80px; /* 高度也缩小 */
    pointer-events: auto;
    justify-content: flex-end; /* 图标靠边 */
    padding-right: 2px;
  }

  .is-left .sticky-edge-trigger {
    border-radius: 0 8px 8px 0;
  }

  .is-right .sticky-edge-trigger {
    border-radius: 8px 0 0 8px;
    justify-content: flex-start;
    padding-left: 2px;
    padding-right: 0;
  }

  .sticky-edge-trigger:active {
    width: 24px; /* 点击时稍微弹出作为反馈 */
    background: #1d4ed8;
  }

  .sticky-icon {
    font-size: 10px; /* 图标缩小，适应窄条 */
  }
}

/* 暗色模式适配 */
:deep(.dark-theme) .sider-wrapper {
  background: rgba(30, 41, 59, 0.85);
  border-color: rgba(255, 255, 255, 0.15);
  color: white;
}

:deep(.dark-theme) .nav-item {
  color: #94a3b8;
}

:deep(.dark-theme) .fixed-header,
:deep(.dark-theme) .fixed-footer {
  border-color: rgba(255, 255, 255, 0.08);
}
</style>
