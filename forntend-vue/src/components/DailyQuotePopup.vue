<!-- 每日一句弹框组件 -->
<template>
  <div v-if="visible" class="quote-popup" @click="handleClose">
    <div class="quote-popup-content" @click.stop>
      <div class="quote-popup-close" @click="handleClose">
        <CloseOutlined />
      </div>
      <h1 class="quote-popup-title">每日一句</h1>
      <div class="loading" v-if="loading">加载中...</div>
      <div v-else class="quote-content">
        <img
          :src="quote.imageUrl"
          class="quote-image"
          alt="每日一句图片"
          loading="lazy"
        >
        <p class="quote-text" :data-text="quote.content">{{ quote.content }}</p>
        <p class="quote-english" :data-text="quote.english">{{ quote.english }}</p>
        <div class="audio-controls">
          <button class="audio-btn" @click="playAudio" :disabled="isPlaying">
            <SoundOutlined v-if="!isPlaying" />
            <LoadingOutlined v-else spin />
            {{ isPlaying ? '朗读中...' : '朗读' }}
          </button>
        </div>
        <div class="quote-update-time">更新时间：{{ quote.updateTime }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CloseOutlined, SoundOutlined, LoadingOutlined } from '@ant-design/icons-vue'
import { ref } from 'vue'

interface Quote {
  content: string
  english: string
  imageUrl: string
  updateTime: string
}

interface Props {
  visible: boolean
  quote: Quote
}

interface Emits {
  (e: 'update:visible', value: boolean): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const loading = ref(false)
const isPlaying = ref(false)

const handleClose = () => {
  emit('update:visible', false)
}

const playAudio = () => {
  if ('speechSynthesis' in window) {
    isPlaying.value = true

    // 创建语音实例
    const utterance = new SpeechSynthesisUtterance()
    utterance.text = props.quote.content + '。' + props.quote.english
    utterance.lang = 'zh-CN' // 设置语言为中文
    utterance.rate = 0.9 // 设置语速稍慢一点
    utterance.pitch = 1 // 设置音高

    // 朗读结束时的回调
    utterance.onend = () => {
      isPlaying.value = false
    }

    // 发生错误时的回调
    utterance.onerror = () => {
      isPlaying.value = false
    }

    // 开始朗读
    window.speechSynthesis.speak(utterance)
  }
}
</script>

<style scoped lang="scss">
.quote-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1112000 !important;
  backdrop-filter: blur(8px);
  animation: fadeIn 0.3s ease-out;
}

.quote-popup-content {
  background: #ffffff;
  color: #334155;
  padding: 40px 32px 32px;
  border-radius: 24px;
  max-width: 90%;
  width: 560px;
  max-height: 85vh;
  position: relative;
  box-shadow: 0 20px 60px rgba(37, 99, 235, 0.15);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid rgba(37, 99, 235, 0.1);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.quote-popup-title {
  font-size: 24px;
  font-weight: 600;
  text-align: center;
  color: #2563eb;
  margin: 0 0 24px;
  position: sticky;
  top: 0;
  padding-top: 8px;
  padding-bottom: 8px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
  z-index: 1;
}

.loading {
  text-align: center;
  color: #64748b;
  margin: 40px 0;
  font-size: 16px;
}

.quote-content {
  text-align: center;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.quote-image {
  width: 100%;
  max-width: 400px;
  max-height: 30vh;
  object-fit: contain;
  border-radius: 12px;
  margin: 0 auto;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.1);
  transition: transform 0.3s ease;
  display: block;

  &:hover {
    transform: scale(1.02);
  }
}

.quote-text {
  position: relative;
  font-size: 20px;
  color: #1e293b;
  line-height: 1.8;
  margin: 0;
  font-weight: 500;
  padding: 0 16px;
  text-align: center;
  white-space: normal; /* 允许文字换行 */
  word-break: break-word; /* 处理长文本换行 */
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f7ff 100%);
  padding: 20px;
  border-radius: 12px;
  border-left: 4px solid #2563eb;
}

/* 优化渐变文字效果，避免影响换行 */
.quote-text::after {
  content: attr(data-text);
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  color: transparent;
  background-image: linear-gradient(to right,
    #2563eb, #3b82f6, #60a5fa, #93c5fd, #2563eb);
  -webkit-background-clip: text;
  background-clip: text;
  opacity: 0;
  transition: opacity 0.3s ease;
  white-space: normal;
  word-break: break-word;
  padding: 20px 16px;
}

.quote-text:hover::after {
  opacity: 1;
  animation: light 8s infinite linear;
}

.quote-english {
  position: relative;
  font-size: 16px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
  font-style: italic;
  padding: 0 16px;
  text-align: center;
  white-space: normal; /* 允许文字换行 */
  word-break: break-word; /* 处理长文本换行 */
  color: #3b82f6;
}

.quote-english::after {
  content: attr(data-text);
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  color: transparent;
  background-image: linear-gradient(to right,
    #3b82f6, #60a5fa, #93c5fd, #3b82f6);
  -webkit-background-clip: text;
  background-clip: text;
  opacity: 0;
  transition: opacity 0.3s ease;
  white-space: normal;
  word-break: break-word;
}

.quote-english:hover::after {
  opacity: 1;
  animation: light 8s infinite linear 0.5s;
}

@keyframes light {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.audio-controls {
  margin: 10px 0 0;
  display: flex;
  justify-content: center;
}

.audio-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border-radius: 30px;
  border: none;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
    background: linear-gradient(135deg, #1d4ed8, #2563eb);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
  }

  .anticon {
    font-size: 16px;
  }
}

.quote-update-time {
  font-size: 14px;
  color: #94a3b8;
  text-align: right;
  margin-top: 8px;
  padding-right: 8px;
}

.quote-popup-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.95);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.1);

  &:hover {
    background: #2563eb;
    transform: rotate(90deg);
    color: white;
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 滚动条美化 */
.quote-popup-content::-webkit-scrollbar {
  width: 6px;
}

.quote-popup-content::-webkit-scrollbar-track {
  background: rgba(37, 99, 235, 0.05);
  border-radius: 3px;
}

.quote-popup-content::-webkit-scrollbar-thumb {
  background: rgba(37, 99, 235, 0.2);
  border-radius: 3px;

  &:hover {
    background: rgba(37, 99, 235, 0.3);
  }
}

@media screen and (max-width: 768px) {
  .quote-popup-content {
    padding: 32px 20px 20px;
    width: calc(100% - 32px);
    margin: 16px;
    max-height: 90vh;
  }

  .quote-image {
    max-width: 100%;
    max-height: 25vh;
  }

  .quote-popup-title {
    font-size: 20px;
    margin: 0 0 20px;
  }

  .quote-popup-close {
    top: 8px;
    right: 8px;
    width: 32px;
    height: 32px;
  }

  .quote-text {
    font-size: 18px;
    padding: 16px;
    line-height: 1.7;
  }

  .quote-english {
    font-size: 14px;
    line-height: 1.5;
  }

  .audio-btn {
    padding: 8px 20px;
    font-size: 14px;
  }
}

@media screen and (max-width: 480px) {
  .quote-text {
    font-size: 16px;
  }

  .quote-image {
    max-height: 20vh;
  }
}
</style>
