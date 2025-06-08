<template>
  <div class="grid-picture-list">
    <!-- PC端瀑布流布局 -->
    <div class="masonry-wrapper">
      <div class="masonry-grid">
        <!-- 使用计算后的列数据进行渲染 -->
        <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column">
          <div
            v-for="picture in column"
            :key="`${picture.id}-${picture.updateTime}`"
            class="masonry-item"
            :class="{ 'long-pressed': isItemLongPressed(picture.id) }"
            @click="handleClick(picture)"
            @touchstart="handleTouchStart(picture.id)"
            @touchend="handleTouchEnd(picture)"
            @touchmove="handleTouchMove"
            :style="{
              backgroundColor: `#${picture.picColor?.substring(2)}`,
              '--hover-color': `#${picture.picColor?.substring(2)}`
            }"
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
              </div>
            </div>
            <!-- 操作层 -->
            <div class="action-layer">
              <div class="action-top">
                <span class="picture-name">{{ picture.name }}</span>
              </div>
              <div class="action-bottom">
                <div class="action-buttons" v-if="showOp">
                  <a-button
                    v-if="canEdit"
                    type="link"
                    class="action-btn edit"
                    @click.stop="doEdit(picture)"
                  >
                    <EditOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    class="action-btn search"
                    @click.stop="doSearch(picture)"
                  >
                    <SearchOutlined />
                  </a-button>
                  <a-button
                    v-if="canDelete"
                    type="link"
                    class="action-btn delete"
                    @click.stop="doDelete(picture)"
                  >
                    <DeleteOutlined />
                  </a-button>
                </div>
                <div class="stats">
                  <span class="stat-item">
                    <EyeOutlined />
                    {{ formatNumber(picture.viewCount) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-wrapper">
        <a-spin>
          <template #indicator>
            <LoadingOutlined style="font-size: 24px; color: #ff8e53;" spin />
          </template>
        </a-spin>
      </div>
      <!-- 没有更多数据 -->
      <div v-if="!hasMore && !loading" class="no-more-data">
        <span>没有更多了</span>
      </div>
    </div>

    <!-- 删除确认对话框 -->
    <a-modal
      v-model:open="deleteModalVisible"
      title="确认删除"
      :footer="null"
      @cancel="cancelDelete"
    >
      <div class="delete-modal-content">
        <p>确定要删除这张图片吗？此操作不可恢复。</p>
        <div class="delete-modal-buttons">
          <a-button @click="cancelDelete">取消</a-button>
          <a-button type="primary" danger @click="confirmDelete" :loading="deleteLoading">
            删除
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  EyeOutlined,
  HeartOutlined,
  CrownOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost } from '@/api/pictureController'
import { throttle } from 'lodash-es'

interface Props {
  dataList: API.PictureVO[]
  loading?: boolean
  showOp?: boolean
  canEdit?: boolean
  canDelete?: boolean
  onReload?: () => void
  hasMore?: boolean
  onLoadMore?: () => void
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showOp: false,
  canEdit: false,
  canDelete: false,
  onReload: () => {},
  hasMore: true,
  onLoadMore: () => {}
})

const router = useRouter()

// 计算列数
const getColumnCount = () => {
  const width = window.innerWidth
  if (width > 1920) return 6
  if (width > 1600) return 5
  if (width > 1200) return 4
  if (width > 768) return 3
  return 2
}

// 计算分列数据
const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: API.PictureVO[][] = Array.from({ length: columnCount }, () => [])

  // 按照从左到右、从上到下的顺序分配数据
  props.dataList.forEach((item, index) => {
    const columnIndex = index % columnCount
    cols[columnIndex].push(item)
  })

  return cols
})

// 处理图片加载
const handleImageLoad = (event: Event) => {
  const img = event.target as HTMLImageElement
  if (img) {
    img.style.opacity = '1'
  }
}

// 处理图片加载错误
const handleImageError = (picture: API.PictureVO) => {
  const imgElement = event.target as HTMLImageElement
  if (imgElement && picture.url) {
    imgElement.src = picture.url
  }
}

// 格式化数字
const formatNumber = (num: number) => {
  if (!num) return '0'
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'k'
  }
  return num.toString()
}

// 点击图片
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}

// 编辑图片
const doEdit = (picture: API.PictureVO) => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.id,
      spaceId: picture.spaceId,
    },
  })
}

// 搜索相似图片
const doSearch = (picture: API.PictureVO) => {
  router.push({
    path: `/search_picture`,
    query: {
      pictureId: picture.id,
    },
  })
}

// 删除相关状态
const deleteModalVisible = ref(false)
const deleteLoading = ref(false)
const pictureToDelete = ref<API.PictureVO | null>(null)

// 删除图片
const doDelete = (picture: API.PictureVO) => {
  pictureToDelete.value = picture
  deleteModalVisible.value = true
}

// 取消删除
const cancelDelete = () => {
  deleteModalVisible.value = false
  pictureToDelete.value = null
}

// 确认删除
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

// 滚动加载相关
const SCROLL_THRESHOLD = 200 // 距离底部多少像素时触发加载
const THROTTLE_TIME = 200 // 节流时间间隔

// 处理滚动事件
const handleScroll = throttle(() => {
  if (props.loading || !props.hasMore || !props.onLoadMore) return

  const scrollHeight = document.documentElement.scrollHeight
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop
  const clientHeight = document.documentElement.clientHeight

  if (scrollHeight - scrollTop - clientHeight < SCROLL_THRESHOLD) {
    props.onLoadMore()
  }
}, THROTTLE_TIME)

