<template>
  <div id="chatListPage">
    <div class="main-content" :class="{ 'pc-main-content': device === DEVICE_TYPE_ENUM.PC }">

      <!-- 顶部导航栏和搜索框 -->
      <div id="chatListHeader" v-if="device !== DEVICE_TYPE_ENUM.PC">
        <!-- 标签栏 -->
        <div class="chat-tabs">
          <!-- 聊天选项 -->
          <div
            class="tab-item"
            :class="{ active: activeTab === 'all', 'scale-down': activeTab !== 'all' }"
            @click="handleTabChange('all')"
          >
            <div class="icon-wrap">
              <i class="far fa-comment icon"></i>
              <Badge v-if="unreadCounts.totalUnread > 0" :count="unreadCounts.totalUnread" :overflowCount="99" class="tab-badge" />
            </div>
            <div class="tab-text">聊天</div>
          </div>
          <!-- 部落选项 -->
          <div
            class="tab-item"
            :class="{ active: activeTab === 'tribe', 'scale-down': activeTab !== 'tribe' }"
            @click="handleTabChange('tribe')"
          >
            <div class="icon-wrap">
              <i class="fas fa-users icon"></i>
            </div>
            <div class="tab-text">部落</div>
          </div>
          <!-- 发现选项 -->
          <div
            class="tab-item"
            :class="{ active: activeTab === 'more', 'scale-down': activeTab !== 'more' }"
            @click="handleTabChange('more')"
          >
            <div class="icon-wrap">
              <i class="fas fa-search icon"></i>
            </div>
            <div class="tab-text">发现</div>
          </div>
        </div>

        <!-- 搜索栏 -->
        <div class="search-container" v-if="activeTab === 'all'">
          <div class="search-box">
            <input
              type="text"
              v-model="searchText"
              placeholder="搜索联系人"
              @keyup.enter="handleSearch"
              class="search-input"
            />
            <button class="search-button" @click="handleSearch" :disabled="loading">
              <i class="fas fa-search search-icon"></i>
            </button>
          </div>
          <!-- 添加清除所有未读按钮 -->
          <div
            v-if="unreadCounts.totalUnread > 0"
            class="clear-all-btn"
            @click.stop="showClearUnreadModal"
          >
            <i class="fas fa-broom"></i>
          </div>
        </div>
      </div>

      <!-- 聊天列表 -->
      <div class="chat-list-wrapper">
        <!-- 部落标签内容 - 推荐部落列表和公共部落列表 -->
        <div v-if="activeTab === 'tribe'" class="tribe-content" @scroll="handleTribeScroll" ref="tribeListRef">
          <!-- 推荐部落 -->
          <div class="recommended-header">
            <h3 class="recommended-title">推荐部落</h3>
          </div>
          <div v-if="recommendedSpaces.length > 0" class="recommended-spaces-list">
            <TeamSpaceCard
              v-for="space in recommendedSpaces"
              :key="space.id"
              :space="space"
            />
          </div>
          <div v-else-if="loadingRecommended" class="loading-state">
            <div class="loading-item" v-for="i in 3" :key="i">
              <div class="skeleton-card">
                <div class="skeleton-cover"></div>
                <div class="skeleton-content">
                  <div class="skeleton-title"></div>
                  <div class="skeleton-desc"></div>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" class="empty-svg">
                <path fill="#2563eb" d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z" />
              </svg>
            </div>
            <h3>暂无推荐部落</h3>
            <p>暂时没有为您推荐的部落，稍后再来看看吧</p>
          </div>

          <!-- 公共部落 -->
          <div class="public-spaces-header">
            <h3 class="public-spaces-title">所有部落</h3>
          </div>
          <div v-if="publicSpaces.length > 0" class="public-spaces-list">
            <TeamSpaceCard
              v-for="space in publicSpaces"
              :key="`public-${space.id}`"
              :space="space"
            />

            <!-- 加载更多按钮 -->
            <div v-if="publicHasMore" class="load-more-container">
              <button class="load-more-btn" @click="loadMorePublicSpaces" :disabled="loadingPublic">
                <span v-if="loadingPublic">加载中...</span>
                <span v-else>加载更多</span>
              </button>
            </div>
            <div v-else-if="publicSpaces.length > 0" class="list-bottom-tip">
              <div class="tip-line">
                <span class="line"></span>
                <span class="text">已经到底啦</span>
                <span class="line"></span>
              </div>
            </div>
          </div>
          <div v-else-if="loadingPublic" class="loading-state">
            <div class="loading-item" v-for="i in 3" :key="`loading-${i}`">
              <div class="skeleton-card">
                <div class="skeleton-cover"></div>
                <div class="skeleton-content">
                  <div class="skeleton-title"></div>
                  <div class="skeleton-desc"></div>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="isPublicSpacesLoaded && publicSpaces.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" class="empty-svg">
                <path fill="#2563eb" d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z" />
              </svg>
            </div>
            <h3>暂无公共部落</h3>
            <p>暂时没有公开的团队部落</p>
          </div>
        </div>

        <!-- 发现标签内容 -->
        <div v-if="activeTab === 'more'" class="more-content">
          <div class="discovery-actions">
            <!-- 树洞 -->
            <div class="action-item" @click="$router.push('/barrage')">
              <div class="action-icon tree-hole">
                <i class="far fa-comment-dots"></i>
              </div>
              <div class="action-label">树洞</div>
            </div>
            <!-- 友链 -->
            <div class="action-item" @click="$router.push('/friend-links')">
              <div class="action-icon friend-link">
                <i class="fas fa-link"></i>
              </div>
              <div class="action-label">友链</div>
            </div>
            <!-- 恋爱广场 -->
            <div class="action-item" @click="$router.push('/loveboard/list')">
              <div class="action-icon love-square">
                <i class="fas fa-heart"></i>
              </div>
              <div class="action-label">恋爱广场</div>
            </div>
            <!-- 实用工具 -->
            <div class="action-item" @click="$router.push('/tools')">
              <div class="action-icon tools">
                <i class="fas fa-tools"></i>
              </div>
              <div class="action-label">实用工具</div>
            </div>
            <!-- 小游戏 -->
            <div class="action-item" @click="$router.push('/games')">
              <div class="action-icon games">
                <i class="fas fa-gamepad"></i>
              </div>
              <div class="action-label">小游戏</div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else-if="!loading && filteredChatList.length === 0" class="empty-state">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" class="empty-svg">
              <path fill="#2563eb" d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z" />
            </svg>
          </div>
          <h3>{{ getEmptyText() }}</h3>
          <p v-if="searchText">换个关键词试试吧</p>
          <!-- 未登录时显示提示 -->
          <p v-if="!loginUserStore.loginUser || !loginUserStore.loginUser.id">请先登录以查看聊天列表</p>
        </div>

        <!-- 聊天列表 -->
        <div v-else class="chat-list" @scroll="handleListScroll" ref="mobileListRef">
          <!-- 骨架屏加载效果 -->
          <div v-if="loading && filteredChatList.length === 0" class="skeleton-list">
            <div class="skeleton-item" v-for="i in 5" :key="`skeleton-${i}`">
              <div class="skeleton-avatar"></div>
              <div class="skeleton-content">
                <div class="skeleton-line name"></div>
                <div class="skeleton-line message"></div>
              </div>
            </div>
          </div>

          <!-- 实际聊天列表 -->
          <div
            v-else
            v-for="item in filteredChatList"
            :key="item.id"
            class="chat-item-wrapper"
            :class="{ 'actions-visible': item.showActions }"
          >
            <div
              class="chat-item"
              :class="{ unread: getUnreadCount(item) > 0 }"
              @click="handleChatClick(item)"
              @touchstart="handleTouchStart($event, item)"
              @touchmove="handleTouchMove($event, item)"
              @touchend="handleTouchEnd(item)"
            >
              <div class="chat-avatar">
                <img
                  :src="item.targetUser?.userAvatar || getDefaultAvatar(item.targetUser?.userName)"
                  :alt="item.targetUser?.userName"
                />
                <span v-if="getUnreadCount(item) > 0" class="unread-badge">
                  {{ getUnreadCount(item) > 99 ? '99+' : getUnreadCount(item) }}
                </span>
              </div>

              <div class="chat-content">
                <div class="chat-header">
                  <div class="chat-name">
                    {{ item.isSender ? item.userChatName : item.targetUserChatName }}
                    <div class="chat-tags">
                      <span :class="getChatTypeClass(item)">
                        <i v-if="item.chatType === 2" class="fas fa-robot"></i>
                        <i v-else-if="item.chatType === 1" class="fas fa-user"></i>
                        <i v-else class="fas fa-comment"></i>
                      </span>
                      <span v-if="item.isSender" class="sender-tag">
                        <i class="fas fa-paper-plane"></i>
                      </span>
                    </div>
                  </div>
                  <div class="chat-time">{{ formatMessageTime(item.lastMessageTime) }}</div>
                </div>
                <div class="chat-message">{{ item.lastMessage || '暂无消息' }}</div>
              </div>
            </div>
            <div class="slide-actions">
              <button class="action-btn edit" @click.stop="showEditNameModal(item)">
                <i class="fas fa-pen"></i>
                <span>编辑</span>
              </button>
              <button class="action-btn delete" @click.stop="showDeleteConfirm(item)">
                <i class="fas fa-trash-alt"></i>
                <span>删除</span>
              </button>
            </div>
          </div>
          <!-- 在聊天列表的底部添加提示 -->
          <div v-if="!loading && filteredChatList.length > 0" class="list-bottom-tip">
            <div class="tip-line">
              <span class="line"></span>
              <span class="text">已经到底啦</span>
              <span class="line"></span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="deleteConfirmVisible" class="modal-overlay" @click="deleteConfirmVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-inner">
          <h3>确认删除该聊天？</h3>
          <p>
            用户名称：{{ selectedChat?.targetUser?.userName || '未设置' }}<br>
            聊天类型：{{ selectedChat?.chatType === 1 ? '好友' : '私信' }}
          </p>
        </div>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="deleteConfirmVisible = false">取消</button>
          <button class="modal-btn confirm danger" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 修改名称弹窗 -->
    <div v-if="editNameVisible" class="modal-overlay" @click="editNameVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-inner">
          <h3>修改聊天名称</h3>
          <input
            v-model="newChatName"
            type="text"
            placeholder="请输入新的聊天名称"
            maxlength="50"
            class="edit-input"
          />
          <div class="input-counter">{{ newChatName.length }}/50</div>
        </div>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="editNameVisible = false">取消</button>
          <button
            class="modal-btn confirm"
            :disabled="editNameLoading"
            @click="handleEditNameConfirm"
          >
            {{ editNameLoading ? '确认中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 底部加载状态 -->
    <div v-if="loading" class="loading-more">
      <i class="fas fa-spinner fa-spin loading-icon"></i>
      <span>加载中...</span>
    </div>

    <!-- 清除未读确认弹窗 -->
    <div v-if="clearUnreadVisible" class="modal-overlay" @click="clearUnreadVisible = false">
      <div class="modal-content" @click.stop>
        <div class="modal-inner">
          <h3>清除所有未读消息</h3>
          <p>
            当前共有 {{ unreadCounts.totalUnread }} 条未读消息
          </p>
        </div>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="clearUnreadVisible = false">取消</button>
          <button
            class="modal-btn confirm danger"
            :disabled="clearUnreadLoading"
            @click="confirmClearUnread"
          >
            {{ clearUnreadLoading ? '清除中...' : '确认' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, reactive, onBeforeUnmount, nextTick, onActivated, onDeactivated, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, notification, Badge } from 'ant-design-vue'
import { EllipsisOutlined, DeleteOutlined, ExclamationCircleOutlined, ExclamationCircleFilled, EditOutlined, UserOutlined, MessageOutlined, RobotOutlined, SendOutlined, EditFilled, DeleteFilled, ClearOutlined, LinkOutlined, HeartFilled, ToolOutlined, PlaySquareOutlined, TeamOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { clearUnreadCountUsingPost } from '@/api/privateChatController'
import TeamSpaceCard from '@/components/TeamSpaceCard.vue'
import { deletePrivateChatUsingPost, updateChatNameUsingPost } from '@/api/privateChatController'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { formatMessageTime } from "@/utils/dateUtils"
import { getDefaultAvatar } from '@/utils/userUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useChatListStore } from '@/stores/useChatListStore'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import { chatListWebSocket } from '@/utils/chatListWebSocket'
import { debounce } from 'lodash-es'
import '@lottiefiles/lottie-player'
import { listRecommendedSpacesUsingGet } from '@/api/spaceController'
import PrivateChat = API.PrivateChat

// 设备类型
const device = ref<string>('')
const isMobile = computed(() => window.innerWidth <= 768)

// 状态变量
const loading = ref(false)
const isTop = ref(true)
const searchText = ref('')
const activeTab = ref('all')
const chatList = ref<PrivateChat[]>([])
const selectedChat = ref<PrivateChat | null>(null)
const deleteConfirmVisible = ref(false)
const editNameVisible = ref(false)
const editNameLoading = ref(false)
const newChatName = ref('')
const hasMore = ref(true)
const total = ref(0)


const clearUnreadVisible = ref(false)
const clearUnreadLoading = ref(false)

// 使用WebSocket的未读消息总数
const unreadCounts = computed(() => chatListWebSocket.unreadCounts.value)

// 移动端分页相关变量
const mobileCurrentPage = ref(1)
const isLoadingMore = ref(false)


// 在 setup 中添加以下变量和方法
const touchStartX = ref(0)
const touchStartY = ref(0)
const minSwipeDistance = 50 // 最小滑动距离
const maxSwipeTime = 300 // 最大滑动时间
const touchStartTime = ref(0)

const handleTouchStart = (e: TouchEvent, item: any) => {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
  touchStartTime.value = Date.now()
  // 重置其他项的展开状态
  chatList.value.forEach(chat => {
    if (chat !== item) {
      chat.showActions = false
    }
  })
}

const handleTouchMove = (e: TouchEvent, item: any) =>
{
  if (!touchStartX.value) return

  const touchX = e.touches[0].clientX
  const touchY = e.touches[0].clientY
  const deltaX = touchStartX.value - touchX
  const deltaY = Math.abs(touchY - touchStartY.value)

  // 如果垂直滑动距离大于水平滑动距离的一半，则不处理
  if (deltaY > Math.abs(deltaX) / 2) return

  // 阻止页面滚动
  e.preventDefault()

  // 限制最大滑动距离
  const maxSlideDistance = 100
  Math.min(Math.max(deltaX, 0), maxSlideDistance)

  // 应用滑动距离
  if (item.showActions && deltaX < 0) {
    item.showActions = false
  } else if (!item.showActions && deltaX > minSwipeDistance) {
    item.showActions = true
  }
}


const handleTouchEnd = (item: any) => {
  const touchEndTime = Date.now()
  const touchDuration = touchEndTime - touchStartTime.value

  // 重置触摸起始位置
  touchStartX.value = 0
  touchStartY.value = 0

  // 如果滑动时间过长，重置状态
  if (touchDuration > maxSwipeTime) {
    item.showActions = false
  }
}

// 获取未读消息数
const getUnreadCount = (chat: PrivateChat) => {
  // 如果当前登录用户是发起者，返回userUnreadCount
  if (chat.userId === loginUserStore.loginUser?.id) {
    return chat.userUnreadCount || 0
  }
  // 如果当前登录用户是目标用户，返回targetUserUnreadCount
  return chat.userUnreadCount || 0
}

// 优化: 列表过滤和排序
const filteredChatList = computed(() => {
  // 检查用户是否已登录，未登录时不显示任何聊天项（包括AI助手和公共群）
  if (!loginUserStore.loginUser || !loginUserStore.loginUser.id) {
    return []
  }

  // 始终包含AI助手和公共群
  let specialChats = [deepseekChat, publicGroupChat]
  let filtered = chatList.value.filter(chat => chat.id !== '-1' && chat.id !== '-2')

  // 搜索过滤
  if (searchText.value) {
    const searchLower = searchText.value.toLowerCase()
    filtered = filtered.filter(chat => {
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
  filtered.sort((a, b) =>
    new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime()
  )

  // 合并特殊聊天和普通聊天
  return [...specialChats, ...filtered]
})

// 优化: 分页处理
computed(() => {
  if (isMobile.value) {
    return filteredChatList.value
  }
  const start = (current.value - 1) * pageSize.value
  return filteredChatList.value.slice(start, start + pageSize.value)
})
// 获取聊天列表store
const chatListStore = useChatListStore()

// 聊天列表容器ref
const mobileListRef = ref<HTMLElement | null>(null)
const pcListRef = ref<HTMLElement | null>(null)
const tribeListRef = ref<HTMLElement | null>(null)

// 初始化
onMounted(async () => {
  device.value = await getDeviceType()

  // 添加点击事件监听
  document.addEventListener('click', handleClickOutside)

  // 初始化 WebSocket 连接
  if (loginUserStore.loginUser?.id) {
    await chatListWebSocket.connect(loginUserStore.loginUser.id)

    // 添加 WebSocket 事件监听
    chatListWebSocket.on('message', (data: any) => {
      if (!data) return

      // 停止加载状态
      loading.value = false
      isLoadingMore.value = false

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

        if (device.value === DEVICE_TYPE_ENUM.PC) {
          // PC端直接替换整个列表，但过滤掉特殊聊天
          chatList.value = records.filter(chat => chat.id !== '-1' && chat.id !== '-2')
        } else {
          // 移动端处理分页
          if (mobileCurrentPage.value === 1) {
            // 第一页，替换整个列表，但过滤掉特殊聊天
            chatList.value = records.filter(chat => chat.id !== '-1' && chat.id !== '-2')
          } else {
            // 追加数据，确保不重复且不包含特殊聊天
            const newRecords = records.filter(record =>
              record.id !== '-1' &&
              record.id !== '-2' &&
              !chatList.value.some(chat => String(chat.id) === String(record.id))
            )
            chatList.value = [...chatList.value, ...newRecords]
          }
        }

        total.value = filteredChatList.value.length // 使用实际显示的聊天列表长度
        hasMore.value = (mobileCurrentPage.value * 20) < totalCount
      } else if (data.type === 'UPDATE_LIST') {
        // 收到更新列表通知，重新请求第一页
        mobileCurrentPage.value = 1 // 重置页码
        chatListWebSocket.sendMessage({
          type: 'REQUEST_LIST',
          data: {
            searchText: searchText.value,
            current: 1,
            pageSize: 20,
            chatType: undefined
          }
        })
      } else if (data.type === 'DELETE') {
        // 删除聊天记录
        const deletedChatId = data.data?.id
        if (deletedChatId) {
          chatList.value = chatList.value.filter(chat =>
            chat.id !== deletedChatId && chat.id !== -1 && chat.id !== -2
          )
        }
      } else if (data.type === 'NEW') {
        // 添加新的聊天记录
        const newChat = data.data
        if (newChat) {
          // 确保不重复添加
          if (!chatList.value.some(chat => chat.id === newChat.id)) {
            chatList.value = [
              ...chatList.value.filter(chat => chat.id !== -1 && chat.id !== -2),
              newChat
            ]
            // 更新总数
            total.value = chatList.value.length + 2 // +2 是为特殊聊天预留的
          }
        }
      } else if (data.type === 'ERROR') {
        // message.error(data.message || '操作失败')
      }
    })

    // 初始请求未读消息总数
    chatListWebSocket.sendMessage({
      type: 'REQUEST_UNREAD_COUNTS'
    })

    chatListWebSocket.on('error', (error) => {
      console.error('WebSocket错误:', error)
      // message.error('聊天列表连接异常，请刷新页面重试')
      loading.value = false
      isLoadingMore.value = false
    })

    // 初始请求数据
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      chatListWebSocket.sendMessage({
        type: 'REQUEST_LIST',
        data: {
          searchText: searchText.value,
          current: pcsearchParams.current,
          pageSize: pcsearchParams.pageSize,
          chatType: undefined
        }
      })
    } else {
      chatListWebSocket.sendMessage({
        type: 'REQUEST_LIST',
        data: {
          searchText: searchText.value,
          current: mobileCurrentPage.value,
          pageSize: 20,
          chatType: undefined
        }
      })
    }
  }
})

// 初始化
onMounted(async () => {
  device.value = await getDeviceType()

  // 恢复之前的状态
  if (chatListStore.chatListData.length > 0 &&
    chatListStore.userId === loginUserStore.loginUser?.id) { // 检查用户ID是否匹配
    // 恢复数据
    chatList.value = chatListStore.chatListData
    // 恢复标签
    activeTab.value = chatListStore.currentTab
    // 恢复搜索文本
    searchText.value = chatListStore.searchText
    // 恢复页码
    if (device.value === DEVICE_TYPE_ENUM.PC) {
      pcsearchParams.current = chatListStore.currentPage
    } else {
      mobileCurrentPage.value = chatListStore.currentPage
    }

    // 等待DOM更新后恢复滚动位置
    await nextTick()
    if (device.value === DEVICE_TYPE_ENUM.PC && pcListRef.value) {
      pcListRef.value.scrollTop = chatListStore.scrollPosition
    } else if (mobileListRef.value) {
      mobileListRef.value.scrollTop = chatListStore.scrollPosition
    }
  } else {
    // 首次加载或数据已清空或用户不匹配，重新获取数据
    await (isMobile.value ? fetchChatList(true) : pcfetchChatList(true))
  }

})

onBeforeUnmount(() => {

  // 保存用户ID
  chatListStore.updateUserId(loginUserStore.loginUser?.id)

  // 保存滚动位置
  if (device.value === DEVICE_TYPE_ENUM.PC && pcListRef.value) {
    chatListStore.updateScrollPosition(pcListRef.value.scrollTop)
  } else if (mobileListRef.value) {
    chatListStore.updateScrollPosition(mobileListRef.value.scrollTop)
  }

  // 保存当前页码
  chatListStore.updateCurrentPage(
    device.value === DEVICE_TYPE_ENUM.PC
      ? pcsearchParams.current
      : mobileCurrentPage.value
  )

  // 保存数据
  chatListStore.updateChatListData(chatList.value)
  // 保存当前标签
  chatListStore.updateCurrentTab(activeTab.value)
  // 保存搜索文本
  chatListStore.updateSearchText(searchText.value)



  // 移除点击事件监听
  document.removeEventListener('click', handleClickOutside)
})

// 监听标签变化
watch(activeTab, (newTab) => {
  // 切换到部落标签时，重新获取部落数据
  if (newTab === 'tribe') {
    goToTribes();
    getPublicSpaces(true);
  } else {
    // 切换到非部落标签时重新获取聊天列表
    fetchChatList(true)
    pcfetchChatList(true)
  }
})

// 监听搜索文本变化
watch(searchText, () => {
  // 搜索时清除store中的数据，强制重新加载
  chatListStore.clearAll()
})

const loginUserStore = useLoginUserStore()
const router = useRouter()
const current = ref(1)
const pageSize = ref(10)

// 推荐部落相关数据
const recommendedSpaces = ref<API.SpaceVO[]>([])
const loadingRecommended = ref(false)

// 公共部落相关数据
const publicSpaces = ref<API.SpaceVO[]>([])
const loadingPublic = ref(false)
const publicCurrentPage = ref(1)
const publicHasMore = ref(true)
const publicTotal = ref(0)
const isPublicSpacesLoaded = ref(false)

// 根据当前标签切换内容
const handleTabChange = (tab: string) => {
  activeTab.value = tab
  // 根据标签类型执行不同操作
  if (tab === 'tribe') {
    // 如果切换到部落标签，调用获取推荐部落和公共部落的方法
    if (recommendedSpaces.value.length === 0 && !loadingRecommended.value) {
      goToTribes();
    }
    // 如果还没有加载公共部落，加载它们
    if (!isPublicSpacesLoaded.value && !loadingPublic.value) {
      getPublicSpaces(true);
    }
  } else if (tab !== 'tribe') {
    // 只有在切换到'聊天'或'发现'标签时才获取聊天列表
    fetchChatList(true)
    pcfetchChatList(true)
  }
}


const pcsearchParams = reactive({
  current: 1,
  pageSize: 6 ,
  searchText: '',
  chatType: undefined
})

const deepseekChat = reactive({
  id: -1, // Special ID for YuemuAI chat
  targetUser: {
    id: -1,
    userName: '悦木助手',
    userAvatar: new URL('@/assets/images/ai.png', import.meta.url).href,
    userAccount: 'yuemuai',
    createTime: new Date().toISOString()
  },
  chatType: 2, // New type for AI chat
  lastMessage: '你好!我是悦木助手，一个智能AI助手',
  lastMessageTime: new Date().toISOString(),
  targetUserChatName: '悦木助手',
  isSender: false,
  userUnreadCount: 0
})

// 添加全员群配置
const publicGroupChat = reactive({
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
})

// 修改获取聊天列表的方法
const fetchChatList = async (isRefresh = false) => {
  if (loading.value) return
  try {
    loading.value = true
    if (isRefresh) {
      chatList.value = []
      if (device.value !== DEVICE_TYPE_ENUM.PC) {
        mobileCurrentPage.value = 1
      }
    }

    // 使用WebSocket请求聊天列表
    chatListWebSocket.sendMessage({
      type: 'REQUEST_LIST',
      data: {
        searchText: searchText.value,
        current: device.value === DEVICE_TYPE_ENUM.PC ? pcsearchParams.current : mobileCurrentPage.value,
        pageSize: device.value === DEVICE_TYPE_ENUM.PC ? pcsearchParams.pageSize : 20,
        chatType: undefined
      }
    })

  } catch (error: any) {
    // message.error('获取聊天列表失败：' + error.message)
  } finally {
    loading.value = false
    isLoadingMore.value = false
  }
}

// 修改PC端获取聊天列表的方法
const pcfetchChatList = async (isRefresh = false) => {
  if (loading.value) return
  try {
    loading.value = true
    if (isRefresh) {
      chatList.value = []
    }

    // 使用WebSocket请求聊天列表
    chatListWebSocket.sendMessage({
      type: 'CHAT_LIST',
      data: {
        searchText: searchText.value,
        current: pcsearchParams.current,
        pageSize: pcsearchParams.pageSize,
        chatType: undefined
      }
    })

  } catch (error: any) {
    // message.error('获取聊天列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 修改滚动处理函数，添加防抖
const handleListScroll = debounce(async (e: Event) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return

  const target = e.target as HTMLElement
  const { scrollHeight, scrollTop, clientHeight } = target

  // 更新顶部状态
  isTop.value = scrollTop <= 0

  // 保存滚动位置
  chatListStore.updateScrollPosition(scrollTop)

  const threshold = 50 // 距底部多少像素时触发加载

  // 修改触底判断逻辑
  if (!loading.value &&
    !isLoadingMore.value &&
    mobileCurrentPage.value * 20 < total.value && // 使用实际页码和页大小计算
    scrollHeight - scrollTop - clientHeight <= threshold) {
    isLoadingMore.value = true
    mobileCurrentPage.value++

    // 使用WebSocket请求更多数据
    chatListWebSocket.sendMessage({
      type: 'REQUEST_LIST',
      data: {
        current: mobileCurrentPage.value,
        pageSize: 20,
        searchText: searchText.value,
        chatType: undefined
      }
    })
  }
}, 200) // 200ms 的防抖延迟

// 处理部落列表滚动
const handleTribeScroll = debounce(async (e: Event) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return

  const target = e.target as HTMLElement
  const { scrollHeight, scrollTop, clientHeight } = target

  const threshold = 50 // 距底部多少像素时触发加载

  // 检查是否滚动到底部且当前标签是部落
  if (activeTab.value === 'tribe' &&
    !loadingPublic.value &&
    publicHasMore.value &&
    scrollHeight - scrollTop - clientHeight <= threshold) {

    await loadMorePublicSpaces()
  }
}, 200) // 200ms 的防抖延迟


// 处理搜索
const handleSearch = () => {
  fetchChatList(true)
  pcfetchChatList(true)
}

// 点击聊天项
const handleChatClick = async (chat: PrivateChat) => {
  if (chat.id === -1) {
    // AI chat - 跳转到AI聊天页面
    router.push('/chat/ai')
  } else if (chat.id === -2) {
    // 群聊
    router.push({
      path: `/chat/${String(chat.targetUser.id)}`,
      query: {
        privateChatId: String(chat.id),
        userName: chat.targetUserChatName,
        userAvatar: chat.targetUser.userAvatar,
        userAccount: chat.targetUser.userAccount,
        createTime: String(chat.targetUser.createTime),
        spaceId: '-2',
        type: 'group'
      }
    })
  } else if (chat.targetUser) {
    // Normal chat
    try {
      // 清除未读消息
      await clearUnreadCountUsingPost({
        targetUserId: String(chat.targetUser.id),
        isSender: chat.isSender
      })

      // 更新本地未读数
      chat.userUnreadCount = 0


    } catch (error) {
      console.error('清除未读消息失败:', error)
    }

    router.push({
      path: `/chat/${String(chat.targetUser.id)}`,
      query: {
        privateChatId: String(chat.id),
        userName: chat.isSender ? chat.userChatName : chat.targetUserChatName,
        userAvatar: chat.targetUser.userAvatar,
        userAccount: chat.targetUser.userAccount,
        createTime: String(chat.targetUser.createTime),
        isSender: String(chat.isSender)
      }
    })
  }
}


// 显示删除确认框
const showDeleteConfirm = (chat: PrivateChat) => {
  selectedChat.value = chat
  deleteConfirmVisible.value = true
}

// 确认删除
const confirmDelete = async () => {
  if (!selectedChat.value?.id) return

  try {
    const res = await deletePrivateChatUsingPost({
      privateChatId: selectedChat.value.id
    })
    if (res.data.code === 0) {
      message.success('删除成功')
      deleteConfirmVisible.value = false
      // 从列表中移除
      chatList.value = chatList.value.filter(item => item.id !== selectedChat.value?.id)
      // 通知其他客户端
      chatListWebSocket.sendMessage({
        type: 'DELETE',
        data: { id: selectedChat.value.id }
      })
    } else {
      // message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    // message.error('删除失败，请稍后重试')
  }
}

// 显示修改名称弹窗
const showEditNameModal = (chat: PrivateChat) => {
  selectedChat.value = chat
  newChatName.value = chat.userChatName || ''
  editNameVisible.value = true
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
      // 更新本地数据
      const chat = chatList.value.find(item => item.id === selectedChat.value?.id)
      if (chat) {
        chat.userChatName = newChatName.value.trim()
        // 通知其他客户端
        chatListWebSocket.sendMessage({
          type: 'UPDATE',
          data: chat
        })
      }
      editNameVisible.value = false
    } else {
      // message.error('修改失败：' + res.data.message)
    }
  } catch (error: any) {
    // message.error('修改失败：' + error.message)
  } finally {
    editNameLoading.value = false
  }
}

// 移动端显示完整用户名
const showMobileNameToast = (name: string) => {
  // 判断名称长度,小于8个字符直接返回不显示弹框
  if(name.length <= 8) return;

  notification.info({
    message: undefined,
    description: name,
    style: {
      borderRadius: '8px',
      backgroundColor: 'rgba(0, 0, 0, 0.75)',
      color: '#fff',
      padding: '12px 16px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
    }
  })
}

// 获取聊天类型样式
const getChatTypeClass = (chat: PrivateChat) => {
  if (chat.chatType === 2) return 'ai-tag'
  return chat.chatType === 1 ? 'friend-tag' : 'private-tag'
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

// 在 setup 中添加以下方法
const handleClickOutside = () => {
  // 关闭所有展开的操作按钮
  chatList.value.forEach(chat => {
    if (chat.showActions) {
      chat.showActions = false
    }
  })
}

// 添加清除所有未读消息的方法
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
  } catch (error) {
    // message.error('清除未读消息失败')
  } finally {
    clearUnreadLoading.value = false
    clearUnreadVisible.value = false
  }
}

// 获取推荐部落
const goToTribes = async () => {
  try {
    loadingRecommended.value = true;
    // 调用API获取推荐部落
    const res = await listRecommendedSpacesUsingGet();

    if (res.data.code === 0 && res.data.data) {
      // 将获取到的推荐部落存储到响应式数据中
      recommendedSpaces.value = res.data.data;
      // message.success(`获取到 ${res.data.data.length} 个推荐部落`);
    } else {
      // 如果没有推荐部落或API调用失败
      recommendedSpaces.value = [];
      // message.info('暂无推荐部落');
    }
  } catch (error) {
    console.error('获取推荐部落失败:', error);
    message.error('获取推荐部落失败');
    recommendedSpaces.value = [];
  } finally {
    loadingRecommended.value = false;
  }
}

// 获取公共部落
const getPublicSpaces = async (isRefresh = false) => {
  if (loadingPublic.value) return;

  try {
    if (isRefresh) {
      publicCurrentPage.value = 1;
      loadingPublic.value = true;
    }

    const res = await listSpaceVoByPageUsingPost({
      current: publicCurrentPage.value,
      pageSize: 10, // 每页获取10个部落
      spaceType: 1, // 只获取团队部落（非私有部落）
    });

    if (res.data.code === 0 && res.data.data) {
      const { records, total, hasMore } = res.data.data;
      if (isRefresh || publicCurrentPage.value === 1) {
        // 刷新时替换整个列表
        publicSpaces.value = records || [];
      } else {
        // 加载更多时追加到现有列表
        publicSpaces.value = [...publicSpaces.value, ...(records || [])];
      }

      publicTotal.value = total || 0;
      publicHasMore.value = hasMore !== false; // 默认为true，除非明确返回false

      // 如果当前页小于总页数，可以继续加载更多
      const totalPages = Math.ceil(publicTotal.value / 10);
      publicHasMore.value = publicCurrentPage.value < totalPages;

      isPublicSpacesLoaded.value = true;
    } else {
      if (isRefresh || publicCurrentPage.value === 1) {
        publicSpaces.value = [];
      }
      publicHasMore.value = false;
    }
  } catch (error) {
    console.error('获取公共部落失败:', error);
    message.error('获取公共部落失败');
    if (isRefresh || publicCurrentPage.value === 1) {
      publicSpaces.value = [];
    }
    publicHasMore.value = false;
  } finally {
    loadingPublic.value = false;
  }
};

// 加载更多公共部落
const loadMorePublicSpaces = async () => {
  if (loadingPublic.value || !publicHasMore.value) return;

  publicCurrentPage.value++;
  await getPublicSpaces(false);
};

</script>

<style lang="scss" scoped>
#chatListPage {
  max-height: 98vh;
  position: relative;
  overflow: hidden;
}

.main-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}

#chatListHeader {
  padding: 16px 16px 12px;
  background: var(--ios-bg-blur);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 0.5px solid var(--ios-header-border);

  .chat-tabs {
    display: flex;
    justify-content: space-around;
    align-items: center;
    margin-bottom: 20px;
    padding: 10px 0;

    .tab-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);

      .icon-wrap {
        width: 54px;
        height: 54px;
        border-radius: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: var(--ios-tab-bg);
        color: var(--ios-tab-text);
        font-size: 24px;
        position: relative;
        transition: all 0.3s ease;

        .tab-badge {
          position: absolute;
          top: -6px;
          right: -6px;
          transform: scale(0.9);

          :deep(.ant-badge-count) {
            background: #ff3b30;
            box-shadow: 0 2px 4px rgba(255, 59, 48, 0.3);
            border: 2px solid #fff;
          }
        }
      }

      .tab-text {
        font-size: 13px;
        font-weight: 600;
        color: var(--ios-tab-text);
        transition: color 0.3s ease;
      }

      &.active {
        transform: scale(1.1);

        .icon-wrap {
          background: #007aff;
          color: #fff;
          box-shadow: 0 8px 16px rgba(0, 122, 255, 0.3);
          transform: translateY(-2px);
        }

        .tab-text {
          color: #007aff;
          font-weight: 700;
        }
      }

      &.scale-down {
        opacity: 0.6;
        transform: scale(0.9);
      }

      &:active {
        transform: scale(0.95);
      }
    }
  }

  .search-container {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 0 4px;

    .search-box {
      flex: 1;
      position: relative;
      background: var(--ios-search-bg);
      border-radius: 12px;
      height: 40px;
      display: flex;
      align-items: center;
      padding: 0 12px;
      transition: background-color 0.2s ease;

      &:focus-within {
        background: var(--ios-search-focus-bg);
      }

      .search-input {
        flex: 1;
        background: transparent;
        border: none;
        outline: none;
        font-size: 17px;
        color: var(--text-primary);
        height: 100%;
        width: 100%;

        &::placeholder {
          color: #8e8e93;
        }
      }

      .search-button {
        background: transparent;
        border: none;
        padding: 4px;
        color: var(--ios-tab-text);
        cursor: pointer;
        font-size: 16px;
        display: flex;
        align-items: center;
        justify-content: center;

        &:hover {
          color: #007aff;
        }
      }
    }

    .clear-all-btn {
      width: 36px;
      height: 36px;
      border-radius: 12px;
      background: var(--ios-tab-bg);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #007aff;
      font-size: 16px;
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
}


.search-icon {
  color: #fff;
  font-size: 16px;
  flex-shrink: 0;
}


/* Chat List Content Style */
#chatListContent {
  .chat-list {
    background: transparent;
    padding: 20px 16px;

    .chat-item-wrapper {
      background: var(--card-background);
      margin-bottom: 0; // Remove gap for grouped list
      border-radius: 0; // Default to square for middle items
      position: relative;
      transition: background-color 0.2s ease;

      // Hairline separator
      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        right: 0;
        left: 76px; // Align with chat content start (avatar width 52 + gap 12 + padding 12)
        height: 0.5px;
        background-color: var(--ios-modal-divider);
      }

      // Rounded corners for grouped list appearance
      &:first-child {
        border-top-left-radius: 12px;
        border-top-right-radius: 12px;
      }

      &:last-child {
        border-bottom-left-radius: 12px;
        border-bottom-right-radius: 12px;
        &::after { display: none; } // Remove last separator
      }

      &:active {
        background-color: var(--hover-background);
        .chat-item {
          transform: scale(0.98);
        }
      }

      .chat-item {
        padding: 12px 16px;
        display: flex;
        align-items: center;
        gap: 12px;
        transition: transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);

        .chat-avatar {
          position: relative;

          img {
            width: 52px;
            height: 52px;
            border-radius: 12px;
            background: var(--ios-tab-bg);
            object-fit: cover;
            box-shadow: 0 2px 8px var(--shadow-color);
          }

          .unread-badge {
            position: absolute;
            top: -6px;
            right: -6px;
            background: #ff3b30;
            color: #fff;
            font-size: 12px;
            font-weight: 600;
            min-width: 20px;
            height: 20px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 6px;
            border: 2px solid var(--card-background);
            box-shadow: 0 2px 4px rgba(255, 59, 48, 0.3);
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

            .chat-name {
              font-size: 17px;
              font-weight: 700;
              color: var(--text-primary);
              letter-spacing: -0.4px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .chat-time {
              font-size: 14px;
              color: var(--text-secondary);
              font-weight: 400;
              margin-left: 8px;
            }
          }

          .chat-message {
            font-size: 15px;
            color: var(--text-secondary);
            line-height: 1.3;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            letter-spacing: -0.2px;
          }
        }
      }
    }
  }
}



