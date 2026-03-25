<template>
  <div id="globalFooter">
    <div v-if="device === DEVICE_TYPE_ENUM.PC"
         class="pc-footer"
         :class="{ 'pc-footer-expanded': isFooterExpanded }"
         @click="toggleFooter">
      <div class="wave-container">
        <div class="wave wave1"></div>
        <div class="wave wave2"></div>
        <div class="wave wave3"></div>
      </div>
      <div class="footer-content">
        <p class="copyright">
          © {{ currentYear }} 鹿梦. All rights reserved. <span style="width: 10px"></span>
          <a href="https://beian.miit.gov.cn/" target="_blank">{{ getBeianNumber() }}</a>
        </p>
        <div class="disclaimer" :class="{ 'disclaimer-expanded': isFooterExpanded }">
          <p>本网站部分内容源自网络，仅供学习与参考之用。</p>
          <p>本网站所呈现的全部内容，并不代表本站立场，亦不意味着本站赞同相关观点或对其真实性负责。</p>
          <p>若无意中侵犯了任何企业或个人的知识产权，请通过电子邮箱 <a href="mailto:109484028@qq.com">109484028@qq.com</a> 及时告知我们，一经核实，本网站将立即删除相关内容。</p>
        </div>
      </div>
      <div class="expand-indicator" :class="{ 'expanded': isFooterExpanded }">
        <i class="expand-icon"></i>
      </div>
    </div>

    <div v-else class="wrapper">
      <nav>
        <input type="radio" name="tab" id="home" :checked="active === 0" @change="handleTabChange(0, '/')">
        <input type="radio" name="tab" id="forum" :checked="active === 1" @change="handleTabChange(1, '/forum')">
        <input type="radio" name="tab" id="add">
        <input type="radio" name="tab" id="chat" :checked="active === 3" @change="handleTabChange(3, '/chat-list')">
        <input type="radio" name="tab" id="my" :checked="active === 4" @change="handleTabChange(4, '/my')">

        <label for="home" class="home" :class="{'active': active === 0}">
          <a>
            <i class="fas fa-home" v-show="active === 0"></i>
            <span>首页</span>
          </a>
        </label>
        <label for="forum" class="forum" :class="{'active': active === 1}">
          <a>
            <i class="fas fa-file-alt" v-show="active === 1"></i>
            <span>论坛</span>
          </a>
        </label>
        <label for="add" class="add" @click="handleAddClick">
          <a>
            <div class="add-icon">
              <!-- 使用Font Awesome加号图标 -->
              <i class="fas fa-plus" style="color: white; font-size: 20px;"></i>
            </div>
          </a>
        </label>
        <label for="chat" class="chat" :class="{'active': active === 3}">
          <a>
            <i class="fas fa-comments" v-show="active === 3"></i>
            <span>互动</span>
            <span v-if="unreadCounts.totalUnread > 0" class="unread-badge">{{ unreadCounts.totalUnread }}</span>
          </a>
        </label>
        <label for="my" class="my" :class="{'active': active === 4}">
          <a>
            <i class="fas fa-user" v-show="active === 4"></i>
            <span>我的</span>
            <span v-if="messageCenterUnreadCount > 0" class="unread-badge">{{ messageCenterUnreadCount }}</span>
          </a>
        </label>
        <div class="tab"></div>
      </nav>
    </div>

    <UploadActionSheet v-model="showActionSheet" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch, nextTick } from 'vue'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'
import router from '@/router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import '@lottiefiles/lottie-player'
import UploadActionSheet from './UploadActionSheet.vue'
import { getCurrentYear } from '@/utils/date'
import { useRoute } from 'vue-router'

// 定义props
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

// 定义用于存储设备类型的响应式变量
const device = ref<string>('')
// 用于跟踪当前活动的底部标签页，初始化为 0，对应第一个tabbar项
const active = ref(0)

// 上传选项相关
const showActionSheet = ref(false)
const loginUserStore = useLoginUserStore()

// 添加滚动控制相关变量
const isHidden = ref(false)
let lastScrollTop = 0
const scrollThreshold = 50

// 处理添加按钮点击
const handleAddClick = () => {
  if (!loginUserStore.loginUser.id) {
    router.push('/user/login')
    return
  }
  showActionSheet.value = true
}

const route = useRoute()

