<template>
  <div class="sliding-puzzle-container">
    <div class="game-header">
      <h1>数字华容道</h1>
      <div class="game-controls">
        <a-button type="primary" @click="startNewGame" class="control-btn">
          <template #icon><ReloadOutlined /></template>
          <span class="button-text">新游戏</span>
        </a-button>
        <a-button @click="toggleShowGuide" class="control-btn">
          <template #icon><QuestionCircleOutlined /></template>
          <span class="button-text">玩法说明</span>
        </a-button>
        <a-button @click="toggleSound" class="control-btn">
          <template #icon>
            <SoundFilled v-if="isSoundEnabled" />
            <SoundOutlined v-else />
          </template>
          <span class="button-text">{{ isSoundEnabled ? '关闭音效' : '开启音效' }}</span>
        </a-button>
      </div>
    </div>

    <div class="game-board">
      <div class="puzzle-grid" :class="{ 'game-won': isGameWon }">
        <div
          v-for="(number, index) in board"
          :key="index"
          class="puzzle-tile"
          :class="{
            'empty': number === 0,
            'can-move': canMoveTile(index)
          }"
          :style="getTileStyle(index)"
          @click="moveTile(index)"
        >
          <span v-if="number !== 0">{{ number }}</span>
        </div>
      </div>
    </div>

    <div class="game-info">
      <div class="stats">
        <div class="stats-item">
          <ClockCircleOutlined class="stats-icon" />
          <span>移动次数: {{ moves }}</span>
        </div>
        <div class="stats-item">
          <TrophyOutlined class="stats-icon" />
          <span>最佳记录: {{ bestScore }}</span>
        </div>
      </div>
    </div>

    <a-modal
      v-model:visible="showGuide"
      title="玩法说明"
      @ok="toggleShowGuide"
      :footer="null"
      class="guide-modal"
    >
      <div class="guide-content">
        <h3>游戏规则：</h3>
        <ol>
          <li>游戏开始时，数字1-15会随机打乱排列，还有一个空格。</li>
          <li>点击空格周围的数字可以将其移动到空格位置。</li>
          <li>你的目标是将数字按1-15的顺序排列，空格在最后。</li>
          <li>尽可能用最少的移动次数完成游戏。</li>
        </ol>
        <div class="guide-example">
          <p>目标排列：</p>
          <div class="example-grid">
            <div v-for="n in 16" :key="n" class="example-tile">
              {{ n === 16 ? '' : n }}
            </div>
          </div>
        </div>
      </div>
    </a-modal>

    <a-modal
      v-model:visible="showWinModal"
      title="恭喜通关！"
      @ok="startNewGame"
      :footer="[
        { text: '再来一局', type: 'primary', onClick: startNewGame },
      ]"
      class="win-modal"
    >
      <div class="win-content">
        <h3>太棒了！你完成了拼图！</h3>
        <p>移动次数：{{ moves }}</p>
        <p v-if="isNewRecord">新纪录！之前的最佳记录是：{{ previousBest }}</p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  QuestionCircleOutlined,
  ClockCircleOutlined,
  TrophyOutlined,
  SoundOutlined,
  SoundFilled
} from '@ant-design/icons-vue'

// 游戏状态
const board = ref<number[]>([])
const moves = ref(0)
const bestScore = ref(Infinity)
const previousBest = ref(Infinity)
const showGuide = ref(false)
const showWinModal = ref(false)
const isNewRecord = ref(false)

