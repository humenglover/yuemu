<template>
  <!-- 强制全屏容器 -->
  <div class="box">
    <div class="sticky-wall" :class="{ 'is-mobile': isMobile }">
      <!-- 顶部控制栏（居中悬浮） -->
      <div class="controls">
        <div class="control-card">
          <div class="control-group">
            <a-button type="primary" class="add-btn" @click="addNote">
              <template #icon><PlusOutlined /></template>
              新建便签
            </a-button>
            <a-button class="clear-btn" @click="clearAll">
              <template #icon><DeleteOutlined /></template>
              清空全部
            </a-button>
          </div>
        </div>
      </div>

      <!-- 便签容器（全屏） -->
      <div id="board" class="notes-container">
        <!-- 空状态提示 -->
        <div v-if="notes.length === 0" class="empty-state">
          <div class="empty-icon">🖊️</div>
          <div class="empty-text">点击上方按钮创建你的第一个便签</div>
        </div>

        <!-- 便签列表 -->
        <div
          v-for="note in notes"
          :key="note.id"
          class="card"
          :class="{
            dragging: draggedNote?.id === note.id,
            maximized: note.maximized
          }"
          :style="{
            backgroundColor: note.color,
            transform: `scale(${note.scale}) rotate(${note.rotation}deg)`,
            left: note.x + 'px',
            top: note.y + 'px',
            zIndex: note.zIndex,
            opacity: note.opacity
          }"
          @mousedown="startDrag($event, note)"
          @touchstart="startDrag($event, note)"
          @click="handleCardClick($event, note)"
          @dblclick="handleCardDblClick($event, note)"
        >
          <!-- 便签头部 -->
          <div class="card-header" :class="{ dragging: draggedNote?.id === note.id }">
            <div class="window-controls">
              <button class="control close" type="button" aria-label="关闭" @click.stop="deleteNote(note.id)"></button>
              <button class="control minimize" type="button" aria-label="最小化" @click.stop="minimizeNote(note)"></button>
              <button class="control maximize" type="button" aria-label="最大化" @click.stop="toggleMaximize(note)"></button>
            </div>
            <div class="card-title">便签</div>
          </div>

          <!-- 便签内容 -->
          <div class="card-body">
            <textarea
              v-model="note.content"
              placeholder="输入你的想法..."
              @input="saveNotes"
              @touchstart.stop
              :class="{ maximized: note.maximized }"
            ></textarea>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

// 扩展Note接口
interface Note {
  id: number
  content: string
  color: string
  x: number
  y: number
  rotation: number
  zIndex: number
  scale: number
  opacity: number
  maximized: boolean
  closing: boolean
  beforeMaximize?: {
    left: number
    top: number
    scale: number
    rotation: number
  }
}

// 柔和莫兰迪色系（更美观）
const colors = [
  '#F8E2CF', '#E8F4F8', '#F0F8F1',
  '#FDF2F8', '#F5F5F5', '#EAF6FA', '#FFF8E6'
]

// 状态管理
const notes = ref<Note[]>([])
const draggedNote = ref<Note | null>(null)
const activeMaximizedCard = ref<Note | null>(null)
const isMobile = ref(false)
const zIndexCursor = ref(200)
const MAXIMIZED_LAYER = 1000000

// 初始化设备检测
const initDeviceDetect = () => {
  const pointerMediaQuery = window.matchMedia('(pointer: coarse)')
  isMobile.value = pointerMediaQuery.matches || window.innerWidth <= 768

  const handleMediaChange = () => {
    isMobile.value = pointerMediaQuery.matches || window.innerWidth <= 768
  }

  if (pointerMediaQuery.addEventListener) {
    pointerMediaQuery.addEventListener('change', handleMediaChange)
  } else {
    pointerMediaQuery.addListener(handleMediaChange)
  }

  window.addEventListener('resize', handleMediaChange)

  return () => {
    if (pointerMediaQuery.removeEventListener) {
      pointerMediaQuery.removeEventListener('change', handleMediaChange)
    } else {
      pointerMediaQuery.removeListener(handleMediaChange)
    }
    window.removeEventListener('resize', handleMediaChange)
  }
}