// 根据当前路由设置激活的标签
watch(() => route.path, (newPath) => {
  switch (newPath) {
    case '/':
      active.value = 0
      break
    case '/forum':
      active.value = 1
      break
    case '/chat-list':
      active.value = 3
      break
    case '/my':
      active.value = 4
      break
    default:
      // 对于其他路径，只匹配明确属于底部导航的路径，否则不改变当前激活状态
      if (newPath.startsWith('/forum')) {
        active.value = 1
      } else if (newPath.startsWith('/chat-list')) {
        active.value = 3
      } else if (newPath.startsWith('/my')) {
        active.value = 4
      }
      break
  }
}, { immediate: true })

// 监听路由变化以恢复滚动位置
watch(() => route.path, (newPath) => {
  nextTick(() => {
    const savedPosition = sessionStorage.getItem(`${newPath}_scrollPosition`)
    if (savedPosition) {
      window.scrollTo({
        top: parseInt(savedPosition),
        behavior: 'auto'
      })
    }
  })
})

// 处理滚动事件
const handleScroll = () => {
  if (device.value === DEVICE_TYPE_ENUM.PC) return

  const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop

  // 判断滚动方向和距离
  if (Math.abs(currentScrollTop - lastScrollTop) > scrollThreshold) {
    isHidden.value = currentScrollTop > lastScrollTop
    lastScrollTop = currentScrollTop
  }
}

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType()
  window.addEventListener('scroll', handleScroll, { passive: true })

  // Font Awesome 已在 main.ts 中全局引入，无需动态加载
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  showActionSheet.value = false
})

const currentYear = computed(() => getCurrentYear())

// 添加展开/收起状态控制
const isFooterExpanded = ref(false)

// 切换展开/收起状态
const toggleFooter = () => {
  isFooterExpanded.value = !isFooterExpanded.value
}

// 处理标签切换
const handleTabChange = (index: number, path: string) => {
  active.value = index
  const currentScrollPosition = window.pageYOffset || document.documentElement.scrollTop
  sessionStorage.setItem(`${route.path}_scrollPosition`, currentScrollPosition.toString())
  router.push(path)
}

// 根据环境获取备案号
const getBeianNumber = () => {
  // 从环境变量获取环境类型
  const env = import.meta.env.VITE_APP_ENV || 'development'

  if (env === 'production') {
    return '陇ICP备2024012699号-3'
  } else {
    // 开发和测试环境都使用-1号
    return '陇ICP备2024012699号-1'
  }
}
</script>

<style scoped>
#globalFooter {
  z-index: 1;
}

.pc-footer {
  position: relative;
  height: 60px;
  text-align: center;
  background: url('https://yuemu-picture-1328106169.cos.ap-chongqing.myqcloud.com/static/Snipaste_2025-05-03_13-44-18.png') no-repeat center center;
  background-size: 120% auto;
  margin: -16px -16px -18px;
  padding: 12px 36px;
  overflow: hidden;
  z-index: 2;
  cursor: pointer;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px 12px 0 0;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.1);
  user-select: none;
}

.pc-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.1), rgba(0, 0, 0, 0.3));
  pointer-events: none;
  z-index: 1;
  transition: opacity 0.5s ease;
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.pc-footer-expanded {
  height: auto;
  padding: 14px 36px 26px;
  margin-bottom: -14px;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: rgba(255, 255, 255, 0.15);
}

.pc-footer-expanded::before {
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0.15), rgba(0, 0, 0, 0.4));
}

.wave-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 16px;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
  z-index: 2;
}

.wave {
  position: absolute;
  left: 0;
  width: 200%;
  height: 100%;
  background-repeat: repeat-x;
  background-size: 50% 100%;
  transform-origin: center bottom;
}

.wave1 {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 800 88.7' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M800 56.9c-155.5 0-204.9-50-405.5-49.9-200 0-250 49.9-394.5 49.9v31.8h800v-.2-31.6z' fill='%232563eb08'/%3E%3C/svg%3E");
  animation: wave 15s linear infinite;
  z-index: 3;
  opacity: 0.8;
}

.wave2 {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 800 88.7' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M800 56.9c-155.5 0-204.9-50-405.5-49.9-200 0-250 49.9-394.5 49.9v31.8h800v-.2-31.6z' fill='%231d4ed808'/%3E%3C/svg%3E");
  animation: wave 10s linear infinite;
  z-index: 2;
  opacity: 0.6;
}

.wave3 {
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 800 88.7' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M800 56.9c-155.5 0-204.9-50-405.5-49.9-200 0-250 49.9-394.5 49.9v31.8h800v-.2-31.6z' fill='%232563eb05'/%3E%3C/svg%3E");
  animation: wave 7s linear infinite;
  z-index: 1;
  opacity: 0.4;
}

