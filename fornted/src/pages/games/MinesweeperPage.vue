<template>
  <div class="minesweeper-container">
    <div class="animated-background">
      <div class="gradient-circle"></div>
      <div class="gradient-circle"></div>
      <div class="gradient-circle"></div>
    </div>

    <div class="game-header">
      <h1>扫雷</h1>
      <div class="game-stats">
        <span class="mines-left">剩余地雷: {{ minesLeft }}</span>
        <span class="time">用时: {{ formatTime(gameState.time) }}</span>
      </div>
    </div>

    <div class="difficulty-controls">
      <button
        v-for="level in difficultyLevels"
        :key="level.name"
        :class="['difficulty-btn', { active: currentDifficulty === level.name }]"
        @click="changeDifficulty(level)"
      >
        {{ level.label }}
      </button>
    </div>

    <div class="game-board" :class="{ 'game-over': gameState.isGameOver }">
      <div class="board-grid" :style="{
        'grid-template-columns': `repeat(${difficultyLevels.find(l => l.name === currentDifficulty)?.cols}, 1fr)`,
        '--cols': difficultyLevels.find(l => l.name === currentDifficulty)?.cols
      }">
        <div
          v-for="(row, rowIndex) in board"
          :key="rowIndex"
          class="board-row"
        >
          <div
            v-for="(cell, colIndex) in row"
            :key="colIndex"
            class="cell"
            :class="{
              'revealed': cell.isRevealed,
              'mine': cell.isMine && cell.isRevealed,
              'flagged': cell.isFlagged,
              'wrong-flag': cell.isFlagged && !cell.isMine && gameState.isGameOver,
              'exploded': cell.isExploded
            }"
            @click="revealCell(rowIndex, colIndex)"
            @contextmenu.prevent="toggleFlag(rowIndex, colIndex)"
          >
            <template v-if="cell.isRevealed && !cell.isMine">
              <span v-if="cell.neighborMines > 0" :class="'number-' + cell.neighborMines">
                {{ cell.neighborMines }}
              </span>
            </template>
            <template v-else-if="cell.isFlagged">
              <span class="flag">🚩</span>
            </template>
            <template v-else-if="cell.isRevealed && cell.isMine">
              <span class="mine">💣</span>
            </template>
          </div>
        </div>
      </div>
    </div>

    <div class="game-controls">
      <button class="control-btn" @click="restartGame">
        <ReloadOutlined />
        重新开始
      </button>
      <button class="control-btn" @click="toggleSound">
        <component :is="gameState.isMuted ? 'SoundMutedOutlined' : 'SoundOutlined'" />
        {{ gameState.isMuted ? '开启声音' : '关闭声音' }}
      </button>
    </div>

    <a-modal
      v-model:visible="gameState.isGameOver"
      :closable="false"
      :maskClosable="false"
      class="game-over-modal"
      :footer="null"
      :width="380"
    >
      <div class="modal-content">
        <div class="modal-header">
          <TrophyOutlined v-if="gameState.isWin" class="trophy-icon" />
          <span v-else class="lose-icon">💣</span>
          <h2>{{ gameState.isWin ? '恭喜通关！' : '游戏结束' }}</h2>
        </div>
        <div class="modal-body">
          <p>用时：{{ formatTime(gameState.time) }}</p>
          <p>难度：{{ difficultyLevels.find(l => l.name === currentDifficulty)?.label }}</p>
          <p class="record-text" v-if="isNewRecord">新纪录！之前的最佳时间：{{ formatTime(previousBestTime) }}</p>
        </div>
        <div class="modal-footer">
          <button class="primary-btn" @click="restartGame">再来一局</button>
          <button class="secondary-btn" @click="goBack">返回菜单</button>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:visible="showMobileWarning"
      title="提示"
      :closable="true"
      :maskClosable="true"
      class="mobile-warning-modal"
      :footer="null"
      :width="300"
    >
      <div class="warning-content">
        <p>请在电脑端体验高级难度哦~</p>
        <p>移动端建议使用初级或中级难度获得最佳体验</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ReloadOutlined, SoundOutlined, TrophyOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

