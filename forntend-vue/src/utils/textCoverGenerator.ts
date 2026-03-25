/**
 * 文字封面生成工具
 * 类似小红书的文字封面生成功能
 */

// 预设的柔和色彩方案
const colorSchemes = [
  {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#a8c8f5'
  },
  {
    background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#ffd1e8'
  },
  {
    background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#b3e5ff'
  },
  {
    background: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#a8ffdb'
  },
  {
    background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#ffeb99'
  },
  {
    background: 'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
    textColor: '#333333',
    shadowColor: 'rgba(255,255,255,0.8)',
    accentColor: '#d4f7f5'
  },
  {
    background: 'linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)',
    textColor: '#333333',
    shadowColor: 'rgba(255,255,255,0.8)',
    accentColor: '#e8c5d9'
  },
  {
    background: 'linear-gradient(135deg, #89f7fe 0%, #66a6ff 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#c7fbff'
  },
  {
    background: 'linear-gradient(135deg, #fdbb2d 0%, #22c1c3 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#7ed3d5'
  },
  {
    background: 'linear-gradient(135deg, #ee9ca7 0%, #ffdde1 100%)',
    textColor: '#333333',
    shadowColor: 'rgba(255,255,255,0.8)',
    accentColor: '#f6cdd1'
  },
  {
    background: 'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
    textColor: '#333333',
    shadowColor: 'rgba(255,255,255,0.8)',
    accentColor: '#fff6e5'
  },
  {
    background: 'linear-gradient(135deg, #a8e6cf 0%, #dcedc1 100%)',
    textColor: '#333333',
    shadowColor: 'rgba(255,255,255,0.8)',
    accentColor: '#c4f0d4'
  },
  // 新增更多动感配色
  {
    background: 'linear-gradient(45deg, #ff9a9e 0%, #fecfef 50%, #fecfef 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.2)',
    accentColor: '#ffe1f2'
  },
  {
    background: 'linear-gradient(135deg, #667db6 0%, #0082c8 25%, #0082c8 50%, #667db6 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.3)',
    accentColor: '#a3c4e8'
  },
  {
    background: 'linear-gradient(45deg, #874da2 0%, #c43a30 100%)',
    textColor: '#ffffff',
    shadowColor: 'rgba(0,0,0,0.4)',
    accentColor: '#b689c9'
  }
]

/**
 * 绘制动态背景元素
 */
function drawDynamicBackground(ctx: CanvasRenderingContext2D, width: number, height: number, colorScheme: any, random: SeededRandom) {
  // 绘制流动的波浪形状
  ctx.save()
  ctx.globalAlpha = 0.1
  ctx.fillStyle = colorScheme.accentColor

  for (let i = 0; i < 3; i++) {
    ctx.beginPath()
    const waveHeight = 60 + i * 20
    const waveLength = width / 3
    const phase = i * Math.PI / 3

    ctx.moveTo(0, height - waveHeight)
    for (let x = 0; x <= width; x += 10) {
      const y = height - waveHeight + Math.sin((x / waveLength) * Math.PI * 2 + phase) * 20
      ctx.lineTo(x, y)
    }
    ctx.lineTo(width, height)
    ctx.lineTo(0, height)
    ctx.closePath()
    ctx.fill()
  }

  ctx.restore()
}

/**
 * 绘制几何装饰元素
 */
function drawGeometricElements(ctx: CanvasRenderingContext2D, width: number, height: number, colorScheme: any, random: SeededRandom) {
  ctx.save()
  ctx.globalAlpha = 0.15
  ctx.strokeStyle = colorScheme.accentColor
  ctx.lineWidth = 2

  // 绘制确定性的几何图形
  const shapes = random.nextInt(2, 5)

  for (let i = 0; i < shapes; i++) {
    const x = random.nextFloat() * width
    const y = random.nextFloat() * height
    const size = random.nextFloat() * 60 + 30
    const shapeType = random.nextInt(0, 3)

    ctx.save()
    ctx.translate(x, y)
    ctx.rotate(random.nextFloat() * Math.PI * 2)

    switch (shapeType) {
      case 0: // 圆形
        ctx.beginPath()
        ctx.arc(0, 0, size / 2, 0, Math.PI * 2)
        ctx.stroke()
        break
      case 1: // 正方形
        ctx.strokeRect(-size / 2, -size / 2, size, size)
        break
      case 2: // 三角形
        ctx.beginPath()
        ctx.moveTo(0, -size / 2)
        ctx.lineTo(-size / 2, size / 2)
        ctx.lineTo(size / 2, size / 2)
        ctx.closePath()
        ctx.stroke()
        break
    }

    ctx.restore()
  }

  ctx.restore()
}

