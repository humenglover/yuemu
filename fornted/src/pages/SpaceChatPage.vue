<template>
  <div class="space-chat-page">
    <!-- 添加装饰元素 -->
    <div class="decorative-elements">
      <div class="floating-circle circle-1"></div>
      <div class="floating-circle circle-2"></div>
      <div class="floating-circle circle-3"></div>
    </div>

    <div class="chat-container">

      <!-- 右侧聊天区域 -->
      <div class="chat-main">
        <div class="chat-header">
          <div class="header-content">
            <div class="header-title">
              <h2 class="space-name" :title="space.spaceName">{{ space.spaceName }}</h2>
              <span class="chat-type">团队聊天室</span>
            </div>
            <div class="header-actions">
              <div class="action-buttons">
                <a-tooltip title="查看历史消息">
                  <HistoryOutlined class="action-icon" />
                </a-tooltip>
                <a-tooltip title="团队公告">
                  <NotificationOutlined class="action-icon" />
                </a-tooltip>
              </div>

              <div class="online-users-section">
                <template v-if="device !== DEVICE_TYPE_ENUM.PC">
                  <div class="online-users-preview" @click="handleShowMemberList">
                    <a-avatar-group :maxCount="3">
                      <a-tooltip
                        v-for="user in onlineUsers"
                        :key="user.id"
                        :title="user.userName"
                      >
                        <a-avatar :src="user.userAvatar" />
                      </a-tooltip>
                    </a-avatar-group>
                    <span class="online-count-text">{{ onlineCount }}/{{ totalCount }}人在线</span>
                  </div>
                </template>

                <template v-else>
                  <a-popover
                    placement="bottomRight"
                    trigger="hover"
                    :overlayClassName="'online-users-popover'"
                  >
                    <template #content>
                      <div class="online-users-list">
                        <!-- 在线用户列表 -->
                        <div class="online-section">
                          <div class="section-title">在线 ({{ onlineCount }})</div>
                          <div
                            v-for="user in onlineUsers"
                            :key="user.id"
                            class="online-user-item"
                            @click="goToUserDetail(user)"
                          >
                            <a-avatar :src="user.userAvatar" size="small" />
                            <span class="online-user-name">{{ user.userName }}</span>
                            <span class="online-status active"></span>
                          </div>
                        </div>
                        <!-- 离线用户列表 -->
                        <div class="offline-section" v-if="offlineUsers.length > 0">
                          <div class="section-title">离线 ({{ offlineCount }})</div>
                          <div
                            v-for="user in offlineUsers"
                            :key="user.id"
                            class="online-user-item"
                            @click="goToUserDetail(user)"
                          >
                            <a-avatar :src="user.userAvatar" size="small" />
                            <span class="online-user-name">{{ user.userName }}</span>
                            <span class="online-status"></span>
                          </div>
                        </div>
                      </div>
                    </template>
                    <div class="online-users-preview">
                      <a-avatar-group :maxCount="3">
                        <a-tooltip
                          v-for="user in onlineUsers"
                          :key="user.id"
                          :title="user.userName"
                        >
                          <a-avatar :src="user.userAvatar" />
                        </a-tooltip>
                      </a-avatar-group>
                      <span class="online-count-text">{{ onlineCount }}/{{ totalCount }}人在线</span>
                    </div>
                  </a-popover>
                </template>
              </div>
            </div>
          </div>
        </div>
        <PictureChatRoom
          v-bind="chatProps"
          @message="handleChatMessage"
        />
      </div>
    </div>

    <!-- 移动端成员列表弹框 -->
    <a-modal
      v-if="device !== DEVICE_TYPE_ENUM.PC"
      v-model:visible="showMemberList"
      title="团队成员"
      :footer="null"
      :width="320"
      destroyOnClose
      @close="showMemberList = false"
      :centered="true"
      :closable="true"
      class="member-list-modal"
    >
      <div class="mobile-members-list">
        <!-- 在线用户 -->
        <div class="online-section">
          <div class="section-title">在线成员 ({{ onlineCount }})</div>
          <div
            v-for="user in onlineUsers"
            :key="user.id"
            class="member-item"
            @click="goToUserDetail(user)"
          >
            <a-avatar :src="user.userAvatar" />
            <span class="member-name">{{ user.userName }}</span>
            <span class="online-status active"></span>
          </div>
        </div>

        <!-- 离线用户 -->
        <div class="offline-section" v-if="offlineUsers.length > 0">
          <div class="section-title">离线成员 ({{ offlineCount }})</div>
          <div
            v-for="user in offlineUsers"
            :key="user.id"
            class="member-item"
            @click="goToUserDetail(user)"
          >
            <a-avatar :src="user.userAvatar" />
            <span class="member-name">{{ user.userName }}</span>
            <span class="online-status"></span>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { HistoryOutlined, NotificationOutlined, TeamOutlined } from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import dayjs from 'dayjs'
