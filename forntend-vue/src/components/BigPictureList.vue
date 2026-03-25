<template>
  <div class="big-picture-list" ref="containerRef">
    <!-- 骨架屏：模拟等高流布局 -->
    <div v-if="loading && (!props.dataList || props.dataList.length === 0)" class="skeleton-wrap">
      <div class="skeleton-row" style="height: 200px">
        <div class="skeleton-cell" style="flex: 1.5"></div>
        <div class="skeleton-cell" style="flex: 1"></div>
        <div class="skeleton-cell" style="flex: 1.2"></div>
      </div>
      <div class="skeleton-row" style="height: 240px">
        <div class="skeleton-cell" style="flex: 1"></div>
        <div class="skeleton-cell" style="flex: 1.8"></div>
      </div>
      <div class="skeleton-row" style="height: 180px">
        <div class="skeleton-cell" style="flex: 1.2"></div>
        <div class="skeleton-cell" style="flex: 1.3"></div>
        <div class="skeleton-cell" style="flex: 0.8"></div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading && (!props.dataList || props.dataList.length === 0)" class="empty-state">
      <div class="custom-empty-state">
        <img :src="emptyImage" alt="暂无内容" class="empty-illustration" />
        <h3 class="empty-title">暂无图片</h3>
        <p class="empty-desc">还没有上传任何图片哦</p>
      </div>
    </div>

    <!-- 等高行 Justified Grid -->
    <div v-else class="justified-feed" :style="{ opacity: containerWidth > 0 ? 1 : 0 }">
      <div
        v-for="(row, rowIndex) in justifiedRows"
        :key="rowIndex"
        class="justified-row"
        :style="{ height: row.height + 'px' }"
      >
        <div
          v-for="picture in row.pictures"
          :key="picture.id"
          class="justified-item"
          :style="{ width: picture.rowWidth + 'px' }"
          @click.stop.prevent="doClickPicture(picture)"
        >
          <div class="j-image-box">
            <!-- 占位底色/渐变 -->
            <div class="j-placeholder" v-if="!imageLoadedMap[picture.id]"></div>

            <img
              :src="picture.thumbnailUrl || picture.url || '/default-image.png'"
              :alt="picture.name || '图片'"
              class="j-image"
              :class="{ 'is-loaded': imageLoadedMap[picture.id] }"
              @load="handleImageLoad(picture.id)"
              @error="handleImageLoad(picture.id)"
            />

            <!-- 信息叠加层 (悬浮显示) -->
            <div class="info-overlay">
              <div class="info-header">
                <!-- 状态徽章 (移入此处) -->
                <div class="item-badges">
                  <div v-if="picture.isFeature === 1" class="badge-tag feature">精选</div>
                  <div v-else-if="picture.isDraft === 1" class="badge-tag draft">草稿</div>
                </div>

                <!-- 审核状态圆点 (悬浮可见) -->
                <div
                  v-if="isMyPosts && picture.isDraft !== 1"
                  class="review-status-dot-wrap"
                  :title="getStatusText(picture.reviewStatus)"
                  @click.stop="showReviewModal(picture)"
                >
                  <span class="status-dot" :class="'status-' + picture.reviewStatus"></span>
                </div>

                <span v-if="picture.isDraft !== 1" class="view-item"><i class="fas fa-eye"></i> {{ formatNumber(picture.viewCount) }}</span>
              </div>

              <div class="info-footer">
                <div class="pic-name">{{ picture.name || '未命名' }}</div>
                <div class="user-meta" v-if="picture.isDraft !== 1 && picture.user">
                  <img
                    :src="picture.user.userAvatar || getDefaultAvatar(picture.user.userName)"
                    class="u-avatar"
                    :class="{ 'loaded': avatarLoadedMap[picture.id] }"
                    @load="handleAvatarLoad(picture.id)"
                  />
                  <span class="u-name">{{ picture.user.userName }}</span>
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

    <!-- 审核弹窗 -->
    <a-modal
      v-model:open="reviewModalVisible"
      :title="getReviewModalTitle(currentPicture?.reviewStatus)"
      :footer="null"
      :centered="true"
    >
      <div class="review-detail-box">
        <div class="status-icon-box">
          <i class="fas fa-clock" v-if="currentPicture?.reviewStatus === 0"></i>
          <i class="fas fa-check-circle" v-else-if="currentPicture?.reviewStatus === 1"></i>
          <i class="fas fa-times-circle" v-else-if="currentPicture?.reviewStatus === 2"></i>
        </div>
        <p class="status-msg">
          <template v-if="currentPicture?.reviewStatus === 0">内容审核中心正在处理，请稍后查看</template>
          <template v-else-if="currentPicture?.reviewStatus === 1">该内容符合发布规范，已在公开列表中展示</template>
          <template v-else-if="currentPicture?.reviewStatus === 2">{{ currentPicture?.reviewMessage || '不符合发布规范' }}</template>
        </p>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Modal as AModal } from 'ant-design-vue'
