<template>
  <div class="queens-game-container">
    <div class="game-header">
      <h1>八皇后游戏</h1>
      <div class="game-controls">
        <a-button type="primary" @click="startNewGame" class="control-btn">
          <template #icon><ReloadOutlined /></template>
          <span class="button-text">新游戏</span>
        </a-button>
        <a-button @click="autoSolve" :loading="solving" class="control-btn">
          <template #icon><ThunderboltOutlined /></template>
          <span class="button-text">自动求解</span>
        </a-button>
        <a-button @click="toggleSound" class="control-btn">
          <template #icon>{{ isSoundEnabled ? '🔊' : '🔇' }}</template>
          <span class="button-text">{{ isSoundEnabled ? '关闭音效' : '开启音效' }}</span>
        </a-button>
      </div>
    </div>

    <div class="game-board">
      <div class="chess-board">
        <div v-for="(row, i) in board" :key="i" class="board-row">
          <div
            v-for="(cell, j) in row"
            :key="j"
            class="board-cell"
            :class="{
              'cell-dark': (i + j) % 2 === 1,
              'cell-light': (i + j) % 2 === 0,
              'has-queen': cell === 1,
              'cell-hover': !cell
            }"
            @click="placeQueen(i, j)"
          >
            <div v-if="cell === 1" class="queen-piece">♕</div>
          </div>
        </div>
      </div>
    </div>

    <div class="game-info">
      <a-alert
        v-if="message"
        :type="messageType"
        :message="message"
        show-icon
        class="game-message"
      ></a-alert>
      <div class="stats">
        <div class="stats-content">
          <CrownOutlined class="stats-icon" />
          <span>已放置皇后: {{ placedQueens }}/8</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Ref } from 'vue'
import { message } from 'ant-design-vue'
import type { AlertProps } from 'ant-design-vue'
import { ReloadOutlined, ThunderboltOutlined, CrownOutlined } from '@ant-design/icons-vue'

// 音效管理
const sounds = {
  move: new Audio('/sounds/move.MP3'),
  clear: new Audio('/sounds/clear.MP3'),
  start: new Audio('/sounds/start.MP3'),
  success: new Audio('/sounds/gameover.MP3'),
  error: new Audio('/sounds/drop.MP3')
}

// 音效开关
const isSoundEnabled = ref(true)

// 播放音效
const playSound = (soundName: keyof typeof sounds) => {
  if (isSoundEnabled.value) {
    sounds[soundName].currentTime = 0
    sounds[soundName].play().catch(() => {})
  }
}

// 切换音效
const toggleSound = () => {
  isSoundEnabled.value = !isSoundEnabled.value
}

type Board = (0 | 1)[][]

const board: Ref<Board> = ref(Array.from({ length: 8 }, () => Array(8).fill(0)))
const solving = ref(false)
const message = ref('')
const messageType = ref<AlertProps['type']>('info')

const placedQueens = computed(() => {
  return board.value.flat().filter(cell => cell === 1).length
})

function isValidPosition(row: number, col: number): boolean {
  // Check row
  if (board.value[row].includes(1)) return false

  // Check column
  if (board.value.some(r => r[col] === 1)) return false

  // Check diagonals
  for (let i = 0; i < 8; i++) {
    for (let j = 0; j < 8; j++) {
      if (board.value[i][j] === 1) {
        if (Math.abs(row - i) === Math.abs(col - j)) return false
      }
    }
  }

  return true
}

function placeQueen(row: number, col: number): void {
  if (board.value[row][col] === 1) {
    board.value[row][col] = 0
    message.value = ''
    playSound('clear')
    return
  }

  if (placedQueens.value >= 8) {
    message.value = '已经放置了8个皇后'
    messageType.value = 'warning'
    playSound('error')
    return
  }

  if (isValidPosition(row, col)) {
    board.value[row][col] = 1
    message.value = ''
    playSound('move')

    if (placedQueens.value === 8) {
      message.value = '恭喜！你成功解决了八皇后问题！'
      messageType.value = 'success'
      playSound('success')
    }
  } else {
    message.value = '此位置会造成冲突'
    messageType.value = 'error'
    playSound('error')
  }
}

