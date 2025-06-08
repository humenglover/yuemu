<template>
  <div class="tetris-page">
    <!-- 添加背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="game-container">
      <!-- PC端布局 -->
      <template v-if="!isMobile">
        <div class="game-header">
          <h2 class="game-title">俄罗斯方块</h2>
          <div class="scores-container">
            <div class="score-box">
              <div class="score-label">当前分数</div>
              <div class="score-value">{{ score }}</div>
            </div>
            <div class="score-box">
              <div class="score-label">最高分数</div>
              <div class="score-value highlight">{{ highScore }}</div>
            </div>
            <div class="score-box">
              <div class="score-label">当前等级</div>
              <div class="score-value">{{ level }}</div>
            </div>
          </div>
        </div>

        <div class="game-controls">
          <div class="control-button" @click="startGame">
            {{ isPlaying ? '重新开始' : '开始游戏' }}
          </div>
          <div class="control-button" @click="pauseGame">
            {{ isPaused ? '继续' : '暂停' }}
          </div>
          <div class="control-button sound-button" @click="toggleSound">
            <component :is="isSoundEnabled ? 'sound-filled' : 'sound-outlined'" />
            {{ isSoundEnabled ? '关闭音效' : '开启音效' }}
          </div>
        </div>
      </template>

      <!-- 移动端顶部控制栏 -->
      <div v-if="isMobile" class="mobile-top-bar">
        <div class="mobile-back-btn" @click="goBack">
          <arrow-left-outlined />
        </div>
        <div class="mobile-controls-left">
          <div class="mobile-score-item">
            <star-outlined />
            <span>{{ score }}</span>
          </div>
          <div class="mobile-score-item">
            <trophy-outlined />
            <span>{{ highScore }}</span>
          </div>
          <div class="mobile-score-item">
            <dashboard-outlined />
            <span>{{ level }}</span>
          </div>
        </div>
        <div class="mobile-controls-right">
          <div class="mobile-control-item" @click="startGame">
            <play-circle-filled v-if="!isPlaying" />
            <redo-outlined v-else />
          </div>
          <div class="mobile-control-item" @click="pauseGame">
            <play-circle-filled v-if="isPaused" />
            <pause-circle-filled v-else />
          </div>
          <div class="mobile-control-item sound-control" @click="toggleSound">
            <sound-filled v-if="isSoundEnabled" />
            <sound-outlined v-else />
          </div>
        </div>
      </div>

      <div class="game-main">
        <!-- 游戏区域 -->
        <div class="game-board" ref="boardRef">
          <div class="grid-background">
            <div v-for="row in BOARD_ROWS" :key="'grid-row-' + row" class="grid-row">
              <div v-for="col in BOARD_COLS" :key="'grid-cell-' + row + '-' + col" class="grid-cell"></div>
            </div>
          </div>
          <!-- 渲染固定的方块 -->
          <div v-for="(row, rowIndex) in board"
               :key="'row-' + rowIndex"
               class="board-row">
            <div v-for="(cell, colIndex) in row"
                 :key="'cell-' + rowIndex + '-' + colIndex"
                 class="board-cell"
                 :class="{ filled: cell !== 0 }"
                 :style="{ '--cell-gradient': cell !== 0 ? colors[cell - 1] : 'none' }">
            </div>
          </div>
          <!-- 渲染当前下落的方块 -->
          <div v-for="(point, index) in currentShapePoints"
               :key="'current-' + index"
               class="block"
               :style="{
                 '--row': point.row,
                 '--col': point.col,
                 '--block-gradient': point.color,
                 display: point.row >= 0 ? 'block' : 'none'
               }">
          </div>
        </div>

        <!-- PC端游戏信息 -->
        <div v-if="!isMobile" class="game-info">
          <div class="next-shape">
            <h3>下一个方块</h3>
            <div class="next-shape-preview">
              <div class="preview-grid">
                <div v-for="row in 4" :key="'preview-row-' + row" class="preview-row">
                  <div v-for="col in 4" :key="'preview-cell-' + row + '-' + col" class="preview-cell"></div>
                </div>
                <div v-for="(point, index) in nextShapePoints"
                     :key="'next-' + index"
                     class="preview-block"
                     :style="{
                       left: point.col * previewCellSize + 'px',
                       top: point.row * previewCellSize + 'px',
                       '--block-gradient': point.color
                     }">
                </div>
              </div>
            </div>
          </div>

          <!-- PC端操作说明 -->
          <div class="controls-guide" v-if="!isMobile">
            <h3>操作说明</h3>
            <ul>
              <li>← → : 左右移动</li>
              <li>↑ : 旋转</li>
              <li>↓ : 加速下落</li>
              <li>空格 : 暂停/继续</li>
            </ul>
          </div>
        </div>
      </div>

      <!-- 移动端控制按钮 -->
      <div class="mobile-controls" v-if="isMobile">
        <div class="mobile-controls-container">
          <button class="mobile-btn" @click="moveShape(0, -1)">
            <left-outlined />
          </button>
          <div class="mobile-controls-center">
            <button class="mobile-btn rotate-btn" @click="rotateShape">
              <rotate-right-outlined />
            </button>
            <button class="mobile-btn" @click="moveShape(1, 0)">
              <down-outlined />
            </button>
          </div>
          <button class="mobile-btn" @click="moveShape(0, 1)">
            <right-outlined />
          </button>
        </div>
      </div>
    </div>

    <!-- 游戏结束弹窗 -->
    <div v-if="isGameOver" class="modal-overlay">
      <div class="modal-content game-over-modal">
        <h3 class="game-over-title">游戏结束</h3>
        <div class="game-over-stats">
          <div class="stat-item">
            <div class="stat-label">最终得分</div>
            <div class="stat-value highlight">{{ score }}</div>
          </div>
        </div>
        <div class="game-over-buttons">
          <div class="control-button" @click="startGame">再来一局</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, type Ref } from 'vue'
