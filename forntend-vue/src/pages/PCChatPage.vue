<template>
  <div class="pc-chat-container">
    <!-- 左侧聊天列表 -->
    <div class="left-panel" :style="{ width: leftPanelWidth + 'px' }">
      <div class="chat-header">
        <div class="header-top">
          <h1 class="page-title">消息</h1>
          <div
            v-if="unreadCounts.totalUnread > 0 && activeTab === 'chat'"
            class="clear-all-btn"
            @click.stop="showClearUnreadModal"
            title="清除所有未读"
          >
            <i class="fas fa-broom"></i>
          </div>
        </div>

        <div class="chat-tabs">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'chat' }"
            @click="handleTabChange('chat')"
          >
            <div class="icon-wrap">
              <i class="far fa-comment icon"></i>
              <a-badge
                v-if="unreadCounts.totalUnread > 0"
                :count="unreadCounts.totalUnread"
                :overflowCount="99"
                class="tab-badge"
              />
            </div>
            <span class="tab-text">聊天</span>
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'tribe' }"
            @click="handleTabChange('tribe')"
          >
            <div class="icon-wrap">
              <i class="fas fa-users icon"></i>
            </div>
            <span class="tab-text">部落</span>
          </div>
        </div>

        <div class="search-section" v-if="activeTab === 'chat'">
          <div class="search-box">
            <i class="fas fa-search search-icon-left"></i>
            <input
              type="text"
              v-model="searchText"
              placeholder="搜索"
              @keyup.enter="handleSearch"
              class="search-input"
            />
          </div>
        </div>
      </div>

      <div class="chat-list-container" ref="chatListContainerRef" style=" min-width: 400px;" @scroll="handleListScroll">
        <div v-if="activeTab === 'chat'">
          <!-- 空状态 -->
          <div v-if="!loading && filteredChatList.length === 0" class="empty-state">
            <img :src="getEmptyStateImage()" alt="暂无数据" class="empty-icon" />
            <h3>{{ getEmptyText() }}</h3>
            <p v-if="searchText">换个关键词试试吧</p>
          </div>

          <!-- 聊天列表 -->
          <a-list v-else
                  :loading="loading"
                  :data-source="filteredChatList"
                  :split="true"
                  class="chat-list"
          >
            <template #renderItem="{ item }">
              <div class="chat-item-wrapper"
                   @click="handleChatSelect(item)"
                   :class="{
                     active: selectedChat?.id === item.id,
                     unread: getUnreadCount(item) > 0
                   }">
                <div class="chat-item">
                  <div class="chat-avatar">
                    <img :src="item.targetUser?.userAvatar || getDefaultAvatar(item.targetUser?.userName)" />
                    <div v-if="getUnreadCount(item) > 0" class="unread-badge">
                      {{ getUnreadCount(item) > 99 ? '99+' : getUnreadCount(item) }}
                    </div>
                  </div>
                  <div class="chat-content">
                    <div class="chat-header">
                      <div class="chat-name">
                        <span class="name-text">{{ item.isSender ? item.userChatName : item.targetUserChatName }}</span>
                        <div class="chat-tags">
                          <span :class="getChatTypeClass(item)">
                            {{ item.chatType === 3 ? '群组' : item.chatType === 2 ? 'AI' : '好友' }}
                          </span>
                        </div>
                      </div>
                      <span class="chat-time">{{ formatMessageTime(item.lastMessageTime) }}</span>
                    </div>
                    <div class="chat-message">{{ item.lastMessage || '暂无消息' }}</div>
                  </div>
                </div>
              </div>
            </template>
          </a-list>
        </div>
        <div v-else-if="activeTab === 'tribe'" class="recommended-spaces-container">
          <RecommendedSpacesList ref="recommendedSpacesListRef" />
        </div>

        <!-- 滚动触底加载提示 -->
        <div v-if="activeTab === 'chat'" class="load-more-container">
          <div v-if="loadingMore" class="loading-more">
            <i class="fas fa-spinner fa-spin"></i>
            加载中...
          </div>
          <div v-else-if="hasMore" class="load-more-trigger" @click="loadMore">
            点击加载更多
          </div>
          <div v-else-if="!hasMore && filteredChatList.length > 0" class="no-more-data">
            已加载全部数据
          </div>
        </div>
      </div>

      <!-- 修改名称弹窗 -->
      <a-modal
        v-model:visible="editNameVisible"
        title="修改聊天名称"
        @ok="handleEditNameConfirm"
        :confirmLoading="editNameLoading"
      >
        <a-input
          v-model:value="newChatName"
          placeholder="请输入新的聊天名称"
          maxlength="50"
          show-count
        />
      </a-modal>
    </div>

    <!-- 分隔条 -->
    <div class="panel-resizer"
         @mousedown="startResize"
         @touchstart="startResize"
         @touchmove.prevent></div>

    <!-- 右侧聊天详情 -->
    <div class="right-panel" v-if="activeTab === 'chat' && selectedChat">
      <div class="chat-header">
        <div class="user-info">
          <img :src="selectedChat.targetUser?.userAvatar || getDefaultAvatar(selectedChat.targetUser?.userName)"
               :alt="selectedChat.targetUser?.userName"
               class="avatar"
               @click="handleAvatarClick">
          <div class="user-meta">
            <span class="username">{{ selectedChat.targetUser?.userName }}</span>
            <!-- 根据聊天类型显示不同的状态 -->
            <template v-if="selectedChat.chatType === 3">
              <span class="group-status">全员群</span>
            </template>
            <template v-else>
              <span class="status-text" :class="{ online: isOnline }">
                {{ isOnline ? '在线' : '离线' }}
              </span>
            </template>
          </div>
        </div>
        <!-- 添加在线用户和公告按钮 -->
        <div v-if="selectedChat.chatType === 3" class="header-actions">
          <button class="action-btn announcement-btn" @click="showAnnouncement = true">
            <i class="fas fa-bullhorn"></i>
          </button>
          <button class="action-btn online-users-btn" @click="showOnlineUsers = true">
            <i class="fas fa-users"></i>
            <span class="online-count" v-if="onlineUsers.length > 0">({{ onlineUsers.length }})</span>
          </button>
        </div>
      </div>

      <!-- 使用key强制重新创建组件 -->
      <PictureChatRoom
        :key="selectedChat.id"
        ref="chatRoomRef"
        :privateChatId="selectedChat.id"
        :spaceId="selectedChat.id === -2 ? -2 : undefined"
        :type="selectedChat.chatType === 3 ? 'space' : 'private'"
        @message="handleChatMessage"
        @mounted="handleChatMounted"
        @update:onlineUsers="updateOnlineUsers"
      />
    </div>

    <!-- 右侧部落列表 -->
    <div class="right-panel" v-else-if="activeTab === 'tribe'">
      <TribeList ref="tribeListRef" />
    </div>

    <!-- 未选择聊天时的右侧面板（仅在聊天选项卡显示） -->
    <div class="right-panel empty-chat" v-else-if="activeTab === 'chat' && !selectedChat">
      <div class="empty-state">
        <img src="/src/assets/nuv.png" alt="选择一个聊天">
        <p>选择一个聊天开始交谈</p>
      </div>
    </div>

    <!-- 添加清除未读消息确认弹框 -->
    <div v-if="clearUnreadVisible" class="modal-overlay" @click="clearUnreadVisible = false">
      <div class="modal-content clear-unread-modal" @click.stop>
        <div class="modal-icon clear">
          <i class="fas fa-broom"></i>
        </div>
        <h3>清除所有未读消息</h3>
        <p class="unread-count">
          当前共有 <span class="count">{{ unreadCounts.totalUnread }}</span> 条未读消息
          <template v-if="unreadCounts.friendUnread > 0 || unreadCounts.privateUnread > 0">
            <br>
            <span class="detail">
              好友消息: {{ unreadCounts.friendUnread }} 条
              <br>
              私信消息: {{ unreadCounts.privateUnread }} 条
            </span>
          </template>
        </p>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="clearUnreadVisible = false">
            取消
          </button>
          <button
            class="modal-btn confirm"
            :disabled="clearUnreadLoading"
            @click="confirmClearUnread"
          >
            <span v-if="clearUnreadLoading" class="loading-dots">清除中</span>
            <span v-else>确认清除</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 在线用户弹框 -->
    <teleport to="body">
      <div v-if="showOnlineUsers" class="user-modal-overlay" @click="showOnlineUsers = false">
        <div class="user-modal-content" @click.stop>
          <div class="modal-header">
            <h3 class="modal-title">
              <i class="fas fa-users"></i>
              在线用户 ({{ onlineUsers.length }})
            </h3>
            <button class="modal-close" @click="showOnlineUsers = false">
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
      width="400px"
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, Badge } from 'ant-design-vue'
import {
  HistoryOutlined,
  SearchOutlined,
  EllipsisOutlined,
  DeleteOutlined,
  EditOutlined,
  LeftOutlined,
  RightOutlined,
  ClearOutlined,
  NotificationOutlined,
  TeamOutlined,
  MessageOutlined
} from '@ant-design/icons-vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import { listPrivateChatByPageUsingPost, deletePrivateChatUsingPost, updateChatNameUsingPost, clearUnreadCountUsingPost } from '@/api/privateChatController'
import { formatMessageTime } from "@/utils/dateUtils"
import PictureChatRoom from '@/components/PictureChatRoom.vue'
import TribeList from '@/components/TribeList.vue'
import RecommendedSpacesList from '@/components/RecommendedSpacesList.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

