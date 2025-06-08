<template>
  <div class="daily-quote" @click="handleQuoteClick">
    <div class="quote-container" :class="{ expanded: isExpanded }">
      <div class="quote-header">
        <h3 class="title">每日一句</h3>
        <button class="barrage-btn" @click.stop="$emit('goToBarrage')">
          <CommentOutlined />
          弹幕墙
        </button>
      </div>
      <div class="quote-content">
        <div class="quote-text-wrapper">
          <p class="quote-text" :class="{ 'truncate': !isExpanded }">
            <span v-for="(char, index) in quote.content"
                  :key="index"
                  :style="{ 'animation-delay': `${index * 0.1}s` }"
                  class="char">
              {{ char }}
            </span>
          </p>
          <p class="quote-author">
            <span class="typing-text">—— {{ quote.author }}</span>
          </p>
        </div>
      </div>
    </div>

    <!-- 展开查看弹出层 -->
    <div class="quote-popup" v-if="isExpanded" @click.self="closeQuotePopup">
      <div class="quote-popup-content">
        <div class="quote-popup-close" @click="closeQuotePopup">×</div>
        <p class="quote-popup-text">{{ quote.content }}</p>
        <p class="quote-popup-author">—— {{ quote.author }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, defineProps, defineEmits } from 'vue'
import { CommentOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  quote: {
    content: string
    author: string
  }
}>()

const emit = defineEmits(['goToBarrage'])

const isExpanded = ref(false)

const handleQuoteClick = () => {
  isExpanded.value = true
}

const closeQuotePopup = () => {
  isExpanded.value = false
}
</script>

<style scoped>
.daily-quote {
  width: 100%;
  position: relative;
}

.quote-container {
  padding: 20px;
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  border-radius: 16px;
  border: 1px solid #ffe4d6;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.quote-container:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(255, 142, 83, 0.12);
}

.quote-container::before {
  content: '"';
  position: absolute;
  top: 12px;
  left: 16px;
  font-size: 64px;
  font-family: serif;
  color: #ff8e53;
  opacity: 0.1;
  line-height: 1;
}

.quote-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: linear-gradient(to bottom, #ff8e53, #ff6b6b);
  border-radius: 2px;
}

.barrage-btn {
  padding: 6px 12px;
  border-radius: 16px;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.2);
}

.barrage-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 142, 83, 0.3);
}

.barrage-btn:active {
  transform: translateY(1px);
}

.quote-content {
  position: relative;
  z-index: 1;
}

.quote-text-wrapper {
  padding: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.95));
  border-radius: 12px;
  box-shadow:
    0 4px 15px rgba(255, 142, 83, 0.1),
    0 1px 5px rgba(255, 142, 83, 0.05);
  transition: all 0.3s ease;
}

.quote-text-wrapper:hover {
  transform: translateY(-2px);
  box-shadow:
    0 8px 25px rgba(255, 142, 83, 0.15),
    0 2px 10px rgba(255, 142, 83, 0.1);
}

.quote-text {
  font-size: 16px;
  line-height: 1.8;
  color: #4a5568;
  margin-bottom: 16px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.quote-text.truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.quote-text .char {
  display: inline-block;
  animation: float 1s ease-in-out infinite;
  animation-play-state: paused;
  transition: color 0.3s ease;
}

.quote-text-wrapper:hover .char {
  animation-play-state: running;
}

.quote-text .char:hover {
  color: #ff8e53;
  transform: translateY(-5px) rotate(10deg) scale(1.1);
}

.quote-author {
  font-size: 14px;
  color: #718096;
  text-align: right;
  font-style: italic;
  margin-top: 12px;
}

.typing-text {
  display: inline-block;
  overflow: hidden;
  border-right: 2px solid #ff8e53;
  white-space: nowrap;
  animation: typing 3.5s steps(40, end), blink-caret 0.75s step-end infinite;
  margin: 0 auto;
}

/* 弹出层样式 */
.quote-popup {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(4px);
  animation: fadeIn 0.2s ease-out;
}

.quote-popup-content {
  background: #fff;
  padding: 24px;
  border-radius: 16px;
  max-width: 90%;
  width: 480px;
  position: relative;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.3s ease-out;
  background: linear-gradient(135deg, #fff6f3 0%, #fff 100%);
  border: 1px solid #ffe4d6;
}

.quote-popup-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 24px;
  height: 24px;
  border-radius: 12px;
  background: rgba(255, 142, 83, 0.1);
  color: #ff8e53;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: all 0.3s ease;
}

.quote-popup-close:hover {
  background: rgba(255, 142, 83, 0.2);
  transform: rotate(90deg);
}

.quote-popup-text {
  font-size: 18px;
  color: #4a5568;
  line-height: 1.8;
  margin-bottom: 16px;
  font-style: italic;
  position: relative;
  padding-left: 28px;
}

.quote-popup-text::before {
  content: '"';
  position: absolute;
  top: 0;
  left: 0;
  font-size: 48px;
  font-family: serif;
  color: #ff8e53;
  opacity: 0.2;
  line-height: 1;
}

.quote-popup-author {
  font-size: 16px;
  color: #718096;
  text-align: right;
  font-weight: 500;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

@keyframes typing {
  from { width: 0 }
  to { width: 100% }
}

@keyframes blink-caret {
  from, to { border-color: transparent }
  50% { border-color: #ff8e53 }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 响应式调整 */
@media screen and (max-width: 1440px) {
  .quote-container {
    padding: 16px;
  }

  .quote-text {
    font-size: 15px;
  }

  .quote-author {
    font-size: 13px;
  }
}

@media screen and (max-width: 1200px) {
  .quote-container {
    padding: 14px;
  }

  .quote-text {
    font-size: 14px;
  }

  .quote-author {
    font-size: 12px;
  }
}
</style>