/**
 * 绘制粒子效果
 */
function drawParticleEffect(ctx: CanvasRenderingContext2D, width: number, height: number, colorScheme: any, random: SeededRandom) {
  ctx.save()
  ctx.globalAlpha = 0.3
  ctx.fillStyle = colorScheme.accentColor

  const particleCount = random.nextInt(15, 35)

  for (let i = 0; i < particleCount; i++) {
    const x = random.nextFloat() * width
    const y = random.nextFloat() * height
    const size = random.nextFloat() * 4 + 1

    ctx.beginPath()
    ctx.arc(x, y, size, 0, Math.PI * 2)
    ctx.fill()

    // 添加小星星效果
    if (random.nextFloat() > 0.7) {
      ctx.save()
      ctx.strokeStyle = colorScheme.accentColor
      ctx.lineWidth = 1
      ctx.globalAlpha = 0.5

      ctx.beginPath()
      ctx.moveTo(x - 6, y)
      ctx.lineTo(x + 6, y)
      ctx.moveTo(x, y - 6)
      ctx.lineTo(x, y + 6)
      ctx.stroke()

      ctx.restore()
    }
  }

  ctx.restore()
}

/**
 * 绘制渐变光效
 */
function drawGlowEffect(ctx: CanvasRenderingContext2D, width: number, height: number, colorScheme: any, random: SeededRandom) {
  ctx.save()

  // 创建径向渐变光效
  const glowCount = random.nextInt(1, 3)

  for (let i = 0; i < glowCount; i++) {
    const centerX = random.nextFloat() * width
    const centerY = random.nextFloat() * height
    const radius = random.nextFloat() * 150 + 100

    const gradient = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, radius)
    gradient.addColorStop(0, colorScheme.accentColor + '40')
    gradient.addColorStop(0.5, colorScheme.accentColor + '20')
    gradient.addColorStop(1, colorScheme.accentColor + '00')

    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, width, height)
  }

  ctx.restore()
}

/**
 * 计算字符串的简单哈希值
 */
function simpleHash(str: string): number {
  let hash = 0
  if (str.length === 0) return hash

  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash // 转换为32位整数
  }

  return Math.abs(hash)
}

/**
 * 基于哈希值的随机数生成器
 */
class SeededRandom {
  private seed: number

  constructor(seed: number) {
    this.seed = seed
  }

  // 线性同余生成器
  next(): number {
    this.seed = (this.seed * 9301 + 49297) % 233280
    return this.seed / 233280
  }

  // 生成指定范围内的整数
  nextInt(min: number, max: number): number {
    return Math.floor(this.next() * (max - min)) + min
  }

  // 生成0-1之间的浮点数
  nextFloat(): number {
    return this.next()
  }
}
function smartTextSplit(text: string, maxLength: number = 8): string[] {
  if (!text) return ['']
  const lines: string[] = []
  let buf = ''
  for (let i = 0; i < text.length; i++) {
    buf += text[i]
    if (buf.length >= maxLength) {
      let splitIdx = buf.length - 1
      for (let j = buf.length - 1; j >= Math.max(0, buf.length - 4); j--) {
        if (/\s|[，。！？；：、,.!?:;]/.test(buf[j])) { splitIdx = j; break }
      }
      lines.push(buf.substring(0, splitIdx + 1).trim())
      buf = buf.substring(splitIdx + 1)
    }
  }
  if (buf) lines.push(buf.trim())
  return lines
}