import { chatListWebSocket } from '@/utils/chatListWebSocket'

// 定义类型
interface PrivateChat {
  id: string;
  userId: string;
  targetUserId: string;
  targetUser?: {
    id: string;
    userName: string;
    userAvatar?: string;
    userAccount: string;
    createTime: string;
  };
  chatType: number;
  lastMessage?: string;
  lastMessageTime: string;
  targetUserChatName: string;
  isSender: boolean;
  userChatName?: string;
  userUnreadCount: number;
}

const router = useRouter()
const chatListRef = ref()
const chatRoomRef = ref()
const selectedChat = ref<PrivateChat | null>(null)
const isOnline = ref(false)
const recommendedSpacesListRef = ref()
const chatListContainerRef = ref<HTMLElement>()

// 在线用户和公告相关状态
const onlineUsers = ref<any[]>([])
const showOnlineUsers = ref(false)
const showAnnouncement = ref(false)

const loginUserStore = useLoginUserStore()
// 搜索参数
const searchParams = ref({
  current: 1,
  pageSize: 10, // 默认每页8条
  searchText: '',
  chatType: undefined as number | undefined
})

// 分页大小选项
const pageSizeOptions = [10,12, 16, 24]

// 触底加载相关状态
const loadingMore = ref(false)
const hasMore = ref(true)
const allChatList = ref<PrivateChat[]>([])

// 防抖变量
let loadMoreTimer: number | null = null

// 状态变量
const loading = ref(false)
const searchText = ref('')
const activeTab = ref('chat')
const chatList = ref<PrivateChat[]>([])
const editNameVisible = ref(false)
const editNameLoading = ref(false)
const newChatName = ref('')
const total = ref(0)

const clearUnreadVisible = ref(false)
const clearUnreadLoading = ref(false)

// 使用WebSocket的未读消息总数
const unreadCounts = computed(() => chatListWebSocket.unreadCounts.value)
const friendUnreadCount = computed(() => unreadCounts.value.friendUnread)
const privateUnreadCount = computed(() => unreadCounts.value.privateUnread)

// AI助手配置
const aiChat = {
  id: -1,
  targetUser: {
    id: -1,
    userName: '悦木助手',
    userAvatar: new URL('@/assets/images/ai.png', import.meta.url).href,
    userAccount: 'yuemuai',
    createTime: new Date().toISOString()
  },
  chatType: 2,
  lastMessage: '你好!我是悦木助手，一个智能AI助手',
  lastMessageTime: new Date().toISOString(),
  targetUserChatName: '悦木助手',
  isSender: false,
  userUnreadCount: 0
}

// 添加全员群配置
const publicGroupChat = {
  id: -2,
  targetUser: {
    id: -2,
    userName: '悦木全员群',
    userAvatar: new URL('@/assets/images/all.png', import.meta.url).href,
    userAccount: 'public_group',
    createTime: new Date().toISOString()
  },
  chatType: 3,
  lastMessage: '欢迎来到悦木全员群~',
  lastMessageTime: new Date().toISOString(),
  targetUserChatName: '悦木全员群',
  isSender: false,
  userUnreadCount: 0
}