interface Cell {
  isMine: boolean
  isRevealed: boolean
  isFlagged: boolean
  neighborMines: number
  isExploded: boolean
}

interface GameState {
  isGameOver: boolean
  isWin: boolean
  time: number
  isMuted: boolean
  bestTimes: Record<string, number>
}

const difficultyLevels = [
  { name: 'easy', label: '初级', rows: 9, cols: 9, mines: 10 },
  { name: 'medium', label: '中级', rows: 12, cols: 12, mines: 40 },
  { name: 'hard', label: '高级', rows: 18, cols: 18, mines: 66 }
]

const router = useRouter()
const board = ref<Cell[][]>([])
const currentDifficulty = ref('easy')
const minesLeft = ref(10)
const timer = ref<number | null>(null)
const isNewRecord = ref(false)
const previousBestTime = ref(0)

const gameState = reactive<GameState>({
  isGameOver: false,
  isWin: false,
  time: 0,
  isMuted: false,
  bestTimes: {}
})

const gridStyle = computed(() => {
  const difficulty = difficultyLevels.find(l => l.name === currentDifficulty.value)
  return {
    'grid-template-columns': `repeat(${difficulty?.cols}, 1fr)`,
    'max-width': `${difficulty?.cols * 30}px`
  }
})

// 添加移动端检测
const isMobile = computed(() => {
  return window.innerWidth <= 768
})

// 添加弹框控制
const showMobileWarning = ref(false)

// 初始化游戏板
const initBoard = (rows: number, cols: number, mines: number) => {
  const newBoard: Cell[][] = Array(rows).fill(null).map(() =>
    Array(cols).fill(null).map(() => ({
      isMine: false,
      isRevealed: false,
      isFlagged: false,
      neighborMines: 0,
      isExploded: false
    }))
  )

  // 随机放置地雷
  let minesPlaced = 0
  while (minesPlaced < mines) {
    const row = Math.floor(Math.random() * rows)
    const col = Math.floor(Math.random() * cols)
    if (!newBoard[row][col].isMine) {
      newBoard[row][col].isMine = true
      minesPlaced++
    }
  }

  // 计算每个格子周围的地雷数
  for (let row = 0; row < rows; row++) {
    for (let col = 0; col < cols; col++) {
      if (!newBoard[row][col].isMine) {
        newBoard[row][col].neighborMines = countNeighborMines(newBoard, row, col)
      }
    }
  }

  return newBoard
}

// 计算周围地雷数
const countNeighborMines = (board: Cell[][], row: number, col: number) => {
  let count = 0
  for (let i = -1; i <= 1; i++) {
    for (let j = -1; j <= 1; j++) {
      const newRow = row + i
      const newCol = col + j
      if (
        newRow >= 0 &&
        newRow < board.length &&
        newCol >= 0 &&
        newCol < board[0].length &&
        board[newRow][newCol].isMine
      ) {
        count++
      }
    }
  }
  return count
}

// 揭开格子
const revealCell = (row: number, col: number) => {
  if (gameState.isGameOver || board.value[row][col].isFlagged) return

  const cell = board.value[row][col]
  if (cell.isRevealed) return

  if (cell.isMine) {
    cell.isExploded = true
    endGame(false)
    playExplosionSound()
    return
  }

  cell.isRevealed = true
  playRevealSound()

  if (cell.neighborMines === 0) {
    // 如果是空格子，递归揭开周围的格子
    for (let i = -1; i <= 1; i++) {
      for (let j = -1; j <= 1; j++) {
        const newRow = row + i
        const newCol = col + j
        if (
          newRow >= 0 &&
          newRow < board.value.length &&
          newCol >= 0 &&
          newCol < board.value[0].length &&
          !board.value[newRow][newCol].isRevealed
        ) {
          revealCell(newRow, newCol)
        }
      }
    }
  }

  checkWin()
}

