<template>
  <div class="link-game-page">
    <!-- 背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="game-container">
      <!-- 游戏头部 -->
      <div class="game-header">
        <h2 class="game-title">连连看</h2>
        <div class="scores-container">
          <div class="score-box">
            <div class="score-label">当前分数</div>
            <div class="score-value">{{ score }}</div>
          </div>
          <div class="score-box">
            <div class="score-label">最高分数</div>
            <div class="score-value highlight">{{ bestScore }}</div>
          </div>
          <div class="score-box">
            <div class="score-label">剩余时间</div>
            <div class="score-value">{{ formatTime(timeLeft) }}</div>
          </div>
        </div>
      </div>

      <!-- 游戏控制按钮 -->
      <div class="game-controls">
        <div class="control-button" @click="startGame">
          <play-circle-outlined v-if="!isPlaying" />
          <redo-outlined v-else />
          <span >{{ isPlaying ? '重新开始' : '开始游戏' }}</span>
        </div>
        <div class="control-button" @click="toggleSound">
          <sound-filled v-if="isSoundEnabled" />
          <sound-outlined v-else />
          <span >{{ isSoundEnabled ? '关闭音效' : '开启音效' }}</span>
        </div>
        <div class="control-button" @click="changeDifficulty">
          <setting-outlined />
          <span >{{ difficulty }}难度</span>
        </div>
      </div>

      <!-- 道具栏 -->
      <div class="tools-container" v-if="isPlaying">
        <div class="tool-item" @click="useTool('bomb')" :class="{ disabled: !tools.bomb.available }">
          <div class="tool-icon">💣</div>
          <div class="tool-name">炸弹</div>
          <div class="cooldown-mask" v-if="!tools.bomb.available"></div>
        </div>
        <div class="tool-item" @click="useTool('refresh')" :class="{ disabled: !tools.refresh.available }">
          <div class="tool-icon">🔄</div>
          <div class="tool-name">刷新</div>
          <div class="cooldown-mask" v-if="!tools.refresh.available"></div>
        </div>
        <div class="tool-item" @click="useTool('hint')" :class="{ disabled: !tools.hint.available }">
          <div class="tool-icon">💡</div>
          <div class="tool-name">提示</div>
          <div class="cooldown-mask" v-if="!tools.hint.available"></div>
        </div>
        <div class="tool-item" @click="useTool('vision')" :class="{ disabled: !tools.vision.available }">
          <div class="tool-icon">👁️</div>
          <div class="tool-name">透视</div>
          <div class="cooldown-mask" v-if="!tools.vision.available"></div>
        </div>
      </div>

      <!-- 游戏画布 -->
      <div class="game-board-container">
        <canvas ref="gameCanvas" class="game-canvas"></canvas>
      </div>
    </div>

    <!-- 游戏结束弹窗 -->
    <div v-if="showEndModal" class="modal-overlay">
      <div class="modal-content game-over-modal">
        <h3 class="game-over-title">{{ isWin ? '恭喜通关！' : '游戏结束' }}</h3>
        <div class="game-over-stats">
          <div class="stat-item">
            <div class="stat-label">最终得分</div>
            <div class="stat-value highlight">{{ score }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">消除组数</div>
            <div class="stat-value">{{ matchedPairs }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">游戏时间</div>
            <div class="stat-value">{{ formatTime(config.difficulties[difficulty].timeLimit - timeLeft) }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">当前难度</div>
            <div class="stat-value">{{ difficulty }}</div>
          </div>
        </div>
        <div class="game-over-buttons">
          <template v-if="isWin && difficulty !== '高级'">
            <div class="control-button next-level" @click="nextLevel">
              <up-outlined />
              <span class="control-text">下一难度</span>
            </div>
          </template>
          <div class="control-button" @click="startGame">
            <reload-outlined />
            <span class="control-text">{{ isWin ? '再玩一次' : '重新开始' }}</span>
          </div>
          <div class="control-button" @click="quitGame">
            <home-outlined />
            <span class="control-text">返回主页</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 提示消息 -->
    <div v-if="showTip" class="tip-message">
      {{ tipMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PlayCircleOutlined,
  RedoOutlined,
  HomeOutlined,
  SoundOutlined,
  SoundFilled,
  SettingOutlined,
  UpOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

interface GameConfig {
  rows: number
  cols: number
  iconSize: number
  iconCount: number
  timeLimit: number
}

interface GameState {
  icon: string
  matched: boolean
}

type DifficultyLevel = '初级' | '中级' | '高级'

const difficulty = ref<DifficultyLevel>('初级')

const config = reactive({
  difficulties: {
    '初级': {
      rows: 8,
      cols: 8,
      iconSize: 64,
      iconCount: 12,
      timeLimit: 180
    },
    '中级': {
      rows: 10,
      cols: 10,
      iconSize: 52,
      iconCount: 16,
      timeLimit: 240
    },
    '高级': {
      rows: 12,
      cols: 12,
      iconSize: 44,
      iconCount: 20,
      timeLimit: 300
    }
  } as Record<DifficultyLevel, GameConfig>,
  icons: [
    '🐨', '🐯', '🐰', '🐶', '🦊', '🐱', '🐷', '🦁',
    '🐼', '🐸', '🐮', '🐹', '🐻', '🐽', '🦒', '🐵',
    '🦆', '🦅', '🦉', '🦋', '🐝', '🐞', '🦗', '🕷️'
  ]
})

const currentConfig = computed(() => config.difficulties[difficulty.value])

const generateBoard = (): GameState[][] => {
  const { rows, cols, iconCount } = currentConfig.value
  const totalCells = rows * cols
  const pairs = totalCells / 2
  const icons = config.icons.slice(0, iconCount)

  // 创建配对数组
  const board: string[] = []
  for (let i = 0; i < pairs; i++) {
    const icon = icons[i % icons.length]
    board.push(icon, icon)
  }

  // 随机打乱
  for (let i = board.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[board[i], board[j]] = [board[j], board[i]]
  }

  // 转换为二维数组
  const newBoard: GameState[][] = []
  for (let i = 0; i < rows; i++) {
    newBoard.push(
      board.slice(i * cols, (i + 1) * cols).map(icon => ({
        icon,
        matched: false
      }))
    )
  }

  return newBoard
}

const calculateIconSize = () => {
  if (window.innerWidth <= 768) {
    // 计算可用空间
    const availableHeight = window.innerHeight - 200 // 减去头部和工具栏的高度
    const availableWidth = window.innerWidth - 20 // 减去左右边距

    // 取较小的值确保是正方形
    const size = Math.min(
      Math.floor(availableWidth / currentConfig.value.cols),
      Math.floor(availableHeight / currentConfig.value.rows)
    )

    return size
  }
  return currentConfig.value.iconSize
}

const iconSize = ref(calculateIconSize())

// 游戏状态
const isPlaying = ref(false)
const gameOver = ref(false)
const score = ref(0)
const bestScore = ref(parseInt(localStorage.getItem('linkGameBestScore') || '0'))
const timeLeft = ref(0) // 初始化为0，在startGame时设置
const matchedPairs = ref(0)
const gameCanvas = ref<HTMLCanvasElement | null>(null)
const ctx = ref<CanvasRenderingContext2D | null>(null)
const board = ref<GameState[][]>([])
const selectedTiles = ref<Array<{ x: number; y: number }>>([])
const comboCount = ref(0)
const showEndModal = ref(false)
const isWin = ref(false)
const gameTimer = ref<number | null>(null)

// 道具状态
const bombCount = ref(3)
const refreshCount = ref(3)
const hintCount = ref(3)
const visionCount = ref(3)

// 音效
const isSoundEnabled = ref(true)
const selectSound = new Audio('/sounds/select.MP3')
const matchSound = new Audio('/sounds/match.MP3')
const comboSound = new Audio('/sounds/combo.MP3')
const winSound = new Audio('/sounds/win.MP3')
const loseSound = new Audio('/sounds/lose.MP3')
const toolSound = new Audio('/sounds/tool.MP3')

// 设置音效音量
selectSound.volume = 0.3
matchSound.volume = 0.3
comboSound.volume = 0.3
winSound.volume = 0.5
loseSound.volume = 0.5
toolSound.volume = 0.3

// 道具系统
const tools = ref({
  bomb: {
    available: true,
    cooldown: 30000, // 30秒
    lastUsed: 0
  },
  refresh: {
    available: true,
    cooldown: 20000, // 20秒
    lastUsed: 0
  },
  hint: {
    available: true,
    cooldown: 15000, // 15秒
    lastUsed: 0
  },
  vision: {
    available: true,
    cooldown: 5000, // 5秒
    lastUsed: 0
  }
})

// 格式化时间
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 初始化游戏板
const initBoard = () => {
  const newBoard: GameState[][] = []
  const { rows, cols } = config.difficulties[difficulty.value]

  // 先用空白填充整个棋盘
  for (let i = 0; i < rows; i++) {
    newBoard.push(Array(cols).fill({ icon: '', matched: false }))
  }

  // 生成配对的图标
  const pairs = generatePairs()

  // 放置图标
  pairs.forEach(icon => {
    let placed = false
    while (!placed) {
      const x = Math.floor(Math.random() * cols)
      const y = Math.floor(Math.random() * rows)
      if (newBoard[y][x].icon === '') {
        newBoard[y][x] = { icon, matched: false }
        placed = true
      }
    }
  })

  board.value = newBoard
}

// 生成配对的图标数组
const generatePairs = () => {
  const { rows, cols, iconCount } = currentConfig.value
  const totalPairs = Math.floor((rows * cols) / 2)
  const pairs: string[] = []
  const icons = config.icons.slice(0, iconCount)

  for (let i = 0; i < totalPairs; i++) {
    const icon = icons[i % icons.length]
    pairs.push(icon, icon)
  }

  // 随机打乱
  for (let i = pairs.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[pairs[i], pairs[j]] = [pairs[j], pairs[i]]
  }

  return pairs
}

// 添加透视状态
const isVisionActive = ref(false)

// 修改检查障碍物函数
const hasObstacle = (x: number, y: number): boolean => {
  // 如果透视状态激活，视为无障碍
  if (isVisionActive.value) {
    return false
  }

  // 如果坐标在边界外，视为无障碍
  if (x < 0 || x >= currentConfig.value.cols || y < 0 || y >= currentConfig.value.rows) {
    return false
  }
  // 已匹配或空白格子视为无障碍
  return !board.value[y][x].matched && board.value[y][x].icon !== ''
}

// 检查两点是否可连接
const canConnect = (start: { x: number; y: number }, end: { x: number; y: number }) => {
  // 相同位置
  if (start.x === end.x && start.y === end.y) return false

  // 检查图标是否相同
  if (board.value[start.y][start.x].icon !== board.value[end.y][end.x].icon) return false

  // 检查三种连接方式
  return checkDirectLine(start, end) ||
    checkOneCorner(start, end) ||
    checkTwoCorners(start, end)
}

// 检查直线连接
const checkDirectLine = (start: { x: number; y: number }, end: { x: number; y: number }) => {
  // 如果不在同一行或同一列，直接返回false
  if (start.x !== end.x && start.y !== end.y) return false

  // 获取起点和终点之间的所有点
  const points: Array<{x: number, y: number}> = []

  if (start.x === end.x) { // 垂直方向
    const minY = Math.min(start.y, end.y)
    const maxY = Math.max(start.y, end.y)
    for (let y = minY + 1; y < maxY; y++) {
      points.push({ x: start.x, y })
    }
  } else { // 水平方向
    const minX = Math.min(start.x, end.x)
    const maxX = Math.max(start.x, end.x)
    for (let x = minX + 1; x < maxX; x++) {
      points.push({ x, y: start.y })
    }
  }

  // 检查路径上是否有障碍物
  return points.every(point => !hasObstacle(point.x, point.y))
}

// 检查一个拐点连接
const checkOneCorner = (start: { x: number; y: number }, end: { x: number; y: number }) => {
  // 尝试两个可能的拐点
  const corners = [
    { x: end.x, y: start.y },   // 水平转垂直
    { x: start.x, y: end.y }    // 垂直转水平
  ]

  // 检查每个拐点
  return corners.some(corner => {
    // 如果拐点位置有障碍物，跳过
    if (hasObstacle(corner.x, corner.y)) return false

    // 检查从起点到拐点，以及从拐点到终点的路径
    return checkDirectLine(start, corner) && checkDirectLine(corner, end)
  })
}

// 检查两个拐点连接
const checkTwoCorners = (start: { x: number; y: number }, end: { x: number; y: number }) => {
  // 遍历起点同行的所有点
  for (let x = -1; x <= currentConfig.value.cols; x++) {
    if (x === start.x) continue
    const corner1 = { x, y: start.y }

    // 如果第一个拐点不可达，跳过
    if (!checkDirectLine(start, corner1)) continue

    // 尝试第二个拐点
    const corner2 = { x, y: end.y }
    if (!hasObstacle(corner1.x, corner1.y) && !hasObstacle(corner2.x, corner2.y)) {
      if (checkDirectLine(corner1, corner2) && checkDirectLine(corner2, end)) {
        return true
      }
    }
  }

  // 遍历起点同列的所有点
  for (let y = -1; y <= currentConfig.value.rows; y++) {
    if (y === start.y) continue
    const corner1 = { x: start.x, y }

    // 如果第一个拐点不可达，跳过
    if (!checkDirectLine(start, corner1)) continue

    // 尝试第二个拐点
    const corner2 = { x: end.x, y }
    if (!hasObstacle(corner1.x, corner1.y) && !hasObstacle(corner2.x, corner2.y)) {
      if (checkDirectLine(corner1, corner2) && checkDirectLine(corner2, end)) {
        return true
      }
    }
  }

  return false
}

// 添加提示消息状态
const tipMessage = ref('')
const showTip = ref(false)

// 修改handleClick函数，添加提示功能
const handleClick = (x: number, y: number) => {
  if (!isPlaying.value || board.value[y][x].matched) return

  if (isSoundEnabled.value) {
    selectSound.currentTime = 0
    selectSound.play().catch(() => {})
  }

  selectedTiles.value.push({ x, y })

  if (selectedTiles.value.length === 2) {
    const [first, second] = selectedTiles.value

    if (canConnect(first, second)) {
      // 先绘制连接线
      drawBoard()

      // 延迟消除，让玩家能看到连接线
      setTimeout(() => {
        // 消除配对
        board.value[first.y][first.x].matched = true
        board.value[second.y][second.x].matched = true
        matchedPairs.value++
        comboCount.value++

        // 计算得分
        const baseScore = 100
        const comboBonus = comboCount.value * 10
        score.value += baseScore + comboBonus

        if (isSoundEnabled.value) {
          playMatchSound()
        }

        selectedTiles.value = []
        drawBoard()

        // 检查游戏是否结束
        if (matchedPairs.value === (currentConfig.value.rows * currentConfig.value.cols) / 2) {
          endGame(true)
        }
      }, 300) // 300ms后消除
    } else {
      // 显示为什么不能消除的提示
      if (board.value[first.y][first.x].icon !== board.value[second.y][second.x].icon) {
        showTipMessage('这两个图案不相同哦！')
      } else {
        showTipMessage('找不到有效的连接路径，试试其他的吧！')
      }
      comboCount.value = 0
      selectedTiles.value = []
      drawBoard()
    }
  } else {
    drawBoard()
  }
}

// 显示提示消息
const showTipMessage = (message: string) => {
  tipMessage.value = message
  showTip.value = true
  setTimeout(() => {
    showTip.value = false
  }, 2000)
}

// 开始游戏
const startGame = () => {
  // 如果游戏已经在进行中，重新开始
  if (timeLeft.value !== currentConfig.value.timeLimit) {
    isPlaying.value = false
    stopTimer()
  }

  isPlaying.value = true
  gameOver.value = false
  score.value = 0
  matchedPairs.value = 0
  comboCount.value = 0
  timeLeft.value = currentConfig.value.timeLimit
  selectedTiles.value = []
  showEndModal.value = false

  // 重置道具状态
  Object.values(tools.value).forEach(tool => {
    tool.available = true
    tool.lastUsed = 0
  })

  initBoard()
  drawBoard()
  startTimer()
}

// 开始游戏计时器
const startTimer = () => {
  if (gameTimer.value) {
    clearInterval(gameTimer.value)
  }

  gameTimer.value = window.setInterval(() => {
    if (timeLeft.value > 0) {
      timeLeft.value--
      if (timeLeft.value === 0) {
        endGame(false)
      }
    }
  }, 1000)
}

// 停止游戏计时器
const stopTimer = () => {
  if (gameTimer.value) {
    clearInterval(gameTimer.value)
    gameTimer.value = null
  }
}

// 重置游戏
const resetGame = () => {
  stopTimer()
  score.value = 0
  matchedPairs.value = 0
  comboCount.value = 0
  timeLeft.value = currentConfig.value.timeLimit
  selectedTiles.value = []
  showEndModal.value = false
  isWin.value = false
  bombCount.value = 3
  refreshCount.value = 3
  hintCount.value = 3
  visionCount.value = 3
  initBoard()
  startTimer()
}

// 使用音效
const playMatchSound = () => {
  if (isSoundEnabled.value) {
    matchSound.currentTime = 0
    matchSound.play()
  }
}

const playWinSound = () => {
  if (isSoundEnabled.value) {
    winSound.currentTime = 0
    winSound.play()
  }
}

const playLoseSound = () => {
  if (isSoundEnabled.value) {
    loseSound.currentTime = 0
    loseSound.play()
  }
}

// 结束游戏
const endGame = (win: boolean) => {
  stopTimer()
  isWin.value = win
  showEndModal.value = true

  if (win) {
    playWinSound()
    if (score.value > bestScore.value) {
      bestScore.value = score.value
      localStorage.setItem('linkGameBestScore', score.value.toString())
    }
  } else {
    playLoseSound()
  }
}

// 使用道具
const useTool = (toolName: 'bomb' | 'refresh' | 'hint' | 'vision') => {
  const tool = tools.value[toolName]
  if (!tool.available || !isPlaying.value) return

  const now = Date.now()
  if (now - tool.lastUsed < tool.cooldown) return

  if (isSoundEnabled.value) {
    toolSound.currentTime = 0
    toolSound.play().catch(() => {})
  }

  tool.available = false
  tool.lastUsed = now

  setTimeout(() => {
    tool.available = true
  }, tool.cooldown)

  switch (toolName) {
    case 'bomb':
      useBombTool()
      break
    case 'refresh':
      useRefreshTool()
      break
    case 'hint':
      useHintTool()
      break
    case 'vision':
      useVisionTool()
      break
  }
}

// 炸弹道具
const useBombTool = () => {
  const centerX = Math.floor(Math.random() * (currentConfig.value.cols - 2)) + 1
  const centerY = Math.floor(Math.random() * (currentConfig.value.rows - 2)) + 1

  // 获取3x3范围内的未匹配图标
  const unmatched: Array<{x: number; y: number; icon: string}> = []

  for (let y = centerY - 1; y <= centerY + 1; y++) {
    for (let x = centerX - 1; x <= centerX + 1; x++) {
      if (y >= 0 && y < currentConfig.value.rows &&
        x >= 0 && x < currentConfig.value.cols &&
        !board.value[y][x].matched &&
        board.value[y][x].icon !== '') {
        unmatched.push({
          x, y,
          icon: board.value[y][x].icon
        })
      }
    }
  }

  // 按图标分组
  const groups = new Map<string, Array<{x: number; y: number}>>()
  unmatched.forEach(({x, y, icon}) => {
    if (!groups.has(icon)) {
      groups.set(icon, [])
    }
    groups.get(icon)!.push({x, y})
  })

  // 消除配对的图标
  groups.forEach((positions) => {
    if (positions.length >= 2) {
      // 如果有多个相同图标，优先消除两个
      positions.slice(0, 2).forEach(({x, y}) => {
        board.value[y][x].matched = true
        matchedPairs.value++
      })
    }
  })

  drawBoard()
  checkGameEnd()
}

// 刷新道具
const useRefreshTool = () => {
  const unmatchedTiles: string[] = []
  board.value.forEach(row => {
    row.forEach(tile => {
      if (!tile.matched) {
        unmatchedTiles.push(tile.icon)
      }
    })
  })

  // Fisher-Yates shuffle
  for (let i = unmatchedTiles.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[unmatchedTiles[i], unmatchedTiles[j]] = [unmatchedTiles[j], unmatchedTiles[i]]
  }

  let index = 0
  board.value.forEach(row => {
    row.forEach(tile => {
      if (!tile.matched) {
        tile.icon = unmatchedTiles[index++]
      }
    })
  })

  drawBoard()
}

// 提示道具
const useHintTool = () => {
  let foundPair = false

  for (let y1 = 0; y1 < currentConfig.value.rows; y1++) {
    for (let x1 = 0; x1 < currentConfig.value.cols; x1++) {
      if (board.value[y1][x1].matched) continue

      for (let y2 = 0; y2 < currentConfig.value.rows; y2++) {
        for (let x2 = 0; x2 < currentConfig.value.cols; x2++) {
          if (board.value[y2][x2].matched) continue

          if ((x1 !== x2 || y1 !== y2) &&
            board.value[y1][x1].icon === board.value[y2][x2].icon &&
            canConnect({ x: x1, y: y1 }, { x: x2, y: y2 })) {
            // 高亮显示可消除的一对
            selectedTiles.value = [{ x: x1, y: y1 }, { x: x2, y: y2 }]
            foundPair = true
            drawBoard()

            // 1秒后清除高亮
            setTimeout(() => {
              selectedTiles.value = []
              drawBoard()
            }, 1000)

            return
          }
        }
      }
    }
  }

  if (!foundPair) {
    message.info('没有找到可以连接的对子')
  }
}

// 透视道具
const useVisionTool = () => {
  // 激活透视状态
  isVisionActive.value = true

  // 重新绘制画布
  drawBoard()

  // 播放音效
  if (isSoundEnabled.value) {
    toolSound.currentTime = 0
    toolSound.play().catch(() => {})
  }

  // 5秒后关闭透视状态
  setTimeout(() => {
    isVisionActive.value = false
    drawBoard()
  }, 5000)
}

// 切换音效
const toggleSound = () => {
  isSoundEnabled.value = !isSoundEnabled.value
}

// 退出游戏
const quitGame = () => {
  if (gameTimer.value) {
    clearInterval(gameTimer.value)
    gameTimer.value = null
  }
  router.push('/games')
}

// 绘制游戏画布
const drawBoard = () => {
  if (!ctx.value || !gameCanvas.value) return

  const { width, height } = gameCanvas.value
  ctx.value.clearRect(0, 0, width, height)

  // 计算网格大小
  const cellWidth = width / currentConfig.value.cols
  const cellHeight = height / currentConfig.value.rows

  // 绘制背景网格
  ctx.value.strokeStyle = '#bbada0'
  ctx.value.lineWidth = 1

  for (let x = 0; x <= currentConfig.value.cols; x++) {
    ctx.value.beginPath()
    ctx.value.moveTo(x * cellWidth, 0)
    ctx.value.lineTo(x * cellWidth, height)
    ctx.value.stroke()
  }

  for (let y = 0; y <= currentConfig.value.rows; y++) {
    ctx.value.beginPath()
    ctx.value.moveTo(0, y * cellHeight)
    ctx.value.lineTo(width, y * cellHeight)
    ctx.value.stroke()
  }

  // 绘制图标
  ctx.value.font = `${Math.min(cellWidth, cellHeight) * 0.6}px Arial`
  ctx.value.textAlign = 'center'
  ctx.value.textBaseline = 'middle'

  // 如果透视状态激活，先绘制所有可能的连接线
  if (isVisionActive.value) {
    ctx.value.globalAlpha = 0.2
    for (let y1 = 0; y1 < currentConfig.value.rows; y1++) {
      for (let x1 = 0; x1 < currentConfig.value.cols; x1++) {
        if (board.value[y1][x1].matched) continue
        for (let y2 = 0; y2 < currentConfig.value.rows; y2++) {
          for (let x2 = 0; x2 < currentConfig.value.cols; x2++) {
            if (board.value[y2][x2].matched) continue
            if ((x1 !== x2 || y1 !== y2) &&
              board.value[y1][x1].icon === board.value[y2][x2].icon &&
              canConnect({ x: x1, y: y1 }, { x: x2, y: y2 })) {
              drawConnectionPath({ x: x1, y: y1 }, { x: x2, y: y2 }, cellWidth, cellHeight)
            }
          }
        }
      }
    }
    ctx.value.globalAlpha = 1
  }

  board.value.forEach((row, y) => {
    row.forEach((tile, x) => {
      if (!tile.matched) {
        const centerX = (x + 0.5) * cellWidth
        const centerY = (y + 0.5) * cellHeight

        // 绘制选中状态
        const isSelected = selectedTiles.value.some(pos => pos.x === x && pos.y === y)
        if (isSelected) {
          ctx.value!.fillStyle = 'rgba(255, 142, 83, 0.3)'
          ctx.value!.fillRect(x * cellWidth, y * cellHeight, cellWidth, cellHeight)
        }

        ctx.value!.fillStyle = '#776e65'
        ctx.value!.fillText(tile.icon, centerX, centerY)
      }
    })
  })

  // 绘制当前选中的连接线
  if (selectedTiles.value.length === 2) {
    const [start, end] = selectedTiles.value
    if (canConnect(start, end)) {
      ctx.value.globalAlpha = 1
      drawConnectionPath(start, end, cellWidth, cellHeight)
    }
  }
}

// 绘制连接路径
const drawConnectionPath = (start: { x: number; y: number }, end: { x: number; y: number }, cellWidth: number, cellHeight: number) => {
  if (!ctx.value || !gameCanvas.value) return

  const { width, height } = gameCanvas.value

  // 设置连接线样式
  ctx.value.strokeStyle = '#ff8e53'
  ctx.value.lineWidth = 3

  // 获取起点和终点的中心坐标
  const startX = (start.x + 0.5) * cellWidth
  const startY = (start.y + 0.5) * cellHeight
  const endX = (end.x + 0.5) * cellWidth
  const endY = (end.y + 0.5) * cellHeight

  // 检查直线连接
  if (checkDirectLine(start, end)) {
    ctx.value.beginPath()
    ctx.value.moveTo(startX, startY)

    // 如果是水平方向且穿过边界
    if (start.y === end.y) {
      if (Math.abs(start.x - end.x) > currentConfig.value.cols / 2) {
        // 向左延伸
        if (start.x > end.x) {
          ctx.value.lineTo(width, startY)
          ctx.value.moveTo(0, startY)
        }
        // 向右延伸
        else {
          ctx.value.lineTo(0, startY)
          ctx.value.moveTo(width, startY)
        }
      }
    }
    // 如果是垂直方向且穿过边界
    else if (start.x === end.x) {
      if (Math.abs(start.y - end.y) > currentConfig.value.rows / 2) {
        // 向上延伸
        if (start.y > end.y) {
          ctx.value.lineTo(startX, height)
          ctx.value.moveTo(startX, 0)
        }
        // 向下延伸
        else {
          ctx.value.lineTo(startX, 0)
          ctx.value.moveTo(startX, height)
        }
      }
    }
    ctx.value.lineTo(endX, endY)
    ctx.value.stroke()
    return
  }

  // 检查一个拐点连接
  const corner = findOneCorner(start, end)
  if (corner) {
    const cornerX = (corner.x + 0.5) * cellWidth
    const cornerY = (corner.y + 0.5) * cellHeight

    ctx.value.beginPath()
    ctx.value.moveTo(startX, startY)

    // 处理第一段线条
    if (corner.x < 0) {
      ctx.value.lineTo(0, cornerY)
    } else if (corner.x >= currentConfig.value.cols) {
      ctx.value.lineTo(width, cornerY)
    } else if (corner.y < 0) {
      ctx.value.lineTo(cornerX, 0)
    } else if (corner.y >= currentConfig.value.rows) {
      ctx.value.lineTo(cornerX, height)
    } else {
      ctx.value.lineTo(cornerX, cornerY)
    }

    // 处理第二段线条
    if (corner.x < 0) {
      ctx.value.moveTo(0, cornerY)
    } else if (corner.x >= currentConfig.value.cols) {
      ctx.value.moveTo(width, cornerY)
    } else if (corner.y < 0) {
      ctx.value.moveTo(cornerX, 0)
    } else if (corner.y >= currentConfig.value.rows) {
      ctx.value.moveTo(cornerX, height)
    }
    ctx.value.lineTo(endX, endY)
    ctx.value.stroke()
    return
  }

  // 检查两个拐点连接
  const corners = findTwoCorners(start, end)
  if (corners) {
    const { corner1, corner2 } = corners
    const corner1X = (corner1.x + 0.5) * cellWidth
    const corner1Y = (corner1.y + 0.5) * cellHeight
    const corner2X = (corner2.x + 0.5) * cellWidth
    const corner2Y = (corner2.y + 0.5) * cellHeight

    ctx.value.beginPath()
    ctx.value.moveTo(startX, startY)

    // 处理第一个拐点
    if (corner1.x < 0) {
      ctx.value.lineTo(0, corner1Y)
      ctx.value.moveTo(0, corner1Y)
    } else if (corner1.x >= currentConfig.value.cols) {
      ctx.value.lineTo(width, corner1Y)
      ctx.value.moveTo(width, corner1Y)
    } else if (corner1.y < 0) {
      ctx.value.lineTo(corner1X, 0)
      ctx.value.moveTo(corner1X, 0)
    } else if (corner1.y >= currentConfig.value.rows) {
      ctx.value.lineTo(corner1X, height)
      ctx.value.moveTo(corner1X, height)
    } else {
      ctx.value.lineTo(corner1X, corner1Y)
    }

    // 处理第二个拐点
    if (corner2.x < 0) {
      ctx.value.lineTo(0, corner2Y)
      ctx.value.moveTo(0, corner2Y)
    } else if (corner2.x >= currentConfig.value.cols) {
      ctx.value.lineTo(width, corner2Y)
      ctx.value.moveTo(width, corner2Y)
    } else if (corner2.y < 0) {
      ctx.value.lineTo(corner2X, 0)
      ctx.value.moveTo(corner2X, 0)
    } else if (corner2.y >= currentConfig.value.rows) {
      ctx.value.lineTo(corner2X, height)
      ctx.value.moveTo(corner2X, height)
    } else {
      ctx.value.lineTo(corner2X, corner2Y)
    }

    ctx.value.lineTo(endX, endY)
    ctx.value.stroke()
  }
}

// 查找一个拐点
const findOneCorner = (start: { x: number; y: number }, end: { x: number; y: number }) => {
  const corners = [
    { x: end.x, y: start.y },
    { x: start.x, y: end.y }
  ]

  for (const corner of corners) {
    if (!hasObstacle(corner.x, corner.y) &&
      checkDirectLine(start, corner) &&
      checkDirectLine(corner, end)) {
      return corner
    }
  }

  return null
}

// 查找两个拐点
const findTwoCorners = (start: { x: number; y: number }, end: { x: number; y: number }) => {
  // 遍历起点同行的所有点
  for (let x = -1; x <= currentConfig.value.cols; x++) {
    if (x === start.x) continue
    const corner1 = { x, y: start.y }

    if (!checkDirectLine(start, corner1)) continue

    const corner2 = { x, y: end.y }
    if (!hasObstacle(corner1.x, corner1.y) && !hasObstacle(corner2.x, corner2.y)) {
      if (checkDirectLine(corner1, corner2) && checkDirectLine(corner2, end)) {
        return { corner1, corner2 }
      }
    }
  }

  // 遍历起点同列的所有点
  for (let y = -1; y <= currentConfig.value.rows; y++) {
    if (y === start.y) continue
    const corner1 = { x: start.x, y }

    if (!checkDirectLine(start, corner1)) continue

    const corner2 = { x: end.x, y }
    if (!hasObstacle(corner1.x, corner1.y) && !hasObstacle(corner2.x, corner2.y)) {
      if (checkDirectLine(corner1, corner2) && checkDirectLine(corner2, end)) {
        return { corner1, corner2 }
      }
    }
  }

  return null
}

// 检查游戏是否结束
const checkGameEnd = () => {
  const totalPairs = (config.difficulties[difficulty.value].rows * config.difficulties[difficulty.value].cols) / 2
  if (matchedPairs.value === totalPairs) {
    endGame(true)
  }
}

// 切换难度
const changeDifficulty = () => {
  // 如果游戏刚开始（时间还没有减少），允许切换难度
  if (!isPlaying.value || timeLeft.value === currentConfig.value.timeLimit) {
    const levels: DifficultyLevel[] = ['初级', '中级', '高级']
    const currentIndex = levels.indexOf(difficulty.value)
    difficulty.value = levels[(currentIndex + 1) % levels.length]
    timeLeft.value = currentConfig.value.timeLimit
    startGame() // 切换难度后立即开始新游戏
  } else {
    showTipMessage('游戏进行中无法切换难度')
  }
}

// 进入下一难度
const nextLevel = () => {
  const levels: DifficultyLevel[] = ['初级', '中级', '高级']
  const currentIndex = levels.indexOf(difficulty.value)
  if (currentIndex < levels.length - 1) {
    difficulty.value = levels[currentIndex + 1]
    startGame()
  }
}

// 组件挂载
onMounted(() => {
  if (!gameCanvas.value) return
  ctx.value = gameCanvas.value.getContext('2d')

  // 设置画布尺寸
  const resizeCanvas = () => {
    const container = gameCanvas.value!.parentElement
    if (!container) return

    const { width, height } = container.getBoundingClientRect()
    gameCanvas.value!.width = width
    gameCanvas.value!.height = height

    if (isPlaying.value) {
      drawBoard()
    }
  }

  resizeCanvas()
  window.addEventListener('resize', resizeCanvas)

  // 添加点击事件监听
  gameCanvas.value.addEventListener('click', (e) => {
    const rect = gameCanvas.value!.getBoundingClientRect()
    const x = Math.floor((e.clientX - rect.left) / (rect.width / currentConfig.value.cols))
    const y = Math.floor((e.clientY - rect.top) / (rect.height / currentConfig.value.rows))

    if (x >= 0 && x < currentConfig.value.cols && y >= 0 && y < currentConfig.value.rows) {
      handleClick(x, y)
    }
  })

  // 监听窗口大小变化
  const handleResize = () => {
    iconSize.value = calculateIconSize()
  }
  window.addEventListener('resize', handleResize)
  handleResize()

  // 自动开始游戏
  startGame()
})

// 组件卸载
onUnmounted(() => {
  stopTimer()
  window.removeEventListener('resize', () => {})
})
</script>

<style scoped>
.link-game-page {
  min-height: 100vh;
  background: #faf8ef;
  margin: 0 !important;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  font-family: "Clear Sans", "Helvetica Neue", Arial, sans-serif;
  touch-action: none;
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
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

.tools-container {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 20px;
}

.tool-item {
  position: relative;
  width: 60px;
  height: 60px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.tool-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.tool-item.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tool-icon {
  font-size: 24px;
  margin-bottom: 4px;
}

.tool-name {
  font-size: 12px;
  color: #666;
}

.cooldown-mask {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
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

.modal-content {
  background: #fff;
  padding: 40px;
  border-radius: 12px;
  text-align: center;
  animation: slide-up 400ms ease-in-out;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 300px;
  max-width: 90%;
}

.game-over-title {
  font-size: 32px;
  color: #776e65;
  margin-bottom: 24px;
}

.game-over-stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 32px;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #776e65;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #776e65;
}

.game-over-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.ranking-modal {
  min-width: 400px;
}

.ranking-title {
  font-size: 24px;
  color: #776e65;
  margin-bottom: 20px;
}

.ranking-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.ranking-tab {
  padding: 8px 16px;
  border-radius: 20px;
  background: #eee4da;
  color: #776e65;
  cursor: pointer;
  transition: all 0.3s ease;
}

.ranking-tab.active {
  background: #8f7a66;
  color: #f9f6f2;
}

.ranking-list {
  max-height: 400px;
  overflow-y: auto;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #eee4da;
}

.ranking-rank {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #eee4da;
  color: #776e65;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 16px;
}

.ranking-info {
  flex: 1;
  text-align: left;
}

.ranking-score {
  font-size: 18px;
  font-weight: bold;
  color: #776e65;
}

.ranking-details {
  font-size: 14px;
  color: #a9a9a9;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slide-up {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media screen and (max-width: 768px) {
  .link-game-page {
    align-items: center;
    padding: 0;
    overflow: hidden;
    min-height: 100vh;
    background: #faf8ef;

  }

  .game-container {
    width: 100%;
    height: 100vh;
    padding: 10px 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    box-shadow: none;
    border-radius: 0;

  }

  .game-header {
    flex-direction: column;
    gap: 8px;
    margin-bottom: 8px;
    padding: 0 10px;
  }

  .game-title {
    font-size: 18px;
    margin-bottom: 4px;
  }

  .scores-container {
    width: 100%;
    justify-content: space-between;
    gap: 6px;
  }

  .score-box {
    padding: 4px 8px;
    min-width: auto;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  }

  .score-label {
    font-size: 9px;
    margin-bottom: 1px;
  }

  .score-value {
    font-size: 12px;
  }

  .game-controls {
    gap: 4px;
    margin-bottom: 8px;
    flex-wrap: nowrap;
    padding: 0 6px;
    justify-content: space-between;
  }

  .control-button {
    padding: 0 8px;
    height: 24px;
    line-height: 24px;
    font-size: 14px!important;
    gap: 3px;
    flex: 1;
    min-width: 0;
    white-space: nowrap;
  }

  .control-button :deep(svg) {
    width: 12px;
    height: 12px;
  }

  .tools-container {
    gap: 8px;
    margin-bottom: 8px;
    flex-wrap: wrap;
    padding: 0 6px;
    justify-content: space-between;
  }

  .tool-item {
    width: calc(25% - 6px);
    height: 36px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    flex-direction: row;
    gap: 4px;
    justify-content: center;
    margin: 0;
  }

  .tool-icon {
    font-size: 16px;
    margin-bottom: 0;
  }

  .tool-name {
    font-size: 8px;
  }

  .game-board-container {
    max-height: calc(100vh - 180px);
    margin-bottom: 74px;
  }

  .tip-message {
    font-size: 12px;
    padding: 6px 12px;
    white-space: nowrap;
    background: rgba(0, 0, 0, 0.8);
  }

  .modal-content {
    padding: 20px;
    border-radius: 8px;
  }

  .game-over-title {
    font-size: 24px;
    margin-bottom: 16px;
  }

  .game-over-stats {
    margin-bottom: 20px;
  }

  .stat-label {
    font-size: 12px;
  }

  .stat-value {
    font-size: 18px;
  }

  .game-over-buttons {
    gap: 10px;
  }

  .game-over-buttons .control-button {
    height: 32px;
    line-height: 32px;
    padding: 0 15px;
  }

  .game-over-modal {
    width: 90%;
    max-width: 320px;
  }

  .game-over-stats {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .stat-item {
    text-align: center;
  }

  .game-over-buttons {
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }

  .game-over-buttons .control-button {
    width: 100%;
    height: 36px;
    line-height: 36px;
    justify-content: center;
    font-size: 18px;
  }

  .game-over-buttons .next-level {
    order: -1;
  }
}

.link-link-container {
  width: 100%;
  height: calc(100vh - 88px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 20px;
  box-sizing: border-box;
  background: var(--background-color);
}

@media screen and (max-width: 768px) {
  .link-link-container {
    height: 100vh;
    padding: 10px;
  }
}

.game-board {
  width: 100%;
  height: 100%;
  display: grid;
  gap: 2px;
  padding: 10px;
  box-sizing: border-box;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  position: relative;
}

.controls {
  position: fixed;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

@media screen and (max-width: 768px) {
  .controls {
    bottom: 20px;
    padding: 12px;
    gap: 12px;
  }

  .control-button {
    font-size: 20px;
  }

  .control-text {
    display: none;
  }
}

.tip-message {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  z-index: 1001;
  animation: fade-in 800ms ease-in-out;
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.next-level {
  background: #ff8e53;
}

.next-level:hover {
  background: #ff9e63;
}
</style>