// 计算属性：过滤后的聊天列表
const filteredChatList = computed(() => {
  if (activeTab.value !== 'chat') {
    return [];
  }

  // 始终包含AI助手和公共群
  let specialChats = [aiChat, publicGroupChat]

  // 使用所有聊天列表数据
  let filtered = allChatList.value

  // 搜索过滤
  if (searchText.value) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter((chat: PrivateChat) => {
      const targetName = chat.targetUser?.userName?.toLowerCase() || ''
      const chatName = (chat.isSender ? chat.userChatName : chat.targetUserChatName)?.toLowerCase() || ''
      const lastMessage = chat.lastMessage?.toLowerCase() || ''
      return targetName.includes(searchLower) ||
        chatName.includes(searchLower) ||
        lastMessage.includes(searchLower)
    })

    // 如果搜索文本匹配特殊聊天，也包含它们
    specialChats = specialChats.filter(chat => {
      const name = chat.targetUser?.userName?.toLowerCase() || ''
      const message = chat.lastMessage?.toLowerCase() || ''
      return name.includes(searchLower) || message.includes(searchLower)
    })
  }

  // 按最后消息时间排序普通聊天
  filtered.sort((a: PrivateChat, b: PrivateChat) =>
    new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime()
  )

  // 合并特殊聊天和普通聊天
  return [...specialChats, ...filtered]
})

// 获取未读消息数
const getUnreadCount = (chat: PrivateChat) => {
  return chat.userUnreadCount || 0
}

// 计算未读消息数
const calculateUnreadCounts = () => {
  // This function is no longer needed as unread counts are managed by WebSocket
}

// 监听聊天列表变化，重新计算未读数
watch(() => chatList.value, calculateUnreadCounts, { deep: true })

// 获取聊天列表
const fetchChatList = async (reset = true) => {
  if (loading.value) return
  try {
    if (reset) {
      loading.value = true
      searchParams.value.current = 1
      allChatList.value = []
    }

    // 使用WebSocket请求聊天列表
    chatListWebSocket.sendMessage({
      type: 'REQUEST_LIST',
      data: {
        searchText: searchText.value,
        current: searchParams.value.current,
        pageSize: searchParams.value.pageSize,
        // 新版本中，聊天标签页显示所有聊天（包括好友和私信），不设置chatType
        chatType: undefined
      }
    })

  } catch (error: any) {
    // message.error('获取聊天列表失败：' + error.message)
    loading.value = false
  }
}

// 处理标签切换
const handleTabChange = (tab: string) => {
  activeTab.value = tab
  if (tab === 'chat') {
    searchParams.value.current = 1
    fetchChatList()
  } else if (tab === 'tribe') {
    // 当切换到部落标签时，如果推荐部落列表组件存在，则刷新数据
    nextTick(() => {
      if (recommendedSpacesListRef.value && recommendedSpacesListRef.value.refresh) {
        recommendedSpacesListRef.value.refresh();
      }
    });
  }
}

// 处理搜索
const handleSearch = () => {
  searchParams.value.current = 1
  fetchChatList()
}

// 处理页码变化
const handlePageChange = (page: number) => {
  searchParams.value.current = page
  fetchChatList()
}

// 处理每页条数变化
const handleSizeChange = (size: number) => {
  searchParams.value.pageSize = size
  searchParams.value.current = 1 // 切换每页条数时重置为第一页
  fetchChatList()
}

// 加载更多聊天
const loadMore = () => {
  // 核心修复：移除 setTimeout 防抖，改用即时状态锁。
  // 连续滚动时，setTimeout 会不断重置导致请求发出太晚，甚至需要“再次滚动”来触发。
  if (loading.value || loadingMore.value || !hasMore.value) return;

  loadingMore.value = true
  searchParams.value.current += 1

  // 使用WebSocket请求聊天列表
  chatListWebSocket.sendMessage({
    type: 'REQUEST_LIST',
    data: {
      searchText: searchText.value,
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize,
      chatType: undefined
    }
  })
}

// 处理滚动事件，实现触底加载
const handleListScroll = (event: Event) => {
  const target = event.target as HTMLElement;
  const { scrollTop, scrollHeight, clientHeight } = target;

  // 严谨校验：仅在非加载状态且有更多数据时探测触底
  // 增加到 100px 阈值以提升响应感
  if (!loading.value && !loadingMore.value && hasMore.value) {
    if (scrollHeight - scrollTop - clientHeight < 100) {
      loadMore();
    }
  }
}

// 自动探测：检查是否需要继续加载数据
// 解决“两次滚动”问题的核心逻辑：加载完成后主动探测高度，而非等待 scroll 事件
const checkAutoLoad = () => {
  nextTick(() => {
    if (!chatListContainerRef.value) return;
    const { scrollTop, scrollHeight, clientHeight } = chatListContainerRef.value;

    // 如果由于数据量少没撑满或用户已经滚到底部，立即加载下一页
    if (scrollHeight - scrollTop - clientHeight < 100 && hasMore.value && !loading.value && !loadingMore.value) {
      console.log('检测到处于底部且有更多数据，自动触发分页加载');
      loadMore();
    }
  });
}

// 处理聊天选择
const handleChatSelect = async (chat: PrivateChat) => {
  if (chat.id === '-1' || Number(chat.id) === -1) {
    // AI chat - 跳转到AI聊天页面
    router.push('/chat/ai')
  } else if (chat.targetUser) {
    try {
      // 清除未读消息
      await clearUnreadCountUsingPost({
        targetUserId: String(chat.targetUser.id),
        isSender: chat.isSender
      })

      // 更新本地未读数
      const updatedChat = chatList.value.find(item => item.id === chat.id)
      if (updatedChat) {
        updatedChat.userUnreadCount = 0
      }
    } catch (error) {
      console.error('清除未读消息失败:', error)
    }

    selectedChat.value = chat
  }
}

// 显示删除确认框
const showDeleteConfirm = (chat: PrivateChat) => {
  Modal.confirm({
    title: '确认删除该聊天？',
    content: `用户名称：${chat.targetUser?.userName || '未设置'}\n聊天类型：${chat.chatType === 1 ? '好友' : '私信'}`,
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const res = await deletePrivateChatUsingPost({
          privateChatId: chat.id
        })
        if (res.data.code === 0) {
          message.success('删除成功')
          // 通知其他客户端
          chatListWebSocket.sendMessage({
            type: 'DELETE',
            data: { id: chat.id }
          })
        } else {
          // message.error('删除失败：' + res.data.message)
        }
      } catch (error) {
        // message.error('删除失败，请稍后重试')
      }
    }
  })
}

