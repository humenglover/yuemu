<template>
  <div id="chatListPage">
    <!-- 背景动画 -->
    <div class="background-animation" v-if="device !== DEVICE_TYPE_ENUM.PC">
      <div class="gradient-bg" style="background: linear-gradient(45deg, rgba(82, 196, 26, 0.03), rgba(115, 209, 61, 0.05))"></div>
      <div class="moving-waves"></div>
    </div>


    <div class="decorative-elements">
      <div class="floating-circle circle-1" style="background: rgba(82, 196, 26, 0.1)"></div>
      <div class="floating-circle circle-2" style="background: rgba(115, 209, 61, 0.08)"></div>
      <div class="floating-circle circle-3" style="background: rgba(135, 208, 104, 0.06)"></div>
    </div>

    <div class="main-content" :class="{ 'pc-main-content': device === DEVICE_TYPE_ENUM.PC }" style="z-index: 10;">
      <!-- 下拉刷新区域 -->
      <van-pull-refresh
        v-model="refreshing"
        @refresh="onRefresh"
        v-if="device !== DEVICE_TYPE_ENUM.PC"
        :disabled="!isTop"
        success-text="刷新成功"
        :head-height="60"
      >
        <!-- 顶部导航栏和搜索框 -->
        <div id="chatListHeader">
          <!-- 标签栏 -->
          <div class="chat-tabs">
            <div
              class="tab-item"
              :class="{ active: activeTab === 'all' }"
              @click="handleTabChange('all')"
            >
              <span>全部</span>
              <div class="tab-line"></div>
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'friend' }"
              @click="handleTabChange('friend')"
            >
              <span>好友</span>
              <div class="badge" v-if="friendUnreadCount > 0">{{ friendUnreadCount }}</div>
              <div class="tab-line"></div>
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'private' }"
              @click="handleTabChange('private')"
            >
              <span>私信</span>
              <div class="badge" v-if="privateUnreadCount > 0">{{ privateUnreadCount }}</div>
              <div class="tab-line"></div>
            </div>
          </div>
          <!-- 搜索栏 -->
          <div class="search-container">
            <div class="search-box">
              <input
                type="text"
                v-model="searchText"
                placeholder="搜索聊天记录"
                @keyup.enter="handleSearch"
                class="search-input"
              />
              <button class="search-button" @click="handleSearch" :disabled="loading">
                <i class="search-icon"></i>
              </button>
            </div>
          </div>
        </div>

        <!-- 聊天列表 -->
        <div class="chat-list-wrapper">
          <!-- 空状态 -->
          <div v-if="!loading && filteredChatList.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" class="empty-svg">
                <path fill="#52c41a" d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z" />
              </svg>
            </div>
            <h3>{{ getEmptyText() }}</h3>
            <p v-if="searchText">换个关键词试试吧</p>
          </div>

          <!-- 聊天列表 -->
          <div v-else class="chat-list" @scroll="handleListScroll">
            <div
              v-for="item in filteredChatList"
              :key="item.id"
              class="chat-item"
              :class="{ unread: getUnreadCount(item) > 0 }"
              @click="handleChatClick(item)"
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
                          <template v-if="device === DEVICE_TYPE_ENUM.PC">
                            <span class="tag-text">{{ item.chatType === 2 ? 'AI助手' : item.chatType === 1 ? '好友' : '私信' }}</span>
                          </template>
                          <template v-else>
                            <i v-if="item.chatType === 2"><RobotOutlined /></i>
                            <i v-else-if="item.chatType === 1"><UserOutlined /></i>
                            <i v-else><MessageOutlined /></i>
                          </template>
                        </span>
                      <span v-if="item.isSender" class="sender-tag">
                          <template v-if="device === DEVICE_TYPE_ENUM.PC">
                            <span class="tag-text">发起者</span>
                          </template>
                          <template v-else>
                            <i><SendOutlined /></i>
                          </template>
                        </span>
                    </div>
                  </div>
                  <div class="chat-time">{{ formatMessageTime(item.lastMessageTime) }}</div>
                </div>
                <div class="chat-message">{{ item.lastMessage || '暂无消息' }}</div>
              </div>

              <div class="chat-actions">
                <button class="action-btn edit" @click.stop="showEditNameModal(item)">
                  <span class="action-icon">✎</span>
                </button>
                <button class="action-btn delete" @click.stop="showDeleteConfirm(item)">
                  <span class="action-icon">×</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </van-pull-refresh>

      <!-- PC端内容 -->
      <template v-else>
        <div id="chatListHeader">
          <!-- 标签栏 -->
          <div class="chat-tabs">
            <div
              class="tab-item"
              :class="{ active: activeTab === 'all' }"
              @click="handleTabChange('all')"
            >
              <span>全部</span>
              <div class="tab-line"></div>
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'friend' }"
              @click="handleTabChange('friend')"
            >
              <span>好友</span>
              <div class="badge" v-if="friendUnreadCount > 0">{{ friendUnreadCount }}</div>
              <div class="tab-line"></div>
            </div>
            <div
              class="tab-item"
              :class="{ active: activeTab === 'private' }"
              @click="handleTabChange('private')"
            >
              <span>私信</span>
              <div class="badge" v-if="privateUnreadCount > 0">{{ privateUnreadCount }}</div>
              <div class="tab-line"></div>
            </div>
          </div>
          <!-- 搜索栏 -->
          <div class="search-container">
            <div class="search-box">
              <input
                type="text"
                v-model="searchText"
                placeholder="搜索聊天记录"
                @keyup.enter="handleSearch"
                class="search-input"
              />
              <button class="search-button" @click="handleSearch" :disabled="loading">
                <i class="search-icon"></i>
              </button>
            </div>
          </div>
        </div>

        <div id="chatListContent" class="pc-chat-list">
          <!-- 空状态 -->
          <div v-if="!loading && filteredChatList.length === 0" class="empty-state">
            <lottie-player
              :src="getEmptyLottieUrl()"
              background="transparent"
              speed="1"
              style="width: 240px; height: 240px;"
              loop
              autoplay
              @error="handleLottieError"
            ></lottie-player>
            <div class="empty-text">
              <h3>{{ getEmptyText() }}</h3>
              <p v-if="searchText">换个关键词试试吧</p>
            </div>
          </div>

          <!-- 聊天列表 -->
          <a-list v-else
                  :loading="loading"
                  :data-source="filteredChatList"
                  :split="true"
                  class="chat-list"
          >
            <template #renderItem="{ item }">
              <a-list-item @click="handleChatClick(item)" :class="{ unread: getUnreadCount(item) > 0 }">
                <a-list-item-meta>
                  <template #avatar>
                    <div class="avatar-wrapper">
                      <a-avatar :src="item.targetUser?.userAvatar || getDefaultAvatar(item.targetUser?.userName)" />
                      <a-badge
                        :count="getUnreadCount(item)"
                        :numberStyle="{ backgroundColor: '#73d13d' }"
                      >
                        <span v-if="getUnreadCount(item) > 0" class="unread-badge">
                          {{ getUnreadCount(item) > 99 ? '99+' : getUnreadCount(item) }}
                        </span>
                      </a-badge>
                    </div>
                  </template>
                  <template #title>
                    <div class="chat-title">
                      <!-- PC端使用tooltip，移动端使用点击事件 -->
                      <a-tooltip
                        v-if="!isMobile"
                        :title="item.isSender ? item.userChatName : item.targetUserChatName"
                        :mouseEnterDelay="0.5"
                        placement="top"
                      >
                        <span class="username-text">{{ item.isSender ? item.userChatName : item.targetUserChatName }}</span>
                      </a-tooltip>
                      <!-- 移动端点击显示 -->
                      <span
                        v-else
                        class="username-text"
                        @click.stop="handleNameClick(item, item.isSender ? item.userChatName : item.targetUserChatName)"
                      >
                        {{ item.isSender ? item.userChatName : item.targetUserChatName }}
                      </span>
                      <span
                        :class="{
                          'friend-tag': item.chatType === 1,
                          'private-tag': item.chatType === 0,
                          'ai-tag': item.chatType === 2
                        }"
                      >
                        {{ item.chatType === 2 ? 'AI助手' : item.chatType === 1 ? '好友' : '私信' }}
                      </span>
                      <span v-if="item.isSender" class="sender-tag">发起者</span>
                    </div>
                  </template>
                  <template #description>
                    <div class="chat-desc">
                      <span class="last-message">{{ item.lastMessage || '暂无消息' }}</span>
                      <span class="message-time">{{ formatMessageTime(item.lastMessageTime) }}</span>
                    </div>
                  </template>
                </a-list-item-meta>
                <!-- 添加更多操作按钮 -->
                <template #extra>
                  <a-dropdown
                    placement="bottomRight"
                    trigger="click"
                    @click.stop
                  >
                    <a-button
                      type="text"
                      class="more-btn"
                      @click.stop
                    >
                      <EllipsisOutlined />
                    </a-button>
                    <template #overlay>
                      <div class="action-menu">
                        <div
                          class="action-item"
                          @click.stop="showEditNameModal(item)"
                        >
                          <EditOutlined class="action-icon" />
                          <span>修改聊天名称</span>
                        </div>
                        <div
                          class="action-item danger"
                          @click.stop="showDeleteConfirm(item)"
                        >
                          <DeleteOutlined class="action-icon" />
                          <span>删除聊天</span>
                        </div>
                      </div>
                    </template>
                  </a-dropdown>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </div>

        <!-- PC端分页器 -->
        <div v-if="device === DEVICE_TYPE_ENUM.PC" class="pagination">
          <div class="pagination-info">共 {{ total }} 条</div>
          <div class="pagination-buttons">
            <button
              class="page-btn"
              :disabled="pcsearchParams.current === 1"
              @click="pchandlePageChange(pcsearchParams.current - 1, pcsearchParams.pageSize)"
            >
              上一页
            </button>
            <div class="page-numbers">
              <button
                v-for="page in pageNumbers"
                :key="page"
                class="page-number"
                :class="{ active: page === pcsearchParams.current }"
                @click="pchandlePageChange(page, pcsearchParams.pageSize)"
              >
                {{ page }}
              </button>
            </div>
            <button
              class="page-btn"
              :disabled="pcsearchParams.current * pcsearchParams.pageSize >= total"
              @click="pchandlePageChange(pcsearchParams.current + 1, pcsearchParams.pageSize)"
            >
              下一页
            </button>
          </div>
          <div class="page-size">
            <select
              v-model="pcsearchParams.pageSize"
              @change="pchandlePageChange(1, Number($event.target.value))"
            >
              <option value="4">4条/页</option>
              <option value="12">12条/页</option>
              <option value="20">20条/页</option>
              <option value="30">30条/页</option>
            </select>
          </div>
        </div>
      </template>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="deleteConfirmVisible" class="modal-overlay">
      <div class="modal-content delete-modal">
        <div class="modal-icon warning">!</div>
        <h3>确认删除该聊天？</h3>
        <p>
          用户名称：{{ selectedChat?.targetUser?.userName || '未设置' }}<br>
          聊天类型：{{ selectedChat?.chatType === 1 ? '好友' : '私信' }}
        </p>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="deleteConfirmVisible = false">取消</button>
          <button class="modal-btn confirm" @click="confirmDelete">确认删除</button>
        </div>
      </div>
    </div>

    <!-- 修改名称弹窗 -->
    <div v-if="editNameVisible" class="modal-overlay">
      <div class="modal-content edit-modal">
        <div class="modal-icon edit">✎</div>
        <h3>修改聊天名称</h3>
        <input
          v-model="newChatName"
          type="text"
          placeholder="请输入新的聊天名称"
          maxlength="50"
          class="edit-input"
        />
        <div class="input-counter">{{ newChatName.length }}/50</div>
        <div class="modal-actions">
          <button class="modal-btn cancel" @click="editNameVisible = false">取消</button>
          <button
            class="modal-btn confirm"
            :disabled="editNameLoading"
            @click="handleEditNameConfirm"
          >
            <span v-if="editNameLoading" class="loading-dots">确认中</span>
            <span v-else>确认</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 底部加载状态 -->
    <div v-if="loading" class="loading-more">
      <a-spin size="small" />
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal, notification } from 'ant-design-vue'
import { SearchOutlined, EllipsisOutlined, DeleteOutlined, ExclamationCircleOutlined, ExclamationCircleFilled, EditOutlined, UserOutlined, MessageOutlined, RobotOutlined, SendOutlined } from '@ant-design/icons-vue'
import { listPrivateChatByPageUsingPost, deletePrivateChatUsingPost, updateChatNameUsingPost } from '@/api/privateChatController'
import { formatMessageTime } from "@/utils/dateUtils"
import { getDefaultAvatar } from '@/utils/userUtils'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import type { PrivateChat } from '@/api/types'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'

