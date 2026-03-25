import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
// import { getUnreadCountUsingGet } from '@/api/messageCenterController'
import { useLoginUserStore } from './useLoginUserStore'

export const useMessageStore = defineStore('message', () => {
  const loginUserStore = useLoginUserStore()

  // 未读消息状态
  const unreadCounts = ref({
    totalUnread: 0,
  })

  const isLoading = ref(false)

  // 计算属性：是否有未读消息
  const hasUnreadMessages = computed(() => unreadCounts.value.totalUnread > 0)

  // 获取未读消息数量
  const fetchUnreadCount = async () => {
    if (!loginUserStore.loginUser?.id) {
      // 未登录时清空消息
      unreadCounts.value = {
        totalUnread: 0,
      }
      return
    }

    if (isLoading.value) return

    try {
      isLoading.value = true
      // const res = await getUnreadCountUsingGet()
      // if (res.data?.code === 0 && res.data.data) {
      //   unreadCounts.value = {
      //     totalUnread: res.data.data.totalUnread || 0,
      //   }
      // }
    } catch (error) {
      console.error('获取未读消息数量失败:', error)
    } finally {
      isLoading.value = false
    }
  }

  // 手动刷新
  const refreshUnreadCount = () => {
    fetchUnreadCount()
  }

  // 清除消息（登出时使用）
  const clearMessages = () => {
    unreadCounts.value = {
      totalUnread: 0,
    }
  }

  return {
    unreadCounts,
    hasUnreadMessages,
    isLoading,
    fetchUnreadCount,
    refreshUnreadCount,
    clearMessages
  }
})