@keyframes wave {
  0% {
    transform: translateX(0) translateZ(0) scaleY(1);
  }
  50% {
    transform: translateX(-25%) translateZ(0) scaleY(0.95);
  }
  100% {
    transform: translateX(-50%) translateZ(0) scaleY(1);
  }
}

.footer-content {
  position: relative;
  z-index: 3;
  padding: 0;
  max-width: 1200px;
  margin: 0 auto;
  pointer-events: none;
}

.footer-content .copyright {
  color: #000000;
  margin: 4px 0;
  font-size: 11px;
  line-height: 1.2;
  transition: all 0.3s ease;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
}

.footer-content .disclaimer {
  opacity: 0;
  max-height: 0;
  overflow: hidden;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  margin: 0;
  padding: 0;
}

.footer-content .disclaimer-expanded {
  opacity: 1;
  max-height: 200px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.footer-content .disclaimer p {
  color: #000000;
  font-size: 11px;
  line-height: 1.6;
  margin: 4px 0;
  text-align: center;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
}

.footer-content a {
  color: #000000;
  text-decoration: none;
  position: relative;
  transition: color 0.3s ease;
  font-size: 11px;
  line-height: 1.2;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.8);
  pointer-events: auto;
}

.footer-content a:hover {
  color: #2563eb;
  text-shadow: 2px 2px 2px rgba(0, 0, 0, 0.8);
}

.footer-content .disclaimer a {
  color: #2563eb;
  opacity: 0.8;
  pointer-events: auto;
}

.footer-content .disclaimer a:hover {
  opacity: 1;
}


.mobile-tabbar :deep(.van-tabbar) {
  height: 50px;
  border-top: none;
  box-shadow: 0 -1px 10px rgba(0, 0, 0, 0.05);
  z-index: 2;
}

.mobile-tabbar :deep(.van-tabbar-item) {
  color: #94a3b8;
  font-size: 10px;
}

.mobile-tabbar .add-button {
  position: relative;
  z-index: 2;
  width: 52px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);

  margin: -40px auto 0;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
  border: 4px solid #fff;
  transition: all 0.3s ease;
}

.mobile-tabbar .add-button .add-icon {
  color: white;
  font-size: 24px;
  font-weight: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  width: 100%;
}

.mobile-tabbar .custom-icon {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mobile-tabbar .custom-icon .nav-icon {
  font-size: 20px;
  font-style: normal;
  transition: all 0.3s ease;
  transform-origin: center;
}

.mobile-tabbar .custom-icon .nav-icon.icon-active {
  color: #2563eb;
  transform: scale(1.2);
}

.mobile-tabbar .custom-icon .nav-icon.icon-active-forum {
  color: #2563eb;
  transform: scale(1.2);
}

.mobile-tabbar .custom-icon .nav-icon.icon-active-chat {
  color: #2563eb;
  transform: scale(1.2);
}

.mobile-tabbar .custom-icon .nav-icon.icon-active-my {
  color: #2563eb;
  transform: scale(1.2);
}

.mobile-tabbar .icon-active {
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.mobile-tabbar-hidden {
  transform: translateY(100%);
}

:deep(.van-tabbar-item) {
  padding: 6px 0 !important;
  height: 52px !important;
  transition: all 0.3s ease;
}

:deep(.van-tabbar-item__icon) {
  margin-bottom: 3px !important;
  height: auto !important;
}

:deep(.van-tabbar-item__text) {
  font-size: 12px !important;
  line-height: 1 !important;
  margin-top: 1px !important;
}

#footerHomeIcon,
#footerForumIcon,
#footerChatIcon,
#footerMyIcon {
  font-size: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 1px;
}

#footerAddButton {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: auto;
  box-shadow: 0 4px 15px rgba(37, 99, 235, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

#footerAddButton .van-icon {
  font-size: 22px;
  color: white;
}

.icon-active {
  color: #2563eb;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active {
  color: #2563eb;
  font-weight: 500;
}

.add-button:active {
  transform: scale(0.9) rotate(-45deg);
}

.tab-text {
  font-size: 12px;
  margin-top: 2px;
  padding-bottom: 2px;
  display: block;
  color: var(--text-primary);
}

:deep(.van-tabbar-item--active) {
  background-color: transparent;
}

:deep(.van-tabbar-item)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 2px 2px 0 0;
}

:deep(.van-tabbar-item--active)::after {
  opacity: 1;
}

#footerActionSheet {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2147483647;
}

.action-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(5px);
  z-index: 2147483647;
}

