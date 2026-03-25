<template>
  <div class="post-list">
    <div class="masonry-wrapper" ref="masonryRef">
      <div v-if="!loading && (!props.dataList || props.dataList.length === 0)" class="empty-state">
        <img :src="emptyImage" alt="暂无内容" class="empty-img">
        <div class="empty-text">
          <h3>暂无帖子</h3>
          <p>快来发布一些有趣的内容吧 (｡•́︿•̀｡)</p>
        </div>
      </div>
      <div v-else class="masonry-grid" ref="gridRef" :class="{
          'single-column': streamLayout.value === 'single',
          'grid-column': streamLayout.value === 'grid'
        }">
        <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column">
          <div v-if="loading" class="column-skeleton">
            <div v-for="(_, index) in 3" :key="index" class="skeleton-item">
              <div class="skeleton-image" :style="{ paddingTop: `${getSkeletonImagePadding()}%` }"></div>
              <div class="skeleton-title"></div>
            </div>
          </div>
          <div v-else>
            <div
              v-for="post in column"
              :key="post.id"
              class="masonry-item"
              @click="handlePostClick(post)"
            >
              <div class="note-card">
                <div v-if="props.showStatus" class="status-dot-wrap" @click.stop="handleStatusClick(post)">
                  <div class="status-dot" :class="['status-' + post.status]"></div>
                </div>
                <div class="note-image-wrapper">
                  <div
                    class="aspect-ratio-box"
                    :style="{
                      paddingTop: `${getPaddingTop(post)}%`
                    }"
                  >
                    <div v-if="!post.imageLoaded" class="skeleton-wrapper">
                      <div class="skeleton-image"></div>
                    </div>
                    <div v-if="post.coverUrl && !post.hdImageLoaded" class="blur-image-wrapper">
                      <img
                        :src="getThumbnailUrl(post.coverUrl)"
                        :alt="post.title || ''"
                        class="blur-image"
                        @load="handleBlurImageLoad(post)"
                      >
                    </div>
                    <img
                      v-show="!loading"
                      v-if="post.coverUrl"
                      :src="post.coverUrl"
                      :alt="post.title || ''"
                      class="note-image"
                      :class="{ loaded: post.hdImageLoaded }"
                      @load="handleImageLoad(post, $event)"
                      @error="handleImageError(post)"
                    />
                    <img
                      v-show="!loading"
                      v-else
                      :src="getPostCover(post)"
                      :alt="post.title || ''"
                      class="note-image text-cover"
                      :class="{ loaded: post.imageLoaded }"
                      @load="handleImageLoad(post, $event)"
                      @error="handleImageError(post)"
                    />
                  </div>
                </div>
                <div class="note-info">
                  <div class="author-section">
                    <img
                      class="author-avatar"
                      :src="post.user?.userAvatar || getDefaultAvatar(post.user?.userName || '')"
                      :alt="post.user?.userName || ''"
                      :class="{ loaded: post.avatarLoaded }"
                      @load="() => { post.avatarLoaded = true }"
                    >
                    <span class="author-name">{{ post.user?.userName || '未知用户' }}</span>
                    <span class="view-count">
                      <EyeOutlined class="view-icon" />
                      {{ formatNumber(Number(post.viewCount) || 0) }}
                    </span>
                  </div>
                  <div class="note-title">{{ post.title || '无标题' }}</div>
                  <div class="post-meta">
                    <span v-if="post.category" class="category-tag">#{{ post.category }}</span>
                    <span class="post-time">{{ formatTime(post.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <a-modal
      v-model:open="showReviewModal"
      :title="getReviewModalTitle(currentPost?.status)"
      :footer="null"
      :closable="true"
      class="review-modal"
      width="340px"
    >
      <div class="review-content">
        <component
          :is="getReviewIcon(currentPost?.status)"
          :class="['review-icon', getReviewStatusClass(currentPost?.status)]"
        />
        <h3 class="review-title">{{ getReviewModalTitle(currentPost?.status) }}</h3>
        <p class="review-message">{{ getReviewMessage(currentPost?.status, currentPost?.reviewMessage) }}</p>
        <a-button type="primary" class="confirm-btn" @click="showReviewModal = false">
          我知道了
        </a-button>
      </div>
    </a-modal>
    <a-modal
      v-model:open="showRejectModal"
      :title="null"
      :footer="null"
      :closable="false"
      class="reject-modal"
      width="340px"
    >
      <div class="reject-content">
        <CloseCircleOutlined class="reject-icon" />
        <h3 class="reject-title">审核未通过</h3>
        <p class="reject-message">{{ currentPost?.reviewMessage || '内容不符合社区规范，请修改后重新发布' }}</p>
        <a-button type="primary" class="confirm-btn" @click="showRejectModal = false">
          我知道了
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { LikeOutlined, EyeOutlined, CommentOutlined, FileTextOutlined, CloseCircleOutlined, CheckCircleOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { likePostUsingPost } from '@/api/postController'
import { getTextCover } from '@/utils/textCoverGenerator'
import { computed, onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { throttle } from 'lodash'
import emptyImage from '@/assets/illustrations/empty.png'
const router = useRouter()

interface PostWithLoadState extends API.Post {
  imageLoaded?: boolean
  hdImageLoaded?: boolean
  avatarLoaded?: boolean
  imageAspectRatio?: number
}

interface Props {
  dataList?: PostWithLoadState[]
  loading?: boolean
  showStatus?: boolean
  isEndOfData?: boolean
  onLoadMore?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showStatus: false,
  isEndOfData: false,
  onLoadMore: () => {}
})

// 调试：打印父组件传递的数据
console.log('PostList props:', props)
if (props.dataList && props.dataList.length > 0) {
  console.log('First post data:', props.dataList[0])
  console.log('First post status:', props.dataList[0].status)
}

const emit = defineEmits(['update:dataList'])

const masonryRef = ref(null)
const gridRef = ref(null)
const showRejectModal = ref(false)
const showReviewModal = ref(false)
const currentPost = ref(null)
const isScrollLoading = ref(false)
const triggerUpdate = ref(0)

const streamLayout = ref('waterfall')
const gridAspectRatio = ref('4:3')

const loadStreamLayout = () => {
  const savedLayout = localStorage.getItem('streamLayout')
  if (savedLayout) {
    streamLayout.value = savedLayout
  }
  const savedAspectRatio = localStorage.getItem('gridAspectRatio')
  if (savedAspectRatio) {
    gridAspectRatio.value = savedAspectRatio
  }
}

const handleStreamLayoutChange = (event) => {
  if (event && event.detail) {
    streamLayout.value = event.detail.layout || streamLayout.value
    if (event.detail.aspectRatio && streamLayout.value === 'grid') {
      gridAspectRatio.value = event.detail.aspectRatio
      localStorage.setItem('gridAspectRatio', event.detail.aspectRatio)
    }
    localStorage.setItem('streamLayout', streamLayout.value)
  }
  recalculateLayout()
}

const formatNumber = (num) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

const getLimitedAspectRatio = (post) => {
  switch(streamLayout.value) {
    case 'waterfall':
      let aspectRatio = 1;
      if (post.imageAspectRatio) {
        aspectRatio = post.imageAspectRatio;
      }
      return Math.max(0.7, Math.min(aspectRatio, 1.2));
    case 'grid':
      switch(gridAspectRatio.value) {
        case '4:3': return 4/3;
        case '3:4': return 3/4;
        case '1:1': return 1;
        default: return 4/3;
      }
    case 'single':
      let singleAspectRatio = 1;
      if (post.imageAspectRatio) {
        singleAspectRatio = post.imageAspectRatio;
      }
      return Math.max(0.5, Math.min(singleAspectRatio, 2.0));
    default:
      let defaultAspectRatio = 1;
      if (post.imageAspectRatio) {
        defaultAspectRatio = post.imageAspectRatio;
      }
      return Math.max(0.7, Math.min(defaultAspectRatio, 1.2));
  }
}

const getPaddingTop = (post) => {
  const aspectRatio = getLimitedAspectRatio(post);
  return (1 / aspectRatio) * 100;
}

const getSkeletonImagePadding = () => {
  switch(gridAspectRatio.value) {
    case '4:3': return 75;
    case '3:4': return 133.33;
    case '1:1': return 100;
    default: return 75;
  }
}

const getThumbnailUrl = (url) => {
  if (!url) return ''
  const ext = url.split('.').pop()
  const baseUrl = url.replace(`.${ext}`, '')
  return `${baseUrl}_thumbnail.${ext}`
}

const handleLike = async (post) => {
  try {
    if (!post.id) return
    await likePostUsingPost({ id: post.id })
    if (typeof post.isLiked === 'number') {
      post.isLiked = post.isLiked === 1 ? 0 : 1
    }
    const currentCount = parseInt(post.likeCount || '0')
    post.likeCount = (currentCount + (post.isLiked === 1 ? 1 : -1)).toString()
  } catch (error) {
    message.error('点赞失败：' + error.message)
  }
}

const handlePostClick = (post) => {
  if (!post?.id) {
    message.error('无效的帖子')
    return
  }
  router.push({
    name: 'PostDetail',
    params: { id: post.id }
  })
}

const handleBlurImageLoad = (post) => {
  post.imageLoaded = true
}

const handleImageLoad = (post, event) => {
  post.hdImageLoaded = true
  post.imageLoaded = true
  const img = event.target
  if (img.naturalWidth && img.naturalHeight) {
    post.imageAspectRatio = img.naturalWidth / img.naturalHeight
  }
  requestAnimationFrame(() => {
    setTimeout(() => {
      recalculateLayout()
    }, 50)
  })
}

const handleImageError = (post) => {
  post.imageLoaded = true
  post.hdImageLoaded = true
  requestAnimationFrame(() => {
    setTimeout(() => {
      recalculateLayout()
    }, 50)
  })
}

const getColumnCount = () => {
  const width = window.innerWidth || document.documentElement.clientWidth
  switch(streamLayout.value) {
    case 'single':
      return 1
    default:
      if (width >= 1200) return 4
      if (width >= 768) return 3
      return 2
  }
}

const columns = computed<PostWithLoadState[][]>(() => {
  const columnCount = getColumnCount()
  const cols: PostWithLoadState[][] = []
  for (let i = 0; i < columnCount; i++) {
    cols.push([])
  }
  if (props.loading || !props.dataList || props.dataList.length === 0) {
    return cols
  }
  if (streamLayout.value === 'grid') {
    props.dataList.forEach((item, index) => {
      const columnIndex = index % columnCount
      cols[columnIndex].push(item)
    })
  } else {
    const columnHeights = []
    for (let i = 0; i < columnCount; i++) {
      columnHeights.push(0)
    }
    props.dataList.forEach((item) => {
      let minHeightIndex = 0
      for (let i = 1; i < columnCount; i++) {
        if (columnHeights[i] < columnHeights[minHeightIndex]) {
          minHeightIndex = i
        }
      }
      cols[minHeightIndex].push(item)
      const containerWidth = gridRef.value?.clientWidth || document.documentElement.clientWidth
      const imageWidth = (containerWidth / columnCount) - 16
      const limitedAspectRatio = getLimitedAspectRatio(item)
      const imageHeight = imageWidth / limitedAspectRatio
      const itemHeight = imageHeight + 120
      columnHeights[minHeightIndex] += itemHeight
    })
  }
  return cols
})

const recalculateLayout = () => {
  requestAnimationFrame(() => {
    nextTick(() => {
      triggerUpdate.value++
    })
  })
}

const handleContainerResize = throttle(() => {
  if (gridRef.value) {
    recalculateLayout()
  }
}, 200)

const checkShouldLoadMore = () => {
  if (props.loading || props.isEndOfData || isScrollLoading.value) return false
  const container = document.querySelector('.post-list')
  if (!container) return false
  const rect = container.getBoundingClientRect()
  const bottomOffset = rect.bottom - window.innerHeight
  return bottomOffset < 100
}

const handleScroll = throttle(async () => {
  if (checkShouldLoadMore()) {
    isScrollLoading.value = true
    try {
      await props.onLoadMore?.()
    } finally {
      isScrollLoading.value = false
    }
  }
}, 200)

onMounted(() => {
  loadStreamLayout()
  const resizeObserver = new ResizeObserver(handleContainerResize)
  if (gridRef.value) {
    resizeObserver.observe(gridRef.value)
  }
  window.addEventListener('resize', handleContainerResize)
  window.addEventListener('streamLayoutChanged', handleStreamLayoutChange)
  const container = document.querySelector('.post-list')
  if (container) {
    container.addEventListener('scroll', handleScroll)
  }
  onUnmounted(() => {
    resizeObserver.disconnect()
    window.removeEventListener('resize', handleContainerResize)
    window.removeEventListener('streamLayoutChanged', handleStreamLayoutChange)
    if (container) {
      container.removeEventListener('scroll', handleScroll)
    }
  })
})

watch(() => props.dataList, () => {
  nextTick(() => {
    if (checkShouldLoadMore()) {
      handleScroll()
    }
  })
}, { deep: true })

const handleStatusClick = (post) => {
  if (post.status !== undefined && post.status !== null) {
    currentPost.value = post
    showReviewModal.value = true
  }
}

const getReviewModalTitle = (status) => {
  switch (status) {
    case 0: return '审核中'
    case 1: return '审核通过'
    case 2: return '审核未通过'
    default: return '审核状态'
  }
}

const getReviewIcon = (status) => {
  switch (status) {
    case 0: return ClockCircleOutlined
    case 1: return CheckCircleOutlined
    case 2: return CloseCircleOutlined
    default: return CloseCircleOutlined
  }
}

const getReviewStatusClass = (status) => {
  switch (status) {
    case 0: return 'pending'
    case 1: return 'approved'
    case 2: return 'rejected'
    default: return 'rejected'
  }
}

const getReviewMessage = (status, reviewMessage) => {
  switch (status) {
    case 0: return '您的内容正在审核中，请耐心等待...'
    case 1: return '恭喜！您的内容已通过审核'
    case 2: return reviewMessage || '抱歉，您的内容未通过审核'
    default: return '内容审核状态未知'
  }
}

const textCoverCache = ref({})
const loadingCovers = ref(new Set())

const getPostCover = (post) => {
  const cacheKey = `text_cover_${post.title || 'untitled'}`
  if (textCoverCache.value[cacheKey]) {
    return textCoverCache.value[cacheKey]
  }
  if (loadingCovers.value.has(cacheKey)) {
    return getDefaultAvatar(post.title || 'Post')
  }
  if (post.title) {
    generateTextCoverForPost(post, cacheKey)
  }
  return getDefaultAvatar(post.title || 'Post')
}

const generateTextCoverForPost = async (post, cacheKey) => {
  if (!post.title || loadingCovers.value.has(cacheKey)) return
  try {
    loadingCovers.value.add(cacheKey)
    const cover = await getTextCover(post.title, 300, 400)
    textCoverCache.value[cacheKey] = cover
    nextTick(() => {
      const imgs = document.querySelectorAll(`img[alt="${post.title}"].text-cover`)
      imgs.forEach((img) => {
        const htmlImg = img
        if (htmlImg.src !== cover) {
          htmlImg.src = cover
        }
      })
    })
  } catch (error) {
    console.error('生成文字封面失败:', error)
  } finally {
    loadingCovers.value.delete(cacheKey)
  }
}

watch(() => props.dataList, (newDataList) => {
  if (newDataList && newDataList.length > 0) {
    newDataList.forEach(post => {
      if (!post.coverUrl && post.title) {
        const cacheKey = `text_cover_${post.title}`
        if (!textCoverCache.value[cacheKey] && !loadingCovers.value.has(cacheKey)) {
          generateTextCoverForPost(post, cacheKey)
        }
      }
      if (!post.imageLoaded) {
        post.imageLoaded = false
      }
      if (!post.hdImageLoaded) {
        post.hdImageLoaded = false
      }
      if (!post.avatarLoaded) {
        post.avatarLoaded = false
      }
    })
  }
}, { deep: true, immediate: true })
</script>

<style scoped>
.post-list {
  width: 100%;
  z-index: 0;
}

.masonry-wrapper {
  width: 100%;
  margin: 0 auto;
  padding: 0;
  position: relative;
}

@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

.masonry-grid {
  display: flex;
  gap: 8px;
  width: 100%;
  box-sizing: border-box;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.column-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-item {
  border-radius: 6px;
  overflow: hidden;
  background: var(--card-background);
  border: 1px solid var(--border-color);
}

.skeleton-image {
  width: 100%;
  height: 264px;
  background: var(--hover-background);
  animation: skeleton-pulse 1.5s infinite ease-in-out;
  position: relative;
}

.skeleton-title {
  height: 24px;
  margin: 8px 12px;
  background: var(--hover-background);
  border-radius: 4px;
  animation: skeleton-pulse 1.5s infinite ease-in-out;
}

.masonry-item {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: var(--card-background);
  border: 1px solid rgba(128, 128, 128, 0.05); /* 使用硬编码 RGB 确保兼容性 */
  box-sizing: border-box;
  margin-bottom: 12px;
  position: relative;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1); /* 弹性悬浮 */
  box-shadow: 0 4px 12px -2px rgba(0, 0, 0, 0.06), 0 2px 6px -1px rgba(0, 0, 0, 0.04);
}

.masonry-item:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  z-index: 2;
}

.note-card {
  background: var(--card-background);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 状态点样式优化 */
.status-dot-wrap {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 10;
  padding: 4px;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.status-dot-wrap:hover {
  transform: scale(1.1);
  background: rgba(0, 0, 0, 0.4);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  box-shadow: 0 0 6px rgba(0, 0, 0, 0.2);
  position: relative;
}

/* 状态颜色定义 */
.status-0 { background-color: #ff9d00; box-shadow: 0 0 8px rgba(255, 157, 0, 0.5); } /* 待审核 */
.status-1 { background-color: #00b96b; box-shadow: 0 0 8px rgba(0, 185, 107, 0.5); } /* 已通过 */
.status-2 { background-color: #ff4d4f; box-shadow: 0 0 8px rgba(255, 77, 79, 0.5); }   /* 已拒绝 */

.status-dot::after {
  content: '';
  position: absolute;
  top: -2px;
  left: -2px;
  right: -2px;
  bottom: -2px;
  border-radius: 50%;
  border: 1px solid currentColor;
  opacity: 0.3;
  animation: dot-pulse 2s infinite;
}

@keyframes dot-pulse {
  0% { transform: scale(1); opacity: 0.5; }
  100% { transform: scale(1.8); opacity: 0; }
}



.note-image-wrapper {
  width: 100%;
  position: relative;
  background: var(--hover-background);
  overflow: hidden;

}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  height: 0;
  overflow: hidden;
}

.blur-image-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.blur-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(8px);
  transform: scale(1.05);
}

.note-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.33, 1, 0.68, 1), opacity 0.5s ease;
  opacity: 0;
}

.note-image.loaded {
  opacity: 1;
}

.masonry-item:hover .note-image {
  transform: scale(1.06);
}

.note-info {
  padding: 12px 14px;
}

.author-section {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
}

.author-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border-color);
  transition: opacity 0.3s ease;
  opacity: 0;
}

.author-avatar.loaded {
  opacity: 1;
}

.author-name {
  color: var(--text-primary);
  flex: 1;
}

.view-count {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--text-secondary);
  font-size: 11px;
}

.view-icon {
  font-size: 10px;
}

.note-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  letter-spacing: 0.01em;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 11px;
  color: var(--text-secondary);
}