// 音效管理
const sounds = {
  move: new Audio('/sounds/move.MP3'),
  start: new Audio('/sounds/start.MP3'),
  success: new Audio('/sounds/gameover.MP3')
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

// 计算属性
const isGameWon = computed(() => {
  return board.value.every((num, index) => {
    if (index === board.value.length - 1) return num === 0
    return num === index + 1
  })
})

// 获取空格位置
const getEmptyIndex = () => board.value.indexOf(0)

// 检查是否可以移动
function canMoveTile(index: number): boolean {
  const emptyIndex = getEmptyIndex()
  const row = Math.floor(index / 4)
  const col = index % 4
  const emptyRow = Math.floor(emptyIndex / 4)
  const emptyCol = emptyIndex % 4

  return (
    (Math.abs(row - emptyRow) === 1 && col === emptyCol) ||
    (Math.abs(col - emptyCol) === 1 && row === emptyRow)
  )
}

// 移动方块
function moveTile(index: number) {
  if (!canMoveTile(index)) return

  const emptyIndex = getEmptyIndex()
  const newBoard = [...board.value]
  ;[newBoard[index], newBoard[emptyIndex]] = [newBoard[emptyIndex], newBoard[index]]
  board.value = newBoard
  moves.value++
  playSound('move')

  if (isGameWon.value) {
    handleWin()
  }
}

// 获取方块样式
function getTileStyle(index: number) {
  const number = board.value[index]
  if (number === 0) return {}

  const originalRow = Math.floor((number - 1) / 4)
  const originalCol = (number - 1) % 4
  const currentRow = Math.floor(index / 4)
  const currentCol = index % 4

  return {
    '--x': `${(currentCol - originalCol) * 100}%`,
    '--y': `${(currentRow - originalRow) * 100}%`,
  }
}

// 打乱方块
function shuffleBoard() {
  let numbers = Array.from({ length: 16 }, (_, i) => i)
  do {
    for (let i = numbers.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [numbers[i], numbers[j]] = [numbers[j], numbers[i]]
    }
  } while (!isSolvable(numbers))

  board.value = numbers
}

// 检查是否可解
function isSolvable(numbers: number[]): boolean {
  let inversions = 0
  const emptyRowFromBottom = Math.floor((15 - numbers.indexOf(0)) / 4)

  for (let i = 0; i < numbers.length - 1; i++) {
    if (numbers[i] === 0) continue
    for (let j = i + 1; j < numbers.length; j++) {
      if (numbers[j] === 0) continue
      if (numbers[i] > numbers[j]) inversions++
    }
  }

  return (inversions + emptyRowFromBottom) % 2 === 0
}

// 开始新游戏
function startNewGame() {
  moves.value = 0
  shuffleBoard()
  showWinModal.value = false
  playSound('start')
}

// 处理胜利
function handleWin() {
  playSound('success')
  if (moves.value < bestScore.value) {
    previousBest.value = bestScore.value
    bestScore.value = moves.value
    isNewRecord.value = true
  } else {
    isNewRecord.value = false
  }
  showWinModal.value = true
}

// 切换显示指南
function toggleShowGuide() {
  showGuide.value = !showGuide.value
}

// 初始化
onMounted(() => {
  const savedBestScore = localStorage.getItem('sliding-puzzle-best-score')
  if (savedBestScore) {
    bestScore.value = parseInt(savedBestScore)
  }
  startNewGame()
})
</script>

<style scoped>
.sliding-puzzle-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: calc(86vh - 64px);
  background: linear-gradient(135deg, #0091ff, #00a6ff);
  position: relative;
}

.game-header {
  text-align: center;
  margin-bottom: 20px;
  width: 100%;
}

.game-header h1 {
  font-size: 28px;
  font-weight: 600;
  color: white;
  margin-bottom: 16px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.game-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.control-btn {
  min-width: 100px;
  height: 36px;
  border-radius: 18px;
  background: white !important;
  color: #0091ff !important;
  border: none !important;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.control-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.game-board {
  width: 100%;
  max-width: 440px;
  aspect-ratio: 1;
  margin: 20px auto;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.puzzle-grid {
  width: 100%;
  aspect-ratio: 1;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.puzzle-tile {
  position: relative;
  background: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  aspect-ratio: 1;
  width: 100%;
  height: 100%;
}

.puzzle-tile.empty {
  background: transparent;
  box-shadow: none;
}

.puzzle-tile.can-move {
  background: rgba(255, 255, 255, 0.9);
  transform: scale(0.95);
}

.puzzle-tile.can-move:hover {
  transform: scale(1);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.game-won .puzzle-tile:not(.empty) {
  background: #f6ffed;
  color: #52c41a;
  animation: tileWin 0.5s ease forwards;
}

.stats {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  margin-top: auto;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  font-size: 14px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.stats-icon {
  font-size: 18px;
}

.guide-content {
  padding: 16px;
}

.guide-example {
  margin-top: 16px;
  text-align: center;
}

.example-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  max-width: 200px;
  margin: 16px auto;
  padding: 4px;
  background: #f0f0f0;
  border-radius: 8px;
}

.example-tile {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
}

.win-content {
  text-align: center;
}

@keyframes gradientBG {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes tileWin {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.button-text {
  display: inline;
}

@media (max-width: 480px) {
  .sliding-puzzle-container {
    margin: -24px;
    height: 94vh ;
  }

  .game-header h1 {
    font-size: 24px;
    margin-bottom: 12px;
  }

  .game-board {
    padding: 10px;
    margin: 10px auto;
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

  .puzzle-grid {
    gap: 6px;
    padding: 6px;
  }

  .puzzle-tile {
    font-size: 20px;
  }

  .stats {
    padding: 10px 16px;
    gap: 16px;
    margin-top: 10px;
  }
}
</style>
