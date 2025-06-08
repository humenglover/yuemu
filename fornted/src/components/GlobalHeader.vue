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
            <router-link to="/" class="nav-item" :class="{ active: route.path === '/' }">
              <i class="nav-icon">🏠</i>
              <span class="nav-text">主页</span>
            </router-link>
            <router-link to="/forum" class="nav-item" :class="{ active: route.path === '/forum' }">
              <i class="nav-icon">📝</i>
              <span class="nav-text">论坛</span>
            </router-link>
            <div class="nav-item add-button" v-if="loginUserStore.loginUser.id" @click="handleAddClick">
              <div class="add-icon">+</div>
            </div>
            <router-link to="/chat-list" v-if="loginUserStore.loginUser.id" class="nav-item" :class="{ active: route.path === '/chat-list' }">
              <i class="nav-icon">💬</i>
              <span class="nav-text">聊天</span>
            </router-link>
            <router-link to="/message-center" v-if="loginUserStore.loginUser.id" class="nav-item" :class="{ active: route.path === '/message-center' }">
              <i class="nav-icon">🔔</i>
              <span class="nav-text">消息</span>
            </router-link>
            <router-link v-if="loginUserStore.loginUser?.userRole === 'admin'" to="/admin/manage" class="nav-item" :class="{ active: route.path.startsWith('/admin/') }">
              <i class="nav-icon" >⚙️</i>
              <span class="nav-text">管理中心</span>
            </router-link>
            <!-- 小屏幕下的展开按钮 -->
            <div class="nav-item menu-button" @click="toggleMobileMenu">
              <i class="nav-icon">☰</i>
            </div>
          </nav>
        </div>

        <div class="header-right">
          <div class="action-buttons">
            <div class="search-btn" @click="handleSearchClick">
              <i class="search-icon">🔍</i>
              <span>搜索</span>
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
                <router-link to="/user/setting" class="menu-item">
                  <i class="menu-icon">⚙️</i>
                  <span>个人中心</span>
                </router-link>
                <router-link to="/my_space" class="menu-item">
                  <i class="menu-icon">👤</i>
                  <span>我的空间</span>
                </router-link>
                <router-link to="/my_ports" class="menu-item">
                  <i class="menu-icon">📤</i>
                  <span>我的发布</span>
                </router-link>
                <router-link to="/tools" class="menu-item">
                  <i class="menu-icon">🛠️</i>
                  <span>实用工具</span>
                </router-link>
                <router-link to="/games" class="menu-item">
                  <i class="menu-icon">🎮</i>
                  <span>休闲游戏</span>
                </router-link>
                <a href="http://my.lumenglover.com/contact" target="_blank" class="menu-item">
                  <i class="menu-icon">🔗</i>
                  <span>联系我们</span>
                </a>
                <div class="menu-item" @click="showLogoutConfirm">
                  <i class="menu-icon">🚪</i>
                  <span>退出登录</span>
                </div>
              </div>
            </template>
            <router-link v-else to="/user/login" class="login-btn">
              <i class="login-icon">👤</i>
              <span>登录</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端菜单 -->
    <div class="mobile-menu" :class="{ 'mobile-menu-active': showMobileMenu }">
      <div class="mobile-menu-content">
        <router-link to="/" class="mobile-menu-item" :class="{ active: route.path === '/' }">
          <i class="nav-icon">🏠</i>
          <span>主页</span>
        </router-link>
        <router-link to="/forum" class="mobile-menu-item" :class="{ active: route.path === '/forum' }">
          <i class="nav-icon">📝</i>
          <span>论坛</span>
        </router-link>
        <router-link to="/chat-list" class="mobile-menu-item" :class="{ active: route.path === '/chat-list' }">
          <i class="nav-icon">💬</i>
          <span>聊天</span>
        </router-link>
        <router-link to="/message-center" class="mobile-menu-item" :class="{ active: route.path === '/message-center' }">
          <i class="nav-icon">🔔</i>
          <span>消息</span>
        </router-link>
        <router-link v-if="loginUserStore.loginUser?.userRole === 'admin'" to="/admin/manage" class="mobile-menu-item" :class="{ active: route.path.startsWith('/admin/') }">
          <i class="nav-icon">⚙️</i>
          <span>管理中心</span>
        </router-link>
      </div>
    </div>

    <!-- 退出确认弹窗 -->
    <div v-if="logoutConfirmVisible" class="logout-modal">
      <div class="modal-content">
        <div class="modal-icon">🚪</div>
        <h3>确认退出登录？</h3>
        <p>退出后需要重新登录才能继续使用</p>
        <div class="modal-actions">
          <button class="cancel-btn" @click="logoutConfirmVisible = false">取消</button>
          <button class="confirm-btn" @click="confirmLogout">确认退出</button>
        </div>
      </div>
    </div>

    <!-- 添加功能弹框 -->
    <div class="modal-container" v-show="showActionSheet">
      <!-- 遮罩层 -->
      <div class="modal-overlay" @click="showActionSheet = false"></div>

      <!-- 弹框内容 -->
      <div class="modal-content">
        <!-- 宠物动画 -->
        <div class="modal-pet">
          <lottie-player
            src="https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json"
            background="transparent"
            speed="1"
            style="width: 150px; height: 150px;"
            loop
            autoplay
          ></lottie-player>
        </div>

        <!-- 选项列表 -->
        <div class="modal-items">
          <div
            v-for="action in actions"
            :key="action.name"
            class="modal-item"
            @click="onActionSelect(action)"
          >
            <Icon :name="action.icon" class="modal-icon" />
            <div class="modal-item-content">
              <div class="modal-item-title">{{ action.name }}</div>
              <div class="modal-item-desc">{{ action.subname }}</div>
            </div>
          </div>
        </div>

        <!-- 取消按钮 -->
        <div class="modal-cancel" @click="showActionSheet = false">
          取消
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { userLogoutUsingPost } from '@/api/userController'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import '@lottiefiles/lottie-player'
import { Icon } from 'vant'