.action-sheet-wrapper {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translateX(-50%);
  width: min(92vw, 560px);
  transform: translate(-50%, -50%);
  z-index: 2147483647;
}

.action-sheet-content {
  background: linear-gradient(to bottom, #eff6ff, #fff);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  position: relative;
  animation: popIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
}

.action-sheet-header {
  padding: 0 0 24px;
  text-align: center;
}

.header-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-icon .van-icon {
  font-size: 32px;
  color: white;
}

.action-sheet-header .title {
  font-size: 20px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.action-sheet-header .subtitle {
  font-size: 14px;
  color: #64748b;
  margin-top: 16px;
}

.action-sheet-items {
  margin: 32px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-sheet-item {
  display: flex;
  align-items: center;
  padding: 24px;
  padding-left: 32px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.action-sheet-item:hover {
  background: rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.15);
}

.action-sheet-item .van-icon {
  font-size: 32px;
  color: #2563eb;
}

.item-content {
  margin-left: 24px;
  flex: 1;
}

.item-name {
  font-size: 20px;
  font-weight: 500;
  color: #2563eb;
  margin-bottom: 6px;
}

.item-subname {
  font-size: 15px;
  color: #64748b;
  line-height: 1.5;
}

.action-sheet-cancel {
  margin-top: 16px;
  padding: 20px;
  text-align: center;
  font-size: 16px;
  color: #64748b;
  background: #fff;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.05);
  font-weight: 500;
}

.action-sheet-cancel:hover {
  background: #f8f9fa;
  color: #2563eb;
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes fadeInUp {
  from {
    transform: translateY(10px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes iconPop {
  from {
    transform: scale(0.5);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideUpIn {
  from {
    transform: translate(-50%, 20px);
    opacity: 0;
  }
  to {
    transform: translate(-50%, 0);
    opacity: 1;
  }
}

.action-sheet-pet {
  position: absolute;
  top: -85px;
  right: -45px;
  z-index: 1;
  pointer-events: none;
  animation: dogBounce 2s ease-in-out infinite;
}

@keyframes dogBounce {
  0%, 100% {
    transform: translateY(0) rotate(3deg);
  }
  50% {
    transform: translateY(-8px) rotate(-3deg);
  }
}

@media screen and (max-width: 768px) {
  .action-sheet-pet {
    top: -75px;
    right: -35px;
    transform: scale(0.7);
  }

  .action-sheet-content {
    padding: 32px 24px;
  }

  .action-sheet-item {
    padding: 20px;
    padding-left: 24px;
  }

  .item-name {
    font-size: 18px;
  }

  .item-subname {
    font-size: 14px;
  }
}

.icon-active {
  color: #2563eb;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-home {
  color: #2563eb;
  font-weight: 500;
}

.icon-active-forum {
  color: #2563eb;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-forum {
  color: #2563eb;
  font-weight: 500;
}

.icon-active-chat {
  color: #2563eb;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-chat {
  color: #2563eb;
  font-weight: 500;
}

.icon-active-my {
  color: #2563eb;
  transform: scale(1.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.text-active-my {
  color: #2563eb;
  font-weight: 500;
}

:deep(.van-tabbar-item)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  opacity: 0;
  transition: opacity 0.3s;
  border-radius: 2px 2px 0 0;
}

:deep(.van-tabbar-item:nth-child(1).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  opacity: 1;
}

:deep(.van-tabbar-item:nth-child(2).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  opacity: 1;
}

:deep(.van-tabbar-item:nth-child(4).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  opacity: 1;
}

:deep(.van-tabbar-item:nth-child(5).van-tabbar-item--active)::after {
  background: linear-gradient(90deg, #2563eb, #3b82f6);
  opacity: 1;
}

:deep(.van-tabbar__placeholder) {
  height: constant(safe-area-inset-bottom);
  height: env(safe-area-inset-bottom);
}

.expand-indicator {
  position: relative;
  z-index: 3;
  transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

.expand-indicator:hover {
  opacity: 1;
}

.expand-icon {
  display: block;
  width: 8px;
  height: 8px;
  border: solid #999;
  border-width: 0 1.5px 1.5px 0;
  transform: translateY(-2px) rotate(45deg);
  margin: 0 auto;
  transition: all 0.3s ease;
}

.expand-indicator.expanded .expand-icon {
  transform: translateY(2px) rotate(-135deg);
}

.pc-footer:hover .expand-icon {
  border-color: #2563eb;
}

.wrapper {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 48px;
  line-height: 48px;
  background: var(--post-background);
  z-index: 999;
}

.wrapper nav {
  display: flex;
  position: relative;
  height: 100%;
  padding: 0;
  align-items: center;
}

.wrapper nav label {
  flex: 1;
  width: 100%;
  position: relative;
  z-index: 1;
  cursor: pointer;
}

.wrapper nav label a {
  position: relative;
  z-index: 2;
  color: var(--text-primary);
  font-size: 16px; /* 调整默认图标大小，减小过大的问题 */
  font-weight: 500;
  text-decoration: none;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  height: 100%;
  line-height: 1.2;
  gap: 8px;
  padding: 0 8px;
}

/* 确保使用v-show时图标能正确显示 */
.wrapper nav label a i[style*="display: none;"] {
  display: none !important;
}

/* 调整文字大小 - 核心修改 */
.wrapper nav label a span {
  font-size: 14px; /* 增大默认文字大小 */
  margin-top: 2px;
  transition: font-size 0.3s ease; /* 添加过渡效果 */
}

/* 修复选中后文字过大问题 */
.wrapper nav #home:checked ~ label.home a span,
.wrapper nav #forum:checked ~ label.forum a span,
.wrapper nav #chat:checked ~ label.chat a span,
.wrapper nav #my:checked ~ label.my a span {
  font-size: 14px; /* 选中后文字大小保持不变 */
  font-weight: 500;
}

.wrapper nav input {
  display: none;
}

.wrapper nav .tab {
  position: absolute;
  height: 36px;
  width: 18%;
  border-radius: 0;
  transition: 0.6s cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

.wrapper nav #home:checked ~ .tab,
.wrapper nav label.home.active ~ .tab {
  left: 1%;
  background: linear-gradient(45deg, #2563eb, #3b82f6);
  border-radius: 12px;
}

.wrapper nav #forum:checked ~ .tab,
.wrapper nav label.forum.active ~ .tab {
  left: 21%;
  background: linear-gradient(45deg, #2563eb, #3b82f6);
  border-radius: 12px;
}

.wrapper nav #chat:checked ~ .tab,
.wrapper nav label.chat.active ~ .tab {
  left: 61%;
  background: linear-gradient(45deg, #2563eb, #3b82f6);
  border-radius: 12px;
}

.wrapper nav #my:checked ~ .tab,
.wrapper nav label.my.active ~ .tab {
  left: 81%;
  background: linear-gradient(45deg, #2563eb, #3b82f6);
  border-radius: 12px;
}

.wrapper nav #home:checked ~ label.home a,
.wrapper nav #forum:checked ~ label.forum a,
.wrapper nav #chat:checked ~ label.chat a,
.wrapper nav #my:checked ~ label.my a,
.wrapper nav label.home.active a,
.wrapper nav label.forum.active a,
.wrapper nav label.chat.active a,
.wrapper nav label.my.active a {
  color: #fff;
  transform: translateY(-2px);
  transition: color 0.3s ease, transform 0.3s ease; /* 只过渡颜色和位置，不过渡大小 */
}

/* 修复选中后图标过大问题 */
.wrapper nav #home:checked ~ label.home a i,
.wrapper nav #forum:checked ~ label.forum a i,
.wrapper nav #chat:checked ~ label.chat a i,
.wrapper nav #my:checked ~ label.my a i,
.wrapper nav label.home.active a i,
.wrapper nav label.forum.active a i,
.wrapper nav label.chat.active a i,
.wrapper nav label.my.active a i {
  font-size: 16px; /* 选中后图标大小保持不变 */
}

.wrapper nav label.add {
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 修改加号按钮样式 - 核心修改 */
.add-icon {
  width: 52px;
  height: 42px;
  border-radius: 12px; /* 圆角矩形 */
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2563eb; /* 统一使用单一蓝色 */
  border: 3px solid #2563eb;
  transition: 0.3s;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.add-icon:active {
  transform: scale(0.9);
}

@supports (padding-bottom: constant(safe-area-inset-bottom)) {
  .wrapper {
    padding-bottom: constant(safe-area-inset-bottom);
    height: calc(48px + constant(safe-area-inset-bottom));
  }
}

@supports (padding-bottom: env(safe-area-inset-bottom)) {
  .wrapper {
    padding-bottom: env(safe-area-inset-bottom);
    height: calc(48px + env(safe-area-inset-bottom));
  }
}

.unread-badge {
  position: absolute;
  top: -10px;
  right: 2%;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #eb2525;
  color: white;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 500;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.3);
  z-index: 3;
}

.wrapper nav label a {
  position: relative;
}
</style>
