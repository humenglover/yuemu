<template>
  <div class="grid-picture-list" ref="containerRef">
    <!-- 等高填充布局容器 -->
    <div class="justified-feed">
      <div
        v-for="(row, rowIndex) in justifiedRows"
        :key="rowIndex"
        class="justified-row"
        :style="{ height: `${row.height}px` }"
      >
        <div
          v-for="picture in row.pictures"
          :key="picture.id"
          class="justified-item"
          :style="{ width: `${picture.rowWidth}px` }"
          @click="doClickPicture(picture)"
        >
          <!-- 图片主体 -->
          <div class="image-wrapper">
            <img
              :alt="picture.name || '图片'"
              class="j-image"
              :src="picture.thumbnailUrl || picture.url"
              :class="{ loaded: imageLoadedMap[picture.id] }"
              @load="() => imageLoadedMap[picture.id] = true"
            />

            <!-- 悬浮遮罩层 -->
            <div class="info-overlay">
              <div class="info-header">
                <!-- 状态标识点 (左上角) -->
                <div v-if="isMyPosts && picture.isDraft !== 1" class="status-dot-wrap" @click.stop="showReviewModal(picture)">
                  <div class="status-dot" :class="['status-' + picture.reviewStatus]"></div>
                </div>

                <!-- 徽章与流量 (右上角) -->
                <div class="header-right">
                  <div class="item-badges">
                    <div v-if="picture.isFeature === 1" class="badge-tag feature" title="精选">
                      <i class="fas fa-crown"></i>
                    </div>
                    <div v-if="picture.isDraft === 1" class="badge-tag draft" title="草稿">
                      <i class="fas fa-file-alt"></i>
                    </div>
                  </div>
                  <div v-if="picture.isDraft !== 1" class="view-item">
                    <i class="fas fa-eye"></i>
                    <span>{{ formatNumber(picture.viewCount) }}</span>
                  </div>
                </div>
              </div>

              <!-- 底部操作与标题 -->
              <div class="info-footer">
                <div class="pic-name">{{ picture.name || '未命名图片' }}</div>
                <div class="footer-bottom">
                  <div class="action-buttons" v-if="showOp">
                    <a-button v-if="canEdit" type="text" class="mini-btn edit" @click.stop="doEdit(picture)">
                      <i class="fas fa-edit"></i>
                    </a-button>
                    <a-button type="text" class="mini-btn search" @click.stop="doSearch(picture)">
                      <i class="fas fa-search"></i>
                    </a-button>
                    <a-button v-if="canDelete" type="text" class="mini-btn delete" @click.stop="doDelete(picture)">
                      <i class="fas fa-trash-alt"></i>
                    </a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="skeleton-wrap">
      <div v-for="i in 3" :key="i" class="skeleton-row">
        <div v-for="j in 4" :key="j" class="skeleton-cell"></div>
      </div>
    </div>

    <!-- 没有更多数据 -->
    <div v-if="!hasMore && !loading && justifiedRows.length > 0" class="no-more-data">
      <span>没有更多了</span>
    </div>

    <!-- 删除确认对话框 -->
    <a-modal v-model:open="deleteModalVisible" title="确认删除" :footer="null" @cancel="cancelDelete">
      <div class="delete-modal-content">
        <p>确定要删除这张图片吗？此操作不可恢复。</p>
        <div class="delete-modal-buttons">
          <a-button @click="cancelDelete">取消</a-button>
          <a-button type="primary" danger @click="confirmDelete" :loading="deleteLoading">删除</a-button>
        </div>
      </div>
    </a-modal>

    <!-- 审核详情弹窗 (复用之前大图模式的逻辑) -->
    <a-modal v-model:open="reviewModalVisible" title="审核状态说明" :footer="null">
      <div class="review-detail-box">
        <div class="status-icon-box">
          <i class="far fa-clock" v-if="currentPicture?.reviewStatus === 0"></i>
          <i class="far fa-check-circle" v-else-if="currentPicture?.reviewStatus === 1"></i>
          <i class="far fa-times-circle" v-else-if="currentPicture?.reviewStatus === 2"></i>
        </div>
        <div class="status-msg">
          <template v-if="currentPicture?.reviewStatus === 0">图片正在审核，请耐心等待排队。</template>
          <template v-else-if="currentPicture?.reviewStatus === 1">审核已通过，感谢您的分享！</template>
          <template v-else-if="currentPicture?.reviewStatus === 2">{{ currentPicture?.reviewMessage || '抱歉，图片未通过平台规则。' }}</template>
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
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import PictureDetailView from '@/components/PictureDetailView.vue'
import { deletePictureUsingPost } from '@/api/pictureController'
import { throttle } from 'lodash-es'
import { getDeviceType } from '@/utils/device'
import { DEVICE_TYPE_ENUM } from '@/constants/device'