/**
 * 计算文字大小
 */
function calculateFontSize(text: string, width: number, height: number): number {
  const lines = smartTextSplit(text, 8)
  const maxLen = Math.max(...lines.map(l => l.length))
  const base = Math.min(width / (maxLen * 1.0), height / (lines.length * 2.2))
  return Math.max(12, Math.min(40, base))
}

function wrapTextByWidth(ctx: CanvasRenderingContext2D, text: string, maxWidth: number, fontFamily: string, fontSize: number): { lines: string[], fontSize: number, lineHeight: number } {
  let size = fontSize
  const minSize = 12
  const maxHeightRatio = 0.8
  let lines: string[] = []
  let lineHeight = 0
  const buildLines = () => {
    ctx.font = `bold ${size}px ${fontFamily}`
    const out: string[] = []
    let buf = ''
    let lastBreak = -1
    for (let i = 0; i < text.length; i++) {
      const ch = text[i]
      buf += ch
      if (/\s|[，。！？；：、,.!?:;]/.test(ch)) lastBreak = buf.length - 1
      if (ctx.measureText(buf).width > maxWidth) {
        let cut = lastBreak >= 0 ? lastBreak : buf.length - 1
        out.push(buf.substring(0, cut + 1).trim())
        buf = buf.substring(cut + 1)
        lastBreak = -1
      }
    }
    if (buf) out.push(buf.trim())
    return out
  }
  while (true) {
    lines = buildLines()
    lineHeight = size * 1.25
    const totalHeight = lines.length * lineHeight
    if (totalHeight <= ctx.canvas.height * maxHeightRatio) break
    if (size <= minSize) {
      while (lines.length * lineHeight > ctx.canvas.height * maxHeightRatio && lines.length > 0) {
        const last = lines[lines.length - 1]
        let ell = last
        while (ctx.measureText(ell + '…').width > maxWidth && ell.length > 1) {
          ell = ell.slice(0, -1)
        }
        lines[lines.length - 1] = ell + '…'
        lines = lines.slice(0, Math.max(1, Math.floor((ctx.canvas.height * maxHeightRatio) / lineHeight)))
      }
      break
    }
    size = Math.max(minSize, Math.floor(size * 0.9))
  }
  return { lines, fontSize: size, lineHeight }
}

/**
 * 生成文字封面
 */
