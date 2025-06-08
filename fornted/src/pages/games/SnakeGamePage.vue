<template>
  <div class="snake-game-page">
    <!-- 添加背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="game-container">
      <!-- 游戏模式选择 -->
      <div v-if="!gameStarted" class="mode-selection">
        <h2>选择游戏模式</h2>
        <div class="mode-buttons">
          <div
            v-for="mode in gameModes"
            :key="mode.id"
            class="mode-button"
            @click="startGame(mode.id)"
          >
            <div class="mode-icon">{{ mode.icon }}</div>
            <div class="mode-content">
              <div class="mode-name">{{ mode.name }}</div>
              <div class="mode-description">{{ mode.description }}</div>
            </div>
          </div>
        </div>

        <!-- 最高分展示 -->
        <div class="highest-scores">
          <div v-for="mode in gameModes" :key="mode.id" class="score-item">
            <div class="score-label">{{ mode.name }}最高分</div>
            <div class="score-value">{{ highestScores[mode.id] || 0 }}</div>
          </div>
        </div>

        <!-- 排行榜按钮 -->
        <div class="ranking-button" @click="showRankingModal = true">
          查看排行榜
        </div>
      </div>

      <!-- 游戏画布 -->
      <div v-show="gameStarted" class="game-play-area">
        <!-- 顶部控制栏 -->
        <div class="mobile-top-bar">
          <div class="left-controls">
            <div class="control-button back-button" @click="quitGame">
              <arrow-left-outlined />
            </div>
          </div>
          <div class="game-info">
            <div class="info-item">
              <div class="info-label">得分</div>
              <div class="info-value">{{ score }}</div>
            </div>
            <div class="info-divider"></div>
            <div class="info-item">
              <div class="info-label">长度</div>
              <div class="info-value">{{ snakeLength }}</div>
            </div>
            <div class="info-divider"></div>
            <div class="info-item">
              <div class="info-label">时间</div>
              <div class="info-value">{{ formatTime(gameTime) }}</div>
            </div>
          </div>
          <div class="right-controls">
            <div class="control-button" @click="newGame">
              <redo-outlined />
            </div>
            <div class="control-button" @click="pauseGame">
              <play-circle-outlined v-if="isPaused" />
              <pause-circle-outlined v-else />
            </div>
            <div class="control-button" @click="toggleSound">
              <sound-filled v-if="isSoundEnabled" />
              <sound-outlined v-else />
            </div>
          </div>
        </div>

        <canvas
          ref="gameCanvas"
          :width="canvasWidth"
          :height="canvasHeight"
          class="game-canvas"
        ></canvas>

        <!-- 移动端底部控制栏 -->
        <div class="mobile-controls" @touchmove.prevent>
          <div class="mobile-controls-container">
            <div class="direction-controls">
              <div class="direction-row">
                <div class="direction-button up" @touchstart.prevent="handleDirection('up')"></div>
              </div>
              <div class="direction-row middle">
                <div class="direction-button left" @touchstart.prevent="handleDirection('left')"></div>
                <div class="direction-spacer"></div>
                <div class="direction-button right" @touchstart.prevent="handleDirection('right')"></div>
              </div>
              <div class="direction-row">
                <div class="direction-button down" @touchstart.prevent="handleDirection('down')"></div>
              </div>
            </div>
          </div>
        </div>
      </div>


    </div>

    <!-- 排行榜弹窗 -->
    <div v-if="showRankingModal" class="modal-overlay" @click="showRankingModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>排行榜</h3>
          <div class="close-button" @click="showRankingModal = false">×</div>
        </div>
        <div class="modal-tabs">
          <div
            v-for="mode in gameModes"
            :key="mode.id"
            class="tab-item"
            :class="{ active: rankingMode === mode.id }"
            @click="rankingMode = mode.id"
          >
            {{ mode.name }}
          </div>
        </div>
        <div class="ranking-table">
          <div class="ranking-list">
            <div v-for="(record, index) in rankingList" :key="record.id" class="ranking-card">
              <div class="ranking-card-header">
                <div class="ranking-position">{{ index + 1 }}</div>
                <div class="ranking-username">{{ record.userName }}</div>
                <div class="ranking-score">{{ record.score }}分</div>
              </div>
              <div class="ranking-details">
                <div class="ranking-detail-item">
                  <div class="detail-label">长度</div>
                  <div class="detail-value">{{ record.snakeLength }}</div>
                </div>
                <div class="ranking-detail-item">
                  <div class="detail-label">食物数</div>
                  <div class="detail-value">{{ record.foodCount }}</div>
                </div>
                <div class="ranking-detail-item">
                  <div class="detail-label">用时</div>
                  <div class="detail-value">{{ formatTime(record.gameTime) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 游戏结束弹窗 -->
    <div v-if="showGameOverModal" class="modal-overlay" @click.stop>
      <div class="modal-content game-over-modal">
        <h3 class="game-over-title">游戏结束</h3>
        <div class="game-over-stats">
          <div class="stat-item">
            <div class="stat-label">最终得分</div>
            <div class="stat-value highlight">{{ score }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">蛇的长度</div>
            <div class="stat-value">{{ snakeLength }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">游戏时间</div>
            <div class="stat-value">{{ formatTime(gameTime) }}</div>
          </div>
        </div>
        <div class="game-over-buttons">
          <div class="control-button" @click="restartGame">
            <redo-outlined />
          </div>
          <div class="control-button" @click="backToMode">
            <home-outlined />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  getRankingListUsingPost,
  saveGameRecordUsingPost,
  getUserAllHighestScoresUsingGet
} from '@/api/snakeGameController.ts'
import { update } from 'lodash-es'
import {
  SoundOutlined,
  SoundFilled,
  ArrowLeftOutlined,
  RedoOutlined,
  TrophyOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  CloseOutlined,
  HomeOutlined
} from '@ant-design/icons-vue'

// 游戏常量
const GRID_SIZE = 20
const GAME_SPEED = 200
const SPEED_INCREMENT = 5
const CANVAS_SIZE = 400  // 添加画布大小常量

// 游戏模式
const gameModes = [
  { id: 1, name: '经典模式', icon: '🎮', description: '撞墙或自身则游戏结束' },
  { id: 2, name: '无墙模式', icon: '🌐', description: '穿墙不会死亡' },
  { id: 3, name: '竞速模式', icon: '⚡', description: '限时3分钟，追求高分' }
]

// 游戏状态
const gameStarted = ref(false)
const isPaused = ref(false)
const score = ref(0)
const snakeLength = ref(3)
const gameTime = ref(0)
const currentMode = ref(1)
const direction = ref('right')
const snake = ref([])
const food = ref({ x: 0, y: 0 })
let gameLoop: number | null = null
let timeInterval: number | null = null

// 画布相关
const gameCanvas = ref<HTMLCanvasElement | null>(null)
const canvasWidth = ref(CANVAS_SIZE)
const canvasHeight = ref(CANVAS_SIZE)
const cellSize = ref(CANVAS_SIZE / GRID_SIZE)

// 排行榜相关
const showRankingModal = ref(false)
const rankingMode = ref(1)
const rankingList = ref([])
const highestScores = ref({
  1: 0, // 经典模式
  2: 0, // 无墙模式
  3: 0  // 竞速模式
})

// 添加当前速度状态
const currentSpeed = ref(GAME_SPEED)

const showGameOverModal = ref(false)

// 添加障碍物相关状态
const obstacles = ref<Array<{ x: number; y: number }>>([])
const OBSTACLE_COUNT = 5 // 经典模式的障碍物数量

// 修改音效相关变量
const isSoundEnabled = ref(true)
const moveSound = new Audio('/sounds/move.MP3')
const eatSound = new Audio('/sounds/merge.MP3')
const gameStartSound = new Audio('/sounds/start.MP3')
const gameOverSound = new Audio('/sounds/gameover.MP3')

// 设置音效音量
moveSound.volume = 0.3
eatSound.volume = 0.4
gameStartSound.volume = 0.5
gameOverSound.volume = 0.5

// 添加音效控制函数
const toggleSound = () => {
  isSoundEnabled.value = !isSoundEnabled.value
  const volume = isSoundEnabled.value ? 0.3 : 0
  moveSound.volume = volume
  eatSound.volume = volume * 1.33
  gameStartSound.volume = volume * 1.67
  gameOverSound.volume = volume * 1.67
}

// 修改播放音效的函数
const playSound = (sound: HTMLAudioElement) => {
  if (!isSoundEnabled.value || !gameStarted.value) return
  // 只播放吃食物和游戏结束的音效
  if (sound === moveSound) return
  if (sound.paused) {
    sound.currentTime = 0
    sound.play().catch(() => {})
  }
}

// 初始化游戏
const initGame = () => {
  // 初始化蛇的位置
  initSnake()

  // 初始化计分
  score.value = 0
  snakeLength.value = 3
  gameTime.value = 0

  // 生成第一个食物
  generateFood()

  // 开始游戏循环
  startGameLoop()

  // 开始计时
  startTimer()

  // 初始化速度
  currentSpeed.value = GAME_SPEED
}

// 开始游戏
const startGame = (mode: number) => {
  currentMode.value = mode
  gameStarted.value = true
  initGame()
  generateObstacles() // 生成障碍物
  gameLoop = requestAnimationFrame(update)
  startTimer()
  playSound(gameStartSound)
}

// 游戏主循环
const startGameLoop = () => {
  if (gameLoop) cancelAnimationFrame(gameLoop)

  let lastTime = 0
  const loop = (timestamp: number) => {
    if (!isPaused.value) {
      // 根据时间差控制蛇的移动
      if (!lastTime || timestamp - lastTime >= currentSpeed.value) {
        moveSnake()
        drawGame()
        lastTime = timestamp

        // 竞速模式逐渐加快
        if (currentMode.value === 3) {
          currentSpeed.value = Math.max(50, GAME_SPEED - Math.floor(gameTime.value / 10) * SPEED_INCREMENT)
        }
      }
    }
    gameLoop = requestAnimationFrame(loop)
  }

  gameLoop = requestAnimationFrame(loop)
}

// 移动蛇
const moveSnake = () => {
  if (!snake.value.length) return

  const head = { ...snake.value[0] }
  const prevDirection = direction.value

  // 根据方向移动
  switch (direction.value) {
    case 'up':
      head.y -= 1
      break
    case 'down':
      head.y += 1
      break
    case 'left':
      head.x -= 1
      break
    case 'right':
      head.x += 1
      break
  }

  // 无墙模式：穿墙
  if (currentMode.value === 2) {
    if (head.x >= GRID_SIZE) head.x = 0
    if (head.x < 0) head.x = GRID_SIZE - 1
    if (head.y >= GRID_SIZE) head.y = 0
    if (head.y < 0) head.y = GRID_SIZE - 1
  }
  // 其他模式：撞墙死亡
  else if (head.x < 0 || head.x >= GRID_SIZE || head.y < 0 || head.y >= GRID_SIZE) {
    playSound(gameOverSound)
    endGame()
    return
  }

  // 检查是否撞到自己
  if (snake.value.some(segment => segment.x === head.x && segment.y === head.y)) {
    playSound(gameOverSound)
    endGame()
    return
  }

  // 添加新头部
  snake.value.unshift(head)

  // 检查是否吃到食物
  if (food.value && head.x === food.value.x && head.y === food.value.y) {
    playSound(eatSound)
    score.value += 10
    snakeLength.value = snake.value.length
    generateFood()
  } else {
    // 如果没吃到食物，移除尾部
    snake.value.pop()
    // 播放移动音效
    playSound(moveSound)
  }
}

// 绘制游戏
const drawGame = () => {
  const ctx = gameCanvas.value?.getContext('2d')
  if (!ctx) return

  // 清空画布
  ctx.clearRect(0, 0, canvasWidth.value, canvasHeight.value)

  // 绘制网格背景
  ctx.fillStyle = '#f8fafc'
  ctx.fillRect(0, 0, canvasWidth.value, canvasHeight.value)

  // 绘制网格线
  ctx.strokeStyle = '#e2e8f0'
  ctx.lineWidth = 0.5
  for (let i = 0; i <= GRID_SIZE; i++) {
    ctx.beginPath()
    ctx.moveTo(i * cellSize.value, 0)
    ctx.lineTo(i * cellSize.value, canvasHeight.value)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(0, i * cellSize.value)
    ctx.lineTo(canvasWidth.value, i * cellSize.value)
    ctx.stroke()
  }

  // 绘制食物
  if (food.value) {
    ctx.fillStyle = '#ff6b6b'
    ctx.beginPath()
    ctx.arc(
      (food.value.x + 0.5) * cellSize.value,
      (food.value.y + 0.5) * cellSize.value,
      cellSize.value / 2.5,
      0,
      Math.PI * 2
    )
    ctx.fill()
  }

  // 绘制蛇
  snake.value.forEach((segment, index) => {
    // 蛇头使用不同颜色
    ctx.fillStyle = index === 0 ? '#ff8e53' : '#ffd0b0'
    ctx.fillRect(
      segment.x * cellSize.value + 1,
      segment.y * cellSize.value + 1,
      cellSize.value - 2,
      cellSize.value - 2
    )
  })
}

// 生成食物
const generateFood = () => {
  let newFood
  do {
    newFood = {
      x: Math.floor(Math.random() * GRID_SIZE),
      y: Math.floor(Math.random() * GRID_SIZE)
    }
  } while (snake.value.some(segment => segment.x === newFood.x && segment.y === newFood.y))
  food.value = newFood
}

// 开始计时
const startTimer = () => {
  if (timeInterval) clearInterval(timeInterval)
  timeInterval = setInterval(() => {
    if (!isPaused.value) {
      gameTime.value++
      // 竞速模式时间限制
      if (currentMode.value === 3 && gameTime.value >= 180) {
        endGame()
      }
    }
  }, 1000)
}

// 格式化时间
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 暂停游戏
const pauseGame = () => {
  isPaused.value = !isPaused.value
}

// 退出游戏
const quitGame = () => {
  endGame()
  gameStarted.value = false
}

// 修改游戏结束处理
const endGame = async () => {
  if (!gameStarted.value || showGameOverModal.value) return

  // 停止游戏循环和计时器
  if (gameLoop) cancelAnimationFrame(gameLoop)
  if (timeInterval) clearInterval(timeInterval)

  gameStarted.value = false
  // 显示游戏结束弹窗
  showGameOverModal.value = true

  // 播放游戏结束音效
  playSound(gameOverSound)

  // 保存游戏记录
  try {
    await saveGameRecordUsingPost({
      score: score.value,
      foodCount: Math.floor(score.value / 10),
      gameTime: gameTime.value,
      snakeLength: snakeLength.value,
      gameMode: currentMode.value
    })

    // 更新最高分
    await getUserHighestScores()
  } catch (error) {
    message.error('保存游戏记录失败')
  }
}

// 重新开始游戏
const restartGame = () => {
  showGameOverModal.value = false
  initGame()
  startGame(currentMode.value)
}

// 返回模式选择
const backToMode = () => {
  showGameOverModal.value = false
  gameStarted.value = false
  initGame()
}

// 获取用户所有模式最高分
const getUserHighestScores = async () => {
  try {
    const res = await getUserAllHighestScoresUsingGet()
    console.log('获取用户所有模式最高分', res)
    if (res.data) {
      highestScores.value = {
        1: res.data.data.classicModeScore || 0,
        2: res.data.data.noWallModeScore || 0,
        3: res.data.data.speedModeScore || 0
      }
    }
  } catch (error) {
    message.error('获取最高分失败')
  }
}

// 获取排行榜数据
const getRankingData = async () => {
  try {
    const res = await getRankingListUsingPost({
      gameMode: rankingMode.value,
      limit: 50
    })
    if (res.data) {
      rankingList.value = res.data.data
    }
  } catch (error) {
    message.error('获取排行榜失败')
  }
}

// 监听排行榜模式切换
watch(rankingMode, () => {
  getRankingData()
})

// 监听排行榜弹窗显示
watch(showRankingModal, (newVal) => {
  if (newVal) {
    rankingMode.value = 1 // 设置为经典模式
    getRankingData() // 获取数据
  }
})

// 组件挂载时
onMounted(() => {
  getUserHighestScores()
  window.addEventListener('keydown', handleKeydown)
})

// 组件卸载时
onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

// 添加键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  if (isPaused.value) return

  switch (e.key) {
    case 'ArrowUp':
      e.preventDefault()
      if (direction.value !== 'down') direction.value = 'up'
      break
    case 'ArrowDown':
      e.preventDefault()
      if (direction.value !== 'up') direction.value = 'down'
      break
    case 'ArrowLeft':
      e.preventDefault()
      if (direction.value !== 'right') direction.value = 'left'
      break
    case 'ArrowRight':
      e.preventDefault()
      if (direction.value !== 'left') direction.value = 'right'
      break
    case ' ':
      e.preventDefault()
      pauseGame()
      break
  }
}

// 修改方向控制函数
const handleDirection = (newDirection: string) => {
  if (isPaused.value) return

  // 防止反向移动
  switch (newDirection) {
    case 'up':
      if (direction.value !== 'down') direction.value = 'up'
      break
    case 'down':
      if (direction.value !== 'up') direction.value = 'down'
      break
    case 'left':
      if (direction.value !== 'right') direction.value = 'left'
      break
    case 'right':
      if (direction.value !== 'left') direction.value = 'right'
      break
  }
}

// 蛇的初始状态
const initSnake = () => {
  // 确保蛇的初始位置在画布中间
  const startX = Math.floor(GRID_SIZE / 2)
  const startY = Math.floor(GRID_SIZE / 2)
  snake.value = [
    { x: startX, y: startY },
    { x: startX - 1, y: startY },
    { x: startX - 2, y: startY }
  ]
  direction.value = 'right'
}

// 生成障碍物
const generateObstacles = () => {
  obstacles.value = []
  if (currentMode.value !== 1) return // 只在经典模式生成障碍物

  for (let i = 0; i < OBSTACLE_COUNT; i++) {
    let obstacle
    do {
      obstacle = {
        x: Math.floor(Math.random() * GRID_SIZE),
        y: Math.floor(Math.random() * GRID_SIZE)
      }
    } while (
      // 确保障碍物不会生成在蛇身上或食物位置
    snake.value.some(segment => segment.x === obstacle.x && segment.y === obstacle.y) ||
    (food.value && food.value.x === obstacle.x && food.value.y === obstacle.y) ||
    obstacles.value.some(obs => obs.x === obstacle.x && obs.y === obstacle.y) ||
    // 避免在蛇头周围生成障碍物
    (Math.abs(obstacle.x - snake.value[0].x) <= 2 && Math.abs(obstacle.y - snake.value[0].y) <= 2)
      )
    obstacles.value.push(obstacle)
  }
}

// 修改碰撞检测
const checkCollision = () => {
  const head = snake.value[0]

  // 检查是否撞到墙
  if (currentMode.value !== 2) { // 非无墙模式
    if (head.x < 0 || head.x >= GRID_SIZE || head.y < 0 || head.y >= GRID_SIZE) {
      return true
    }
  }

  // 检查是否撞到自己
  for (let i = 1; i < snake.value.length; i++) {
    if (head.x === snake.value[i].x && head.y === snake.value[i].y) {
      return true
    }
  }

  // 检查是否撞到障碍物
  if (currentMode.value === 1) { // 经典模式
    if (obstacles.value.some(obs => head.x === obs.x && head.y === obs.y)) {
      return true
    }
  }

  return false
}

// 修改绘制函数
const draw = () => {
  if (!gameCanvas.value || !ctx) return

  // 清空画布
  ctx.clearRect(0, 0, canvasWidth.value, canvasHeight.value)

  // 绘制网格背景
  drawGrid()

  // 绘制障碍物
  if (currentMode.value === 1) {
    ctx.fillStyle = '#666666'
    obstacles.value.forEach(obstacle => {
      ctx.fillRect(
        obstacle.x * cellSize.value + 1,
        obstacle.y * cellSize.value + 1,
        cellSize.value - 2,
        cellSize.value - 2
      )
    })
  }

}

// 修改画布尺寸计算
const getCanvasSize = () => {
  // 获取屏幕宽度
  const screenWidth = window.innerWidth
  // 获取容器宽度，减去padding
  const containerWidth = screenWidth > 768 ? 800 : screenWidth - 40
  // 确保是20的倍数
  const size = Math.floor(containerWidth / 20) * 20
  return {
    width: size,
    height: size
  }
}

// 响应式更新画布尺寸
const updateCanvasSize = () => {
  const { width, height } = getCanvasSize()
  canvasWidth.value = width
  canvasHeight.value = height
  cellSize.value = width / GRID_SIZE
}

// 监听窗口大小变化
window.addEventListener('resize', updateCanvasSize)
onUnmounted(() => {
  window.removeEventListener('resize', updateCanvasSize)
})

</script>

<style scoped>
.snake-game-page {
  min-height: 100vh;
  margin: 0;
  padding: 0;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1;
  background: #faf8ef;
}

.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.05));
}

