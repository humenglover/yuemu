<template>
  <div class="brick-game-page"
       @mousemove="handleMouseMove"
       @touchmove="handleTouchMove"
       @touchstart="handleTouchStart">
    <!-- 背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="game-container">
      <!-- 游戏头部 -->
      <div class="game-header">
        <h2 class="game-title">打砖块</h2>
        <div class="scores-container">
          <div class="score-box">
            <div class="score-label">当前分数</div>
            <div class="score-value">{{ score }}</div>
          </div>
          <div class="score-box">
            <div class="score-label">最高分数</div>
            <div class="score-value highlight">{{ bestScore }}</div>
          </div>
        </div>
      </div>

      <!-- 游戏控制 -->
      <div class="game-controls">
        <div class="control-button" @click="startGame" v-if="!isPlaying">
          <play-circle-outlined />
          <span class="button-text">开始游戏</span>
        </div>
        <div class="control-button" @click="pauseGame" v-if="isPlaying && !isPaused">
          <pause-circle-outlined />
          <span class="button-text">暂停</span>
        </div>
        <div class="control-button" @click="resumeGame" v-if="isPlaying && isPaused">
          <play-circle-outlined />
          <span class="button-text">继续</span>
        </div>
        <div class="control-button" @click="restartGame">
          <redo-outlined />
          <span class="button-text">重新开始</span>
        </div>
        <div class="control-button sound-button" @click="toggleSound">
          <sound-filled v-if="isSoundEnabled" />
          <sound-outlined v-else />
          <span class="button-text">{{ isSoundEnabled ? '关闭音效' : '开启音效' }}</span>
        </div>
      </div>

      <!-- 游戏画布 -->
      <div class="game-board-container">
        <canvas ref="gameCanvas" class="game-canvas"></canvas>
      </div>

      <!-- 生命值显示 -->
      <div class="lives-container">
        <heart-filled v-for="n in lives" :key="n" class="life-icon" />
      </div>
    </div>

    <!-- 游戏结束弹窗 -->
    <div v-if="gameOver" class="modal-overlay">
      <div class="modal-content game-over-modal">
        <h3 class="game-over-title">游戏结束</h3>
        <div class="game-over-stats">
          <div class="stat-item">
            <div class="stat-label">最终得分</div>
            <div class="stat-value highlight">{{ score }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">击破砖块</div>
            <div class="stat-value">{{ bricksDestroyed }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">游戏时间</div>
            <div class="stat-value">{{ formatTime(gameTime) }}</div>
          </div>
        </div>
        <div class="game-over-buttons">
          <div class="control-button" @click="restartGame">
            <redo-outlined />
            <span class="button-text">再来一局</span>
          </div>
          <div class="control-button" @click="quitGame">
            <arrow-left-outlined />
            <span class="button-text">返回菜单</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  RedoOutlined,
  SoundOutlined,
  SoundFilled,
  HeartFilled,
  ArrowLeftOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

// 游戏状态
const isPlaying = ref(false)
const isPaused = ref(false)
const gameOver = ref(false)
const score = ref(0)
const bestScore = ref(parseInt(localStorage.getItem('brickGameBestScore') || '0'))
const lives = ref(3)
const bricksDestroyed = ref(0)
const gameTime = ref(0)
const timeInterval = ref<number | null>(null)
const gameCanvas = ref<HTMLCanvasElement | null>(null)
const ctx = ref<CanvasRenderingContext2D | null>(null)

// 音效控制
const isSoundEnabled = ref(true)
const hitSound = new Audio('/sounds/merge.MP3')
const breakSound = new Audio('/sounds/merge.MP3')
const gameOverSound = new Audio('/sounds/gameover.MP3')

// 设置音效音量
hitSound.volume = 0.3
breakSound.volume = 0.3
gameOverSound.volume = 0.5

// 游戏配置
const config = {
  paddleHeight: 12,
  paddleWidth: 150,
  ballRadius: 6,
  brickRowCount: 8,
  brickColumnCount: 16,
  brickWidth: 60,
  brickHeight: 20,
  brickPadding: 2,
  brickOffsetTop: 15,
  brickOffsetLeft: 15,
  ballSpeed: 2
}

// 游戏对象
const paddle = ref({
  x: 0,
  width: config.paddleWidth,
  height: config.paddleHeight
})

const ball = ref({
  x: 0,
  y: 0,
  dx: config.ballSpeed,
  dy: -config.ballSpeed,
  radius: config.ballRadius
})

// 砖块数组
const bricks = ref<any[][]>([])

// 初始化砖块
const initBricks = () => {
  for (let c = 0; c < config.brickColumnCount; c++) {
    bricks.value[c] = []
    for (let r = 0; r < config.brickRowCount; r++) {
      bricks.value[c][r] = {
        x: 0,
        y: 0,
        status: 1,
        color: getRandomBrickColor()
      }
    }
  }
}

// 随机砖块颜色
const getRandomBrickColor = () => {
  const colors = [
    '#FF6B6B', // 红色
    '#4ECDC4', // 青色
    '#45B7D1', // 蓝色
    '#96CEB4', // 绿色
    '#FFEEAD', // 黄色
    '#D4A5A5', // 粉色
    '#9B59B6', // 紫色
    '#3498DB'  // 深蓝色
  ]
  return colors[Math.floor(Math.random() * colors.length)]
}

// 绘制砖块
const drawBricks = () => {
  if (!ctx.value || !gameCanvas.value) return

  // 计算砖块实际大小和偏移，使其均匀分布在画布上
  const totalBrickWidth = config.brickColumnCount * (config.brickWidth + config.brickPadding) - config.brickPadding
  const canvasWidth = gameCanvas.value.width
  const startX = Math.max((canvasWidth - totalBrickWidth) / 2, config.brickOffsetLeft)  // 确保最小左侧偏移

  for (let c = 0; c < config.brickColumnCount; c++) {
    for (let r = 0; r < config.brickRowCount; r++) {
      if (bricks.value[c][r].status === 1) {
        const brickX = startX + c * (config.brickWidth + config.brickPadding)
        const brickY = config.brickOffsetTop + r * (config.brickHeight + config.brickPadding)
        bricks.value[c][r].x = brickX
        bricks.value[c][r].y = brickY

        // 绘制砖块主体
        ctx.value.beginPath()
        ctx.value.rect(brickX, brickY, config.brickWidth, config.brickHeight)
        ctx.value.fillStyle = bricks.value[c][r].color
        ctx.value.fill()
        ctx.value.closePath()

        // 添加高光效果
        ctx.value.beginPath()
        ctx.value.moveTo(brickX, brickY)
        ctx.value.lineTo(brickX + config.brickWidth, brickY)
        ctx.value.strokeStyle = 'rgba(255, 255, 255, 0.3)'
        ctx.value.stroke()
        ctx.value.closePath()
      }
    }
  }
}

// 绘制弹板
const drawPaddle = () => {
  if (!ctx.value || !gameCanvas.value) return

  const paddleY = gameCanvas.value.height - config.paddleHeight - 10

  // 绘制主体
  ctx.value.beginPath()
  ctx.value.rect(paddle.value.x, paddleY, config.paddleWidth, config.paddleHeight)
  ctx.value.fillStyle = '#2C3E50'
  ctx.value.fill()
  ctx.value.closePath()

  // 添加高光效果
  ctx.value.beginPath()
  ctx.value.moveTo(paddle.value.x, paddleY)
  ctx.value.lineTo(paddle.value.x + config.paddleWidth, paddleY)
  ctx.value.strokeStyle = 'rgba(255, 255, 255, 0.5)'
  ctx.value.stroke()
  ctx.value.closePath()
}

// 绘制小球
const drawBall = () => {
  if (!ctx.value) return

  ctx.value.beginPath()
  ctx.value.arc(ball.value.x, ball.value.y, config.ballRadius, 0, Math.PI * 2)
  ctx.value.fillStyle = '#E74C3C'
  ctx.value.fill()
  ctx.value.closePath()

  // 添加高光效果
  ctx.value.beginPath()
  ctx.value.arc(
    ball.value.x - config.ballRadius/3,
    ball.value.y - config.ballRadius/3,
    config.ballRadius/3,
    0,
    Math.PI * 2
  )
  ctx.value.fillStyle = 'rgba(255, 255, 255, 0.6)'
  ctx.value.fill()
  ctx.value.closePath()
}

// 碰撞检测
const collisionDetection = () => {
  for (let c = 0; c < config.brickColumnCount; c++) {
    for (let r = 0; r < config.brickRowCount; r++) {
      const b = bricks.value[c][r]
      if (b.status === 1) {
        if (
          ball.value.x > b.x &&
          ball.value.x < b.x + config.brickWidth &&
          ball.value.y > b.y &&
          ball.value.y < b.y + config.brickHeight
        ) {
          ball.value.dy = -ball.value.dy
          b.status = 0
          score.value += 10
          bricksDestroyed.value++
          playSound(hitSound)

          // 检查是否获胜
          if (bricksDestroyed.value === config.brickRowCount * config.brickColumnCount) {
            handleWin()
          }
        }
      }
    }
  }
}

// 处理胜利
const handleWin = () => {
  isPlaying.value = false
  gameOver.value = true
  clearInterval(timeInterval.value!)
  updateBestScore()
}

// 更新最高分
const updateBestScore = () => {
  if (score.value > bestScore.value) {
    bestScore.value = score.value
    localStorage.setItem('brickGameBestScore', score.value.toString())
  }
}

// 游戏主循环
const draw = () => {
  if (!ctx.value || !gameCanvas.value || isPaused.value) return

  // 清空画布
  ctx.value.clearRect(0, 0, gameCanvas.value.width, gameCanvas.value.height)

  // 绘制游戏元素
  drawBricks()
  drawPaddle()
  drawBall()

  // 碰撞检测
  collisionDetection()

  // 球碰到墙壁
  if (ball.value.x + ball.value.dx > gameCanvas.value.width - config.ballRadius ||
    ball.value.x + ball.value.dx < config.ballRadius) {
    ball.value.dx = -ball.value.dx
    playSound(hitSound)
  }

  // 球碰到天花板
  if (ball.value.y + ball.value.dy < config.ballRadius) {
    ball.value.dy = -ball.value.dy
    playSound(hitSound)
  }

  // 球碰到弹板
  if (ball.value.y + ball.value.dy > gameCanvas.value.height - config.ballRadius - config.paddleHeight) {
    if (ball.value.x > paddle.value.x && ball.value.x < paddle.value.x + config.paddleWidth) {
      // 根据击球位置调整反弹角度
      const hitPoint = (ball.value.x - (paddle.value.x + config.paddleWidth/2)) / (config.paddleWidth/2)
      ball.value.dx = hitPoint * config.ballSpeed
      ball.value.dy = -ball.value.dy
      playSound(hitSound)
    } else if (ball.value.y + ball.value.dy > gameCanvas.value.height - config.ballRadius) {
      // 球落地
      lives.value--
      if (lives.value === 0) {
        handleGameOver()
      } else {
        resetBall()
      }
    }
  }

  // 更新球的位置
  ball.value.x += ball.value.dx
  ball.value.y += ball.value.dy

  if (isPlaying.value) {
    requestAnimationFrame(draw)
  }
}

// 重置球的位置
const resetBall = () => {
  if (!gameCanvas.value) return

  ball.value.x = gameCanvas.value.width / 2
  ball.value.y = gameCanvas.value.height - 30
  ball.value.dx = config.ballSpeed * (Math.random() > 0.5 ? 1 : -1)
  ball.value.dy = -config.ballSpeed

  // 重置弹板位置
  paddle.value.x = (gameCanvas.value.width - config.paddleWidth) / 2
}

// 处理游戏结束
const handleGameOver = () => {
  isPlaying.value = false
  gameOver.value = true
  clearInterval(timeInterval.value!)
  playSound(gameOverSound)
  updateBestScore()
}

// 开始游戏
const startGame = () => {
  if (!gameCanvas.value) return

  isPlaying.value = true
  isPaused.value = false
  gameOver.value = false
  score.value = 0
  lives.value = 3
  bricksDestroyed.value = 0
  gameTime.value = 0

  // 初始化砖块
  initBricks()

  // 初始化球和弹板位置
  resetBall()

  // 开始计时
  timeInterval.value = setInterval(() => {
    if (!isPaused.value) {
      gameTime.value++
    }
  }, 1000)

  // 开始游戏循环
  draw()
}

// 暂停游戏
const pauseGame = () => {
  isPaused.value = true
}

// 继续游戏
const resumeGame = () => {
  isPaused.value = false
  draw()
}

// 重新开始游戏
const restartGame = () => {
  startGame()
}

// 退出游戏
const quitGame = () => {
  router.push('/games')
}

// 播放音效
const playSound = (sound: HTMLAudioElement) => {
  if (!isSoundEnabled.value) return
  sound.currentTime = 0
  sound.play().catch(() => {})
}

// 切换音效
const toggleSound = () => {
  isSoundEnabled.value = !isSoundEnabled.value
}

// 处理鼠标移动
const handleMouseMove = (e: MouseEvent) => {
  if (!gameCanvas.value || !isPlaying.value || isPaused.value) return

  const rect = gameCanvas.value.getBoundingClientRect()  // 获取画布相对于视口的位置
  const relativeX = e.clientX - rect.left  // 计算鼠标相对于画布的X坐标

  if (relativeX > 0 && relativeX < gameCanvas.value.width) {
    paddle.value.x = relativeX - config.paddleWidth / 2
  }

  // 确保弹板不会超出画布
  if (paddle.value.x < 0) {
    paddle.value.x = 0
  } else if (paddle.value.x + config.paddleWidth > gameCanvas.value.width) {
    paddle.value.x = gameCanvas.value.width - config.paddleWidth
  }
}

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  e.preventDefault()
}

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (!gameCanvas.value || !isPlaying.value || isPaused.value) return

  e.preventDefault()
  const touch = e.touches[0]
  const rect = gameCanvas.value.getBoundingClientRect()  // 获取画布相对于视口的位置
  const relativeX = touch.clientX - rect.left  // 计算触摸点相对于画布的X坐标

  if (relativeX > 0 && relativeX < gameCanvas.value.width) {
    paddle.value.x = relativeX - config.paddleWidth / 2
  }

  // 确保弹板不会超出画布
  if (paddle.value.x < 0) {
    paddle.value.x = 0
  } else if (paddle.value.x + config.paddleWidth > gameCanvas.value.width) {
    paddle.value.x = gameCanvas.value.width - config.paddleWidth
  }
}

