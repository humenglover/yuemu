<template>
  <div class="pc-picture-list">
    <div class="masonry-wrapper" ref="masonryRef">
      <div class="masonry-grid" ref="gridRef">
        <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column" :class="{
          'single-column': streamLayout.value === 'single',
          'grid-column': streamLayout.value === 'grid'
        }">
          <div v-if="loading" class="column-skeleton">
            <div v-for="(_, index) in 3" :key="index" class="skeleton-item">
              <div class="skeleton-image"></div>
            </div>
          </div>
          <div v-else-if="!loading && (!props.dataList || props.dataList.length === 0)" class="empty-state">
            <img :src="emptyImage" alt="暂无内容" class="empty-image">
            <div class="empty-text">
              <h3>暂无图片</h3>
              <p>快去上传一些精彩的照片吧 <i class="fas fa-sad-tear"></i></p>
            </div>
          </div>
          <div v-else>
            <div v-for="picture in column" :key="picture.id" class="card-spacing">
              <div
                class="masonry-item"
                :data-pic-id="picture.id"
                :class="{ 'draft': picture.isDraft === 1 }"
                @click.stop="doClickPicture(picture)"
              >
                <div class="image-wrapper">
                  <div
                    class="aspect-ratio-box"
                    :style="{ paddingTop: `${getLimitedPaddingTop(picture)}%` }"
                  >
                    <img
                      class="masonry-image"
                      :src="picture.thumbnailUrl || picture.url || '/default-image.png'"
                      :alt="picture.name || '图片'"
                      :class="{ loaded: picture.imageLoaded }"
                      @load="handleImageLoad(picture)"
                      @error="handleImageError(picture)"
                    />

                    <div class="image-view-count">
                      <i class="fas fa-eye view-icon"></i>
                      <span class="view-number">{{ formatNumber(picture.viewCount) || 0 }}</span>
                    </div>
                    <div class="image-badges">
                      <div v-if="picture.isFeature === 1" class="badge feature-badge">
                        <i class="fas fa-crown"></i>
                        <span>精选</span>
                      </div>
                      <div v-else-if="picture.isDraft === 1" class="badge draft-badge">
                        <i class="fas fa-file-alt"></i>
                        <span>草稿</span>
                      </div>
                    </div>

                    <div v-if="isMyPosts && picture.isDraft !== 1" class="review-status">
                      <a-button type="link" class="review-button" @click.stop="showReviewModal(picture)">
                        <template v-if="picture.reviewStatus === 0">
                          <i class="fas fa-clock status-icon pending"></i>
                          <span class="status-text">审核中</span>
                        </template>
                        <template v-else-if="picture.reviewStatus === 1">
                          <i class="fas fa-check-circle status-icon approved"></i>
                          <span class="status-text">已通过</span>
                        </template>
                        <template v-else-if="picture.reviewStatus === 2">
                          <i class="fas fa-times-circle status-icon rejected"></i>
                          <span class="status-text">未通过</span>
                        </template>
                      </a-button>
                    </div>

                    <div class="hover-overlay">
                      <div class="overlay-content">
                        <div class="picture-title">{{ picture.name || '未命名图片' }}</div>
                        <div class="user-info">
                          <img
                            class="user-avatar"
                            :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                            :alt="picture.user?.userName || '用户头像'"
                            :class="{ loaded: picture.avatarLoaded }"
                            @load="() => { picture.avatarLoaded = true }"
                          />
                          <div class="user-meta">
                            <span class="user-name">{{ picture.user?.userName || '未知用户' }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 新版图片详情蒙层 (自定义原生风格) -->
    <PictureDetailView
      v-if="selectedPictureId"
      :id="selectedPictureId"
      :visible="detailModalVisible"
      :initialData="selectedPictureData"
      @close="handleDetailModalClose"
    />

    <a-modal
      v-model:open="reviewModalVisible"
      :title="getReviewModalTitle(currentPicture?.reviewStatus)"
      :footer="null"
      class="review-modal"
    >
      <div class="review-detail">
        <div class="status-icon-large">
          <i class="fas fa-clock pending" v-if="currentPicture?.reviewStatus === 0"></i>
          <i class="fas fa-check-circle approved" v-else-if="currentPicture?.reviewStatus === 1"></i>
          <i class="fas fa-times-circle rejected" v-else-if="currentPicture?.reviewStatus === 2"></i>
        </div>
        <div class="review-message">
          <template v-if="currentPicture?.reviewStatus === 0">您的图片正在审核中，请耐心等待...</template>
          <template v-else-if="currentPicture?.reviewStatus === 1">恭喜！您的图片已通过审核</template>
          <template v-else-if="currentPicture?.reviewStatus === 2">{{ currentPicture?.reviewMessage || '抱歉，您的图片未通过审核' }}</template>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CrownOutlined,
  FileTextOutlined,
  HeartOutlined,
  CommentOutlined,
  EyeOutlined,
  ShareAltOutlined
} from '@ant-design/icons-vue'
import { Button as AButton, Modal as AModal } from 'ant-design-vue'
import { debounce, throttle } from 'lodash-es'
import { getDefaultAvatar } from '@/utils/userUtils'
import emptyImage from '@/assets/empty.png'
import PictureDetailView from './PictureDetailView.vue'