// 显示修改名称弹窗
const showEditNameModal = (chat: PrivateChat) => {
  newChatName.value = chat.userChatName || ''
  editNameVisible.value = true
  selectedChat.value = chat
}

// 确认修改名称
const handleEditNameConfirm = async () => {
  if (!selectedChat.value?.id || !newChatName.value.trim()) return

  try {
    editNameLoading.value = true
    const res = await updateChatNameUsingPost({
      privateChatId: selectedChat.value.id,
      chatName: newChatName.value.trim()
    })

    if (res.data.code === 0) {
      message.success('修改成功')
      // 通知其他客户端
      chatListWebSocket.sendMessage({
        type: 'UPDATE',
        data: {
          id: selectedChat.value.id,
          chatName: newChatName.value.trim()
        }
      })
      editNameVisible.value = false
    } else {
      // message.error('修改失败：' + res.data.message)
    }
  } catch (error) {
    // message.error('修改失败：' + (error as Error).message)
  } finally {
    editNameLoading.value = false
  }
}

// 获取聊天类型样式
const getChatTypeClass = (chat: PrivateChat) => {
  if (chat.chatType === 2) return 'ai-tag'
  if (chat.chatType === 3) return 'group-tag'
  return chat.chatType === 1 ? 'friend-tag' : 'private-tag'
}

// 获取空状态图片
const getEmptyStateImage = () => {
  if (searchText.value) {
    return 'https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg'
  }
  return 'https://gw.alipayobjects.com/zos/antfincdn/ZHrcdLPrvN/empty.svg'
}

// 获取空状态文本
const getEmptyText = () => {
  if (searchText.value) {
    return '没有找到相关聊天'
  }
  switch (activeTab.value) {
    case 'friend':
      return '暂无好友聊天'
    case 'private':
      return '暂无私信聊天'
    default:
      return '暂无聊天记录'
  }
}

// 处理头像点击
const handleAvatarClick = () => {
  if (!selectedChat.value?.targetUser) return

  router.push({
    path: `/user/${selectedChat.value.targetUser.id}`,
    query: {
      userName: selectedChat.value.targetUser.userName,
      userAvatar: selectedChat.value.targetUser.userAvatar,
      userAccount: selectedChat.value.targetUser.userAccount,
      createTime: selectedChat.value.targetUser.createTime
    }
  })
}

// 处理聊天组件挂载完成
const handleChatMounted = async () => {
  await nextTick()
  const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
  if (messageContainer) {
    messageContainer.scrollTop = messageContainer.scrollHeight
  }
}

// 处理聊天消息
const handleChatMessage = (msg: {
  type: string;
  message?: string;
  onlineUsers?: Array<{
    id: string | number;
    userName: string;
  }>;
  messageId?: string | number;
}) => {
  if (msg.type === 'error') {
    console.error('聊天错误:', msg.message)
    return
  }

  // 处理在线用户消息
  if (msg.type === 'onlineUsers') {
    const targetUserOnline = msg.onlineUsers?.some(
      (user) => user.id === selectedChat.value?.targetUser?.id
    ) ?? false
    isOnline.value = targetUserOnline
  }

  // 处理历史消息和更多历史消息
  else if (msg.type === 'history' || msg.type === 'moreHistory') {
    if (msg.type === 'history') {
      nextTick(() => {
        const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
        if (messageContainer) {
          messageContainer.scrollTop = messageContainer.scrollHeight
        }
      })
    }
  }

  // 处理撤回消息
  else if (msg.type === 'RECALL' && msg.messageId) {
    // 更新聊天列表中的最后一条消息
    if (selectedChat.value) {
      const chat = chatList.value.find(item => item.id === selectedChat.value?.id)
      if (chat) {
        chat.lastMessage = '消息已撤回'
      }
    }
  }

  // 处理普通消息
  else if (msg.type === 'message') {
    nextTick(() => {
      const messageContainer = chatRoomRef.value?.$el.querySelector('.chat-messages')
      if (messageContainer) {
        messageContainer.scrollTop = messageContainer.scrollHeight
      }
    })
  }
}

// 更新在线用户
const updateOnlineUsers = (users: any[]) => {
  onlineUsers.value = users
}

// 处理用户点击
const handleUserClick = (user: any) => {
  // 这里可以添加点击用户的逻辑
  console.log('点击用户:', user)
  // 关闭弹窗
  showOnlineUsers.value = false
}

// 计算显示的页码范围
const displayedPages = computed(() => {
  const maxPage = Math.ceil(total.value / searchParams.value.pageSize)
  const current = searchParams.value.current
  const delta = 2 // 当前页前后显示的页码数

  let start = Math.max(1, current - delta)
  let end = Math.min(maxPage, current + delta)

  // 调整start和end，确保显示5个页码
  if (end - start < 4) {
    if (start === 1) {
      end = Math.min(5, maxPage)
    } else {
      start = Math.max(1, end - 4)
    }
  }

  const pages = []

  // 添加第一页
  if (start > 1) {
    pages.push(1)
    if (start > 2) pages.push('...')
  }

  // 添加中间页码
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }

  // 添加最后一页
  if (end < maxPage) {
    if (end < maxPage - 1) pages.push('...')
    pages.push(maxPage)
  }

  return pages
})

// 修复select事件处理
const handleSelectChange = (event: Event) => {
  const target = event.target as HTMLSelectElement
  const value = String(target.value)
  if (!isNaN(value)) {
    handleSizeChange(value)
  }
}

// 添加key来强制重新渲染
const key = ref(Date.now)

// 监听选中的聊天变化
watch(() => selectedChat.value?.id, async (newId, oldId) => {
  if (newId && newId !== oldId) {
    // 等待DOM更新后再操作
    await nextTick()
    if (chatRoomRef.value) {
      // 重新初始化WebSocket连接
      chatRoomRef.value.initWebSocket()
    }
  }
})

// 添加左侧面板宽度相关的状态
const leftPanelWidth = ref(400)
const isDragging = ref(false)
const minWidth = 400
const maxWidth = 800
const touchStartX = ref(0)
const initialWidth = ref(0)

