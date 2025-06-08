<template>
  <div id="basicLayout">
    <GlobalLoading :show="isRouteLoading" />
    <a-layout style="min-height: 100vh">
      <div
        class="header-wrapper"
        v-show="device === DEVICE_TYPE_ENUM.PC"
      >
        <a-layout-header
          class="header"
          :class="{ 'header-hidden': !showHeader }"
        >
          <GlobalHeader/>
        </a-layout-header>
      </div>
      <a-layout>
        <div
          class="sider-trigger-area"
          v-show="device === DEVICE_TYPE_ENUM.PC"
          @mouseenter="handleSiderEnter"
        >
          <!-- 改进三角形展开/折叠按钮 -->
          <div class="sider-toggle" :class="{ 'collapsed': !showSider }" @click="toggleSider">
            <svg class="toggle-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
                 :class="{ 'rotated': showSider }">
              <path d="M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"/>
              <path d="M13 8L9 12M9 12L13 16M9 12H16"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"/>
            </svg>
          </div>
          <GlobalSider
            class="sider"
            :class="{ 'sider-hidden': !showSider }"
            @mouseleave="handleSiderLeave"
          />
        </div>

        <!-- 添加返回按钮 -->
        <div
          v-if="showBackButton"
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
              />
            </keep-alive>
            <component
              :is="Component"
              v-if="!route.meta.keepAlive"
              :key="route.meta.name"
            />
          </router-view>
        </a-layout-content>
      </a-layout>
      <!-- PC端底部，只在滚动到底部时渲染 -->
      <a-layout-footer v-if="device === DEVICE_TYPE_ENUM.PC && isAtBottom" class="footer">
        <GlobalFooter />
      </a-layout-footer>

      <!-- 移动端底部 -->
      <a-layout-footer v-else-if="device === DEVICE_TYPE_ENUM.MOBILE" class="footer">
        <GlobalFooter />
      </a-layout-footer>
    </a-layout>

    <!-- 工具按钮组 -->
    <div class="tool-buttons" v-show="showBackTop || showBackButton || device === DEVICE_TYPE_ENUM.PC">
      <!-- 返回上一步按钮 -->
      <div
        v-if="showBackButton"
        class="tool-btn back-btn"
        @click="handleBack"
        :title="'返回上一页'"
      >
        <svg class="icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M19 12H5M5 12L12 19M5 12L12 5"
                stroke="currentColor"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"/>
        </svg>
      </div>
      <!-- 回到顶部按钮 -->
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
          <path
            d="M423.602441 746.060699c6.47054-6.297579 12.823107-7.017417 21.629121-2.784372 34.520213 16.582259 70.232157 19.645568 107.031855 9.116944 8.118169-2.323476 15.974396-5.475765 23.598677-9.22392 13.712907-6.73648 26.003134 0.8878 26.080116 16.13936 0.109975 22.574907-0.024994 45.142816 0.080982 67.709725 0.031993 7.464316-2.277486 13.322995-9.44387 16.608254-7.277358 3.333248-13.765895 1.961558-19.526595-3.264264-3.653176-3.313253-7.063407-6.897444-10.634601-10.304675-6.563519-6.259588-6.676494-6.25259-10.625603 1.603638-8.437097 16.80121-16.821205 33.623415-25.257302 50.423625-2.489438 4.953882-5.706713 9.196925-11.411426 10.775569-8.355115 2.315478-15.772442-1.070758-20.272427-9.867774-8.774021-17.15313-17.269104-34.453228-25.918153-51.669344-3.750154-7.469315-3.9891-7.479313-10.141712-1.514658-3.715162 3.602187-7.31435 7.326347-11.142486 10.800563-5.571743 5.060858-11.934308 6.269586-18.936728 3.207277-6.82746-2.984327-9.869774-8.483086-9.892769-15.685462-0.070984-23.506697-0.041991-47.018393-0.020995-70.532089 0.007998-4.679944 1.46467-8.785018 4.803916-11.538397z"
            fill="currentColor"/>
        </svg>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, onBeforeUnmount, watch } from 'vue';
import { getDeviceType } from '@/utils/device.ts';
import { DEVICE_TYPE_ENUM } from '@/constants/device.ts';
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
import GlobalSider from '@/components/GlobalSider.vue'
import GlobalLoading from '@/components/GlobalLoading.vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isRouteLoading = ref(false)
const isScrolled = ref(false)
const hoverEnter = ref(false)
let loadingTimeout: number | null = null

