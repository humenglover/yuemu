<template>
  <div class="message-center" :class="{ 'pc-message-center': !isMobile }">
    <!-- 页面标题和统计 -->
    <div class="message-header">
      <div class="header-left">
        <h2>消息中心</h2>
        <div class="total-unread" :class="{ 'has-unread': messageData.totalUnread > 0 }">
          共 <span class="highlight">{{ messageData.totalUnread || 0 }}</span> 条未读消息
        </div>
      </div>
      <div class="header-right">
        <a-button
          class="history-btn"
          @click="goToHistory"
        >
          <template #icon><HistoryOutlined /></template>
          互动历史
        </a-button>
        <a-button
          type="primary"
          class="mark-read-btn"
          :disabled="messageData.totalUnread <= 0"
          :loading="markingRead"
          @click="handleMarkAllRead"
        >
          全部已读
        </a-button>
      </div>
    </div>

    <!-- 消息统计卡片 -->
    <div class="stats-cards">
      <div
        class="stat-card"
        :class="{ active: activeTab === 'comments' }"
        @click="handleTabChange('comments')"
      >
        <div class="stat-icon comments">
          <CommentOutlined />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ messageData.unreadComments || 0 }}</div>
          <div class="stat-label">未读评论</div>
        </div>
      </div>

      <div
        class="stat-card"
        :class="{ active: activeTab === 'likes' }"
        @click="handleTabChange('likes')"
      >
        <div class="stat-icon likes">
          <LikeOutlined />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ messageData.unreadLikes || 0 }}</div>
          <div class="stat-label">未读点赞</div>
        </div>
      </div>

      <div
        class="stat-card"
        :class="{ active: activeTab === 'shares' }"
        @click="handleTabChange('shares')"
      >
        <div class="stat-icon shares">
          <ShareAltOutlined />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ messageData.unreadShares || 0 }}</div>
          <div class="stat-label">未读分享</div>
        </div>
      </div>
    </div>

    <!-- 消息列表区域 -->
    <div class="message-list">
      <div class="list-header">
        <span class="list-title">
          {{
            activeTab === 'comments' ? '评论消息' :
              activeTab === 'likes' ? '点赞消息' : '分享消息'
          }}
        </span>
        <span class="list-count">
          {{
            activeTab === 'comments' ? messageData.unreadComments :
              activeTab === 'likes' ? messageData.unreadLikes : messageData.unreadShares
          }} 条未读
        </span>
      </div>
      <div class="list-content">
        <UnreadCommentList
          v-if="activeTab === 'comments'"
          :comments="unreadComments"
          :is-received="true"
        />
        <UnreadLikeList
          v-else-if="activeTab === 'likes'"
          :likes="unreadLikes"
          :is-received="true"
        />
        <UnreadShareList
          v-else-if="activeTab === 'shares'"
          :shares="unreadShares"
          :is-received="true"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getUnreadCountUsingGet, markAllAsReadUsingPost } from '@/api/messageCenterController'
