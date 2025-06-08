<template>
  <div class="waterfall-picture-list">
    <!-- 根据设备类型显示不同布局 -->
    <template v-if="!isMobile">
      <!-- PC端瀑布流布局 -->
      <div class="masonry-wrapper" ref="masonryRef">
        <!-- 添加空状态组件 -->
        <div v-if="!loading && (!props.dataList || props.dataList.length === 0)" class="empty-state">
          <lottie-player
            src="https://assets10.lottiefiles.com/packages/lf20_AMBEWz.json"
            background="transparent"
            speed="1"
            style="width: 200px; height: 200px;"
            loop
            autoplay
          ></lottie-player>
          <div class="empty-text">
            <h3>暂无图片</h3>
            <p>快来上传一些精彩的照片吧 (｡•́︿•̀｡)</p>
          </div>
        </div>
        <div v-else class="masonry-grid" ref="gridRef">
          <!-- 使用计算后的列数据进行渲染 -->
          <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column">
            <div
              v-for="picture in column"
              :key="picture.id"
              class="masonry-item"
              :style="{
                background: picture.picColor ? `rgba(${parseInt(picture.picColor.slice(2,4), 16)}, ${parseInt(picture.picColor.slice(4,6), 16)}, ${parseInt(picture.picColor.slice(6,8), 16)}, 0.3)` : '#ffffff'
              }"
              @click="doClickPicture(picture)"
            >
              <div class="image-wrapper">
                <div
                  class="aspect-ratio-box"
                  :style="{ paddingTop: `${(1 / (picture.picScale || 1)) * 100}%` }"
                >
                  <!-- 精选标志 -->
                  <div v-if="picture.isFeature === 1" class="feature-badge">
                    <CrownOutlined />
                    <span>精选</span>
                  </div>
                  <img
                    :alt="picture.name"
                    class="masonry-image"
                    :src="picture.thumbnailUrl || picture.url"
                    @load="handleImageLoad"
                    @error="handleImageError(picture)"
                  />
                  <!-- 添加图片内部的信息层 -->
                  <div class="image-overlay">
                    <div class="overlay-top">
                      <div class="view-count">
                        <EyeOutlined />
                        <span>{{ formatNumber(picture.viewCount) }}</span>
                      </div>
                    </div>
                    <div class="overlay-bottom">
                      <div class="picture-user" @click.stop="handleUserClick(picture.user)">
                        <a-avatar class="user-avatar" :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"/>
                        <span class="user-name">{{ picture.user?.userName }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div class="picture-info">
                <div class="picture-title">{{ picture.name }}</div>
              </div>
            </div>
            <!-- 在最短的列末尾添加加载动画 -->
            <div v-if="loading && isShortestColumn(columnIndex)" class="masonry-item loading-item">
              <svg class="loading-camera" viewBox="0 0 100 100">
                <path class="camera-body" d="M25,30H75a8,8,0,0,1,8,8V70a8,8,0,0,1-8,8H25a8,8,0,0,1-8-8V38A8,8,0,0,1,25,30Zm5-10H70a2,2,0,0,1,2,2v4a2,2,0,0,1-2,2H30a2,2,0,0,1-2-2V22A2,2,0,0,1,30,20Z"/>
                <circle class="camera-lens" cx="50" cy="54" r="15"/>
                <circle class="camera-flash" cx="72" cy="42" r="4"/>
              </svg>
            </div>
          </div>
        </div>
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-wrapper">
          <svg class="loading-camera" viewBox="0 0 100 100">
            <path class="camera-body" d="M25,30H75a8,8,0,0,1,8,8V70a8,8,0,0,1-8,8H25a8,8,0,0,1-8-8V38A8,8,0,0,1,25,30Zm5-10H70a2,2,0,0,1,2,2v4a2,2,0,0,1-2,2H30a2,2,0,0,1-2-2V22A2,2,0,0,1,30,20Z"/>
            <circle class="camera-lens" cx="50" cy="54" r="15"/>
            <circle class="camera-flash" cx="72" cy="42" r="4"/>
          </svg>
        </div>
        <!-- 没有更多数据 -->
        <div v-if="isEndOfData" class="no-more-data">
          <span>没有更多了</span>
        </div>
      </div>
    </template>

    <!-- 审核详情弹窗 -->
    <a-modal
      v-model:open="reviewModalVisible"
      :title="getReviewModalTitle(currentPicture?.reviewStatus)"
      :footer="null"
      class="review-modal"
    >
      <div class="review-detail">
        <div class="status-icon-large">
          <ClockCircleOutlined v-if="currentPicture?.reviewStatus === 0" class="pending" />
          <CheckCircleOutlined v-else-if="currentPicture?.reviewStatus === 1" class="approved" />
          <CloseCircleOutlined v-else-if="currentPicture?.reviewStatus === 2" class="rejected" />
        </div>
        <div class="review-message">
          <template v-if="currentPicture?.reviewStatus === 0">
            您的图片正在审核中，请耐心等待...
          </template>
          <template v-else-if="currentPicture?.reviewStatus === 1">
            恭喜！您的图片已通过审核
          </template>
          <template v-else-if="currentPicture?.reviewStatus === 2">
            {{ currentPicture?.reviewMessage || '抱歉，您的图片未通过审核' }}
          </template>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { h, onMounted, onUnmounted, ref, nextTick, computed, watch } from 'vue'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  EyeOutlined,
  LoadingOutlined,
  CrownOutlined,
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'
import '@lottiefiles/lottie-player'
import { getDefaultAvatar } from '@/utils/userUtils.ts'

// 自定义 debounce 函数
const debounce = <T extends (...args: any[]) => void>(fn: T, delay: number) => {
  let timer: ReturnType<typeof setTimeout> | null = null
  return function(this: any, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
      timer = null
    }, delay)
  }
}

