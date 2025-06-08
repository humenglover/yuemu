<template>
  <div class="post-list">
    <div class="masonry-wrapper">
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
          <h3>暂无帖子</h3>
          <p>快来发布一些有趣的内容吧 (｡•́︿•̀｡)</p>
        </div>
      </div>
      <div v-else class="masonry-grid">
        <!-- 使用计算后的列数据进行渲染 -->
        <div v-for="(column, columnIndex) in columns" :key="columnIndex" class="masonry-column">
          <div
            v-for="post in column"
            :key="post.id"
            class="masonry-item"
            @click="handlePostClick(post)"
          >
            <!-- 添加状态标签 -->
            <div v-if="props.showStatus" class="post-status" @click.stop="handleStatusClick(post)">
              <a-tag v-if="post.status === 0" color="orange">待审核</a-tag>
              <a-tag v-else-if="post.status === 2" color="red" class="reject-tag">已拒绝</a-tag>
            </div>

            <!-- 封面图片 -->
            <div class="image-wrapper">
              <div class="aspect-ratio-box">
                <img
                  :src="getPostCoverImage(post)"
                  :alt="post.title"
                  class="masonry-image"
                  @load="handleImageLoad"
                  @error="handleImageError"
                />
              </div>
            </div>

            <!-- 帖子信息 -->
            <div class="post-info">
              <div class="post-header">
                <div class="post-title">{{ post.title }}</div>
                <div class="category-tag" v-if="post.category">{{ post.category }}</div>
              </div>
              <div class="post-meta">
                <div class="author-info">
                  <div class="author-avatar">
                    <img :src="post.user?.userAvatar || getDefaultAvatar(post.user?.userName)" :alt="post.user?.userName">
                  </div>
                  <span class="author-name">{{ post.user?.userName }}</span>
                </div>
                <div class="post-time">{{ formatTime(post.createTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加拒绝原因弹框 -->
    <a-modal
      v-model:open="showRejectModal"
      :title="null"
      :footer="null"
      :closable="false"
      class="reject-modal"
      width="320px"
    >
      <div class="reject-content">
        <CloseCircleOutlined class="reject-icon" />
        <h3 class="reject-title">审核未通过</h3>
        <p class="reject-message">{{ currentPost?.reviewMessage || '内容不符合规范' }}</p>
        <a-button type="primary" @click="showRejectModal = false">
          我知道了
        </a-button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { LikeOutlined, EyeOutlined, CommentOutlined, FileTextOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { likePostUsingPost } from '@/api/postController'
import { getPostCoverImage, getRandomCoverImage } from '@/utils/imageUtils'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { throttle } from 'lodash'

const router = useRouter()

interface Props {
  dataList?: API.Post[]
  loading?: boolean
  showStatus?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  dataList: () => [],
  loading: false,
  showStatus: false
})

const emit = defineEmits(['update:dataList'])

// 格式化数字
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

// 点击帖子
const handlePostClick = (post: API.Post) => {
  if (!post?.id) {
    message.error('无效的帖子')
    return
  }
  router.push({
    name: 'postDetail',
    params: { id: post.id }
  })
}

// 点赞
const handleLike = async (post: API.Post) => {
  try {
    await likePostUsingPost(post.id)
    post.isLiked = post.isLiked === 1 ? 0 : 1
    post.likeCount = (post.likeCount || 0) + (post.isLiked ? 1 : -1)
  } catch (error: any) {
    message.error('点赞失败：' + error.message)
  }
}

// 处理图片加载
const handleImageLoad = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.style.opacity = '1'
}

// 处理图片加载失败
const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = getRandomCoverImage() // 加载失败时使用另一张随机图片
}

// 计算列数
const getColumnCount = () => {
  const width = window.innerWidth
  if (width > 1920) return 6
  if (width > 1600) return 5
  if (width > 1400) return 4
  if (width > 1200) return 3
  if (width > 900) return 2
  return 1
}

// 计算分列数据
const columns = computed(() => {
  const columnCount = getColumnCount()
  const cols: API.Post[][] = Array.from({ length: columnCount }, () => [])

  // 按照从左到右、从上到下的顺序分配数据
  props.dataList.forEach((item, index) => {
    const columnIndex = index % columnCount
    cols[columnIndex].push(item)
  })

  return cols
})

// 监听窗口大小变化
onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const handleResize = throttle(() => {
  // 触发重新计算列
  columns.value = computed(() => {
    const columnCount = getColumnCount()
    const cols: API.Post[][] = Array.from({ length: columnCount }, () => [])

    props.dataList.forEach((item, index) => {
      const columnIndex = index % columnCount
      cols[columnIndex].push(item)
    })

    return cols
  }).value
}, 200)

// 添加状态相关变量
const showRejectModal = ref(false)
const currentPost = ref<API.Post | null>(null)

// 添加状态点击处理函数
const handleStatusClick = (post: API.Post) => {
  if (post.status === 2) { // 2 表示已拒绝
    currentPost.value = post
    showRejectModal.value = true
  }
}
</script>

<style scoped>
.post-list {
  width: 100%;
  padding: 16px;
  background: #f8f9fa;
}

.masonry-wrapper {
  width: 100%;
  max-width: 1800px;
  margin: 0 auto;
}

.masonry-grid {
  display: flex;
  gap: 20px;
  width: 100%;
}

.masonry-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.masonry-item {
  position: relative;
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.masonry-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.image-wrapper {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.aspect-ratio-box {
  position: relative;
  width: 100%;
  padding-top: 75%;
  background: #f3f4f6;
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

.masonry-item:hover .masonry-image {
  transform: scale(1.05);
}

.post-info {
  padding: 16px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,1) 100%);
}

.post-header {
  margin-bottom: 12px;
}

.post-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.category-tag {
  display: inline-block;
  padding: 4px 12px;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 12px;
  border-radius: 20px;
  font-weight: 500;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.author-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.author-name {
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
}

.post-time {
  font-size: 12px;
  color: #9ca3af;
}

.post-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px;
  border-radius: 8px;
  color: #6b7280;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f9fafb;
}

.action-button:hover {
  background: #f3f4f6;
  color: #4b5563;
  transform: translateY(-1px);
}

.action-button.active {
  color: #ef4444;
  background: #fef2f2;
}

.action-button.active:hover {
  background: #fee2e2;
}

.action-button i {
  font-size: 14px;
}

/* 响应式设计 */
@media screen and (max-width: 768px) {

  .masonry-grid {
    gap: 12px;
  }

  .masonry-column {
    gap: 12px;
  }

  .post-info {
    padding: 12px;
  }

  .post-title {
    font-size: 14px;
  }

  .post-actions {
    gap: 4px;
  }

  .action-button {
    padding: 6px;
    font-size: 12px;
  }
}

/* 加载动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.masonry-item {
  animation: fadeIn 0.5s ease-out forwards;
}

/* 添加状态标签样式 */
.post-status {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
}

.reject-tag {
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 6px rgba(255, 77, 79, 0.2);
  }
}

/* 添加拒绝原因弹框样式 */
.reject-modal {
  :deep(.ant-modal-content) {
    border-radius: 16px;
    overflow: hidden;
    background: white;
  }
}

.reject-content {
  padding: 24px;
  text-align: center;
}

.reject-icon {
  font-size: 48px;
  color: #ff4d4f;
  margin-bottom: 16px;
}

.reject-title {
  font-size: 18px;
  font-weight: 500;
  color: #1a1a1a;
  margin-bottom: 12px;
}

.reject-message {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
  padding: 0 16px;
  line-height: 1.6;
}

.reject-content .ant-btn {
  min-width: 120px;
  height: 36px;
  border-radius: 18px;
  font-weight: 500;
  background: #ff4d4f;
  border-color: #ff4d4f;

  &:hover {
    background: #ff7875;
    border-color: #ff7875;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 77, 79, 0.2);
  }

  &:active {
    background: #f5222d;
    border-color: #f5222d;
    transform: translateY(0);
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
