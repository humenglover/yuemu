<template>
  <div class="game-2048-page" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd">
    <!-- 添加背景动画 -->
    <div class="background-animation">
      <div class="gradient-bg"></div>
    </div>

    <div class="game-container">
      <!-- 游戏头部 -->
      <div class="game-header">
        <h2 class="game-title">2048</h2>
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
        <div class="control-button" @click="newGame">
          <redo-outlined />
          <span class="button-text">新游戏</span>
        </div>
        <div class="control-button" @click="showRankingModal = true">
          <trophy-outlined />
          <span class="button-text">排行榜</span>
        </div>
        <div class="control-button" @click="showHistoryModal = true">
          <history-outlined />
          <span class="button-text">历史</span>
        </div>
        <div class="control-button sound-button" @click="toggleSound">
          <sound-filled v-if="isSoundEnabled" />
          <sound-outlined v-else />

        </div>
        <div v-if="won && !keepPlaying" class="control-button" @click="keepPlaying = true">
          <play-circle-outlined />
          <span class="button-text">继续游戏</span>
        </div>
      </div>

      <!-- 游戏网格 -->
      <div class="grid-container">
        <div class="grid-background">
          <div v-for="row in 4" :key="'row-'+row" class="grid-row">
            <div v-for="col in 4" :key="'cell-'+row+'-'+col" class="grid-cell"></div>
          </div>
        </div>
        <div class="tiles-container">
          <div
            v-for="tile in tiles"
            :key="tile.id"
            class="tile"
            :class="{
              [`tile-${tile.value}`]: true,
              'tile-new': tile.isNew,
              'tile-merged': tile.mergedFrom
            }"
            :style="{
              transform: `translate(calc(${tile.y * 100}% + ${tile.y * 12}px), calc(${tile.x * 100}% + ${tile.x * 12}px))`
            }"
          >
            <div class="tile-inner">{{ tile.value }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 游戏结束弹窗 -->
    <div v-if="gameOver || (won && !keepPlaying)" class="modal-overlay" @click.stop>
      <div class="modal-content game-over-modal">
        <h3 class="game-over-title">{{ won ? '恭喜获胜!' : '游戏结束' }}</h3>
        <div class="game-over-stats">
          <div class="stat-item">
            <div class="stat-label">最终得分</div>
            <div class="stat-value highlight">{{ score }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">最大数字</div>
            <div class="stat-value">{{ getMaxTile() }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">游戏时间</div>
            <div class="stat-value">{{ formatTime(gameTime) }}</div>
          </div>
        </div>
        <div class="game-over-buttons">
          <div v-if="won && !keepPlaying" class="control-button" @click="keepPlaying = true">
            <play-circle-outlined />
            <span class="button-text">继续游戏</span>
          </div>
          <div class="control-button" @click="newGame">
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

    <!-- 排行榜弹窗 -->
    <div v-if="showRankingModal" class="modal-overlay" @click.stop @touchmove.stop>
      <div class="modal-content ranking-modal" @click.stop>
        <h3 class="ranking-title">排行榜</h3>
        <div class="ranking-list" @touchmove.stop>
          <div v-for="(record, index) in rankingList" :key="index" class="ranking-item">
            <div class="rank-number">{{ index + 1 }}</div>
            <div class="player-info"
                 @click="handleUserClick(record)"
                 style="cursor: pointer;"
                 :title="'查看' + (record.userName || '匿名玩家') + '的主页'">
              <img :src="record.userAvatar || getDefaultAvatar(record.userName) " class="player-avatar" />
              <span class="player-name">{{ record.userName || '匿名玩家' }}</span>
            </div>
            <div class="score-info">
              <div class="ranking-score">{{ record.score }}分</div>
              <div class="ranking-details">
                最大数字: {{ record.maxTile }} | 用时: {{ formatTime(record.gameTime) }}
              </div>
            </div>
          </div>
        </div>
        <div class="modal-close">
          <div class="control-button" @click="showRankingModal = false">
            <close-outlined />
            <span class="button-text">关闭</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 历史记录弹窗 -->
    <div v-if="showHistoryModal" class="modal-overlay" @click.stop @touchmove.stop>
      <div class="modal-content history-modal" @click.stop>
        <h3 class="history-title">历史记录</h3>
        <div class="history-list" @touchmove.stop>
          <div v-for="(record, index) in historyList" :key="index" class="history-item">
            <div class="history-info">
              <div class="history-score">{{ record.score }}分</div>
              <div class="history-details">
                最大数字: {{ record.maxTile }} | 用时: {{ formatTime(record.gameTime) }}
              </div>
              <div class="history-time">{{ formatDate(record.createTime) }}</div>
            </div>
          </div>
        </div>
        <div class="modal-close">
          <div class="control-button" @click="showHistoryModal = false">
            <close-outlined />
            <span class="button-text">关闭</span>
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
import { getUserHighestScoreUsingGet, getRankingListUsingGet, saveGame2048RecordUsingPost, getUserGameHistoryUsingGet } from '@/api/game2048Controller.ts'
import { getDefaultAvatar } from '@/utils/userUtils.ts'
import {
  SoundOutlined,
  SoundFilled,
  RedoOutlined,
  TrophyOutlined,
  HistoryOutlined,
  PlayCircleOutlined,
  CloseOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

// Grid类
class Grid {
  size: number
  cells: any[][]

  constructor(size: number, previousState?: any) {
    this.size = size
    this.cells = previousState ? this.fromState(previousState) : this.empty()
  }

  empty() {
    const cells = []
    for (let x = 0; x < this.size; x++) {
      const row = cells[x] = []
      for (let y = 0; y < this.size; y++) {
        row.push(null)
      }
    }
    return cells
  }

  fromState(state: any) {
    const cells = []
    for (let x = 0; x < this.size; x++) {
      const row = cells[x] = []
      for (let y = 0; y < this.size; y++) {
        const tile = state[x][y]
        row.push(tile ? new Tile(tile.position, tile.value) : null)
      }
    }
    return cells
  }

  randomAvailableCell() {
    const cells = this.availableCells()
    if (cells.length) {
      return cells[Math.floor(Math.random() * cells.length)]
    }
  }

  availableCells() {
    const cells = []
    this.eachCell((x: number, y: number, tile: any) => {
      if (!tile) {
        cells.push({ x: x, y: y })
      }
    })
    return cells
  }

  eachCell(callback: (x: number, y: number, tile: any) => void) {
    for (let x = 0; x < this.size; x++) {
      for (let y = 0; y < this.size; y++) {
        callback(x, y, this.cells[x][y])
      }
    }
  }

  cellsAvailable() {
    return !!this.availableCells().length
  }

  cellAvailable(cell: { x: number, y: number }) {
    return !this.cellOccupied(cell)
  }

  cellOccupied(cell: { x: number, y: number }) {
    return !!this.cellContent(cell)
  }

  cellContent(cell: { x: number, y: number }) {
    if (this.withinBounds(cell)) {
      return this.cells[cell.x][cell.y]
    }
    return null
  }

  insertTile(tile: any) {
    this.cells[tile.x][tile.y] = tile
  }

  removeTile(tile: any) {
    this.cells[tile.x][tile.y] = null
  }

  withinBounds(position: { x: number, y: number }) {
    return position.x >= 0 && position.x < this.size &&
      position.y >= 0 && position.y < this.size
  }
}

// Tile类
class Tile {
  x: number
  y: number
  value: number
  previousPosition: any
  mergedFrom: any

  constructor(position: { x: number, y: number }, value: number) {
    this.x = position.x
    this.y = position.y
    this.value = value || 2
    this.previousPosition = null
    this.mergedFrom = null
  }

  savePosition() {
    this.previousPosition = { x: this.x, y: this.y }
  }

  updatePosition(position: { x: number, y: number }) {
    this.x = position.x
    this.y = position.y
  }
}

// 游戏管理器
class GameManager {
  size: number
  grid: Grid
  score: number
  over: boolean
  won: boolean
  keepPlaying: boolean

  constructor(size: number) {
    this.size = size
    this.score = 0
    this.over = false
    this.won = false
    this.keepPlaying = false
    this.grid = new Grid(size)
    this.addStartTiles()
  }

  // 重置游戏
  restart() {
    this.grid = new Grid(this.size)
    this.score = 0
    this.over = false
    this.won = false
    this.keepPlaying = false
    this.addStartTiles()
  }

  // 继续游戏
  keepPlayingGame() {
    this.keepPlaying = true
  }

  // 游戏是否结束
  isGameTerminated() {
    return this.over || (this.won && !this.keepPlaying)
  }

  // 添加初始方块
  addStartTiles() {
    for (let i = 0; i < 2; i++) {
      this.addRandomTile()
    }
  }

  // 添加随机方块
  addRandomTile() {
    if (this.grid.cellsAvailable()) {
      const value = Math.random() < 0.9 ? 2 : 4
      const cell = this.grid.randomAvailableCell()
      const tile = new Tile(cell, value)
      this.grid.insertTile(tile)
    }
  }

  // 准备方块移动
  prepareTiles() {
    this.grid.eachCell((x: number, y: number, tile: any) => {
      if (tile) {
        tile.mergedFrom = null
        tile.savePosition()
      }
    })
  }

  // 移动方块
  moveTile(tile: any, cell: { x: number, y: number }) {
    this.grid.cells[tile.x][tile.y] = null
    this.grid.cells[cell.x][cell.y] = tile
    tile.updatePosition(cell)
  }

  // 获取移动向量
  getVector(direction: string) {
    const map: { [key: string]: { x: number, y: number } } = {
      'up': { x: -1, y: 0 },
      'right': { x: 0, y: 1 },
      'down': { x: 1, y: 0 },
      'left': { x: 0, y: -1 }
    }
    return map[direction]
  }

  // 构建遍历顺序
  buildTraversals(vector: { x: number, y: number }) {
    const traversals = { x: [], y: [] }

    for (let pos = 0; pos < this.size; pos++) {
      traversals.x.push(pos)
      traversals.y.push(pos)
    }

    if (vector.x === 1) traversals.x.reverse()
    if (vector.y === 1) traversals.y.reverse()

    return traversals
  }

  // 找到最远位置
  findFarthestPosition(cell: { x: number, y: number }, vector: { x: number, y: number }) {
    let previous

    do {
      previous = cell
      cell = { x: previous.x + vector.x, y: previous.y + vector.y }
    } while (this.grid.withinBounds(cell) && this.grid.cellAvailable(cell))

    return {
      farthest: previous,
      next: cell
    }
  }

  // 移动
  move(direction: string) {
    if (this.isGameTerminated()) return

    const vector = this.getVector(direction)
    const traversals = this.buildTraversals(vector)
    let moved = false

    this.prepareTiles()

    traversals.x.forEach((x) => {
      traversals.y.forEach((y) => {
        const cell = { x: x, y: y }
        const tile = this.grid.cellContent(cell)

        if (tile) {
          const positions = this.findFarthestPosition(cell, vector)
          const next = this.grid.cellContent(positions.next)

          if (next && next.value === tile.value && !next.mergedFrom) {
            const merged = new Tile(positions.next, tile.value * 2)
            merged.mergedFrom = [tile, next]

            this.grid.insertTile(merged)
            this.grid.removeTile(tile)

            tile.updatePosition(positions.next)

            this.score += merged.value

            if (merged.value === 2048) this.won = true
          } else {
            this.moveTile(tile, positions.farthest)
          }

          if (!this.positionsEqual(cell, tile)) {
            moved = true
          }
        }
      })
    })

    if (moved) {
      this.addRandomTile()
    }

    // 移动后，无论是否moved都检查游戏是否结束
    if (!this.movesAvailable()) {
      this.over = true
    }
  }

  // 检查位置是否相等
  positionsEqual(first: { x: number, y: number }, second: { x: number, y: number }) {
    return first.x === second.x && first.y === second.y
  }

  // 检查是否还有可用移动
  movesAvailable() {
    return this.grid.cellsAvailable() || this.tileMatchesAvailable()
  }

  // 检查是否有可合并的方块
  tileMatchesAvailable() {
    for (let x = 0; x < this.size; x++) {
      for (let y = 0; y < this.size; y++) {
        const tile = this.grid.cellContent({ x: x, y: y })

        if (tile) {
          // 检查右边
          const right = this.grid.cellContent({ x: x, y: y + 1 })
          if (right && right.value === tile.value) return true

          // 检查下边
          const down = this.grid.cellContent({ x: x + 1, y: y })
          if (down && down.value === tile.value) return true
        }
      }
    }
    return false
  }
}

// 游戏状态
const gameManager = ref<GameManager | null>(null)
const score = ref(0)
const bestScore = ref(parseInt(localStorage.getItem('game2048BestScore') || '0'))
const gameTime = ref(0)
const gameOver = ref(false)
const won = ref(false)
const keepPlaying = ref(false)
const tiles = ref([])
const timeInterval = ref(null)

// 排行榜相关
const showRankingModal = ref(false)
const rankingList = ref([])

// 历史记录相关
const showHistoryModal = ref(false)
const historyList = ref([])

// 触摸操作相关变量
const touchStartX = ref(0)
const touchStartY = ref(0)
const touchEndX = ref(0)
const touchEndY = ref(0)

// 音效相关
const isSoundEnabled = ref(true)
const moveSound = new Audio('/sounds/move.MP3')
const mergeSound = new Audio('/sounds/merge.MP3')
const gameStartSound = new Audio('/sounds/start.MP3')
const gameOverSound = new Audio('/sounds/gameover.MP3')

// 音效音量设置
moveSound.volume = 0.3
mergeSound.volume = 0.4
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

// 添加音效控制函数
const toggleSound = () => {
  isSoundEnabled.value = !isSoundEnabled.value
  // 更新所有音效的音量
  const volume = isSoundEnabled.value ? 0.3 : 0
  moveSound.volume = volume
  mergeSound.volume = volume * 1.33
  gameStartSound.volume = volume * 1.67
  gameOverSound.volume = volume * 1.67
}

// 添加防抖函数
const debounce = (fn: Function, delay: number) => {
  let timer: any = null
  return (...args: any[]) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

// 使用防抖包装保存游戏记录的函数
const debouncedSaveGameRecord = debounce(async () => {
  try {
    const request = {
      score: score.value,
      maxTile: getMaxTile(),
      gameTime: gameTime.value,
      moveCount: moveCount.value
    }
    await saveGame2048RecordUsingPost(request)
    // message.success('游戏记录保存成功')
  } catch (error) {
    message.error('保存游戏记录失败')
  }
}, 1000) // 1秒的防抖延迟

// 键盘事件处理
const handleKeydown = (e: KeyboardEvent) => {
  if (!gameManager.value) return

  const keyMap: { [key: string]: string } = {
    'ArrowUp': 'up',
    'ArrowRight': 'right',
    'ArrowDown': 'down',
    'ArrowLeft': 'left',
    'w': 'up',
    'W': 'up',
    'd': 'right',
    'D': 'right',
    's': 'down',
    'S': 'down',
    'a': 'left',
    'A': 'left'
  }

  const direction = keyMap[e.key]
  if (direction) {
    e.preventDefault()
    handleMove(direction)
  }
}

// 初始化游戏
const initGame = () => {
  gameManager.value = new GameManager(4)
  gameTime.value = 0
  startTimer()
  updateGameState()
}

// 更新游戏状态
const updateGameState = () => {
  if (!gameManager.value) return

  score.value = gameManager.value.score
  gameOver.value = gameManager.value.over
  won.value = gameManager.value.won
  keepPlaying.value = gameManager.value.keepPlaying

  // 游戏结束时停止计时
  if (gameOver.value || (won.value && !keepPlaying.value)) {
    if (timeInterval.value) {
      clearInterval(timeInterval.value)
      timeInterval.value = null
    }
  }

  // 更新方块数据
  const newTiles = []
  gameManager.value.grid.eachCell((x: number, y: number, tile: any) => {
    if (tile) {
      newTiles.push({
        id: `${x}-${y}-${tile.value}`,
        x: x,
        y: y,
        value: tile.value,
        isNew: !tile.previousPosition && !tile.mergedFrom,
        mergedFrom: tile.mergedFrom
      })
    }
  })
  tiles.value = newTiles

  // 更新最高分
  if (score.value > bestScore.value) {
    bestScore.value = score.value
    localStorage.setItem('game2048BestScore', score.value.toString())
  }
}

// 处理移动
const handleMove = (direction: string) => {
  if (!gameManager.value) return

  const previousScore = gameManager.value.score
  const previousState = JSON.stringify(gameManager.value.grid.cells)
  gameManager.value.move(direction)
  const currentState = JSON.stringify(gameManager.value.grid.cells)
  const moved = previousState !== currentState
  updateGameState()

  // 如果分数变化了，说明发生了合并，播放合并音效
  if (gameManager.value.score > previousScore) {
    playSound(mergeSound)
    moveCount.value++
  } else if (moved) { // 如果只是移动了方块，播放移动音效
    playSound(moveSound)
  }

  // 游戏结束时保存记录和播放音效
  if (gameOver.value || (won.value && !keepPlaying.value)) {
    playSound(gameOverSound)
    debouncedSaveGameRecord()
  }
}

// 新游戏
const newGame = () => {
  if (!gameManager.value) return

  gameManager.value.restart()
  gameTime.value = 0
  moveCount.value = 0
  startTimer()
  updateGameState()
  playSound(gameStartSound)
}

// 继续游戏
const continueGame = () => {
  if (!gameManager.value) return

  gameManager.value.keepPlayingGame()
  updateGameState()
}

// 获取当前最大数字
const getMaxTile = () => {
  if (!gameManager.value) return 0

  let max = 0
  gameManager.value.grid.eachCell((x: number, y: number, tile: any) => {
    if (tile) {
      max = Math.max(max, tile.value)
    }
  })
  return max
}

// 开始计时
const startTimer = () => {
  // 清除之前的计时器
  if (timeInterval.value) {
    clearInterval(timeInterval.value)
    timeInterval.value = null
  }

  // 重置游戏时间
  gameTime.value = 0

  // 开始新的计时器
  timeInterval.value = setInterval(() => {
    // 只有在游戏未结束时才计时
    if (!gameOver.value && !(won.value && !keepPlaying.value)) {
      gameTime.value++
    }
  }, 1000)
}

// 格式化时间
const formatTime = (seconds: number) => {
  const minutes = Math.floor(seconds / 60)
  const remainingSeconds = seconds % 60
  return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
}

// 退出游戏
const quitGame = () => {
  router.push('/games')
}

// 获取用户最高分
const fetchUserHighestScore = async () => {
  try {
    const res = await getUserHighestScoreUsingGet()
    if (res.data) {
      bestScore.value = res.data.data===null?0:res.data.data
    }
  } catch (error) {
    message.error('获取最高分失败')
  }
}

// 获取排行榜数据
const getRankingData = async () => {
  try {
    const res = await getRankingListUsingGet({ limit: 100 })
    if (res.data && res.data.data) {
      // 使用 Map 进行去重，键为 "userId_score" 组合
      const uniqueMap = new Map()
      res.data.data.forEach(record => {
        const key = `${record.userId}_${record.score}`
        // 如果已存在相同的键，保留分数较高的记录
        if (!uniqueMap.has(key) || uniqueMap.get(key).score < record.score) {
          uniqueMap.set(key, record)
        }
      })
      // 转换回数组并按分数降序排序
      rankingList.value = Array.from(uniqueMap.values())
        .sort((a, b) => b.score - a.score)
        .slice(0, 100) // 保留前100条记录
    } else {
      rankingList.value = []
    }
  } catch (error) {
    console.error('获取排行榜失败:', error)
    message.error('获取排行榜失败')
    rankingList.value = []
  }
}

// 获取历史记录数据
const getHistoryData = async () => {
  try {
    const res = await getUserGameHistoryUsingGet({
      current: 1,
      pageSize: 50
    })
    if (res.data && res.data.data && res.data.data.records) {
      // 对历史记录也进行去重，保留最新的记录
      const uniqueMap = new Map()
      res.data.data.records.forEach(record => {
        const key = `${record.score}_${record.maxTile}`
        if (!uniqueMap.has(key) || new Date(uniqueMap.get(key).createTime) < new Date(record.createTime)) {
          uniqueMap.set(key, record)
        }
      })
      historyList.value = Array.from(uniqueMap.values())
        .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
    } else {
      historyList.value = []
    }
  } catch (error) {
    console.error('获取历史记录失败:', error)
    message.error('获取历史记录失败')
    historyList.value = []
  }
}

// 格式化日期
const formatDate = (timestamp: string) => {
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

// 移动次数计数
const moveCount = ref(0)

// 处理触摸开始
const handleTouchStart = (event: TouchEvent) => {
  touchStartX.value = event.touches[0].clientX
  touchStartY.value = event.touches[0].clientY
}

// 处理触摸移动
const handleTouchMove = (event: TouchEvent) => {
  event.preventDefault()
}

// 处理触摸结束
const handleTouchEnd = (event: TouchEvent) => {
  touchEndX.value = event.changedTouches[0].clientX
  touchEndY.value = event.changedTouches[0].clientY

  const deltaX = touchEndX.value - touchStartX.value
  const deltaY = touchEndY.value - touchStartY.value

  // 判断滑动方向
  if (Math.abs(deltaX) > Math.abs(deltaY)) {
    // 水平滑动
    if (Math.abs(deltaX) > 30) { // 设置最小滑动距离
      if (deltaX > 0) {
        handleMove('right')
      } else {
        handleMove('left')
      }
    }
  } else {
    // 垂直滑动
    if (Math.abs(deltaY) > 30) { // 设置最小滑动距离
      if (deltaY > 0) {
        handleMove('down')
      } else {
        handleMove('up')
      }
    }
  }
}

// 处理用户点击
const handleUserClick = (record: any) => {
  if (!record) return
  router.push({
    path: `/user/${record.userId}`,
    query: {
      userName: record.userName,
      userAvatar: record.userAvatar,
      userAccount: record.userAccount,
      userProfile: record.userProfile,
      userRole: record.userRole,
      createTime: record.createTime
    }
  })
}

// 组件挂载
onMounted(() => {
  getRankingData()
  fetchUserHighestScore()
  getHistoryData()
  window.addEventListener('keydown', handleKeydown)
  initGame()
})

// 组件卸载
onUnmounted(() => {
  if (timeInterval.value) clearInterval(timeInterval.value)
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.game-2048-page {
  min-height: 88vh;
  background: #faf8ef;
  margin: -20px !important;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: "Clear Sans", "Helvetica Neue", Arial, sans-serif;
  touch-action: none; /* 防止页面滚动 */
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
  max-width: 500px;
  width: 100%;
  padding: 20px;
  background: #faf8ef;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.game-title {
  font-size: 64px;
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
  position: relative;
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
  justify-content: space-between;
  margin-bottom: 20px;
}

.control-button {
  background: #8f7a66;
  border-radius: 6px;
  padding: 0 24px;
  height: 44px;
  line-height: 44px;
  text-align: center;
  color: #f9f6f2;
  font-weight: bold;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  text-decoration: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.control-button:hover {
  background: #9f8b77;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

.grid-container {
  position: relative;
  background: #bbada0;
  border-radius: 6px;
  width: 100%;
  height: 0;
  padding-bottom: 100%;
  margin-bottom: 30px;
}

.grid-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 12px;
}

.grid-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  height: calc(25% - 9px);
}

.grid-row:last-child {
  margin-bottom: 0;
}

.grid-cell {
  flex: 1;
  background: rgba(238, 228, 218, 0.35);
  border-radius: 3px;
}

.tiles-container {
  position: absolute;
  top: 12px;
  left: 12px;
  right: 12px;
  bottom: 12px;
}

.tile {
  position: absolute;
  width: calc(25% - 9px);
  height: calc(25% - 9px);
  transition: transform 0.15s ease-in-out;
  z-index: 1;
}

.tile-inner {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 3px;
  background: #eee4da;
  text-align: center;
  font-weight: bold;
  z-index: 10;
  font-size: min(55px, 12vw);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.15s ease-in-out;
}

/* 不同数字的背景色和字体颜色 */
.tile-2 .tile-inner {
  background: #eee4da;
  color: #776e65;
  font-size: min(55px, 12vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0), inset 0 0 0 1px rgba(255, 255, 255, 0);
}

.tile-4 .tile-inner {
  background: #ede0c8;
  color: #776e65;
  font-size: min(55px, 12vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0), inset 0 0 0 1px rgba(255, 255, 255, 0);
}

.tile-8 .tile-inner {
  background: #f2b179;
  color: #f9f6f2;
  font-size: min(55px, 12vw);
}

.tile-16 .tile-inner {
  background: #f59563;
  color: #f9f6f2;
  font-size: min(50px, 11vw);
}

.tile-32 .tile-inner {
  background: #f67c5f;
  color: #f9f6f2;
  font-size: min(50px, 11vw);
}

.tile-64 .tile-inner {
  background: #f65e3b;
  color: #f9f6f2;
  font-size: min(50px, 11vw);
}

.tile-128 .tile-inner {
  background: #edcf72;
  color: #f9f6f2;
  font-size: min(45px, 9vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0.2381), inset 0 0 0 1px rgba(255, 255, 255, 0.14286);
}

.tile-256 .tile-inner {
  background: #edcc61;
  color: #f9f6f2;
  font-size: min(45px, 9vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0.31746), inset 0 0 0 1px rgba(255, 255, 255, 0.19048);
}

.tile-512 .tile-inner {
  background: #edc850;
  color: #f9f6f2;
  font-size: min(45px, 9vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0.39683), inset 0 0 0 1px rgba(255, 255, 255, 0.2381);
}

.tile-1024 .tile-inner {
  background: #edc53f;
  color: #f9f6f2;
  font-size: min(35px, 7vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0.47619), inset 0 0 0 1px rgba(255, 255, 255, 0.28571);
}

.tile-2048 .tile-inner {
  background: #edc22e;
  color: #f9f6f2;
  font-size: min(35px, 7vw);
  box-shadow: 0 0 30px 10px rgba(243, 215, 116, 0.55556), inset 0 0 0 1px rgba(255, 255, 255, 0.33333);
}

.tile-super .tile-inner {
  background: #3c3a32;
  color: #f9f6f2;
  font-size: min(30px, 6vw);
}

.tile-new .tile-inner {
  animation: appear 200ms ease-in-out;
}

.tile-merged .tile-inner {
  z-index: 20;
  animation: pop 200ms ease-in-out;
}

@keyframes appear {
  0% {
    opacity: 0;
    transform: scale(0);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes pop {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

/* 移动端控制按钮样式 */
.mobile-controls {
  display: none;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .game-container {
    padding: 15px;
    margin: 0 15px;
  }

  .game-title {
    font-size: 48px;
  }

  .score-box {
    padding: 10px 15px;
    min-width: 80px;
  }

  .score-value {
    font-size: 20px;
  }

  .control-button {
    padding: 0;
    width: 44px;
    border-radius: 50%;
  }

  .control-button .button-text {
    display: none;
  }

  .control-button .anticon {
    font-size: 24px;
  }

  .game-controls {
    justify-content: space-around;
    padding: 0 10px;
  }
}

/* 游戏结束弹窗样式 */
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
  overflow: hidden;
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
  margin: 5px;
}

@media screen and (max-width: 768px) {
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

/* 排行榜样式 */
.ranking-modal {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  text-align: center;
  animation: slide-up 400ms ease-in-out;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 20px;
  overflow-y: auto;
  padding: 0 10px;
  flex-grow: 1;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  touch-action: pan-y;
  position: relative;
  max-height: calc(80vh - 140px);
  min-height: 200px;
}

.ranking-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #eee4da;
  border-radius: 8px;
  transition: transform 0.2s;
}

.ranking-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.rank-number {
  font-size: 24px;
  font-weight: bold;
  color: #776e65;
  width: 40px;
  text-align: center;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
}

.player-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.player-name {
  font-size: 16px;
  color: #776e65;
  font-weight: bold;
}

.score-info {
  text-align: right;
}

.ranking-score {
  font-size: 20px;
  font-weight: bold;
  color: #f65e3b;
}

.ranking-details {
  font-size: 12px;
  color: #a39c95;
  margin-top: 4px;
}

.ranking-title {
  font-size: 36px;
  font-weight: bold;
  color: #776e65;
  margin-bottom: 20px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

/* 移动端样式优化 */
@media screen and (max-width: 768px) {
  .ranking-modal {
    padding: 20px;
    margin: 15px;
    width: calc(100% - 30px);
    max-height: 85vh;
  }

  .ranking-list {
    padding: 0 5px;
    margin: 10px -5px;
    gap: 10px;
    max-height: calc(85vh - 120px);
  }

  .ranking-item {
    padding: 12px 10px;
    margin-bottom: 0;
  }

  .ranking-title {
    font-size: 28px;
    margin-bottom: 15px;
  }

  .rank-number {
    font-size: 20px;
    width: 30px;
  }

  .player-avatar {
    width: 36px;
    height: 36px;
  }

  .player-name {
    font-size: 14px;
  }

  .ranking-score {
    font-size: 16px;
  }

  .ranking-details {
    font-size: 11px;
  }

  .modal-close {
    padding-top: 10px;
    margin-top: auto;
  }
}

/* 滚动条样式 */
.ranking-list::-webkit-scrollbar {
  width: 4px;
}

.ranking-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.ranking-list::-webkit-scrollbar-thumb {
  background: rgba(187, 173, 160, 0.8);
  border-radius: 2px;
}

.ranking-list::-webkit-scrollbar-thumb:hover {
  background: rgba(187, 173, 160, 1);
}

.modal-close {
  margin-top: 20px;
  text-align: center;
  flex-shrink: 0;
}

/* 历史记录样式 */
.history-modal {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  text-align: center;
  animation: slide-up 400ms ease-in-out;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.history-title {
  font-size: 32px;
  font-weight: bold;
  color: #776e65;
  margin-bottom: 20px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.1);
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
  overflow-y: auto;
  padding: 0 10px;
  flex-grow: 1;
  -webkit-overflow-scrolling: touch;
  overscroll-behavior: contain;
  touch-action: pan-y;
  position: relative;
  max-height: calc(80vh - 140px);
  min-height: 200px;
}

.history-item {
  background: #eee4da;
  border-radius: 8px;
  padding: 15px;
  transition: transform 0.2s;
}

.history-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.history-info {
  text-align: left;
}

.history-score {
  font-size: 24px;
  font-weight: bold;
  color: #f65e3b;
  margin-bottom: 4px;
}

.history-details {
  font-size: 14px;
  color: #776e65;
  margin-bottom: 4px;
}

.history-time {
  font-size: 12px;
  color: #a39c95;
}

/* 控制按钮图标样式 */
.control-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.control-button i {
  font-size: 16px;
}

.button-text {
  margin-left: 4px;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .history-modal {
    padding: 20px;
    margin: 15px;
    width: calc(100% - 30px);
    max-height: 85vh;
  }

  .history-list {
    padding: 0 5px;
    margin: 10px -5px;
    gap: 10px;
    max-height: calc(85vh - 120px);
  }

  .history-title {
    font-size: 28px;
    margin-bottom: 15px;
  }

  .history-item {
    padding: 12px;
  }

  .history-score {
    font-size: 20px;
  }

  .history-details {
    font-size: 13px;
  }

  .history-time {
    font-size: 11px;
  }
}

/* 滚动条样式 */
.history-list::-webkit-scrollbar {
  width: 4px;
}

.history-list::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb {
  background: rgba(187, 173, 160, 0.8);
  border-radius: 2px;
}

.history-list::-webkit-scrollbar-thumb:hover {
  background: rgba(187, 173, 160, 1);
}

/* 添加响应式调整 */
@media screen and (max-width: 500px) {
  .tile-2 .tile-inner,
  .tile-4 .tile-inner,
  .tile-8 .tile-inner {
    font-size: 10vw;
  }

  .tile-16 .tile-inner,
  .tile-32 .tile-inner,
  .tile-64 .tile-inner {
    font-size: 9vw;
  }

  .tile-128 .tile-inner,
  .tile-256 .tile-inner,
  .tile-512 .tile-inner {
    font-size: 8vw;
  }

  .tile-1024 .tile-inner,
  .tile-2048 .tile-inner {
    font-size: 6vw;
  }

  .tile-super .tile-inner {
    font-size: 5vw;
  }
}

.sound-button {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sound-button .anticon {
  font-size: 20px;
}
</style>
