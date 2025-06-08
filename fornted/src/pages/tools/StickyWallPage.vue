<template>
  <div class="sticky-wall">
    <div class="controls">
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

    <div class="notes-container">
      <div
        v-for="note in notes"
        :key="note.id"
        class="note"
        :style="{
          backgroundColor: note.color,
          transform: `rotate(${note.rotation}deg)`,
          left: note.x + 'px',
          top: note.y + 'px'
        }"
        @mousedown="startDrag($event, note)"
        @touchstart="startDrag($event, note)"
      >
        <div class="note-header">
          <div class="color-picker">
            <div
              v-for="color in colors"
              :key="color"
              class="color-option"
              :style="{ backgroundColor: color }"
              @click="changeNoteColor(note, color)"
            ></div>
          </div>
          <a-button
            type="text"
            class="delete-btn"
            @click="deleteNote(note.id)"
          >
            <DeleteOutlined />
          </a-button>
        </div>
        <textarea
          v-model="note.content"
          placeholder="写点什么..."
          @input="saveNotes"
          @touchstart.stop
        ></textarea>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

interface Note {
  id: number
  content: string
  color: string
  x: number
  y: number
  rotation: number
}

const notes = ref<Note[]>([])
const colors = ['#feff9c', '#7afcff', '#ff7eb9', '#fff740', '#98ff98']
let draggedNote: Note | null = null
let initialX = 0
let initialY = 0
let offsetX = 0
let offsetY = 0

const addNote = () => {
  // 获取容器元素
  const container = document.querySelector('.notes-container')
  if (!container) return

  // 使用容器的实际尺寸
  const containerWidth = container.clientWidth - 280 // 减去便签宽度
  const containerHeight = container.clientHeight - 280 // 减去便签高度
  const scrollTop = container.scrollTop

  const newNote: Note = {
    id: Date.now(),
    content: '',
    color: colors[Math.floor(Math.random() * colors.length)],
    x: Math.random() * containerWidth + 20, // 添加边距
    y: Math.random() * containerHeight + 160 + scrollTop, // 考虑滚动位置
    rotation: Math.random() * 6 - 3
  }
  notes.value.push(newNote)
  saveNotes()
}

const deleteNote = (id: number) => {
  notes.value = notes.value.filter(note => note.id !== id)
  saveNotes()
  message.success('便签删除成功')
}

const clearAll = () => {
  if (notes.value.length === 0) {
    message.warning('当前没有便签可清空')
    return
  }
  notes.value = []
  saveNotes()
  message.success('已清空所有便签')
}

const changeNoteColor = (note: Note, color: string) => {
  note.color = color
  saveNotes()
}

const startDrag = (event: MouseEvent | TouchEvent, note: Note) => {
  if ((event.target as HTMLElement).tagName === 'TEXTAREA') return

  draggedNote = note
  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  const clientY = 'touches' in event ? event.touches[0].clientY : event.clientY

  initialX = clientX - note.x
  initialY = clientY - note.y

  document.addEventListener('mousemove', onDrag, { passive: false })
  document.addEventListener('touchmove', onDrag, { passive: false })
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchend', stopDrag)
}

const onDrag = (event: MouseEvent | TouchEvent) => {
  if (!draggedNote) return

  event.preventDefault()
  const container = document.querySelector('.notes-container')
  if (!container) return

  const clientX = 'touches' in event ? event.touches[0].clientX : event.clientX
  const clientY = 'touches' in event ? event.touches[0].clientY : event.clientY

  // 计算相对于容器的位置
  const rect = container.getBoundingClientRect()
  const x = clientX - rect.left - initialX
  const y = clientY - rect.top - initialY + container.scrollTop

  // 限制在容器内
  draggedNote.x = Math.max(0, Math.min(x, container.clientWidth - 280))
  draggedNote.y = Math.max(0, Math.min(y, container.scrollHeight - 280))

  saveNotes()
}

const stopDrag = () => {
  draggedNote = null
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchend', stopDrag)
}

const saveNotes = () => {
  localStorage.setItem('sticky-notes', JSON.stringify(notes.value))
}

const loadNotes = () => {
  const savedNotes = localStorage.getItem('sticky-notes')
  if (savedNotes) {
    notes.value = JSON.parse(savedNotes)
  }
}

onMounted(() => {
  loadNotes()
})