import { getDefaultAvatar } from '@/utils/userUtils'
import emptyImage from '@/assets/illustrations/empty.png'
import { throttle } from 'lodash-es'
import PictureDetailView from './PictureDetailView.vue'

interface PictureVO {
  id: string | number
  url: string
  thumbnailUrl?: string
  name?: string
  picScale?: number
  isFeature?: number
  reviewStatus?: number
  reviewMessage?: string
  viewCount?: number
  isDraft?: number
  user?: {
    id: string | number
    userName: string
    userAvatar: string
  }
}

interface JustifiedRow {
  pictures: (PictureVO & { rowWidth: number })[]
  height: number
  isLastRow?: boolean
}

const props = withDefaults(defineProps<{
  dataList?: PictureVO[]
  loading?: boolean
  showOp?: boolean
  onReload?: () => void
  isMyPosts?: boolean
}>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  onReload: () => {},
  isMyPosts: false,
})

const router = useRouter()
const route = useRoute()
const containerRef = ref<HTMLElement | null>(null)
const containerWidth = ref(0)
const windowWidth = ref(window.innerWidth)

// 详情弹窗相关
const detailModalVisible = ref(false)
const selectedPictureId = ref<string | number | null>(null)
const selectedPictureData = ref<PictureVO | null>(null)

// 响应式配置
const isMobile = computed(() => windowWidth.value <= 768)
const GAP = computed(() => isMobile.value ? 4 : 12)
// 引导高度：移动端 300 让竖图有更舒适宽度；PC 340 保持 4 列主体
const TARGET_HEIGHT = computed(() => isMobile.value ? 300 : 340)
const CONTAINER_PADDING = computed(() => isMobile.value ? 2 : 8)
const MOBILE_MAX_ITEMS = 3 // 移动端每行上限强制为 3

// 使用普通的对象记录加载状态，保证响应式
const imageLoadedMap = reactive<Record<string | number, boolean>>({})
const avatarLoadedMap = reactive<Record<string | number, boolean>>({})

const reviewModalVisible = ref(false)
const currentPicture = ref<PictureVO | null>(null)