import {
  StarOutlined,
  TrophyOutlined,
  DashboardOutlined,
  PlayCircleFilled,
  PauseCircleFilled,
  RedoOutlined,
  QuestionCircleOutlined,
  RotateRightOutlined,
  LeftOutlined,
  RightOutlined,
  DownOutlined,
  ArrowLeftOutlined,
  SoundOutlined,
  SoundFilled
} from '@ant-design/icons-vue'

// 游戏常量
const BOARD_ROWS = 20
const BOARD_COLS = 10
const INITIAL_SPEED = 600
const SPEED_INCREMENT = 100
const LEVEL_SCORE_THRESHOLD = 800

// 响应式变量
const isMobile = ref(false)
const cellSize = ref(30)
const previewCellSize = ref(25)
const isSoundEnabled = ref(true)

// 检测是否为移动端并调整大小的函数
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  // 根据屏幕宽度调整格子大小
  if (isMobile.value) {
    const screenWidth = window.innerWidth
    const screenHeight = window.innerHeight
    const maxBoardWidth = Math.min(screenWidth - 20, 300)
    const maxBoardHeight = screenHeight - 200
    const cellSizeFromWidth = Math.floor(maxBoardWidth / BOARD_COLS)
    const cellSizeFromHeight = Math.floor(maxBoardHeight / BOARD_ROWS)
    cellSize.value = Math.floor(Math.min(cellSizeFromWidth, cellSizeFromHeight) / 2) * 2
  } else {
    cellSize.value = 30
  }
  // 统一设置CSS变量
  document.documentElement.style.setProperty('--cell-size', `${cellSize.value}px`)
  previewCellSize.value = Math.floor(cellSize.value * 0.8)
}

// 创建空游戏板函数
function createEmptyBoard(): number[][] {
  var board = new Array(BOARD_ROWS);
  for (var i = 0; i < BOARD_ROWS; i++) {
    board[i] = new Array(BOARD_COLS);
    for (var j = 0; j < BOARD_COLS; j++) {
      board[i][j] = 0;
    }
  }
  return board;
}

interface GameState {
  board: number[][]
  colors: string[]
  score: number
  highScore: number
}

interface Point {
  row: number
  col: number
  color: string
}