// 设备类型
const device = ref<string>('')
const isMobile = computed(() => window.innerWidth <= 768)

// 状态变量
const loading = ref(false)
const refreshing = ref(false)
const isTop = ref(true)
const searchText = ref('')
const activeTab = ref('all')
const friendUnreadCount = ref(0)
const privateUnreadCount = ref(0)
const chatList = ref<PrivateChat[]>([])
const selectedChat = ref<PrivateChat | null>(null)
const deleteConfirmVisible = ref(false)
const editNameVisible = ref(false)
const editNameLoading = ref(false)
const newChatName = ref('')
const hasMore = ref(true)
const total = ref(0)

// 移动端分页相关变量
const mobileCurrentPage = ref(1)
const isLoadingMore = ref(false)

// 获取未读消息数
const getUnreadCount = (chat: PrivateChat) => {
  // 如果当前登录用户是发起者，返回userUnreadCount
  if (chat.userId === loginUserStore.loginUser?.id) {
    return chat.userUnreadCount || 0
  }
  // 如果当前登录用户是目标用户，返回targetUserUnreadCount
  return chat.userUnreadCount || 0
}

// 计算未读消息数
const calculateUnreadCounts = () => {
  friendUnreadCount.value = chatList.value
    .filter(chat => chat.chatType === 1)
    .reduce((sum, chat) => sum + (getUnreadCount(chat) || 0), 0)

  privateUnreadCount.value = chatList.value
    .filter(chat => chat.chatType === 0)
    .reduce((sum, chat) => sum + (getUnreadCount(chat) || 0), 0)
}