// 格式化时间
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 调整画布大小
const resizeCanvas = () => {
  if (!gameCanvas.value) return

  const container = gameCanvas.value.parentElement
  if (!container) return

  // 设置画布尺寸为容器的实际尺寸
  gameCanvas.value.width = container.clientWidth
  gameCanvas.value.height = container.clientHeight

  // 根据画布宽度调整砖块大小
  const maxBrickWidth = Math.floor((gameCanvas.value.width - config.brickOffsetLeft * 2 - (config.brickColumnCount - 1) * config.brickPadding) / config.brickColumnCount)
  config.brickWidth = Math.min(maxBrickWidth, 45)  // 减小砖块最大宽度

  // 根据画布高度调整砖块高度
  const maxBrickHeight = Math.floor((gameCanvas.value.height * 0.35 - config.brickOffsetTop - (config.brickRowCount - 1) * config.brickPadding) / config.brickRowCount)
  config.brickHeight = Math.min(maxBrickHeight, 18)  // 减小砖块最大高度

  // 调整砖块间距以确保铺满
  const availableWidth = gameCanvas.value.width - config.brickOffsetLeft * 2
  const totalBrickWidth = config.brickColumnCount * config.brickWidth
  config.brickPadding = Math.max((availableWidth - totalBrickWidth) / (config.brickColumnCount - 1), 2)

  // 重置游戏对象位置
  if (isPlaying.value) {
    resetBall()
  }
}

