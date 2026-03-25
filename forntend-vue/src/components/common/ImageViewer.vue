<template>
  <Teleport to="body">
    <div v-if="visible" class="image-viewer-overlay" @click="handleClose">
      <div class="image-viewer-container" @click.stop>
        <div class="image-viewer-content">
          <img
            ref="imageRef"
            :src="imageUrl"
            :alt="imageAlt"
            class="viewer-image"
            @click.stop
            @touchstart.prevent="handleTouchStart"
            @touchmove.prevent="handleTouchMove"
            @touchend.prevent="handleTouchEnd"
            @wheel.prevent="handleWheel"
            :style="{
              transform: `scale(${scale}) rotate(${rotation}deg) translate(${translateX}px, ${translateY}px)`,
              transition: isTransitioning ? 'all 0.3s ease' : 'none'
            }"
          />

          <!-- 工具栏 -->
          <div class="toolbar" @click.stop>
            <button class="tool-button" @click="rotateLeft" title="向左旋转">
              <svg viewBox="0 0 24 24" class="icon">
                <path fill="currentColor" d="M7.11 8.53L5.7 7.11C4.8 8.27 4.24 9.61 4.07 11h2.02c.14-.87.49-1.72 1.02-2.47zM6.09 13H4.07c.17 1.39.72 2.73 1.62 3.89l1.41-1.42c-.52-.75-.87-1.59-1.01-2.47zm1.01 5.32c1.16.9 2.51 1.44 3.9 1.61V17.9c-.87-.15-1.71-.49-2.46-1.03L7.1 18.32zM13 4.07V1L8.45 5.55 13 10V6.09c2.84.48 5 2.94 5 5.91s-2.16 5.43-5 5.91v2.02c3.95-.49 7-3.85 7-7.93s-3.05-7.44-7-7.93z"/>
              </svg>
            </button>
            <button class="tool-button" @click="rotateRight" title="向右旋转">
              <svg viewBox="0 0 24 24" class="icon">
                <path fill="currentColor" d="M15.55 5.55L11 1v3.07C7.06 4.56 4 7.92 4 12s3.05 7.44 7 7.93v-2.02c-2.84-.48-5-2.94-5-5.91s2.16-5.43 5-5.91V10l4.55-4.45zM19.93 11c-.17-1.39-.72-2.73-1.62-3.89l-1.42 1.42c.54.75.88 1.6 1.02 2.47h2.02zM13 17.9v2.02c1.39-.17 2.74-.71 3.9-1.61l-1.44-1.44c-.75.54-1.59.89-2.46 1.03zm3.89-2.42l1.42 1.41c.9-1.16 1.45-2.5 1.62-3.89h-2.02c-.14.87-.48 1.72-1.02 2.48z"/>
              </svg>
            </button>
            <button class="tool-button" @click="zoomIn" title="放大">
              <svg viewBox="0 0 24 24" class="icon">
                <path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
              </svg>
            </button>
            <button class="tool-button" @click="zoomOut" title="缩小">
              <svg viewBox="0 0 24 24" class="icon">
                <path fill="currentColor" d="M19 13H5v-2h14v2z"/>
              </svg>
            </button>
            <button class="tool-button" @click="resetTransform" title="重置">
              <svg viewBox="0 0 24 24" class="icon">
                <path fill="currentColor" d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
              </svg>
            </button>
            <button class="tool-button close" @click="handleClose" title="关闭">
              <svg viewBox="0 0 24 24" class="icon">
                <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Ref } from 'vue'

interface Props {
  visible: boolean
  imageUrl: string
  imageAlt?: string
}

const props = withDefaults(defineProps<Props>(), {
  imageAlt: ''
})

const emit = defineEmits(['update:visible', 'close'])

// 状态
const scale = ref(1)
const rotation = ref(0)
const translateX = ref(0)
const translateY = ref(0)
const isTransitioning = ref(false)

// 触摸状态
let initialScale = 1
let initialDistance = 0
let lastTouchX = 0
let lastTouchY = 0
let lastTouchTime = 0
let tapCount = 0

const imageRef = ref<HTMLImageElement | null>(null)

// 重置变换
const resetTransform = () => {
  scale.value = 1
  rotation.value = 0
  translateX.value = 0
  translateY.value = 0
  isTransitioning.value = true
  setTimeout(() => {
    isTransitioning.value = false
  }, 300)
}

// 旋转操作
const rotateLeft = () => {
  rotation.value = (rotation.value - 90) % 360
  isTransitioning.value = true
  setTimeout(() => {
    isTransitioning.value = false
  }, 300)
}

const rotateRight = () => {
  rotation.value = (rotation.value + 90) % 360
  isTransitioning.value = true
  setTimeout(() => {
    isTransitioning.value = false
  }, 300)
}

// 缩放操作
const zoomIn = () => {
  scale.value = Math.min(scale.value * 1.2, 3)
  isTransitioning.value = true
  setTimeout(() => {
    isTransitioning.value = false
  }, 300)
}