import { getDefaultAvatar } from '@/utils/userUtils'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const space = ref<API.SpaceVO>({})
const members = ref<any[]>([])
const onlineCount = ref(0)
const offlineCount = ref(0)
const totalCount = ref(0)
const onlineUsers = ref<any[]>([])
const offlineUsers = ref<any[]>([])
const showMemberList = ref<boolean>(false)
const device = getDeviceType()

// 添加处理函数
const handleShowMemberList = () => {
  showMemberList.value = true
}

// 添加更新函数
// 修改获取空间信息的函数
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data
      // 初始化成员列表，所有成员默认离线
      if (res.data.data.members) {
        members.value = res.data.data.members.map((member: any) => ({
          ...member,
          userAvatar: member.userAvatar || getDefaultAvatar(member.userName),
          online: false
        }))
      }
    } else {
      message.error('获取空间详情失败：' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取空间信息失败：' + e.message)
  }
}

// 修改 PictureChatRoom 组件的消息处理
const handleChatMessage = (msg: any) => {
  if (msg.type === 'onlineUsers') {
    // 更新在线用户信息
    onlineCount.value = msg.onlineCount
    offlineCount.value = msg.offlineCount
    totalCount.value = msg.totalCount
    onlineUsers.value = msg.onlineUsers.map((user: any) => ({
      ...user,
      userAvatar: user.userAvatar || getDefaultAvatar(user.userName)
    }))
    offlineUsers.value = msg.offlineUsers.map((user: any) => ({
      ...user,
      userAvatar: user.userAvatar || getDefaultAvatar(user.userName)
    }))
  }
}

// 将处理函数传递给聊天组件
const chatProps = {
  type: 'space',
  spaceId: id.toString(),
  onMessage: handleChatMessage
}

// 修改跳转方法，确保头像也被传递
const goToUserDetail = (user: API.UserVO) => {
  router.push({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar || getDefaultAvatar(user.userName),
      userAccount: user.userAccount,
      userProfile: user.userProfile,
      userRole: user.userRole,
      createTime: user.createTime
    }
  })
}

onMounted(() => {
  fetchSpaceDetail()
})
</script>

<style scoped>
.space-chat-page {
  height: 89vh; /* PC 端高度 */
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 0;
  margin: 0;
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

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(5deg);
  }
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

.members-panel {
  width: 250px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  height: calc(100% - 32px); /* 减去container的padding */
}

.panel-header {
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.panel-header h3 {
  margin: 0;
  color: #333;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.online-count {
  font-size: 12px;
  color: #666;
}

.members-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-radius: 16px;
  transition: all 0.3s ease;
  cursor: pointer;  /* 添加手型光标 */

  &:hover {
    transform: translateX(4px);
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  }
}

.member-name {
  margin-left: 12px;
  flex: 1;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.online-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
  box-shadow: 0 0 0 2px rgba(204, 204, 204, 0.2);
}

.online-status.active {
  background: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
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
  height: calc(100% - 32px); /* 减去container的padding */
}

.chat-header {
  padding: 16px 24px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
  flex-wrap: wrap;
}

.space-name {
  overflow: visible; /* 允许完整显示 */
  text-overflow: initial;
  white-space: normal;
  margin: 0;
  line-height: 1.4;
}

.chat-type {
  color: #666;
  font-size: 14px;
  white-space: nowrap;
  &::before {
    content: '-';
    margin: 0 8px;
    color: #ddd;
  }
}

.chat-header h2 {
  margin: 0;
  color: #333;
  font-size: 18px;
  max-width: 100%;
}

.header-actions {
  display: flex;
  justify-content: flex-end; /* 靠右对齐 */
  align-items: center;
  gap: 16px;
  width: auto; /* 不再占满宽度 */
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
  order: 1; /* 调整顺序 */
}

.online-users-section {
  display: flex;
  align-items: center;
  order: 2; /* 放到最右边 */
}

.action-icon {
  font-size: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.action-icon:hover {
  color: #1890ff;
  transform: scale(1.1);
}

/* 移动端在线人数按钮样式 */
.member-count-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #666;
}