/** 核心算法：Justified Grid (等高瀑布流) */
const justifiedRows = computed(() => {
  const list = props.dataList
  if (!list || list.length === 0) return []

  // 准确计算可用宽度：当前容器宽度 - 容器内左右边距
  const availableWidth = (containerWidth.value || window.innerWidth) - CONTAINER_PADDING.value

  const rows: JustifiedRow[] = []
  let currentRow: any[] = []
  let rowNaturalWidth = 0

  const buildRow = (items: any[], isLast: boolean): JustifiedRow => {
    const totalGaps = GAP.value * (items.length - 1)
    const usableWidth = Math.max(0, availableWidth - totalGaps)
    const totalScale = items.reduce((sum, item) => sum + (item.picScale || 1.1), 0)

    let rowHeight: number
    const naturalRowWidth = totalScale * TARGET_HEIGHT.value

    // 强制铺满逻辑：
    // 如果不是绝对最后一行，或者最后一行已经占据了容器 45% 以上的宽度，就强制拉伸填满。
    // 这能有效解决分页过程中产生的“断层”和“空白色块”。
    if (isLast && naturalRowWidth < availableWidth * 0.45) {
      rowHeight = TARGET_HEIGHT.value
    } else {
      rowHeight = totalScale > 0 ? usableWidth / totalScale : TARGET_HEIGHT.value
      // 限制过度缩放，保护图片比例
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
    // 核心改进：取消 hardcoded maxItems，完全基于宽度判定换行
    rowNaturalWidth += (item.picScale || 1.1) * TARGET_HEIGHT.value

    const gaps = GAP.value * (currentRow.length - 1)
    const projectedWidth = rowNaturalWidth + gaps

    // 换行判定逻辑：
    // 1. 投影宽度达到 82% 可用宽时换行
    // 2. 移动端强制限制每行最多展示 3 张图 (彻底解决挤压)
    // 3. 最后一组数据处理
    if (projectedWidth >= availableWidth * 0.82 || (isMobile.value && currentRow.length >= MOBILE_MAX_ITEMS)) {
      rows.push(buildRow(currentRow, false))
      currentRow = []
      rowNaturalWidth = 0
    } else if (i === list.length - 1) {
      rows.push(buildRow(currentRow, true))
    }
  }
  return rows
})

const updateWidth = () => {
  if (containerRef.value) {
    containerWidth.value = Math.floor(containerRef.value.clientWidth)
  }
  windowWidth.value = window.innerWidth
}

const handleImageLoad = (id: string | number) => {
  imageLoadedMap[id] = true
}
const handleAvatarLoad = (id: string | number) => {
  avatarLoadedMap[id] = true
}

const doClickPicture = (picture: PictureVO) => {
  console.log('Clicked picture:', picture.id, 'isMobile:', isMobile.value)
  if (picture.isDraft === 1) {
    router.push({ path: '/add_picture', query: { id: picture.id } })
  } else {
    // PC端使用弹窗，移动端继续跳转
    if (!isMobile.value) {
      openDetailModal(picture)
    } else {
      router.push({
        path: `/picture-redirect/${picture.id}`,
        state: { pictureData: JSON.parse(JSON.stringify(picture)) }
      })
    }
  }
}

const openDetailModal = (picture: any) => {
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

const getStatusText = (status?: number) => {
  if (status === 0) return '审核中'
  if (status === 1) return '已通过'
  if (status === 2) return '未通过'
  return ''
}

const getReviewModalTitle = (status?: number) => {
  if (status === 0) return '内容正在审核'
  if (status === 1) return '审核已通过'
  if (status === 2) return '审核未通过'
  return '发布状态'
}

const formatNumber = (num?: number) => {
  if (!num) return '0'
  return num > 10000 ? (num / 10000).toFixed(1) + 'w' : num
}

let resizeObserver: ResizeObserver | null = null

onMounted(async () => {
  await nextTick()
  updateWidth()
  window.addEventListener('resize', throttle(updateWidth, 200))
  window.addEventListener('popstate', handlePopState)

  resizeObserver = new ResizeObserver(throttle(updateWidth, 100))
  if (containerRef.value) resizeObserver.observe(containerRef.value)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateWidth)
  window.removeEventListener('popstate', handlePopState)
  if (resizeObserver) resizeObserver.disconnect()
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

.not-found-state,
.empty-state {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  text-align: center;
}

.custom-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
}

.empty-illustration {
  width: 100%;
  max-width: 300px;
  height: auto;
  opacity: 0.85;
  filter: drop-shadow(0 15px 30px rgba(0, 0, 0, 0.05));
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.justified-feed {
  width: 100%;
  transition: opacity 0.3s ease;
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
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  background: var(--hover-background);
  flex-shrink: 0; // 防止 flex 压缩算法计算出的宽度
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);

  &:hover {
    transform: scale(1.02);
    z-index: 5;
    .info-overlay { opacity: 1; }

    @media (max-width: 768px) {
      transform: scale(1.005);
    }
  }
}

.j-image-box {
  width: 100%;
  height: 100%;
  position: relative;
}

.j-placeholder {
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, var(--hover-background) 8%, var(--border-color) 18%, var(--hover-background) 33%);
  background-size: 200% 100%;
  animation: shine 1.5s linear infinite;
}

@keyframes shine {
  to { background-position-x: -200%; }
}

.j-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.4s ease, transform 0.5s ease;

  &.is-loaded { opacity: 1; }
}