// 监听聊天列表变化，重新计算未读数
watch(() => chatList.value, calculateUnreadCounts, { deep: true })

// 优化: 列表过滤和排序
const filteredChatList = computed(() => {
  let filtered = chatList.value

  // 根据标签过滤
  if (activeTab.value !== 'all') {
    filtered = filtered.filter(chat =>
      activeTab.value === 'friend' ? chat.chatType === 1 : chat.chatType === 0
    )
  }

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
  }

  // 按最后消息时间排序
  return filtered.sort((a, b) =>
    new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime()
  )
})

// 优化: 分页处理
const paginatedList = computed(() => {
  if (isMobile.value) {
    return filteredChatList.value
  }
  const start = (current.value - 1) * pageSize.value
  return filteredChatList.value.slice(start, start + pageSize.value)
})

// 初始化
onMounted(async () => {
  device.value = await getDeviceType()
  isMobile.value ? fetchChatList(true) : pcfetchChatList(true)
})

const loginUserStore = useLoginUserStore()
const router = useRouter()
const current = ref(1)
const pageSize = ref(10)

// 根据当前标签过滤聊天列表
const handleTabChange = (tab: string) => {
  activeTab.value = tab
  fetchChatList(true)
  pcfetchChatList(true)
}

// 修改搜索参数的定义
const searchParams = reactive({
  current: 1,
  pageSize: 15,
  searchText: '',
  chatType: undefined
})

