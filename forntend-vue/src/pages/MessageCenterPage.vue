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
          type="primary"
          class="mark-read-btn"
          :disabled="messageData.totalUnread <= 0"
          :loading="markingRead"
          @click="handleMarkAllRead"
        >
          <i v-if="markingRead" class="fas fa-spinner fa-spin loading-icon"></i>
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
          <i class="fas fa-comment"></i>
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
          <i class="fas fa-heart"></i>
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
          <i class="fas fa-share-alt"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ messageData.unreadShares || 0 }}</div>
          <div class="stat-label">未读分享</div>
        </div>
      </div>

      <!-- 添加系统通知统计卡片 -->
      <div
        class="stat-card"
        :class="{ active: activeTab === 'system' }"
        @click="handleTabChange('system')"
      >
        <div class="stat-icon system">
          <i class="fas fa-bell"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ messageData.unreadSystemNotifies || 0 }}</div>
          <div class="stat-label">系统通知</div>
        </div>
      </div>
    </div>

    <!-- 消息列表区域 -->
    <div class="message-list">
      <div
        class="list-content"
        ref="messageListRef"
      >
        <!-- 初始加载状态 -->
        <div v-if="loading && getCurrentData().length === 0" class="initial-loading-state">
          <i class="fas fa-spinner fa-spin loading-icon-large"></i>
          <p>加载中...</p>
        </div>

        <!-- 使用统一消息卡片组件 -->
        <transition name="fade" mode="out-in">
          <UnifiedMessageCard
            v-if="activeTab === 'comments' && !(loading && getCurrentData().length === 0)"
            messageType="comment"
            :all-messages="allCommentsData"
            :unread-count="unreadCommentsCount"
            @refresh="handleTabChange(activeTab)"
            :key="'comments'"
          />

          <UnifiedMessageCard
            v-else-if="activeTab === 'likes' && !(loading && getCurrentData().length === 0)"
            messageType="like"
            :all-messages="allLikesData"
            :unread-count="unreadLikesCount"
            @refresh="handleTabChange(activeTab)"
            :key="'likes'"
          />

          <UnifiedMessageCard
            v-else-if="activeTab === 'shares' && !(loading && getCurrentData().length === 0)"
            messageType="share"
            :all-messages="allSharesData"
            :unread-count="unreadSharesCount"
            @refresh="handleTabChange(activeTab)"
            :key="'shares'"
          />

          <UnifiedMessageCard
            v-else-if="activeTab === 'system' && !(loading && getCurrentData().length === 0)"
            messageType="system"
            :all-messages="allSystemNotifiesData"
            :unread-count="unreadSystemNotifiesCount"
            @refresh="handleTabChange(activeTab)"
            :key="'system'"
          />
        </transition>

        <!-- 触底加载提示 -->
        <div v-if="isLoadingMore" class="load-more-loading">
          <i class="fas fa-spinner fa-spin loading-icon-small"></i>
          <span>加载中...</span>
        </div>
        <div v-else-if="!hasMore() && getCurrentData().length > 0" class="no-more-data">
          <span>没有更多数据了</span>
        </div>
      </div>
    </div>

    <!-- 移动端分页器 -->
    <!--    <div v-if="device === 'mobile' && getCurrentData().length > 0" class="mobile-pagination">-->
    <!--      <div class="page-info">-->
    <!--        <span>第 {{ pagination.current }} 页</span>-->
    <!--        <span class="separator">/</span>-->
    <!--        <span>共 {{ Math.ceil(pagination.total / pagination.pageSize) }} 页</span>-->
    <!--      </div>-->
    <!--      <div class="page-actions">-->
    <!--        <a-button-->
    <!--          class="page-button prev"-->
    <!--          :disabled="pagination.current === 1"-->
    <!--          @click="async () => await onPageChange(pagination.current - 1, pagination.pageSize)"-->
    <!--        >-->
    <!--          上一页-->
    <!--        </a-button>-->
    <!--        <a-button-->
    <!--          class="page-button next"-->
    <!--          :disabled="pagination.current >= Math.ceil(pagination.total / pagination.pageSize)"-->
    <!--          @click="async () => await onPageChange(pagination.current + 1, pagination.pageSize)"-->
    <!--        >-->
    <!--          下一页-->
    <!--        </a-button>-->
    <!--      </div>-->
    <!--    </div>-->
  </div>