const gameState = ref<GameState>({
  board: createEmptyBoard(),
  colors: [
    'linear-gradient(135deg, #FF416C, #FF4B2B)', // 红色渐变 - I
    'linear-gradient(135deg, #FFD700, #FFA500)', // 金色渐变 - O
    'linear-gradient(135deg, #8E2DE2, #4A00E0)', // 紫色渐变 - T
    'linear-gradient(135deg, #FF8008, #FFA500)', // 橙色渐变 - L
    'linear-gradient(135deg, #2193b0, #6dd5ed)', // 蓝色渐变 - J
    'linear-gradient(135deg, #00b09b, #96c93d)', // 绿色渐变 - S
    'linear-gradient(135deg, #FF69B4, #FF1493)'  // 粉色渐变 - Z
  ],
  score: 0,
  highScore: 0
})

// 更新所有引用以使用gameState
const board = computed(() => gameState.value.board)
const colors = computed(() => gameState.value.colors)
const score = computed({
  get: () => gameState.value.score,
  set: (val) => gameState.value.score = val
})
const highScore = computed({
  get: () => gameState.value.highScore,
  set: (val) => gameState.value.highScore = val
})

// 方块形状定义 - 调整为从左上角开始的标准坐标
const shapes = [
  // I
  [[0, 0], [0, 1], [0, 2], [0, 3]],
  // O
  [[0, 0], [0, 1], [1, 0], [1, 1]],
  // T
  [[0, 1], [1, 0], [1, 1], [1, 2]],
  // L
  [[0, 2], [1, 0], [1, 1], [1, 2]],
  // J
  [[0, 0], [1, 0], [1, 1], [1, 2]],
  // S
  [[0, 1], [0, 2], [1, 0], [1, 1]],
  // Z
  [[0, 0], [0, 1], [1, 1], [1, 2]]
]

const level = ref<number>(1)
const speed = ref<number>(INITIAL_SPEED)
const isPlaying = ref<boolean>(false)
const isPaused = ref<boolean>(false)
const isGameOver = ref<boolean>(false)
const gameInterval = ref<number | null>(null)

// 当前方块状态
const currentShape = ref<number[][]>([])
const currentShapeIndex = ref<number>(0)
const nextShapeIndex = ref<number>(0)
const currentPos = ref<[number, number]>([-2, 0])

// 渲染当前方块的点
const currentShapePoints = computed<Point[]>(() => {
  if (!currentShape.value) return []
  return currentShape.value.map(([row, col]) => ({
    row: Math.round(row + currentPos.value[0]),
    col: Math.round(col + currentPos.value[1]),
    color: colors.value[currentShapeIndex.value]
  })).filter(point => point.row >= 0)
})

// 计算下一个方块的预览坐标点
const nextShapePoints = computed(() => {
  const shape = shapes[nextShapeIndex.value];
  if (!shape) return [];

  // 计算形状的尺寸
  const rows = Math.max(...shape.map(([row]) => row)) - Math.min(...shape.map(([row]) => row)) + 1;
  const cols = Math.max(...shape.map(([,col]) => col)) - Math.min(...shape.map(([,col]) => col)) + 1;

  // 计算居中偏移
  const offsetRow = Math.floor((4 - rows) / 2);
  const offsetCol = Math.floor((4 - cols) / 2);

  return shape.map(([row, col]) => ({
    row: row + offsetRow,
    col: col + offsetCol,
    color: colors.value[nextShapeIndex.value]
  }));
})

// 音效相关
const moveSound = new Audio('/sounds/move.MP3')
const rotateSound = new Audio('/sounds/rotate.MP3')
const dropSound = new Audio('/sounds/drop.MP3')
const clearSound = new Audio('/sounds/clear.MP3')
const gameStartSound = new Audio('/sounds/start.MP3')
const gameOverSound = new Audio('/sounds/gameover.MP3')

// 音效音量设置
moveSound.volume = 0.3
rotateSound.volume = 0.3
dropSound.volume = 0.4
clearSound.volume = 0.4
gameStartSound.volume = 0.5
gameOverSound.volume = 0.5

// 播放音效的函数
const playSound = (sound: HTMLAudioElement) => {
  if (!isSoundEnabled.value) return
  if (sound.paused) {
    sound.currentTime = 0
    sound.play().catch(() => {})
  } else {
    sound.currentTime = 0
  }
}

