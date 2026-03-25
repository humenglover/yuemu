<template>
  <div :class="['audio-bubble', { 'is-self': isSelf }]" @click="debouncedTogglePlay">
    <div class="audio-content">
      <div class="play-button">
        <i class="fas fa-play-circle play-icon" v-if="!isPlaying"></i>
        <i class="fas fa-pause-circle pause-icon" v-else></i>
      </div>
      <div class="audio-info">
        <div class="wave-container">
          <div ref="waveformRef" class="waveform"></div>
          <div class="wave-group" v-show="!isWaveformReady">
            <div v-for="i in 4" :key="i" :class="['wave-bar', { 'active': isPlaying }]"
                 :style="{ animationDelay: `${i * 0.15}s` }">
            </div>
          </div>
        </div>
        <div class="time-info">
          <span class="duration">{{ audioDuration }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { PlayCircleFilled, PauseCircleFilled } from '@ant-design/icons-vue'
import { debounce } from 'lodash-es'
import WaveSurfer from 'wavesurfer.js'

const props = defineProps<{
  url: string
  isSelf: boolean
}>()

const isPlaying = ref(false)
const duration = ref<number>(0)
const isLoading = ref(true)
const waveformRef = ref<HTMLElement | null>(null)
const wavesurfer = ref<WaveSurfer | null>(null)
const isWaveformReady = ref(false)

// 格式化音频时长
const audioDuration = computed(() => {
  if (isLoading.value) {
    return '加载中...'
  }
  if (!duration.value || isNaN(duration.value)) {
    return '0:00'
  }
  const minutes = Math.floor(duration.value / 60)
  const seconds = Math.floor(duration.value % 60)
  // 兼容性处理padStart
  const paddedSeconds = seconds < 10 ? `0${seconds}` : `${seconds}`;
  return `${minutes}:${paddedSeconds}`
})

// 停止所有其他音频播放
const stopAllOtherAudio = () => {
  document.querySelectorAll('audio').forEach(a => a.pause())
  document.querySelectorAll('wavesurfer').forEach((ws: any) => {
    if (ws !== wavesurfer.value) {
      ws.pause()
    }
  })
}

// 音频播放切换
const togglePlay = async () => {
  if (!wavesurfer.value || isLoading.value) return

  try {
    if (isPlaying.value) {
      wavesurfer.value.pause()
    } else {
      stopAllOtherAudio()
      // 添加播放前的检查
      if (wavesurfer.value.isPlaying()) {
        wavesurfer.value.pause()
      }
      wavesurfer.value.play()
    }
  } catch (error) {
    console.error('音频播放失败:', error)
    isPlaying.value = false
  }
}

// 添加防抖处理
const debouncedTogglePlay = debounce(togglePlay, 100)

// 初始化 WaveSurfer
const initWaveSurfer = () => {
  if (!waveformRef.value) return

  // 销毁之前的实例
  if (wavesurfer.value) {
    try {
      if (wavesurfer.value.isPlaying()) {
        wavesurfer.value.pause()
      }
    } catch (e) {
      // 忽略错误
    }
    wavesurfer.value.destroy()
  }

  try {
    wavesurfer.value = WaveSurfer.create({
      container: waveformRef.value,
      waveColor: props.isSelf ? 'rgba(255, 255, 255, 0.5)' : '#bae7ff',
      progressColor: props.isSelf ? '#fff' : '#1890ff',
      cursorColor: 'transparent',
      barWidth: 2,
      barGap: 1,
      barRadius: 1,
      height: 16,
      normalize: true,
      interact: false,
      responsive: true,
      partialRender: true,
      forceDecode: false,
      xhr: {
        cache: 'default',
        timeout: 10000,
        headers: [
          ['Accept', 'audio/*']
        ]
      }
    })

    wavesurfer.value.on('ready', () => {
      isLoading.value = false
      isWaveformReady.value = true
      duration.value = wavesurfer.value?.getDuration() || 0
    })

    wavesurfer.value.on('play', () => {
      isPlaying.value = true
    })

    wavesurfer.value.on('pause', () => {
      isPlaying.value = false
    })

    wavesurfer.value.on('finish', () => {
      isPlaying.value = false
    })

    wavesurfer.value.on('error', (error) => {
      console.error('WaveSurfer 错误:', error)
      isLoading.value = false
      duration.value = 0
    })

    // 添加加载超时处理
    const loadTimeout = setTimeout(() => {
      if (isLoading.value) {
        console.warn('音频加载超时')
        isLoading.value = false
      }
    }, 15000)

    // 加载音频
    wavesurfer.value.load(props.url)

    // 清理超时
    wavesurfer.value.on('ready', () => {
      clearTimeout(loadTimeout)
    })

    wavesurfer.value.on('error', () => {
      clearTimeout(loadTimeout)
    })
  } catch (error) {
    console.error('初始化WaveSurfer失败:', error)
    isLoading.value = false
    duration.value = 0
  }
}

onMounted(() => {
  initWaveSurfer()

  // 监听全局暂停事件
  const handlePauseOthers = () => {
    if (wavesurfer.value && isPlaying.value) {
      try {
        wavesurfer.value.pause()
      } catch (e) {
        // 忽略错误
      }
    }
  }

  window.addEventListener('pauseOtherAudios', handlePauseOthers)

  // 清理事件监听器
  onUnmounted(() => {
    window.removeEventListener('pauseOtherAudios', handlePauseOthers)
  })
})

onUnmounted(() => {
  if (wavesurfer.value) {
    // 先暂停播放再销毁
    try {
      if (wavesurfer.value.isPlaying()) {
        wavesurfer.value.pause()
      }
    } catch (e) {
      // 忽略错误
    }
    wavesurfer.value.destroy()
    wavesurfer.value = null
  }
  // 清理其他资源
  isLoading.value = false
  isPlaying.value = false
})
</script>

<style lang="scss" scoped>
.audio-bubble {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s ease;
  min-width: 120px;
  max-width: 200px;
  position: relative;

  &.is-self {
    background: linear-gradient(135deg, #1890ff 0%, #69c0ff 100%);

    .play-button {
      color: rgba(255, 255, 255, 0.95);

      &:hover {
        color: #fff;
      }

      .anticon {
        font-size: 24px;
      }
    }

    .wave-bar {
      background: rgba(255, 255, 255, 0.8);
    }

    .time-info {
      color: rgba(255, 255, 255, 0.9);
    }

    .waveform {
      opacity: 0.8;
    }
  }

  &:not(.is-self) {
    background: #fff;
    border: 1px solid #d9d9d9;

    .play-button {
      color: #1890ff;

      &:hover {
        color: #40a9ff;
      }

      .anticon {
        font-size: 24px;
      }
    }

    .wave-bar {
      background: #bae7ff;

      &.active {
        background: #1890ff;
      }
    }

    .time-info {
      color: rgba(0, 0, 0, 0.65);
    }
  }

  &:hover {
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  .audio-content {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
  }

  .play-button {
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    flex-shrink: 0;
  }

  .audio-info {
    flex-grow: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  .wave-container {
    height: 20px;
    display: flex;
    align-items: center;
    position: relative;
  }

  .waveform {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    opacity: 0.9;
  }

  .wave-group {
    display: flex;
    align-items: center;
    gap: 2px;
    height: 100%;
  }

  .wave-bar {
    width: 3px;
    height: 16px;
    border-radius: 1px;
    transition: all 0.2s ease;

    &.active {
      animation: wave 1s ease-in-out infinite;

      @for $i from 1 through 4 {
        &:nth-child(#{$i}) {
          animation-delay: #{$i * 0.15}s;
        }
      }
    }
  }

  .time-info {
    font-size: 12px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .duration {
    font-variant-numeric: tabular-nums;
  }
}

@keyframes wave {
  0%, 100% {
    transform: scaleY(0.4);
  }
  50% {
    transform: scaleY(1);
  }
}
</style>