// 开始拖动
const startResize = (e: MouseEvent | TouchEvent) => {
  isDragging.value = true
  if (e instanceof MouseEvent) {
    document.addEventListener('mousemove', handleResize)
    document.addEventListener('mouseup', stopResize)
  } else {
    touchStartX.value = e.touches[0].clientX
    initialWidth.value = leftPanelWidth.value
    document.addEventListener('touchmove', handleTouchResize)
    document.addEventListener('touchend', stopResize)
  }
  // 防止选中文本
  document.body.style.userSelect = 'none'
}

// 处理触摸拖动
const handleTouchResize = (e: TouchEvent) => {
  if (!isDragging.value) return

  const touchDelta = e.touches[0].clientX - touchStartX.value
  const newWidth = initialWidth.value + touchDelta

  if (newWidth >= minWidth && newWidth <= maxWidth) {
    leftPanelWidth.value = newWidth
  }

  // 阻止页面滚动
  e.preventDefault()
}

// 处理鼠标拖动
const handleResize = (e: MouseEvent) => {
  if (!isDragging.value) return

  const newWidth = e.clientX
  if (newWidth >= minWidth && newWidth <= maxWidth) {
    leftPanelWidth.value = newWidth
  }
}

// 停止拖动
const stopResize = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleTouchResize)
  document.removeEventListener('touchend', stopResize)
  document.body.style.userSelect = ''
}

// 添加清除所有未读消息的方法
const handleClearAllUnread = async () => {
  try {
    // 显示确认对话框
    Modal.confirm({
      title: '确认清除所有未读消息？',
      content: `将清除 ${unreadCounts.value.totalUnread} 条未读消息`,
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        chatListWebSocket.clearAllUnreadMessages()
        message.success('已清除所有未读消息')
      }
    })
  } catch (error) {
    // message.error('清除未读消息失败')
  }
}

// 显示清除未读消息弹框
const showClearUnreadModal = () => {
  clearUnreadVisible.value = true
}

// 确认清除未读消息
const confirmClearUnread = async () => {
  try {
    clearUnreadLoading.value = true
    // 使用WebSocket发送清除所有未读消息的请求
    chatListWebSocket.sendMessage({
      type: 'CLEAR_ALL_UNREAD'
    })

    // 清除本地未读消息状态
    chatList.value.forEach(chat => {
      if (chat) {
        chat.userUnreadCount = 0
      }
    })

    // 等待后端响应
    await new Promise(resolve => setTimeout(resolve, 500))

    // 主动请求最新的未读消息计数
    chatListWebSocket.sendMessage({
      type: 'REQUEST_UNREAD_COUNTS'
    })

  } catch (error) {
    // message.error('清除未读消息失败')
  } finally {
    clearUnreadLoading.value = false
    clearUnreadVisible.value = false
  }
}

// 组件卸载时清理事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', handleResize)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleTouchResize)
  document.removeEventListener('touchend', stopResize)

  // 清理 WebSocket 连接
  chatListWebSocket.disconnect()


})

// 修改onMounted钩子
onMounted(async () => {
  // 初始化 WebSocket 连接
  if (loginUserStore.loginUser?.id) {
    await chatListWebSocket.connect(loginUserStore.loginUser.id)

    // 添加 WebSocket 事件监听
    chatListWebSocket.on('message', (data: any) => {
      if (!data) return

      // 停止加载状态
      loading.value = false

      // 检查数据类型
      if (data.type === 'CHAT_LIST') {
        // 处理完整的聊天列表数据
        const records = (data.records || []).map(record => ({
          ...record,
          id: String(record.id),
          userId: String(record.userId),
          targetUserId: String(record.targetUserId)
        }))
        const totalCount = parseInt(data.total) || 0
        const currentPage = parseInt(data.current) || 1
        const pageSize = parseInt(data.pageSize) || 10

        // 如果是第一页，替换整个列表；否则追加到现有列表
        if (currentPage === 1) {
          // 更新聊天列表，不包含特殊聊天项
          allChatList.value = records.filter(chat => chat.id !== '-1' && chat.id !== '-2')
        } else {
          // 追加到现有列表
          const newChats = records.filter(chat =>
            chat.id !== '-1' && chat.id !== '-2' &&
            !allChatList.value.some(existing => existing.id === chat.id)
          )
          allChatList.value = [...allChatList.value, ...newChats]
        }

        // 更新总记录数和是否有更多数据
        total.value = totalCount
        hasMore.value = allChatList.value.length < totalCount

        // 重置加载状态
        loading.value = false
        loadingMore.value = false

        // 如果当前页大于最大页数，说明已加载完所有数据
        const maxPage = Math.ceil(totalCount / pageSize)
        if (currentPage >= maxPage) {
          hasMore.value = false
        }

        // 核心修复：加载完成后立即探测位置，确保如果处于底部则自动续接下一页
        checkAutoLoad();
      } else if (data.type === 'UPDATE_LIST') {
        // 收到更新列表通知，重新请求第一页
        searchParams.value.current = 1 // 重置页码
        chatListWebSocket.sendMessage({
          type: 'REQUEST_LIST',
          data: {
            searchText: searchText.value,
            current: searchParams.value.current,
            pageSize: searchParams.value.pageSize,
            chatType: undefined
          }
        })
      } else if (data.type === 'DELETE') {
        // 删除聊天记录
        const deletedChatId = data.data?.id
        if (deletedChatId) {
          chatList.value = chatList.value.filter(chat =>
            String(chat.id) !== String(deletedChatId) && chat.id !== '-1' && chat.id !== '-2'
          )
          allChatList.value = allChatList.value.filter(chat =>
            String(chat.id) !== String(deletedChatId) && chat.id !== '-1' && chat.id !== '-2'
          )
        }
      } else if (data.type === 'NEW') {
        // 添加新的聊天记录
        const newChat = data.data
        if (newChat) {
          // 确保不重复添加
          if (!allChatList.value.some(chat => String(chat.id) === String(newChat.id))) {
            allChatList.value = [
              ...allChatList.value.filter(chat => chat.id !== '-1' && chat.id !== '-2'),
              newChat
            ]
          }
        }
      } else if (data.type === 'UNREAD_COUNTS') {
        // 处理未读消息计数更新
        // 这里不需要做任何事情，因为unreadCounts是通过computed获取的，会自动更新
      } else if (data.type === 'UNREAD_COUNT_UPDATE') {
        // 处理单个聊天的未读消息计数更新
        const { chatId, unreadCount } = data
        const chat = allChatList.value.find(item => item.id === String(chatId))
        if (chat) {
          chat.userUnreadCount = Number(unreadCount)
        }
      } else if (data.type === 'ERROR') {
        // message.error(data.message || '操作失败')
        loading.value = false
        loadingMore.value = false
      }
    })

    chatListWebSocket.on('error', (error) => {
      console.error('WebSocket错误:', error)
      // message.error('聊天列表连接异常，请刷新页面重试')
      loading.value = false
    })

    // 初始请求数据
    fetchChatList()

    // 请求未读消息计数
    chatListWebSocket.sendMessage({
      type: 'REQUEST_UNREAD_COUNTS'
    })
  }
})
</script>