.category-tag {
  background: rgba(0, 102, 179, 0.08);
  color: #0066B3;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.category-tag:hover {
  background: rgba(0, 102, 179, 0.15);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  height: 310px;
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  margin: 16px 8px;
}

.empty-img {
  width: 120px;
  height: 220px;
}

.empty-text h3 {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.empty-text p {
  font-size: 13px;
  color: var(--text-secondary);
}

.review-modal, .reject-modal {
  :deep(.ant-modal-content) {
    border-radius: 8px;
    border: 1px solid var(--border-color);
    background: var(--card-background);
  }
  :deep(.ant-modal-body) {
    padding: 0;
  }
}

.review-content, .reject-content {
  padding: 20px;
  text-align: center;
}

.review-icon, .reject-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.review-title, .reject-title {
  font-size: 16px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.review-message, .reject-message {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.confirm-btn {
  width: 100%;
  height: 36px;
  border-radius: 18px !important;
  font-size: 14px !important;
}

.masonry-grid.single-column .masonry-column {
  width: 100%;
  max-width: 600px;
  margin: 0 auto;
}

@media screen and (max-width: 768px) {
  .masonry-grid {
    gap: 2px;
    padding: 0;
  }
  .masonry-column {
    gap: 4px;
  }
  .note-info {
    padding: 6px 8px;
  }
  .author-avatar {
    width: 20px;
    height: 20px;
  }
  .note-title {
    font-size: 12px;
  }
  .post-meta {
    font-size: 10px;
  }
  .empty-img {
    width: 100px;
    height: 220px;
  }
  .blur-image {
    filter: blur(4px);
  }
  /* 移动端优化状态标签 */
  .status-tag {
    font-size: 10px !important;
    padding: 1px 4px !important;
  }
}

/* =========================================
   PostList Cinematic Dark Mode
   ========================================= */
:global(.dark-theme) .note-card {
  background: #1e1e1e;
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.4);
}

:global(.dark-theme) .masonry-item {
  border-color: rgba(255, 255, 255, 0.05);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
}

:global(.dark-theme) .masonry-item:hover {
  box-shadow: 0 20px 30px rgba(0, 0, 0, 0.5);
  border-color: rgba(255, 255, 255, 0.12);
}

:global(.dark-theme) .note-title {
  color: rgba(255, 255, 255, 0.95);
}

:global(.dark-theme) .author-name {
  color: rgba(255, 255, 255, 0.65);
}

:global(.dark-theme) .view-count {
  color: rgba(255, 255, 255, 0.45);
}

:global(.dark-theme) .category-tag {
  background: rgba(56, 189, 248, 0.1);
  color: #38bdf8;
}

:global(.dark-theme) .status-dot-wrap {
  background: rgba(0, 0, 0, 0.5);
  border-color: rgba(255, 255, 255, 0.1);
}

:global(.dark-theme) .skeleton-item {
  background: #1a1a1a;
  border-color: rgba(255, 255, 255, 0.05);
}

:global(.dark-theme) .skeleton-image,
:global(.dark-theme) .skeleton-title {
  background: #262626;
}

:global(.dark-theme) .empty-state {
  background: #111;
  border-color: rgba(255, 255, 255, 0.05);
}

:global(.dark-theme) .empty-text h3 {
  color: #e2e8f0;
}

:global(.dark-theme) .empty-text p {
  color: #64748b;
}

@media screen and (max-width: 768px) {
  .masonry-item {
    border-radius: 8px;
    margin-bottom: 8px;
  }
  .note-card {
    border-radius: 8px;
  }
  .masonry-item:hover {
    transform: translateY(-4px);
  }
}
</style>
