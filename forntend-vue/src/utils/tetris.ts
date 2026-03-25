// 定义Tetromino类型
type TetrominoType = {
  shape: number[][];
  color: string;
}

type TetrominosType = {
  [key in '0' | 'I' | 'J' | 'L' | 'O' | 'S' | 'T' | 'Z']: TetrominoType;
}

// 俄罗斯方块形状定义
export const TETROMINOS: TetrominosType = {
  0: { shape: [[0]], color: '0' },
  I: {
    shape: [
      [0, 0, 0, 0],
      [1, 1, 1, 1],
      [0, 0, 0, 0],
      [0, 0, 0, 0]
    ],
    color: '1'
  },
  J: {
    shape: [
      [0, 0, 0],
      [2, 2, 2],
      [0, 0, 2]
    ],
    color: '2'
  },
  L: {
    shape: [
      [0, 0, 0],
      [3, 3, 3],
      [3, 0, 0]
    ],
    color: '3'
  },
  O: {
    shape: [
      [4, 4],
      [4, 4]
    ],
    color: '4'
  },
  S: {
    shape: [
      [0, 5, 5],
      [5, 5, 0],
      [0, 0, 0]
    ],
    color: '5'
  },
  T: {
    shape: [
      [0, 0, 0],
      [6, 6, 6],
      [0, 6, 0]
    ],
    color: '6'
  },
  Z: {
    shape: [
      [7, 7, 0],
      [0, 7, 7],
      [0, 0, 0]
    ],
    color: '7'
  }
}

// 随机生成方块
export const randomTetromino = () => {
  const tetrominos = ['I', 'J', 'L', 'O', 'S', 'T', 'Z'] as const
  const randTetromino = tetrominos[Math.floor(Math.random() * tetrominos.length)]
  return TETROMINOS[randTetromino]
}

// 旋转方块
export const rotate = (matrix: number[][], dir: number) => {
  // 转置矩阵
  const transposed = matrix.map((_, index) =>
    matrix.map(col => col[index])
  )
  // 根据方向翻转行
  if (dir > 0) {
    return transposed.map(row => row.reverse())
  }
  return transposed.reverse()
}

// 检查碰撞
export const checkCollision = (
  player: { pos: { x: number, y: number }, tetromino: { shape: number[][] } },
  board: number[][],
  { x: moveX, y: moveY }: { x: number, y: number }
) => {
  for (let y = 0; y < player.tetromino.shape.length; y++) {
    for (let x = 0; x < player.tetromino.shape[y].length; x++) {
      // 1. 检查是否在方块内
      if (player.tetromino.shape[y][x] !== 0) {
        const nextY = y + player.pos.y + moveY;
        const nextX = x + player.pos.x + moveX;

        // 2. 检查是否超出左右边界
        if (nextX < 0 || nextX >= board[0].length) {
          return true;
        }

        // 3. 检查是否超出底部边界
        if (nextY >= board.length) {
          return true;
        }

        // 4. 检查是否与其他方块重叠（仅当在游戏板范围内时）
        if (nextY >= 0 && board[nextY][nextX] !== 0) {
          return true;
        }
      }
    }
  }
  return false;
}

// 创建游戏板
export const createBoard = (width: number, height: number) =>
  Array.from(Array(height), () => Array(width).fill(0))

// 检查并清除完整的行
export const clearRows = (board: number[][]) => {
  let linesCleared = 0
  const newBoard = board.reduce((acc, row) => {
    if (row.every(cell => cell !== 0)) {
      linesCleared++
      acc.unshift(new Array(row.length).fill(0))
      return acc
    }
    acc.push(row)
    return acc
  }, [] as number[][])

  return { newBoard, linesCleared }
}

// 计算得分
export const calculateScore = (linesCleared: number, level: number) => {
  const linePoints = [40, 100, 300, 1200]
  return linesCleared > 0 ? linePoints[linesCleared - 1] * (level + 1) : 0
}

// 计算等级
export const calculateLevel = (linesCleared: number) => {
  return Math.floor(linesCleared / 10)
}

// 计算下落速度
export const calculateDropTime = (level: number) => {
  // 基础下落速度为800ms，随等级提升逐渐加快
  // 最快速度为200ms，确保玩家有足够的反应时间
  return Math.max(800 - (level * 60), 200)
}

// 获取虚影位置
export const getGhostPosition = (
  player: { pos: { x: number, y: number }, tetromino: { shape: number[][] } },
  board: number[][]
) => {
  const ghost = {
    pos: { ...player.pos },
    tetromino: player.tetromino
  }

  while (!checkCollision(ghost, board, { x: 0, y: 1 })) {
    ghost.pos.y++
  }
  return ghost
}

// 更新游戏板
export const updateBoard = (
  board: number[][],
  player: { pos: { x: number, y: number }, tetromino: { shape: number[][], color: string } }
) => {
  const newBoard = board.map(row => [...row])

  player.tetromino.shape.forEach((row, y) => {
    row.forEach((value, x) => {
      if (value !== 0) {
        const boardY = y + player.pos.y
        const boardX = x + player.pos.x
        // 只渲染在游戏区域内的部分
        if (boardY >= 0 && boardY < board.length && boardX >= 0 && boardX < board[0].length) {
          newBoard[boardY][boardX] = parseInt(player.tetromino.color)
        }
      }
    })
  })
  return newBoard
}
