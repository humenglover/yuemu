<template>
  <div class="mobile-picture-list">
    <!-- 图片列表 -->
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
          <p>快去上传一些精彩的照片吧 (｡•́︿•̀｡)</p>
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
              backgroundColor: picture.picColor ? `#${picture.picColor.substring(2)}` : '#f8f8f8',
              '--hover-color': picture.picColor ? `#${picture.picColor.substring(2)}` : '#f8f8f8',
            }"
            @click="doClickPicture(picture)"
          >
            <div class="image-wrapper">
              <div
                class="aspect-ratio-box"
                :style="{
                  paddingTop: `${(() => {
                    const ratio = picture.picScale || 1
                    // 如果是横图（宽高比大于4:3）
                    if (ratio > 4/3) {
                      return (3/4) * 100 // 限制为4:3
                    }
                    // 如果是竖图，保持原比例，但最大不超过7:5
                    return Math.min((1 / ratio), 7/5) * 100
                  })()}%`
                }"
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
                  @load="(e) => handleImageLoad(e, picture)"
                  @error="handleImageError(picture)"
                />
                <!-- 图片信息浮层 -->
                <div class="picture-overlay">
                  <div class="picture-info">
                    <div class="picture-header">
                      <div class="picture-user" >
                        <img
                          class="user-avatar"
                          :src="picture.user?.userAvatar || getDefaultAvatar(picture.user?.userName)"
                          @load="(e) => handleAvatarLoad(e, picture)"
                        />
                        <span class="user-name">{{ picture.user?.userName }}</span>
                      </div>
                      <div class="view-count">
                        <van-icon name="eye-o" size="16" color="#fff" />
                        <span>{{ formatNumber(picture.viewCount) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <!-- 照片描述 -->
            <div class="picture-title">{{ picture.name }}</div>
            <!-- 审核状态 -->
            <div v-if="isMyPosts" class="review-status">
              <a-button type="link" class="review-button" @click.stop="showReviewModal(picture)">
                <template v-if="picture.reviewStatus === 0">
                  <ClockCircleOutlined class="status-icon pending" />
                  <span class="status-text">待审核</span>
                </template>
                <template v-else-if="picture.reviewStatus === 1">
                  <CheckCircleOutlined class="status-icon approved" />
                  <span class="status-text">已通过</span>
                </template>
                <template v-else-if="picture.reviewStatus === 2">
                  <CloseCircleOutlined class="status-icon rejected" />
                  <span class="status-text">已拒绝</span>
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-wrapper">
        <van-loading type="spinner" color="#ff8e53" size="24px" />
        <span class="loading-text">加载中...</span>
      </div>
      <!-- 没有更多数据 -->
      <div v-if="isEndOfData" class="no-more-data">
        <span>没有更多了</span>
      </div>
    </div>

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
import { h, onMounted, onUnmounted, reactive, ref, nextTick, computed, watch } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  CloseOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  SmileOutlined,
  MessageOutlined,
  ArrowRightOutlined,
  CrownOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController'
import { throttle } from 'lodash-es'
// import EmojiPicker from "vue3-emoji-picker";
import EmojiPicker from '@/components/EmojiPicker.vue'
import '@lottiefiles/lottie-player'

import { doLikeUsingPost } from '@/api/likeRecordController'
import { doShareUsingPost } from '@/api/shareRecordController'
import { getDefaultAvatar } from '@/utils/userUtils'

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
  name: string
  picScale?: number
  picColor?: string
  isFeature?: number
  reviewStatus?: number
  reviewMessage?: string
  viewCount?: number
  avatarLoaded?: boolean
  user?: User
}

const loginUserStore = useLoginUserStore()
const currPicture = ref<PictureVO>()
const isEndOfData = ref(false)
onMounted(async () => {
  currPicture.value = props.dataList[0]
  // console.log(props.dataList)
})

interface Props {
  dataList?: PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  isMyPosts?: boolean
  canEdit?: boolean
  canDelete?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  onReload: () => {},
  isMyPosts: false,
  canEdit: false,
  canDelete: false,
})

const router = useRouter()

// 处理图片加载完成事件
const handleImageLoad = (event: Event, picture: PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  if (!imgElement) return

  // 处理宽高比
  const ratio = picture.picScale || 1
  const MAX_RATIO = 5/7

  imgElement.style.width = '100%'
  imgElement.style.height = '100%'
  imgElement.style.objectFit = 'cover'
  imgElement.style.objectPosition = ratio > MAX_RATIO ? 'center center' : 'center'
}