onUnmounted(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchend', stopDrag)
})
</script>

<style scoped>
.sticky-wall {
  height: 89vh;
  margin: -20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  overflow: hidden;
  position: relative;
}

.controls {
  position: fixed;
  top: 100px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  width: 100%;
  max-width: 400px;
  padding: 0 20px;
}

.control-group {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  padding: 16px;
  display: flex;
  gap: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.4);
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
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.control-group :deep(.add-btn) {
  background: linear-gradient(135deg, #34d399 0%, #3b82f6 100%);
  border: none;
  color: white;
}

.control-group :deep(.add-btn:hover) {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(59, 130, 246, 0.3);
}

.control-group :deep(.clear-btn) {
  background: white;
  border: 1px solid #e5e7eb;
  color: #6b7280;
}

.control-group :deep(.clear-btn:hover) {
  color: #ef4444;
  border-color: #ef4444;
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(239, 68, 68, 0.15);
}

.control-group :deep(.anticon) {
  font-size: 18px;
}

.notes-container {
  position: relative;
  height: calc(88vh - 140px);
  margin-top: 160px;
  padding: 20px;
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(156, 163, 175, 0.5) transparent;
}

.notes-container::-webkit-scrollbar {
  width: 6px;
}

.notes-container::-webkit-scrollbar-track {
  background: transparent;
}

.notes-container::-webkit-scrollbar-thumb {
  background-color: rgba(156, 163, 175, 0.5);
  border-radius: 3px;
}

.note {
  position: absolute;
  width: 240px;
  min-height: 240px;
  padding: 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  cursor: move;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.4);
}

.note:hover {
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px) scale(1.02);
  z-index: 2;
}

.note-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.color-picker {
  display: flex;
  gap: 8px;
}

.color-option {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.color-option:hover {
  transform: scale(1.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.delete-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.1);
  border: none;
  color: #ef4444;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.delete-btn:hover {
  background: #ef4444;
  color: white;
  transform: rotate(90deg);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.2);
}

.note textarea {
  width: 100%;
  height: calc(100% - 60px);
  min-height: 140px;
  padding: 12px;
  border: none;
  background: transparent;
  resize: none;
  font-family: inherit;
  font-size: 15px;
  line-height: 1.6;
  color: #374151;
}

.note textarea::placeholder {
  color: #9ca3af;
}

.note textarea:focus {
  outline: none;
}

@media (max-width: 768px) {
  .sticky-wall {
    height: 94vh;
  }
  .controls {
    top: calc(env(safe-area-inset-top) + 20px);
    padding: 0 16px;
    max-width: 160px;
  }

  .notes-container {
    margin-top: 100px;
  }

  .control-group {
    padding: 12px;
    gap: 12px;
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(12px);
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.6);
  }

  .control-group :deep(.ant-btn) {
    width: 48px;
    height: 48px;
    padding: 0;
    border-radius: 16px;
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .control-group :deep(.ant-btn span:not(.anticon)) {
    display: none;
  }

  .control-group :deep(.anticon) {
    font-size: 24px;
    margin: 0;
  }

  .control-group :deep(.add-btn) {
    background: linear-gradient(135deg, #34d399 0%, #3b82f6 100%);
    border: none;
    box-shadow: 0 4px 12px rgba(52, 211, 153, 0.2);
  }

  .control-group :deep(.add-btn:hover),
  .control-group :deep(.add-btn:active) {
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(59, 130, 246, 0.3);
  }

  .control-group :deep(.clear-btn) {
    background: white;
    border: none;
    color: #ef4444;
    box-shadow: 0 4px 12px rgba(239, 68, 68, 0.1);
  }

  .control-group :deep(.clear-btn:hover),
  .control-group :deep(.clear-btn:active) {
    background: #ef4444;
    color: white;
    transform: translateY(-2px);
    box-shadow: 0 8px 16px rgba(239, 68, 68, 0.2);
  }

  .note {
    width: calc(100% - 40px);
    max-width: 300px;
    min-height: 200px;
    padding: 16px;
    touch-action: none;
  }

  .note textarea {
    font-size: 14px;
    -webkit-appearance: none;
    border-radius: 0;
    padding: 8px;
  }

  .color-option {
    width: 20px;
    height: 20px;
  }

  .delete-btn {
    width: 28px;
    height: 28px;
  }
}
</style>
