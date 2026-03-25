<template>
  <div class="vine-decoration" :class="`season-${activeSeason}`" aria-hidden="true" :title="`当前季节主题：${activeSeason}`">
    <svg viewBox="0 0 400 30" width="100%" height="100%" preserveAspectRatio="xMidYMid slice" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient :id="`vine-grad-${activeSeason}`" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" :stop-color="config.stop1" stop-opacity="0" />
          <stop offset="30%" :stop-color="config.stop2" stop-opacity="0.9" />
          <stop offset="70%" :stop-color="config.stop3" stop-opacity="0.9" />
          <stop offset="100%" :stop-color="config.stop4" stop-opacity="0" />
        </linearGradient>
        <filter :id="`vine-glow-${activeSeason}`" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="1.5" result="blur" />
          <feComposite in="SourceGraphic" in2="blur" operator="over" />
        </filter>
      </defs>

      <!-- 两根柔和交织的发光细线（抽象藤蔓） -->
      <path class="elegant-vine-1" d="M 0 15 Q 50 6 100 15 T 200 15 T 300 15 T 400 15" fill="none" :stroke="`url(#vine-grad-${activeSeason})`" stroke-width="1.5" :filter="`url(#vine-glow-${activeSeason})`" />
      <path class="elegant-vine-2" d="M 0 15 Q 50 24 100 15 T 200 15 T 300 15 T 400 15" fill="none" :stroke="`url(#vine-grad-${activeSeason})`" stroke-width="1" opacity="0.6" />

      <!-- 分布的发光点（嫩芽/冰晶/落叶），采用原生 SVG 动画极致流畅 -->
      <circle cx="50" cy="11" r="0" :fill="config.bud1" :filter="`url(#vine-glow-${activeSeason})`">
        <animate attributeName="r" values="0; 2; 1; 0" dur="3s" begin="0s" repeatCount="indefinite" />
        <animate attributeName="opacity" values="0; 1; 0.5; 0" dur="3s" begin="0s" repeatCount="indefinite" />
      </circle>
      <circle cx="150" cy="19" r="0" :fill="config.bud2" :filter="`url(#vine-glow-${activeSeason})`">
        <animate attributeName="r" values="0; 2.5; 1.5; 0" dur="4s" begin="1s" repeatCount="indefinite" />
        <animate attributeName="opacity" values="0; 1; 0.6; 0" dur="4s" begin="1s" repeatCount="indefinite" />
      </circle>
      <circle cx="250" cy="11" r="0" :fill="config.bud3" :filter="`url(#vine-glow-${activeSeason})`">
        <animate attributeName="r" values="0; 1.5; 1; 0" dur="2.5s" begin="2s" repeatCount="indefinite" />
        <animate attributeName="opacity" values="0; 0.8; 0.4; 0" dur="2.5s" begin="2s" repeatCount="indefinite" />
      </circle>
      <circle cx="350" cy="19" r="0" :fill="config.bud4" :filter="`url(#vine-glow-${activeSeason})`">
        <animate attributeName="r" values="0; 2; 1; 0" dur="3.5s" begin="0.5s" repeatCount="indefinite" />
        <animate attributeName="opacity" values="0; 0.9; 0.5; 0" dur="3.5s" begin="0.5s" repeatCount="indefinite" />
      </circle>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  season: {
    type: String,
    default: 'auto', // 'auto' | 'spring' | 'summer' | 'autumn' | 'winter'
  }
})

// 智能判定当前所在的季节
const activeSeason = computed(() => {
  if (props.season !== 'auto') return props.season
  const month = new Date().getMonth() + 1 // 1-12 (自然月)
  if (month >= 3 && month <= 5) return 'spring'
  if (month >= 6 && month <= 8) return 'summer'
  if (month >= 9 && month <= 11) return 'autumn'
  return 'winter'
})

// 四季调色盘：包含渐变的四阶色带主干流光色与 4 个动态点缀色
// 根据不同季节的情绪注入截然不同的视觉能量
const seasonColors = {
  // 春季：嫩绿与翡翠交织，充满生机勃勃的气息
  spring: {
    stop1: '#10b981', stop2: '#34d399', stop3: '#10b981', stop4: '#059669',
    bud1: '#a7f3d0', bud2: '#34d399', bud3: '#a7f3d0', bud4: '#10b981'
  },
  // 夏季：绿松石与深海蓝叠加，浓郁且极具通透感
  summer: {
    stop1: '#059669', stop2: '#06b6d4', stop3: '#0ea5e9', stop4: '#0369a1',
    bud1: '#67e8f9', bud2: '#22d3ee', bud3: '#67e8f9', bud4: '#06b6d4'
  },
  // 秋季：明橙与琥珀火色，代表丰收的柔和落叶辉光
  autumn: {
    stop1: '#ea580c', stop2: '#f59e0b', stop3: '#ea580c', stop4: '#dc2626',
    bud1: '#fde68a', bud2: '#fbbf24', bud3: '#fde68a', bud4: '#f59e0b'
  },
  // 冬季：霜银白与冰晶蓝，纯粹高雅的雪花冷光源
  winter: {
    stop1: '#3b82f6', stop2: '#93c5fd', stop3: '#60a5fa', stop4: '#2563eb',
    bud1: '#e0f2fe', bud2: '#bae6fd', bud3: '#e0f2fe', bud4: '#7dd3fc'
  }
}

const config = computed(() => {
  return seasonColors[activeSeason.value as keyof typeof seasonColors] || seasonColors['spring']
})
</script>

<style scoped>
/* 藤蔓动画特效区 - 自动流光版 */
.vine-decoration {
  flex: 1;
  margin: 0 40px;
  height: 24px;
  opacity: 0.85;
  overflow: hidden;
  position: relative;
  /* 平滑的边缘渐变消失，隐入虚空 */
  mask-image: linear-gradient(to right, transparent, black 15%, black 85%, transparent);
  -webkit-mask-image: linear-gradient(to right, transparent, black 15%, black 85%, transparent);
}

.elegant-vine-1, .elegant-vine-2 {
  stroke-dasharray: 400;
  stroke-dashoffset: 400;
}

.elegant-vine-1 {
  animation: flowLine 2s cubic-bezier(0.25, 1, 0.5, 1) forwards;
}

.elegant-vine-2 {
  animation: flowLine 2.5s cubic-bezier(0.25, 1, 0.5, 1) 0.2s forwards;
}

@keyframes flowLine {
  0% { stroke-dashoffset: 400; opacity: 0; }
  10% { opacity: 1; }
  100% { stroke-dashoffset: 0; opacity: 1; }
}

/* 黑暗模式增加一定亮度发光，防止与暗黑背景融为一体 */
:global(.dark-theme) .vine-decoration {
  filter: brightness(1.2);
}
</style>