#chatListLoadMore {
  text-align: center;
  margin-top: 20px;

  :deep(.ant-btn-link) {
    color: #666;
    font-size: 14px;

    &:hover {
      color: #3b82f6;
    }
  }
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 16px;
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  }

  .empty-text {
    h3 {
      margin: 0;
      font-size: 18px;
      color: #333;
      font-weight: 600;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #666;
    }
  }
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  padding: 12px 0;
}

.no-more {
  color: #999;
  font-size: 14px;
  text-align: center;
  padding: 16px 0;
}

.ai-tag {
  width: auto;
  color: #722ed1 !important;
  background: linear-gradient(135deg, rgba(114, 46, 209, 0.1), rgba(173, 55, 255, 0.15)) !important;
  padding: 2px 8px !important;
  border-radius: 12px !important;
  font-size: 12px !important;
  font-weight: normal !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05) !important;
  backdrop-filter: blur(4px) !important;
}

.chat-list-wrapper {

  color: var(--text-primary);
  border-radius: 16px;

  overflow: hidden;
  height: calc(100vh - 200px);
  position: relative;
}

.chat-list {
  height: 100%;
  overflow-y: auto;
  padding: 8px;
  padding-bottom: 84px;

  &::-webkit-scrollbar {
    width: 3px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(37, 99, 235, 0.2);
    border-radius: 3px;

    &:hover {
      background: rgba(37, 99, 235, 0.4);
    }
  }
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin: 4px 0;
  border-radius: 12px;

  color: var(--text-primary);
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  border: 1px solid transparent;
  transform: translateX(0);
  width: 100%;
  box-sizing: border-box;

  &.slide-left {
    transform: translateX(-100px);
  }
}