const pcsearchParams = reactive({
  current: 1,
  pageSize: 6 ,
  searchText: '',
  chatType: undefined
})

// Add after the existing data declarations in setup
const deepseekChat = reactive({
  id: -1, // Special ID for DeepSeek chat
  targetUser: {
    id: -1,
    userName: 'DeepSeek v3',
    userAvatar: 'https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com/public/1866450683272450049/2025-03-14_ZKdnsKKV4Z3Rxvcx.webp',
    userAccount: 'deepseek',
    createTime: new Date().toISOString()
  },
  chatType: 2, // New type for AI chat
  lastMessage: '你好!我是 DeepSeek v3, 一个智能AI助手',
  lastMessageTime: new Date().toISOString(),
  targetUserChatName: 'DeepSeek v3',
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
    const res = await listPrivateChatByPageUsingPost({
      searchText: searchText.value,
      current: device.value === DEVICE_TYPE_ENUM.PC ? searchParams.current : mobileCurrentPage.value,
      pageSize: device.value === DEVICE_TYPE_ENUM.PC ? 7 : 15,
      chatType: activeTab.value === 'all' ? undefined : activeTab.value === 'friend' ? 1 : 0
    })
    if (res.data.code === 0) {
      const { records, total: totalCount } = res.data.data
      // Add DeepSeek chat at the beginning
      const allChats = [deepseekChat, ...records]
      if (device.value === DEVICE_TYPE_ENUM.PC) {
        chatList.value = allChats
      } else {
        // Mobile append data
        if (isRefresh) {
          chatList.value = allChats
        } else {
          chatList.value = [...chatList.value, ...records]
        }
        hasMore.value = records.length === 15
      }
      total.value = totalCount + 1 // Add 1 for DeepSeek chat
    } else {
      message.error('获取聊天列表失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取聊天列表失败：' + error.message)
  } finally {
    loading.value = false
    isLoadingMore.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  fetchChatList(true)
  pcfetchChatList(true)
}

// 点击聊天项
const handleChatClick = (chat: PrivateChat) => {
  if (chat.id === -1) {
    // DeepSeek chat
    router.push({
      path: '/chat/ai',
      query: {
        userName: 'DeepSeek v3',
        userAvatar: 'https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com/public/1866450683272450049/2025-03-14_ZKdnsKKV4Z3Rxvcx.webp',
        userAccount: 'deepseek',
        createTime: new Date().toISOString(),
        isAI: 'true'
      }
    })
  } else if (chat.targetUser) {
    // Normal chat
    router.push({
      path: `/chat/${chat.targetUser.id}`,
      query: {
        privateChatId: chat.id,
        userName: chat.isSender ? chat.userChatName : chat.targetUserChatName,
        userAvatar: chat.targetUser.userAvatar,
        userAccount: chat.targetUser.userAccount,
        createTime: chat.targetUser.createTime,
        isSender: chat.isSender.toString()
      }
    })
  }
}

// 添加移动端滚动加载处理
const handleListScroll = async (e: Event) => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return

  const { scrollHeight, scrollTop, clientHeight } = e.target as HTMLElement
  // 更新顶部状态
  isTop.value = scrollTop <= 0

  const threshold = 50 // 距底部多少像素时触发加载

  if (!loading.value && !isLoadingMore.value && hasMore.value && scrollHeight - scrollTop - clientHeight <= threshold) {
    isLoadingMore.value = true
    mobileCurrentPage.value++
    await fetchChatList()
    isLoadingMore.value = false
  }
}

// 修改下拉刷新处理
const onRefresh = async () => {
  if (!isTop.value) {
    refreshing.value = false
    return
  }

  try {
    await fetchChatList(true)
  } finally {
    refreshing.value = false
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
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('删除失败，请稍后重试')
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
      }
      editNameVisible.value = false
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('修改失败：' + error.message)
  } finally {
    editNameLoading.value = false
  }
}

