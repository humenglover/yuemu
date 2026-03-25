<template>
  <Teleport to="body" :disabled="!modelValue">
    <div id="uploadActionSheet" class="custom-action-sheet" v-show="modelValue">
      <!-- 遮罩层 -->
      <div class="action-sheet-overlay" @click="$emit('update:modelValue', false)"></div>

      <!-- PC端弹框样式 -->
      <div class="action-sheet-wrapper pc-wrapper" v-if="isPc">
        <div class="action-sheet-content pc-content">
          <div class="action-sheet-pet">
            <lottie-player
              src="https://assets5.lottiefiles.com/packages/lf20_syqnfe7c.json"
              background="transparent"
              speed="1"
              style="width: 150px; height: 150px;"
              loop
              autoplay
            ></lottie-player>
          </div>
          <div class="action-sheet-items">
            <div
              v-for="action in actions"
              :key="action.name"
              class="action-sheet-item"
              @click="onSelect(action)"
            >
              <van-icon :name="action.icon" />
              <div class="item-content">
                <div class="item-name">{{ action.name }}</div>
                <div class="item-subname">{{ action.subname }}</div>
              </div>
            </div>
          </div>
          <div class="action-sheet-cancel" @click="$emit('update:modelValue', false)">
            取消
          </div>
        </div>
      </div>

      <!-- 移动端抽屉样式 -->
      <div class="action-sheet-drawer mobile-drawer" v-else>
        <div class="drawer-content">
          <!-- 抽屉头部 -->
          <div class="drawer-header">
            <h3 class="drawer-title">发布内容</h3>
            <div class="drawer-close" @click="$emit('update:modelValue', false)">
              <van-icon name="cross" />
            </div>
          </div>

          <!-- 抽屉选项列表 -->
          <div class="drawer-items">
            <div
              v-for="action in actions"
              :key="action.name"
              class="drawer-item"
              @click="onSelect(action)"
            >
              <van-icon :name="action.icon" class="item-icon" />
              <div class="item-content">
                <div class="item-name">{{ action.name }}</div>
                <div class="item-subname">{{ action.subname }}</div>
              </div>
              <van-icon name="arrow" class="item-arrow" />
            </div>
          </div>

          <!-- 抽屉底部 -->
          <div class="drawer-footer">
            <div class="drawer-cancel" @click="$emit('update:modelValue', false)">
              取消
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { message } from 'ant-design-vue'
import '@lottiefiles/lottie-player'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import { getDeviceType } from '@/utils/device'

