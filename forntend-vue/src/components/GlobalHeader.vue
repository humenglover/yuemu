<template>
  <div class="root-container">
    <div class="header-container">
      <div class="header-content">
        <div class="header-left">
          <router-link to="/" class="logo-link">
            <img src="../assets/nuv.png" alt="logo" class="logo-image" />
          </router-link>
        </div>

        <div class="header-center">
          <nav class="nav-menu">
            <router-link to="/" class="nav-item home" :class="{ active: route.path === '/' }">
              <div class="nav-content">
                <i class="fa-solid fa-house" aria-hidden="true"></i>
                <span class="nav-text">主页</span>
              </div>
            </router-link>
            <router-link to="/forum" class="nav-item forum" :class="{ active: route.path === '/forum' }">
              <div class="nav-content">
                <i class="fa-solid fa-file-lines" aria-hidden="true"></i>
                <span class="nav-text">论坛</span>
              </div>
            </router-link>
            <div class="nav-item add-button" v-if="loginUserStore.loginUser.id" @click="handleAddClick">
              <i class="fa-solid fa-plus add-icon" aria-hidden="true"></i>
            </div>
            <router-link to="/chat-redirect" v-if="loginUserStore.loginUser.id" class="nav-item chat" :class="{ active: route.path === '/pc-chat' }">
              <div class="nav-content">
                <i class="fa-solid fa-comments" aria-hidden="true"></i>
                <span class="nav-text">互动</span>
                <span v-if="unreadCounts.totalUnread > 0" class="unread-badge">{{ unreadCounts.totalUnread }}</span>
              </div>
            </router-link>
            <router-link to="/discovery" v-if="loginUserStore.loginUser.id" class="nav-item discovery" :class="{ active: route.path === '/discovery' }">
              <div class="nav-content">
                <i class="fa-solid fa-compass" aria-hidden="true"></i>
                <span class="nav-text">发现</span>
              </div>
            </router-link>

            <router-link v-if="loginUserStore.loginUser?.userRole === 'admin'" to="/admin/manage" class="nav-item admin" :class="{ active: route.path.startsWith('/admin/') }">
              <div class="nav-content">
                <i class="fa-solid fa-user-shield" aria-hidden="true"></i>
                <span class="nav-text">管理中心</span>
              </div>
            </router-link>
            <div class="nav-item menu-button" @click="toggleMobileMenu">
              <i class="fa-solid fa-bars" aria-hidden="true"></i>
            </div>
          </nav>
        </div>

        <div class="header-right">
          <div class="action-buttons">
            <div class="nav-content">
              <router-link to="/message-center" v-if="loginUserStore.loginUser.id" class="nav-item message" :class="{ active: route.path === '/message-center' }">
                <div class="nav-content">
                  <i class="fa-solid fa-bell" aria-hidden="true"></i>
                  <span class="nav-text">消息</span>
                  <span v-if="messageCenterUnreadCount > 0" class="unread-badge">{{ messageCenterUnreadCount }}</span>
                </div>
              </router-link>
              <button @click="handleSearchClick" class="search-button" :class="{ 'search-button-active': isSearchRoute }">
                <i class="fa-solid fa-magnifying-glass search-icon" aria-hidden="true"></i>
                <span class="nav-text">搜索</span>
              </button>
            </div>
          </div>

          <div class="user-section">
            <template v-if="loginUserStore.loginUser.id">
              <div class="user-info" @click="toggleUserMenu">
                <img :src="loginUserStore.loginUser?.userAvatar || getDefaultAvatar(loginUserStore.loginUser?.userName)"
                     class="user-avatar"
                     alt="avatar" />
                <span class="username">{{ loginUserStore.loginUser.userName || '无名' }}</span>
              </div>
              <div v-if="showUserMenu" class="user-menu">
                <router-link to="/my" class="menu-item">
                  <i class="fa-solid fa-user" aria-hidden="true"></i>
                  <span>我的</span>
                </router-link>
                <router-link to="/user/setting" class="menu-item">
                  <i class="fa-solid fa-gear" aria-hidden="true"></i>
                  <span>设置</span>
                </router-link>
                <div class="menu-item" @click="toggleTheme">
                  <i :class="themeIconClass" aria-hidden="true"></i>
                  <span>{{ themeText }}</span>
                </div>
                <a href="https://official.yuemutuku.com" target="_blank" class="menu-item">
                  <i class="fa-solid fa-link" aria-hidden="true"></i>
                  <span>联系我们</span>
                </a>
              </div>
            </template>
            <router-link v-else to="/user/login" class="login-btn">
              <i class="fa-solid fa-user" aria-hidden="true"></i>
              <span>登录</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <div class="mobile-menu" :class="{ 'mobile-menu-active': showMobileMenu }">
      <div class="mobile-menu-content">
        <router-link to="/" class="mobile-menu-item" :class="{ active: route.path === '/' }">
          <i class="nav-icon fa-solid fa-house" aria-hidden="true"></i>
          <span>主页</span>
        </router-link>
        <router-link to="/forum" class="mobile-menu-item" :class="{ active: route.path === '/forum' }">
          <i class="nav-icon fa-solid fa-file-lines" aria-hidden="true"></i>
          <span>论坛</span>
        </router-link>
        <router-link to="/chat-redirect" class="mobile-menu-item" :class="{ active: route.path === '/pc-chat' }">
          <i class="nav-icon fa-solid fa-comments" aria-hidden="true"></i>
          <span>聊天</span>
        </router-link>
        <router-link to="/discovery" class="mobile-menu-item" :class="{ active: route.path === '/discovery' }">
          <i class="nav-icon fa-solid fa-compass" aria-hidden="true"></i>
          <span>发现</span>
        </router-link>
        <router-link to="/message-center" class="mobile-menu-item" :class="{ active: route.path === '/message-center' }">
          <i class="nav-icon fa-solid fa-bell" aria-hidden="true"></i>
          <span>消息</span>
        </router-link>
        <router-link v-if="loginUserStore.loginUser?.userRole === 'admin'" to="/admin/manage" class="mobile-menu-item" :class="{ active: route.path.startsWith('/admin/') }">
          <i class="nav-icon fa-solid fa-user-shield" aria-hidden="true"></i>
          <span>管理中心 </span>
        </router-link>
      </div>
    </div>

    <div v-if="logoutConfirmVisible" class="logout-modal">
      <div class="modal-content">
        <i class="fa-solid fa-circle-xmark" aria-hidden="true"></i>
        <h3>确认退出登录？</h3>
        <p>退出后需要重新登录才能继续使用</p>
        <div class="modal-actions">
          <button class="cancel-btn" @click="logoutConfirmVisible = false">取消</button>
          <button class="confirm-btn" @click="confirmLogout">确认退出</button>
        </div>
      </div>
    </div>

    <UploadActionSheet
      v-model="showActionSheet"
    />
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted, onUnmounted, computed, shallowRef, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useThemeStore } from '@/stores/useThemeStore'
import { userLogoutUsingPost } from '@/api/userController'
import { message } from 'ant-design-vue'
import '@lottiefiles/lottie-player'
import { Icon } from 'vant'
import ThemeToggleButtonNew from '@/components/ThemeToggleButton.vue'
import UploadActionSheet from '@/components/UploadActionSheet.vue'