const loginUserStore = useLoginUserStore()

onMounted(async () => {
  // 禁用页面初始滚动
  document.body.style.overflowY = 'hidden'

  // 等待图片初始化完成后再启用滚动
  nextTick(() => {
    setTimeout(() => {
      document.body.style.overflowY = 'auto'
      window.addEventListener('scroll', handleWindowScroll)
    }, 500)  // 给予足够的时间让图片加载和布局稳定
  })
})

interface Props {
  dataList: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  isMyPosts?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onLoadMore?: (page: number) => Promise<boolean>
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  onReload: () => {},
  isMyPosts: false,
  canEdit: false,
  canDelete: false,
  onLoadMore: undefined
})

const router = useRouter()

// 新增用于移动端分页的页码变量
const page = ref(1)
const isEndOfData = ref(false)
const isLoading = ref(false)

// 记录上一次滚动位置
let lastScrollTop = 0

// 预加载阈值
const SCROLL_THRESHOLD = 1200  // 增加基础预加载阈值

// 处理滚动事件
const handleWindowScroll = debounce(() => {
  if (isLoading.value || isEndOfData.value || !props.onLoadMore) return

  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 检查是否有列高度差异过大
  const columnHeights = calculateColumnHeights()
  const maxHeight = Math.max(...columnHeights)
  const minHeight = Math.min(...columnHeights)
  const heightDiff = maxHeight - minHeight

  // 如果列高度差异大于一定值，提前触发加载
  const shouldLoadEarly = heightDiff > windowHeight * 0.3  // 降低高度差异的触发阈值

  // 根据列高度差异和滚动位置动态调整加载阈值
  let dynamicThreshold = SCROLL_THRESHOLD

  // 根据滚动速度调整阈值
  const scrollSpeed = Math.abs(lastScrollTop - scrollTop)
  if (scrollSpeed > 50) {  // 如果滚动速度较快
    dynamicThreshold *= 1.5
  }

  // 根据列高度差异调整阈值
  if (shouldLoadEarly) {
    dynamicThreshold *= 1.8  // 增加提前加载的倍数
  }

  // 存储当前滚动位置
  lastScrollTop = scrollTop

  if (documentHeight - scrollTop - windowHeight < dynamicThreshold) {
    loadMore()
  }
}, 50)  // 减少防抖时间，使响应更快速

// 修改图片加载处理函数
const handleImageLoad = (event: Event) => {
  const imgElement = event.target as HTMLImageElement
  if (imgElement) {
    // 设置一个短暂的延时，确保图片完全加载
    setTimeout(() => {
      imgElement.style.opacity = '1'
    }, 50)
  }
}

// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    name: '图片详情',
    params: {
      id: picture.id
    }
  })
}

// 修改图片错误处理函数
const handleImageError = (picture: API.PictureVO, event: Event) => {
  const imgElement = event.target as HTMLImageElement
  if (imgElement && picture.url) {
    imgElement.src = picture.url
  }
}

// 格式化数字的函数，将较大数字转换为带k、w的格式
const formatNumber = (num: number): string => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 判断是否为移动端
const isMobile = ref(getDeviceType() === DEVICE_TYPE_ENUM.MOBILE)

