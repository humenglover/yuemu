<template>
  <div class="note-picture-list">
    <div class="masonry-wrapper" ref="masonryRef">
      <div v-if="!loading && (!props.dataList || props.dataList.length === 0)" class="empty-state">
        <div class="custom-empty-state">
          <img :src="emptyImage" alt="暂无内容" class="empty-illustration" />
          <div class="empty-text">
            <h3>暂无笔记</h3>
            <p>快去发布你的第一条笔记吧 <i class="fas fa-star empty-emoji"></i></p>
          </div>
        </div>
      </div>
      <div :class="{
        'masonry-grid': !isGridLayout,
        'grid-absolute-equal-height': isGridLayout
      }" ref="gridRef">
        <div v-for="(column, columnIndex) in columns" :key="columnIndex" :class="{
          'masonry-column': !isGridLayout,
        }">
          <div v-if="loading" class="column-skeleton">
            <div v-for="(_, index) in 4" :key="index" class="skeleton-item">
              <div class="skeleton-image"></div>
              <div class="skeleton-content">
                <div class="skeleton-user-info">
                  <div class="skeleton-avatar"></div>
                  <div class="skeleton-username"></div>
                </div>
                <div class="skeleton-title"></div>
              </div>
            </div>
          </div>
          <div v-else>
            <div
              v-for="picture in column"
              :key="picture.id"
              class="masonry-item"
              :data-pic-id="picture.id"
              :class="{ 'loading': loading }"
              @click="doClickPicture(picture)"
            >
              <div class="image-wrapper">
                <div
                  class="aspect-ratio-box"
                  :style="{ paddingTop: `${getLimitedPaddingTop(picture)}%` }"
                >
                  <div v-if="loading" class="skeleton-wrapper">
                    <div class="skeleton-image"></div>
                  </div>
                  <div v-if="picture.isDraft === 1" class="draft-badge" title="草稿">
                    <i class="fas fa-file-alt draft-icon"></i>
                  </div>
                  <div v-else-if="picture.isFeature === 1" class="feature-badge" title="精选">
                    <i class="fas fa-crown feature-icon"></i>
                  </div>
                  <div v-if="isMyPosts && picture.isDraft !== 1 && !loading" class="review-status-dot-wrap" @click.stop="showReviewModal(picture)">
                    <div class="status-dot" :class="['status-' + picture.reviewStatus]"></div>
                  </div>
                  <div v-if="picture.isDraft !== 1 && !loading" class="view-count-badge">
                    <i class="fas fa-eye view-icon"></i>
                    <span style="padding-left: 4px;">{{ formatNumber(picture.viewCount || 0) }}</span>
                  </div>
                  <img
                    v-show="!loading"
                    :alt="picture.name || '笔记图片'"
                    class="masonry-image"
                    :src="picture.thumbnailUrl || picture.url || '/default-image.png'"
                    :class="{ loaded: picture.imageLoaded }"
                    @load="handleImageLoad(picture)"
                    @error="handleImageError(picture)"
                  />
                </div>
              </div>
              <div class="picture-content">
                <div class="user-info" v-if="picture.isDraft !== 1">
                  <img
                    class="user-avatar"
                    :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                    :alt="picture.user?.userName || '用户头像'"
                    :class="{ loaded: picture.avatarLoaded }"
                    @load="() => { picture.avatarLoaded = true }"
                  />
                  <span class="user-name">{{ picture.user?.userName || '未知用户' }}</span>
                </div>
                <div class="picture-title">{{ picture.name || '未命名笔记' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <a-modal
      v-model:open="reviewModalVisible"
      :title="getReviewModalTitle(currentPicture?.reviewStatus)"
      :footer="null"
      class="review-modal"
    >
      <div class="review-detail">
        <div class="status-icon-large">
          <i class="fas fa-clock pending-icon" v-if="currentPicture?.reviewStatus === 0"></i>
          <i class="fas fa-check-circle approved-icon" v-else-if="currentPicture?.reviewStatus === 1"></i>
          <i class="fas fa-times-circle rejected-icon" v-else-if="currentPicture?.reviewStatus === 2"></i>
        </div>
        <div class="review-message">
          <template v-if="currentPicture?.reviewStatus === 0">您的笔记正在审核中，请耐心等待...</template>
          <template v-else-if="currentPicture?.reviewStatus === 1">恭喜！您的笔记已通过审核</template>
          <template v-else-if="currentPicture?.reviewStatus === 2">{{ currentPicture?.reviewMessage || '抱歉，您的笔记未通过审核' }}</template>
        </div>
      </div>
    </a-modal>

    <!-- 新版图片详情蒙层 (自定义原生风格) -->
    <PictureDetailView
      v-if="selectedPictureId"
      :id="selectedPictureId"
      :visible="detailModalVisible"
      :initialData="selectedPictureData"
      @close="handleDetailModalClose"
    />
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import PictureDetailView from '@/components/PictureDetailView.vue'
import { ref, computed, onMounted, onUnmounted, nextTick, inject } from 'vue'
import {
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CrownOutlined,
  FileTextOutlined,
  MenuOutlined,
  UnorderedListOutlined
} from '@ant-design/icons-vue'
import { Button as AButton, Modal as AModal } from 'ant-design-vue'
import { throttle } from 'lodash-es'
import { getDefaultAvatar } from '@/utils/userUtils'
import emptyImage from '@/assets/illustrations/empty.png'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

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
const isEndOfData = ref(false)
const triggerUpdate = ref(0)
const isMobile = ref(false)
const mobileColumnCount = ref(2)

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

const handleStreamLayoutChange = (event: CustomEvent) => {
  if (event.detail) {
    streamLayout.value = event.detail.layout || streamLayout.value
    if (event.detail.aspectRatio && streamLayout.value === 'grid') {
      gridAspectRatio.value = event.detail.aspectRatio
      localStorage.setItem('gridAspectRatio', event.detail.aspectRatio)
    }
    localStorage.setItem('streamLayout', streamLayout.value)
  }
  recalculateLayout()
}

onMounted(async () => {
  const deviceType = await getDeviceType()
  isMobile.value = deviceType === DEVICE_TYPE_ENUM.MOBILE
  loadStreamLayout()
  const resizeObserver = new ResizeObserver(handleContainerResize)
  if (gridRef.value) {
    resizeObserver.observe(gridRef.value)
  }
  window.addEventListener('resize', handleContainerResize)
  window.addEventListener('streamLayoutChanged', handleStreamLayoutChange)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleContainerResize)
  window.removeEventListener('streamLayoutChanged', handleStreamLayoutChange)
})

const toggleColumnCount = () => {
  mobileColumnCount.value = mobileColumnCount.value === 1 ? 2 : 1
  recalculateLayout()
}

const columnToggleText = computed(() => {
  return mobileColumnCount.value === 1 ? '切换为两列' : '切换为一列'
})

const columnToggleIconName = computed(() => {
  return mobileColumnCount.value === 1 ? 'MenuOutlined' : 'UnorderedListOutlined'
})

const getLimitedPaddingTop = (picture: PictureVO) => {
  const ratio = picture.picScale || 1
  let minRatio, maxRatio;
  switch(streamLayout.value) {
    case 'waterfall':
      minRatio = 0.75;
      maxRatio = 1.6;
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
      maxRatio = 2.0;
      break;
    default:
      minRatio = 0.75;
      maxRatio = 1.6;
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

const getColumnCount = (): number => {
  const width = window.innerWidth || document.documentElement.clientWidth
  if (isMobile.value) {
    switch(streamLayout.value) {
      case 'waterfall':
        return mobileColumnCount.value
      case 'grid':
        return 2
      case 'single':
        return 1
      default:
        return mobileColumnCount.value
    }
  }
  if (width >= 1200) return 4
  if (width >= 768) return 3
  return 2
}

const handleImageLoad = (picture: PictureVO) => {
  picture.imageLoaded = true
}

const handleImageError = (picture: PictureVO) => {
  picture.imageLoaded = true
}

const doClickPicture = (picture: PictureVO) => {
  if (picture.isDraft === 1) {
    router.push({ path: '/add_picture', query: { id: picture.id } })
  } else {
    if (isMobile.value) {
      router.push({
        path: `/mobile/picture/${picture.id}`,
        state: { pictureData: JSON.parse(JSON.stringify(picture)) }
      })
    } else {
      selectedPictureId.value = picture.id
      selectedPictureData.value = picture
      detailModalVisible.value = true
    }
  }
}

const handleDetailModalClose = (needReload = false) => {
  detailModalVisible.value = false
  selectedPictureId.value = null
  selectedPictureData.value = null
  if (needReload && props.onReload) {
    props.onReload()
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

const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: PictureVO[][] = Array(columnCount).fill(null).map(() => [])
  if (streamLayout.value === 'grid') {
    props.dataList.forEach((item, index) => {
      const columnIndex = index % columnCount
      cols[columnIndex].push(item)
    })
  } else {
    const columnHeights: number[] = Array(columnCount).fill(0)
    props.dataList.forEach((item) => {
      let minHeightIndex = 0
      for (let i = 1; i < columnCount; i++) {
        if (columnHeights[i] < columnHeights[minHeightIndex]) {
          minHeightIndex = i
        }
      }
      cols[minHeightIndex].push(item)
      const ratio = item.picScale || 1
      let minRatio, maxRatio;
      switch(streamLayout.value) {
        case 'waterfall':
          minRatio = 0.75;
          maxRatio = 1.6;
          break;
        case 'grid':
          switch(gridAspectRatio.value) {
            case '4:3':
              minRatio = 3/4;
              maxRatio = 3/4;
              break;
            case '3:4':
              minRatio = 4/3;
              maxRatio = 4/3;
              break;
            case '1:1':
              minRatio = 1;
              maxRatio = 1;
              break;
            default:
              minRatio = 3/4;
              maxRatio = 3/4;
          }
          break;
        case 'single':
          minRatio = 0.5;
          maxRatio = 2.0;
          break;
        default:
          minRatio = 0.75;
          maxRatio = 1.6;
      }
      const limitedRatio = Math.max(minRatio, Math.min(ratio, maxRatio))
      const containerWidth = gridRef.value?.clientWidth || document.documentElement.clientWidth
      const imageWidth = (containerWidth / columnCount) - 8
      const imageHeight = imageWidth / limitedRatio
      const itemHeight = imageHeight + 80 + 16
      columnHeights[minHeightIndex] += itemHeight
    })
  }
  return cols
})

const isGridLayout = computed(() => {
  return streamLayout.value === 'grid'
})

const recalculateLayout = () => {
  nextTick(() => {
    triggerUpdate.value++
  })
}

const handleContainerResize = throttle(() => {
  if (gridRef.value) {
    recalculateLayout()
  }
}, 500)
</script>

<style lang="scss" scoped>
.note-picture-list {
  width: 100%;
  color: var(--text-primary);
  z-index: 0;
  box-sizing: border-box;
  padding: 8px 8px;
}

.column-toggle-btn-wrapper {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 999;
  box-shadow: 0 2px 8px var(--shadow-color);
  border-radius: 20px;
  overflow: hidden;

  .column-toggle-btn {
    background: var(--card-background);
    color: #2563eb;
    border-radius: 20px;
    padding: 8px 16px;
    height: 40px;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 6px;
    border: 1px solid var(--border-color);

    &:hover {
      background: var(--hover-background);
      color: #1d4ed8;
    }
  }
}

.masonry-wrapper {
  width: 100%;
  padding: 0;
  max-width: 1400px;
  margin: 0 auto;
}

.masonry-grid {
  display: flex;
  gap: 2px;
  width: 100%;
  padding: 0;
  margin: 0;
  box-sizing: border-box;
}

.grid-absolute-equal-height {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px;
  width: 100%;
  padding: 0;
  margin: 0;
  box-sizing: border-box;

  @media (min-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (min-width: 1200px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

.grid-absolute-equal-height .masonry-item {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.grid-absolute-equal-height .image-wrapper {
  flex: 1;
  display: flex;
}

.grid-absolute-equal-height .aspect-ratio-box {
  flex: 1;
  position: relative;
  width: 100%;
  height: 0;
  overflow: hidden;
}

.grid-absolute-equal-height .picture-content {
  flex-shrink: 0;
  padding: 8px;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.column-skeleton {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skeleton-item {
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  background: var(--card-background);
}

.skeleton-image {
  width: 100%;
  height: 220px;
  background: var(--card-background);
  filter: saturate(0.8) brightness(0.98);
  animation: skeleton-loading 1.2s infinite ease-in-out;
  border-radius: 4px 4px 0 0;
}

.skeleton-content {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.skeleton-user-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.skeleton-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--card-background);
  filter: saturate(0.8) brightness(0.98);
  animation: skeleton-loading 1.2s infinite ease-in-out;
}

.skeleton-username {
  width: 80px;
  height: 16px;
  background: var(--card-background);
  filter: saturate(0.8) brightness(0.98);
  animation: skeleton-loading 1.2s infinite ease-in-out;
  border-radius: 2px;
}

.skeleton-title {
  width: 100%;
  height: 32px;
  background: var(--card-background);
  filter: saturate(0.8) brightness(0.98);
  animation: skeleton-loading 1.2s infinite ease-in-out;
  border-radius: 2px;
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
  .skeleton-image,
  .skeleton-avatar,
  .skeleton-username,
  .skeleton-title {
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
  margin: 0;
  break-inside: avoid;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  background-color: var(--card-background);
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px var(--shadow-color);
  margin-bottom: 8px;
  border: 1px solid var(--border-color);

  &:last-child {
    margin-bottom: 8px;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px var(--shadow-color);
    border-color: var(--border-color);
  }

  &.loading {
    pointer-events: none;
    transform: none !important;

    .picture-content {
      opacity: 0;
    }

    &:hover {
      transform: none;
      box-shadow: 0 1px 2px var(--shadow-color);
      border-color: var(--border-color);
    }
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: var(--hover-background);
}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  height: 0;
  overflow: hidden;
}

.masonry-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover !important;
  object-position: center !important;
  transition: opacity 0.3s ease-in-out;
  opacity: 0;
}

.masonry-image.loaded {
  opacity: 1;
}

.view-count-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 11px;
  color: var(--text-other);
  z-index: 1;
}

.review-status-dot-wrap {
  position: absolute;
  top: 6px;
  left: 6px;
  z-index: 2;
  background: rgba(255, 255, 255, 0.85);
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);

  :global(.dark-theme) & {
    background: rgba(45, 45, 45, 0.85);
  }

  .status-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    &.status-0 { background: #1890ff; }
    &.status-1 { background: #52c41a; }
    &.status-2 { background: #ff4d4f; }
  }
}

.feature-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(59, 130, 246, 0.9);
  border-radius: 50%; // 改为圆形徽标
  width: 20px;
  height: 20px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);

  .feature-icon {
    font-size: 10px;
  }
}

.draft-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  background: rgba(100, 116, 139, 0.9);
  border-radius: 50%;
  width: 20px;
  height: 20px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
  box-shadow: 0 1px 4px rgba(0,0,0,0.1);

  .draft-icon {
    font-size: 10px;
  }
}

.picture-content {
  padding: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.user-avatar {
  width: 24px !important;
  height: 24px !important;
  border-radius: 50%;
  flex-shrink: 0;
  transition: opacity 0.2s ease;
  opacity: 0;
  border: 1px solid var(--border-color);
}

.user-avatar.loaded {
  opacity: 1;
}

.user-name {
  font-size: 12px;
  color: var(--text-primary);
  font-weight: 500;
  max-width: calc(100% - 30px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.picture-title {
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  max-height: 34px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  min-height: 400px;
  background: var(--card-background);
  border-radius: 8px;
  margin: 16px auto;
  box-sizing: border-box;
  max-width: 500px;
  border: 1px solid var(--border-color);

  @media screen and (max-width: 480px) {
    margin: 8px;
    min-height: 300px;
  }

  .empty-text {
    margin-top: 20px;

    h3 {
      font-size: 18px;
      color: var(--text-primary);
      margin-bottom: 6px;
      font-weight: 500;
    }

    p {
      font-size: 14px;
      color: var(--text-secondary);
      margin: 0;
    }
  }
}

.skeleton-wrapper {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: var(--hover-background);
  overflow: hidden;
  border-radius: 4px 4px 0 0;
}

.review-modal {
  :deep(.ant-modal-content) {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 10px 20px var(--shadow-color);
    border: 1px solid var(--border-color);
    background: var(--card-background);
  }

  :deep(.ant-modal-header) {
    border-bottom: 1px solid var(--border-color);
    padding: 16px 20px;
    background: var(--card-background);
  }

  :deep(.ant-modal-title) {
    font-size: 16px;
    font-weight: 500;
    text-align: center;
    color: var(--text-primary);
  }

  :deep(.ant-modal-body) {
    padding: 20px;
    background: var(--card-background);
  }
}

.review-detail {
  text-align: center;
  padding: 20px;
  background: var(--hover-background);
  border-radius: 8px;
}

.status-icon-large {
  font-size: 40px;
  margin-bottom: 16px;

  .pending-icon {
    color: #0088ff;
  }

  .approved-icon {
    color: #00c805;
  }

  .rejected-icon {
    color: #ff4d4f;
  }
}

.review-message {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.5;
  padding: 0 8px;
}

@media screen and (max-width: 768px) {
  .masonry-grid {
    gap: 2px;
  }

  .masonry-column {
    gap: 12px;
  }

  .note-picture-list {
    padding: 6px 2px;
  }

  .masonry-item {
    margin-bottom: 3px;

    &:last-child {
      margin-bottom: 6px;
    }
  }

  .view-count-badge,
  .review-text,
  .feature-badge,
  .draft-badge {
    padding: 2px 6px;
    font-size: 10px;
  }

  .review-modal {
    :deep(.ant-modal-content) {
      border-radius: 8px;
    }

    :deep(.ant-modal-header) {
      padding: 14px 16px;
    }

    :deep(.ant-modal-title) {
      font-size: 15px;
    }

    :deep(.ant-modal-body) {
      padding: 16px;
    }
  }

  .review-detail {
    padding: 16px;
  }

  .status-icon-large {
    font-size: 36px;
    margin-bottom: 14px;
  }

  .review-message {
    font-size: 14px;
    padding: 0 6px;
  }
}

@media screen and (max-width: 480px) {
  .masonry-item {
    &:last-child {
      margin-bottom: 3px;
    }
  }

  .picture-content {
    padding: 6px;
  }

  .user-avatar {
    width: 22px !important;
    height: 22px !important;
  }

  .user-name {
    font-size: 11px;
  }

  .picture-title {
    font-size: 11px;
    -webkit-line-clamp: 2;
    max-height: 31px;
  }
}
</style>