// 路由切换时显示加载状态
router.beforeEach(() => {
  // 设置延迟显示，避免快速加载时的闪烁
  loadingTimeout = setTimeout(() => {
    isRouteLoading.value = true
  }, 200)
  return true
})

router.afterEach(() => {
  // 清除延时器并隐藏加载状态
  if (loadingTimeout) {
    clearTimeout(loadingTimeout)
  }
  isRouteLoading.value = false
})

// 组件销毁前清理
onBeforeUnmount(() => {
  if (loadingTimeout) {
    clearTimeout(loadingTimeout)
  }
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', checkIfAtBottom)
})

// 定义用于存储设备类型的响应式变量
const device = ref<string>('');
// 控制侧边栏显示状态
const showSider = ref(false);
// 控制顶部导航栏显示状态
const showHeader = ref(true);
let lastScrollTop = 0;

const isAtBottom = ref(false)

// 新增回到顶部相关的响应式变量和函数
const showBackTop = ref(false)

// 控制返回按钮显示
const showBackButton = ref(false)

// 检查是否滚动到底部
const checkIfAtBottom = () => {
  if (device.value !== DEVICE_TYPE_ENUM.PC) return;

  const scrollHeight = document.documentElement.scrollHeight;
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
  const clientHeight = document.documentElement.clientHeight;
  const distanceToBottom = scrollHeight - scrollTop - clientHeight;

  // 如果页面内容高度小于或等于视窗高度，显示底部
  if (scrollHeight <= clientHeight) {
    isAtBottom.value = true;
    return;
  }

  // 允许有1px的误差，因为某些浏览器可能会有小数点的差异
  isAtBottom.value = distanceToBottom <= 1;
};

// 处理侧边栏鼠标进入
const handleSiderEnter = () => {
  showSider.value = true;
  showHeader.value = true;
};

// 处理侧边栏鼠标离开
const handleSiderLeave = () => {
  // 自动折叠侧边栏
  showSider.value = false;
  if (lastScrollTop > 100) {
    showHeader.value = false;
  }
};

// 修改handleScroll函数
const handleScroll = () => {
  const currentScrollTop = window.pageYOffset || document.documentElement.scrollTop;

  // 更新滚动状态
  isScrolled.value = currentScrollTop > window.innerHeight / 2;

  // 只在非hover状态下控制顶部导航栏的显示/隐藏
  if (!showSider.value) {
    if (currentScrollTop > lastScrollTop && currentScrollTop > 100) {
      showHeader.value = false;
    }
    else if (currentScrollTop < lastScrollTop) {
      showHeader.value = true;
    }
  }

  lastScrollTop = currentScrollTop;

  // 检查是否需要显示底部
  checkIfAtBottom();

  // 新增回到顶部相关的逻辑
  showBackTop.value = currentScrollTop > window.innerHeight / 2;
};

// 滚动到顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
};

// 处理返回按钮点击
const handleBack = () => {
  router.back()
}

// 监听路由变化，控制返回按钮显示
watch(() => route.path, (newPath) => {
  // 在首页不显示返回按钮
  showBackButton.value = newPath !== '/'
})

// 页面加载时获取设备类型并获取数据
onMounted(async () => {
  device.value = await getDeviceType();
  window.addEventListener('scroll', handleScroll);
  // 初始检查一次滚动位置
  checkIfAtBottom();
  // 添加 resize 事件监听，当窗口大小改变时也检查一次
  window.addEventListener('resize', checkIfAtBottom);
  // 添加一个短暂延时，确保在页面完全加载后再次检查
  setTimeout(checkIfAtBottom, 100);
});

// 添加侧边栏切换函数
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
  height: 64px;
  z-index: 8;
}

#basicLayout .header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  padding-inline: 20px;
  background: rgba(255, 255, 255, 0.01);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
  box-shadow: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.01);
  color: var(--black);
  margin-bottom: 1px;
  height: 64px;
  line-height: 64px;
  transition: all 0.3s ease;

}

.header-hidden {
  transform: translateY(-64px);
}

#basicLayout .sider {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-right: 1px solid rgba(255, 255, 255, 0.2);
  padding-top: 20px;
  position: fixed;
  left: 0;
  top: 64px;
  bottom: 60px;
  width: 200px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1002;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
  border-radius: 0 6px 6px 0;
  transform-origin: left center;
}

