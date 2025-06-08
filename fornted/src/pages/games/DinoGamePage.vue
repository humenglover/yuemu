<template>
  <div class="dino-game-page">
    <!-- 添加教程按钮 -->
    <div class="tutorial-button" @click="showTutorial" :class="{ 'tutorial-button-mobile': isMobile }">
      <question-circle-outlined />
    </div>

    <!-- 游戏教程弹窗 -->
    <div v-if="isTutorialVisible" class="modal-overlay">
      <div class="modal-content tutorial-modal">
        <h3 class="tutorial-title">游戏教程</h3>
        <div class="tutorial-content">
          <div class="tutorial-section">
            <h4>基本操作</h4>
            <p>PC端：</p>
            <ul>
              <li>空格键或↑键：跳跃（按住可二段跳）</li>
              <li>→键：加速</li>
              <li>ESC：暂停游戏</li>
            </ul>
            <p>移动端：</p>
            <ul>
              <li>点击跳跃按钮：跳跃（快速点击两次可二段跳）</li>
              <li>长按加速按钮：加速</li>
            </ul>
          </div>
          <div class="tutorial-section">
            <h4>道具说明</h4>
            <ul>
              <li>🛡️ 护盾：暂时无敌</li>
              <li>🧲 磁铁：吸引金币</li>
              <li>✨ 双倍：得分翻倍</li>
              <li>💥 清除：消除所有障碍</li>
              <li>⏱️ 慢动作：减慢游戏速度</li>
            </ul>
          </div>
          <div class="tutorial-section">
            <h4>障碍物</h4>
            <ul>
              <li>地面障碍：岩石、树木、仙人掌、尖刺</li>
              <li>空中障碍：飞鸟、导弹、激光、龙卷风、火球</li>
            </ul>
          </div>
        </div>
        <div class="tutorial-footer">
          <button class="control-button" @click="closeTutorial">
            <check-outlined />
            <span class="button-text">知道了</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="game-container">
      <!-- 游戏头部 -->
      <div class="game-header">
        <h2 class="game-title">恐龙快跑</h2>
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
        <!-- 添加道具计时器显示 -->
        <div class="power-up-timers" v-if="isPlaying && !gameOver">
          <div class="timer-item" v-if="gameState.activePowerUps.shield">
            <img src="/game_pictures/shield.png" class="timer-icon" />
            <div class="timer-bar">
              <div class="timer-progress" :style="getTimerStyle('shield')"></div>
            </div>
          </div>
          <div class="timer-item" v-if="gameState.activePowerUps.magnet">
            <img src="/game_pictures/magnet.png" class="timer-icon" />
            <div class="timer-bar">
              <div class="timer-progress" :style="getTimerStyle('magnet')"></div>
            </div>
          </div>
          <div class="timer-item" v-if="gameState.activePowerUps.doubleScore">
            <div class="timer-icon">x2</div>
            <div class="timer-bar">
              <div class="timer-progress" :style="getTimerStyle('doubleScore')"></div>
            </div>
          </div>
          <div class="timer-item" v-if="gameState.activePowerUps.slowMotion">
            <div class="timer-icon">⏱️</div>
            <div class="timer-bar">
              <div class="timer-progress" :style="getTimerStyle('slowMotion')"></div>
            </div>
          </div>
        </div>
        <canvas
          ref="gameCanvas"
          class="game-canvas"
          @touchstart.stop.prevent="handleJump"
          @touchend.stop.prevent="() => {}"
        ></canvas>
      </div>

      <!-- 生命值显示 -->
      <div class="lives-container">
        <heart-filled v-for="n in lives" :key="n" class="life-icon" />
      </div>
    </div>

    <!-- 移动端控制按钮 -->
    <div class="mobile-controls" v-if="isMobile">
      <div class="mobile-controls-container">
        <div class="mobile-controls-left">
          <button
            class="mobile-btn jump-btn"
            @touchstart.stop.prevent="handleJump"
            @touchend.stop.prevent="() => {}"
          >
            <up-outlined />
            <span class="btn-label">跳跃</span>
          </button>
        </div>
        <div class="mobile-controls-right">
          <button
            class="mobile-btn accelerate-btn"
            @touchstart.stop.prevent="handleAccelerationStart"
            @touchend.stop.prevent="handleAccelerationEnd"
            @touchcancel.stop.prevent="handleAccelerationEnd"
          >
            <right-outlined />
            <span class="btn-label">加速</span>
          </button>
        </div>
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
            <div class="stat-label">收集金币</div>
            <div class="stat-value">{{ gameState.coinsCollected }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">存活时间</div>
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
import {
  PlayCircleOutlined,
  PauseCircleOutlined,
  RedoOutlined,
  SoundOutlined,
  SoundFilled,
  HeartFilled,
  ArrowLeftOutlined,
  UpOutlined,
  RightOutlined,
  QuestionCircleOutlined,
  CheckOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

interface GameState {
  score: number
  bestScore: number
  isPlaying: boolean
  isPaused: boolean
  gameOver: boolean
  lives: number
  gameTime: number
  coinsCollected: number
  activePowerUps: {
    shield: boolean
    magnet: boolean
    doubleScore: boolean
    slowMotion: boolean  // 添加减速状态
  }
  powerUpTimers: {
    shield: number
    magnet: number
    doubleScore: number
    slowMotion: number   // 添加减速计时器
  }
}

interface Obstacle {
  x: number
  y: number
  width: number
  height: number
  type: 'rock' | 'tree' | 'missile' | 'bird' | 'cactus' | 'spike' | 'laser' | 'tornado' | 'fireball'
  speed: number
  createdAt: number
  verticalOffset: number
  color: string
  flying: boolean
}

interface DinoState {
  x: number
  y: number
  velocityY: number
  isJumping: boolean
  jumpState: 'idle' | 'rising' | 'falling'
  lastJumpTime: number
  canJump: boolean
  jumpCount: number
  maxJumpCount: number
  isAccelerating: boolean
  accelerationStartTime: number | null
}

interface PowerUp {
  x: number
  y: number
  width: number
  height: number
  type: 'shield' | 'magnet' | 'doubleScore' | 'clear' | 'slowMotion'  // 添加减速类型
  active: boolean
  duration: number
  startTime?: number
  color: string
  collected: boolean
}

interface Coin {
  x: number
  y: number
  width: number
  height: number
  collected: boolean
  magnetized: boolean
  value: number
}

const gameState = ref<GameState>({
  score: 0,
  bestScore: parseInt(localStorage.getItem('dinoGameBestScore') || '0'),
  isPlaying: false,
  isPaused: false,
  gameOver: false,
  lives: 1,
  gameTime: 0,
  coinsCollected: 0,
  activePowerUps: {
    shield: false,
    magnet: false,
    doubleScore: false,
    slowMotion: false    // 添加减速状态
  },
  powerUpTimers: {
    shield: 0,
    magnet: 0,
    doubleScore: 0,
    slowMotion: 0        // 添加减速计时器
  }
})

const dino = ref<DinoState>({
  x: 100,
  y: 0,
  velocityY: 0,
  isJumping: false,
  jumpState: 'idle',
  lastJumpTime: 0,
  canJump: true,
  jumpCount: 0,
  maxJumpCount: 2,
  isAccelerating: false,
  accelerationStartTime: null
})

const obstacles = ref<Obstacle[]>([])
const powerUps = ref<PowerUp[]>([])
const coins = ref<Coin[]>([])

// 游戏状态
const isPlaying = ref(false)
const isPaused = ref(false)
const gameOver = ref(false)
const score = ref(0)
const bestScore = ref(parseInt(localStorage.getItem('dinoGameBestScore') || '0'))
const lives = ref(1)
const gameTime = ref(0)
const timeInterval = ref<number | null>(null)
const gameCanvas = ref<HTMLCanvasElement | null>(null)
const ctx = ref<CanvasRenderingContext2D | null>(null)

// 音效控制
const isSoundEnabled = ref(true)
const jumpSound = new Audio('/sounds/merge.MP3')
const gameOverSound = new Audio('/sounds/gameover.MP3')
const shieldEffect = ref<HTMLAudioElement>(new Audio('/sounds/shield.MP3'))
const coinEffect = ref<HTMLAudioElement>(new Audio('/sounds/coin.MP3'))
const powerUpEffect = ref<HTMLAudioElement>(new Audio('/sounds/powerup.MP3'))
const clearEffect = ref<HTMLAudioElement>(new Audio('/sounds/clear.MP3'))

// 设置音效音量
jumpSound.volume = 0.3
gameOverSound.volume = 0.5
shieldEffect.value.volume = 0.3
coinEffect.value.volume = 0.2
powerUpEffect.value.volume = 0.3
clearEffect.value.volume = 0.4

// 游戏配置
const config = {
  // 基础配置
  groundHeight: 20,
  // 通用游戏参数
  jumpCooldown: 300,
  doubleTapDelay: 400,
  accelerationDelay: 800,
  maxAccelerationMultiplier: 1.4,
  accelerationGrowthRate: 0.2,
  obstacleLifetime: 10000,
  collisionBuffer: 8,
  powerUps: {
    shield: {
      duration: 5000,
      probability: 0.008,
      color: 'rgba(64, 196, 255, 0.7)'
    },
    magnet: {
      duration: 7000,
      probability: 0.01,
      range: 200,
      color: 'rgba(147, 112, 219, 0.7)'
    },
    doubleScore: {
      duration: 10000,
      probability: 0.008,
      color: 'rgba(255, 215, 0, 0.7)'
    },
    slowMotion: {        // 添加减速道具配置
      duration: 6000,
      probability: 0.007,
      slowFactor: 0.6,   // 速度减慢到原来的60%
      color: 'rgba(0, 128, 128, 0.7)'
    },
    clear: {
      probability: 0.005,
      color: 'rgba(255, 69, 0, 0.7)'
    }
  },
  coins: {
    value: 10,
    probability: 0.08,
    color: '#ffd700'
  },
  // 桌面端配置
  desktop: {
    dinoWidth: 40,
    dinoHeight: 40,
    obstacleTypes: [
      { type: 'rock', width: 30, height: 30, probability: 0.15, speed: 1, color: '#666666' },
      { type: 'tree', width: 30, height: 50, probability: 0.15, speed: 1, color: '#3a5a40' },
      { type: 'missile', width: 25, height: 15, probability: 0.1, speed: 1.3, color: '#ff4444', flying: true },
      { type: 'bird', width: 35, height: 20, probability: 0.15, speed: 1.4, color: '#4a90e2', flying: true },
      { type: 'cactus', width: 25, height: 45, probability: 0.15, speed: 1, color: '#2d4330' },
      { type: 'spike', width: 20, height: 40, probability: 0.1, speed: 1.1, color: '#8b0000' },
      { type: 'laser', width: 15, height: 80, probability: 0.07, speed: 1.5, color: '#ff0000', flying: true },
      { type: 'tornado', width: 40, height: 60, probability: 0.07, speed: 1.2, color: '#4a4a4a', flying: true },
      { type: 'fireball', width: 30, height: 30, probability: 0.06, speed: 1.6, color: '#ff8c00', flying: true }
    ],
    minObstacleDistance: 400,
    maxObstacleDistance: 600,
    initialGameSpeed: 2,
    maxGameSpeed: 6,
    speedIncreaseFactor: 0.0002,
    normalJumpForce: 14,
    highJumpForce: 17,
    doubleJumpForce: 16,
    gravity: 0.25,
    maxJumpHeight: 220,
    maxFallingSpeed: 4
  },
  // 移动端配置
  mobile: {
    dinoWidth: 40, // 原为25，增大恐龙尺寸
    dinoHeight: 40, // 原为25，增大恐龙尺寸
    obstacleTypes: [
      { type: 'rock', width: 20, height: 20, probability: 0.2, speed: 1, color: '#666666' },
      { type: 'tree', width: 20, height: 35, probability: 0.2, speed: 1, color: '#3a5a40' },
      { type: 'missile', width: 25, height: 15, probability: 0.1, speed: 1.2, color: '#ff4444', flying: true },
      { type: 'bird', width: 25, height: 15, probability: 0.15, speed: 1.3, color: '#4a90e2', flying: true },
      { type: 'cactus', width: 18, height: 32, probability: 0.15, speed: 1, color: '#2d4330' },
      { type: 'spike', width: 15, height: 28, probability: 0.1, speed: 1.1, color: '#8b0000' },
      { type: 'laser', width: 12, height: 60, probability: 0.05, speed: 1.4, color: '#ff0000', flying: true },
      { type: 'tornado', width: 30, height: 45, probability: 0.03, speed: 1.2, color: '#4a4a4a', flying: true },
      { type: 'fireball', width: 20, height: 20, probability: 0.02, speed: 1.5, color: '#ff8c00', flying: true }
    ],
    minObstacleDistance: 300,
    maxObstacleDistance: 450,
    initialGameSpeed: 1.5,
    maxGameSpeed: 4.5,
    speedIncreaseFactor: 0.00015,
    normalJumpForce: 13,     // 稍微减小初始跳跃力度
    highJumpForce: 15,       // 保持高跳的力度
    doubleJumpForce: 12,     // 降低二段跳的力度使其更容易控制
    gravity: 0.25,           // 增加重力使下落更快
    maxJumpHeight: 160,      // 减小最大跳跃高度
    maxFallingSpeed: 4       // 增加最大下落速度
  }
}

// 检测是否为移动设备
const isMobile = ref(window.innerWidth <= 768)
// 获取当前配置
const getCurrentConfig = () => {
  return isMobile.value ? config.mobile : config.desktop
}

// 监听窗口大小变化
window.addEventListener('resize', () => {
  isMobile.value = window.innerWidth <= 768
  resizeCanvas()
  initDino()
})

// 初始化恐龙位置
const initDino = () => {
  if (!gameCanvas.value) return
  // 移动端恐龙更靠左
  dino.value.x = isMobile.value ? 40 : 100
  dino.value.y = gameCanvas.value.height - getCurrentConfig().dinoHeight - config.groundHeight
}

// 生成随机障碍物
const generateObstacle = () => {
  if (!gameCanvas.value) return

  const currentConfig = getCurrentConfig()
  // 随机选择障碍物类型
  const random = Math.random()
  let cumulativeProbability = 0
  let selectedType = currentConfig.obstacleTypes[0]

  for (const type of currentConfig.obstacleTypes) {
    cumulativeProbability += type.probability
    if (random <= cumulativeProbability) {
      selectedType = type
      break
    }
  }

  const lastObstacle = obstacles.value[obstacles.value.length - 1]
  const minDistance = lastObstacle
    ? lastObstacle.x + currentConfig.minObstacleDistance
    : gameCanvas.value.width
  const maxDistance = lastObstacle
    ? lastObstacle.x + currentConfig.maxObstacleDistance
    : gameCanvas.value.width

  const verticalPosition = calculateObstacleY(selectedType, gameCanvas.value)

  obstacles.value.push({
    x: Math.random() * (maxDistance - minDistance) + minDistance,
    y: verticalPosition,
    width: selectedType.width,
    height: selectedType.height,
    type: selectedType.type,
    speed: selectedType.speed,
    color: selectedType.color,
    flying: selectedType.flying || false,
    createdAt: Date.now(),
    verticalOffset: 0
  })
}

// 更新障碍物位置
const updateObstacles = () => {
  if (!gameCanvas.value) return

  const currentTime = Date.now()
  obstacles.value = obstacles.value.filter(obstacle => {
    // 移除超时或移出屏幕的障碍物
    return obstacle.x + obstacle.width > 0 &&
      (currentTime - obstacle.createdAt) < config.obstacleLifetime
  })

  // 根据分数计算当前游戏速度
  const currentSpeed = Math.min(
    getCurrentConfig().maxGameSpeed,
    getCurrentConfig().initialGameSpeed + (score.value * getCurrentConfig().speedIncreaseFactor)
  )

  obstacles.value.forEach(obstacle => {
    // 更新水平位置
    obstacle.x -= currentSpeed * obstacle.speed

    // 每2秒对垂直位置进行微调（仅针对飞弹类型）
    if (obstacle.type === 'missile' && currentTime % 2000 < 16) {
      obstacle.verticalOffset = (Math.random() - 0.5) * 20
    }
    obstacle.y += obstacle.verticalOffset
  })

  // 生成新的障碍物
  if (obstacles.value.length === 0 ||
    obstacles.value[obstacles.value.length - 1].x < gameCanvas.value.width - getCurrentConfig().minObstacleDistance) {
    generateObstacle()
  }
}

// 绘制恐龙
const drawDino = () => {
  if (!ctx.value || !gameCanvas.value) return

  const currentConfig = getCurrentConfig()
  const x = dino.value.x
  const y = dino.value.y
  const context = ctx.value
  const scale = isMobile.value ? 0.625 : 1 // 25/40 = 0.625，移动端缩放比例

  context.save()

  // 恐龙身体
  context.fillStyle = '#535353'
  context.beginPath()
  context.roundRect(
    x + 10 * scale,
    y + 10 * scale,
    20 * scale,
    25 * scale,
    4 * scale
  )
  context.fill()

  // 恐龙头部
  context.beginPath()
  context.roundRect(
    x + 25 * scale,
    y,
    12 * scale,
    18 * scale,
    3 * scale
  )
  context.fill()

  // 恐龙眼睛
  context.fillStyle = '#ffffff'
  context.beginPath()
  context.arc(
    x + 32 * scale,
    y + 6 * scale,
    2 * scale,
    0,
    Math.PI * 2
  )
  context.fill()

  // 恐龙腿部
  context.fillStyle = '#535353'
  const legOffset = Math.sin(Date.now() / 100) * (4 * scale)
  // 左腿
  context.fillRect(
    x + 15 * scale,
    y + 35 * scale + (dino.value.isJumping ? 0 : legOffset),
    6 * scale,
    8 * scale
  )
  // 右腿
  context.fillRect(
    x + 22 * scale,
    y + 35 * scale + (dino.value.isJumping ? 0 : -legOffset),
    6 * scale,
    8 * scale
  )

  context.restore()
}

// 绘制障碍物
const drawObstacles = () => {
  if (!ctx.value || !gameCanvas.value) return

  obstacles.value.forEach(obstacle => {
    const context = ctx.value!
    const x = obstacle.x
    const y = obstacle.y

    context.save()

    switch (obstacle.type) {
      case 'rock':
        // 绘制石头
        context.fillStyle = obstacle.color
        context.beginPath()
        context.roundRect(x, y, obstacle.width, obstacle.height, 8)
        context.fill()
        // 添加纹理
        context.strokeStyle = '#555555'
        context.lineWidth = 2
        context.beginPath()
        context.moveTo(x + obstacle.width * 0.3, y + obstacle.height * 0.4)
        context.lineTo(x + obstacle.width * 0.7, y + obstacle.height * 0.4)
        context.stroke()
        break

      case 'tree':
        // 绘制树木
        context.fillStyle = obstacle.color
        // 树干
        context.beginPath()
        context.roundRect(x + obstacle.width * 0.4, y + obstacle.height * 0.2,
          obstacle.width * 0.2, obstacle.height * 0.8, 2)
        context.fill()
        // 树叶
        context.beginPath()
        context.arc(x + obstacle.width * 0.5, y + obstacle.height * 0.3,
          obstacle.width * 0.4, 0, Math.PI * 2)
        context.fill()
        break

      case 'missile':
        // 绘制飞弹
        context.fillStyle = obstacle.color
        // 飞弹主体
        context.beginPath()
        context.roundRect(x, y, obstacle.width, obstacle.height, 5)
        context.fill()
        // 飞弹尾部
        context.fillStyle = '#ff8888'
        context.beginPath()
        context.moveTo(x - obstacle.width * 0.2, y + obstacle.height * 0.5)
        context.lineTo(x, y)
        context.lineTo(x, y + obstacle.height)
        context.closePath()
        context.fill()
        break

      case 'bird':
        // 绘制鸟
        context.fillStyle = obstacle.color
        const wingOffset = Math.sin(Date.now() / 100) * 5
        // 身体
        context.beginPath()
        context.ellipse(x + obstacle.width * 0.5, y + obstacle.height * 0.5,
          obstacle.width * 0.4, obstacle.height * 0.3, 0, 0, Math.PI * 2)
        context.fill()
        // 翅膀
        context.beginPath()
        context.ellipse(x + obstacle.width * 0.5, y + obstacle.height * 0.5 - wingOffset,
          obstacle.width * 0.3, obstacle.height * 0.2, Math.PI * 0.25, 0, Math.PI * 2)
        context.fill()
        // 头部
        context.beginPath()
        context.arc(x + obstacle.width * 0.7, y + obstacle.height * 0.4,
          obstacle.height * 0.2, 0, Math.PI * 2)
        context.fill()
        break

      case 'cactus':
        // 绘制仙人掌
        context.fillStyle = obstacle.color
        // 主干
        context.beginPath()
        context.roundRect(x + obstacle.width * 0.4, y,
          obstacle.width * 0.2, obstacle.height, 2)
        context.fill()
        // 左侧分支
        context.beginPath()
        context.roundRect(x, y + obstacle.height * 0.3,
          obstacle.width * 0.5, obstacle.width * 0.2, 2)
        context.fill()
        // 右侧分支
        context.beginPath()
        context.roundRect(x + obstacle.width * 0.5, y + obstacle.height * 0.5,
          obstacle.width * 0.5, obstacle.width * 0.2, 2)
        context.fill()
        break

      case 'spike':
        // 绘制尖刺
        context.fillStyle = obstacle.color
        // 主体
        context.beginPath()
        context.moveTo(x + obstacle.width * 0.5, y)
        context.lineTo(x + obstacle.width, y + obstacle.height)
        context.lineTo(x, y + obstacle.height)
        context.closePath()
        context.fill()
        // 阴影效果
        context.fillStyle = 'rgba(0,0,0,0.2)'
        context.beginPath()
        context.moveTo(x + obstacle.width * 0.5, y)
        context.lineTo(x + obstacle.width * 0.8, y + obstacle.height)
        context.lineTo(x + obstacle.width * 0.2, y + obstacle.height)
        context.closePath()
        context.fill()
        break

      case 'laser':
        // 绘制激光
        context.fillStyle = obstacle.color
        context.globalAlpha = 0.8
        // 激光主体
        context.fillRect(x, y, obstacle.width, obstacle.height)
        // 发光效果
        const gradient = context.createLinearGradient(x, y, x + obstacle.width, 0)
        gradient.addColorStop(0, 'rgba(255, 0, 0, 0.2)')
        gradient.addColorStop(0.5, 'rgba(255, 0, 0, 0.5)')
        gradient.addColorStop(1, 'rgba(255, 0, 0, 0.2)')
        context.fillStyle = gradient
        context.fillRect(x - 5, y, obstacle.width + 10, obstacle.height)
        context.globalAlpha = 1
        break

      case 'tornado':
        // 绘制龙卷风
        const time = Date.now() / 200
        context.fillStyle = obstacle.color
        context.beginPath()
        for (let i = 0; i < 5; i++) {
          const offset = Math.sin(time + i) * 5
          const width = obstacle.width * (1 - i * 0.15)
          const height = obstacle.height / 5
          context.ellipse(
            x + obstacle.width / 2 + offset,
            y + i * height,
            width / 2,
            height / 2,
            0,
            0,
            Math.PI * 2
          )
        }
        context.fill()
        break

      case 'fireball':
        // 绘制火球
        const fireballTime = Date.now() / 100
        // 火球主体
        context.fillStyle = obstacle.color
        context.beginPath()
        context.arc(x + obstacle.width / 2, y + obstacle.height / 2,
          obstacle.width / 2, 0, Math.PI * 2)
        context.fill()

        // 火焰效果
        for (let i = 0; i < 8; i++) {
          const angle = (i / 8) * Math.PI * 2 + fireballTime
          const flameLength = Math.sin(fireballTime * 2 + i) * 10 + 15
          context.fillStyle = `rgba(255, ${Math.random() * 100 + 100}, 0, ${Math.random() * 0.5 + 0.5})`
          context.beginPath()
          context.moveTo(x + obstacle.width / 2, y + obstacle.height / 2)
          context.lineTo(
            x + obstacle.width / 2 + Math.cos(angle) * flameLength,
            y + obstacle.height / 2 + Math.sin(angle) * flameLength
          )
          context.lineTo(
            x + obstacle.width / 2 + Math.cos(angle + 0.5) * (flameLength * 0.6),
            y + obstacle.height / 2 + Math.sin(angle + 0.5) * (flameLength * 0.6)
          )
          context.closePath()
          context.fill()
        }
        break
    }

    context.restore()
  })
}

// 绘制地面
const drawGround = () => {
  if (!ctx.value || !gameCanvas.value) return

  const context = ctx.value
  const groundY = gameCanvas.value.height - config.groundHeight

  // 绘制主地面
  context.fillStyle = '#ada397'
  context.fillRect(0, groundY, gameCanvas.value.width, config.groundHeight)

  // 添加地面纹理
  context.fillStyle = '#8b7355'
  for (let i = 0; i < gameCanvas.value.width; i += 30) {
    context.beginPath()
    context.arc(i, groundY + 5, 2, 0, Math.PI * 2)
    context.fill()
  }

  // 添加地面线条
  context.strokeStyle = '#8b7355'
  context.lineWidth = 1
  context.beginPath()
  context.moveTo(0, groundY + 2)
  context.lineTo(gameCanvas.value.width, groundY + 2)
  context.stroke()
}

// 碰撞检测
const checkCollision = () => {
  if (!gameCanvas.value) return false

  return obstacles.value.some(obstacle => {
    const collision = checkCollisionWithDino(obstacle)

    if (collision) {
      // 如果有护盾，销毁障碍物而不是结束游戏
      if (gameState.value.activePowerUps.shield) {
        obstacles.value = obstacles.value.filter(o => o !== obstacle)
        shieldEffect.value.play()
        return false
      }
      return true
    }
    return false
  })
}

// 通用碰撞检测函数
const checkCollisionWithDino = (object: { x: number, y: number, width: number, height: number }) => {
  const dinoBox = {
    left: dino.value.x + config.collisionBuffer,
    right: dino.value.x + getCurrentConfig().dinoWidth - config.collisionBuffer,
    top: dino.value.y + config.collisionBuffer,
    bottom: dino.value.y + getCurrentConfig().dinoHeight - config.collisionBuffer
  }

  const objectBox = {
    left: object.x + config.collisionBuffer,
    right: object.x + object.width - config.collisionBuffer,
    top: object.y + config.collisionBuffer,
    bottom: object.y + object.height - config.collisionBuffer
  }

  return (
    dinoBox.left < objectBox.right &&
    dinoBox.right > objectBox.left &&
    dinoBox.top < objectBox.bottom &&
    dinoBox.bottom > objectBox.top
  )
}

// 更新恐龙位置
const updateDino = () => {
  if (!gameCanvas.value) return

  // 处理加速和减速效果
  let speedMultiplier = 1
  if (dino.value.isAccelerating && dino.value.accelerationStartTime) {
    const accelerationTime = Date.now() - dino.value.accelerationStartTime
    if (accelerationTime >= config.accelerationDelay) {
      speedMultiplier = Math.min(
        config.maxAccelerationMultiplier,
        1 + ((accelerationTime - config.accelerationDelay) / 2000) * config.accelerationGrowthRate
      )
    }
  }

  // 应用减速效果
  if (gameState.value.activePowerUps.slowMotion) {
    speedMultiplier *= config.powerUps.slowMotion.slowFactor
  }

  if (dino.value.isJumping) {
    // 更新垂直速度和位置
    dino.value.velocityY += getCurrentConfig().gravity

    // 在上升过程中使用较小的重力
    if (dino.value.velocityY < 0) {
      dino.value.velocityY += getCurrentConfig().gravity * 0.8 // 上升时重力减小20%
    }

    // 限制最大下落速度
    if (dino.value.velocityY > getCurrentConfig().maxFallingSpeed) {
      dino.value.velocityY = getCurrentConfig().maxFallingSpeed
    }

    dino.value.y += dino.value.velocityY

    // 更新跳跃状态
    if (dino.value.velocityY > 0) {
      dino.value.jumpState = 'falling'
    }

    // 检查最大高度
    const maxHeight = gameCanvas.value.height - getCurrentConfig().dinoHeight - config.groundHeight - getCurrentConfig().maxJumpHeight
    if (dino.value.y < maxHeight) {
      dino.value.y = maxHeight
      dino.value.velocityY = 0
      dino.value.jumpState = 'falling'
    }

    // 检查是否落地
    if (dino.value.y >= gameCanvas.value.height - getCurrentConfig().dinoHeight - config.groundHeight) {
      dino.value.y = gameCanvas.value.height - getCurrentConfig().dinoHeight - config.groundHeight
      dino.value.isJumping = false
      dino.value.jumpState = 'idle'
      dino.value.velocityY = 0
      dino.value.jumpCount = 0 // 重置跳跃次数
      dino.value.canJump = true // 立即允许下一次跳跃
    }
  }

  // 应用加速效果到障碍物移动速度
  obstacles.value.forEach(obstacle => {
    obstacle.speed *= speedMultiplier
  })
}

// 游戏主循环
const draw = () => {
  if (!ctx.value || !gameCanvas.value || isPaused.value) return

  // 清空画布
  ctx.value.clearRect(0, 0, gameCanvas.value.width, gameCanvas.value.height)

  // 更新游戏状态
  updateDino()
  updateObstacles()
  updatePowerUps()

  // 检查碰撞
  if (checkCollision()) {
    handleGameOver()
    return
  }

  // 绘制游戏元素
  drawGround()
  drawPowerUps()
  drawDino()
  drawObstacles()

  // 更新分数
  score.value++

  if (isPlaying.value) {
    requestAnimationFrame(draw)
  }
}

// 恢复道具生成函数
const lastPowerUpTime = ref(0)
const minPowerUpInterval = 3000 // 道具之间至少间隔3秒

const generatePowerUp = () => {
  if (!gameCanvas.value) return

  const currentTime = Date.now()
  if (currentTime - lastPowerUpTime.value < minPowerUpInterval) {
    return
  }

  Object.entries(config.powerUps).forEach(([type, settings]) => {
    if (Math.random() < settings.probability) {
      const powerUp: PowerUp = {
        x: gameCanvas.value!.width + Math.random() * 100,
        y: Math.random() * (gameCanvas.value!.height * 0.6),
        width: 30,
        height: 30,
        type: type as 'shield' | 'magnet' | 'doubleScore' | 'clear' | 'slowMotion',
        active: false,
        duration: (settings as any).duration || 0,
        color: settings.color,
        collected: false
      }
      powerUps.value.push(powerUp)
      lastPowerUpTime.value = currentTime
    }
  })
}

// 修改组件挂载代码，添加passive选项
onMounted(() => {
  if (!gameCanvas.value) return

  ctx.value = gameCanvas.value.getContext('2d')
  resizeCanvas()
  checkIsMobile() // 初始检查设备类型

  // 添加事件监听
  window.addEventListener('resize', () => {
    resizeCanvas()
    checkIsMobile()
  })

  // PC端键盘事件
  if (!checkIsMobile()) {
    window.addEventListener('keydown', handleKeyDown)
    window.addEventListener('keyup', handleKeyUp)
  }

  // 阻止移动端默认滚动行为
  document.body.addEventListener('touchmove', (e) => {
    if (e.target && (e.target as HTMLElement).closest('.mobile-controls')) {
      e.preventDefault()
    }
  }, { passive: false })
})

// 修改卸载逻辑
onUnmounted(() => {
  if (timeInterval.value) {
    clearInterval(timeInterval.value)
  }

  window.removeEventListener('resize', resizeCanvas)
  if (!checkIsMobile()) {
    window.removeEventListener('keydown', handleKeyDown)
    window.removeEventListener('keyup', handleKeyUp)
  }
})

// 修改移动端检测逻辑
const checkIsMobile = () => {
  const result = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) || window.innerWidth <= 768
  isMobile.value = result
  return result
}

// 修改handleJump函数，处理passive事件
const handleJump = (e?: TouchEvent | KeyboardEvent | null) => {
  if (!isPlaying.value || isPaused.value) return

  if (e) {
    if (e instanceof TouchEvent) {
      // 不再检查触摸位置，任何触摸都可以触发跳跃
      e.preventDefault()
    } else {
      e.preventDefault()
    }
  }

  const currentTime = Date.now()
  const timeSinceLastJump = currentTime - dino.value.lastJumpTime

  // 修改跳跃逻辑
  if (!dino.value.isJumping && dino.value.canJump) {
    // 正常跳跃
    performJump('normal')
  } else if (dino.value.jumpCount < dino.value.maxJumpCount &&
    timeSinceLastJump <= config.doubleTapDelay) {
    // 二段跳时给予更大的上升速度
    dino.value.velocityY = Math.min(dino.value.velocityY, 0) // 确保向上的速度
    performJump('double')
  }
}

// 添加新的跳跃函数
const performJump = (type: 'normal' | 'high' | 'double') => {
  const currentConfig = getCurrentConfig()
  const currentTime = Date.now()

  let jumpForce = 0
  switch (type) {
    case 'normal':
      jumpForce = currentConfig.normalJumpForce
      break
    case 'high':
      jumpForce = currentConfig.highJumpForce
      break
    case 'double':
      jumpForce = currentConfig.doubleJumpForce
      // 二段跳时完全重置向上速度，使效果更明显
      dino.value.velocityY = -jumpForce
      break
  }

  dino.value.isJumping = true
  dino.value.jumpState = 'rising'

  if (type !== 'double') {
    dino.value.velocityY = -jumpForce
  }

  dino.value.lastJumpTime = currentTime
  dino.value.jumpCount++

  if (type === 'normal' || type === 'high') {
    dino.value.canJump = false
  }

  // 播放跳跃音效
  if (type === 'double') {
    // 二段跳使用更高的音量
    jumpSound.volume = 0.4
  } else {
    jumpSound.volume = 0.3
  }
  playSound(jumpSound)

  // 重置跳跃冷却
  setTimeout(() => {
    if (type !== 'double') {
      dino.value.canJump = true
    }
  }, config.jumpCooldown)
}

// 添加加速相关函数
const handleAccelerationStart = () => {
  if (!dino.value.isAccelerating) {
    dino.value.isAccelerating = true
    dino.value.accelerationStartTime = Date.now()
  }
}

const handleAccelerationEnd = () => {
  dino.value.isAccelerating = false
  dino.value.accelerationStartTime = null
}

// 更新键盘事件处理
const handleKeyDown = (e: KeyboardEvent) => {
  if (isPlaying.value && !isPaused.value) {
    if (e.code === 'Space' || e.code === 'ArrowUp') {
      e.preventDefault()
      handleJump(e)
    } else if (e.code === 'ArrowRight') {
      e.preventDefault()
      handleAccelerationStart()
    }
  }
}

const handleKeyUp = (e: KeyboardEvent) => {
  if (e.code === 'ArrowRight') {
    handleAccelerationEnd()
  }
}

// 处理游戏结束
const handleGameOver = () => {
  isPlaying.value = false
  gameOver.value = true
  clearInterval(timeInterval.value!)
  playSound(gameOverSound)
  updateBestScore()
}

// 更新最高分
const updateBestScore = () => {
  if (score.value > bestScore.value) {
    bestScore.value = score.value
    localStorage.setItem('dinoGameBestScore', score.value.toString())
  }
}

// 开始游戏
const startGame = () => {
  if (!gameCanvas.value) return

  isPlaying.value = true
  isPaused.value = false
  gameOver.value = false
  score.value = 0
  lives.value = 1
  gameTime.value = 0
  obstacles.value = []
  powerUps.value = []
  coins.value = []

  // 重置所有道具状态
  gameState.value.activePowerUps = {
    shield: false,
    magnet: false,
    doubleScore: false,
    slowMotion: false    // 添加减速状态
  }
  gameState.value.powerUpTimers = {
    shield: 0,
    magnet: 0,
    doubleScore: 0,
    slowMotion: 0        // 添加减速计时器
  }

  // 初始化恐龙位置
  initDino()

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

  gameCanvas.value.width = container.clientWidth
  gameCanvas.value.height = container.clientHeight

  // 重置恐龙位置
  if (isPlaying.value) {
    initDino()
  }
}

// 生成障碍物时的垂直位置计算
const calculateObstacleY = (selectedType: any, gameCanvas: HTMLCanvasElement) => {
  if (selectedType.flying) {
    // 飞行物体（如鸟）在上半部分随机高度
    return Math.random() * (gameCanvas.height * 0.6) + gameCanvas.height * 0.1
  } else {
    // 地面物体
    return gameCanvas.height - selectedType.height - config.groundHeight
  }
}

// 修改生成金币的函数
const generateCoins = () => {
  if (!gameCanvas.value) return

  if (Math.random() < 0.08) { // 降低金币生成概率
    const maxHeight = gameCanvas.value.height - getCurrentConfig().maxJumpHeight
    const minHeight = gameCanvas.value.height - getCurrentConfig().maxJumpHeight * 1.2

    // 随机决定是生成单个金币还是金币组
    const isGroup = Math.random() < 0.3 // 30%概率生成金币组

    if (isGroup) {
      // 生成3-5个金币的组合
      const coinCount = Math.floor(Math.random() * 3) + 3
      const baseX = gameCanvas.value.width + Math.random() * 100
      const baseY = Math.random() * (maxHeight - minHeight) + minHeight

      for (let i = 0; i < coinCount; i++) {
        coins.value.push({
          x: baseX + i * 30, // 横向间隔30像素
          y: baseY + Math.sin(i * Math.PI / 2) * 20, // 使用正弦函数创建波浪形状
          width: 20,
          height: 20,
          collected: false,
          magnetized: false,
          value: config.coins.value
        })
      }
    } else {
      // 生成单个金币
      coins.value.push({
        x: gameCanvas.value.width + Math.random() * 100,
        y: Math.random() * (maxHeight - minHeight) + minHeight,
        width: 20,
        height: 20,
        collected: false,
        magnetized: false,
        value: config.coins.value
      })
    }
  }
}

// 更新道具状态
const updatePowerUps = () => {
  const currentTime = Date.now()

  // 更新道具位置和状态
  powerUps.value = powerUps.value.filter(powerUp => {
    if (!powerUp.collected) {
      powerUp.x -= getCurrentConfig().initialGameSpeed * 2

      // 检查是否被收集
      if (checkCollisionWithDino(powerUp)) {
        collectPowerUp(powerUp)
        return false
      }

      return powerUp.x + powerUp.width > 0
    }
    return true
  })

  // 更新金币位置和状态
  coins.value = coins.value.filter(coin => {
    if (!coin.collected) {
      if (gameState.value.activePowerUps.magnet && !coin.magnetized) {
        // 计算与恐龙的距离
        const dx = (dino.value.x + getCurrentConfig().dinoWidth / 2) - (coin.x + coin.width / 2)
        const dy = (dino.value.y + getCurrentConfig().dinoHeight / 2) - (coin.y + coin.height / 2)
        const distance = Math.sqrt(dx * dx + dy * dy)

        if (distance < config.powerUps.magnet.range) {
          coin.magnetized = true
        }
      }

      if (coin.magnetized) {
        // 向恐龙移动
        const speed = 5
        const dx = dino.value.x - coin.x
        const dy = dino.value.y - coin.y
        const angle = Math.atan2(dy, dx)
        coin.x += Math.cos(angle) * speed
        coin.y += Math.sin(angle) * speed
      } else {
        coin.x -= getCurrentConfig().initialGameSpeed * 2
      }

      // 检查是否被收集
      if (checkCollisionWithDino(coin)) {
        collectCoin(coin)
        return false
      }

      return coin.x + coin.width > 0
    }
    return false
  })

  // 更新道具持续时间
  Object.entries(gameState.value.powerUpTimers).forEach(([type, endTime]) => {
    if (endTime > 0 && currentTime >= endTime) {
      deactivatePowerUp(type as keyof typeof gameState.value.activePowerUps)
    }
  })

  // 生成新的道具和金币
  generatePowerUp()
  generateCoins()
}

// 收集道具
const collectPowerUp = (powerUp: PowerUp) => {
  powerUp.collected = true
  powerUpEffect.value.play()

  switch (powerUp.type) {
    case 'shield':
      activateShield()
      break
    case 'magnet':
      activateMagnet()
      break
    case 'doubleScore':
      activateDoubleScore()
      break
    case 'slowMotion':   // 添加减速道具处理
      activateSlowMotion()
      break
    case 'clear':
      clearObstacles()
      break
  }
}

// 收集金币
const collectCoin = (coin: Coin) => {
  coin.collected = true
  coinEffect.value.play()

  // 更新金币计数
  gameState.value.coinsCollected++

  // 如果有双倍积分道具，金币价值翻倍
  const value = gameState.value.activePowerUps.doubleScore ? coin.value * 2 : coin.value
  score.value += value
}

// 激活护盾
const activateShield = () => {
  gameState.value.activePowerUps.shield = true
  gameState.value.powerUpTimers.shield = Date.now() + config.powerUps.shield.duration
  shieldEffect.value.play()
}

// 激活磁铁
const activateMagnet = () => {
  gameState.value.activePowerUps.magnet = true
  gameState.value.powerUpTimers.magnet = Date.now() + config.powerUps.magnet.duration
}

// 激活双倍积分
const activateDoubleScore = () => {
  gameState.value.activePowerUps.doubleScore = true
  gameState.value.powerUpTimers.doubleScore = Date.now() + config.powerUps.doubleScore.duration
}

// 清除障碍物
const clearObstacles = () => {
  clearEffect.value.play()
  obstacles.value = []
}

// 绘制道具
const drawPowerUps = () => {
  if (!ctx.value) return

  // 绘制金币
  coins.value.forEach(coin => {
    if (!coin.collected) {
      ctx.value!.save()
      ctx.value!.fillStyle = config.coins.color
      ctx.value!.beginPath()
      ctx.value!.arc(
        coin.x + coin.width / 2,
        coin.y + coin.height / 2,
        coin.width / 2,
        0,
        Math.PI * 2
      )
      ctx.value!.fill()
      ctx.value!.restore()
    }
  })

  // 绘制道具
  powerUps.value.forEach(powerUp => {
    if (!powerUp.collected) {
      ctx.value!.save()
      if (powerUp.type === 'slowMotion') {
        // 使用 slow.png 图片
        if (slowImage.complete) {
          ctx.value!.drawImage(slowImage, powerUp.x, powerUp.y, powerUp.width, powerUp.height)
        } else {
          // 图片未加载时用圆形占位
          ctx.value!.fillStyle = powerUp.color
          ctx.value!.beginPath()
          ctx.value!.arc(powerUp.x + powerUp.width/2, powerUp.y + powerUp.height/2, powerUp.width/2, 0, Math.PI * 2)
          ctx.value!.fill()
        }
      } else {
        ctx.value!.fillStyle = powerUp.color
        // 根据道具类型绘制不同的图形
        switch (powerUp.type) {
          case 'shield':
            drawShieldPowerUp(powerUp)
            break
          case 'magnet':
            drawMagnetPowerUp(powerUp)
            break
          case 'doubleScore':
            drawDoubleScorePowerUp(powerUp)
            break
          case 'clear':
            drawClearPowerUp(powerUp)
            break
        }
      }
      ctx.value!.restore()
    }
  })

  // 如果护盾处于激活状态，绘制护盾效果
  if (gameState.value.activePowerUps.shield) {
    drawShieldEffect()
  }
}

// 添加图片资源加载
const shieldImage = new Image()
const magnetImage = new Image()
shieldImage.src = '/game_pictures/shield.png'
magnetImage.src = '/game_pictures/magnet.png'

// 添加 slowImage 资源加载
const slowImage = new Image()
slowImage.src = '/game_pictures/slow.png'

// 修改绘制护盾道具的函数
const drawShieldPowerUp = (powerUp: PowerUp) => {
  if (!ctx.value) return

  const x = powerUp.x
  const y = powerUp.y
  const size = powerUp.width

  // 如果图片已加载完成，使用图片
  if (shieldImage.complete) {
    ctx.value.drawImage(shieldImage, x, y, size, size)
  } else {
    // 如果图片未加载完成，使用备用的绘制方式
    ctx.value.beginPath()
    ctx.value.arc(x + size/2, y + size/2, size/2, 0, Math.PI * 2)
    ctx.value.stroke()
    ctx.value.beginPath()
    ctx.value.moveTo(x + size/4, y + size/2)
    ctx.value.lineTo(x + size*3/4, y + size/2)
    ctx.value.moveTo(x + size/2, y + size/4)
    ctx.value.lineTo(x + size/2, y + size*3/4)
    ctx.value.stroke()
  }
}

// 修改绘制磁铁道具的函数
const drawMagnetPowerUp = (powerUp: PowerUp) => {
  if (!ctx.value) return

  const x = powerUp.x
  const y = powerUp.y
  const size = powerUp.width

  // 如果图片已加载完成，使用图片
  if (magnetImage.complete) {
    ctx.value.drawImage(magnetImage, x, y, size, size)
  } else {
    // 如果图片未加载完成，使用备用的绘制方式
    ctx.value.beginPath()
    ctx.value.arc(x + size/4, y + size/2, size/6, 0, Math.PI * 2)
    ctx.value.arc(x + size*3/4, y + size/2, size/6, 0, Math.PI * 2)
    ctx.value.fill()
  }
}

// 修改绘制护盾效果的函数，使其更加明显
const drawShieldEffect = () => {
  if (!ctx.value) return

  const x = dino.value.x + getCurrentConfig().dinoWidth / 2
  const y = dino.value.y + getCurrentConfig().dinoHeight / 2
  const radius = Math.max(getCurrentConfig().dinoWidth, getCurrentConfig().dinoHeight) * 0.8

  // 创建更明显的渐变效果
  const gradient = ctx.value.createRadialGradient(x, y, radius * 0.6, x, y, radius)
  gradient.addColorStop(0, 'rgba(64, 196, 255, 0.2)')
  gradient.addColorStop(0.6, 'rgba(64, 196, 255, 0.15)')
  gradient.addColorStop(1, 'rgba(64, 196, 255, 0.1)')

  // 绘制护盾
  ctx.value.save()

  // 绘制主要护盾效果
  ctx.value.beginPath()
  ctx.value.arc(x, y, radius, 0, Math.PI * 2)
  ctx.value.fillStyle = gradient
  ctx.value.fill()

  // 添加更明显的边缘效果
  const time = Date.now() / 1000
  const flashIntensity = Math.sin(time * 5) * 0.4 + 0.6
  ctx.value.strokeStyle = `rgba(64, 196, 255, ${flashIntensity})`
  ctx.value.lineWidth = 3
  ctx.value.stroke()

  // 添加内部光环效果
  ctx.value.beginPath()
  ctx.value.arc(x, y, radius * 0.7, 0, Math.PI * 2)
  ctx.value.strokeStyle = `rgba(64, 196, 255, ${flashIntensity * 0.5})`
  ctx.value.lineWidth = 2
  ctx.value.stroke()

  ctx.value.restore()
}

// 绘制双倍积分道具
const drawDoubleScorePowerUp = (powerUp: PowerUp) => {
  if (!ctx.value) return

  const x = powerUp.x + powerUp.width / 2
  const y = powerUp.y + powerUp.height / 2
  const radius = powerUp.width / 2

  // 绘制圆形背景
  ctx.value.beginPath()
  ctx.value.arc(x, y, radius, 0, Math.PI * 2)
  ctx.value.fill()

  // 绘制 "x2" 文字
  ctx.value.fillStyle = '#ffffff'
  ctx.value.font = '16px Arial'
  ctx.value.textAlign = 'center'
  ctx.value.textBaseline = 'middle'
  ctx.value.fillText('x2', x, y)
}

// 绘制清除道具
const drawClearPowerUp = (powerUp: PowerUp) => {
  if (!ctx.value) return

  const x = powerUp.x + powerUp.width / 2
  const y = powerUp.y + powerUp.height / 2
  const radius = powerUp.width / 2

  // 绘制爆炸形状
  ctx.value.beginPath()
  for (let i = 0; i < 8; i++) {
    const angle = (i / 8) * Math.PI * 2
    const innerRadius = radius / 2
    const outerRadius = radius

    ctx.value.moveTo(
      x + Math.cos(angle) * innerRadius,
      y + Math.sin(angle) * innerRadius
    )
    ctx.value.lineTo(
      x + Math.cos(angle) * outerRadius,
      y + Math.sin(angle) * outerRadius
    )
  }
  ctx.value.stroke()
}

// 取消激活道具
const deactivatePowerUp = (type: keyof typeof gameState.value.activePowerUps) => {
  gameState.value.activePowerUps[type] = false
  gameState.value.powerUpTimers[type] = 0
}

// 添加教程状态
const isTutorialVisible = ref(false)

// 添加教程相关方法
const showTutorial = () => {
  isTutorialVisible.value = true
  if (isPlaying.value && !isPaused.value) {
    pauseGame()
  }
}

const closeTutorial = () => {
  isTutorialVisible.value = false
}

// 添加计时器样式计算函数
const getTimerStyle = (type: keyof typeof gameState.value.activePowerUps) => {
  const currentTime = Date.now()
  const endTime = gameState.value.powerUpTimers[type]
  const duration = config.powerUps[type].duration

  if (endTime && endTime > currentTime) {
    const remaining = endTime - currentTime
    const percentage = (remaining / duration) * 100
    return {
      width: `${percentage}%`
    }
  }
  return { width: '0%' }
}

// 添加减速道具激活函数
const activateSlowMotion = () => {
  gameState.value.activePowerUps.slowMotion = true
  gameState.value.powerUpTimers.slowMotion = Date.now() + config.powerUps.slowMotion.duration
  powerUpEffect.value.play()
}

// 添加减速道具的绘制函数
const drawSlowMotionPowerUp = (powerUp: PowerUp) => {
  if (!ctx.value) return

  const x = powerUp.x + powerUp.width / 2
  const y = powerUp.y + powerUp.height / 2
  const radius = powerUp.width / 2

  // 绘制表盘外圈
  ctx.value.beginPath()
  ctx.value.arc(x, y, radius, 0, Math.PI * 2)
  ctx.value.fill()

  // 绘制表盘指针
  const time = Date.now() / 1000
  ctx.value.strokeStyle = '#ffffff'
  ctx.value.lineWidth = 2
  ctx.value.beginPath()
  ctx.value.moveTo(x, y)
  ctx.value.lineTo(
    x + Math.cos(time * Math.PI) * radius * 0.6,
    y + Math.sin(time * Math.PI) * radius * 0.6
  )
  ctx.value.stroke()

  // 绘制中心点
  ctx.value.fillStyle = '#ffffff'
  ctx.value.beginPath()
  ctx.value.arc(x, y, 3, 0, Math.PI * 2)
  ctx.value.fill()
}
</script>

<style scoped>
.dino-game-page {
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
  height: 400px;
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
    height: 300px;
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
    height: 250px;
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

/* 移动端控制按钮样式 */
.mobile-controls {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  padding-bottom: max(20px, env(safe-area-inset-bottom, 34px));
  background: linear-gradient(to bottom, transparent, rgba(143, 122, 102, 0.95));
  backdrop-filter: blur(10px);
  z-index: 1000;
  touch-action: none;
}

.mobile-controls-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 600px;
  margin: 0 auto;
  padding: 10px 20px;
}

.mobile-controls-left,
.mobile-controls-right {
  flex: 1;
  display: flex;
  justify-content: center;
}

.mobile-btn {
  -webkit-tap-highlight-color: transparent;
  user-select: none;
  touch-action: manipulation;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0;
}

.mobile-btn:active {
  transform: scale(0.95);
  opacity: 0.8;
  background: rgba(255, 255, 255, 0.3);
}

.jump-btn {
  background: rgba(76, 175, 80, 0.3);
}

.accelerate-btn {
  background: rgba(255, 193, 7, 0.3);
}

.btn-label {
  font-size: 14px;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  margin-top: 4px;
}

.power-up-timers {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 10;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.timer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.5);
  padding: 4px 8px;
  border-radius: 12px;
}

