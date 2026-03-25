<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <div
        class="header-wrapper"
        v-show="device === DEVICE_TYPE_ENUM.PC && !shouldHideHeader"
      >
        <a-layout-header
          class="header"
          :class="{ 'header-hidden': !showHeader }"
        >
          <GlobalHeader :unread-counts="unreadCounts" :message-center-unread-count="messageCenterStore.unreadMessageData.totalUnread" />
        </a-layout-header>
      </div>
      <a-layout >
        <div
          class="sider-trigger-area"
          :class="[layoutStore.siderSide === 'left' ? 'is-left' : 'is-right']"
          v-show="device === DEVICE_TYPE_ENUM.PC"
          @mouseenter="handleSiderEnter"
        >
          <GlobalSider
            class="sider"
            :class="[layoutStore.siderSide === 'left' ? 'is-left' : 'is-right']"
          />
        </div>

        <div
          v-if="showBackButton && !(device === DEVICE_TYPE_ENUM.MOBILE && shouldHideMobileFooterRoute(route.path))"
          class="back-button"
          @click="handleBack"
        >
          <img src="@/assets/icons/icon-back.svg" alt="back" />
        </div>

        <a-layout-content
          class="content"
          :style="{
            marginTop: device === DEVICE_TYPE_ENUM.PC ? '64px' : '0',
            marginLeft: '0'
          }"
        >
          <router-view v-slot="{ Component, route }">
            <keep-alive>
              <component
                :is="Component"
                v-if="route.meta.keepAlive"
                :key="route.meta.name"
                :message-center-unread-count="messageCenterStore.unreadMessageData.totalUnread"
              />
            </keep-alive>
            <component
              :is="Component"
              v-if="!route.meta.keepAlive"
              :key="route.meta.name"
              :message-center-unread-count="messageCenterStore.unreadMessageData.totalUnread"
            />
          </router-view>
        </a-layout-content>
      </a-layout>
      <a-layout-footer
        v-if="device === DEVICE_TYPE_ENUM.PC && isAtBottom && !shouldHideFooter"
        class="footer"
      >
        <GlobalFooter :unread-counts="unreadCounts" :message-center-unread-count="messageCenterStore.unreadMessageData.totalUnread" />
      </a-layout-footer>

      <a-layout-footer
        v-else-if="device === DEVICE_TYPE_ENUM.MOBILE && !shouldHideMobileFooter"
        class="footer"
      >
        <GlobalFooter :key="`footer-${unreadCounts.totalUnread}`" :unread-counts="unreadCounts" :message-center-unread-count="messageCenterStore.unreadMessageData.totalUnread" />
      </a-layout-footer>
    </a-layout>

    <div v-if="isLoginOrRegisterPage" class="tool-buttons">
      <div
        class="tool-btn home-btn"
        @click="router.push('/home')"
        :title="'回到主页'"
      >
        <svg class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"/>
          <path d="M9 22V12h6v10"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"/>
        </svg>
      </div>
    </div>
    <div v-else class="tool-buttons" v-show="showBackTop || showBackButton || device === DEVICE_TYPE_ENUM.PC">
      <!--      <div-->
      <!--        v-if="showBackButton && !(device === DEVICE_TYPE_ENUM.MOBILE && shouldHideMobileFooterRoute(route.path))"-->
      <!--        class="tool-btn back-btn"-->
      <!--        @click="handleBack"-->
      <!--        :title="'返回上一页'"-->
      <!--      >-->
      <!--        <svg class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">-->
      <!--          <path d="M19 12H5M5 12L12 19M5 12L12 5"-->
      <!--                stroke="currentColor"-->
      <!--                stroke-width="2"-->
      <!--                stroke-linecap="round"-->
      <!--                stroke-linejoin="round"/>-->
      <!--        </svg>-->
      <!--      </div>-->
      <div
        class="tool-btn theme-btn"
        v-if="device === DEVICE_TYPE_ENUM.PC"
        @click="themeStore.toggleTheme"
        :title="themeStore.isDarkTheme ? '切换到亮色模式' : '切换到暗色模式'"
      >
        <svg v-if="themeStore.isDarkTheme" class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M21.752 15.002A9.718 9.718 0 0118 15.75c-5.385 0-9.75-4.365-9.75-9.75 0-1.33.266-2.597.748-3.752A9.753 9.753 0 003 11.25C3 16.635 7.365 21 12.75 21a9.753 9.753 0 009.002-5.998z"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <svg v-else class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M12 3v1m0 16v1m-8-9H3m3.314-5.686L5.5 5.5m12.186.814L18.5 5.5m-12.186 12.186L5.5 18.5m12.186-.814L18.5 18.5M21 12h-1m-4 0a4 4 0 11-8 0 4 4 0 018 0z"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </div>
      <div
        v-show="showBackTop"
        class="tool-btn top-btn"
        @click="scrollToTop"
        :title="'回到顶部'"
      >
        <svg viewBox="0 0 1024 1024" width="28" height="28">
          <path
            d="M696.741825 447.714002c2.717387-214.485615-173.757803-312.227566-187.33574-320.371729-10.857551 5.430775-190.050127 103.168727-187.33274 320.371729-35.297037 24.435488-73.306463 65.1623-67.875688 135.752376 5.430775 70.589076 76.018851 119.460051 103.168726 116.745664 27.152875-2.716387 19.004713-21.7221 19.004713-21.7221l8.148162-38.011425s40.721814 59.732525 51.583363 59.732525h146.609927c13.574938 0 51.585363-59.732525 51.585363-59.732525l8.147162 38.011425s-8.147162 19.005713 19.004713 21.7221c27.148876 2.714388 97.738951-46.156588 103.168727-116.745664s-32.57965-111.316888-67.876688-135.752376z"
            fill="currentColor"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, onBeforeUnmount, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalSider from '@/components/GlobalSider.vue'