// 组件挂载
onMounted(() => {
  if (!gameCanvas.value) return

  ctx.value = gameCanvas.value.getContext('2d')
  resizeCanvas()

  window.addEventListener('resize', resizeCanvas)
})

// 组件卸载
onUnmounted(() => {
  if (timeInterval.value) {
    clearInterval(timeInterval.value)
  }
  window.removeEventListener('resize', resizeCanvas)
})
</script>

<style scoped>
.brick-game-page {
  min-height: 88vh;
  background: #faf8ef;
  margin: -20px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: "Clear Sans", "Helvetica Neue", Arial, sans-serif;
  touch-action: none;
  padding: 20px;
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
  width: 90%;
  max-width: 1200px;
  padding: 20px;
  background: #faf8ef;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  z-index: 1;
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.game-title {
  font-size: 48px;
  font-weight: bold;
  color: #776e65;
  margin: 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
}

.scores-container {
  display: flex;
  gap: 10px;
}

.score-box {
  background: #bbada0;
  padding: 12px 20px;
  border-radius: 6px;
  min-width: 100px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.score-label {
  font-size: 13px;
  color: #eee4da;
  text-transform: uppercase;
  margin-bottom: 4px;
  letter-spacing: 1px;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

.score-value.highlight {
  color: #fff;
  text-shadow: 0 0 4px rgba(255, 255, 255, 0.3);
}

.game-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.control-button {
  background: #8f7a66;
  border-radius: 6px;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
  color: #f9f6f2;
  font-weight: bold;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.control-button:hover {
  background: #9f8b77;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.game-board-container {
  width: 100%;
  height: 600px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.1);
  margin: 0 auto;
}

.game-canvas {
  width: 100%;
  height: 100%;
  display: block;
  touch-action: none;
}

.lives-container {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}

.life-icon {
  color: #e74c3c;
  font-size: 24px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(238, 228, 218, 0.90);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fade-in 800ms ease-in-out;
}

.game-over-modal {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  text-align: center;
  animation: slide-up 400ms ease-in-out;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 300px;
  max-width: 90%;
}

@keyframes fade-in {
  0% {
    opacity: 0;
  }
  100% {
    opacity: 1;
  }
}

@keyframes slide-up {
  0% {
    opacity: 0;
    transform: translateY(20px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.game-over-title {
  font-size: 48px;
  font-weight: bold;
  color: #776e65;
  margin-bottom: 30px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.game-over-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 20px;
  margin-bottom: 30px;
}

.stat-item {
  background: #eee4da;
  padding: 15px 25px;
  border-radius: 8px;
  color: #776e65;
  min-width: 120px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stat-label {
  font-size: 14px;
  text-transform: uppercase;
  margin-bottom: 8px;
  color: #a39c95;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
}

.stat-value.highlight {
  color: #f65e3b;
}

.game-over-buttons {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 15px;
}

.game-over-buttons .control-button {
  min-width: 120px;
}

@media screen and (max-width: 768px) {
  .game-container {
    width: 95%;
    padding: 15px;
    margin: 0 auto;
  }

  .game-title {
    font-size: 32px;
  }

  .score-box {
    padding: 8px 12px;
    min-width: 80px;
  }

  .score-value {
    font-size: 20px;
  }

  .game-board-container {
    height: 500px;
  }

  .control-button {
    padding: 0 15px;
    height: 36px;
    font-size: 14px;
  }

  .control-button .button-text {
    display: none;
  }

  .game-over-modal {
    padding: 30px 20px;
  }

  .game-over-title {
    font-size: 36px;
  }

  .stat-item {
    padding: 12px 20px;
    min-width: 100px;
  }

  .stat-value {
    font-size: 24px;
  }
}

@media screen and (max-width: 480px) {
  .game-container {
    width: 100%;
    padding: 10px;
  }

  .game-board-container {
    height: 400px;
  }

  .game-controls {
    gap: 8px;
  }

  .control-button {
    width: 36px;
    padding: 0;
    border-radius: 50%;
  }

  .life-icon {
    font-size: 20px;
  }
}
</style>