// 处理名称点击
const handleNameClick = (item: PrivateChat, name: string) => {
  if (name.length > 8) {
    showMobileNameToast(name)
  } else {
    handleChatClick(item)
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

// 修改分页处理方法
const pchandlePageChange = (page: number, size: number) => {

  pcsearchParams.current = page
  pcsearchParams.pageSize = size
  pcfetchChatList(true)
}
// 修改获取聊天列表的方法
const pcfetchChatList = async (isRefresh = false) => {
  if (loading.value) return
  try {
    loading.value = true
    if (isRefresh) {
      chatList.value = []
    }
    const res = await listPrivateChatByPageUsingPost({
      searchText: searchText.value,
      current: pcsearchParams.current,
      pageSize: pcsearchParams.pageSize,
      chatType: activeTab.value === 'all' ? undefined : activeTab.value === 'friend' ? 1 : 0
    })
    console.log(res.data.data)
    if (res.data.code === 0) {
      const { records, total: totalCount } = res.data.data
      chatList.value =[deepseekChat,...records]
      total.value = totalCount
    } else {
      message.error('获取聊天列表失败：' + res.data.message)
    }
  } catch (error: any) {
    message.error('获取聊天列表失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 获取聊天类型样式
const getChatTypeClass = (chat: PrivateChat) => {
  if (chat.chatType === 2) return 'ai-tag'
  return chat.chatType === 1 ? 'friend-tag' : 'private-tag'
}

// 获取空状态动画URL
const getEmptyLottieUrl = () => {
  if (searchText.value) {
    return 'https://assets1.lottiefiles.com/packages/lf20_bujdzzfn.json'
  }
  switch (activeTab.value) {
    case 'friend':
      return 'https://assets1.lottiefiles.com/packages/lf20_yg3groupm.json'
    case 'private':
      return 'https://assets1.lottiefiles.com/packages/lf20_AMBEWz.json'
    default:
      return 'https://assets1.lottiefiles.com/packages/lf20_yg3groupm.json'
  }
}

// Add error handling for lottie player
const handleLottieError = (e: any) => {
  console.warn('Lottie animation failed to load:', e)
  // You can set a fallback image or handle the error in another way
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

// 计算分页页码
const pageNumbers = computed(() => {
  const current = pcsearchParams.current
  const pageCount = Math.ceil(total.value / pcsearchParams.pageSize)
  const pages: number[] = []

  if (pageCount <= 5) {
    for (let i = 1; i <= pageCount; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 3) {
      for (let i = 1; i <= 5; i++) {
        pages.push(i)
      }
    } else if (current >= pageCount - 2) {
      for (let i = pageCount - 4; i <= pageCount; i++) {
        pages.push(i)
      }
    } else {
      for (let i = current - 2; i <= current + 2; i++) {
        pages.push(i)
      }
    }
  }

  return pages
})

</script>

<style lang="scss" scoped>
/* 移动端性能优化 */
@media screen and (max-width: 768px) {
  .main-content {
    backdrop-filter: none !important;

  }

  #chatListContent {
    .chat-list {
      transform: translate3d(0, 0, 0);
      will-change: transform;
      background: rgba(255, 255, 255, 0.9) !important; /* Semi-transparent background */
      box-shadow: none;
      border-radius: 12px;

      :deep(.ant-list-item) {
        transition: none !important;
        animation: none !important;
        transform: none !important;
        background: rgba(255, 255, 255, 0.8) !important; /* Semi-transparent items */

        &:hover {
          transform: none !important;
          box-shadow: none !important;
          background: rgba(255, 255, 255, 0.95) !important;
        }

        &.unread {
          background: rgba(82, 196, 26, 0.1) !important;
        }

        .ant-avatar {
          transition: none !important;
          transform: none !important;
          box-shadow: none !important;
          border: none !important;
        }

        .chat-title, .chat-desc {
          transform: translateZ(0);
        }
      }
    }
  }

  .background-animation {
    display: none !important;
  }

  .search-container {
    padding: 0 12px;
    margin-bottom: 16px;

    .search-input {
      height: 40px;
      font-size: 14px;
    }

    .search-button {
      width: 32px;
      height: 32px;
    }
  }

  .chat-tabs {
    gap: 24px;
    margin: 4px 0;

    .tab-item {
      font-size: 15px;
    }
  }

  :deep(.van-pull-refresh__track) {
    will-change: transform;
  }

  .loading-more {
    opacity: 0.8;
    transition: none !important;
  }
}

#chatListPage {
  max-height: 98vh;
  position: relative;
  overflow: hidden;
  margin-left: -20px!important;
  margin-right: -20px !important;
  margin-top: -24px !important;
  border-radius: 24px !important;
}

/* 背景动画 */
.background-animation {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.gradient-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
}

.moving-waves {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.15) 25%, transparent 25%),
  linear-gradient(-45deg, rgba(255, 255, 255, 0.15) 25%, transparent 25%);
  background-size: 60px 60px;
  animation: waveMove 20s linear infinite;
}

/* 装饰元素 */
.decorative-elements {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
  pointer-events: none;
}

.floating-circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(255, 142, 83, 0.1), rgba(255, 107, 107, 0.1));
  animation: float 20s infinite;
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -150px;
  left: -150px;
  animation-delay: -5s;
}