<style lang="scss" scoped>
.pc-chat-container {
  display: flex;
  height: 90vh;
  width: 100%;
  max-width: 1400px;
  margin: auto;
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-top: 6px;
  position: relative;

  .left-panel {
    min-width: 320px;
    max-width: 450px;
    border-right: 0.5px solid var(--ios-modal-divider);
    display: flex;
    flex-direction: column;
    background: var(--card-background);
    color: var(--text-primary);
    position: relative;
    z-index: 2;

    .chat-header {
      padding: 12px 16px;
      background: var(--ios-bg-blur);
      backdrop-filter: blur(20px);
      border-bottom: 0.5px solid var(--ios-header-border);
      position: sticky;
      top: 0;
      z-index: 10;

      .header-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .page-title {
          font-size: 24px;
          font-weight: 700;
          color: var(--text-primary);
          margin: 0;
          letter-spacing: -0.5px;
        }

        .clear-all-btn {
          width: 32px;
          height: 32px;
          border-radius: 10px;
          background: var(--ios-tab-bg);
          display: flex;
          align-items: center;
          justify-content: center;
          color: #007aff;
          font-size: 14px;
          cursor: pointer;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

          &:hover {
            background: var(--ios-search-focus-bg);
            transform: scale(1.05);
          }

          &:active {
            transform: scale(0.9);
          }
        }
      }

      .chat-tabs {
        display: flex;
        background: var(--ios-header-border);
        padding: 2px;
        border-radius: 10px;
        margin-bottom: 12px;

        .tab-item {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 6px;
          padding: 6px;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.2s ease;
          color: var(--ios-tab-text);

          .icon-wrap {
            font-size: 16px;
            position: relative;
            display: flex;
            align-items: center;
          }

          .tab-text {
            font-size: 13px;
            font-weight: 500;
          }

          &.active {
            background: var(--card-background);
            color: var(--text-primary);
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);

            .far, .fas {
              color: #007aff;
            }
          }

          &:not(.active):hover {
            background: rgba(0, 0, 0, 0.05);
          }
        }
      }

      .search-section {
        .search-box {
          position: relative;
          display: flex;
          align-items: center;
          background: var(--ios-search-bg);
          border-radius: 10px;
          padding: 6px 10px;
          transition: all 0.3s ease;

          &:focus-within {
            background: var(--ios-search-focus-bg);
          }

          .search-icon-left {
            color: var(--ios-tab-text);
            font-size: 14px;
            margin-right: 8px;
          }

          .search-input {
            flex: 1;
            border: none;
            background: transparent;
            color: var(--text-primary);
            font-size: 15px;
            outline: none;

            &::placeholder {
              color: var(--ios-tab-text);
            }
          }
        }
      }
    }


    .chat-list-container {
      flex: 1;
      overflow-y: auto;
      background: var(--background);

      .chat-list {
        background: transparent;
        padding: 12px 0;

        .chat-item-wrapper {
          position: relative;
          padding: 0 16px;
          transition: all 0.2s ease;

          .chat-item {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 12px;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
            position: relative;

            // Hairline separator
            &::after {
              content: '';
              position: absolute;
              bottom: 0px;
              right: 12px;
              left: 72px;
              height: 0.5px;
              background-color: var(--ios-modal-divider);
            }

            .chat-avatar {
              position: relative;
              flex-shrink: 0;

              img {
                width: 48px;
                height: 48px;
                border-radius: 10px;
                object-fit: cover;
                background: var(--ios-tab-bg);
              }

              .unread-badge {
                position: absolute;
                top: -6px;
                right: -6px;
                background: #ff3b30;
                color: #fff;
                font-size: 11px;
                font-weight: 600;
                min-width: 18px;
                height: 18px;
                border-radius: 9px;
                display: flex;
                align-items: center;
                justify-content: center;
                padding: 0 5px;
                border: 2px solid var(--card-background);
                box-shadow: 0 2px 4px rgba(255, 59, 48, 0.2);
                z-index: 2;
              }
            }

            .chat-content {
              flex: 1;
              min-width: 0;

              .chat-header {
                display: flex;
                justify-content: space-between;
                align-items: baseline;
                margin-bottom: 2px;
                padding: 0;
                background: transparent;
                border: none;

                .chat-name {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  flex: 1;
                  min-width: 0;

                  .name-text {
                    font-size: 16px;
                    font-weight: 600;
                    color: var(--text-primary);
                    letter-spacing: -0.3px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                  }
                }

                .chat-time {
                  font-size: 13px;
                  color: var(--text-secondary);
                  margin-left: 8px;
                  flex-shrink: 0;
                }
              }

              .chat-message {
                font-size: 14px;
                color: var(--text-secondary);
                line-height: 1.3;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                letter-spacing: -0.2px;
              }
            }

            .chat-tags {
              display: flex;
              gap: 4px;

              .friend-tag, .private-tag, .ai-tag, .group-tag {
                padding: 1px 6px;
                border-radius: 4px;
                font-size: 11px;
                font-weight: 500;
              }

              .friend-tag { color: #34c759; background: rgba(52, 199, 89, 0.1); }
              .private-tag { color: #007aff; background: rgba(0, 122, 255, 0.1); }
              .ai-tag { color: #5856d6; background: rgba(88, 86, 214, 0.1); }
              .group-tag { color: #af52de; background: rgba(175, 82, 222, 0.1); }
            }
          }

          &:hover .chat-item {
            background-color: var(--hover-background);
          }

          &.active .chat-item {
            background-color: var(--nav-item-active);
            &::after { display: none; }
          }

          &:last-child .chat-item::after { display: none; }

          &:active .chat-item {
            transform: scale(0.98);
          }
        }
      }

      .custom-pagination {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 12px 20px;
        border-top: 1px solid var(--border-color);
        background: var(--card-background);
        height: 52px;

        .pagination-left {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .pagination-right {
          display: flex;
          align-items: center;
          gap: 12px;
        }

        .pagination-size {
          display: flex;
          align-items: center;
          gap: 8px;

          label {
            color: var(--text-secondary);
            font-size: 14px;
          }

          select {
            padding: 4px 8px;
            border: 1px solid var(--border-color);
            border-radius: 6px;
            background: var(--card-background);
            color: var(--text-primary);
            outline: none;
            cursor: pointer;
            transition: all 0.3s;
            min-width: 90px;

            &:hover {
              border-color: #0066B3;
            }
          }
        }

        .pagination-info {
          color: var(--text-secondary);
          font-size: 14px;
          white-space: nowrap;
        }

        .pagination-buttons {
          display: flex;
          align-items: center;
          gap: 8px;

          .arrow-btn,
          .page-btn {
            min-width: 32px;
            height: 32px;
            padding: 0 8px;
            border: 1px solid var(--border-color);
            border-radius: 6px;
            background: var(--card-background);
            color: var(--text-primary);
            cursor: pointer;
            transition: all 0.3s;
            display: flex;
            align-items: center;
            justify-content: center;

            &:hover:not(:disabled) {
              color: #0066B3;
              border-color: #0066B3;
            }

            &:disabled {
              color: var(--text-secondary);
              cursor: not-allowed;
            }

            &.active {
              color: var(--card-background);
              background: #0066B3;
              border-color: #0066B3;
            }
          }

          .page-numbers {
            display: flex;
            gap: 4px;
          }
        }
      }
    }
  }

  .panel-resizer {
    width: 8px; /* 增加宽度以便于触摸 */
    background: transparent;
    position: relative;
    cursor: col-resize;
    transition: all 0.3s;
    margin: 0 -3px; /* 负边距使实际点击区域不影响布局 */
    z-index: 10; /* 确保在其他元素之上 */

    &::before {
      content: '';
      position: absolute;
      left: 3px;
      top: 0;
      width: 2px;
      height: 100%;
      background: var(--border-color);
      transition: all 0.3s;
    }

    &:hover::before,
    &:active::before {
      background: #0066B3;
      width: 2px;
    }

    // 拖动把手
    &::after {
      content: '⋮';
      position: absolute;
      left: 50%;
      top: 50%;
      transform: translate(-50%, -50%);
      color: var(--text-secondary);
      font-size: 16px;
      opacity: 0;
      transition: opacity 0.3s;
      pointer-events: none;
    }

    &:hover::after {
      opacity: 1;
    }

    /* 添加触摸设备的样式 */
    @media (hover: none) and (pointer: coarse) {
      &::before {
        width: 2px;
        background: rgba(0, 102, 179, 0.5);
      }

      &::after {
        opacity: 0.5;
      }
    }
  }

  .right-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    background: var(--background);
    position: relative;
    min-width: 0;

    .chat-header {
      padding: 16px 24px;
      background: var(--card-background);
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;

      .user-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .avatar {
          width: 48px;
          height: 48px;
          border-radius: 50%;
          cursor: pointer;
          transition: all 0.3s;
          border: 2px solid transparent;

          &:hover {
            border-color: #0066B3;
            transform: scale(1.05);
          }
        }

        .user-meta {
          display: flex;
          flex-direction: column;
          gap: 4px;

          .username {
            font-size: 16px;
            font-weight: 500;
            color: var(--text-primary);
          }

          .status-text {
            font-size: 12px;
            color: var(--text-secondary);
            display: flex;
            align-items: center;
            gap: 4px;

            &::before {
              content: '';
              display: inline-block;
              width: 6px;
              height: 6px;
              border-radius: 50%;
              background: #999;
            }

            &.online {
              color: #52c41a;

              &::before {
                background: #52c41a;
                box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.2);
              }
            }
          }
        }
      }

      .header-actions {
        display: flex;
        gap: 8px;

        .action-btn {
          color: var(--text-secondary);
          transition: all 0.3s;
          display: flex;
          align-items: center;
          gap: 4px;
          padding: 6px 10px;
          border: none;
          border-radius: 16px;
          font-size: 12px;
          cursor: pointer;
          background: #f5f5f5;

          &:hover {
            color: #0066B3;
            background: #cce6f5;
          }

          .online-count {
            font-size: 12px;
          }
        }

        .announcement-btn {
          background: #0066B3;
          color: #fff;

          &:hover {
            background: #005599;
          }
        }

        .online-users-btn {
          background: #1890ff;
          color: #fff;

          &:hover {
            background: #0c81f0;
          }
        }
      }
    }
  }

  .empty-chat {
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--background);

    .empty-state {
      text-align: center;
      padding: 32px;

      img {
        width: 50%;
        max-width: 300px;
        margin-bottom: 24px;
        opacity: 0.8;
        transition: all 0.3s;

        &:hover {
          opacity: 1;
          transform: scale(1.02);
        }
      }

      p {
        color: var(--text-secondary);
        font-size: 16px;
        margin: 0;
      }
    }
  }

  // 自定义滚动条样式
  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }

  ::-webkit-scrollbar-track {
    background: transparent;
    border-radius: 3px;
  }

  ::-webkit-scrollbar-thumb {
    background: rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    cursor: pointer;

    &:hover {
      background: rgba(0, 0, 0, 0.2);
    }
  }

  .dark-theme & {
    ::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.1);

      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }
  }

  .chat-list {
    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.1);
      border-radius: 2px;

      &:hover {
        background: rgba(0, 0, 0, 0.2);
      }
    }
  }

  .chat-messages {
    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.1);
      border-radius: 2px;

      &:hover {
        background: rgba(0, 0, 0, 0.2);
      }
    }
  }
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .username-text {
    color: var(--text-primary);
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.chat-desc {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;

  .last-message {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #666;
    font-size: 13px;
  }

  .message-time {
    font-size: 12px;
    color: #999;
  }
}

.friend-tag,
.private-tag,
.ai-tag,
.sender-tag {
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 12px;
}

.friend-tag {
  color: #52c41a;
  background: #f6ffed;
}

.private-tag {
  color: #1890ff;
  background: #e6f7ff;
}

.ai-tag {
  color: #722ed1;
  background: #f9f0ff;
}

.sender-tag {
  color: #0066B3;
  background: #cce6f5;
}

.group-tag {
  color: #0066B3;
  background: #cce6f5;
  padding: 0 4px;
  border-radius: 12px;
}

.pagination {
  padding: 12px;
  text-align: center;
  border-top: 1px solid #e8e8e8;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 32px 16px;
  text-align: center;

  .empty-icon {
    width: 100%;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  h3 {
    margin: 0 0 8px;
    color: #666;
  }

  p {
    margin: 0;
    color: #999;
  }
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--background);
  position: relative;
  min-width: 0;

  .chat-header {
    padding: 16px 24px;
    background: var(--card-background);
    border-bottom: 1px solid var(--border-color);
    display: flex;
    justify-content: space-between;
    align-items: center;

    .user-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .avatar {
        width: 48px;
        height: 48px;
        border-radius: 50%;
        cursor: pointer;
        transition: all 0.3s;
        border: 2px solid transparent;

        &:hover {
          border-color: #0066B3;
          transform: scale(1.05);
        }
      }

      .user-meta {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .username {
          font-size: 16px;
          font-weight: 500;
          color: var(--text-primary);
        }

        .status-text {
          font-size: 12px;
          color: var(--text-secondary);
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .header-actions {
      display: flex;
      gap: 8px;

      .action-btn {
        color: var(--text-secondary);
        transition: all 0.3s;

        &:hover {
          color: #0066B3;
        }
      }
    }
  }
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--background);

  .empty-state {
    text-align: center;
    padding: 32px;

    p {
      color: var(--text-secondary);
      font-size: 16px;
      margin: 0;
    }
  }
}

.group-status {
  font-size: 12px;
  color: #0066B3;
  background: #cce6f5;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.clear-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(82, 196, 26, 0.1);
  color: #52c41a;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-left: 16px;
  border: 1px solid transparent;

  &:hover {
    background: rgba(82, 196, 26, 0.2);
    border-color: #52c41a;
    transform: rotate(15deg);
  }

  &:active {
    transform: scale(0.95) rotate(15deg);
  }

  .anticon {
    font-size: 16px;
  }
}

/* 清除未读消息弹框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

.modal-content {
  background: var(--card-background);
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  position: relative;
  text-align: center;
  animation: slideUp 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.clear-unread-modal {
  .modal-icon.clear {
    background: rgba(82, 196, 26, 0.1);
    color: #52c41a;
    width: 56px;
    height: 56px;
    border-radius: 28px;
    font-size: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 20px;
    animation: rotateIn 0.4s ease;
  }

  h3 {
    font-size: 18px;
    color: var(--text-primary);
    margin: 0 0 16px;
  }

  .unread-count {
    font-size: 15px;
    color: var(--text-secondary);
    margin: 16px 0 24px;
    line-height: 1.6;

    .count {
      color: #52c41a;
      font-weight: 600;
      font-size: 18px;
    }

    .detail {
      display: block;
      margin-top: 8px;
      font-size: 14px;
      opacity: 0.8;
    }
  }

  .modal-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    margin-top: 24px;

    .modal-btn {
      min-width: 100px;
      height: 36px;
      border-radius: 18px;
      font-size: 14px;
      border: none;
      cursor: pointer;
      transition: all 0.3s ease;
      display: flex;
      align-items: center;
      justify-content: center;

      &.cancel {
        background: var(--hover-background);
        color: var(--text-secondary);

        &:hover {
          background: var(--hover-background-darker);
        }
      }

      &.confirm {
        background: #52c41a;
        color: white;

        &:hover:not(:disabled) {
          background: #73d13d;
        }

        &:disabled {
          background: #d9d9d9;
          cursor: not-allowed;
        }
      }
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
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

@keyframes rotateIn {
  from {
    transform: rotate(-180deg) scale(0.5);
    opacity: 0;
  }
  to {
    transform: rotate(0) scale(1);
    opacity: 1;
  }
}

.loading-dots {
  position: relative;
  &::after {
    content: '';
    animation: loadingDots 1.5s infinite;
  }
}

@keyframes loadingDots {
  0% { content: ''; }
  25% { content: '.'; }
  50% { content: '..'; }
  75% { content: '...'; }
  100% { content: ''; }
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
  background: var(--card-background);
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
  animation: scaleIn 0.2s ease;
  color: var(--text-primary);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
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
  background: var(--hover-background);
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: var(--text-secondary);

  &:hover {
    background: var(--hover-background-darker);
    color: var(--text-primary);
  }

  &:active {
    transform: scale(0.9);
  }
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
  color: var(--text-secondary);
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

  &:hover {
    background: var(--hover-background);
  }

  &:active {
    background: var(--hover-background-darker);
  }
}

.user-avatar {
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
  border: 1px solid var(--border-color);
}

.user-avatar .online-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #52c41a;
  border: 1px solid var(--card-background);
}

.user-meta {
  flex: 1;
  min-width: 0;
}

.user-meta .user-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.user-meta .user-role {
  font-size: 11px;
  color: var(--text-secondary);
}

/* 公告弹框 */
.announcement-modal .announcement-content {
  padding: 12px 0;
  color: var(--text-primary);
}

.announcement-modal .announcement-content p {
  margin-bottom: 12px;
  color: var(--text-secondary);
}

.announcement-modal .announcement-content ul {
  margin: 0;
  padding-left: 20px;
}

.announcement-modal .announcement-content ul li {
  margin-bottom: 8px;
  color: var(--text-secondary);
  line-height: 1.5;
}

/* 动画 */
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

.load-more-container {
  padding: 12px 20px;
  text-align: center;
  border-top: 1px solid var(--border-color);
  background: var(--card-background);

  .loading-more,
  .load-more-trigger,
  .no-more-data {
    padding: 8px;
    color: var(--text-secondary);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      color: #0066B3;
    }
  }

  .load-more-trigger {
    color: #0066B3;
    font-weight: 500;

    &:hover {
      background: #f0f7ff;
      border-radius: 4px;
    }
  }

  .no-more-data {
    color: #999;
    font-style: italic;
  }
}
</style>

