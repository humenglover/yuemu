import { defineStore } from 'pinia'
import { ref } from 'vue'

// 定义未读消息数据类型
interface UnreadMessageData {
  totalUnread: number
  unreadComments: number
  unreadLikes: number
  unreadShares: number
  unreadSystemNotifies: number
}

export const useMessageCenterStore = defineStore('messageCenter', () => {
  // 未读消息状态
  const unreadMessageData = ref<UnreadMessageData>({
    totalUnread: 0,
    unreadComments: 0,
    unreadLikes: 0,
    unreadShares: 0,
    unreadSystemNotifies: 0
  })

  // 更新未读消息数据
  const updateUnreadMessageData = (data: Partial<UnreadMessageData>) => {
    unreadMessageData.value = {
      ...unreadMessageData.value,
      ...data
    }
  }

  // 重置未读消息数据
  const resetUnreadMessageData = () => {
    unreadMessageData.value = {
      totalUnread: 0,
      unreadComments: 0,
      unreadLikes: 0,
      unreadShares: 0,
      unreadSystemNotifies: 0
    }
  }

  return {
    unreadMessageData,
    updateUnreadMessageData,
    resetUnreadMessageData
  }
})
