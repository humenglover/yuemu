<template>
  <div class="space-chat-page">
    <div class="chat-container">
      <!-- 主聊天区域 -->
      <div class="chat-main">
        <!-- 顶部标题栏 -->
        <div class="header-title">
          <div class="back-btn" @click="router.back()">
            <i class="fas fa-arrow-left back-icon"></i>
          </div>
          <div class="user-info">
            <div class="user-meta">
              <span class="user-name">{{ space.spaceName }}</span>
              <span class="online-count"><i class="fas fa-user-check online-icon"></i> {{ onlineCount }}人在线</span>
            </div>
          </div>
          <div class="header-actions">
            <div class="header-btn" @click="showAnnouncement = true">
              <i class="fas fa-bell notification-icon"></i>
            </div>
            <div class="header-btn" @click="showMemberList = true">
              <i class="fas fa-users team-icon"></i>
              <span class="count-badge">{{ onlineCount }}</span>
            </div>
          </div>
        </div>

        <!-- 聊天内容区域 -->
        <PictureChatRoom
          ref="chatRoomRef"
          v-bind="chatProps"
          @message="handleChatMessage"
        />
      </div>
    </div>

    <!-- 群公告弹框 -->
    <a-modal
      v-model:visible="showAnnouncement"
      title="群公告"
      :footer="null"
      :width="isMobile ? '90%' : '400px'"
      :centered="true"
      class="custom-modal announcement-modal"
    >
      <div class="announcement-content">
        <p>欢迎来到团队聊天室！为了营造良好的交流环境，请大家：</p>
        <ul>
          <li><i class="fas fa-star rule-icon"></i> 文明用语，互相尊重</li>
          <li><i class="fas fa-handshake rule-icon"></i> 友善交流，互帮互助</li>
          <li><i class="fas fa-palette rule-icon"></i> 分享有趣的想法和创意</li>
          <li><i class="fas fa-ban rule-icon"></i> 禁止发布违规、广告等不良内容</li>
        </ul>
      </div>
    </a-modal>

    <!-- 成员列表抽屉 -->
    <a-drawer
      v-model:visible="showMemberList"
      :title="'聊天室成员 (' + totalCount + ')'"
      :width="isMobile ? '100%' : '380px'"
      :bodyStyle="{ padding: '0px' }"
      placement="right"
      class="member-drawer"
    >
      <div class="member-list">
        <!-- 在线用户 -->
        <div class="member-section">
          <div class="section-header">
            <i class="fas fa-circle online-status-icon"></i> 在线 - {{ onlineCount }}人
          </div>
          <div class="member-items">
            <div
              v-for="user in onlineUsers"
              :key="user.id"
              class="member-item"
              @click="goToUserDetail(user)"
            >
              <a-avatar :src="user.userAvatar" :size="40" />
              <div class="member-info">
                <span class="member-name">{{ user.userName }}</span>
                <span class="online-tag"><i class="fas fa-circle online-indicator"></i> 在线</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 离线用户 -->
        <div class="member-section" v-if="offlineUsers.length > 0">
          <div class="section-header">
            <i class="fas fa-circle offline-status-icon"></i> 离线 - {{ offlineCount }}人
          </div>
          <div class="member-items">
            <div
              v-for="user in offlineUsers"
              :key="user.id"
              class="member-item"
              @click="goToUserDetail(user)"
            >
              <a-avatar :src="user.userAvatar" :size="40" />
              <div class="member-info">
                <span class="member-name">{{ user.userName }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import { NotificationOutlined, TeamOutlined, LeftOutlined } from '@ant-design/icons-vue'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDefaultAvatar } from '@/utils/userUtils'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const space = ref<API.SpaceVO>({})
const onlineCount = ref(0)
const offlineCount = ref(0)
const totalCount = ref(0)
const onlineUsers = ref<any[]>([])
const offlineUsers = ref<any[]>([])
const showMemberList = ref<boolean>(false)
const showAnnouncement = ref<boolean>(false)
const device = getDeviceType()
const isMobile = device === DEVICE_TYPE_ENUM.MOBILE

