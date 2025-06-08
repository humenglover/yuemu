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
        <img :src="quote.imageUrl" class="quote-image" alt="每日一句图片">
        <p class="quote-text">{{ quote.content }}</p>
        <p class="quote-english">{{ quote.english }}</p>
        <div class="audio-controls">
          <button class="audio-btn" @click="playAudio" :disabled="isPlaying">
            <sound-outlined v-if="!isPlaying" />
            <loading-outlined v-else spin />
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
  z-index: 1112000!important; /* 提高z-index确保遮住导航栏 */
  backdrop-filter: blur(8px);
  animation: fadeIn 0.3s ease-out;
}

.quote-popup-content {
  background: linear-gradient(135deg, #fff 0%, #fff6f3 100%);
  padding: 32px;
  border-radius: 24px;
  max-width: 90%;
  width: 560px;
  max-height: 85vh; /* 设置最大高度为视口高度的85% */
  position: relative;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid rgba(148, 163, 184, 0.1);
  overflow-y: auto; /* 添加垂直滚动 */
  display: flex;
  flex-direction: column;
}

.quote-popup-title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  color: #334155;
  margin: 8px 0 24px; /* 调整上边距，为关闭按钮留出空间 */
  position: sticky;
  top: 0;
  background: inherit;
  padding-top: 8px;
  z-index: 1;
}

.loading {
  text-align: center;
  color: #64748b;
  margin: 24px 0;
}

.quote-content {
  text-align: center;
}

.quote-image {
  width: auto; /* 改为自动宽度 */
  max-width: 360px; /* 减小最大宽度 */
  max-height: 35vh; /* 减小最大高度 */
  object-fit: contain; /* 保持图片比例 */
  border-radius: 12px;
  margin: 0 auto 20px; /* 居中并减小底部间距 */
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease;
  display: block; /* 确保图片块级显示 */

  &:hover {
    transform: scale(1.02);
  }
}

.quote-text {
  font-size: 18px;
  color: #334155;
  line-height: 1.8;
  margin-bottom: 16px;
  font-weight: 500;
  padding: 0 16px;
}

.quote-english {
  font-size: 16px;
  color: #64748b;
  line-height: 1.6;
  margin-bottom: 24px;
  font-style: italic;
  padding: 0 16px;
}

.audio-controls {
  margin: 20px 0;
  display: flex;
  justify-content: center;
}

.audio-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  border-radius: 20px;
  border: none;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.2);

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 16px rgba(255, 142, 83, 0.3);
  }

  &:active {
    transform: translateY(1px);
  }

  &:disabled {
    opacity: 0.7;
    cursor: not-allowed;
    transform: none;
  }

  .anticon {
    font-size: 16px;
  }
}

.quote-update-time {
  font-size: 14px;
  color: #94a3b8;
  text-align: right;
}

.quote-popup-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10; /* 确保按钮在最上层 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); /* 添加阴影提升层次感 */

  &:hover {
    background: rgba(255, 255, 255, 1);
    transform: rotate(90deg);
    color: #ff6b6b; /* 悬停时改变颜色 */
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

@media screen and (max-width: 768px) {
  .quote-popup-content {
    padding: 24px;
    width: calc(100% - 32px);
    margin: 16px;
    max-height: 90vh;
  }

  .quote-image {
    max-width: 280px; /* 移动端进一步减小最大宽度 */
    max-height: 30vh; /* 移动端减小最大高度 */
    margin-bottom: 16px; /* 减小移动端底部间距 */
  }

  .quote-popup-title {
    margin: 4px 0 20px; /* 移动端调整间距 */
  }

  .quote-popup-close {
    top: 8px;
    right: 8px;
    width: 28px; /* 移动端稍微调小按钮 */
    height: 28px;
  }

  .quote-text {
    font-size: 16px;
  }

  .quote-english {
    font-size: 14px;
  }
}
</style>