const loginUserStore = useLoginUserStore()
const route = useRoute()
const router = useRouter()

const showUserMenu = ref(false)
const logoutConfirmVisible = ref(false)
const showActionSheet = ref(false)
const previousRoute = ref('')
const showMobileMenu = ref(false)

const getDefaultAvatar = (userName: string) => {
  const defaultName = userName || 'Guest'
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(defaultName)}&backgroundColor=ffd5dc,ffdfbf,ffd5dc`
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}

const handleSearchClick = () => {
  router.push('/search')
}

const handleAddClick = () => {
  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }
  showActionSheet.value = true
}

const showLogoutConfirm = () => {
  logoutConfirmVisible.value = true
  showUserMenu.value = false
}

const confirmLogout = async () => {
  try {
    const res = await userLogoutUsingPost()
    if (res.data.code === 0) {
      logoutConfirmVisible.value = false
      loginUserStore.setLoginUser({
        userName: '未登录',
      })
      await router.push('/user/login')
    }
  } catch (error) {
    console.error('退出登录失败', error)
  }
}

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
}

// 点击外部关闭所有菜单
const closeMenus = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (!target.closest('.user-section')) {
    showUserMenu.value = false
  }
  if (!target.closest('.nav-menu')) {
    showMobileMenu.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', closeMenus)
})

onUnmounted(() => {
  document.removeEventListener('click', closeMenus)
})

// 监听 ActionSheet 的显示状态
watch(showActionSheet, (newVal) => {
  if (!newVal && previousRoute.value && route.fullPath === previousRoute.value) {
    router.push(previousRoute.value)
    previousRoute.value = ''
  }
})

// 动作面板选项
const actions = [
  {
    name: '发布帖子',
    color: '#ff8e53',
    subname: '分享您的想法和图片',
    icon: 'edit',
    path: '/post/edit'
  },
  {
    name: '上传到公共图库',
    color: '#ff8e53',
    subname: '您的图片将在审核通过后展示给所有用户',
    icon: 'photo-o',
    path: '/add_picture'
  },
  {
    name: '上传到个人空间',
    color: '#ff6b6b',
    subname: '仅您自己可以查看和管理',
    icon: 'user-o',
    path: '/add_picture',
    needSpace: true
  },
]

// 处理动作选择
const onActionSelect = async (action: any) => {
  showActionSheet.value = false  // 先关闭弹框
  if (action.needSpace) {
    try {
      // 获取用户的第一个空间
      const res = await listSpaceVoByPageUsingPost({
        userId: loginUserStore.loginUser.id,
        current: 1,
        pageSize: 1,
      })
      if (res.data?.code === 0) {
        // 如果有空间，则进入上传页面
        if (res.data.data?.records?.length > 0) {
          const space = res.data.data.records[0]
          await router.push({
            path: action.path,
            query: {
              spaceId: space.id,
            },
          })
        } else {
          // 如果没有空间，则跳转到创建空间页面
          await router.push('/add_space')
          message.warn('请先创建空间')
        }
      } else {
        message.error('加载我的空间失败，' + res.data.message)
      }
    } catch (error: any) {
      console.error('获取空间信息失败：', error)
      message.error('获取空间信息失败，请稍后重试')
    }
  } else {
    try {
      await router.push(action.path)
    } catch (error) {
      console.error('路由跳转失败:', error)
      message.error('页面跳转失败，请稍后重试')
    }
  }
}
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
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 0 0 4px 4px;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.05);
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
  height: 32px;
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

/* 菜单按钮默认隐藏 */
.menu-button {
  display: none !important;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 12px;
  color: rgba(0, 0, 0, 0.75);
  text-decoration: none;
  transition: all 0.3s ease;
  font-weight: 500;
  height: 40px;
  line-height: 40px;
  backdrop-filter: blur(4px);
  position: relative;
}

.nav-item:not(.add-button)::after {
  content: "";
  position: absolute;
  bottom: 4px;
  left: 50%;
  width: 0;
  height: 6px;
  background: #ff8e53;
  transition: all 0.3s ease;
  transform: translateX(-50%);
  opacity: 0;
}

.nav-item:not(.add-button):hover::after {
  width: calc(100% - 32px);
  opacity: 1;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.3);
  color: #333;
  transform: translateY(-1px);
}

.nav-item.active {
  color: #ff8e53;
  background: rgba(255, 142, 83, 0.1);
}

.nav-item.active::after {
  display: none;
}

.add-button {
  width: 40px;
  height: 40px;
  padding: 0 !important;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b) !important;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  margin: 0 8px;
  border: none;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.add-button:hover {
  transform: translateY(-2px) rotate(180deg) !important;
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3) !important;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b) !important;
}

.add-icon {
  color: white;
  font-size: 24px;
  font-weight: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
  margin-top: -6px;
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
}

.search-btn, .reminder-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.search-btn:hover, .reminder-btn:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  padding: 8px;
  min-width: 200px;
}

.menu-item {
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

.menu-item:hover {
  background: rgba(255, 142, 83, 0.1);
  color: #ff8e53;
}

.login-btn {
  display: flex;
  align-items: center;
  height: 48px;
  gap: 8px;
  padding: 0 20px;
  border-radius: 24px;
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  color: white;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
}

/* 退出登录弹框样式 */
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
  background: linear-gradient(to bottom, #fff9f6, #fff);
  border-radius: 20px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  animation: modalPop 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 2001;
  margin: 20px;
}

.logout-modal .modal-icon {
  font-size: 48px;
  margin-bottom: 20px;
  animation: iconBounce 2s ease-in-out infinite;
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
  background: linear-gradient(135deg, #ff8e53, #ff6b6b);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.2);
}

.cancel-btn:hover {
  background: #f1f5f9;
  transform: translateY(-1px);
}

.confirm-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(255, 107, 107, 0.3);
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

  .logout-modal .modal-icon {
    font-size: 40px;
  }

  .logout-modal h3 {
    font-size: 18px;
  }

  .cancel-btn, .confirm-btn {
    padding: 10px 24px;
    font-size: 14px;
  }
}

/* 添加功能弹框样式 */
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
  background: linear-gradient(to bottom, #fff9f6, #fff);
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
  background: rgba(255, 142, 83, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
}

.modal-icon {
  font-size: 24px;
  color: #ff8e53;
}

.modal-item-content {
  margin-left: 16px;
  flex: 1;
}

.modal-item-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--color-primary, #ff8e53);
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
  color: #ff6b6b;
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

@media screen and (max-width: 768px) {
  .modal-content {
    margin: 16px;
    padding: 20px;
  }

  .modal-pet {
    transform: scale(0.6);
    top: -60px;
    right: -25px;
  }

  .modal-item {
    padding: 16px;
  }

  .modal-item-title {
    font-size: 15px;
  }

  .modal-item-desc {
    font-size: 12px;
  }
}

/* 响应式样式 */
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

  .search-btn span,
  .user-info .username {
    display: none;
  }

  .search-btn {
    padding: 8px;
    width: 40px;
    height: 40px;
    justify-content: center;
  }

  .user-info {
    padding: 4px;
    width: 40px;
    justify-content: center;
  }

  /* 当导航文字隐藏时显示展开按钮 */
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

  .search-btn {
    width: 36px;
    height: 36px;
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

/* 移动端菜单样式 */
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
  background: rgba(255, 142, 83, 0.1);
  color: #ff8e53;
}

.mobile-menu-item.active {
  color: #ff8e53;
  background: rgba(255, 142, 83, 0.1);
}
</style>
