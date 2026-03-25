<template>
  <div class="chat-page">
    <div class="chat-container">
      <!-- 主聊天区域 -->
      <div class="chat-main">
        <!-- 顶部按钮 -->
        <div class="header-title">
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
              <span v-if="!isGroupChat" class="status-text" :class="{ online: isOnline }">
                {{ isOnline ? '在线' : '离线' }}
              </span>
              <span v-else class="group-tag">全员群</span>
            </div>
            <div v-if="isGroupChat && spaceId === -2" class="top-buttons">
              <div class="mobile-buttons">
                <button class="announcement-btn" @click="showAnnouncement = true" type="button">
                  <i class="fas fa-bullhorn"></i>
                  <span>公告</span>
                </button>
                <button class="online-users-btn" @click="toggleUserModal" type="button">
                  <i class="fas fa-users"></i>
                  <span>({{ onlineUsers.length }})</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- 顶部加载指示器 -->
        <div v-if="loadingHistory" class="top-loading-indicator">
          <i class="fas fa-spinner fa-spin loading-icon"></i>
          <span>加载中...</span>
        </div>

        <PictureChatRoom
          ref="chatRoomRef"
          :type="isGroupChat ? 'group' : 'private'"
          :privateChatId="privateChatId"
          :spaceId="spaceId"
          @message="handleChatMessage"
          @mounted="handleChatMounted"
          @update:onlineUsers="updateOnlineUsers"
        />
      </div>
    </div>
  </div>

  <!-- 在线用户弹框 -->
  <teleport to="body">
    <div v-if="showUserModal" class="user-modal-overlay" @click="toggleUserModal">
      <div class="user-modal-content" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">
            <i class="fas fa-users"></i>
            在线用户 ({{ onlineUsers.length }})
          </h3>
          <button class="modal-close" @click="toggleUserModal" type="button">
            ✕
          </button>
        </div>
        <div class="modal-body">
          <div v-if="!onlineUsers.length" class="empty-users">
            <div class="empty-icon">👥</div>
            <p>暂无在线用户</p>
          </div>
          <div class="users-scroll">
            <div v-for="user in onlineUsers" :key="user.id" class="user-item" @click="handleUserClick(user)">
              <div class="user-avatar" :style="{ backgroundImage: `url(${user.userAvatar || getDefaultAvatar(user.userName)})` }">
                <div class="online-dot"></div>
              </div>
              <div class="user-meta">
                <div class="user-name">{{ user.userName }}</div>
                <div class="user-role">{{ user.userRole }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </teleport>

  <!-- 公告弹框 -->
  <a-modal
    v-model:visible="showAnnouncement"
    title="群公告"
    :footer="null"
    :width="isMobile ? '90%' : '400px'"
    class="announcement-modal"
  >
    <div class="announcement-content">
      <p>欢迎来到聊天室！为了营造良好的交流环境，请大家：</p>
      <ul>
        <li>🌟 文明用语，互相尊重</li>
        <li>🤝 友善交流，互帮互助</li>
        <li>🎨 分享有趣的图片和想法</li>
        <li>❌ 禁止发布违规、广告等不良内容</li>
      </ul>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, onMounted as vueOnMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CloseOutlined,
  LeftOutlined,
  HistoryOutlined,
  NotificationOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { getDefaultAvatar } from '@/utils/userUtils'

import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const mounted = ref(false)
const isOnline = ref(false)
const chatRoomRef = ref()
const chatContainer = ref()

// 在线用户和公告相关状态
const onlineUsers = ref<any[]>([])
const showUserModal = ref(false)
const showAnnouncement = ref(false)
const loadingHistory = ref(false)

// 目标用户信息
const targetUser = ref({
  id: route.params.userId.toString(),
  userName: route.query.userName as string,
  userAvatar: route.query.userAvatar as string,
  userAccount: route.query.userAccount as string,
  createTime: route.query.createTime as string
})

// 私聊ID和空间ID
const privateChatId = ref(route.query.privateChatId as string)
const spaceId = ref(route.query.spaceId ? Number(route.query.spaceId) : undefined)

// 是否是群聊
const isGroupChat = computed(() => route.query.type === 'group')

// 是否是移动端
const isMobile = computed(() => {
  return window.innerWidth <= 768
})

// 更新在线用户
const updateOnlineUsers = (users: any[]) => {
  onlineUsers.value = users
}

// 切换在线用户弹框
const toggleUserModal = () => {
  showUserModal.value = !showUserModal.value
}

// 处理用户点击
const handleUserClick = (user: any) => {
  // 这里可以添加点击用户的逻辑
  console.log('点击用户:', user)
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

  // 处理加载状态
  if (msg.type === 'history' || msg.type === 'moreHistory') {
    loadingHistory.value = false
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
  if (isGroupChat.value) return // 群聊不跳转到用户页面

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
  height: 92vh;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  color: var(--text-primary);

}

.chat-container {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  overflow: hidden;
  background: var(--header-background);
  color: var(--text-primary);
}

.chat-main {
  flex: 1;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  position: relative;
}

.top-buttons {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 8px 12px 0;
  position: absolute;
  right: 12px;
  z-index: 1000;
}

.mobile-buttons {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.announcement-btn,
.online-users-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: none;
  border-radius: 16px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f5f5f5;
  color: #333;
}

.announcement-btn {
  background: #ff8e53;
  color: #fff;
}

.online-users-btn {
  background: #1890ff;
  color: #fff;
}

.top-loading-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  gap: 8px;
  color: #999;
  font-size: 14px;
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
  padding: 6px 10px;
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
  color: #796c58;
}

.status-text {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-text::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  transition: all 0.3s ease;
}

.status-text.online {
  color: #52c41a;
}

.status-text.online::before {
  background: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
}

.avatar {
  cursor: pointer;
  transition: transform 0.3s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

.group-tag {
  color: #ff8e53;
  background: #fff2e8;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 13px;
}

/* 在线用户弹框样式 */
.user-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

.user-modal-content {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  animation: scaleIn 0.2s ease;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e0e0e0;
  flex-shrink: 0;
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 500;
  margin: 0;
}

.modal-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.modal-close:active {
  transform: scale(0.9);
  background: #e0e0e0;
}

.modal-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.empty-users {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
  gap: 8px;
}

.empty-users .empty-icon {
  font-size: 48px;
}

.users-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.user-item:active {
  background: #f0f0f0;
}

.user-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.user-avatar .online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #52c41a;
  border: 1px solid #fff;
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-meta .user-name {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.user-meta .user-role {
  font-size: 11px;
  color: #999;
}

/* 公告弹框 */
.announcement-modal .announcement-content {
  padding: 12px 0;
}

.announcement-modal .announcement-content p {
  margin-bottom: 12px;
  color: #666;
}

.announcement-modal .announcement-content ul {
  margin: 0;
  padding-left: 20px;
}

.announcement-modal .announcement-content ul li {
  margin-bottom: 8px;
  color: #666;
  line-height: 1.5;
}

/* 动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
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
    z-index: 100;
    margin: 0;
    background: var(--header-background);
    color: var(--text-primary);
    height: 100vh;
    overflow: hidden;
  }

  .chat-container {
    padding: 4px;
    height: 100%;
    max-width: none;
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

  .top-buttons {
    padding: 4px 8px 0;
  }
}
</style>