import { useThemeStore } from '@/stores/useThemeStore'
import { useLayoutStore } from '@/stores/useLayoutStore'
import { shouldHidePCFooter, shouldHidePCHeader, shouldHideMobileFooter as shouldHideMobileFooterRoute } from '@/constants/route'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { chatListWebSocket } from '@/utils/chatListWebSocket'
import { messageWebSocketService } from '@/utils/messageWebSocketService'
import { useMessageStore } from '@/stores/useMessageStore'
import { useMessageCenterStore } from '@/stores/useMessageCenterStore'
const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const loginUserStore = useLoginUserStore()
const layoutStore = useLayoutStore()

const device = ref<string>('')
const showSider = ref(false)
const showHeader = ref(true)
let lastScrollTop = 0

const isAtBottom = ref(false)
const isRouteLoading = ref(false)
const isScrolled = ref(false)
let loadingTimeout: number | null = null

const showBackTop = ref(false)

const showBackButton = ref(false)

const unreadCounts = ref({
  totalUnread: 0,
  privateUnread: 0,
  friendUnread: 0
})

const messageStore = useMessageStore()
const messageCenterStore = useMessageCenterStore()

const handleUnreadCountsUpdate = (event: CustomEvent) => {
  unreadCounts.value = {
    totalUnread: event.detail.totalUnread || 0,
    privateUnread: event.detail.privateUnread || 0,
    friendUnread: event.detail.friendUnread || 0
  }
}

const handleMessageCenterUnreadCountsUpdate = (event: CustomEvent) => {
  const data = event.detail
  if (data) {
    messageCenterStore.updateUnreadMessageData({
      totalUnread: Number(data.totalUnread) || 0,
      unreadComments: Number(data.unreadComments) || 0,
      unreadLikes: Number(data.unreadLikes) || 0,
      unreadShares: Number(data.unreadShares) || 0,
      unreadSystemNotifies: Number(data.unreadSystemNotifies) || 0
    })
  }
}

const initWebSocket = async () => {
  if (loginUserStore.loginUser?.id) {
    console.log('[BasicLayout] 初始化WebSocket连接...')
    try {
      await chatListWebSocket.connect(loginUserStore.loginUser.id)
      console.log('[BasicLayout] 聊天列表WebSocket连接完成')

      await messageWebSocketService.connect(loginUserStore.loginUser.id)
      console.log('[BasicLayout] 信息中心WebSocket连接完成')

      window.removeEventListener('unreadCountsUpdated', handleUnreadCountsUpdate as EventListener)
      window.removeEventListener('messageUnreadCountsUpdated', handleMessageCenterUnreadCountsUpdate as EventListener)

      window.addEventListener('unreadCountsUpdated', handleUnreadCountsUpdate as EventListener)
      window.addEventListener('messageUnreadCountsUpdated', handleMessageCenterUnreadCountsUpdate as EventListener)

      chatListWebSocket.on('message', (data: any) => {
        if (data?.type === 'UNREAD_COUNTS') {
          unreadCounts.value = {
            totalUnread: data.totalUnread || 0,
            privateUnread: data.privateUnread || 0,
            friendUnread: data.friendUnread || 0
          }
          window.dispatchEvent(new CustomEvent('unreadCountsUpdated', {
            detail: { ...unreadCounts.value }
          }))
        }
      })

      chatListWebSocket.sendMessage({
        type: 'REQUEST_UNREAD_COUNTS'
      })

      messageWebSocketService.requestUnreadCounts()
    } catch (error) {
      console.error('[BasicLayout] WebSocket连接失败:', error)
    }
  }
}