// 处理用户点击
const handleUserClick = (user: API.UserVO | undefined) => {
  if (!user) return
  router.push({
    path: `/user/${user.id}`,
    query: {
      userName: user.userName,
      userAvatar: user.userAvatar,
      userAccount: user.userAccount,
      userProfile: user.userProfile,
      userRole: user.userRole,
      createTime: user.createTime
    }
  })
}

// 加载更多数据
const loadMore = async () => {
  if (isLoading.value || isEndOfData.value || !props.onLoadMore) return
  isLoading.value = true

  const startTime = Date.now()

  try {
    const hasMore = await props.onLoadMore(page.value + 1)
    if (hasMore) {
      page.value++
    } else {
      isEndOfData.value = true
    }
  } catch (error) {
    console.error('加载更多数据失败:', error)
  } finally {
    // 减少最小加载时间
    const loadTime = Date.now() - startTime
    const minLoadTime = 150  // 减少最小加载时间
    if (loadTime < minLoadTime) {
      await new Promise(resolve => setTimeout(resolve, minLoadTime - loadTime))
    }
    isLoading.value = false
  }
}

// 监听滚动事件
onMounted(() => {
  window.addEventListener('scroll', handleWindowScroll)
})

// 组件卸载时移除滚动监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleWindowScroll)
  document.body.style.overflowY = 'auto'
})

// 监听数据列表变化，重置状态
watch(() => props.dataList, (newVal, oldVal) => {
  // 只在数据完全重置时重置状态
  if (oldVal?.length && newVal.length === 0) {
    page.value = 1
    isEndOfData.value = false
    isLoading.value = false
    // 重置时也暂时禁用滚动
    document.body.style.overflowY = 'hidden'
    nextTick(() => {
      setTimeout(() => {
        document.body.style.overflowY = 'auto'
      }, 300)
    })
  }
})

// 瀑布流容器ref
const masonryRef = ref<HTMLElement | null>(null)
const gridRef = ref<HTMLElement | null>(null)

// 计算列数
const getColumnCount = () => {
  const containerWidth = gridRef.value?.clientWidth || window.innerWidth
  const gap = 20 // 列间距
  const minColumnWidth = 300 // 最小列宽

  // 根据容器宽度和最小列宽计算最大可能的列数
  const maxColumns = Math.floor((containerWidth + gap) / (minColumnWidth + gap))

  // 根据容器宽度动态调整列数
  if (containerWidth > 1920) return Math.min(6, maxColumns)
  if (containerWidth > 1600) return Math.min(5, maxColumns)
  if (containerWidth > 1200) return Math.min(4, maxColumns)
  if (containerWidth > 900) return Math.min(3, maxColumns)
  return Math.min(2, maxColumns)
}

// 计算分列数据
const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: API.PictureVO[][] = Array.from({ length: columnCount }, () => [])
  const columnHeights = new Array(columnCount).fill(0)

  // 使用Set来跟踪已经分配的图片ID
  const assignedPictures = new Set()

  props.dataList.forEach((item) => {
    // 检查图片是否已经被分配
    if (assignedPictures.has(item.id)) {
      return
    }

    // 找出当前高度最小的列
    const minHeightIndex = columnHeights.indexOf(Math.min(...columnHeights))
    cols[minHeightIndex].push(item)
    assignedPictures.add(item.id)

    // 更新列高度（根据图片宽高比计算）
    const aspectRatio = item.picWidth / item.picHeight || 1
    const imageHeight = (gridRef.value?.clientWidth || window.innerWidth) / columnCount / aspectRatio
    columnHeights[minHeightIndex] += imageHeight + 120 // 120px是卡片其他内容的高度
  })

  return cols
})

// 使用 ResizeObserver 监听容器大小变化
let resizeObserver: ResizeObserver | null = null

// 处理容器大小变化
const handleContainerResize = debounce(() => {
  if (gridRef.value) {
    // 强制重新计算列布局
    const newColumnCount = getColumnCount()
    const oldColumnCount = columns.value.length

    // 只有当列数发生变化时才重新计算
    if (newColumnCount !== oldColumnCount) {
      // 触发响应式更新
      nextTick(() => {
        // 重新计算列布局会自动触发computed重新执行
        gridRef.value?.clientWidth // 访问属性触发响应式更新
      })
    }
  }
}, 200)

onMounted(() => {
  // 创建 ResizeObserver 实例
  resizeObserver = new ResizeObserver(handleContainerResize)

  // 开始监听容器大小变化
  if (gridRef.value) {
    resizeObserver.observe(gridRef.value)
  }

  // 同时也监听窗口大小变化
  window.addEventListener('resize', handleContainerResize)
})

