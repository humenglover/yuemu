import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { PrivateChat } from '@/api/types'

export const useChatListStore = defineStore('chatList', () => {
  // 存储用户ID
  const userId = ref<number | null>(null)
  // 存储滚动位置
  const scrollPosition = ref(0)
  // 存储当前页码
  const currentPage = ref(1)
  // 存储已加载的聊天列表数据
  const chatListData = ref<PrivateChat[]>([])
  // 存储当前标签
  const currentTab = ref('all')
  // 存储搜索文本
  const searchText = ref('')

  // 更新用户ID
  const updateUserId = (id: number | null | undefined) => {
    userId.value = id ?? null
  }

  // 更新滚动位置
  const updateScrollPosition = (position: number) => {
    scrollPosition.value = position
  }

  // 更新页码
  const updateCurrentPage = (page: number) => {
    currentPage.value = page
  }

  // 更新聊天列表数据
  const updateChatListData = (data: PrivateChat[]) => {
    chatListData.value = data
  }

  // 更新当前标签
  const updateCurrentTab = (tab: string) => {
    currentTab.value = tab
  }

  // 更新搜索文本
  const updateSearchText = (text: string) => {
    searchText.value = text
  }

  // 清除所有状态
  const clearAll = () => {
    userId.value = null
    scrollPosition.value = 0
    currentPage.value = 1
    chatListData.value = []
    currentTab.value = 'all'
    searchText.value = ''
  }

  return {
    userId,
    scrollPosition,
    currentPage,
    chatListData,
    currentTab,
    searchText,
    updateUserId,
    updateScrollPosition,
    updateCurrentPage,
    updateChatListData,
    updateCurrentTab,
    updateSearchText,
    clearAll
  }
})