// 添加新便签
const addNote = () => {
  const cardWidth = isMobile.value ? 180 : 220
  const cardHeight = isMobile.value ? 130 : 140
  const horizontalMargin = isMobile.value ? 20 : 30
  const verticalMargin = isMobile.value ? 20 : 40

  // 更合理的随机位置（避开顶部按钮）
  const left = horizontalMargin + Math.random() *
    Math.max(window.innerWidth - cardWidth - horizontalMargin * 2, 0)
  const top = verticalMargin + Math.random() *
    Math.max(window.innerHeight - cardHeight - verticalMargin * 2, 0)

  // 更轻微的旋转角度（更整洁）
  const angleRange = isMobile.value ? 3 : 5
  const rotation = (Math.random() - 0.5) * angleRange

  if (activeMaximizedCard.value && zIndexCursor.value >= MAXIMIZED_LAYER - 2) {
    zIndexCursor.value = MAXIMIZED_LAYER - 2
  }

  const newNote: Note = {
    id: Date.now(),
    content: '',
    color: colors[Math.floor(Math.random() * colors.length)],
    x: left,
    y: top,
    rotation: rotation,
    zIndex: ++zIndexCursor.value,
    scale: isMobile.value ? 0.9 : 0.85,
    opacity: 0,
    maximized: false,
    closing: false
  }

  notes.value.push(newNote)

  // 平滑入场动画
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const note = notes.value.find(n => n.id === newNote.id)
      if (note) {
        note.scale = 1
        note.opacity = 1
      }
    })
  })

  // 限制数量避免拥挤
  if (notes.value.length > (isMobile.value ? 80 : 120)) {
    notes.value.shift()
  }

  saveNotes()
  message.success('✅ 新便签已创建')
}

// 删除单个便签
const deleteNote = (id: number) => {
  const note = notes.value.find(n => n.id === id)
  if (!note || note.closing) return

  if (note === activeMaximizedCard.value) {
    activeMaximizedCard.value = null
  }

  // 优雅的删除动画
  note.closing = true
  note.scale = 0.8
  note.opacity = 0

  setTimeout(() => {
    notes.value = notes.value.filter(n => n.id !== id)
    saveNotes()
    message.success('🗑️ 便签已删除')
  }, 300)
}

// 最小化便签
const minimizeNote = (note: Note) => {
  if (note.closing) return

  note.closing = true
  const bottom = Math.max(window.innerHeight - 24, 0)
  const targetLeft = Math.max(16, Math.min(note.x, window.innerWidth - 200 - 16))

  note.x = targetLeft
  note.y = bottom
  note.scale = 0.2
  note.rotation = 0
  note.opacity = 0.2

  setTimeout(() => {
    notes.value = notes.value.filter(n => n.id !== note.id)
    saveNotes()
  }, 300)
}

// 最大化/还原便签
const toggleMaximize = (note: Note) => {
  if (note.closing) return

  if (note.maximized) {
    // 还原
    if (note.beforeMaximize) {
      note.x = note.beforeMaximize.left
      note.y = note.beforeMaximize.top
      note.scale = note.beforeMaximize.scale
      note.rotation = note.beforeMaximize.rotation
    } else {
      note.scale = 1
      note.rotation = (Math.random() - 0.5) * (isMobile.value ? 3 : 5)
    }
    note.maximized = false
    activeMaximizedCard.value = null
  } else {
    // 最大化
    if (activeMaximizedCard.value) {
      const activeNote = activeMaximizedCard.value
      if (activeNote.beforeMaximize) {
        activeNote.x = activeNote.beforeMaximize.left
        activeNote.y = activeNote.beforeMaximize.top
        activeNote.scale = activeNote.beforeMaximize.scale
        activeNote.rotation = activeNote.beforeMaximize.rotation
      }
      activeNote.maximized = false
    }

    // 保存最大化前状态
    note.beforeMaximize = {
      left: note.x,
      top: note.y,
      scale: note.scale,
      rotation: note.rotation
    }

    note.maximized = true
    note.x = 0
    note.y = 0
    note.scale = 1
    note.rotation = 0
    note.zIndex = MAXIMIZED_LAYER
    activeMaximizedCard.value = note
  }

  saveNotes()
}

// 清空所有便签
const clearAll = () => {
  if (notes.value.length === 0) {
    message.warning('⚠️ 当前没有便签可清空')
    return
  }

  Modal.confirm({
    title: '清空所有便签',
    content: '确定要删除所有便签吗？所有内容将无法恢复',
    okText: '清空',
    cancelText: '取消',
    okType: 'danger',
    onOk: () => {
      // 批量删除动画
      notes.value.forEach(note => {
        note.closing = true
        note.scale = 0.8
        note.opacity = 0
      })

      setTimeout(() => {
        notes.value = []
        activeMaximizedCard.value = null
        saveNotes()
        message.success('🗑️ 已清空所有便签')
      }, 300)
    }
  })
}

// 卡片点击处理
const handleCardClick = (event: MouseEvent, note: Note) => {
  const control = (event.target as HTMLElement).closest('.control')
  if (!control) {
    bringToFront(note)
  }
}