// 修改获取空间信息的函数
const fetchSpaceDetail = async () => {
  try {
    const res = await getSpaceVoByIdUsingGet({ id })
    if (res.data.code === 0 && res.data.data) {
      space.value = res.data.data;
      totalCount.value = res.data.data.members?.length || 0;
      offlineUsers.value = res.data.data.members?.map((member: any) => ({
        ...member,
        userAvatar: member.userAvatar || getDefaultAvatar(member.userName),
      })) || [];
    } else {
      message.error('获取空间详情失败：' + (res.data.message || '未知错误'))
    }
  } catch (e: any) {
    message.error('获取空间信息失败：' + e.message)
  }
}

// 修改 PictureChatRoom 组件的消息处理
const handleChatMessage = (msg: any) => {
  if (msg.type === 'onlineUsers') {
    // 更新在线用户信息
    onlineCount.value = msg.onlineUsers?.length || 0;
    offlineCount.value = totalCount.value - onlineCount.value;
    onlineUsers.value = msg.onlineUsers?.map((user: any) => ({
      ...user,
      userAvatar: user.userAvatar || getDefaultAvatar(user.userName)
    })) || [];

    // 从所有成员中筛选出离线用户
    const onlineUserIds = new Set(onlineUsers.value.map(u => u.id));
    offlineUsers.value = (space.value.members || []).filter((member: any) =>
      !onlineUserIds.has(member.id)
    ).map((member: any) => ({
      ...member,
      userAvatar: member.userAvatar || getDefaultAvatar(member.userName),
    }));
  }
}

// 将处理函数传递给聊天组件
const chatProps = {
  type: 'space',
  spaceId: id.toString(),
  onMessage: handleChatMessage
}

// 修改跳转方法
const goToUserDetail = (user: any) => {
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
/* 根容器占满整个视口高度 */
.space-chat-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #fff; /* 使用白色背景以匹配设计 */
  z-index: 100; /* 确保在顶层 */
}

/* 聊天容器，负责居中并提供内边距 */
.chat-container {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
  padding: 16px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
}

/* 主聊天区域，这是关键的flex容器 */
.chat-main {
  flex: 1;
  background: var(--header-background);
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
}

/* 顶部标题栏 */
.header-title {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0; /* 防止标题栏被压缩 */
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
  margin-right: 12px;
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.user-info {
  flex: 1; /* 让用户信息区域占据中间所有空间 */
}

.user-meta {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.online-count {
  font-size: 12px;
  color: #999;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
  position: relative;
}

.header-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.count-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #ff6b6b;
  color: #fff;
  font-size: 12px;
  line-height: 16px;
  text-align: center;
  font-weight: 600;
}

/* 自定义模态框样式 */
:deep(.custom-modal) {
  .ant-modal-content {
    border-radius: 12px;
    overflow: hidden;
  }
  .ant-modal-header {
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
  }
  .ant-modal-body {
    padding: 20px;
    max-height: 70vh;
    overflow-y: auto;
  }
}

/* 成员列表抽屉样式 */
:deep(.member-drawer) {
  .ant-drawer-header { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; }
  .ant-drawer-title { font-size: 16px; font-weight: 600; color: #333; }
}

/* 成员列表样式 */
.member-list {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
  .member-section { margin-bottom: 24px; &:last-child { margin-bottom: 0; } }
  .section-header { font-size: 14px; color: #999; margin-bottom: 12px; padding-left: 4px; }
  .member-items { display: flex; flex-direction: column; gap: 4px; }
  .member-item {
    display: flex;
    align-items: center;
    padding: 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s;
    &:hover { background: #f5f5f5; }
  }
  .member-info {
    margin-left: 12px;
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .member-name {
    font-size: 14px;
    color: #333;
    font-weight: 500;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .online-tag {
    flex-shrink: 0;
    padding: 2px 8px;
    border-radius: 12px;
    background: #e8f5e9;
    color: #4caf50;
    font-size: 12px;
  }
}

/* 群公告样式 */
.announcement-content {
  color: #666;
  line-height: 1.6;
  p { margin-bottom: 16px; }
  ul { padding-left: 20px; margin: 0; li { margin-bottom: 12px; &:last-child { margin-bottom: 0; } } }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .chat-container { padding: 8px; }
  .header-title { padding: 10px 12px; }
  .user-name { font-size: 15px; }
  .online-count { font-size: 11px; }
  .header-actions { gap: 8px; }
  .header-btn { width: 32px; height: 32px; }
  .count-badge { min-width: 14px; height: 14px; font-size: 10px; line-height: 14px; }
}
</style>
