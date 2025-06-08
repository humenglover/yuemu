<template>
  <div class="memory-game-container">
    <div class="animated-background">
      <div class="gradient-circle"></div>
      <div class="gradient-circle"></div>
      <div class="gradient-circle"></div>
    </div>

    <div class="game-header">
      <h1>记忆翻牌</h1>
      <div class="game-stats">
        <span class="time">用时: {{ formatTime(gameState.time) }}</span>
        <span class="flips">翻牌次数: {{ gameState.flips }}</span>
      </div>
    </div>

    <div class="memory-board" :class="{ 'game-over': gameState.isGameOver }">
      <div v-for="(card, index) in cards"
           :key="index"
           class="memory-card"
           :class="{ 'flipped': card.isFlipped, 'matched': card.isMatched }"
           @click="!card.isMatched && !card.isFlipped && flipCard(index)">
        <div class="card-inner">
          <div class="card-front">
            <span class="card-icon">{{ card.icon }}</span>
          </div>
          <div class="card-back">
            <div class="card-pattern"></div>
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
          <TrophyOutlined class="trophy-icon" />
          <h2>恭喜通关！</h2>
        </div>
        <div class="modal-body">
          <p>用时：{{ formatTime(gameState.time) }}</p>
          <p>翻牌次数：{{ gameState.flips }}</p>
          <p class="record-text" v-if="isNewRecord">新纪录！之前的最佳时间：{{ formatTime(previousBestTime) }}</p>
        </div>
        <div class="modal-footer">
          <button class="primary-btn" @click="restartGame">再来一局</button>
          <button class="secondary-btn" @click="goBack">返回菜单</button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ReloadOutlined, SoundOutlined, SoundMutedOutlined, TrophyOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

interface Card {
  icon: string
  isFlipped: boolean
  isMatched: boolean
}

interface GameState {
  time: number
  flips: number
  isGameOver: boolean
  isMuted: boolean
  bestTime: number | null
}

const router = useRouter()
const cards = ref<Card[]>([])
const gameState = reactive<GameState>({
  time: 0,
  flips: 0,
  isGameOver: false,
  isMuted: false,
  bestTime: null
})

const timer = ref<number | null>(null)
const flippedCards = ref<number[]>([])
const isNewRecord = ref(false)
const previousBestTime = ref<number>(0)

// 卡片图标
const cardIcons = ['🎮', '🎲', '🎯', '🎨', '🎭', '🎪', '🎫', '🎬', '🎼', '🎹']

// 格式化时间
const formatTime = (seconds: number): string => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 初始化游戏
const initGame = () => {
  const icons = [...cardIcons, ...cardIcons]
  const shuffled = icons.sort(() => Math.random() - 0.5)
  cards.value = shuffled.map(icon => ({
    icon,
    isFlipped: false,
    isMatched: false
  }))

  gameState.time = 0
  gameState.flips = 0
  gameState.isGameOver = false
  flippedCards.value = []

  // 从localStorage获取最佳时间
  const savedBestTime = localStorage.getItem('memoryGameBestTime')
  gameState.bestTime = savedBestTime ? parseInt(savedBestTime) : null
}

// 翻牌逻辑
const flipCard = (index: number) => {
  if (flippedCards.value.length === 2) return

  cards.value[index].isFlipped = true
  flippedCards.value.push(index)
  playFlipSound()

  if (flippedCards.value.length === 2) {
    gameState.flips++
    setTimeout(checkMatch, 800)
  }
}

// 检查匹配
const checkMatch = () => {
  const [first, second] = flippedCards.value
  const match = cards.value[first].icon === cards.value[second].icon

  if (match) {
    cards.value[first].isMatched = true
    cards.value[second].isMatched = true
    playMatchSound()

    if (cards.value.every(card => card.isMatched)) {
      endGame()
    }
  } else {
    cards.value[first].isFlipped = false
    cards.value[second].isFlipped = false
    playMismatchSound()
  }

  flippedCards.value = []
}

// 游戏结束处理
const endGame = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }

  // 检查是否创造新纪录
  if (!gameState.bestTime || gameState.time < gameState.bestTime) {
    previousBestTime.value = gameState.bestTime || 0
    gameState.bestTime = gameState.time
    localStorage.setItem('memoryGameBestTime', gameState.time.toString())
    isNewRecord.value = true
  }

  gameState.isGameOver = true
  playWinSound()
}

// 重新开始游戏
const restartGame = () => {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }

  initGame()
  startTimer()
}