onUnmounted(() => {
  // 清理 ResizeObserver
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  // 移除窗口事件监听
  window.removeEventListener('resize', handleContainerResize)
})

// 审核弹窗相关
const reviewModalVisible = ref(false)
const currentPicture = ref<API.PictureVO>()
const getReviewModalTitle = (status?: number) => {
  switch (status) {
    case 0:
      return '审核中'
    case 1:
      return '审核通过'
    case 2:
      return '审核未通过'
    default:
      return '审核状态'
  }
}

// 计算列高度
const calculateColumnHeights = () => {
  return columns.value.map(column => {
    return column.reduce((height, item) => {
      const aspectRatio = (item as any).width / (item as any).height || 1
      const imageHeight = (gridRef.value?.clientWidth || window.innerWidth) / columns.value.length / aspectRatio
      return height + imageHeight + 120
    }, 0)
  })
}

// 判断是否是最短的列
const isShortestColumn = (columnIndex: number) => {
  const columnHeights = calculateColumnHeights()
  const minHeight = Math.min(...columnHeights)
  return columnHeights[columnIndex] === minHeight
}
</script>

<style scoped>
.waterfall-picture-list {
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.masonry-wrapper {
  width: 100%;
  max-height: 100vh;
  padding: 0;
  box-sizing: border-box;
}

.masonry-grid {
  display: flex;
  gap: 20px;
  width: 100%;
  padding: 16px;
  margin: 0 auto;
  box-sizing: border-box;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 280px;
  box-sizing: border-box;
}

/* 响应式布局调整 */
@media screen and (max-width: 1920px) {
  .masonry-grid {
    gap: 16px;
  }
  .masonry-column {
    min-width: 260px;
  }
}

@media screen and (max-width: 1600px) {
  .masonry-grid {
    gap: 14px;
  }
  .masonry-column {
    min-width: 240px;
  }
}

@media screen and (max-width: 1400px) {
  .masonry-grid {
    gap: 12px;
    padding: 12px;
  }
  .masonry-column {
    min-width: 220px;
  }
}

@media screen and (max-width: 1200px) {
  .masonry-grid {
    gap: 10px;
    padding: 10px;
  }
  .masonry-column {
    min-width: 200px;
  }
}


:deep(.ant-list-items) {
  width: 100%;
}

:deep(.ant-list-item) {
  width: 100% !important;
  padding: 0 !important;
}
.picture-card :deep(.ant-card-body) {
  background: rgba(255, 255, 255, 0.8);
}

.picture-card :deep(.ant-card-actions) {
  background: rgba(255, 255, 255, 0.8);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  border-radius: 12px;
  background: #f5f5f5;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    to bottom,
    transparent 60%,
    rgba(0, 0, 0, 0.4) 100%
  );
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 12px;
  opacity: 1;
}

.overlay-top {
  display: flex;
  justify-content: flex-end;
}

.view-count {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #fff;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 20px;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(4px);
}

.overlay-bottom {
  display: flex;
  align-items: center;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s ease;
}

.masonry-item:hover .overlay-bottom {
  opacity: 1;
  transform: translateY(0);
}

.picture-user {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #fff;
  max-width: calc(100% - 16px);
  background: rgba(87, 84, 84, 0.4);
  backdrop-filter: blur(4px);
  padding: 2px 12px;
  border-radius: 20px;
}

.user-avatar {
  width: 24px !important;
  height: 24px !important;
  border: 1px solid rgba(255, 255, 255, 0.8);
  flex-shrink: 0;
}