.chat-item-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 12px;

  .slide-actions {
    position: absolute;
    right: 0;
    top: 0;
    height: 100%;
    display: flex;
    align-items: stretch;
    transform: translateX(100%);
    transition: transform 0.3s ease;

    .action-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 80px;
      border: none;
      color: white;
      font-size: 14px;
      transition: all 0.3s ease;

      .anticon {
        font-size: 20px;
        margin-bottom: 4px;
      }

      span {
        margin-top: 2px;
      }

      &.edit {
        background: #13c2c2;

        &:active {
          background: #08979c;
        }
      }

      &.delete {
        background: #ff4d4f;

        &:active {
          background: #cf1322;
        }
      }
    }
  }

  &.actions-visible {
    .chat-item {
      transform: translateX(-160px);
    }

    .slide-actions {
      transform: translateX(0);
    }
  }
}

@media screen and (max-width: 768px) {
  .chat-item-wrapper {
    touch-action: pan-y pinch-zoom;

    .chat-item {
      margin: 0;
      border-radius: 0;
      padding: 12px 16px;
    }

    .slide-actions {
      .action-btn {
        width: 68px;
        height: 48px;
        margin-top: 8px;
        margin-right: 4px;
        font-size: 13px;

        .anticon {
          font-size: 18px;
        }
      }
    }
  }

  .chat-item {
    &.unread {
      background: rgba(37, 99, 235, 0.1);
    }
  }
}