interface Props {
  dataList: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
  hasMore?: boolean
  onLoadMore?: () => void
  isMyPosts?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
  onReload: () => {},
  hasMore: true,
  onLoadMore: () => {},
  isMyPosts: false
})

const router = useRouter()
const containerRef = ref<HTMLElement | null>(null)
const containerWidth = ref(0)

// 响应式配置
const isMobile = window.innerWidth <= 768
const GAP = isMobile ? 4 : 12
// Grid 模式基准高度设定比 Big 模式稍小，以容纳更多内容
const TARGET_HEIGHT = isMobile ? 220 : 320
const MOBILE_MAX_ITEMS = 3

const imageLoadedMap = reactive<Record<string | number, boolean>>({})
const reviewModalVisible = ref(false)
const currentPicture = ref<any | null>(null)

// 详情弹窗相关
const detailModalVisible = ref(false)
const selectedPictureId = ref<string | number | null>(null)
const selectedPictureData = ref<any | null>(null)

/** 核心算法：Justified Grid (等高瀑布流) */
const justifiedRows = computed(() => {
  const list = props.dataList
  if (!list || list.length === 0) return []

  const availableWidth = (containerWidth.value || window.innerWidth) - 8
  const rows: any[] = []
  let currentRow: any[] = []
  let rowNaturalWidth = 0

  const buildRow = (items: any[], isLast: boolean) => {
    const totalGaps = GAP * (items.length - 1)
    const usableWidth = Math.max(0, availableWidth - totalGaps)
    const totalScale = items.reduce((sum, item) => sum + (item.picScale || 1.1), 0)

    let rowHeight: number
    const naturalRowWidth = totalScale * TARGET_HEIGHT

    // 强制铺满逻辑：非最后一行或占宽 45% 以上则拉伸
    if (isLast && naturalRowWidth < availableWidth * 0.45) {
      rowHeight = TARGET_HEIGHT
    } else {
      rowHeight = totalScale > 0 ? usableWidth / totalScale : TARGET_HEIGHT
      const maxH = TARGET_HEIGHT * 2.2
      const minH = TARGET_HEIGHT * 0.5
      if (rowHeight > maxH) rowHeight = maxH
      if (rowHeight < minH) rowHeight = minH
    }

    return {
      height: rowHeight,
      pictures: items.map(item => ({
        ...item,
        rowWidth: (item.picScale || 1.1) * rowHeight
      })),
      isLastRow: isLast
    }
  }

  for (let i = 0; i < list.length; i++) {
    const item = list[i]
    currentRow.push(item)
    rowNaturalWidth += (item.picScale || 1.1) * TARGET_HEIGHT

    const gaps = GAP * (currentRow.length - 1)
    const projectedWidth = rowNaturalWidth + gaps

    if (projectedWidth >= availableWidth * 0.82 || (isMobile && currentRow.length >= MOBILE_MAX_ITEMS)) {
      rows.push(buildRow(currentRow, false))
      currentRow = []
      rowNaturalWidth = 0
    } else if (i === list.length - 1) {
      rows.push(buildRow(currentRow, true))
    }
  }
  return rows
})

const handleResize = throttle(() => {
  if (containerRef.value) {
    containerWidth.value = containerRef.value.clientWidth
  }
}, 200)

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('scroll', handleScroll)
})