// 定义props（完全对齐底部组件的props定义）
const props = defineProps<{
  unreadCounts: {
    totalUnread: number
    privateUnread: number
    friendUnread: number
  },
  messageCenterUnreadCount: {
    type: Number,
    default: 0
  }
}>()

// 定义emit（规范事件派发）
const emit = defineEmits<{
  (e: 'logout'): void
  (e: 'navigate', path: string): void
}>()

// 状态管理
const loginUserStore = useLoginUserStore()
const themeStore = useThemeStore()
const route = useRoute()
const router = useRouter()
const showUserMenu = ref(false)
const logoutConfirmVisible = ref(false)
const showActionSheet = ref(false)
const previousRoute = ref('')
const showMobileMenu = ref(false)
const themeIconClass = ref('fa-solid fa-moon')
const themeText = ref('深色主题')

// 初始化主题图标和文字
onMounted(() => {
  const currentTheme = themeStore.isDarkTheme ? 'dark' : 'light'
  if (currentTheme === 'dark') {
    themeIconClass.value = 'fa-solid fa-sun'
    themeText.value = '浅色主题'
  } else {
    themeIconClass.value = 'fa-solid fa-moon'
    themeText.value = '深色主题'
  }
})

// 使用shallowRef优化复杂对象性能（和底部组件保持一致）
const currentUnreadCounts = shallowRef(props.unreadCounts)
const currentMessageCenterUnreadCount = ref(props.messageCenterUnreadCount || 0)