.timer-icon {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.timer-icon:not(img) {
  background: #ffd700;
  color: #000;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.timer-bar {
  width: 100px;
  height: 6px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  overflow: hidden;
}

.timer-progress {
  height: 100%;
  background: #4CAF50;
  transition: width 0.1s linear;
}

/* 添加教程按钮样式 */
.tutorial-button {
  position: fixed;
  top: 80px; /* 调整顶部距离，避免被导航栏遮挡 */
  right: 20px;
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.tutorial-button-mobile {
  top: 60px;
  right: 10px;
  width: 36px;
  height: 36px;
}

/* 教程弹窗样式 */
.tutorial-modal {
  max-width: 500px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  padding: 30px;
  background: #faf8ef;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.tutorial-title {
  font-size: 28px;
  color: #776e65;
  margin-bottom: 20px;
  text-align: center;
}

.tutorial-content {
  color: #776e65;
}

.tutorial-section {
  margin-bottom: 20px;
}

.tutorial-section h4 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #8f7a66;
}

.tutorial-section ul {
  list-style-type: none;
  padding-left: 0;
}

.tutorial-section li {
  margin: 8px 0;
  padding-left: 20px;
  position: relative;
}

.tutorial-section li:before {
  content: "•";
  position: absolute;
  left: 0;
  color: #8f7a66;
}

.tutorial-footer {
  margin-top: 20px;
  text-align: center;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .tutorial-button {
    top: 10px;
    right: 10px;
    width: 36px;
    height: 36px;
  }

  .tutorial-button .anticon {
    font-size: 20px;
  }

  .tutorial-modal {
    padding: 20px;
  }

  .tutorial-title {
    font-size: 24px;
  }

  .tutorial-section h4 {
    font-size: 16px;
  }

  .tutorial-section li {
    font-size: 14px;
  }
}

/* 确保移动端按钮样式正确 */
.mobile-controls {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  padding-bottom: max(20px, env(safe-area-inset-bottom, 34px));
  background: linear-gradient(to bottom, transparent, rgba(143, 122, 102, 0.95));
  backdrop-filter: blur(10px);
  z-index: 1000;
  touch-action: none;
}

.mobile-btn {
  -webkit-tap-highlight-color: transparent;
  user-select: none;
  touch-action: manipulation;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0;
}

.mobile-btn:active {
  transform: scale(0.95);
  opacity: 0.8;
  background: rgba(255, 255, 255, 0.3);
}

.jump-btn {
  background: rgba(76, 175, 80, 0.3);
}

.accelerate-btn {
  background: rgba(255, 193, 7, 0.3);
}

/* 确保移动端按钮在正确的位置 */
.mobile-controls-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 600px;
  margin: 0 auto;
  padding: 10px 20px;
}

.mobile-controls-left,
.mobile-controls-right {
  flex: 1;
  display: flex;
  justify-content: center;
}

.btn-label {
  font-size: 14px;
  color: #ffffff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  margin-top: 4px;
}
</style>