/* 叠加层样式 - 默认隐藏，悬浮显示 */
.info-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.6) 0%, transparent 40%, transparent 60%, rgba(0,0,0,0.3) 100%);
  opacity: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 8px; // 缩小 Padding
  pointer-events: none;
  transition: all 0.3s ease;
  z-index: 3;
  @media (min-width: 769px) {
    padding: 12px;
  }
}

.info-header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.view-item {
  color: #fff;
  font-size: 10px; // 缩小字号
  background: rgba(0,0,0,0.2);
  padding: 1px 6px; // 缩小间距
  border-radius: 10px;
  backdrop-filter: blur(4px);
}

.info-footer {
  transform: translateY(10px);
  transition: transform 0.3s ease;
  width: 100%;

  // 移动端取消位移防止卡顿或显示不全
  @media (max-width: 768px) {
    transform: none !important;
  }
}

.pic-name {
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 4px;
  text-shadow: 0 1px 3px rgba(0,0,0,0.5); // 加强阴影
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

.user-meta {
  display: flex;
  align-items: center;
  gap: 4px; // 缩小 Gap
  overflow: hidden;
}

.u-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1.5px solid rgba(255,255,255,0.8);
  opacity: 0;
  &.loaded { opacity: 1; }
}

.u-name {
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}
@media (max-width: 768px) {
  .justified-item {
    .info-footer {
      width: 100%;
    }

    // 自动适配逻辑
    .u-name {
      font-size: 10px;
      max-width: 65px;
    }

    .view-item {
      font-size: 9px;
      padding: 1px 4px;
    }
  }
}

.justified-item:hover {
  .info-overlay {
    opacity: 1;
  }
  .info-footer {
    transform: translateY(0);
  }
  .j-image {
    transform: scale(1.05);

    @media (max-width: 768px) {
      transform: scale(1.01);
    }
  }
}

/* 徽章与状态 */
.item-badges {
  display: flex;
  gap: 4px;
}

.badge-tag {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  backdrop-filter: blur(4px);

  &.feature { background: #3b82f6; }
  &.draft { background: #64748b; }
}

.review-status-dot-wrap {
  background: rgba(255,255,255,0.8);
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  pointer-events: auto;
  backdrop-filter: blur(4px);
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.1);
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    &.status-0 { background: #1890ff; }
    &.status-1 { background: #52c41a; }
    &.status-2 { background: #ff4d4f; }
  }
}


/* 骨架屏修正 */
.skeleton-wrap { display: flex; flex-direction: column; gap: 4px; padding: 0 2px; }
.skeleton-row { display: flex; gap: 4px; margin-bottom: 4px; }
.skeleton-cell {
  background: linear-gradient(90deg, var(--hover-background) 25%, var(--border-color) 37%, var(--hover-background) 63%);
  background-size: 400% 100%;
  border-radius: 8px;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

.review-detail-box {
  padding: 30px 20px;
  text-align: center;
}

.status-icon-box {
  font-size: 40px;
  margin-bottom: 16px;
  .fa-clock { color: #1890ff; }
  .fa-check-circle { color: #52c41a; }
  .fa-times-circle { color: #ff4d4f; }
}

.status-msg {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.6;
}
</style>