.sider-hidden {
  transform: translateX(-200px);
  box-shadow: none;
  opacity: 0;
}

#basicLayout .content {
  min-height: calc(100vh - 124px);
  padding: 28px;
  background: linear-gradient(to right, #fefefe, #ffffff);
  margin-top: 64px;
  transition: all 0.3s ease;
  margin-left: 0 !important;
  width: 100%;
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

/* PC端底部样式 */
@media screen and (min-width: 769px) {
  #basicLayout .footer {
    border-radius: 1.5rem 1.5rem 0 0;
    background: rgba(29, 29, 31, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    box-shadow: 0 -1px 0 rgba(255, 255, 255, 0.08);

  }

  :deep(.footer-content) {
    color: rgba(245, 245, 247, 0.8);
    font-size: 12px;
    padding: 8px 0;
    opacity: 1;
    transition: all 0.3s ease;
    letter-spacing: 0.01em;
    font-weight: 400;
  }

  :deep(.footer-content:hover) {
    color: rgba(245, 245, 247, 0.95);
  }

  :deep(.footer-links) {
    margin-top: 6px;
    font-size: 12px;
  }

  :deep(.footer-links a) {
    color: rgba(245, 245, 247, 0.7);
    text-decoration: none;
    transition: all 0.3s ease;
    margin: 0 8px;
    letter-spacing: 0.01em;
    position: relative;
  }

  :deep(.footer-links a:hover) {
    color: rgba(245, 245, 247, 0.95);
  }

  :deep(.footer-links a::after) {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 100%;
    height: 1px;
    background: rgba(245, 245, 247, 0.3);
    transform: scaleX(0);
    transform-origin: right;
    transition: transform 0.3s ease;
  }

  :deep(.footer-links a:hover::after) {
    transform: scaleX(1);
    transform-origin: left;
  }
}

/* 移动端底部样式 */
@media screen and (max-width: 768px) {
  #basicLayout .footer {
    background: rgba(255, 255, 255, 0.85);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 0 -1px 10px rgba(0, 0, 0, 0.05);
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
    background: rgba(255, 255, 255, 0.85);
    border-radius: 0 6px 6px 0;
  }

  .sider-trigger-area {
    top: 48px;
  }

  .back-button {
    right: 0.5vh;
    bottom: calc(6vh + 100px); /* 同样增加移动端的距离，原来是60px */
    width: 32px;
    height: 32px;
  }

  .back-button img {
    width: 20px;
    height: 20px;
  }
}

/* 添加渐变背景动画 */
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

/* 添加底部显示动画 */
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
  max-width: 120px;
  left: 0;
  top: 64px;
  bottom: 60px;
  width: 20px;
  z-index: 100000!important;
  background: transparent;
  border-radius: 0 6px 6px 0;
  transition: all 0.3s ease;
}

/* 修改三角形按钮样式 */
.sider-toggle {
  position: absolute;
  left: 200px;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  z-index: 100001;
}

.sider-toggle.collapsed {
  left: 0;
}

.sider-toggle:hover {
  background: rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
}

.sider-toggle:active {
  transform: translateY(-50%) scale(0.95);
}

.toggle-icon {
  width: 24px;
  height: 24px;
  color: #333;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.toggle-icon.rotated {
  transform: rotate(180deg);
}

.sider-toggle:hover .toggle-icon {
  color: #000;
}

.sider-toggle:hover .toggle-icon.rotated {
  transform: rotate(180deg) scale(1.1);
}

/* 删除不需要的菜单按钮样式 */
.menu-btn,
.menu-btn::after,
.menu-btn.active,
.menu-btn.active .icon {
  display: none;
}

/* 添加进入动画 */
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

/* 工具按钮组样式 */
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
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.tool-btn:hover {
  transform: translateY(-3px) scale(1.05);
  background: rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 12px rgba(255, 142, 83, 0.15);
  border-color: rgba(255, 255, 255, 0.2);
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

/* SVG图标样式 */
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

/* 添加动画效果 */
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

/* 移动端适配 */
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

/* 小屏幕适配 */
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

/* 删除不需要的样式 */
.toolButton,
.backTop,
.back-button {
  display: none;
}
</style>