// 旋转方块 - 改进旋转逻辑
const rotateShape = () => {
  if (isPaused.value || !isPlaying.value) return

  // 保存原始形状和位置
  const originalShape = [...currentShape.value]
  const originalPos = [...currentPos.value]

  // 计算当前形状的边界
  const minRow = Math.min(...currentShape.value.map(([row]) => row))
  const maxRow = Math.max(...currentShape.value.map(([row]) => row))
  const minCol = Math.min(...currentShape.value.map(([, col]) => col))
  const maxCol = Math.max(...currentShape.value.map(([, col]) => col))

  // 计算中心点
  const centerRow = minRow + Math.floor((maxRow - minRow) / 2)
  const centerCol = minCol + Math.floor((maxCol - minCol) / 2)

  // 围绕中心点旋转
  const rotated = currentShape.value.map(([row, col]) => {
    const relativeRow = row - centerRow
    const relativeCol = col - centerCol
    return [
      centerRow - relativeCol,
      centerCol + relativeRow
    ]
  })

  currentShape.value = rotated

  // 尝试不同的偏移位置，优先尝试原位置
  const offsets = [
    [0, 0], // 原位置
    [0, -1], // 左移
    [0, 1], // 右移
    [-1, 0], // 上移
    [1, 0], // 下移
    [-1, -1], // 左上
    [-1, 1], // 右上
    [1, -1], // 左下
    [1, 1] // 右下
  ]

  let validRotation = false
  for (const [offsetRow, offsetCol] of offsets) {
    currentPos.value = [
      originalPos[0] + offsetRow,
      originalPos[1] + offsetCol
    ]
    if (!checkCollision()) {
      validRotation = true
      break
    }
  }

  if (!validRotation) {
    // 如果所有偏移都失败，恢复原始状态
    currentShape.value = originalShape
    currentPos.value = originalPos
  } else {
    playSound(rotateSound) // 旋转成功时播放音效
    // 添加旋转动画
    const blocks = document.querySelectorAll('.block')
    blocks.forEach(block => {
      block.classList.add('rotating')
      setTimeout(() => block.classList.remove('rotating'), 150)
    })
  }
}

// 更新碰撞检测和游戏结束逻辑
const checkGameOver = () => {
  // 检查顶部两行是否有方块
  for (let row = 0; row < 2; row++) {
    for (let col = 0; col < BOARD_COLS; col++) {
      if (board.value[row][col] !== 0) {
        gameOver()
        return true
      }
    }
  }
  return false
}

// 更新游戏结束处理
const gameOver = () => {
  isPlaying.value = false
  isGameOver.value = true
  if (gameInterval.value) {
    clearInterval(gameInterval.value)
    gameInterval.value = null
  }
  updateHighScore()
  playSound(gameOverSound) // 游戏结束时播放音效
}

// 更新下落逻辑
const dropPiece = () => {
  if (!isPlaying.value || isPaused.value) return

  // 确保每次下落都是整数单位
  if (!checkCollision(1, 0)) {
    currentPos.value = [
      Math.round(currentPos.value[0] + 1),
      Math.round(currentPos.value[1])
    ]
  } else {
    freezeShape()
    if (checkGameOver()) {
      return
    }
    clearLines()
    createNewShape()
  }
}

// 更新方块冻结逻辑
const freezeShape = () => {
  currentShape.value.forEach(([row, col]) => {
    const boardRow = row + currentPos.value[0]
    const boardCol = col + currentPos.value[1]
    if (boardRow >= 0) {
      board.value[boardRow][boardCol] = currentShapeIndex.value + 1
    }
  })
  playSound(dropSound) // 方块落地时播放音效
}