const formatNumber = (num?: number) => {
  if (!num) return '0'
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const doClickPicture = (picture: any) => {
  const deviceType = getDeviceType()
  if (deviceType === DEVICE_TYPE_ENUM.MOBILE) {
    if (picture.isDraft === 1) {
      router.push({ path: '/add_picture', query: { id: picture.id } })
    } else {
      router.push({
        path: `/mobile/picture/${picture.id}`,
        state: { pictureData: JSON.parse(JSON.stringify(picture)) }
      })
    }
  } else {
    selectedPictureId.value = picture.id
    selectedPictureData.value = picture
    detailModalVisible.value = true
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

const showReviewModal = (picture: any) => {
  currentPicture.value = picture
  reviewModalVisible.value = true
}

// 操作方法
const doEdit = (picture: any) => {
  router.push({ path: '/add_picture', query: { id: picture.id, spaceId: picture.spaceId } })
}

const doSearch = (picture: any) => {
  router.push({ path: '/search_picture', query: { pictureId: picture.id } })
}

const deleteModalVisible = ref(false)
const deleteLoading = ref(false)
const pictureToDelete = ref<any | null>(null)

const doDelete = (picture: any) => {
  pictureToDelete.value = picture
  deleteModalVisible.value = true
}

const cancelDelete = () => {
  deleteModalVisible.value = false
  pictureToDelete.value = null
}

const confirmDelete = async () => {
  if (!pictureToDelete.value) return
  deleteLoading.value = true
  try {
    const res = await deletePictureUsingPost({ id: pictureToDelete.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      props.onReload?.()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('操作失败，请重试')
  } finally {
    deleteLoading.value = false
    deleteModalVisible.value = false
    pictureToDelete.value = null
  }
}

const SCROLL_THRESHOLD = 300
const handleScroll = throttle(() => {
  if (props.loading || !props.hasMore || !props.onLoadMore) return
  const scrollHeight = document.documentElement.scrollHeight
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const clientHeight = document.documentElement.clientHeight
  if (scrollHeight - scrollTop - clientHeight < SCROLL_THRESHOLD) {
    props.onLoadMore()
  }
}, 200)

</script>

<style lang="scss" scoped>
.grid-picture-list {
  width: 100%;
  padding: 0 4px;
  box-sizing: border-box;
  overflow-x: hidden;
  max-width: 1440px;
  margin: 0 auto;
}

.justified-feed {
  width: 100%;
}

.justified-row {
  display: flex;
  width: 100%;
  box-sizing: border-box;
  gap: 4px;
  margin-bottom: 4px;

  @media (min-width: 769px) {
    gap: 12px;
    margin-bottom: 12px;
  }
}

.justified-item {
  position: relative;
  overflow: hidden;
  border-radius: 4px;
  cursor: pointer;
  background-color: var(--hover-background);
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.j-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  opacity: 0;
  transition: opacity 0.3s ease, transform 0.5s ease;
  &.loaded { opacity: 1; }
}

/* 核心：悬浮信息层适配 Grid */
.info-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 40%, transparent 60%, rgba(0,0,0,0.3) 100%);
  opacity: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px;
  pointer-events: none;
  transition: all 0.3s ease;
  z-index: 3;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.item-badges {
  display: flex;
  gap: 4px;
}

.badge-tag {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  backdrop-filter: blur(4px);
  &.feature { background: rgba(59, 130, 246, 0.9); }
  &.draft { background: rgba(100, 116, 139, 0.9); }
}

.view-item {
  color: #fff;
  font-size: 10px;
  background: rgba(0,0,0,0.3);
  padding: 1px 6px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
}

.info-footer {
  width: 100%;
}

.pic-name {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  text-shadow: 0 1px 3px rgba(0,0,0,0.5);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-buttons {
  display: flex;
  gap: 6px;
  pointer-events: auto;
}

.mini-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  border-radius: 50%;
  background: rgba(255,255,255,0.2);
  color: #fff;
  border: none;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;

  &:hover {
    background: rgba(255,255,255,0.4);
    transform: scale(1.1);
  }
}

.justified-item:hover {
  .info-overlay { opacity: 1; }
  .j-image { transform: scale(1.04); }
}

/* 状态点 */
.status-dot-wrap {
  background: rgba(255,255,255,0.85);
  padding: 4px;
  border-radius: 50%;
  display: flex;
  cursor: pointer;
  pointer-events: auto;
  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    &.status-0 { background: #1890ff; }
    &.status-1 { background: #52c41a; }
    &.status-2 { background: #ff4d4f; }
  }
}

/* 骨架屏 */
.skeleton-wrap { display: flex; flex-direction: column; gap: 10px; padding: 10px; }
.skeleton-row { display: flex; gap: 8px; height: 180px; }
.skeleton-cell { flex: 1; background: var(--hover-background); border-radius: 4px; animation: pulse 1.5s infinite; }
@keyframes pulse { 50% { opacity: 0.5; } }

.no-more-data { text-align: center; padding: 20px; color: var(--text-secondary); }
.delete-modal-content { text-align: center; padding: 20px; }
.delete-modal-buttons { margin-top: 20px; display: flex; justify-content: center; gap: 12px; }
.review-detail-box { text-align: center; padding: 20px; .status-icon-box { font-size: 40px; margin-bottom: 12px; } }
</style>