// 切换旗子
const toggleFlag = (row: number, col: number) => {
  if (gameState.isGameOver || board.value[row][col].isRevealed) return

  const cell = board.value[row][col]
  cell.isFlagged = !cell.isFlagged
  minesLeft.value += cell.isFlagged ? -1 : 1
  playFlagSound()

  checkWin()
}

// 检查是否获胜
const checkWin = () => {
  const difficulty = difficultyLevels.find(l => l.name === currentDifficulty.value)!
  let correctFlags = 0
  let revealedCount = 0

  for (let row = 0; row < board.value.length; row++) {
    for (let col = 0; col < board.value[0].length; col++) {
      const cell = board.value[row][col]
      if (cell.isFlagged && cell.isMine) correctFlags++
      if (cell.isRevealed && !cell.isMine) revealedCount++
    }
  }

  if (
    correctFlags === difficulty.mines &&
    revealedCount === difficulty.rows * difficulty.cols - difficulty.mines
  ) {
    endGame(true)
  }
}

// 结束游戏
const endGame = (isWin: boolean) => {
  gameState.isGameOver = true
  gameState.isWin = isWin

  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }

  // 显示所有地雷
  if (!isWin) {
    for (let row = 0; row < board.value.length; row++) {
      for (let col = 0; col < board.value[0].length; col++) {
        const cell = board.value[row][col]
        if (cell.isMine && !cell.isFlagged) {
          cell.isRevealed = true
        }
      }
    }
  }

  // 检查是否创造新纪录
  const bestTime = gameState.bestTimes[currentDifficulty.value]
  if (isWin && (!bestTime || gameState.time < bestTime)) {
    previousBestTime.value = bestTime || 0
    gameState.bestTimes[currentDifficulty.value] = gameState.time
    localStorage.setItem('minesweeperBestTimes', JSON.stringify(gameState.bestTimes))
    isNewRecord.value = true
  }

  if (isWin) {
    playWinSound()
  }
}

// 重新开始游戏
const restartGame = () => {
  const difficulty = difficultyLevels.find(l => l.name === currentDifficulty.value)!
  board.value = initBoard(difficulty.rows, difficulty.cols, difficulty.mines)
  minesLeft.value = difficulty.mines
  gameState.isGameOver = false
  gameState.isWin = false
  gameState.time = 0
  isNewRecord.value = false

  if (timer.value) {
    clearInterval(timer.value)
  }
  startTimer()
}

// 修改changeDifficulty函数
const changeDifficulty = (level: typeof difficultyLevels[0]) => {
  if (isMobile.value && level.name === 'hard') {
    showMobileWarning.value = true
    return
  }
  currentDifficulty.value = level.name
  restartGame()
}

// 开始计时器
const startTimer = () => {
  timer.value = window.setInterval(() => {
    gameState.time++
  }, 1000)
}

// 格式化时间
const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 返回菜单
const goBack = () => {
  router.push('/games')
}

// 音效控制
const toggleSound = () => {
  gameState.isMuted = !gameState.isMuted
  message.success(gameState.isMuted ? '已关闭声音' : '已开启声音')
}

// 音效函数
const playRevealSound = () => {
  if (!gameState.isMuted) {
    const audio = new Audio('/sounds/powerup.MP3')
    audio.play().catch(() => {})
  }
}

const playFlagSound = () => {
  if (!gameState.isMuted) {
    const audio = new Audio('/sounds/move.MP3')
    audio.play().catch(() => {})
  }
}

const playExplosionSound = () => {
  if (!gameState.isMuted) {
    const audio = new Audio('/sounds/coin.MP3')
    audio.play().catch(() => {})
  }
}

const playWinSound = () => {
  if (!gameState.isMuted) {
    const audio = new Audio('/sounds/win.MP3')
    audio.play().catch(() => {})
  }
}

// 生命周期钩子
onMounted(() => {
  // 从localStorage加载最佳时间
  const savedBestTimes = localStorage.getItem('minesweeperBestTimes')
  if (savedBestTimes) {
    gameState.bestTimes = JSON.parse(savedBestTimes)
  }
  restartGame()
})

onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value)
  }
})
</script>

<style>
.minesweeper-container {
  min-height: 84vh;
  width: 100%;
  padding: 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  overflow: hidden;
  background: #1a1a2e;
}