.circle-2 {
  width: 400px;
  height: 400px;
  top: 40%;
  right: -200px;
  animation-delay: -10s;
}

.circle-3 {
  width: 200px;
  height: 200px;
  bottom: -100px;
  left: 30%;
  animation-delay: -15s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(10px, 10px) rotate(5deg);
  }
  50% {
    transform: translate(0, 20px) rotate(0deg);
  }
  75% {
    transform: translate(-10px, 10px) rotate(-5deg);
  }
}

@keyframes waveMove {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 60px 60px;
  }
}

.main-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
  margin: 0 auto;
}
:deep(.ant-btn-primary) {
  background: #52c41a !important;
  border-color: #52c41a !important;
  height: 30px !important;
  width: 50px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  border-radius: 0 20px 20px 0 !important;
  transition: all 0.3s ease !important;

  &:hover {
    background: #52c41a !important;
    border-color: #52c41a !important;
  }

  &:active {
    transform: scale(0.98);
  }
}
#chatListHeader {
  display: flex;
  flex-direction: column;
  margin-bottom: 6px;

  .chat-tabs {
    display: flex;
    justify-content: center;
    gap: 32px;
    margin: 8px 0;
    padding: 0 16px;
    position: relative;
  }

  .tab-item {
    position: relative;
    padding: 8px 4px;
    font-size: 16px;
    color: #666;
    cursor: pointer;
    transition: all 0.3s ease;
    user-select: none;

    &:hover {
      color: #52c41a;
    }

    &.active {
      color: #52c41a;
      font-weight: 500;

      .tab-line {
        transform: scaleX(1);
        opacity: 1;
      }
    }

    .tab-line {
      position: absolute;
      bottom: -2px;
      left: 0;
      width: 100%;
      height: 3px;
      background: #52c41a;
      border-radius: 2px;
      transform: scaleX(0);
      opacity: 0;
      transition: all 0.3s ease;
      transform-origin: center;
    }

    .badge {
      position: absolute;
      top: -8px;
      right: -12px;
      min-width: 18px;
      height: 18px;
      padding: 0 6px;
      background: #52c41a;
      color: white;
      font-size: 12px;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: normal;
      box-shadow: 0 2px 6px rgba(82, 196, 26, 0.2);
    }
  }

  .search-container {
    padding: 0 16px;
    margin-bottom: 4px;

    .search-box {
      position: relative;
      width: 100%;
      max-width: 600px;
      margin: 0 auto;
    }

    .search-input {
      width: 100%;
      height: 44px;
      padding: 0 44px 0 16px;
      border: 2px solid #e8f5e9;
      border-radius: 22px;
      font-size: 15px;
      background: #fff;
      transition: all 0.3s ease;
      color: #333;

      &:focus {
        outline: none;
        border-color: #52c41a;
        box-shadow: 0 0 0 3px rgba(82, 196, 26, 0.1);
      }

      &::placeholder {
        color: #999;
      }
    }

    .search-button {
      position: absolute;
      right: 4px;
      top: 4px;
      width: 36px;
      height: 36px;
      border: none;
      border-radius: 18px;
      background: #52c41a;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s ease;

      &:hover {
        background: #73d13d;
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(1px);
      }

      &:disabled {
        background: #d9d9d9;
        cursor: not-allowed;
      }

      .search-icon {
        width: 16px;
        height: 16px;
        border: 2px solid #fff;
        border-radius: 50%;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          width: 2px;
          height: 8px;
          background: #fff;
          bottom: -6px;
          right: -6px;
          transform: rotate(-45deg);
        }
      }
    }
  }
}