</template>

<script setup lang="ts">
// @ts-nocheck
import { ref, onMounted, computed, onUnmounted, nextTick } from 'vue'
import { getAllUnreadCountUsingGet, markAllAsReadUsingPost, getMessageListUsingGet, markSingleAsReadUsingPost } from '@/api/messageCenterController'
import { CommentOutlined, LikeOutlined, ShareAltOutlined, HistoryOutlined, BellOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import UnifiedMessageCard from '@/components/UnifiedMessageCard.vue'
import { Grid } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useMessageStore } from '@/stores/useMessageStore'
import { useMessageCenterStore } from '@/stores/useMessageCenterStore'

const { useBreakpoint } = Grid
const screens = useBreakpoint()
const isMobile = computed(() => !screens.md)

const activeTab = ref('comments')
const messageData = ref({
  totalUnread: 0,
  unreadComments: 0,
  unreadLikes: 0,
  unreadShares: 0,
  unreadSystemNotifies: 0
})

// 新的响应式变量，用于存储所有消息数据
const allCommentsData = ref([])
const allLikesData = ref([])
const allSharesData = ref([])
const allSystemNotifiesData = ref([])

// 新的响应式变量，用于存储未读消息数量
const unreadCommentsCount = ref(0)
const unreadLikesCount = ref(0)
const unreadSharesCount = ref(0)
const unreadSystemNotifiesCount = ref(0)

// 分页相关状态
const pagination = ref({
  current: 1,
  pageSize: 20, // 每页20条数据
  total: 0
})

const loading = ref(false)
const isLoadingMore = ref(false)
const loadError = ref(false)
const scrollTimer = ref<number | null>(null)
const messageListRef = ref<HTMLDivElement | null>(null)
const device = ref('pc') // 设备类型

const markingRead = ref(false)
const messageStore = useMessageStore()
const messageCenterStore = useMessageCenterStore()
const router = useRouter()

// 检查是否还有更多数据
const hasMore = () => {
  return pagination.value.current * pagination.value.pageSize < pagination.value.total
}

// 获取当前标签页的数据
const getCurrentData = () => {
  switch (activeTab.value) {
    case 'comments': return allCommentsData.value
    case 'likes': return allLikesData.value
    case 'shares': return allSharesData.value
    case 'system': return allSystemNotifiesData.value
    default: return []
  }
}

// 获取评论数据（未读和已读）
const fetchCommentsData = async (isLoadMore = false) => {
  try {
    if (!isLoadMore) {
      loading.value = true
    }

    const res = await getMessageListUsingGet({
      type: 'comment',
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    })

    if (res.data?.code === 0) {
      const newComments = res.data.data?.records || []
      if (isLoadMore) {
        // 触底加载，追加数据
        allCommentsData.value = [...allCommentsData.value, ...newComments]
      } else {
        // 首次加载或刷新，替换数据
        allCommentsData.value = newComments
      }
      pagination.value.total = Number(res.data.data?.total || 0)
      // 计算未读数量
      unreadCommentsCount.value = newComments.filter(comment => comment.isRead === 0).length
    }
  } catch (error) {
    console.error('获取评论数据失败:', error)
    if (!isLoadMore) {
      allCommentsData.value = []
      pagination.value.total = 0
    }
  } finally {
    if (!isLoadMore) {
      loading.value = false
    }
  }
}