// 卡片双击处理
const handleCardDblClick = (event: MouseEvent, note: Note) => {
  const header = (event.target as HTMLElement).closest('.card-header')
  const control = (event.target as HTMLElement).closest('.control')

  if (header && !control) {
    toggleMaximize(note)
  }
}

// 提升层级
const bringToFront = (note: Note) => {
  if (note.maximized) {
    note.zIndex = MAXIMIZED_LAYER
    return
  }

  zIndexCursor.value += 1
  if (activeMaximizedCard.value && zIndexCursor.value >= MAXIMIZED_LAYER) {
    zIndexCursor.value = MAXIMIZED_LAYER - 1
  }

  note.zIndex = zIndexCursor.value
}

// 开始拖拽
const startDrag = (event: MouseEvent | TouchEvent, note: Note) => {
  const textarea = (event.target as HTMLElement).tagName === 'TEXTAREA'
  const control = (event.target as HTMLElement).closest('.control')

  if (textarea || control || note.maximized || note.closing) return

  draggedNote.value = note
  bringToFront(note)

  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  const clientY = 'touches' in event ? event.touches[0].clientY : event.clientY

  const initialX = clientX - note.x
  const initialY = clientY - note.y

  // 高性能拖拽处理
  let dragFrame: number | null = null
  let pendingLeft = note.x
  let pendingTop = note.y

  const commitDrag = () => {
    dragFrame = null
    const maxLeft = Math.max(window.innerWidth - (isMobile.value ? 180 : 220), 0)
    const maxTop = Math.max(window.innerHeight - (isMobile.value ? 130 : 140), 0)

    note.x = Math.max(-(isMobile.value ? 180 : 220) * 0.2, Math.min(pendingLeft, maxLeft))
    note.y = Math.max(-(isMobile.value ? 130 : 140) * 0.2, Math.min(pendingTop, maxTop))
  }

  const handlePointerMove = (moveEvent: MouseEvent | TouchEvent) => {
    if (!draggedNote.value || draggedNote.value.id !== note.id) return

    const clientX = 'touches' in moveEvent ? moveEvent.touches[0].clientX : moveEvent.clientX
    const clientY = 'touches' in moveEvent ? moveEvent.touches[0].clientY : moveEvent.clientY

    pendingLeft = clientX - initialX
    pendingTop = clientY - initialY

    if (dragFrame === null) {
      dragFrame = requestAnimationFrame(commitDrag)
    }
  }

  const handlePointerUp = () => {
    draggedNote.value = null
    if (dragFrame !== null) {
      cancelAnimationFrame(dragFrame)
      commitDrag()
    }
    document.removeEventListener('mousemove', handlePointerMove as EventListener)
    document.removeEventListener('touchmove', handlePointerMove as EventListener)
    document.removeEventListener('mouseup', handlePointerUp)
    document.removeEventListener('touchend', handlePointerUp)

    saveNotes()
  }

  document.addEventListener('mousemove', handlePointerMove as EventListener, { passive: false })
  document.addEventListener('touchmove', handlePointerMove as EventListener, { passive: false })
  document.addEventListener('mouseup', handlePointerUp)
  document.addEventListener('touchend', handlePointerUp)
}

// 保存/加载便签
const saveNotes = () => {
  localStorage.setItem('sticky-notes', JSON.stringify(notes.value))
}

const loadNotes = () => {
  const savedNotes = localStorage.getItem('sticky-notes')
  if (savedNotes) {
    try {
      const parsedNotes = JSON.parse(savedNotes) as Note[]
      notes.value = parsedNotes.map(note => ({
        ...note,
        scale: note.scale || 1,
        opacity: note.opacity || 1,
        maximized: note.maximized || false,
        closing: note.closing || false
      }))

      const maxZIndex = Math.max(...notes.value.map(n => n.zIndex), 200)
      zIndexCursor.value = maxZIndex + 1
    } catch (e) {
      console.error('加载便签失败:', e)
      message.error('加载便签失败，将创建新的便签墙')
    }
  }
}

// 生命周期
let cleanupDeviceDetect: () => void
onMounted(() => {
  cleanupDeviceDetect = initDeviceDetect()
  loadNotes()

  // 强制全屏
  document.documentElement.style.overflow = 'hidden'
  document.body.style.overflow = 'hidden'
})

onUnmounted(() => {
  cleanupDeviceDetect()
  draggedNote.value = null

  // 恢复滚动
  document.documentElement.style.overflow = ''
  document.body.style.overflow = ''
})
</script>