#chatListContent {
  .chat-list {
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    border-radius: 24px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
    padding: 4px;
    transform-style: preserve-3d;
    transition: all 0.3s ease;
    position: relative;
    z-index: 2;
    scrollbar-width: thin;
    scrollbar-color: #e5e7eb transparent;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }

    &::-webkit-scrollbar-thumb {
      background-color: #e5e7eb;
      border-radius: 3px;

      &:hover {
        background-color: #d1d5db;
      }
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    }
  }

  .chat-title {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #333;

    .username-text {
      max-width: 8em;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      line-height: 1.5;
      padding: 2px 0;
    }

    .friend-tag,
    .private-tag,
    .sender-tag {
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: normal;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      backdrop-filter: blur(4px);
    }

    .friend-tag {
      color: #52c41a;
      background: linear-gradient(135deg, rgba(82, 196, 26, 0.1), rgba(115, 209, 61, 0.15));
    }

    .private-tag {
      color: #1890ff;
      background: linear-gradient(135deg, rgba(24, 144, 255, 0.1), rgba(54, 207, 201, 0.15));
    }

    .sender-tag {
      color: #722ed1;
      background: linear-gradient(135deg, rgba(114, 46, 209, 0.1), rgba(173, 55, 255, 0.15));
    }
  }

  .avatar-wrapper {
    position: relative;

    :deep(.ant-avatar) {
      border: 2px solid rgba(255, 255, 255, 0.8);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      transition: all 0.3s ease;

      &:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
    }
  }

  .chat-desc {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #666;
    font-size: 13px;
    margin-top: 4px;

    .last-message {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      margin-right: 12px;
    }

    .message-time {
      font-size: 12px;
      color: #94a3b8;
      background: rgba(148, 163, 184, 0.1);
      padding: 2px 6px;
      border-radius: 10px;
    }
  }

  :deep(.ant-list-item) {
    cursor: pointer;
    transition: all 0.3s;
    padding: 12px 16px;
    border-radius: 8px;
    margin: 2px 0;
    border: none !important;
    background: transparent;

    &:hover {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.9));
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    &.unread {
      background: rgba(82, 196, 26, 0.05);

      .last-message {
        color: #52c41a;
        font-weight: 500;
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
      color: #ff8e53;
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
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  height: calc(100vh - 200px);
  position: relative;
}

.chat-list {
  height: 100%;
  overflow-y: auto;
  padding: 8px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(82, 196, 26, 0.2);
    border-radius: 3px;

    &:hover {
      background: rgba(82, 196, 26, 0.4);
    }
  }
}

.chat-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin: 4px 0;
  border-radius: 12px;
  background: #fafafa;
  transition: all 0.3s ease;
  cursor: pointer;
  position: relative;
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-1px);
    border-color: #b7eb8f;
    box-shadow: 0 4px 12px rgba(82, 196, 26, 0.08);

    .chat-actions {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.unread {
    background: #f6ffed;
    border-color: #b7eb8f;

    .chat-message {
      color: #52c41a;
      font-weight: 500;
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
      border-color: #52c41a;
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
    background: #52c41a;
    color: white;
    font-size: 12px;
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(82, 196, 26, 0.2);
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
  color: #333;
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
    color: #52c41a;
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
    border-top-color: #52c41a;
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
      color: #52c41a;
      border-color: #52c41a;
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
      color: #52c41a;
      border-color: #52c41a;
    }

    &.active {
      background: #52c41a;
      color: #fff;
      border-color: #52c41a;
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
        border-color: #52c41a;
      }

      &:focus {
        outline: none;
        border-color: #52c41a;
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
      color: #52c41a;
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
    background: #52c41a;
    color: #fff;

    &:hover {
      background: #73d13d;
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
    border-color: #52c41a;
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
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
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
</style>