// 开始计时器
const startTimer = () => {
  timer.value = window.setInterval(() => {
    gameState.time++
  }, 1000)
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
const playFlipSound = () => {
  if (!gameState.isMuted) {
    // 添加翻牌音效
    const audio = new Audio('/sounds/flip.MP3')
    audio.play().catch(() => {})
  }
}

const playMatchSound = () => {
  if (!gameState.isMuted) {
    // 添加匹配成功音效
    const audio = new Audio('/sounds/match.MP3')
    audio.play().catch(() => {})
  }
}

const playMismatchSound = () => {
  if (!gameState.isMuted) {
    // 添加匹配失败音效
    const audio = new Audio('/sounds/mismatch.MP3')
    audio.play().catch(() => {})
  }
}

const playWinSound = () => {
  if (!gameState.isMuted) {
    // 添加胜利音效
    const audio = new Audio('/sounds/win.MP3')
    audio.play().catch(() => {})
  }
}

// 生命周期钩子
onMounted(() => {
  initGame()
  startTimer()
})

onUnmounted(() => {
  if (timer.value) {
    clearInterval(timer.value)
  }
})
</script>

<style scoped>
.memory-game-container {
  min-height: 86vh;
  padding: 20px;
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
  background: linear-gradient(45deg, #ff6b6b, #feca57);
  top: -50px;
  left: -50px;
  animation-delay: 0s;
}

.gradient-circle:nth-child(2) {
  width: 400px;
  height: 400px;
  background: linear-gradient(45deg, #4834d4, #686de0);
  bottom: -100px;
  right: -100px;
  animation-delay: -2s;
}

.gradient-circle:nth-child(3) {
  width: 250px;
  height: 250px;
  background: linear-gradient(45deg, #6c5ce7, #a8e6cf);
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
  position: relative;
  z-index: 1;
  text-align: center;
  margin-bottom: 30px;
  color: #fff;
}

.game-header h1 {
  font-size: 2.5rem;
  margin-bottom: 15px;
  background: linear-gradient(45deg, #ff6b6b, #feca57);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.game-stats {
  display: flex;
  gap: 20px;
  font-size: 1.1rem;
  color: #a8e6cf;
}

.memory-board {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  max-width: 600px;
  width: 100%;
  padding: 20px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  margin: auto;
}

.memory-card {
  aspect-ratio: 3/4;
  perspective: 1000px;
  cursor: pointer;
  transition: transform 0.3s ease;
  width: 100%;
  max-height: calc((86vh - 200px) / 4);
}

.memory-card:hover {
  transform: translateY(-5px);
}

.card-inner {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

.memory-card.flipped .card-inner {
  transform: rotateY(180deg);
}

.card-front,
.card-back {
  position: absolute;
  width: 100%;
  height: 100%;
  backface-visibility: hidden;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.card-front {
  background: linear-gradient(135deg, #fff, #f0f0f0);
  transform: rotateY(180deg);
}

.card-back {
  background: linear-gradient(135deg, #4834d4, #686de0);
}

.card-icon {
  font-size: 2rem;
}

.card-pattern {
  width: 60%;
  height: 60%;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  position: relative;
}

.card-pattern::before,
.card-pattern::after {
  content: '';
  position: absolute;
  background: rgba(255, 255, 255, 0.2);
}

.card-pattern::before {
  width: 100%;
  height: 2px;
  top: 50%;
  transform: translateY(-50%);
}

.card-pattern::after {
  width: 2px;
  height: 100%;
  left: 50%;
  transform: translateX(-50%);
}

.game-controls {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 15px;
  margin-top: 30px;
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
  backdrop-filter: blur(5px);
}

.control-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.game-over-modal {
  :deep(.ant-modal-content) {
    background: rgba(26, 26, 46, 0.95);
    backdrop-filter: blur(10px);
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
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

.modal-header h2 {
  font-size: 1.8rem;
  background: linear-gradient(45deg, #ff6b6b, #feca57);
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
  background: linear-gradient(45deg, #ff6b6b, #feca57);
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

@media (max-width: 768px) {
  .memory-game-container {
    padding: 10px;
  }

  .memory-board {
    grid-template-columns: repeat(4, 1fr);
    gap: 6px;
    padding: 12px;
    max-width: 400px;
  }

  .memory-card {
    max-height: calc((86vh - 160px) / 5);
  }

  .game-header {
    margin-bottom: 15px;
  }

  .game-header h1 {
    font-size: 1.8rem;
    margin-bottom: 10px;
  }

  .game-stats {
    font-size: 0.9rem;
    gap: 15px;
  }

  .card-icon {
    font-size: 1.3rem;
  }

  .control-btn {
    padding: 8px 16px;
    font-size: 0.9rem;
  }

  .card-pattern {
    width: 70%;
    height: 70%;
  }
}

@media (max-width: 480px) {
  .memory-game-container {
    padding: 8px;
  }

  .memory-board {
    grid-template-columns: repeat(4, 1fr);
    gap: 4px;
    padding: 8px;
    max-width: 320px;
  }

  .memory-card {
    max-height: calc((86vh - 140px) / 5);
  }

  .game-header {
    margin-bottom: 10px;
  }

  .game-header h1 {
    font-size: 1.5rem;
    margin-bottom: 8px;
  }

  .game-stats {
    font-size: 0.85rem;
    gap: 12px;
  }

  .card-icon {
    font-size: 1.1rem;
  }

  .control-btn {
    padding: 6px 12px;
    font-size: 0.85rem;
    gap: 4px;
  }

  .game-controls {
    margin-top: 15px;
  }
}

@media (max-width: 360px) {
  .memory-game-container {
    padding: 5px;
  }

  .memory-board {
    grid-template-columns: repeat(4, 1fr);
    gap: 3px;
    padding: 6px;
    max-width: 280px;
  }

  .memory-card {
    max-height: calc((86vh - 120px) / 5);
  }

  .game-header {
    margin-bottom: 8px;
  }

  .game-header h1 {
    font-size: 1.3rem;
    margin-bottom: 6px;
  }

  .game-stats {
    font-size: 0.8rem;
    gap: 10px;
  }

  .card-icon {
    font-size: 1rem;
  }

  .card-pattern {
    width: 75%;
    height: 75%;
  }

  .control-btn {
    padding: 5px 10px;
    font-size: 0.8rem;
  }

  .game-controls {
    margin-top: 10px;
  }
}

@media (max-height: 600px) {
  .memory-game-container {
    padding: 5px;
  }

  .game-header {
    margin-bottom: 5px;
  }

  .game-header h1 {
    font-size: 1.2rem;
    margin-bottom: 4px;
  }

  .game-stats {
    font-size: 0.8rem;
    gap: 8px;
  }

  .memory-board {
    padding: 4px;
  }

  .memory-card {
    max-height: calc((86vh - 100px) / 5);
  }

  .game-controls {
    margin-top: 8px;
  }

  .control-btn {
    padding: 4px 8px;
    font-size: 0.8rem;
  }
}
</style>
