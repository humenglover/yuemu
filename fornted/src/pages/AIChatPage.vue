<template>
  <div class="ai-chat-page">
    <div class="chat-header">
      <div class="header-content">
        <div class="user-info">
          <a-avatar :src="'https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com/public/1866450683272450049/2025-03-14_ZKdnsKKV4Z3Rxvcx.webp'" :size="40" />
          <div class="info-text">
            <div class="name">DeepSeek v3</div>
            <div class="status">AI助手</div>
          </div>
        </div>
      </div>
    </div>

    <AIChatRoom style="margin-top: 10px" ref="chatRoom" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import AIChatRoom from '@/components/AIChatRoom.vue'

const chatRoom = ref()
</script>

<style scoped>
.ai-chat-page {
  margin-left: -20px !important;
  margin-right: -20px !important;
  margin-top: -20px!important;
  height: calc(100vh - 88px); /* 减去底部信息栏的高度 */
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f6f8fc 0%, #f0f7ff 100%);
  position: relative;
}

.chat-header {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  padding: 1px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  position: sticky;
  top: 0;

}

.header-content {
  max-width: 88%;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-text {
  display: flex;
  flex-direction: column;
}

.name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.status {
  font-size: 12px;
  color: #722ed1;
  background: rgba(114, 46, 209, 0.1);
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .chat-header {
    padding: 8px 16px;
  }

  .name {
    font-size: 15px;
  }
}

/* 添加响应式样式 */
@media screen and (min-width: 768px) {
  .ai-chat-page {
    height: calc(100vh - 80px); /* PC端底部留更多空间 */
  }
}
</style>