const zoomOut = () => {
  scale.value = Math.max(scale.value / 1.2, 0.5)
  isTransitioning.value = true
  setTimeout(() => {
    isTransitioning.value = false
  }, 300)
}

// 处理关闭
const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  resetTransform()
}

// 处理滚轮缩放
const handleWheel = (e: WheelEvent) => {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const newScale = Math.min(Math.max(scale.value + delta, 0.5), 3)

  if (newScale !== scale.value) {
    scale.value = newScale
    isTransitioning.value = true
    setTimeout(() => {
      isTransitioning.value = false
    }, 300)
  }
}

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  const touches = e.touches
  const now = Date.now()

  // 检测双击
  if (touches.length === 1) {
    if (now - lastTouchTime < 300) {
      tapCount++
      if (tapCount === 2) {
        // 双击时重置或放大
        if (scale.value > 1) {
          resetTransform()
        } else {
          scale.value = 2
          isTransitioning.value = true
          setTimeout(() => {
            isTransitioning.value = false
          }, 300)
        }
        tapCount = 0
      }
    } else {
      tapCount = 1
    }
    lastTouchTime = now
    lastTouchX = touches[0].clientX
    lastTouchY = touches[0].clientY
  } else if (touches.length === 2) {
    // 双指操作初始化
    const dx = touches[1].clientX - touches[0].clientX
    const dy = touches[1].clientY - touches[0].clientY
    initialDistance = Math.sqrt(dx * dx + dy * dy)
    initialScale = scale.value
  }

  isTransitioning.value = false
}

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  e.preventDefault()
  const touches = e.touches

  if (touches.length === 2) {
    // 双指缩放
    const dx = touches[1].clientX - touches[0].clientX
    const dy = touches[1].clientY - touches[0].clientY
    const distance = Math.sqrt(dx * dx + dy * dy)

    requestAnimationFrame(() => {
      const scaleFactor = distance / initialDistance
      let newScale = initialScale * (1 + (scaleFactor - 1) * 0.8)

      // 限制缩放范围
      newScale = Math.min(Math.max(newScale, 0.5), 3)
      scale.value = newScale
    })
  } else if (touches.length === 1 && scale.value > 1) {
    // 单指平移
    const currentX = touches[0].clientX
    const currentY = touches[0].clientY
    const deltaX = currentX - lastTouchX
    const deltaY = currentY - lastTouchY

    requestAnimationFrame(() => {
      // 根据旋转角度调整移动方向
      const rad = (rotation.value * Math.PI) / 180
      const adjustedDeltaX = deltaX * Math.cos(rad) + deltaY * Math.sin(rad)
      const adjustedDeltaY = -deltaX * Math.sin(rad) + deltaY * Math.cos(rad)

      translateX.value += adjustedDeltaX
      translateY.value += adjustedDeltaY

      // 限制移动范围
      const maxTranslate = 100 * scale.value
      translateX.value = Math.min(Math.max(translateX.value, -maxTranslate), maxTranslate)
      translateY.value = Math.min(Math.max(translateY.value, -maxTranslate), maxTranslate)
    })

    lastTouchX = currentX
    lastTouchY = currentY
  }
}

// 处理触摸结束
const handleTouchEnd = () => {
  if (scale.value < 1) {
    scale.value = 1
    isTransitioning.value = true
  }

  initialScale = scale.value
  initialDistance = 0
}

// 键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  if (!props.visible) return

  switch (e.key) {
    case 'Escape':
      handleClose()
      break
    case 'ArrowLeft':
      rotateLeft()
      break
    case 'ArrowRight':
      rotateRight()
      break
    case '+':
    case '=':
      zoomIn()
      break
    case '-':
      zoomOut()
      break
    case 'r':
      resetTransform()
      break
  }
}

// 生命周期钩子
onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.image-viewer-overlay {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.9);
  backdrop-filter: blur(10px);
  z-index: 9999;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.2s ease;
}

.image-viewer-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.image-viewer-content {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.viewer-image {
  max-width: 90%;
  max-height: 90vh;
  object-fit: contain;
  user-select: none;
  will-change: transform;
  cursor: grab;
}

.viewer-image:active {
  cursor: grabbing;
}

.toolbar {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 8px;
  z-index: 10000;
}

.tool-button {
  width: 40px;
  height: 40px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tool-button:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: scale(1.1);
}

.tool-button.close {
  position: fixed;
  top: 20px;
  right: 20px;
}

.icon {
  width: 24px;
  height: 24px;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .viewer-image {
    max-width: 95%;
    max-height: 85vh;
  }

  .toolbar {
    bottom: 30px;
    padding: 8px;
    gap: 8px;
  }

  .tool-button {
    width: 36px;
    height: 36px;
  }

  .tool-button.close {
    top: auto;
    right: auto;
    bottom: 100px;
    left: 50%;
    transform: translateX(-50%);
  }

  .icon {
    width: 20px;
    height: 20px;
  }
}
</style>