// 获取点赞数据（未读和已读）
const fetchLikesData = async (isLoadMore = false) => {
  try {
    if (!isLoadMore) {
      loading.value = true
    }

    const res = await getMessageListUsingGet({
      type: 'like',
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    })

    if (res.data?.code === 0) {
      const newLikes = res.data.data?.records || []
      if (isLoadMore) {
        // 触底加载，追加数据
        allLikesData.value = [...allLikesData.value, ...newLikes]
      } else {
        // 首次加载或刷新，替换数据
        allLikesData.value = newLikes
      }
      pagination.value.total = Number(res.data.data?.total || 0)
      // 计算未读数量
      unreadLikesCount.value = newLikes.filter(like => like.isRead === 0).length
    }
  } catch (error) {
    console.error('获取点赞数据失败:', error)
    if (!isLoadMore) {
      allLikesData.value = []
      pagination.value.total = 0
    }
  } finally {
    if (!isLoadMore) {
      loading.value = false
    }
  }
}

// 获取分享数据（未读和已读）
const fetchSharesData = async (isLoadMore = false) => {
  try {
    if (!isLoadMore) {
      loading.value = true
    }

    const res = await getMessageListUsingGet({
      type: 'share',
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    })

    if (res.data?.code === 0) {
      const newShares = res.data.data?.records || []
      if (isLoadMore) {
        // 触底加载，追加数据
        allSharesData.value = [...allSharesData.value, ...newShares]
      } else {
        // 首次加载或刷新，替换数据
        allSharesData.value = newShares
      }
      pagination.value.total = Number(res.data.data?.total || 0)
      // 计算未读数量
      unreadSharesCount.value = newShares.filter(share => share.isRead === 0).length
    }
  } catch (error) {
    console.error('获取分享数据失败:', error)
    if (!isLoadMore) {
      allSharesData.value = []
      pagination.value.total = 0
    }
  } finally {
    if (!isLoadMore) {
      loading.value = false
    }
  }
}

// 获取系统通知数据（未读和已读）
const fetchSystemNotifiesData = async (isLoadMore = false) => {
  try {
    if (!isLoadMore) {
      loading.value = true
    }

    const res = await getMessageListUsingGet({
      type: 'system',
      current: pagination.value.current,
      pageSize: pagination.value.pageSize
    })

    if (res.data?.code === 0) {
      const newNotifies = res.data.data?.records || []
      if (isLoadMore) {
        // 触底加载，追加数据
        allSystemNotifiesData.value = [...allSystemNotifiesData.value, ...newNotifies]
      } else {
        // 首次加载或刷新，替换数据
        allSystemNotifiesData.value = newNotifies
      }
      pagination.value.total = Number(res.data.data?.total || 0)
      // 计算未读数量
      unreadSystemNotifiesCount.value = newNotifies.filter(notify => notify.readStatus === 0).length
    }
  } catch (error) {
    console.error('获取系统通知数据失败:', error)
    if (!isLoadMore) {
      allSystemNotifiesData.value = []
      pagination.value.total = 0
    }
  } finally {
    if (!isLoadMore) {
      loading.value = false
    }
  }
}

// 处理标签切换
const handleTabChange = async (tab: string) => {
  activeTab.value = tab

  // 重置分页
  pagination.value.current = 1
  pagination.value.total = 0

  if (tab === 'comments') {
    await fetchCommentsData()
  } else if (tab === 'likes') {
    await fetchLikesData()
  } else if (tab === 'shares') {
    await fetchSharesData()
  } else if (tab === 'system') {
    await fetchSystemNotifiesData()
  }
}

// 触底加载更多
const loadMore = async () => {
  if (isLoadingMore.value || !hasMore()) return

  isLoadingMore.value = true
  try {
    pagination.value.current += 1

    if (activeTab.value === 'comments') {
      await fetchCommentsData(true)
    } else if (activeTab.value === 'likes') {
      await fetchLikesData(true)
    } else if (activeTab.value === 'shares') {
      await fetchSharesData(true)
    } else if (activeTab.value === 'system') {
      await fetchSystemNotifiesData(true)
    }
  } catch (error: any) {
    pagination.value.current -= 1
    message.error('加载更多失败：' + error.message)
  } finally {
    isLoadingMore.value = false
  }
}