watch(() => loginUserStore.loginUser?.id, (newId) => {
  if (newId) {
    console.log('[BasicLayout] 检测到用户登录，初始化WebSocket连接...')
    initWebSocket()
  } else {
    console.log('[BasicLayout] 用户登出，断开WebSocket连接')
    chatListWebSocket.disconnect()
    messageWebSocketService.disconnect()
    window.removeEventListener('unreadCountsUpdated', handleUnreadCountsUpdate as EventListener)
    window.removeEventListener('messageUnreadCountsUpdated', handleMessageCenterUnreadCountsUpdate as EventListener)
  }
}, { immediate: true })

const shouldHideFooter = computed(() => {
  return shouldHidePCFooter(route.path)
})

const shouldHideHeader = computed(() => {
  return shouldHidePCHeader(route.path)
})

const shouldHideMobileFooter = computed(() => {
  return shouldHideMobileFooterRoute(route.path)
})

onBeforeUnmount(() => {
  if (loadingTimeout) {
    clearTimeout(loadingTimeout)
  }
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', checkIfAtBottom)
})

const checkIfAtBottom = () => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) return;

  const scrollHeight = document.documentElement.scrollHeight;
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const clientHeight = document.documentElement.clientHeight;
  const distanceToBottom = scrollHeight - scrollTop - clientHeight;

  if (scrollHeight <= clientHeight) {
    isAtBottom.value = true;
    return;
  }

  isAtBottom.value = distanceToBottom <= 1;
};

const handleSiderEnter = () => {
  showSider.value = true;
  showHeader.value = true;
};

const handleSiderLeave = () => {
  showSider.value = false;
  if (lastScrollTop > 100) {
    showHeader.value = false;
  }
};

const handleScroll = () => {
  const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop;

  isScrolled.value = currentScrollTop > window.innerHeight / 2;

  if (!showSider.value) {
    if (currentScrollTop > lastScrollTop && currentScrollTop > 100) {
      showHeader.value = false;
    }
    else if (currentScrollTop < lastScrollTop) {
      showHeader.value = true;
    }
  }

  lastScrollTop = currentScrollTop;

  checkIfAtBottom();

  showBackTop.value = currentScrollTop > window.innerHeight / 2;
};

const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

const handleBack = () => {
  router.back()
}

watch(() => route.path, (newPath) => {
  showBackButton.value = newPath !== '/'
})

onMounted(async () => {
  device.value = await getDeviceType()
  window.addEventListener('scroll', handleScroll)
  checkIfAtBottom()
  window.addEventListener('resize', checkIfAtBottom)
  setTimeout(checkIfAtBottom, 100)
  themeStore.initTheme()

  if (!loginUserStore.loginUser?.id) {
    try {
      await loginUserStore.fetchLoginUser()
    } catch (error) {
      console.error('[BasicLayout] 获取用户信息失败:', error)
    }
  }

  if (loginUserStore.loginUser?.id) {
    await initWebSocket()
  }
})

onUnmounted(() => {
  window.removeEventListener('unreadCountsUpdated', handleUnreadCountsUpdate as EventListener)
  window.removeEventListener('messageUnreadCountsUpdated', handleMessageCenterUnreadCountsUpdate as EventListener)
  chatListWebSocket.disconnect()
  messageWebSocketService.disconnect()
})

const isLoginOrRegisterPage = computed(() => {
  return route.path === '/user/login' || route.path === '/user/register'
})

const toggleSider = () => {
  showSider.value = !showSider.value;
  showHeader.value = true;
};
</script>

<style scoped>
.header-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 8;
}

#basicLayout .header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding-inline: 20px;
  background: var(--header-background);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  box-shadow: none;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
  margin-bottom: 1px;
  height: 64px;
  line-height: 64px;
  transition: all 0.3s ease;
}

.header-hidden {
  transform: translateY(-64px);
}

.sider-hidden {
  transform: translateX(-200px);
  box-shadow: none;
  opacity: 0;
}

#basicLayout .content {
  min-height: calc(100vh - 124px);
  background: var(--post-background);
  margin-top: 64px;
  transition: all 0.3s ease;
  width: 100%;
  position: relative;
  overflow: hidden;
}

#basicLayout .content.pc-content {
  margin-left: 0;
}

