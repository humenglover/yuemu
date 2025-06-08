<template>
  <div class="mobile-picture-list">
    <!-- 图片列表 -->
    <div v-if="!loading && (!dataList || dataList.length === 0)" class="empty-state">
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
    <a-list
      v-else
      :grid="{ gutter: 16, xs: 1, sm: 2, md: 3, lg: 4, xl: 5, xxl: 6 }"
      :data-source="dataList"
      :loading="loading"
    >
      <template #renderItem="{ item: picture }">
        <a-list-item class="picture-item">
          <!-- 单张图片卡片 -->
          <a-card class="picture-card" hoverable @click="doClickPicture(picture)">
            <!-- 图片封面 -->
            <template #cover>
              <div class="image-wrapper">
                <div class="image-container">
                  <img
                    :alt="picture.name"
                    :src="`${picture.thumbnailUrl ?? picture.url}?${new Date().getTime()}`"
                    @load="handleImageLoad(picture)"
                    @error="handleImageError(picture)"
                  />
                </div>
              </div>
            </template>

            <!-- 操作按钮 -->
            <template v-if="showOp" #actions>
              <!-- 我的发布页面显示审核信息 -->
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
              <!-- 其他页面显示操作按钮 -->
              <div v-else class="operation-buttons">
                <a-button
                  v-if="canEdit"
                  type="link"
                  class="action-button edit-button"
                  @click="(e) => doEdit(picture, e)"
                >
                  <EditOutlined />
                </a-button>
                <a-button
                  type="link"
                  class="action-button search-button"
                  @click="(e) => doSearch(picture, e)"
                >
                  <SearchOutlined />
                </a-button>
                <a-button
                  v-if="canDelete"
                  type="link"
                  class="action-button delete-button"
                  @click="(e) => doDelete(picture, e)"
                >
                  <DeleteOutlined />
                </a-button>
              </div>
            </template>
          </a-card>
        </a-list-item>
      </template>
    </a-list>

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
import { useRouter, useRoute } from 'vue-router'
import ShareModal from '@/components/ShareModal.vue'
import { h, onMounted, reactive, ref, nextTick, computed } from 'vue'
import {
  DeleteOutlined,
  EditOutlined,
  SearchOutlined,
  CloseOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ArrowRightOutlined,
  SmileOutlined,
  MessageOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController.ts'
import { message, Modal } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import CommentList from '@/components/CommentList.vue'
import { addCommentUsingPost, queryCommentUsingPost } from '@/api/commentsController.ts'
import { throttle } from 'vant/es/lazyload/vue-lazyload/util'

import { DEVICE_TYPE_ENUM } from '@/constants/device.ts'
import { getDeviceType } from '@/utils/device.ts'
import EmojiPicker from '@/components/EmojiPicker.vue'
import { doLikeUsingPost } from '@/api/likeRecordController.ts'
import { doShareUsingPost } from '@/api/shareRecordController.ts'

const loginUserStore = useLoginUserStore()
const currPicture = ref<API.PictureVO>()
const isEndOfData = ref(false)
// 添加设备类型检测
const device = ref<string>('')

onMounted(async () => {
  device.value = await getDeviceType()
  currPicture.value = props.dataList[0]
})

interface Props {
  dataList?: API.PictureVO[]
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
// 处理图片加载完成事件，根据宽高比设置行内样式
const handleImageLoad = (picture: API.PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  const width = imgElement.width
  const ratio = picture.picScale
  if (ratio && ratio > 0.58) {
    imgElement.style.width = '100%'
    imgElement.style.height = '100%'
    imgElement.style.objectFit = 'cover'
  } else if (ratio) {
    imgElement.style.width = '100%'
    imgElement.style.height = `${width * 1.78}px`
    imgElement.style.objectFit = 'cover'
    imgElement.style.objectPosition = 'center'
  }
}

// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

const doSearch = (picture, e) => {
  e.stopPropagation()
  router.push({
    path: `/search_picture`,
    query: {
      pictureId: picture.id,
    },
  })
}

// 编辑
const doEdit = (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  // 跳转时一定要携带 spaceId
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 删除数据，使用ant-design-vue的Modal.confirm进行删除提示
const doDelete = async (picture, e) => {
  // 阻止冒泡
  e.stopPropagation()
  const id = picture.id
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该图片吗？此操作不可撤销哦！',
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      const res = await deletePictureUsingPost({ id })
      if (res.data.code === 0) {
        message.success('删除成功，数据更新可能需要一段时间')
        props.onReload?.()
      } else {
        message.error('删除失败')
      }
    },
  })
}