// 添加头像加载状态
const handleAvatarLoad = (event: Event, picture: PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  if (!imgElement) return

  picture.avatarLoaded = true
  imgElement.classList.add('loaded')
}

// 处理用户点击
const handleUserClick = (user: User | undefined) => {
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

// 跳转至图片详情页
const doClickPicture = (picture: PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 处理图片加载错误
const handleImageError = (picture: PictureVO, event: Event) => {
  const imgElement = event.target as HTMLImageElement
  if (!imgElement) return
  imgElement.src = picture.url || '/default-image.png'
}

// 格式化数字的函数，将较大数字转换为带k、w的格式，保留两位小数
const formatNumber = (num: number): string => {
  if (num >= 10000) {
    const wan = (num / 10000).toFixed(2)
    return `${wan}w`
  } else if (num >= 1000) {
    const qian = (num / 1000).toFixed(2)
    return `${qian}k`
  }
  return num.toString()
}
let visible = ref(false)
// 处理返回键
const handleBackButton = () => {

  if (visible.value) {
    visible.value = false
    return true
  }
  return false
}

// 监听返回键
onMounted(() => {
  window.addEventListener('popstate', () => {
    if (handleBackButton()) {
      // 阻止默认的返回行为
      history.pushState(null, '', document.URL)
    }
  })

  // 初始化时添加一个历史记录，用于触发 popstate
  history.pushState(null, '', document.URL)
})

// 清理监听器
onUnmounted(() => {
  window.removeEventListener('popstate', handleBackButton)
})

// 审核弹窗相关
const reviewModalVisible = ref(false)
const currentPicture = ref<PictureVO>()

const showReviewModal = (picture: PictureVO) => {
  currentPicture.value = picture
  reviewModalVisible.value = true
}

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

// 瀑布流容器ref
const masonryRef = ref<HTMLElement | null>(null)
const gridRef = ref<HTMLElement | null>(null)

// 修改计算列数的函数
const getColumnCount = () => {
  const width = window.innerWidth
  if (width > 768) return 3
  if (width > 480) return 2
  return 2 // 手机端也保持2列
}

// 计算分列数据
const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: PictureVO[][] = Array.from({ length: columnCount }, () => [])
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
    const aspectRatio = item.picScale || 1
    const imageHeight = (gridRef.value?.clientWidth || window.innerWidth) / columnCount / aspectRatio
    columnHeights[minHeightIndex] += imageHeight + 120 // 120px是卡片其他内容的高度
  })

  return cols
})

// 使用 ResizeObserver 监听容器大小变化
let resizeObserver: ResizeObserver | null = null

// 处理容器大小变化
const handleContainerResize = throttle(() => {
  if (gridRef.value) {
    // 触发重新计算列
    columns.value = computed(() => {
      const columnCount = getColumnCount()
      const cols: PictureVO[][] = Array.from({ length: columnCount }, () => [])
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
        const aspectRatio = item.picScale || 1
        const imageHeight = (gridRef.value?.clientWidth || window.innerWidth) / columnCount / aspectRatio
        columnHeights[minHeightIndex] += imageHeight + 120 // 120px是卡片其他内容的高度
      })

      return cols
    }).value
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
</script>

<style lang="scss" scoped>
.mobile-picture-list {
  width: 100%;
  background-color: #fafafa;
  z-index: 0;
}

.masonry-wrapper {
  width: 100%;
  padding: 0;
}

.masonry-grid {
  display: flex;
  gap: 12px;
  width: 100%;
  padding: 0;
  margin: 0;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.masonry-item {
  width: 100%;
  margin: 0;
  break-inside: avoid;
  background: var(--hover-color, #f8f8f8);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 16px;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  }

  @media screen and (max-width: 480px) {
    margin-bottom: 12px;
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #f8f8f8;
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
  object-fit: cover;
  transition: transform 0.3s ease;
}

.picture-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.6), rgba(0, 0, 0, 0.3) 50%, transparent);
  padding: 12px;
  color: #fff;
  transition: opacity 0.3s ease;
}

.picture-info {
  position: relative;

}

.picture-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

.picture-user {
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  min-width: 0;

  .user-avatar {
    width: 20px !important;
    height: 20px !important;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.3);
    flex-shrink: 0;
  }

  .user-name {
    font-size: 12px;
    color: #fff;
    font-weight: normal;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }
}