.chat-avatar {
  position: relative;
  margin-right: 12px;
  flex-shrink: 0;

  img {
    width: 48px;
    height: 48px;
    border-radius: 24px;
    object-fit: cover;
    border: 2px solid #f0f0f0;
    transition: all 0.3s ease;

    &:hover {
      border-color: #2563eb;
      transform: scale(1.05);
    }
  }

  .unread-badge {
    position: absolute;
    top: -4px;
    right: -4px;
    min-width: 18px;
    height: 18px;
    padding: 0 6px;
    background: #2563eb;
    color: white;
    font-size: 12px;
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
  }
}

.chat-content {
  flex: 1;
  min-width: 0;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.chat-name {
  font-size: 15px;
  font-weight: 500;

  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-tags {
  display: flex;
  gap: 6px;
  align-items: center;

  span {

    border-radius: 10px;
    font-size: 12px;
    font-weight: normal;
  }
}

.friend-tag {
  color: #2563eb;
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
  color: #fa8c16;
  background: #fff7e6;
}

.chat-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  margin-left: 8px;
}

.chat-message {
  font-size: 14px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-actions {
  display: flex;
  margin-left: 12px;
  margin-right: -4px;
  margin-bottom: -18px;
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s ease;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 16px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #999;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
    transform: scale(1.1);
  }

  &.edit:hover {
    color: #2563eb;
  }

  &.delete:hover {
    color: #ff4d4f;
  }

  .action-icon {
    font-size: 18px;
    line-height: 1;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 20px;
  text-align: center;

  .empty-icon {
    width: 64px;
    height: 64px;
    margin-bottom: 16px;

    .empty-svg {
      width: 100%;
      height: 100%;
      opacity: 0.5;
    }
  }

  h3 {
    font-size: 16px;
    color: #333;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: #666;
  font-size: 14px;

  .loading-spinner {
    width: 20px;
    height: 20px;
    border: 2px solid #f0f0f0;
    border-top-color: #2563eb;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media screen and (max-width: 768px) {
  .chat-list-wrapper {
    height: calc(100vh - 155px);
    border-radius: 12px;
  }

  .chat-item {
    padding: 10px;
    margin: 2px 0;

    &:hover {
      transform: none;
    }
  }

  .chat-avatar img {
    width: 40px;
    height: 40px;
  }

  .chat-name {
    font-size: 14px;
  }

  .chat-message {
    font-size: 13px;
  }

  .chat-actions {
    opacity: 1;
    transform: none;
  }

  .action-btn {
    width: 28px;
    height: 28px;

    .action-icon {
      font-size: 16px;
    }
  }
}

/* 分页器样式 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px;
  border-radius: 12px;

  .pagination-info {
    color: #666;
    font-size: 14px;
  }

  .pagination-buttons {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .page-btn {
    padding: 6px 12px;
    border: 1px solid #e8e8e8;
    border-radius: 6px;
    background: #fff;
    color: #666;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover:not(:disabled) {
      color: #2563eb;
      border-color: #2563eb;
    }

    &:disabled {
      color: #d9d9d9;
      border-color: #f0f0f0;
      cursor: not-allowed;
    }
  }

  .page-numbers {
    display: flex;
    gap: 4px;
  }

  .page-number {
    width: 32px;
    height: 32px;
    border: 1px solid #e8e8e8;
    border-radius: 6px;
    background: #fff;
    color: #666;
    font-size: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;

    &:hover:not(.active) {
      color: #2563eb;
      border-color: #2563eb;
    }

    &.active {
      background: #2563eb;
      color: #fff;
      border-color: #2563eb;
    }
  }

  .page-size {
    select {
      padding: 6px 24px 6px 12px;
      border: 1px solid #e8e8e8;
      border-radius: 6px;
      color: #666;
      font-size: 14px;
      cursor: pointer;
      appearance: none;
      background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23666' d='M6 8L2 4h8z'/%3E%3C/svg%3E") no-repeat right 8px center;
      transition: all 0.3s ease;

      &:hover {
        border-color: #2563eb;
      }

      &:focus {
        outline: none;
        border-color: #2563eb;
        box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.1);
      }
    }
  }
}

/* 弹窗样式 */
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
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  position: relative;
  text-align: center;
  animation: slideUp 0.3s ease;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

  .modal-icon {
    width: 48px;
    height: 48px;
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 16px;
    font-size: 24px;
    font-weight: bold;

    &.warning {
      background: #fff2e8;
      color: #fa8c16;
    }

    &.edit {
      background: #f6ffed;
      color: #2563eb;
    }
  }

  h3 {
    margin: 0 0 16px;
    color: #333;
    font-size: 18px;
  }

  p {
    margin: 0 0 24px;
    color: #666;
    font-size: 14px;
    line-height: 1.6;
  }
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.modal-btn {
  padding: 8px 24px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: none;

  &.cancel {
    background: #f5f5f5;
    color: #666;

    &:hover {
      background: #e8e8e8;
    }
  }

  &.confirm {
    background: #2563eb;
    color: #fff;

    &:hover {
      background: #3b82f6;
    }

    &:disabled {
      background: #d9d9d9;
      cursor: not-allowed;
    }
  }
}

.edit-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 8px;
  transition: all 0.3s ease;

  &:focus {
    outline: none;
    border-color: #2563eb;
    box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.1);
  }
}

.input-counter {
  text-align: right;
  color: #999;
  font-size: 12px;
  margin-bottom: 16px;
}

.loading-dots::after {
  content: '...';
  animation: dots 1.5s infinite;
}

@keyframes dots {
  0%, 20% { content: '.'; }
  40%, 60% { content: '..'; }
  80%, 100% { content: '...'; }
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

@media screen and (max-width: 768px) {
  .pagination {
    flex-direction: column;
    gap: 12px;
    margin-top: 16px;
    padding: 12px;

    .page-btn, .page-number {
      height: 36px;
    }
  }

  .modal-content {
    padding: 20px;
    width: calc(100% - 32px);
    margin: 16px;
  }
}

.pc-main-content {
  height: calc(100vh - 120px) !important;
  display: flex;
  flex-direction: column;
}

#chatListContent.pc-chat-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  margin-bottom: 16px;

  color: var(--text-primary);
  border-radius: 16px;
  padding: 12px;

}

// 修改 action-menu 的样式
.action-menu {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px;
  min-width: 160px;

  .action-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    color: #333;
    font-size: 14px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      background: #f5f5f5;
    }

    &.danger {
      color: #ff4d4f;

      &:hover {
        background: #fff1f0;
      }
    }

    .action-icon {
      font-size: 16px;
    }
  }
}