const props = defineProps({
  modelValue: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

// 响应式变量 - 判断是否为PC端
const isPc = ref(false)

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 动作面板选项
const actions = [
  {
    name: '上传到公共图库',
    color: '#367bf3',
    subname: '图片将在审核通过后展示给所有用户',
    icon: 'photo-o',
    path: '/add_picture'
  },
  {
    name: '发布帖子',
    color: '#367bf3',
    subname: '分享您的想法和图片',
    icon: 'edit',
    path: '/post/edit'
  },
]

// 初始化 - 判断设备类型
onMounted(async () => {
  const deviceType = await getDeviceType()
  isPc.value = deviceType === DEVICE_TYPE_ENUM.PC

  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})

// 处理窗口大小变化
const handleResize = async () => {
  const deviceType = await getDeviceType()
  isPc.value = deviceType === DEVICE_TYPE_ENUM.PC
}

// 处理选项选择
const onSelect = async (action: any) => {
  emit('update:modelValue', false)  // 先关闭弹框/抽屉

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
.custom-action-sheet {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2147483647;
  font-family: var(--font-family-base);
  transition: var(--theme-transition);
}

/* 遮罩层样式 */
.action-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: var(--comment-drawer-backdrop);
  backdrop-filter: blur(5px);
  z-index: 2147483647;
  transition: opacity 0.3s ease;
}

/* ===================== PC端弹框样式 ===================== */
.pc-wrapper {
  position: fixed;
  left: 50%;
  top: 50%;
  transform: translateX(-50%);
  width: min(92vw, 560px);
  transform: translate(-50%, -50%);
  z-index: 2147483648;
}

.pc-content {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 8px 30px var(--shadow-color);
  position: relative;
  animation: popIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: visible;
  border: 1px solid var(--border-color);
}

/* 宠物动画样式 */
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

.pc-content .action-sheet-items {
  margin: 32px 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pc-content .action-sheet-item {
  display: flex;
  align-items: center;
  padding: 24px;
  padding-left: 32px;
  border-radius: 16px;
  background: var(--card-background);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  box-shadow: 0 2px 8px var(--shadow-color);
}

.pc-content .action-sheet-item:hover {
  background: var(--hover-background);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(54, 123, 243, 0.15);
}

.pc-content .action-sheet-item .van-icon {
  font-size: 32px;
  color: #367bf3;
}

.pc-content .item-content {
  margin-left: 24px;
  flex: 1;
}

.pc-content .item-name {
  font-size: 20px;
  font-weight: 500;
  color: #367bf3;
  margin-bottom: 6px;
}

.pc-content .item-subname {
  font-size: 15px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.pc-content .action-sheet-cancel {
  margin-top: 16px;
  padding: 20px;
  text-align: center;
  font-size: 16px;
  color: var(--text-secondary);
  background: var(--card-background);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  font-weight: 500;
}

.pc-content .action-sheet-cancel:hover {
  background: var(--hover-background);
  color: #ff6b6b;
}

/* ===================== 移动端抽屉样式 ===================== */
.mobile-drawer {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  z-index: 2147483648;
}

.drawer-content {
  color: var(--link-color);
  background: var(--card-background);
  border-radius: 20px 20px 0 0;
  transform: translateY(100%);
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s forwards;
  border-top: 1px solid var(--border-color);
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

/* 抽屉头部 */
.drawer-header {
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
}

.drawer-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.drawer-close {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--hover-background);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.drawer-close .van-icon {
  font-size: 16px;
  color: var(--text-secondary);
}

/* 抽屉选项列表 */
.drawer-items {
  flex: 1;
  padding: 10px 0;
  overflow-y: auto;
}

.drawer-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.drawer-item:hover {
  background-color: var(--hover-background);
}

.drawer-item .item-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  background-color: #367bf3;
  color: #fff !important;
  font-size: 20px !important;
}

.drawer-item .item-content {
  flex: 1;
}

.drawer-item .item-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.drawer-item .item-subname {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.drawer-item .item-arrow {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 抽屉底部 */
.drawer-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-color);
}

.drawer-cancel {
  width: 100%;
  height: 44px;
  border-radius: 8px;
  background: var(--hover-background);
  border: 1px solid var(--border-color);
  font-size: 16px;
  color: var(--text-primary);
  cursor: pointer;
  transition: background-color 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.drawer-cancel:hover {
  background-color: var(--card-background);
}

/* ===================== 响应式适配 ===================== */
@media screen and (max-width: 768px) {
  /* PC端弹框在小屏幕下的适配 */
  .pc-content {
    padding: 32px 24px;
  }

  .pc-content .action-sheet-pet {
    top: -75px;
    right: -35px;
    transform: scale(0.7);
  }

  .pc-content .action-sheet-item {
    padding: 20px;
    padding-left: 24px;
  }

  .pc-content .item-name {
    font-size: 18px;
  }

  .pc-content .item-subname {
    font-size: 14px;
  }

  /* 移动端安全区域适配 */
  .drawer-content {
    padding-bottom: env(safe-area-inset-bottom);
    padding-bottom: constant(safe-area-inset-bottom);
  }

  .drawer-footer {
    padding-bottom: calc(16px + env(safe-area-inset-bottom));
    padding-bottom: calc(16px + constant(safe-area-inset-bottom));
  }
}

/* PC端媒体查询 */
@media screen and (min-width: 769px) {
  .mobile-drawer {
    display: none;
  }
}
</style>