// 深度监听props变化，实时更新未读数（和底部组件逻辑完全一致）
watch(
  () => props.unreadCounts,
  (newVal) => {
    currentUnreadCounts.value = newVal
  },
  { deep: true, immediate: true }
)

watch(
  () => props.messageCenterUnreadCount,
  (newVal) => {
    currentMessageCenterUnreadCount.value = newVal || 0
  },
  { immediate: true }
)

// 计算属性：判断当前是否在搜索路由
const isSearchRoute = computed(() => {
  return route.path === '/search'
})

// 工具函数抽离
const getDefaultAvatar = (userName = 'Guest') => {
  const defaultName = encodeURIComponent(userName)
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${defaultName}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}

// 事件处理函数
const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const toggleTheme = () => {
  // 统一通过 themeStore 切换，各组件仅响应状态变化
  themeStore.toggleTheme()

  // 同步本地图标/文字状态
  if (themeStore.isDarkTheme) {
    themeIconClass.value = 'fa-solid fa-sun'
    themeText.value = '浅色主题'
  } else {
    themeIconClass.value = 'fa-solid fa-moon'
    themeText.value = '深色主题'
  }

  // 触发主题变更事件（兼容性保留）
  window.dispatchEvent(new Event('themeChange'))
}

const handleSearchClick = () => {
  emit('navigate', '/search')
  router.push('/search')
}

const handleAddClick = () => {
  if (!loginUserStore.loginUser?.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  showActionSheet.value = true
}

const confirmLogout = async () => {
  try {
    const res = await userLogoutUsingPost()
    if (res.data.code === 0) {
      logoutConfirmVisible.value = false
      loginUserStore.setLoginUser({ userName: '未登录' })
      emit('logout')
      await router.push('/user/login')
    }
  } catch (error) {
    console.error('退出登录失败:', error)
    message.error('退出登录失败，请稍后重试')
  }
}

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

// 点击外部关闭所有菜单（优化性能）
const closeMenus = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.user-section')) {
    showUserMenu.value = false
  }
  if (!target.closest('.nav-menu')) {
    showMobileMenu.value = false
  }
}

// 处理动作选择
const onActionSelect = async (action: { path: string }) => {
  showActionSheet.value = false
  try {
    emit('navigate', action.path)
    await router.push(action.path)
  } catch (error) {
    console.error('路由跳转失败:', error)
    message.error('页面跳转失败，请稍后重试')
  }
}

// 监听ActionSheet状态
watch(showActionSheet, (newVal) => {
  if (!newVal && previousRoute.value && route.fullPath === previousRoute.value) {
    router.push(previousRoute.value)
    previousRoute.value = ''
  }
})

// 当用户发生上下滚动时，主动收起顶层浮窗组件
const handleScroll = () => {
  // 只在菜单是打开状态时触发响应式更新，避免过渡消耗性能
  if (showUserMenu.value) showUserMenu.value = false
  if (showMobileMenu.value) showMobileMenu.value = false
}

// 生命周期（和底部组件保持一致的清理逻辑）
onMounted(() => {
  document.addEventListener('click', closeMenus)
  // 绑定被动滚动事件提升性能
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenus)
  window.removeEventListener('scroll', handleScroll)
  // 清理所有弹窗状态，防止内存泄漏
  showActionSheet.value = false
  showMobileMenu.value = false
  showUserMenu.value = false
})
</script>

<style scoped>
.root-container {
  position: relative;
}

.header-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  background: var(--header-background);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--header-border);
  border-radius: 0 0 4px 4px;
  box-shadow: 0 4px 30px var(--header-shadow);
  transition: var(--theme-transition);
  z-index: 999; /* 确保header层级正确 */
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-link {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 4px;
  transition: all 0.3s ease;
  transform: translateY(-2px);
}

.logo-image {
  height: 66px;
  width: auto;
  transition: all 0.3s ease;
  opacity: 0.85;
}