// 监听窗口大小变化
const handleResize = throttle(() => {
  // 触发重新计算列
  columns.value = computed(() => {
    const columnCount = getColumnCount()
    const cols: API.PictureVO[][] = Array.from({ length: columnCount }, () => [])

    props.dataList.forEach((item, index) => {
      const columnIndex = index % columnCount
      cols[columnIndex].push(item)
    })

    return cols
  }).value
}, 200)

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
})

// 监听数据变化，重置状态
watch(() => props.dataList, () => {
  // 数据变化时重新计算列布局
  handleResize()
})

// 长按相关状态
const touchTimer = ref<number | null>(null)
const isTouchMoved = ref(false)
const longPressedItems = ref<Set<string>>(new Set())
const LONG_PRESS_DURATION = 500 // 长按触发时间（毫秒）

// 检查是否长按
const isItemLongPressed = (id: string) => {
  return longPressedItems.value.has(id)
}

// 处理触摸开始
const handleTouchStart = (id: string) => {
  isTouchMoved.value = false
  touchTimer.value = window.setTimeout(() => {
    if (!isTouchMoved.value) {
      longPressedItems.value.add(id)
    }
  }, LONG_PRESS_DURATION)
}

// 处理触摸移动
const handleTouchMove = () => {
  isTouchMoved.value = true
  if (touchTimer.value) {
    clearTimeout(touchTimer.value)
    touchTimer.value = null
  }
}

// 处理触摸结束
const handleTouchEnd = (picture: API.PictureVO) => {
  if (touchTimer.value) {
    clearTimeout(touchTimer.value)
    touchTimer.value = null
  }
  // 如果不是长按状态，延迟移除长按标记，以确保点击事件能正确判断
  setTimeout(() => {
    longPressedItems.value.delete(picture.id)
  }, 50)
}

// 处理点击事件
const handleClick = (picture: API.PictureVO) => {
  // 如果是长按状态或触摸移动，不触发点击事件
  if (isItemLongPressed(picture.id) || isTouchMoved.value) {
    return
  }
  doClickPicture(picture)
}
</script>

<style scoped>
.grid-picture-list {
  width: 100%;
  padding: 8px;
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
  background: var(--hover-color);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.masonry-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
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
  opacity: 0;
  transition: all 0.3s ease-in-out;
  will-change: opacity, transform;
}

.action-layer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background: linear-gradient(
    to top,
    rgba(0, 0, 0, 0.8) 0%,
    rgba(0, 0, 0, 0.4) 60%,
    transparent 100%
  );
  color: white;
  opacity: 0;
  visibility: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(8px);
  transform: translateY(10px);
}

.masonry-item:hover .action-layer {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.action-top {
  margin-bottom: 12px;
}

.picture-name {
  font-size: 14px;
  font-weight: 500;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 0.5px;
}

.action-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 8px;
  opacity: 0;
  transform: translateY(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) 0.1s;
}

.masonry-item:hover .action-buttons {
  opacity: 1;
  transform: translateY(0);
}

.action-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
  border: none;
  color: white;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 18px;
}

.action-btn:hover {
  transform: scale(1.1);
  background: rgba(255, 255, 255, 0.25);
}

.action-btn.edit:hover {
  color: #ff8e53;
  background: rgba(255, 142, 83, 0.2);
}

.action-btn.search:hover {
  color: #45b090;
  background: rgba(69, 176, 144, 0.2);
}

.action-btn.delete:hover {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.2);
}

.stats {
  display: flex;
  gap: 12px;
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) 0.2s;
}

.masonry-item:hover .stats {
  opacity: 1;
  transform: translateX(0);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.feature-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: linear-gradient(135deg, #ffd700 0%, #ffaa00 100%);
  padding: 6px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  opacity: 0;
  transform: translateY(-10px);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.masonry-item:hover .feature-badge {
  opacity: 1;
  transform: translateY(0);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .grid-picture-list {
    padding: 4px;
  }

  .masonry-grid {
    gap: 8px;
  }

  .masonry-column {
    gap: 8px;
  }

  .action-layer {
    opacity: 0;
    visibility: visible;
    transform: translateY(0);
    padding: 12px;
  }

  .action-buttons,
  .stats {
    opacity: 1;
    transform: none;
  }

  .action-btn {
    width: 32px;
    height: 32px;
    font-size: 16px;
  }

  .feature-badge {
    opacity: 1;
    transform: none;
    top: 8px;
    left: 8px;
    padding: 4px 8px;
  }
}

/* 触摸设备的长按效果 */
@media (hover: none) {
  .action-layer {
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
  }

  .masonry-item.long-pressed .action-layer {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
  }

  .action-buttons,
  .stats {
    opacity: 0;
    transform: translateY(10px);
  }

  .masonry-item.long-pressed .action-buttons,
  .masonry-item.long-pressed .stats {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 禁用长按时的系统默认菜单 */
.masonry-item {
  -webkit-touch-callout: none;
  -webkit-user-select: none;
  user-select: none;
  touch-action: manipulation;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 0;
  width: 100%;
}

.no-more-data {
  text-align: center;
  padding: 24px 0;
  color: #999;
  font-size: 14px;
}

/* 删除对话框样式 */
.delete-modal-content {
  text-align: center;
  padding: 24px 16px;
}

.delete-modal-buttons {
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