#basicLayout .footer {
  padding: 16px;
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  text-align: center;
  user-select: none;
  animation: slideUp 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media screen and (min-width: 769px) {
  #basicLayout .footer {
    border-radius: 1.5rem 1.5rem 0 0;
    background: var(--post-background);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    box-shadow: 0 -1px 0 var(--border-color);
  }

  :deep(.footer-content) {
    color: var(--text-secondary);
    font-size: 12px;
    padding: 8px 0;
    opacity: 1;
    transition: all 0.3s ease;
    letter-spacing: 0.01em;
    font-weight: 400;
  }

  :deep(.footer-content:hover) {
    color: var(--text-primary);
  }

  :deep(.footer-links) {
    margin-top: 6px;
    font-size: 12px;
  }

  :deep(.footer-links a) {
    color: var(--text-secondary);
    text-decoration: none;
    transition: all 0.3s ease;
    margin: 0 8px;
    letter-spacing: 0.01em;
    position: relative;
  }

  :deep(.footer-links a:hover) {
    color: var(--text-primary);
  }

  :deep(.footer-links a::after) {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 100%;
    height: 1px;
    background: var(--border-color);
    transform: scaleX(0);
    transform-origin: right;
    transition: transform 0.3s ease;
  }
}

@media screen and (max-width: 768px) {
  #basicLayout .footer {
    background: var(--post-background);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-top: 1px solid var(--border-color);
    box-shadow: 0 -1px 10px var(--shadow-color);
    padding: 0;
    margin: 0;
    z-index: 1;
  }

  #basicLayout .header {
    height: 48px;
    line-height: 48px;
  }

  .header-hidden {
    transform: translateY(-48px);
  }

  #basicLayout .sider {
    top: 48px;
    background: var(--post-background);
    border-radius: 0 6px 6px 0;
  }

  .back-button {
    right: 0.5vh;
    bottom: calc(6vh + 100px);
    width: 32px;
    height: 32px;
  }

  .back-button img {
    width: 20px;
    height: 20px;
  }
}

@keyframes gradientBG {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes hideToShow {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.footer {
  animation: hideToShow 1s ease-out;
}

.sider-trigger-area {
  position: fixed;
  max-width: 88px;
  top: 20%;
  bottom: 60px;
  width: 48px;
  z-index: 100000!important;
  background: transparent;
  transition: all 0.3s ease;
}

.sider-trigger-area.is-left {
  left: 0;
  border-radius: 0 6px 6px 0;
}

.sider-trigger-area.is-right {
  right: 0;
  border-radius: 6px 0 0 6px;
}

@keyframes siderEnter {
  from {
    transform: translateX(-200px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

#basicLayout .sider:not(.sider-hidden) {
  animation: siderEnter 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tool-buttons {
  position: fixed;
  right: 3vh;
  bottom: 8vh;
  display: flex;
  flex-direction: column;
  gap: 12px;
  z-index: 1000;
  animation: slide-in 0.5s ease-in-out both;
}

.tool-btn {
  width: 48px;
  height: 48px;
  background: var(--toggle-background);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px var(--shadow-color);
  border: 1px solid var(--border-color);
}

.tool-btn:hover {
  transform: translateY(-3px) scale(1.05);
  background: var(--toggle-hover-background);
  box-shadow: 0 4px 12px var(--shadow-color);
  border-color: var(--border-color-hover);
}

.tool-btn:active {
  transform: scale(0.95);
}

.emoji {
  font-size: 26px;
  transition: all 0.3s ease;
  line-height: 1;
  filter: none;
}

.tool-btn:hover .emoji {
  transform: scale(1.1);
}

.icon {
  width: 24px;
  height: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tool-btn:hover .icon {
  transform: scale(1.1);
}

.back-btn:hover .icon {
  transform: rotate(-12deg) scale(1.1);
}

.theme-btn {
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  display: none;
}

@media screen and (max-width: 768px) {
  .theme-btn {
    display: flex;
  }
}

.theme-btn .icon {
  width: 24px;
  height: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--text-primary);
}

.theme-btn:hover .icon {
  transform: rotate(15deg) scale(1.1);
  color: var(--text-primary);
}

.theme-btn:active .icon {
  transform: scale(0.95);
}

.dark-theme .theme-btn .icon {
  color: var(--text-primary);
}

.dark-theme .theme-btn:hover .icon {
  color: var(--text-primary);
}

@keyframes floatUpDown {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-2px); }
}

.top-btn .icon {
  animation: floatUpDown 2s ease-in-out infinite;
}

.top-btn:hover .icon {
  animation: none;
  transform: translateY(-2px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

@media screen and (max-width: 768px) {
  .tool-buttons {
    right: 16px;
    bottom: calc(70px + 6vh);
    gap: 8px;
  }

  .tool-btn {
    width: 40px;
    height: 40px;
    border-radius: 14px;
  }

  .icon {
    width: 20px;
    height: 20px;
  }
}

@media screen and (max-width: 375px) {
  .tool-buttons {
    right: 12px;
    bottom: calc(70px + 4vh);
    gap: 6px;
  }

  .tool-btn {
    width: 36px;
    height: 36px;
    border-radius: 12px;
  }

  .icon {
    width: 18px;
    height: 18px;
  }
}

.toolButton,
.backTop,
.back-button {
  display: none;
}
</style>