.game-container {
  position: relative;
  z-index: 1;
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.mode-selection {
  text-align: center;
}

.mode-selection h2 {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 32px;
}

.mode-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.mode-button {
  height: auto;
  padding: 20px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  border: none;
  transition: all 0.3s ease;
}

.mode-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(255, 142, 83, 0.2);
}

.mode-icon {
  font-size: 32px;
  color: #ff8e53;
}

.mode-content {
  text-align: left;
}

.mode-name {
  font-size: 18px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.mode-description {
  font-size: 14px;
  color: #666;
}

.highest-scores {
  display: grid;
  gap: 16px;
  margin-top: 24px;
}

.score-item {
  padding: 12px 8px;
}

.score-label {
  font-size: 12px;
}

.score-value {
  font-size: 16px;
}

.game-play-area {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.game-canvas {
  border: 2px solid #333;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.mobile-top-bar {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  z-index: 1000;
  padding: 0 16px;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.left-controls, .right-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.right-controls {
  margin-left: auto;
}

.control-button {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #ff8e53;
  color: #ff8e53;
  transition: all 0.2s ease;
}

.control-button:active {
  transform: scale(0.95);
  background: #ff8e53;
  color: white;
}

.control-button .anticon {
  font-size: 20px;
}

.game-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 16px;
}

.info-item {
  text-align: center;
}

.info-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #ff8e53;
}

.info-divider {
  width: 1px;
  height: 20px;
  background: #eee;
}

.mobile-controls {
  display: none;
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20px;
  padding-bottom: calc(env(safe-area-inset-bottom) + 80px);
  background: linear-gradient(to bottom,
  rgba(255, 255, 255, 0),
  rgba(255, 255, 255, 0.8) 20%,
  rgba(255, 255, 255, 0.95));
  backdrop-filter: blur(10px);
  z-index: 100;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.1);
}