import { getUnreadCommentsUsingGet } from '@/api/commentsController'
import { getUnreadLikesUsingGet } from '@/api/likeRecordController'
import { getUnreadSharesUsingGet } from '@/api/shareRecordController'
import { CommentOutlined, LikeOutlined, ShareAltOutlined, HistoryOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import UnreadCommentList from '@/components/UnreadCommentList.vue'
import UnreadLikeList from '@/components/UnreadLikeList.vue'
import UnreadShareList from '@/components/UnreadShareList.vue'
import { Grid } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const { useBreakpoint } = Grid
const screens = useBreakpoint()
const isMobile = computed(() => !screens.md)

const activeTab = ref('comments')
const messageData = ref({
  totalUnread: 0,
  unreadComments: 0,
  unreadLikes: 0,
  unreadShares: 0
})
const unreadComments = ref([])
const unreadLikes = ref([])
const unreadShares = ref([])
const markingRead = ref(false)

const router = useRouter()

// 获取消息数据
const fetchMessageData = async () => {
  try {
    const res = await getUnreadCountUsingGet()
    if (res.data?.code === 0) {
      messageData.value = {
        totalUnread: res.data.data.totalUnread || 0,
        unreadComments: res.data.data.unreadComments || 0,
        unreadLikes: res.data.data.unreadLikes || 0,
        unreadShares: res.data.data.unreadShares || 0
      }
    }
  } catch (error) {
    console.error('获取消息数据失败:', error)
  }
}

// 获取未读评论
const fetchUnreadComments = async () => {
  try {
    const res = await getUnreadCommentsUsingGet()
    if (res.data?.code === 0) {
      unreadComments.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取未读评论失败:', error)
  }
}

// 获取未读点赞
const fetchUnreadLikes = async () => {
  try {
    const res = await getUnreadLikesUsingGet()
    if (res.data?.code === 0) {
      unreadLikes.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取未读点赞失败:', error)
  }
}

// 获取未读分享
const fetchUnreadShares = async () => {
  try {
    const res = await getUnreadSharesUsingGet()
    if (res.data?.code === 0) {
      unreadShares.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取未读分享失败:', error)
  }
}

// 处理标签切换
const handleTabChange = async (tab: string) => {
  activeTab.value = tab
  if (tab === 'comments') {
    await fetchUnreadComments()
  } else if (tab === 'likes') {
    await fetchUnreadLikes()
  } else if (tab === 'shares') {
    await fetchUnreadShares()
  }
  // 在切换标签后更新消息计数
  await fetchMessageData()
}

// 处理全部已读
const handleMarkAllRead = async () => {
  if (messageData.value.totalUnread <= 0) return

  try {
    markingRead.value = true
    const res = await markAllAsReadUsingPost()

    if (res.data.code === 0) {
      message.success('已将全部消息标记为已读')
      // 更新消息计数和当前标签页的数据
      await Promise.all([
        fetchMessageData(),
        handleTabChange(activeTab.value)
      ])
    } else {
      message.error('操作失败，请重试')
    }
  } catch (error) {
    console.error('标记全部已读失败:', error)
    message.error('操作失败，请重试')
  } finally {
    markingRead.value = false
  }
}

// 跳转到历史记录页面
const goToHistory = () => {
  router.push('/message/history')
}

onMounted(async () => {
  // 先获取消息计数
  await fetchMessageData()
  // 再获取默认标签的数据
  await fetchUnreadComments()
})
</script>

<style scoped>
/* PC端特定样式 */
.pc-message-center {
  max-width: 1000px !important;
  margin: 0 auto !important;
  padding: 32px !important;
  background: transparent !important;
}

.pc-message-center .message-header {
  background: white;
  padding: 24px 32px;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.pc-message-center .stats-cards {
  grid-template-columns: repeat(3, 1fr) !important;
  gap: 24px;
  padding: 0;
}

.pc-message-center .stat-card {
  padding: 32px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  position: relative;
  overflow: hidden;
}

.pc-message-center .stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, transparent, currentColor, transparent);
  opacity: 0.1;
  transition: opacity 0.3s ease;
}

.pc-message-center .stat-card:hover::before {
  opacity: 0.2;
}

.pc-message-center .stat-card.active::before {
  opacity: 0.3;
}

.pc-message-center .stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 32px;
  font-size: 28px;
}

.pc-message-center .stat-value {
  font-size: 32px;
  margin-bottom: 8px;
}

.pc-message-center .stat-label {
  font-size: 15px;
}

.pc-message-center .mark-read-btn {
  height: 36px;
  padding: 0 24px;
  font-size: 14px;
}

/* 原有的样式保持不变 */
.message-center {
  margin: -24px;
  padding: 24px;
  min-height: 94vh;
  background: #f8fafc;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 0 16px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.header-left h2 {
  font-size: 24px;
  color: #1a1a1a;
  font-weight: 600;
  margin: 0;
}

.total-unread {
  font-size: 14px;
  color: #666;
}

.highlight {
  color: #ff6b6b;
  font-weight: 500;
  font-size: 16px;
}

.mark-read-btn {
  height: 32px;
  border-radius: 16px;
  padding: 0 16px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  border: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover:not(:disabled) {
    background: linear-gradient(135deg, #ff7a33 0%, #ff5151 100%);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
  }

  &:disabled {
    background: #e2e8f0;
    cursor: not-allowed;
    opacity: 0.6;
    transform: none;
    box-shadow: none;
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  padding: 0 16px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  cursor: pointer;
  border: 2px solid transparent;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &.active {
    border-color: #ff8e53;
    background: #fff8f6;
  }
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.3s ease;

  &.comments {
    background: rgba(96, 165, 250, 0.1);
    color: #60a5fa;
  }

  &.likes {
    background: rgba(255, 107, 107, 0.1);
    color: #ff6b6b;
  }

  &.shares {
    background: rgba(96, 195, 213, 0.1);
    color: #60c3d5;
  }
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .message-center {
    margin-left: -20px !important;
    margin-right: -20px !important;
    padding: 0;
    background: #f8fafc;
  }

  .message-header {
    flex-direction: row;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px;
    background: white;
    border-bottom: 1px solid #f0f0f0;
    justify-content: space-between;
  }

  .header-left {
    flex-direction: column;
    gap: 4px;
    align-items: flex-start;
  }

  .header-left h2 {
    font-size: 18px;
    margin: 0;
    line-height: 1.2;
  }

  .total-unread {
    font-size: 13px;
    color: #666;
    padding: 2px 0;
    transition: all 0.3s ease;
  }

  .total-unread.has-unread {
    color: #ff4d4f;
  }

  .highlight {
    font-size: 15px;
    font-weight: 500;
    margin: 0 2px;
  }

  .header-right {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .history-btn {
    height: 28px;
    font-size: 12px;
    padding: 0 10px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    gap: 4px;
    white-space: nowrap;

    :deep(.anticon) {
      font-size: 14px;
    }
  }

  .mark-read-btn {
    height: 28px;
    font-size: 12px;
    padding: 0 10px;
    border-radius: 14px;
    white-space: nowrap;
  }

  .stats-cards {
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    padding: 12px;
    margin-bottom: 12px;
  }

  .stat-card {
    padding: 12px;
    flex-direction: column;
    text-align: center;
    gap: 8px;
  }

  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 18px;
  }

  .stat-value {
    font-size: 20px;
    margin-bottom: 2px;
  }

  .stat-label {
    font-size: 12px;
  }

  .message-list {
    border-radius: 0;
    box-shadow: none;
    margin-top: 0;
    min-height: 100vh ;
  }

  .list-header {
    padding: 12px 16px;
  }

  .list-title {
    font-size: 15px;
  }

  .list-count {
    font-size: 13px;
  }

  .list-content {
    padding: 12px;
  }
}

/* 针对特小屏幕的额外优化 */
@media screen and (max-width: 360px) {
  .header-left h2 {
    font-size: 16px;
  }

  .total-unread {
    font-size: 12px;
  }

  .highlight {
    font-size: 14px;
  }

  .history-btn,
  .mark-read-btn {
    height: 26px;
    font-size: 11px;
    padding: 0 8px;
  }

  .history-btn :deep(.anticon) {
    font-size: 13px;
  }
}

/* 删除不需要的样式 */
.message-content,
.comments-section {
  display: none;
}

/* 原有样式保持不变，添加消息列表样式 */
.message-list {
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-top: 24px;
  overflow: hidden;
  min-height: calc(100vh - 350px);
}

.list-header {
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
}

.list-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
}

.list-count {
  font-size: 14px;
  color: #666;
}

.list-content {
  padding: 16px;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.history-btn {
  height: 32px;
  border-radius: 16px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;

  &:hover {
    color: #ff8e53;
    border-color: #ff8e53;
  }
}
</style>
