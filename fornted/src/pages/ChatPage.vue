<template>
  <div class="chat-page">
    <!-- 添加装饰元素 -->
    <div class="decorative-elements">
      <div class="floating-circle circle-1"></div>
      <div class="floating-circle circle-2"></div>
      <div class="floating-circle circle-3"></div>
    </div>

    <div class="chat-container">
      <!-- 主聊天区域 -->
      <div class="chat-main">
        <div class="chat-header">
          <div class="header-content">
            <div class="header-title">
              <a-button class="back-btn" type="link" @click="goBack">
                <template #icon><LeftOutlined /></template>
              </a-button>
              <div class="user-info">
                <van-image
                  :src="targetUser.userAvatar || getDefaultAvatar(targetUser.userName)"
                  round
                  width="40"
                  height="40"
                  class="avatar"
                  @click="handleAvatarClick"
                />
                <div class="user-meta">
                  <span class="user-name">{{ targetUser.userName }}</span>
                  <span class="status-text" :class="{ online: isOnline }">
                    {{ isOnline ? '在线' : '离线' }}
                  </span>
                </div>
              </div>
            </div>
            <div class="header-actions">
              <div class="action-buttons">
                <a-tooltip title="查看历史消息">
                  <a-button type="link" class="action-btn">
                    <template #icon><HistoryOutlined /></template>
                  </a-button>
                </a-tooltip>
              </div>
            </div>
          </div>
        </div>

        <PictureChatRoom
          ref="chatRoomRef"
          type="private"
          :privateChatId="privateChatId"
          @message="handleChatMessage"
          @mounted="handleChatMounted"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted as vueOnMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CloseOutlined,
  LeftOutlined,
  HistoryOutlined
} from '@ant-design/icons-vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import { clearUnreadCountUsingPost } from '@/api/privateChatController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const mounted = ref(false)
const isOnline = ref(false)
const chatRoomRef = ref()
const chatContainer = ref()

// 目标用户信息
const targetUser = ref({
  id: route.params.userId.toString(),
  userName: route.query.userName as string,
  userAvatar: route.query.userAvatar as string,
  userAccount: route.query.userAccount as string,
  createTime: route.query.createTime as string
})

// 私聊ID
const privateChatId = ref(route.query.privateChatId as string)

// 清理未读消息
const clearUnreadMessages = async () => {
  try {
    await clearUnreadCountUsingPost({
      targetUserId: targetUser.value.id,
      isSender: route.query.isSender
    })
  } catch (error) {
    console.error('清理未读消息失败:', error)
  }
}

// 处理聊天组件挂载完成
const handleChatMounted = async () => {
  // 等待DOM更新
  await nextTick()
  // 滚动到底部
  const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
  if (messageContainer) {
    messageContainer.scrollTop = messageContainer.scrollHeight
  }
}

// 处理聊天消息
const handleChatMessage = (msg: any) => {
  if (msg.type === 'error') {
    console.error('聊天错误:', msg.message)
    return
  }
  // 处理在线用户消息
  if (msg.type === 'onlineUsers') {
    const targetUserOnline = msg.onlineUsers?.some(
      (user: any) => user.id === targetUser.value?.id
    )
    isOnline.value = targetUserOnline
  }
  // 处理历史消息和更多历史消息
  else if (msg.type === 'history' || msg.type === 'moreHistory') {
    console.log('收到历史消息:', msg)
    // 历史消息加载完成后滚动到底部
    if (msg.type === 'history') {
      nextTick(() => {
        const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
        if (messageContainer) {
          messageContainer.scrollTop = messageContainer.scrollHeight
        }
      })
    }
  }
  // 处理普通消息
  else if (msg.type === 'message') {
    console.log('收到聊天消息:', msg.message)
    // 收到新消息后滚动到底部
    nextTick(() => {
      const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
      if (messageContainer) {
        messageContainer.scrollTop = messageContainer.scrollHeight
      }
    })
  }
}

// 处理头像点击
const handleAvatarClick = () => {
  router.push({
    path: `/user/${targetUser.value.id}`,
    query: {
      userName: targetUser.value.userName,
      userAvatar: targetUser.value.userAvatar,
      userAccount: targetUser.value.userAccount,
      createTime: targetUser.value.createTime
    }
  })
}

// 返回上一页
const goBack = () => {
  mounted.value = false
  setTimeout(() => {
    router.back()
  }, 300)
}

// 组件挂载后显示动画
vueOnMounted(() => {
  setTimeout(() => {
    mounted.value = true
  }, 100)
  // 立即清理未读消息
  clearUnreadMessages()
  // 组件挂载后滚动到底部
  nextTick(() => {
    const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
    if (messageContainer) {
      messageContainer.scrollTop = messageContainer.scrollHeight
    }
  })
})

// 组件卸载前断开连接
onBeforeUnmount(() => {
  if (chatRoomRef.value) {
    chatRoomRef.value.disconnect()
  }
})
</script>

<style scoped>
.chat-page {
  height: 90vh; /* PC 端高度 */
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  margin: -20px;

  background: linear-gradient(45deg, rgba(255, 142, 83, 0.1) 0%, rgba(255, 107, 107, 0.1) 100%);
}

.decorative-elements {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.floating-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
  pointer-events: none;
}

.circle-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  top: -150px;
  right: -150px;
  animation: float 8s ease-in-out infinite;
}

.circle-2 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e53 100%);
  bottom: -100px;
  left: -100px;
  animation: float 6s ease-in-out infinite reverse;
}

.circle-3 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  top: 50%;
  left: 50%;
  animation: float 10s ease-in-out infinite;
}

.chat-container {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  padding: 16px;
  gap: 20px;
  width: 100%;
  max-width: 1200px; /* 添加最大宽度限制 */
  margin: 0 auto;
  overflow: hidden;
}

.chat-main {
  flex: 1;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  height: calc(100% - 32px);
}

.chat-header {
  flex-shrink: 0;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 8px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.status-text {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 6px;

  &::before {
    content: '';
    display: inline-block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #999;
    transition: all 0.3s ease;
  }

  &.online {
    color: #52c41a;

    &::before {
      background: #52c41a;
      box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
    }
  }
}

.avatar {
  cursor: pointer;
  transition: transform 0.3s ease;

  &:hover {
    transform: scale(1.05);
  }
}

.back-btn {
  padding: 4px 8px;
  color: #666;

  &:hover {
    color: #ff8e53;
  }
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 16px;
  color: #666;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;

  &:hover {
    color: #ff8e53;
    background: rgba(255, 142, 83, 0.1);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .chat-page {
    padding: 0;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 100000000000;
    margin: 0;
    background: #fff;
    height: 100vh;
    overflow: hidden;
  }

  .chat-container {
    padding: 8px;
    height: 100%;
    max-width: none; /* 移动端取消最大宽度限制 */
  }

  .chat-main {
    margin-top: 0;
    height: 100%;
  }

  .chat-header {
    padding: 12px;
  }

  .header-content {
    padding: 0;
  }

  .user-name {
    font-size: 15px;
  }

  .status-text {
    font-size: 12px;
  }
}
</style>