function startNewGame(): void {
  board.value = Array.from({ length: 8 }, () => Array(8).fill(0))
  message.value = ''
  messageType.value = 'info'
  playSound('start')
}

async function autoSolve(): Promise<void> {
  solving.value = true
  startNewGame()

  try {
    const solution = await solveQueens()
    if (solution) {
      board.value = solution
      message.value = '自动求解完成！'
      messageType.value = 'success'
      playSound('success')
    } else {
      message.value = '未找到解决方案'
      messageType.value = 'error'
      playSound('error')
    }
  } catch (error) {
    message.value = '求解过程出错'
    messageType.value = 'error'
    playSound('error')
  } finally {
    solving.value = false
  }
}

function solveQueens(row = 0): Board | false {
  if (row >= 8) return board.value

  for (let col = 0; col < 8; col++) {
    if (isValidPosition(row, col)) {
      board.value[row][col] = 1
      if (solveQueens(row + 1)) return board.value
      board.value[row][col] = 0
    }
  }

  return false
}
</script>

<style scoped>
.queens-game-container {
  max-width: 800px;
  margin: 24px auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: calc(86vh - 48px);
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
  position: relative;
  overflow: hidden;
}

/* Add page background styles */
.queens-game-container::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #1a2a6c, #b21f1f, #fdbb2d);
  background-size: 400% 400%;
  animation: gradientBG 15s ease infinite;
  z-index: -1;
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

.game-header {
  text-align: center;
  margin-bottom: 0;
  flex-shrink: 0;
}

.game-header h1 {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #2c3e50, #3498db);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.game-controls {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.control-btn {
  min-width: 120px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 500;
  border-radius: 20px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.button-text {
  display: inline;
}

.control-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.game-board {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  min-height: 0;
}

.chess-board {
  width: min(100%, min(70vh, 600px));
  height: min(100%, min(70vh, 600px));
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  border: 4px solid #2c3e50;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  background: #fff;
}

.board-row {
  display: flex;
  flex: 1;
}

.board-cell {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.cell-light {
  background-color: #f0d9b5;
}

.cell-dark {
  background-color: #b58863;
}

.cell-hover:hover {
  background-color: rgba(24, 144, 255, 0.2);
}

.cell-hover:hover::after {
  content: '♕';
  position: absolute;
  font-size: min(8vw, 40px);
  color: rgba(24, 144, 255, 0.3);
  animation: fadeIn 0.3s ease;
}

.queen-piece {
  font-size: min(8vw, 40px);
  color: #2c3e50;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  animation: queenPlaced 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  transform-origin: center center;
}

.game-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 20px;
}

.game-message {
  text-align: center;
  border-radius: 12px;
  animation: slideIn 0.3s ease;
}

.stats {
  text-align: center;
  background: rgba(255, 255, 255, 0.9);
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.stats-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  color: #2c3e50;
  font-weight: 500;
}

.stats-icon {
  font-size: 24px;
  color: #3498db;
}

@keyframes queenPlaced {
  0% {
    transform: scale(0) rotate(-180deg);
    opacity: 0;
  }
  100% {
    transform: scale(1) rotate(0);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    transform: translateY(-10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Update mobile styles */
@media (max-width: 480px) {
  .queens-game-container {
    margin: -24px;
    height: 94vh;
    border-radius: 0;
    padding: 16px;
  }

  .game-header h1 {
    font-size: 24px;
    margin-bottom: 16px;
  }

  .chess-board {
    width: min(100%, min(80vh, 400px));
    height: min(100%, min(80vh, 400px));
  }

  .control-btn {
    min-width: 48px;
    width: 48px;
    height: 48px;
    padding: 0;
    font-size: 24px;
    border-radius: 50%;
  }

  .button-text {
    display: none;
  }

  .stats-content {
    font-size: 14px;
  }

  .stats-icon {
    font-size: 18px;
  }

  .game-info {
    padding: 0;
  }
}
</style>