// 更新创建新方块的逻辑
const createNewShape = () => {
  currentShapeIndex.value = nextShapeIndex.value
  currentShape.value = JSON.parse(JSON.stringify(shapes[currentShapeIndex.value]))
  nextShapeIndex.value = Math.floor(Math.random() * shapes.length)

  // 计算方块的实际宽度并确保居中
  const minCol = Math.min(...currentShape.value.map(([, col]) => col))
  const maxCol = Math.max(...currentShape.value.map(([, col]) => col))
  const shapeWidth = maxCol - minCol + 1

  // 设置初始位置，确保方块在中间且对齐网格
  const startCol = Math.floor((BOARD_COLS - shapeWidth) / 2)
  currentPos.value = [-2, startCol]
}

// 碰撞检测
const checkCollision = (offsetRow = 0, offsetCol = 0) => {
  return currentShape.value.some(([row, col]) => {
    const newRow = row + currentPos.value[0] + offsetRow
    const newCol = col + currentPos.value[1] + offsetCol
    return (
      newCol < 0 || // 超出左边界
      newCol >= BOARD_COLS || // 超出右边界
      newRow >= BOARD_ROWS || // 超出底部
      (newRow >= 0 && board.value[newRow][newCol] !== 0) // 碰到其他方块
    )
  })
}

// 移动方块
const moveShape = (offsetRow: number, offsetCol: number) => {
  if (isPaused.value || !isPlaying.value) return

  // 确保偏移量为整数
  offsetRow = Math.round(offsetRow)
  offsetCol = Math.round(offsetCol)

  const newPos = [
    Math.round(currentPos.value[0] + offsetRow),
    Math.round(currentPos.value[1] + offsetCol)
  ]

  if (!checkCollision(offsetRow, offsetCol)) {
    currentPos.value = newPos as [number, number]
    if (offsetCol !== 0) playSound(moveSound) // 左右移动时播放音效
    return true
  }
  return false
}

// 方块下落
const moveDown = () => {
  if (!isPlaying.value || isPaused.value) return
  dropPiece()
}

// 消除满行
const clearLines = () => {
  let linesCleared: number = 0;

  for (let row: number = BOARD_ROWS - 1; row >= 0; row--) {
    var isFullRow: boolean = true;
    for (var col: number = 0; col < BOARD_COLS; col++) {
      if (board.value[row][col] === 0) {
        isFullRow = false;
        break;
      }
    }

    if (isFullRow) {
      // 移除当前行
      for (var r: number = row; r > 0; r--) {
        for (var c: number = 0; c < BOARD_COLS; c++) {
          board.value[r][c] = board.value[r - 1][c];
        }
      }
      // 在顶部添加新的空行
      for (var c: number = 0; c < BOARD_COLS; c++) {
        board.value[0][c] = 0;
      }
      linesCleared++;
      row++; // 检查同一行（因为上面的行下移了）
    }
  }

  if (linesCleared > 0) {
    playSound(clearSound) // 消除行时播放音效
    const points: number = linesCleared * 100 * level.value;
    score.value += points;
    updateHighScore();

    // 更新等级和速度
    level.value = Math.floor(score.value / LEVEL_SCORE_THRESHOLD) + 1;
    speed.value = Math.max(100, INITIAL_SPEED - (level.value - 1) * SPEED_INCREMENT);

    // 重新设置定时器
    if (gameInterval.value) {
      clearInterval(gameInterval.value);
      gameInterval.value = window.setInterval(moveDown, speed.value);
    }
  }
}

// 处理键盘事件
const handleKeyPress = (event: KeyboardEvent) => {
  if (!isPlaying.value || isPaused.value) return

  // 阻止默认行为
  event.preventDefault()

  switch (event.key) {
    case 'ArrowLeft':
      moveShape(0, -1)
      break
    case 'ArrowRight':
      moveShape(0, 1)
      break
    case 'ArrowDown':
      moveShape(1, 0)
      break
    case 'ArrowUp':
      rotateShape()
      break
    case ' ':
      pauseGame()
      break
  }
}

// 更新最高分
const updateHighScore = () => {
  if (score.value > highScore.value) {
    highScore.value = score.value
    localStorage.setItem('tetris-high-score', score.value.toString())
  }
}