const Button = AButton
const Modal = AModal

interface User {
  id: string | number
  userName: string
  userAvatar: string
  userAccount: string
  userProfile: string
  userRole: string
  createTime: string
}

interface PictureVO {
  id: string | number
  url: string
  thumbnailUrl?: string
  name?: string
  picScale?: number
  picColor?: string
  isFeature?: number
  reviewStatus?: number
  reviewMessage?: string
  viewCount?: number
  avatarLoaded?: boolean
  imageLoaded?: boolean
  user?: User
  itemHeight?: number
  isDraft?: number
  createTime?: string
}

const props = withDefaults(defineProps<{
  dataList?: PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  isMyPosts?: boolean
  canEdit?: boolean
  canDelete?: boolean
}>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  onReload: () => {},
  isMyPosts: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()
const masonryRef = ref<HTMLElement | null>(null)
const gridRef = ref<HTMLElement | null>(null)
const reviewModalVisible = ref(false)
const currentPicture = ref<PictureVO | null>(null)
const windowWidth = ref(window.innerWidth)
const triggerUpdate = ref(0)
const columnHeights = ref<number[]>([])

// 详情弹窗相关
const detailModalVisible = ref(false)
const selectedPictureId = ref<string | number | null>(null)
const selectedPictureData = ref<PictureVO | null>(null)

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

const getLimitedPaddingTop = (picture: PictureVO) => {
  const ratio = picture.picScale || 1

  let minRatio, maxRatio;
  switch(streamLayout.value) {
    case 'waterfall':
      minRatio = 0.75;
      maxRatio = 1.60;
      break;
    case 'grid':
      switch(gridAspectRatio.value) {
        case '4:3':
          return (3/4) * 100;
        case '3:4':
          return (4/3) * 100;
        case '1:1':
          return 1 * 100;
        default:
          return (3/4) * 100;
      }
    case 'single':
      minRatio = 0.5;
      maxRatio = 1.6;
      break;
    default:
      minRatio = 0.75;
      maxRatio = 0.95;
  }

  const limitedRatio = Math.max(minRatio, Math.min(ratio, maxRatio))
  return (1 / limitedRatio) * 100
}

const formatNumber = (num?: number): string => {
  if (!num) return '0'
  if (num >= 10000) return `${(num / 10000).toFixed(1)}w`
  if (num >= 1000) return `${(num / 1000).toFixed(1)}k`
  return num.toString()
}

const formatTime = (time?: string): string => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

const getColumnCount = (): number => {
  switch(streamLayout.value) {
    case 'single':
      return 1
    default:
      if (windowWidth.value >= 1200) return 3 // Changed from 4 to max 3
      if (windowWidth.value >= 768) return 2  // Changed from 3 to 2 for tablets
      return 1 // Changed from 2 to 1 for mobile or keep 1
  }
}

const columns = computed(() => {
  const count = getColumnCount()
  const cols = [] as PictureVO[][]
  for (let i = 0; i < count; i++) {
    cols.push([])
  }

  if (props.loading || !props.dataList || props.dataList.length === 0) {
    const heights = [] as number[]
    for (let i = 0; i < count; i++) {
      heights.push(0)
    }
    columnHeights.value = heights
    return cols
  }

  if (streamLayout.value === 'grid') {
    props.dataList.forEach((item, index) => {
      const columnIndex = index % count
      cols[columnIndex].push(item)
    })
    return cols
  }

  const heights_data = [] as number[]
  for (let j = 0; j < count; j++) {
    heights_data.push(0)
  }
  columnHeights.value = heights_data

  props.dataList.forEach(picture => {
    const minHeight = Math.min(...columnHeights.value)
    const targetColumnIndex = columnHeights.value.indexOf(minHeight)

    cols[targetColumnIndex].push(picture)

    const picRatio = picture.picScale || 1
    const limitedRatio = Math.max(0.75, Math.min(picRatio, 1.60))
    const picHeight = (1 / limitedRatio) * 100
    const contentHeight = 0

    columnHeights.value[targetColumnIndex] += picHeight + contentHeight + 8
  })

  return cols
})

const handleImageLoad = (picture: PictureVO) => {
  picture.imageLoaded = true
  updateColumnHeights()
  recalculateLayout()
}

const handleImageError = (picture: PictureVO) => {
  picture.imageLoaded = true
  recalculateLayout()
}

const updateColumnHeights = () => {
  nextTick(() => {
    if (!gridRef.value) return

    const columns = gridRef.value.querySelectorAll('.masonry-column')
    columns.forEach((column, index) => {
      if (index < columnHeights.value.length) {
        columnHeights.value[index] = column.offsetHeight
      }
    })
  })
}

const recalculateLayout = () => {
  requestAnimationFrame(() => {
    nextTick(() => {
      triggerUpdate.value++
    })
  })
}

const doClickPicture = (picture: PictureVO) => {
  if (picture.isDraft === 1) {
    router.push({ path: '/add_picture', query: { id: picture.id } })
  } else {
    // 始终使用弹窗处理 (PC端组件默认行为)
    openDetailModal(picture)
  }
}

const openDetailModal = (picture: PictureVO) => {
  selectedPictureId.value = picture.id
  selectedPictureData.value = picture
  detailModalVisible.value = true
}

const handleDetailModalClose = (needReload = false) => {
  detailModalVisible.value = false
  selectedPictureId.value = null
  selectedPictureData.value = null

  if (needReload && props.onReload) {
    props.onReload()
  }
}

// 监听浏览器后退
const handlePopState = (event: PopStateEvent) => {
  if (detailModalVisible.value) {
    detailModalVisible.value = false
    selectedPictureId.value = null
    selectedPictureData.value = null
  }
}

const showReviewModal = (picture: PictureVO) => {
  currentPicture.value = picture
  reviewModalVisible.value = true
}

const getReviewModalTitle = (status?: number): string => {
  switch (status) {
    case 0: return '审核中'
    case 1: return '审核通过'
    case 2: return '审核未通过'
    default: return '审核状态'
  }
}

const handleResize = throttle(() => {
  windowWidth.value = window.innerWidth
  recalculateLayout()
}, 200)

watch(
  () => props.dataList,
  () => {
    recalculateLayout()
  },
  { deep: true }
)

watch([windowWidth, streamLayout], () => {
  const count = getColumnCount()
  const hs = [] as number[]
  for (let i = 0; i < count; i++) hs.push(0)
  columnHeights.value = hs
  recalculateLayout()
})

const handleStreamLayoutChange = (event: CustomEvent) => {
  if (event.detail) {
    streamLayout.value = event.detail.layout || streamLayout.value
    if (event.detail.aspectRatio && streamLayout.value === 'grid') {
      gridAspectRatio.value = event.detail.aspectRatio
      localStorage.setItem('gridAspectRatio', event.detail.aspectRatio)
    }
    localStorage.setItem('streamLayout', streamLayout.value)
  }
  const count = getColumnCount()
  const hs = [] as number[]
  for (let i = 0; i < count; i++) hs.push(0)
  columnHeights.value = hs
  recalculateLayout()
}

onMounted(() => {
  loadStreamLayout()
  windowWidth.value = window.innerWidth
  recalculateLayout()

  window.addEventListener('resize', handleResize)
  window.addEventListener('streamLayoutChanged', handleStreamLayoutChange)
  window.addEventListener('popstate', handlePopState)

  const resizeObserver = new ResizeObserver(handleResize)
  if (gridRef.value) {
    resizeObserver.observe(gridRef.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('streamLayoutChanged', handleStreamLayoutChange)
  window.removeEventListener('popstate', handlePopState)
})
</script>

<style lang="scss" scoped>
/* 弹窗样式定制 */
:deep(.picture-detail-modal) {
  .ant-modal-content {
    background: transparent;
    box-shadow: none;
    padding: 0;
  }
  .ant-modal-close {
    color: #fff;
    background: rgba(0,0,0,0.3);
    border-radius: 50%;
    top: 16px;
    right: 16px;
    z-index: 10;

    &:hover {
      background: rgba(0,0,0,0.5);
    }
  }
}

.pc-picture-list {
  width: 100%;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  min-width: 320px;
}

.masonry-wrapper {
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
  min-height: 500px;
  width: 100%;
  background: var(--card-background);
  border-radius: 8px;
  margin: 16px auto;
  border: 1px solid var(--border-color);

  .empty-image {
    width: 240px;
    height: 240px;
    margin-bottom: 24px;
  }

  .empty-text {
    h3 {
      font-size: 18px;
      color: var(--text-primary);
      margin-bottom: 8px;
      font-weight: 500;
    }

    p {
      font-size: 14px;
      color: var(--text-secondary);
      margin: 0;
    }
  }
}

.masonry-grid {
  display: flex;
  gap: 16px; /* Increased from 4px to 16px */
  width: 100%;
  box-sizing: border-box;
  justify-content: flex-start;
  flex-wrap: nowrap !important;
  overflow: hidden;
}

.masonry-column {
  flex: 1 0 calc((100% - 32px) / 3) !important; /* Update width calculation based on 3 columns and 16x2 gaps */
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px; /* Increased padding vertically */
  box-sizing: border-box;
}

.masonry-column.single-column {
  flex: 1 0 100% !important;
}

.masonry-column.grid-column {
  flex: 1 0 calc((100% - 16px) / 2) !important;
}

.card-spacing {
  width: 100%;
  margin-bottom: 16px; /* Added spacing between vertically stacked items */
  box-sizing: border-box;
  &:last-child {
    margin-bottom: 0;
  }
}

.column-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-item {
  border-radius: 8px;
  overflow: hidden;
  background: var(--card-background);
  box-shadow: 0 2px 12px var(--shadow-color);
  width: 100%;
  min-height: 200px;
}

.skeleton-image {
  width: 100%;
  height: 300px;
  background: var(--card-background);
  filter: saturate(0.8) brightness(0.98);
  animation: skeleton-loading 1.2s infinite ease-in-out;
  min-height: 200px;
  border-radius: 8px;
}

@keyframes skeleton-loading {
  0%, 100% {
    filter: saturate(0.8) brightness(0.98);
  }
  50% {
    filter: saturate(0.8) brightness(1.02);
  }
}

:global(.dark-theme) {
  .skeleton-image {
    filter: saturate(0.7) brightness(0.9);
  }
  @keyframes skeleton-loading {
    0%, 100% {
      filter: saturate(0.7) brightness(0.9);
    }
    50% {
      filter: saturate(0.7) brightness(0.98);
    }
  }
}

.masonry-item {
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: var(--card-background);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); /* Softer shadow */
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  border: none; /* Emulate pexels clean borderless layout */
  box-sizing: border-box;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px var(--shadow-color);

    .hover-overlay {
      opacity: 1;
    }

    .masonry-image {
      transform: scale(1.03);
    }

    .image-view-count {
      background: rgba(0, 0, 0, 0.7);
    }
  }

  &.draft {
    opacity: 0.7;
    filter: grayscale(20%);
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: var(--hover-background);
  box-sizing: border-box;
}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  height: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.masonry-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  box-sizing: border-box;
}

.masonry-image.loaded {
  opacity: 1;
}

.image-view-count {
  position: absolute;
  bottom: 12px;
  right: 12px;
  z-index: 5;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.5);
  color: var(--text-other);
  font-size: 11px;
  font-weight: 500;
  backdrop-filter: blur(2px);
  transition: background 0.2s ease;

  .view-icon {
    font-size: 10px;
  }

  .view-number {
    letter-spacing: 0.5px;
  }
}

.hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, transparent 40%);
  opacity: 0;
  transition: opacity 0.3s ease;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: 16px;
  z-index: 4;
  box-sizing: border-box;
}

.overlay-content {
  width: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 8px;

  .action-btn {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: var(--text-other);
    font-size: 11px;
    cursor: pointer;
    transition: transform 0.2s;

    &:hover {
      transform: translateY(-2px);
    }

    :deep(.anticon) {
      font-size: 18px;
      filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
    }

    span {
      opacity: 0.9;
    }
  }
}

.image-badges {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  gap: 4px;
  z-index: 3;
}

.badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px var(--shadow-color);

  :deep(.anticon) {
    font-size: 12px;
  }
}

.feature-badge {
  background: linear-gradient(135deg, #0066B3 0%, #0066B3 100%);
  color: var(--text-other);
}

.draft-badge {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
  color: var(--text-other);
}

.review-status {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 4;
}

.review-button {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 4px 10px;
  font-size: 11px;
  height: auto;
  display: flex;
  align-items: center;
  gap: 4px;
  border: 1px solid var(--border-color);
  backdrop-filter: blur(10px);
  box-sizing: border-box;
}

.status-icon {
  font-size: 14px;

  &.pending {
    color: #1890ff;
  }

  &.approved {
    color: #52c41a;
  }

  &.rejected {
    color: #ff4d4f;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;

  .user-avatar {
    width: 28px !important;
    height: 28px !important;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.4);
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    transition: opacity 0.3s;
    opacity: 0;
    box-sizing: border-box;

    &.loaded {
      opacity: 1;
    }
  }

  .user-meta {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 1px;

    .user-name {
      font-size: 13px;
      font-weight: 500;
      color: #fff;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.8);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.picture-title {
  font-size: 14px;
  color: #fff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
  line-height: 1.4;
  margin-bottom: 4px;
  padding: 0;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  box-sizing: border-box;
}

.review-modal {
  :deep(.ant-modal-content) {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 10px 20px var(--shadow-color);
    border: 1px solid var(--border-color);
  }

  :deep(.ant-modal-header) {
    border-bottom: 1px solid var(--border-color);
    padding: 16px 20px;
  }

  :deep(.ant-modal-title) {
    font-size: 16px;
    font-weight: 500;
    text-align: center;
    color: var(--text-primary);
  }

  :deep(.ant-modal-body) {
    padding: 20px;
  }
}

.review-detail {
  text-align: center;
  padding: 24px;
  background: var(--hover-background);
  border-radius: 12px;
}

.status-icon-large {
  font-size: 48px;
  margin-bottom: 20px;

  .pending {
    color: #1890ff;
  }
  .approved {
    color: #52c41a;
  }
  .rejected {
    color: #ff4d4f;
  }
}

.review-message {
  font-size: 16px;
  color: var(--text-primary);
  line-height: 1.6;
  padding: 0 16px;
}

@media screen and (max-width: 1200px) {
  .masonry-column {
    flex: 1 0 calc((100% - 16px) / 2) !important; /* Update variable query */
  }
}

@media screen and (max-width: 768px) {
  .masonry-grid {
    gap: 8px;
  }
  .column-skeleton, .masonry-column {
    gap: 8px;
  }
  .masonry-column {
    flex: 1 0 calc((100% - 8px) / 2) !important;
  }
  .image-view-count {
    padding: 3px 7px;
    font-size: 10px;
    bottom: 8px;
    right: 8px;
  }
}

@media screen and (max-width: 480px) {
  .masonry-column {
    flex: 1 0 100% !important;
  }
  .masonry-item {
    border-radius: 12px;
  }
  .picture-title {
    font-size: 12px;
  }
  .user-avatar {
    width: 28px !important;
    height: 28px !important;
  }
  .image-view-count {
    padding: 2px 6px;
    font-size: 9px;
  }
  .masonry-item:hover {
    transform: none;
    box-shadow: 0 2px 12px var(--shadow-color);
  }
}

:global(.dark-theme) .review-button {
  background: rgba(45, 45, 45, 0.95);
  color: var(--text-primary);
}
</style>