// 分页变化
const onPageChange = async (page: number, pageSize: number) => {
  pagination.value.current = page
  pagination.value.pageSize = pageSize

  // 重新获取当前标签页数据，不重置分页
  if (activeTab.value === 'comments') {
    await fetchCommentsData()
  } else if (activeTab.value === 'likes') {
    await fetchLikesData()
  } else if (activeTab.value === 'shares') {
    await fetchSharesData()
  } else if (activeTab.value === 'system') {
    await fetchSystemNotifiesData()
  }
}

// 处理滚动事件
const handleScroll = () => {
  if (scrollTimer.value !== null) return
  scrollTimer.value = window.setTimeout(() => {
    // 检查是否滚动到页面底部
    const scrollTop = window.pageYOffset || document.documentElement.scrollTop
    const scrollHeight = document.documentElement.scrollHeight
    const clientHeight = window.innerHeight || document.documentElement.clientHeight

    // 当滚动到底部30px以内时触发加载更多
    if (scrollHeight - scrollTop - clientHeight < 30 && !loading.value && !isLoadingMore.value && hasMore()) {
      loadMore()
    }

    if (scrollTimer.value !== null) {
      clearTimeout(scrollTimer.value)
      scrollTimer.value = null
    }
  }, 150)
}

// 处理全部已读
const handleMarkAllRead = async () => {
  if (messageData.value.totalUnread <= 0) return

  try {
    markingRead.value = true
    const res = await markAllAsReadUsingPost()

    if (res.data.code === 0) {
      message.success('已将全部消息标记为已读')
      // 重置所有未读计数
      messageData.value.totalUnread = 0
      messageData.value.unreadComments = 0
      messageData.value.unreadLikes = 0
      messageData.value.unreadShares = 0
      messageData.value.unreadSystemNotifies = 0

      // 重置各个类型的未读数量变量
      unreadCommentsCount.value = 0
      unreadLikesCount.value = 0
      unreadSharesCount.value = 0
      unreadSystemNotifiesCount.value = 0

      // 更新全局状态
      messageCenterStore.updateUnreadMessageData({
        totalUnread: 0,
        unreadComments: 0,
        unreadLikes: 0,
        unreadShares: 0,
        unreadSystemNotifies: 0
      })

      // 更新当前标签页的数据
      await handleTabChange(activeTab.value)

      // 等待一小段时间让WebSocket接收更新数据
      await new Promise(resolve => setTimeout(resolve, 100))
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

// WebSocket消息处理函数
const handleMessageUnreadCountsUpdate = (event: CustomEvent) => {
  const data = event.detail
  if (data) {
    // 更新消息计数，确保将字符串转换为数字
    messageData.value.totalUnread = Number(data.totalUnread) || 0
    messageData.value.unreadComments = Number(data.unreadComments) || 0
    messageData.value.unreadLikes = Number(data.unreadLikes) || 0
    messageData.value.unreadShares = Number(data.unreadShares) || 0
    messageData.value.unreadSystemNotifies = Number(data.unreadSystemNotifies) || 0

    // 同时更新各个类型的未读数量变量
    unreadCommentsCount.value = Number(data.unreadComments) || 0
    unreadLikesCount.value = Number(data.unreadLikes) || 0
    unreadSharesCount.value = Number(data.unreadShares) || 0
    unreadSystemNotifiesCount.value = Number(data.unreadSystemNotifies) || 0

    // 同时更新全局状态
    messageCenterStore.updateUnreadMessageData({
      totalUnread: Number(data.totalUnread) || 0,
      unreadComments: Number(data.unreadComments) || 0,
      unreadLikes: Number(data.unreadLikes) || 0,
      unreadShares: Number(data.unreadShares) || 0,
      unreadSystemNotifies: Number(data.unreadSystemNotifies) || 0
    })

    console.log('通过WebSocket更新未读消息计数:', data)
  }
}

onMounted(async () => {
  // 设置设备类型
  device.value = isMobile.value ? 'mobile' : 'pc'

  // 添加WebSocket事件监听器
  window.addEventListener('messageUnreadCountsUpdated', handleMessageUnreadCountsUpdate as EventListener)

  // 添加滚动事件监听器
  window.addEventListener('scroll', handleScroll)

  // 进入页面时刷新全局未读消息计数
  messageStore.refreshUnreadCount()

  // 直接从后端获取最新的未读消息数据
  try {
    const res = await getAllUnreadCountUsingGet()
    if (res.data?.code === 0 && res.data.data) {
      const data = res.data.data
      messageData.value = {
        totalUnread: Number(data.totalUnread) || 0,
        unreadComments: Number(data.unreadComments) || 0,
        unreadLikes: Number(data.unreadLikes) || 0,
        unreadShares: Number(data.unreadShares) || 0,
        unreadSystemNotifies: Number(data.unreadSystemNotifies) || 0
      }

      // 同时更新各个类型的未读数量变量
      unreadCommentsCount.value = Number(data.unreadComments) || 0
      unreadLikesCount.value = Number(data.unreadLikes) || 0
      unreadSharesCount.value = Number(data.unreadShares) || 0
      unreadSystemNotifiesCount.value = Number(data.unreadSystemNotifies) || 0

      // 同时更新全局状态
      messageCenterStore.updateUnreadMessageData({
        totalUnread: Number(data.totalUnread) || 0,
        unreadComments: Number(data.unreadComments) || 0,
        unreadLikes: Number(data.unreadLikes) || 0,
        unreadShares: Number(data.unreadShares) || 0,
        unreadSystemNotifies: Number(data.unreadSystemNotifies) || 0
      })

      console.log('从后端获取初始未读消息数据:', messageData.value)
    }
  } catch (error) {
    console.error('获取初始未读消息数据失败:', error)
  }

  // 等待一小段时间让WebSocket接收初始数据
  await new Promise(resolve => setTimeout(resolve, 100))

  // 根据未读消息数量决定默认标签
  if (messageData.value.unreadComments > 0) {
    activeTab.value = 'comments'
    await fetchCommentsData()
  } else if (messageData.value.unreadLikes > 0) {
    activeTab.value = 'likes'
    await fetchLikesData()
  } else if (messageData.value.unreadShares > 0) {
    activeTab.value = 'shares'
    await fetchSharesData()
  } else if (messageData.value.unreadSystemNotifies > 0) {
    activeTab.value = 'system'
    await fetchSystemNotifiesData()
  } else {
    // 如果都没有未读消息，默认显示评论
    activeTab.value = 'comments'
    await fetchCommentsData()
  }
})

onUnmounted(() => {
  // 移除WebSocket事件监听器
  window.removeEventListener('messageUnreadCountsUpdated', handleMessageUnreadCountsUpdate as EventListener)

  // 移除滚动事件监听器
  window.removeEventListener('scroll', handleScroll)

  // 清除定时器
  if (scrollTimer.value !== null) {
    clearTimeout(scrollTimer.value)
  }
})
</script>


<style scoped>
/* 原有的样式保持不变 */
.message-center {
  max-width: 1400px;
  margin: 0 auto;
  padding: 4px 20px;
  background: transparent;
}

.pc-message-center {
  padding-top: 48px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding: 16px;
  margin-top: 6px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.header-left h2 {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  color: var(--text-primary);
  letter-spacing: -0.5px;
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
}

.mark-read-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #ff7a33 0%, #ff5151 100%);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.mark-read-btn:disabled {
  background: var(--header-background);
  color: var(--text-primary);
  cursor: not-allowed;
  opacity: 0.6;
  transform: none;
  box-shadow: none;
}

.mark-read-btn:active:not(:disabled) {
  transform: translateY(0);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 20px;
  padding: 0 16px;
}

.stat-card {
  background: var(--card-background);
  color: var(--text-primary);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
  cursor: pointer;
  border: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
  border-color: #ff8e53;
}

.stat-card.active {
  border-color: #ff8e53;
  background: var(--hover-background);
  box-shadow: inset 0 0 0 1px #ff8e53;
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
}

.stat-icon.comments {
  background: rgba(96, 165, 250, 0.1);
  color: #60a5fa;
}

.stat-icon.likes {
  background: rgba(255, 107, 107, 0.1);
  color: #ff6b6b;
}

.stat-icon.shares {
  background: rgba(96, 195, 213, 0.1);
  color: #60c3d5;
}

/* 系统通知图标样式 */
.stat-icon.system {
  background: rgba(168, 85, 247, 0.1);
  color: #a855f7;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .message-center {
    padding: 0;
    background: var(--header-background);
    color: var(--text-primary);
  }

  .message-header {
    flex-direction: row;
    align-items: center;
    margin-bottom: 16px;
    padding: 16px;
    background: var(--header-background);
    color: var(--text-primary);
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
    background: var(--header-background);
    color: var(--text-primary);
  }

  .history-btn :deep(.anticon) {
    font-size: 14px;
  }

  .mark-read-btn {
    height: 28px;
    font-size: 12px;
    padding: 0 10px;
    border-radius: 14px;
    white-space: nowrap;
  }

  .stats-cards {
    grid-template-columns: repeat(4, 1fr);
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
  .empty-state.inline p {
    font-size: 12px;
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
  color: var(--text-primary);
  border-radius: 20px;
  margin-top: 32px;
  overflow: hidden;
  background: transparent;
}

.list-content {
  padding: 0;
  width: 100%;
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
}

.history-btn:hover {
  color: #ff8e53;
  border-color: #ff8e53;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #666;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}

/* 添加分页器样式 */
.pagination-container {
  padding: 20px;
  display: flex;
  justify-content: center;
  background: var(--header-background);
  color: var(--text-primary);
  border-top: 1px solid #f0f0f0;
}

/* 移动端分页器样式 */
.mobile-pagination {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  margin-bottom: 42px;
  width: 100%;
}

.page-info {
  text-align: center;
  margin-bottom: 12px;
  margin-top: 24px;
  color: #666;
  font-size: 14px;
}

.separator {
  margin: 0 8px;
  color: #999;
}

.page-actions {
  display: flex;
  gap: 12px;
  padding: 0 20px 20px 20px;
}

.page-button {
  flex: 1;
  height: 36px;
  border-radius: 8px;
  font-size: 14px;
  border: none;
  transition: all 0.3s ease;
}

.page-button.prev {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
}

.page-button.next {
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  color: white;
}

.page-button:not(:disabled):hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.3);
}

.page-button:disabled {
  background: #f5f5f5;
  color: #999;
}

/* 添加 section header 样式 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: var(--card-background);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin: 32px 0 16px 0;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: #1a1a1a;
}

.section-count {
  font-size: 14px;
  color: #666;
}

/* 添加空状态样式 */
.empty-section {
  margin-bottom: 16px;
}

.empty-state.inline {
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.empty-icon.small {
  font-size: 24px;
  margin-bottom: 8px;
}

.empty-state.inline p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

/* 初始加载状态样式 */
.initial-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #999;
}

.initial-loading-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* 触底加载更多样式 */
.load-more-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 12px;
  color: #999;
  gap: 8px;
}

.no-more-data {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 14px;
}

/* 淡入淡出过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