.user-name {
  font-size: 13px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.picture-info {
  padding-left: 12px;

  margin-bottom: -8px;
  background: #fff;
}

.picture-title {
  font-size: 14px;
  line-height: 1.4;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .overlay-bottom {
    opacity: 1;
    transform: translateY(0);
  }

  .picture-user {
    padding: 4px 8px;
  }

  .user-avatar {
    width: 20px !important;
    height: 20px !important;
  }

  .user-name {
    font-size: 12px;
    max-width: 120px;
  }

  .view-count {
    font-size: 11px;
    padding: 3px 6px;
  }
}

.close-button span {
  position: absolute;
  width: 16px;
  height: 2px;
  background-color: #666;
  transition: background-color 0.3s;
}

.close-button span:first-child {
  transform: rotate(45deg);
}

.close-button span:last-child {
  transform: rotate(-45deg);
}

.close-button:hover span {
  background-color: #333;
}

/* 修改无更多数据提示样式 */
.no-more-data {
  margin: auto;
  text-align: center;
  padding: 16px;
  padding-bottom: 88px;
  color: #c4947e;
  font-size: 14px;
  opacity: 0.8;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .no-more-data {
    padding-bottom: 24px;
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* PC端瀑布流样式 */
.masonry-wrapper {
  width: 100%;
  min-height: 100vh;
  padding: 0;
}

.masonry-grid {
  display: flex;
  gap: 20px;
  width: 100%;
  padding: 16px;
  margin: 0 auto;
  box-sizing: border-box;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 280px; /* 减小最小列宽，使布局更灵活 */
  box-sizing: border-box;
}

.masonry-item {
  width: 100%;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.masonry-item:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.1);
}

.masonry-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease-in-out;
  will-change: opacity;
}

.masonry-item:hover .masonry-image {
  transform: scale(1);
}

.picture-info {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  flex: 1;
  display: flex;
  flex-direction: column;
  border-top: 1px solid rgba(255, 255, 255, 0.3);
}


.picture-user {
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.3s ease;
}

.user-avatar {
  width: 40px !important;
  height: 40px !important;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.picture-user:hover .user-avatar {
  border-color: #2997ff;
  transform: scale(1.05);
}

.picture-user span {
  font-size: 15px;
  font-weight: 500;
}

.picture-title {
  font-size: 16px;
  line-height: 1.5;
  color: #1d1d1f;
  margin: 0 0 20px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.action-item .anticon {
  font-size: 18px;
  transition: transform 0.3s ease;
}

.action-item:hover .anticon {
  transform: scale(1.1);
}

.feature-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  background: linear-gradient(135deg, #007AFF 0%, #2997ff 100%);
  padding: 8px 14px;
  border-radius: 12px;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  z-index: 2;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.feature-badge .anticon {
  font-size: 16px;
  color: #fff;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .masonry-item {
    border-radius: 16px;
  }

  .picture-user {
    gap: 8px;
  }

  .user-avatar {
    width: 32px !important;
    height: 32px !important;
  }

  .picture-user span {
    font-size: 14px;
  }

  .picture-title {
    font-size: 15px;
    margin: 0 0 16px;
  }


  .action-item .anticon {
    font-size: 16px;
  }

  .feature-badge {
    padding: 6px 10px;
    font-size: 13px;
    border-radius: 10px;
  }
}

.loading-wrapper {
  display: none;
}

/* 修改加载动画样式 */
.loading-item {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0.9) 100%) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 142, 83, 0.1);
  cursor: default;
  box-shadow: 0 8px 24px rgba(255, 142, 83, 0.08);
}

.loading-item:hover {
  transform: none;
  box-shadow: 0 8px 24px rgba(255, 142, 83, 0.08);
}

.loading-camera {
  width: 60px;
  height: 60px;
  animation: float 1.5s ease-in-out infinite;
}

.camera-body {
  fill: none;
  stroke: #ff8e53;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-dasharray: 200;
  stroke-dashoffset: 200;
  animation: draw 3s ease-in-out infinite;
}

.camera-lens {
  fill: none;
  stroke: #ff8e53;
  stroke-width: 3;
  stroke-dasharray: 100;
  stroke-dashoffset: 100;
  animation: draw 3s ease-in-out infinite 0.5s;
}

.camera-flash {
  fill: none;
  stroke: #ff8e53;
  stroke-width: 3;
  animation: flash 1.5s ease-in-out infinite;
}

@keyframes draw {
  0% {
    stroke-dashoffset: 200;
  }
  45%, 50% {
    stroke-dashoffset: 0;
  }
  95%, 100% {
    stroke-dashoffset: -200;
  }
}

@keyframes flash {
  0%, 100% {
    opacity: 0.3;
  }
  50% {
    opacity: 1;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

/* 添加淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(0.6);
  }
  to {
    opacity: 0.9;
    transform: scale(0.8);
  }
}


/* 精选标志样式 */
.feature-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
  padding: 4px 8px;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 2;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.feature-badge .anticon {
  font-size: 14px;
}


/* 添加空状态样式 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  min-height: 400px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0.9) 100%);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  margin: 16px;

  @media screen and (max-width: 480px) {
    margin: 8px;
    min-height: 300px;
    border-radius: 16px;
  }

  .empty-text {
    margin-top: 24px;

    h3 {
      font-size: 20px;
      color: #1a1a1a;
      margin-bottom: 8px;
      font-weight: 600;
    }

    p {
      font-size: 14px;
      color: #94a3b8;
      margin: 0;
    }
  }
}
</style>