.view-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #fff;
  flex-shrink: 0;
}

.feature-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #ff8e53 0%, #ff6b6b 100%);
  padding: 4px 8px;
  border-radius: 6px;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 1;

  box-shadow: 0 2px 6px rgba(255, 142, 83, 0.3);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.2);

  :deep(.anticon) {
    font-size: 11px;
  }
}

.review-status {
  position: absolute;
  top: 12px;
  right: 12px;

}

.review-button {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(4px);
  border-radius: 6px;
  padding: 2px 6px;
  font-size: 11px;
  height: auto;
  display: flex;
  align-items: center;
  gap: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);

  .status-icon {
    font-size: 12px;
  }

  .status-text {
    font-size: 11px;
    margin-left: 2px;
  }
}

.loading-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px;
}

.loading-text {
  font-size: 14px;
  color: #94a3b8;
}

.no-more-data {
  text-align: center;
  padding: 24px;
  color: #94a3b8;
  font-size: 14px;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .masonry-wrapper {
    padding: 4px;
  }

  .masonry-grid {
    gap: 4px;
  }

  .masonry-column {
    gap: 4px;
  }

  .picture-actions {
    gap: 2px;
    padding: 0 2px;
    margin-top: 8px;
  }

  .action-item {
    padding: 4px;
    min-width: auto;

    span {
      font-size: 12px;
    }

    .action-icon {
      font-size: 18px;
    }
  }
}

@media screen and (max-width: 375px) {

  .action-item {
    padding: 6px;

    .action-icon {
      font-size: 20px;
    }
  }
}

.picture-title {
  font-size: 13px;
  color: #333333;
  padding: 8px 12px;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  @media screen and (max-width: 480px) {
    font-size: 12px;
    padding: 8px 10px;
  }
}

.interaction-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: flex-start;
  margin-top: 12px;
}


@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
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

.status-text {
  font-size: 12px;
  margin-left: 2px;
}

@media screen and (max-width: 480px) {
  .masonry-item {
    margin-bottom: 8px;
  }

  .picture-overlay {
    padding: 12px;
  }

  .picture-user {
    .user-avatar {
      width: 20px !important;
      height: 20px !important;
    }

    .user-name {
      font-size: 12px;
    }
  }

  .view-count {
    font-size: 12px;
  }
}

/* 审核弹窗样式优化 */
.review-modal {
  :deep(.ant-modal-content) {
    border-radius: 16px;
    overflow: hidden;
  }

  :deep(.ant-modal-header) {
    border-bottom: none;
    padding: 20px 24px 0;
  }

  :deep(.ant-modal-title) {
    font-size: 18px;
    font-weight: 600;
    text-align: center;
  }

  :deep(.ant-modal-body) {
    padding: 20px 24px 24px;
  }
}

.review-detail {
  text-align: center;
  padding: 24px;
  background: #f8fafc;
  border-radius: 12px;
}

.status-icon-large {
  font-size: 48px;
  margin-bottom: 20px;

  .pending {
    color: #1890ff;
    filter: drop-shadow(0 4px 8px rgba(24, 144, 255, 0.2));
  }

  .approved {
    color: #52c41a;
    filter: drop-shadow(0 4px 8px rgba(82, 196, 26, 0.2));
  }

  .rejected {
    color: #ff4d4f;
    filter: drop-shadow(0 4px 8px rgba(255, 77, 79, 0.2));
  }
}

.review-message {
  font-size: 16px;
  color: #1f2937;
  line-height: 1.6;
  padding: 0 16px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .review-modal {
    :deep(.ant-modal-content) {
      border-radius: 12px;
    }

    :deep(.ant-modal-header) {
      padding: 16px 20px 0;
    }

    :deep(.ant-modal-title) {
      font-size: 16px;
    }

    :deep(.ant-modal-body) {
      padding: 16px 20px 20px;
    }
  }

  .review-detail {
    padding: 20px;
  }

  .status-icon-large {
    font-size: 40px;
    margin-bottom: 16px;
  }

  .review-message {
    font-size: 14px;
    padding: 0 12px;
  }
}

@keyframes bounce {
  0%, 100% {
    transform: scale(0.8) translateY(0);
  }
  50% {
    transform: scale(0.8) translateY(-10px);
  }
}

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

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .pet-animation {
    right: 10px;
    bottom: 80px;
    transform: scale(0.6);
  }
}

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