.logo-link:hover .logo-image {
  opacity: 1;
  transform: scale(1.05);
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-button {
  display: none !important;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 12px;
  color: var(--header-text);
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  height: 40px;
  position: relative; /* 关键：确保未读数徽章定位正确 */
  overflow: hidden;
}

.nav-content {
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative; /* 关键：未读数徽章的父容器需要定位 */
  z-index: 2;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.nav-item:hover .nav-content {
  transform: scale(1.05);
}

.nav-item.active .nav-content i {
  animation: nav-icon-pulse 0.5s ease-out;
}

@keyframes nav-icon-pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1.1); }
}

.nav-icon {
  font-size: 18px;
}

.nav-text {
  font-size: 14px;
}

.nav-item::after {
  content: "";
  position: absolute;
  height: 32px;
  width: 0;
  left: 50%;
  top: 4px;
  border-radius: 16px;
  transition: all 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
  transform: translateX(-50%);
  opacity: 0;
  z-index: 1;
}

.nav-item.home::after,
.nav-item.forum::after,
.nav-item.chat::after,
.nav-item.discovery::after,
.nav-item.message::after,
.nav-item.admin::after {
  background: linear-gradient(45deg, #2563eb, #3b82f6);
}

.nav-item:not(.add-button):hover::after,
.nav-item:not(.add-button).active::after {
  width: calc(100% - 16px);
  opacity: 1;
}

.nav-item:hover,
.nav-item.active {
  color: white;
  transform: translateY(-1px);
}

.add-button {
  width: 64px;
  height: 32px;
  padding: 0 !important;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
  background: #2563eb;
  border: 2px solid #2563eb;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin: 0 8px;
}

.add-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3) !important;
}

.add-icon {
  color: white;
  font-size: 20px;
  font-weight: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
  margin: auto;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.action-buttons {
  display: flex;
  gap: 24px;
  height: 48px;
  align-items: center;
}

.theme-toggle-container {
  display: none !important;
}

@media screen and (min-width: 769px) {
  .theme-toggle-container {
    display: block;
  }
}

.search-and-theme {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  height: 40px;
  border-radius: 20px;
  border: 1px solid #eeeeee;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  background: rgba(255, 255, 255, 0.8);
}

.search-button:hover {
  border-color: transparent;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.1);
}

.search-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.15);
}

.search-button-active {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-color: transparent;
  color: white;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.search-icon {
  font-size: 16px;
  transition: all 0.3s ease;
}

.search-text {
  transition: all 0.3s ease;
  color: #0a0a0a;
}

/* 暗色模式下搜索按钮样式 */
.dark-theme .search-button {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.dark-theme .search-button:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.3);
}

.dark-theme .search-button:active {
  background: rgba(255, 255, 255, 0.35);
}

.dark-theme .search-button-active {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  border-color: transparent;
  color: white;
}

.dark-theme .search-icon,
.dark-theme .search-text {
  color: #ffffff;
}

@media screen and (max-width: 768px) {
  .search-button {
    padding: 6px 12px;
    height: 36px;
  }
  .search-text {
    display: none;
  }
  /* 确保搜索图标在平板上可见 */
  .search-icon {
    font-size: 18px;
    display: block;
  }
}