// 开始游戏
const startGame = () => {
  // 清除现有状态
  if (gameInterval.value) {
    clearInterval(gameInterval.value)
    gameInterval.value = null
  }

  // 重置游戏状态
  gameState.value.board = createEmptyBoard()
  gameState.value.score = 0
  level.value = 1
  speed.value = INITIAL_SPEED
  isPlaying.value = true
  isPaused.value = false
  isGameOver.value = false

  // 清空当前方块
  currentShape.value = []
  currentPos.value = [-2, 0]

  // 初始化新方块
  nextShapeIndex.value = Math.floor(Math.random() * shapes.length)
  createNewShape()

  // 播放开始音效
  playSound(gameStartSound)

  // 设置定时器
  gameInterval.value = window.setInterval(moveDown, speed.value)
}

// 暂停游戏
const pauseGame = () => {
  if (!isPlaying.value || isGameOver.value) return
  isPaused.value = !isPaused.value
  if (!isPaused.value) {
    // 继续游戏时重新开始计时器
    if (gameInterval.value) {
      clearInterval(gameInterval.value)
    }
    gameInterval.value = window.setInterval(moveDown, speed.value)
  }
}

// 返回上一级
const goBack = () => {
  window.history.back()
}

// 添加音频控制函数
const toggleSound = () => {
  isSoundEnabled.value = !isSoundEnabled.value
  // 更新所有音效的音量
  const volume = isSoundEnabled.value ? 0.3 : 0
  moveSound.volume = volume
  rotateSound.volume = volume
  dropSound.volume = volume
  clearSound.volume = volume
  gameStartSound.volume = volume * 1.67  // 保持原来的相对音量比例
  gameOverSound.volume = volume * 1.67
}

// 生命周期钩子
onMounted(() => {
  // 初始化移动端检测
  checkMobile()
  window.addEventListener('resize', checkMobile)

  // 预加载音频文件
  const sounds = [moveSound, rotateSound, dropSound, clearSound, gameStartSound, gameOverSound]
  sounds.forEach(sound => {
    sound.load()
    // 添加错误处理
    sound.onerror = () => {
      console.error('Failed to load sound:', sound.src)
    }
  })

  // 加载最高分
  const savedHighScore = localStorage.getItem('tetris-high-score')
  if (savedHighScore) {
    highScore.value = parseInt(savedHighScore)
  }

  // 添加键盘事件监听
  window.addEventListener('keydown', handleKeyPress, { passive: false })
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyPress)
  window.removeEventListener('resize', checkMobile)
  if (gameInterval.value) {
    clearInterval(gameInterval.value)
  }
})
</script>

<style scoped>
.tetris-page {
  min-height: 90vh;
  padding-bottom: 80px; /* 添加底部padding以防止内容被控制栏遮挡 */
  background: #faf8ef;
  margin: -20px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: "Clear Sans", "Helvetica Neue", Arial, sans-serif;
  position: relative;
  overflow: hidden;
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
  background-size: 400% 400%;
  animation: gradientMove 10s ease infinite;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.game-container {
  position: relative;
  max-width: 1000px;
  width: 100%;
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
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
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
  text-align: center;
}

.score-label {
  font-size: 13px;
  color: #eee4da;
  text-transform: uppercase;
  margin-bottom: 4px;
}

.score-value {
  font-size: 24px;
  font-weight: bold;
  color: white;
}

.score-value.highlight {
  color: #f65e3b;
}

.game-controls {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;
}

.control-button {
  background: #8f7a66;
  border-radius: 6px;
  padding: 0 24px;
  height: 44px;
  line-height: 44px;
  color: #f9f6f2;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.control-button:hover {
  background: #9f8b77;
  transform: translateY(-2px);
}

.game-main {
  display: flex;
  gap: 30px;
  justify-content: center;
}

.game-board {
  background: #bbada0;
  padding: 5px;
  border-radius: 8px;
  position: relative;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.1);
  width: calc(var(--cell-size, 30px) * 10 + 10px);
  height: calc(var(--cell-size, 30px) * 20 + 10px);
  display: flex;
  flex-direction: column;
  margin: 0 auto;
}

.grid-background {
  position: absolute;
  top: 5px;
  left: 5px;
  right: 5px;
  bottom: 5px;
  z-index: 0;
  display: flex;
  flex-direction: column;
}

.grid-row {
  display: flex;
  height: var(--cell-size, 30px);
}