.animated-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.gradient-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.4;
  animation: float 8s infinite ease-in-out;
}

.gradient-circle:nth-child(1) {
  width: 300px;
  height: 300px;
  background: linear-gradient(45deg, #4CAF50, #8BC34A);
  top: -50px;
  left: -50px;
  animation-delay: 0s;
}

.gradient-circle:nth-child(2) {
  width: 400px;
  height: 400px;
  background: linear-gradient(45deg, #2196F3, #03A9F4);
  bottom: -100px;
  right: -100px;
  animation-delay: -2s;
}

.gradient-circle:nth-child(3) {
  width: 250px;
  height: 250px;
  background: linear-gradient(45deg, #9C27B0, #E91E63);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -4s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(20px, 20px);
  }
}

.game-header {
  width: 100%;
  text-align: center;
  margin-bottom: 15px;
  color: #fff;
}

.game-header h1 {
  font-size: 2.5rem;
  margin-bottom: 15px;
  background: linear-gradient(45deg, #4CAF50, #8BC34A);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.game-stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 10px;
  color: #a8e6cf;
  font-size: 1.1rem;
}

.difficulty-controls {
  display: flex;
  gap: 10px;
  margin: 15px 0;
  justify-content: center;
  width: 100%;
  padding: 0 10px;
}

.difficulty-btn {
  flex: 1;
  max-width: 120px;
  padding: 10px 20px;
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 1rem;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.difficulty-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(45deg, rgba(76, 175, 80, 0.2), rgba(139, 195, 74, 0.2));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.difficulty-btn:active {
  transform: scale(0.95);
}

.difficulty-btn.active {
  background: rgba(76, 175, 80, 0.3);
  transform: scale(1.05);
}

.difficulty-btn.active::before {
  opacity: 1;
}

.game-board {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  padding: 15px;
  backdrop-filter: blur(10px);
  margin: 0 auto;
  width: fit-content;
  max-width: 95vw;
  overflow: visible;
}

.board-grid {
  display: grid;
  gap: 2px;
  background: rgba(0, 0, 0, 0.2);
  padding: 2px;
  border-radius: 8px;
  margin: 0 auto;
}

.cell {
  aspect-ratio: 1;
  width: 30px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
  font-size: 14px;
  user-select: none;
  -webkit-user-select: none;
  touch-action: manipulation;
}

.cell:active {
  transform: scale(0.95);
  background: rgba(255, 255, 255, 1);
}

.cell:hover:not(.revealed) {
  background: rgba(255, 255, 255, 1);
  transform: scale(0.95);
}

.cell.revealed {
  background: rgba(255, 255, 255, 0.7);
}

.cell.mine {
  background: rgba(244, 67, 54, 0.9);
}

.cell.exploded {
  background: #f44336;
  animation: explode 0.5s ease-out;
}

.cell.flagged {
  background: rgba(255, 255, 255, 0.8);
}

.cell.wrong-flag {
  background: rgba(244, 67, 54, 0.7);
}

@keyframes explode {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.number-1 { color: #2196F3; }
.number-2 { color: #4CAF50; }
.number-3 { color: #F44336; }
.number-4 { color: #673AB7; }
.number-5 { color: #795548; }
.number-6 { color: #009688; }
.number-7 { color: #000000; }
.number-8 { color: #607D8B; }

.game-controls {
  margin-top: 15px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

.control-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  font-size: 1rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.game-over-modal {
  display: flex;
  align-items: center;
  justify-content: center;
}

.game-over-modal .ant-modal-content {
  background: rgba(26, 26, 46, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-content {
  color: #fff;
  text-align: center;
  padding: 20px;
}

.modal-header {
  margin-bottom: 20px;
}

.trophy-icon {
  font-size: 3rem;
  color: #ffd700;
  margin-bottom: 15px;
  animation: bounce 1s infinite;
}

.lose-icon {
  font-size: 3rem;
  margin-bottom: 15px;
  display: inline-block;
  animation: shake 0.5s ease-in-out;
}

.modal-header h2 {
  font-size: 1.8rem;
  background: linear-gradient(45deg, #4CAF50, #8BC34A);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
}

.modal-body {
  margin: 20px 0;
}

.modal-body p {
  margin: 10px 0;
  font-size: 1.1rem;
  color: #a8e6cf;
}

.record-text {
  color: #ffd700 !important;
  font-weight: bold;
}

.modal-footer {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin-top: 25px;
}

.primary-btn,
.secondary-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 30px;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
}

.primary-btn {
  background: linear-gradient(45deg, #4CAF50, #8BC34A);
  color: #fff;
}

.secondary-btn {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.primary-btn:hover,
.secondary-btn:hover {
  transform: translateY(-2px);
  filter: brightness(1.1);
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-5px);
  }
  75% {
    transform: translateX(5px);
  }
}

@media (max-width: 768px) {
  .minesweeper-container {
    padding: 10px 5px;
    min-height: calc(96vh - 60px);
  }

  .game-header h1 {
    font-size: 1.8rem;
    margin-bottom: 10px;
  }

  .game-stats {
    font-size: 0.9rem;
    margin-bottom: 10px;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
  }

  .difficulty-controls {
    margin: 10px 0;
    padding: 0 5px;
    gap: 8px;
  }

  .difficulty-btn {
    padding: 8px 12px;
    font-size: 0.9rem;
    max-width: 100px;
    min-width: 80px;
  }

  .game-board {
    padding: 10px;
    max-width: 98vw;
  }

  .board-grid {
    gap: 1px;
    padding: 1px;
  }

  .cell {
    width: calc(min(28px, (98vw - 40px) / var(--cols)));
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .minesweeper-container {
    padding: 5px;
  }

  .game-header h1 {
    font-size: 1.5rem;
  }

  .game-stats {
    font-size: 0.85rem;
    gap: 8px;
  }

  .difficulty-controls {
    gap: 6px;
    margin: 8px 0;
  }

  .difficulty-btn {
    padding: 6px 10px;
    font-size: 0.85rem;
    max-width: 90px;
    min-width: 70px;
  }

  .game-board {
    padding: 8px;
  }

  .cell {
    width: calc(min(24px, (98vw - 36px) / var(--cols)));
    font-size: 11px;
  }
}

@media (max-width: 360px) {
  .minesweeper-container {
    padding: 3px;
  }

  .game-header h1 {
    font-size: 1.3rem;
  }

  .difficulty-btn {
    padding: 5px 8px;
    font-size: 0.8rem;
    max-width: 80px;
    min-width: 60px;
  }

  .game-board {
    padding: 5px;
  }

  .cell {
    width: calc(min(20px, (98vw - 30px) / var(--cols)));
    font-size: 10px;
  }

  .control-btn {
    padding: 6px 12px;
    font-size: 0.8rem;
  }
}

@media (max-height: 480px) and (orientation: landscape) {
  .minesweeper-container {
    padding: 5px;
  }

  .game-header {
    margin-bottom: 5px;
  }

  .game-header h1 {
    font-size: 1.2rem;
    margin-bottom: 5px;
  }

  .game-stats {
    font-size: 0.8rem;
    margin: 5px 0;
  }

  .difficulty-controls {
    margin: 5px 0;
  }

  .difficulty-btn {
    padding: 4px 8px;
    font-size: 0.8rem;
    max-width: 100px;
  }

  .game-board {
    padding: 5px;
  }

  .cell {
    width: calc(min(22px, (98vh - 120px) / var(--cols)));
    font-size: 10px;
  }

  .control-btn {
    padding: 4px 8px;
    font-size: 0.8rem;
  }
}

.mobile-warning-modal .ant-modal-content {
  background: rgba(26, 26, 46, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.warning-content {
  color: #fff;
  text-align: center;
  padding: 20px 0;
}

.warning-content p {
  margin: 10px 0;
  font-size: 1.1rem;
  color: #a8e6cf;
}

.warning-content p:first-child {
  color: #ffd700;
  font-weight: bold;
}
</style>
