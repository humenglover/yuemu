import { ref } from 'vue';
import { defineStore } from 'pinia';
import { getLoginUserUsingGet } from '@/api/userController';
import { useChatListStore } from './useChatListStore';

// 导入 WebSocket 服务
import { messageWebSocketService } from '@/utils/messageWebSocketService';
import { chatListWebSocket } from '@/utils/chatListWebSocket';

/**
 * 存储登录用户信息的状态
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录'
  });

  /**
   * 远程获取登录用户信息
   */
  async function fetchLoginUser() {
    const res = await getLoginUserUsingGet();
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data;
    }
  }

  /**
   * 设置登录用户
   * @param newLoginUser
   */
  function setLoginUser(newLoginUser: any) {
    // 如果用户发生变化，清除聊天列表缓存
    if ((!loginUser.value && newLoginUser) || // 首次登录
      (loginUser.value && newLoginUser && loginUser.value.id !== newLoginUser.id) || // 切换账号
      (loginUser.value && !newLoginUser)) { // 退出登录
      const chatListStore = useChatListStore();
      chatListStore.clearAll();

      // 如果是退出登录，断开所有 WebSocket 连接
      if (loginUser.value && !newLoginUser) {
        disconnectAllWebSockets();
      }
    }
    loginUser.value = newLoginUser;
  }

  /**
   * 清除登录态的方法
   */
  function logout() {
    // 断开所有 WebSocket 连接
    disconnectAllWebSockets();

    // 1. 清除Pinia仓库中存储的用户信息数据，将其恢复到初始状态
    loginUser.value = {
      userName: '未登录',
    };

    // 2. 清除可能存在的本地存储中的登录凭证等相关信息
    localStorage.removeItem('token'); // 假设使用localStorage存储了token作为登录凭证，根据实际情况调整键名
  }

  /**
   * 断开所有 WebSocket 连接
   */
  function disconnectAllWebSockets() {
    try {
      // 断开消息 WebSocket 连接
      messageWebSocketService.disconnect();

      // 断开聊天列表 WebSocket 连接
      chatListWebSocket.disconnect();

      console.log('[WebSocket] 所有 WebSocket 连接已断开');
    } catch (error) {
      console.error('[WebSocket] 断开连接时发生错误:', error);
    }
  }

  return { loginUser, fetchLoginUser, setLoginUser, logout };
});