.grid-cell {
  width: var(--cell-size, 30px);
  height: var(--cell-size, 30px);
  border: 1px solid rgba(238, 228, 218, 0.35);
  box-sizing: border-box;
}

.board-row {
  display: flex;
  position: relative;
  z-index: 1;
  height: var(--cell-size, 30px);
}

.board-cell {
  width: var(--cell-size, 30px);
  height: var(--cell-size, 30px);
  position: relative;
  box-sizing: border-box;
}

.board-cell.filled {
  background-image: var(--cell-gradient);
  border-radius: 3px;
  box-shadow: inset 0 0 10px rgba(255, 255, 255, 0.2);
}

.block {
  width: var(--cell-size, 30px);
  height: var(--cell-size, 30px);
  position: absolute;
  background-image: var(--block-gradient);
  border-radius: 3px;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
  z-index: 2;
  box-sizing: border-box;
  left: 0;
  top: 0;
  transform: translate(calc(var(--col) * var(--cell-size, 30px) + 5px),
  calc(var(--row) * var(--cell-size, 30px) + 5px));
  will-change: transform;
  transition: transform 0.15s ease;
}

.next-shape {
  background: #bbada0;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.next-shape h3 {
  color: #f9f6f2;
  margin: 0 0 10px;
  text-align: center;
  font-size: 18px;
}

.next-shape-preview {
  background: rgba(238, 228, 218, 0.35);
  padding: 10px;
  border-radius: 4px;
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.preview-grid {
  position: relative;
  width: calc(var(--preview-cell-size, 25px) * 4);
  height: calc(var(--preview-cell-size, 25px) * 4);
  display: flex;
  flex-direction: column;
}

.preview-row {
  display: flex;
  height: var(--preview-cell-size, 25px);
}

.preview-cell {
  width: var(--preview-cell-size, 25px);
  height: var(--preview-cell-size, 25px);
  border: 1px solid rgba(238, 228, 218, 0.35);
  box-sizing: border-box;
}

.preview-block {
  width: var(--preview-cell-size, 25px);
  height: var(--preview-cell-size, 25px);
  position: absolute;
  background-image: var(--block-gradient);
  border-radius: 2px;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.2);
  transition: all 0.15s ease;
  box-sizing: border-box;
  transform: translate3d(calc(var(--preview-cell-size, 25px) * var(--col)), calc(var(--preview-cell-size, 25px) * var(--row)), 0);
}

.controls-guide {
  background: #bbada0;
  padding: 15px;
  border-radius: 8px;
  color: #f9f6f2;
}

.controls-guide h3 {
  margin: 0 0 10px;
  text-align: center;
  font-size: 18px;
}

.controls-guide ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.controls-guide li {
  margin-bottom: 8px;
  padding-left: 20px;
  position: relative;
}

.controls-guide li:before {
  content: "•";
  position: absolute;
  left: 8px;
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
}

.modal-content {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.game-over-title {
  font-size: 48px;
  font-weight: bold;
  color: #776e65;
  margin-bottom: 30px;
}

.game-over-stats {
  margin-bottom: 30px;
}

.game-over-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
}

/* 移动端顶部控制栏样式 */
.mobile-top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(143, 122, 102, 0.95);
  padding: 8px 10px;
  border-radius: 16px;
  margin: 8px 5px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
}

.mobile-back-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mobile-back-btn .anticon {
  font-size: 18px;
  color: #f9f6f2;
}

.mobile-controls-left {
  display: flex;
  gap: 12px;
}

.mobile-controls-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.mobile-score-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.mobile-score-item .anticon {
  font-size: 16px;
  color: #f9f6f2;
}

.mobile-score-item span {
  font-size: 14px;
  font-weight: bold;
  color: #f9f6f2;
}

.mobile-control-item {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
}