.user-section {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px;
  border-radius: 24px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  height: 40px;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.username {
  font-size: 14px;
  color: #333;
  font-weight: normal;
}

.user-menu {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: var(--user-menu-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 4px 20px var(--user-menu-shadow);
  padding: 8px;
  min-width: 140px;
  transition: var(--theme-transition);
  z-index: 100000;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: var(--user-menu-text);
  text-decoration: none;
  border-radius: 8px;
  transition: var(--theme-transition);
  height: 44px;
  line-height: 20px;
  font-size: 14px;
}

.menu-item:hover {
  background: #e4ecf1;
}

.dark-theme .user-menu .menu-item:hover {
  background: #2d3748;
  color: white;
}

.menu-item i {
  font-size: 16px;
}

.login-btn {
  display: flex;
  align-items: center;
  height: 48px;
  gap: 8px;
  padding: 0 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

.logout-modal {
  position: fixed;
  left: 0;
  top: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logout-modal .modal-content {
  position: relative;
  width: min(92vw, 400px);
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 20px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  animation: modalPop 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 2001;
  margin: 20px;
}

.logout-modal h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 12px;
}

.logout-modal p {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.cancel-btn, .confirm-btn {
  padding: 12px 28px;
  border-radius: 16px;
  border: none;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 15px;
}

.cancel-btn {
  background: #f8fafc;
  color: #64748b;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.confirm-btn {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.cancel-btn:hover {
  background: #f1f5f9;
  transform: translateY(-1px);
}

.confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

@keyframes iconBounce {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-6px) scale(1.05);
  }
}

@media screen and (max-width: 768px) {
  .logout-modal .modal-content {
    margin: 16px;
    padding: 24px;
  }

  .logout-modal h3 {
    font-size: 18px;
  }

  .cancel-btn, .confirm-btn {
    padding: 10px 24px;
    font-size: 14px;
  }
}

.modal-container {
  position: fixed;
  left: 0;
  top: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-overlay {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}

.modal-content {
  position: relative;
  width: min(92vw, 560px);
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  animation: modalPop 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 2001;
  margin: 20px;
}

.modal-pet {
  position: absolute;
  top: -75px;
  right: -35px;
  pointer-events: none;
  animation: petBounce 2s ease-in-out infinite;
  z-index: 2002;
}

.modal-items {
  margin: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal-item {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.modal-item:hover {
  background: rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}

.modal-icon {
  font-size: 24px;
  color: #2563eb;
}

.modal-item-content {
  margin-left: 16px;
  flex: 1;
}

.modal-item-title {
  font-size: 16px;
  font-weight: 500;
  color: #2563eb;
  margin-bottom: 4px;
}

.modal-item-desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.4;
}

.modal-cancel {
  margin-top: 12px;
  padding: 14px;
  text-align: center;
  font-size: 15px;
  color: #64748b;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  font-weight: 500;
}

.modal-cancel:hover {
  background: #f8f9fa;
  color: #2563eb;
}

@keyframes modalPop {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes petBounce {
  0%, 100% {
    transform: translateY(0) rotate(3deg) scale(0.8);
  }
  50% {
    transform: translateY(-8px) rotate(-3deg) scale(0.8);
  }
}

@media screen and (max-width: 1200px) {
  .nav-text {
    display: none;
  }

  .nav-item {
    padding: 8px;
    width: 40px;
    height: 40px;
    justify-content: center;
  }

  .nav-icon {
    font-size: 18px;
  }

  .user-info .username {
    display: none;
  }

  .user-info {
    padding: 4px;
    width: 40px;
    justify-content: center;
  }

  .menu-button {
    display: flex !important;
    margin-left: 8px;
  }
}

@media screen and (max-width: 768px) {
  .nav-menu {
    gap: 4px;
  }

  .nav-item {
    padding: 6px;
    width: 36px;
    height: 36px;
  }

  .add-button {
    width: 36px;
    height: 36px;
  }

  .nav-icon {
    font-size: 16px;
  }

  .user-info {
    width: 36px;
  }
}

@media screen and (max-width: 576px) {
  .nav-menu > .nav-item:not(.menu-button):not(.add-button) {
    display: none;
  }

  .header-content {
    padding: 0 16px;
  }
}

.mobile-menu {
  position: fixed;
  top: 72px;
  right: 16px;
  width: 160px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1000;
  opacity: 0;
  transform: translateY(-10px) scale(0.95);
  pointer-events: none;
  padding: 8px;
}

.mobile-menu-active {
  opacity: 1;
  transform: translateY(0) scale(1);
  pointer-events: auto;
}

.mobile-menu-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mobile-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  color: #333;
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.2s ease;
  height: 44px;
  line-height: 20px;
  font-size: 14px;
}

.mobile-menu-item:hover {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

.mobile-menu-item.active {
  color: #2563eb;
  background: rgba(37, 99, 235, 0.1);
}

@media screen and (max-width: 768px) {
  .nav-item.discovery {
    display: none;
  }
}

/* ========== 核心：完全对齐底部组件的未读数徽章样式 ========== */
.unread-badge {
  position: absolute;
  top: 12px;
  right: -12px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #eb2525; /* 和底部保持一致的红色 */
  color: white;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
  z-index: 3;
  /* 小屏幕适配（和底部保持一致） */
  @media screen and (max-width: 768px) {
    top: -4px;
    right: -4px;
    min-width: 16px;
    height: 16px;
    line-height: 16px;
  }
}

/* 确保nav-content作为未读数的父容器有定位 */
.nav-content {
  position: relative;
}
</style>