// 处理图片加载错误的函数
const handleImageError = (picture: API.PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  imgElement.src = picture.url // 将图片src替换为picture.url
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

// 审核弹窗相关
const reviewModalVisible = ref(false)
const currentPicture = ref<API.PictureVO>()

const showReviewModal = (picture: API.PictureVO) => {
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
</script>

<style scoped>
.mobile-picture-list {
  padding: 4px;
  width: 100%;
  margin: 0 auto;
}

.picture-item {
  margin-bottom: 16px;
  width: 100% !important;
}

:deep(.ant-list-items) {
  width: 100%;
}

:deep(.ant-list-item) {
  width: 100% !important;
  padding: 0 !important;
}

.picture-card {
  width: 100% !important;
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  }

  :deep(.ant-card-body) {
    background: rgba(255, 255, 255, 0.8);
  }

  :deep(.ant-card-actions) {
    background: rgba(255, 255, 255, 0.8);
    border-top: 1px solid rgba(0, 0, 0, 0.05);
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 66%;
  background: #f8fafc;
  border-radius: 12px 12px 0 0;
  overflow: hidden;
}

.image-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.picture-card:hover .image-container img {
  transform: scale(1.05);
}

.operation-buttons {
  display: flex;
  justify-content: space-around;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.8);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border-radius: 6px;
  transition: all 0.3s ease;

  .anticon {
    font-size: 16px;
  }

  &:hover {
    background: rgba(0, 0, 0, 0.02);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(1px);
  }
}

.edit-button {
  color: #ff8e53;

  &:hover {
    color: #ff7a3d;
    background: rgba(255, 142, 83, 0.1);
  }
}

.search-button {
  color: #45b090;

  &:hover {
    color: #3a9579;
    background: rgba(69, 176, 144, 0.1);
  }
}

.delete-button {
  color: #ff6b6b;

  &:hover {
    color: #ff5252;
    background: rgba(255, 107, 107, 0.1);
  }
}

/* 评论抽屉样式 */
.comments-drawer {
  :deep(.ant-drawer-content) {
    border-radius: 0;
  }

  :deep(.ant-drawer-content-wrapper) {
    /* PC 端右侧抽屉样式 */
    @media screen and (min-width: 769px) {
      box-shadow: -4px 0 16px rgba(0, 0, 0, 0.08);
    }
  }

  /* 移动端底部抽屉样式 */
  @media screen and (max-width: 768px) {
    :deep(.ant-drawer-content) {
      border-radius: 16px 16px 0 0;
    }
  }

  :deep(.ant-drawer-header) {
    padding: 16px 24px;
    border-bottom: 1px solid #f0f0f0;
  }

  :deep(.ant-drawer-body) {
    padding: 0;
  }
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}


.review-status {
  padding: 8px;
  text-align: center;
}

.review-message {
  margin-left: 8px;
  color: #ff4d4f;
  font-size: 12px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .review-status {
    padding: 4px;
  }

  .review-message {
    display: block;
    margin-top: 4px;
    margin-left: 0;
  }
}

.review-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.status-icon {
  font-size: 16px;

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
  font-size: 14px;
}

/* 审核弹窗样式 */
.review-detail {
  text-align: center;
  padding: 24px;
}

.status-icon-large {
  font-size: 48px;
  margin-bottom: 16px;

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
  color: #1f2937;
  line-height: 1.5;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .review-button {
    padding: 4px 8px;
  }

  .status-icon {
    font-size: 14px;
  }

  .status-text {
    font-size: 13px;
  }

  .review-detail {
    padding: 16px;
  }

  .status-icon-large {
    font-size: 40px;
  }

  .review-message {
    font-size: 14px;
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