.list-bottom-tip {
  padding: 16px 0;
  text-align: center;

  .tip-line {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;

    .line {
      flex: 1;
      height: 1px;
      background: linear-gradient(to right, transparent, var(--divider-color, rgba(0, 0, 0, 0.06)), transparent);
    }

    .text {
      padding: 0 12px;
      font-size: 13px;
      color: var(--text-secondary, #999);
      white-space: nowrap;
    }
  }
}

@media screen and (max-width: 768px) {
  .list-bottom-tip {
    padding: 12px 0;

    .tip-line {
      padding: 0 12px;

      .text {
        font-size: 12px;
        padding: 0 8px;
      }
    }
  }
}

.clear-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #2563eb;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-left: 16px;
  border: 1px solid transparent;

  &:hover {
    background: rgba(37, 99, 235, 0.2);
    border-color: #2563eb;
    transform: rotate(15deg);
  }

  &:active {
    transform: scale(0.95) rotate(15deg);
  }

  .anticon {
    font-size: 16px;
  }
}

@media screen and (max-width: 768px) {
  .clear-all-btn {
    width: 28px;
    height: 28px;
    margin-left: 8px;

    .anticon {
      font-size: 14px;
    }
  }
}

/* Apple Alert Modal Style */
.modal-overlay {
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.modal-content {
  background: var(--ios-modal-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 14px;
  padding: 0;
  width: 270px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);

  .modal-inner {
    padding: 20px;
  }

  h3 {
    font-size: 17px;
    font-weight: 600;
    margin-bottom: 4px;
    color: var(--text-primary);
  }

  p {
    font-size: 13px;
    line-height: 1.4;
    color: var(--text-primary);
    margin: 0;
  }

  .modal-actions {
    display: flex;
    border-top: 0.5px solid var(--ios-modal-divider);

    .modal-btn {
      flex: 1;
      height: 44px;
      padding: 0;
      background: transparent;
      border: none;
      font-size: 17px;
      color: #007aff;
      cursor: pointer;
      font-weight: 400;

      &:hover { background: rgba(128, 128, 128, 0.1); }
      &:active { background: rgba(128, 128, 128, 0.2); }

      &.confirm {
        font-weight: 600;
        border-left: 0.5px solid var(--ios-modal-divider);
      }

      &.danger { color: #ff3b30; }
    }
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


/* Discovery Actions Style */
.discovery-actions {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;

  .action-item {
    background: var(--card-background);
    border-radius: 20px;
    padding: 20px 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    box-shadow: 0 4px 12px var(--shadow-color);
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    border: 1px solid var(--border-color);

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px var(--shadow-color);
      border-color: rgba(0, 122, 255, 0.2);
    }

    .action-icon {
      width: 64px;
      height: 64px;
      border-radius: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 28px;
      color: #fff;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);

      &.tree-hole { background: linear-gradient(135deg, #AF52DE, #5856D6); }
      &.friend-link { background: linear-gradient(135deg, #007AFF, #5AC8FA); }
      &.love-square { background: linear-gradient(135deg, #FF2D55, #FF375F); }
      &.tools { background: linear-gradient(135deg, #FF9500, #FFCC00); }
      &.games { background: linear-gradient(135deg, #34C759, #32D74B); }
    }

    .action-label {
      font-size: 15px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
      letter-spacing: -0.2px;
    }
  }
}

/* 部落内容区域样式 */
.tribe-content {
  padding: 6px;
  height: calc(100vh - 200px);
  overflow-y: auto;
  position: relative;
}

.tribe-content::-webkit-scrollbar {
  width: 3px;
}

.tribe-content::-webkit-scrollbar-track {
  background: transparent;
}

.tribe-content::-webkit-scrollbar-thumb {
  background: var(--nav-item-active);
  border-radius: 3px;
}

.tribe-content::-webkit-scrollbar-thumb:hover {
  background: var(--nav-item-active);
}

/* 推荐部落标题样式 */
.recommended-header {
  margin-bottom: 16px;
}

.recommended-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  padding-left: 8px;
  position: relative;
}

.recommended-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 20px;
  background: #007aff;
  border-radius: 4px;
}

.recommended-spaces-list {
  display: flex;
  flex-direction: row;
  gap: 16px;
  padding: 8px;
  overflow-x: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  /* 防止卡片被压缩 */
  flex-wrap: nowrap;
}

.recommended-spaces-list::-webkit-scrollbar {
  display: none;
}

.recommended-spaces-list > * {
  /* 保持卡片原始宽度，不被压缩 */
  flex: 0 0 auto;
  width: 300px;
}

.recommended-spaces-list::-webkit-scrollbar {
  height: 8px;
}

.recommended-spaces-list::-webkit-scrollbar-track {
  background: #f9fafb;
  border-radius: 4px;
}

.recommended-spaces-list::-webkit-scrollbar-thumb {
  background-color: #d1d5db;
  border-radius: 4px;
}

.recommended-spaces-list::-webkit-scrollbar-thumb:hover {
  background-color: #9ca3af;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .recommended-spaces-list {
    padding: 4px;
  }
}

/* 加载状态样式 */
.loading-state {
  padding: 20px 0;
}

.loading-item {
  margin-bottom: 16px;
}

.skeleton-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.skeleton-cover {
  height: 180px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.skeleton-content {
  padding: 16px;
}

.skeleton-title {
  height: 20px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  margin-bottom: 12px;
  border-radius: 4px;
}

.skeleton-desc {
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
  border-radius: 4px;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 公共部落列表样式 */
.public-spaces-header {
  margin: 20px 0 10px 0;
  padding: 0 16px;
}

.public-spaces-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.load-more-container {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.load-more-btn {
  padding: 8px 24px;
  color: #776e6e;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.load-more-btn:hover:not(:disabled) {
  background-color: #1d4ed8;
}

.load-more-btn:disabled {
  background-color: #cbd5e1;
  cursor: not-allowed;
}

/* 发现页功能列表容器样式 */
.function-container {
  width: 100%;
  max-width: 750px;
  margin: 0 auto;
  padding: 20px 6px;
  min-height: 100vh;
}

.function-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.function-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 15px;
  background: linear-gradient(135deg, #fff 0%, #fefefe 100%);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
}

.function-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.function-card:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-treehole {
  background: linear-gradient(135deg, #9c66e2 0%, #b48ef5 100%);
}

.icon-link {
  background: linear-gradient(135deg, #40a9ff 0%, #69c0ff 100%);
}

.icon-love {
  background: linear-gradient(135deg, #ff6b81 0%, #ff8fa3 100%);
}

.icon-tool {
  background: linear-gradient(135deg, #ff9a44 0%, #ffb76b 100%);
}

.icon-game {
  background: linear-gradient(135deg, #52c41a 0%, #73d13d 100%);
}

.card-text {
  flex: 1;
}

.card-title {
  font-size: 16px;
  color: #333;
  font-weight: 500;
  line-height: 1.4;
}

.card-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.2;
}

/* 小屏适配 */
@media (max-width: 375px) {
  .function-grid {
    grid-template-columns: 1fr;
  }
}

.empty-state .empty-icon {
  margin-bottom: 20px;
}

.empty-state .empty-svg {
  width: 80px;
  height: 80px;
}

.empty-state h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

/* 骨架屏样式 */
.skeleton-list {
  padding: 8px;

  .skeleton-item {
    display: flex;
    align-items: center;
    padding: 12px;
    margin: 4px 0;
    border-radius: 12px;
    background: var(--card-background);
    animation: skeleton-loading 1.5s infinite ease-in-out;
  }

  .skeleton-avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--ios-tab-bg);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    margin-right: 12px;
    flex-shrink: 0;
  }

  .skeleton-content {
    flex: 1;

    .skeleton-line {
      height: 16px;
      border-radius: 8px;
      background: var(--ios-tab-bg);
      background-size: 200% 100%;
      animation: skeleton-loading 1.5s infinite;

      &.name {
        width: 120px;
        margin-bottom: 8px;
      }

      &.message {
        width: 200px;
      }
    }
  }
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