.mobile-control-item .anticon {
  font-size: 20px;
  color: #ffffff;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

.mobile-controls {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 15px 20px;
  padding-bottom: max(20px, env(safe-area-inset-bottom, 34px));
  background: linear-gradient(to bottom, transparent, rgba(143, 122, 102, 0.95));
  backdrop-filter: blur(10px);
  z-index: 1000;
}

.mobile-controls-container {
  background: transparent;
  box-shadow: none;
}

.mobile-btn {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  border: 2px solid rgba(255, 255, 255, 0.3);
}

.mobile-controls-center {
  display: flex;
  gap: 20px;
}

.mobile-btn {
  width: 52px;
  height: 52px;
}

.mobile-btn .anticon {
  font-size: 28px;
  color: #ffffff;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

@media screen and (max-width: 768px) {
  .tetris-page {
    padding: 0;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 999;
    margin: 0 !important;
    background: #faf8ef;
    height: 100vh;
    min-height: 100vh;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .game-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 0;
    position: relative;
    z-index: 1;
  }

  .game-board {
    margin: 0 auto;
    margin-bottom: 80px;
    padding: 2px;
    width: calc(var(--cell-size, 30px) * 10 + 4px);
    height: calc(var(--cell-size, 30px) * 20 + 4px);
  }

  .block {
    transform: translate(calc(var(--col) * var(--cell-size, 30px) + 2px),
    calc(var(--row) * var(--cell-size, 30px) + 2px));
  }

  .grid-background {
    top: 2px;
    left: 2px;
    right: 2px;
    bottom: 2px;
  }

  .mobile-controls {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 15px 20px;
    padding-bottom: max(20px, env(safe-area-inset-bottom, 34px));
    background: linear-gradient(to bottom, transparent, rgba(143, 122, 102, 0.95));
    backdrop-filter: blur(10px);
    z-index: 1000;
  }

  .mobile-control-item {
    width: 40px;
    height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.2);
    border: 2px solid rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .mobile-control-item:active {
    transform: scale(0.95);
    background: rgba(255, 255, 255, 0.3);
  }

  .mobile-control-item .anticon {
    font-size: 26px;
    color: #ffffff;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
  }

  .mobile-controls-container {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 25px;
    padding: 15px 30px;
    border-radius: 30px;
    background: rgba(255, 255, 255, 0.1);
    box-shadow: 0 -4px 15px rgba(0, 0, 0, 0.2);
  }
}

@media screen and (max-width: 360px) {
  .game-board {
    margin-bottom: 100px;
    padding: 3px;
  }

  .mobile-top-bar {
    padding: 8px 10px;
    margin: 5px;
    border-radius: 16px;
  }

  .mobile-back-btn {
    width: 32px;
    height: 32px;
  }

  .mobile-back-btn .anticon {
    font-size: 18px;
  }

  .mobile-controls-left {
    gap: 12px;
  }

  .mobile-controls-right {
    gap: 8px;
  }

  .mobile-score-item {
    gap: 4px;
  }

  .mobile-score-item .anticon {
    font-size: 14px;
  }

  .mobile-score-item span {
    font-size: 13px;
  }

  .mobile-control-item {
    width: 32px;
    height: 32px;
  }

  .mobile-control-item .anticon {
    font-size: 20px;
  }
}

@media screen and (max-width: 320px) {
  .mobile-top-bar {
    padding: 6px 8px;
    margin: 4px;
  }

  .mobile-controls-left {
    gap: 8px;
  }

  .mobile-controls-right {
    gap: 6px;
  }

  .mobile-control-item {
    width: 28px;
    height: 28px;
  }

  .mobile-control-item .anticon {
    font-size: 18px;
  }

  .mobile-score-item span {
    font-size: 12px;
  }
}

/* 确保分享按钮不会遮挡游戏区域 */
:deep(.ant-share-alt) {
  background: transparent !important;
  box-shadow: none !important;
}

.sound-button {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sound-button .anticon {
  font-size: 20px;
}

.mobile-control-item.sound-control {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mobile-control-item.sound-control:active {
  transform: scale(0.95);
  background: rgba(255, 255, 255, 0.3);
}

.mobile-control-item.sound-control .anticon {
  font-size: 24px;
  color: #ffffff;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
}

@media screen and (max-width: 360px) {
  .mobile-control-item.sound-control {
    width: 36px;
    height: 36px;
  }

  .mobile-control-item.sound-control .anticon {
    font-size: 20px;
  }
}
</style>