.mobile-controls-container {
  max-width: 400px;
  margin: 0 auto;
  background: rgba(255, 142, 83, 0.1);
  border-radius: 24px;
  padding: 20px;
  box-shadow:
    0 8px 24px rgba(255, 142, 83, 0.15),
    inset 0 0 0 1px rgba(255, 142, 83, 0.2);
}

.direction-controls {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.direction-row {
  display: flex;
  justify-content: center;
  gap: 16px;
  height: 64px;
}

.direction-row.middle {
  width: 100%;
  justify-content: space-between;
  padding: 0 40px;
}

.direction-spacer {
  width: 60px;
}

.direction-button {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 142, 83, 0.1);
  box-shadow:
    0 4px 12px rgba(255, 142, 83, 0.15),
    inset 0 0 0 2px rgba(255, 142, 83, 0.3),
    inset 0 -4px 8px rgba(255, 142, 83, 0.2);
  position: relative;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.direction-button::before {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 2px;
  border: 2px solid rgba(255, 142, 83, 0.8);
  border-right: none;
  border-bottom: none;
  transition: all 0.2s ease;
}

.direction-button.up::before {
  transform: translateY(4px) rotate(45deg);
}

.direction-button.down::before {
  transform: translateY(-4px) rotate(-135deg);
}

.direction-button.left::before {
  transform: translateX(4px) rotate(-45deg);
}

.direction-button.right::before {
  transform: translateX(-4px) rotate(135deg);
}

.direction-button:active {
  transform: scale(0.95) translateY(2px);
  background: rgba(255, 142, 83, 0.2);
  box-shadow:
    0 2px 8px rgba(255, 142, 83, 0.2),
    inset 0 0 0 2px rgba(255, 142, 83, 0.4),
    inset 0 -2px 4px rgba(255, 142, 83, 0.3);
}

.direction-button:active::before {
  border-color: rgba(255, 142, 83, 1);
}

.ranking-button {
  background: linear-gradient(135deg, #ff8e53, #ff5151);
  color: white;
  padding: 10px 24px;
  border-radius: 20px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.2);
  margin: 20px auto;
  width: fit-content;
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes waveMove {
  from {
    background-position: 0 0;
  }
  to {
    background-position: 100% 0;
  }
}

/* 自定义模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.close-button {
  font-size: 24px;
  cursor: pointer;
  color: #666;
}

.modal-tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.tab-item {
  padding: 8px 16px;
  cursor: pointer;
  color: #666;
}

.tab-item.active {
  color: #ff8e53;
  border-bottom: 2px solid #ff8e53;
}

/* 排行榜表格样式 */
.ranking-table {
  padding: 20px;
  overflow-y: auto;
  max-height: 60vh;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid #f0f0f0;
}

.ranking-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.ranking-position {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #ff8e53;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 500;
  font-size: 14px;
}

.ranking-username {
  font-weight: 500;
  color: #1a1a1a;
  flex: 1;
}

.ranking-score {
  color: #ff8e53;
  font-weight: 600;
  font-size: 16px;
}

.ranking-details {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 8px;
}

.ranking-detail-item {
  text-align: center;
  padding: 8px;
  background: #f8fafc;
  border-radius: 8px;
}

.detail-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.detail-value {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 500;
}

/* 移除表格相关样式 */
.ranking-table table {
  display: none;
}

/* 游戏结束弹窗样式 */
.game-over-modal {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 32px;
  text-align: center;
  max-width: 320px;
}

.game-over-title {
  font-size: 24px;
  color: #1a1a1a;
  margin-bottom: 24px;
}

.game-over-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.stat-item {
  padding: 12px;
  background: rgba(255, 142, 83, 0.1);
  border-radius: 16px;
}

.stat-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}

.stat-value.highlight {
  color: #ff8e53;
  font-size: 24px;
}

.game-over-buttons {
  display: flex;
  gap: 24px;
  justify-content: center;
}

.game-over-buttons .control-button {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border: 2px solid #ff8e53;
  color: #ff8e53;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.game-over-buttons .control-button:active {
  transform: scale(0.95);
  background: #ff8e53;
  color: white;
}

/* 响应式样式 */
@media screen and (max-width: 768px) {
  .snake-game-page {
    padding-top: 60px;
  }

  .game-container {
    padding: 0;
  }

  .game-play-area {
    padding-top: 20px;
    padding-bottom: 280px;
  }

  .mobile-top-bar {
    display: flex;
  }

  .mobile-controls {
    display: block;
  }

  .game-canvas {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
    margin: 0 auto;
    max-width: 400px;
    border: 2px solid rgba(255, 142, 83, 0.2);
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(255, 142, 83, 0.1);
  }
}

/* PC端适配 */
@media screen and (min-width: 769px) {
  .snake-game-page {
    padding-top: 80px;
  }

  .game-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px;
  }

  .game-play-area {
    padding: 20px;
  }

  .game-canvas {
    border: 2px solid #333;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  }
}

/* 小屏幕适配 */
@media screen and (max-width: 360px) {
  .control-button {
    width: 56px;
    height: 56px;
  }

  .control-button .anticon {
    font-size: 18px;
  }

  .game-info {
    gap: 12px;
    padding: 0 8px;
  }

  .info-value {
    font-size: 14px;
  }

  .direction-button {
    width: 50px;
    height: 50px;
  }

  .direction-row.middle {
    padding: 0 20px;
  }

  .direction-button::before {
    width: 16px;
    height: 16px;
  }

  .game-play-area {
    padding-bottom: 240px;
  }

  .game-over-modal {
    padding: 24px;
  }

  .game-over-stats {
    gap: 12px;
  }

  .stat-value {
    font-size: 18px;
  }

  .stat-value.highlight {
    font-size: 22px;
  }

  .mobile-controls {
    padding: 15px;
    padding-bottom: calc(env(safe-area-inset-bottom) + 60px);
  }

  .mobile-controls-container {
    padding: 15px;
  }
}
</style>