export function generateTextCover(
  title: string,
  width: number = 300,
  height: number = 400
): Promise<string> {
  return new Promise((resolve) => {
    // 创建 canvas
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')!

    // 设置高分辨率
    const scale = window.devicePixelRatio || 2
    canvas.width = width * scale
    canvas.height = height * scale
    canvas.style.width = width + 'px'
    canvas.style.height = height + 'px'
    ctx.scale(scale, scale)

    // 基于标题生成哈希值，确保相同标题生成相同的封面
    const hash = simpleHash(title)
    const random = new SeededRandom(hash)

    // 使用确定性随机选择配色方案
    const colorScheme = colorSchemes[random.nextInt(0, colorSchemes.length)]

    // 创建渐变背景
    const gradient = ctx.createLinearGradient(0, 0, width, height)
    const gradientMatch = colorScheme.background.match(/linear-gradient\([\d\w\s,%-]+,\s*(#[\w]+|\w+)\s+[\d%]+,\s*(#[\w]+|\w+)\s+[\d%]+\)/)

    if (gradientMatch) {
      const color1 = gradientMatch[1]
      const color2 = gradientMatch[2]
      gradient.addColorStop(0, color1)
      gradient.addColorStop(1, color2)
    } else {
      gradient.addColorStop(0, '#667eea')
      gradient.addColorStop(1, '#764ba2')
    }

    // 绘制背景
    ctx.fillStyle = gradient
    ctx.fillRect(0, 0, width, height)

    // 绘制动态背景元素（使用确定性随机）
    drawDynamicBackground(ctx, width, height, colorScheme, random)

    // 绘制渐变光效
    drawGlowEffect(ctx, width, height, colorScheme, random)

    // 绘制几何装饰元素
    drawGeometricElements(ctx, width, height, colorScheme, random)

    // 绘制粒子效果
    drawParticleEffect(ctx, width, height, colorScheme, random)

    const fontFamily = `-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif`
    const initialSize = calculateFontSize(title, width, height)
    const paddingX = Math.floor(width * 0.1)
    const maxTextWidth = width - paddingX * 2
    const wrapped = wrapTextByWidth(ctx, title, maxTextWidth, fontFamily, initialSize)
    ctx.font = `bold ${wrapped.fontSize}px ${fontFamily}`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    const totalHeight = wrapped.lines.length * wrapped.lineHeight
    const startY = (height - totalHeight) / 2 + wrapped.lineHeight / 2

    // 绘制文字（增强版）
    wrapped.lines.forEach((line, index) => {
      const y = startY + index * wrapped.lineHeight

      // 绘制多层阴影效果
      for (let i = 0; i < 3; i++) {
        ctx.fillStyle = colorScheme.shadowColor
        ctx.globalAlpha = 0.3 - i * 0.1
        ctx.fillText(line, width / 2 + 3 + i, y + 3 + i)
      }

      // 绘制主文字
      ctx.globalAlpha = 1
      ctx.fillStyle = colorScheme.textColor
      ctx.fillText(line, width / 2, y)

      // 添加文字发光效果
      ctx.save()
      ctx.globalAlpha = 0.6
      ctx.shadowColor = colorScheme.textColor
      ctx.shadowBlur = 20
      ctx.fillText(line, width / 2, y)
      ctx.restore()
    })

    // 绘制现代化装饰元素
    ctx.strokeStyle = colorScheme.textColor
    ctx.lineWidth = 3
    ctx.globalAlpha = 0.4
    ctx.lineCap = 'round'

    // 左上角装饰 - 更动感的设计
    ctx.beginPath()
    ctx.moveTo(25, 25)
    ctx.lineTo(50, 25)
    ctx.moveTo(25, 25)
    ctx.lineTo(25, 50)
    // 添加小点
    ctx.moveTo(55, 30)
    ctx.lineTo(60, 30)
    ctx.stroke()

    // 右下角装饰 - 更动感的设计
    ctx.beginPath()
    ctx.moveTo(width - 25, height - 25)
    ctx.lineTo(width - 50, height - 25)
    ctx.moveTo(width - 25, height - 25)
    ctx.lineTo(width - 25, height - 50)
    // 添加小点
    ctx.moveTo(width - 60, height - 30)
    ctx.lineTo(width - 55, height - 30)
    ctx.stroke()

    // 添加确定性的装饰线条
    ctx.lineWidth = 2
    ctx.globalAlpha = 0.2
    for (let i = 0; i < 2; i++) {
      const x = random.nextFloat() * width
      const y = random.nextFloat() * height
      const length = random.nextFloat() * 40 + 20
      const angle = random.nextFloat() * Math.PI * 2

      ctx.beginPath()
      ctx.moveTo(x, y)
      ctx.lineTo(x + Math.cos(angle) * length, y + Math.sin(angle) * length)
      ctx.stroke()
    }

    ctx.globalAlpha = 1

    // 转换为 Data URL
    const dataUrl = canvas.toDataURL('image/png', 0.9)
    resolve(dataUrl)
  })
}

/**
 * 缓存文字封面
 */
const coverCache = new Map<string, string>()

/**
 * 获取文字封面（带缓存）
 */
export async function getTextCover(
  title: string,
  width: number = 300,
  height: number = 400
): Promise<string> {
  const cacheKey = `${title}_${width}_${height}`

  if (coverCache.has(cacheKey)) {
    return coverCache.get(cacheKey)!
  }

  const cover = await generateTextCover(title, width, height)
  coverCache.set(cacheKey, cover)

  // 限制缓存大小
  if (coverCache.size > 100) {
    const firstKey = coverCache.keys().next().value
    coverCache.delete(firstKey)
  }

  return cover
}

/**
 * 清除缓存
 */
export function clearTextCoverCache(): void {
  coverCache.clear()
}