<style scoped>
/* 强制全屏容器 - 核心样式 */
.box {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fff;
  z-index: 100;
  overflow: hidden;
}

/* 全局样式 */
.sticky-wall {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', sans-serif;
  background-color: #fff;
  background-image: linear-gradient(0deg, #f5f5f5 1px, transparent 0),
  linear-gradient(90deg, #f5f5f5 1px, transparent 0);
  background-size: 40px 40px;
  color: #333;
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.sticky-wall.is-mobile {
  overflow-y: auto;
}

/* 顶部控制栏 - 居中悬浮 */
.controls {
  position: fixed;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 999;
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
}

.control-card {
  backdrop-filter: blur(8px);
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #f0f0f0;
}

.control-group {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.control-group :deep(.ant-btn) {
  flex: 1;
  height: 44px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  transition: all 0.2s ease;
}

.control-group :deep(.add-btn) {
  background: #4299e1;
  color: white;
}

.control-group :deep(.add-btn:hover) {
  background: #3182ce;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(66, 153, 225, 0.3);
}

.control-group :deep(.clear-btn) {
  background: #f56565;
  color: white;
}

.control-group :deep(.clear-btn:hover) {
  background: #e53e3e;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(245, 101, 101, 0.3);
}

/* 便签容器 - 全屏 */
.notes-container {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.sticky-wall.is-mobile .notes-container {
  height: auto;
  min-height: 100%;
}

/* 空状态 */
.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #999;
  padding: 20px;
}

.empty-icon {
  font-size: 80px;
  margin-bottom: 20px;
  color: #ddd;
}

.empty-text {
  font-size: 18px;
  line-height: 1.5;
}

/* 便签样式 - 精致美观 */
.card {
  position: absolute;
  width: 220px;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  background: #fff;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: center;
}

.card.dragging {
  transition: none;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  z-index: 9999 !important;
}

.card.maximized {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  border-radius: 0;
  box-shadow: none;
  border: none;
  z-index: 99999 !important;
}

/* 便签头部 */
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  cursor: grab;
  user-select: none;
  border-bottom: 1px solid #f8f8f8;
}

.card-header.dragging {
  cursor: grabbing;
}

/* 窗口控制按钮 - 精致化 */
.window-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.window-controls .control {
  position: relative;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  outline: none;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.window-controls .control.close {
  background: #ff605c;
}

.window-controls .control.minimize {
  background: #ffbd44;
}

.window-controls .control.maximize {
  background: #00ca4e;
}

.window-controls .control:hover {
  transform: scale(1.1);
}

.window-controls .control::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.card-header:hover .window-controls .control::after {
  opacity: 0.9;
}

.window-controls .control.close::after {
  content: '×';
  font-size: 10px;
  line-height: 1;
  font-weight: 600;
  color: white;
}

.window-controls .control.minimize::after {
  width: 6px;
  height: 2px;
  background: white;
}

.window-controls .control.maximize::after {
  width: 7px;
  height: 7px;
  background: linear-gradient(
    45deg,
    white 0%,
    white 45%,
    transparent 45%,
    transparent 55%,
    white 55%,
    white 100%
  );
}

/* 便签标题 */
.card-title {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  padding-left: 8px;
  flex: 1;
}

/* 便签内容 */
.card-body {
  padding: 20px 16px;
}

.card-body textarea {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  background: transparent;
  font-size: 16px;
  line-height: 1.6;
  font-weight: 400;
  color: #333;
  word-break: break-word;
  overflow-wrap: anywhere;
  min-height: 90px;
  font-family: inherit;
}

.card-body textarea::placeholder {
  color: #ccc;
}

/* 最大化样式 */
.card.maximized {
  display: flex;
  flex-direction: column;
}

.card.maximized .card-title {
  display: none;
}

.card.maximized .card-header {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  z-index: 10;
}

.card.maximized .card-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  padding: 80px 20px 20px;
}

.card.maximized .card-body textarea {
  font-size: clamp(24px, 8vw, 64px);
  line-height: 1.2;
  min-height: auto;
  text-align: center;
  padding: 20px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .card {
    width: 180px;
    border-radius: 14px;
  }

  .card-body {
    padding: 16px 14px;
  }

  .card-body textarea {
    font-size: 15px;
    min-height: 80px;
  }

  .control-group {
    flex-direction: column;
  }

  .empty-icon {
    font-size: 60px;
  }

  .empty-text {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .controls {
    max-width: 90%;
  }

  .card-header {
    padding: 10px 12px;
  }

  .window-controls .control {
    width: 12px;
    height: 12px;
  }
}
</style>