.member-count-btn:hover {
  color: #1890ff;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .space-chat-page {
    padding: 0;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;  /* 延伸到底部 */
    z-index: 1;
    margin: 0;
    background: #fff;
    height: 88vh;  /* 确保全屏高度 */
    overflow: hidden;  /* 防止滚动 */
  }

  .chat-container {
    padding: 8px;
    height: 100%;
    position: relative;
    z-index: 1;
    overflow-y: auto;  /* 允许内容滚动 */
    -webkit-overflow-scrolling: touch;  /* 优化 iOS 滚动 */
  }

  .chat-main {
    margin-top: 0;
    height: 100%;  /* 使用容器的全部高度 */
    position: relative;
  }

  .decorative-elements {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: -56px;
    z-index: -1;
    pointer-events: none;
    overflow: hidden;
  }

  .floating-circle {
    opacity: 0.3;
  }

  /* 移动端成员列表样式 */
  .mobile-members-list {
    padding: 16px;
    background: transparent;
    position: relative;
    z-index: 9999;
  }

  .member-item {
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    margin-bottom: 8px;
    padding: 12px 16px;
    border-radius: 16px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateX(4px);
      background: rgba(255, 255, 255, 0.9);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }
  }

  .header-content {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }

  .header-title {
    width: 100%;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
  }

  .chat-type {
    font-size: 12px;
    &::before {
      margin: 0 4px;
    }
  }

  .chat-header h2 {
    font-size: 16px;
  }

  .online-users-section {
    flex: 1;
  }

  .action-buttons {
    gap: 12px;
  }

  @media screen and (max-width: 360px) {
    .header-actions {
      justify-content: flex-start;
      gap: 12px;
    }

    .member-count-btn {
      padding: 4px 8px;
      .action-icon {
        font-size: 16px;
      }
      span {
        font-size: 12px;
      }
    }
  }

  .online-users-preview {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 20px;
    cursor: pointer;
    transition: all 0.3s ease;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);

    &:active {
      background: rgba(255, 255, 255, 0.2);
    }
  }

  :deep(.member-list-modal) {
    z-index: 9999 !important;  /* 提高弹框层级 */

    .ant-modal-content {
      background: rgba(255, 255, 255, 0.98);
      backdrop-filter: blur(10px);
      border-radius: 16px;
      overflow: hidden;
      position: relative;
      z-index: 9999;
    }

    .ant-modal-header {
      padding: 16px 20px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      background: transparent;
      position: relative;
      z-index: 9999;
    }

    .ant-modal-body {
      padding: 0;
      max-height: 70vh;
      overflow-y: auto;
      position: relative;
      z-index: 9999;
    }

    .ant-modal-wrap {
      z-index: 9999;
    }

    .ant-modal-mask {
      z-index: 9998;
    }

    .ant-modal-close {
      color: #666;
      transition: all 0.3s ease;
      z-index: 10000;

      &:hover {
        color: #ff8e53;
        transform: rotate(90deg);
      }
    }
  }
}

.online-users-preview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.online-users-preview:hover {
  background: rgba(255, 255, 255, 0.3);
}

.online-count-text {
  font-size: 13px;
  color: #666;
}

.online-users-list {
  max-height: 300px;
  overflow-y: auto;
  min-width: 200px;
}

.online-user-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  cursor: pointer;  /* 添加手型光标 */

  &:hover {
    background: rgba(0, 0, 0, 0.02);
  }
}

.online-user-name {
  font-size: 14px;
  color: #333;
}

:deep(.online-users-popover) {
  .ant-popover-inner {
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}

:deep(.ant-avatar-group) {
  .ant-avatar {
    border: 2px solid white;
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

/* 添加新的样式 */
.online-users-section {
  display: flex;
  align-items: center;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 添加新样式 */
.offline-divider {
  padding: 8px;
  color: #999;
  font-size: 13px;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.mobile-members-list {
  position: relative;
  z-index: 2;
  padding: 16px;

  .member-item {
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
    margin-bottom: 8px;
    padding: 12px 16px;
    border-radius: 16px;
    transition: all 0.3s ease;

    &:hover {
      transform: translateX(4px);
      background: rgba(255, 255, 255, 0.9);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    }

    .member-name {
      margin-left: 12px;
      flex: 1;
      font-size: 14px;
      color: #333;
      font-weight: 500;
    }

    .online-status {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #ccc;
      box-shadow: 0 0 0 2px rgba(204, 204, 204, 0.2);

      &.active {
        background: #52c41a;
        box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
      }
    }
  }
}

.offline-divider {
  margin: 20px 0 16px;
  padding: 0 16px;
  color: #666;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  border-left: 3px solid #ff8e53;
}

/* 修改抽屉宽度 */
@media screen and (max-width: 768px) {
  :deep(.ant-drawer-content-wrapper) {
    max-width: 85vw !important;
  }
}

/* 美化移动端抽屉样式 */
:deep(.ant-drawer-content) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

:deep(.ant-drawer-header) {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  background: transparent;
}

:deep(.ant-drawer-title) {
  font-size: 16px;
  color: #333;
  font-weight: 600;
}

:deep(.ant-drawer-body) {
  padding: 0;
}

/* 抽屉背景动画 */
.drawer-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255, 142, 83, 0.1) 0%, rgba(255, 107, 107, 0.1) 100%);
}

/* 宠物动画 */
.drawer-pet {
  position: absolute;
  bottom: 20px;
  right: 20px;
  z-index: 1;
  pointer-events: none;
  transform: scale(0.8);
  transition: all 0.3s ease;
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.6);
  }
  to {
    opacity: 1;
    transform: scale(0.8);
  }
}

/* 添加新样式 */
.section-title {
  padding: 12px 16px;
  color: #666;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  border-left: 3px solid #ff8e53;
  margin: 16px 0 12px;
  background: rgba(255, 142, 83, 0.05);
  border-radius: 0 8px 8px 0;
}

.online-section {
  margin-bottom: 20px;
}

.offline-section {
  opacity: 0.8;
}
</style>

