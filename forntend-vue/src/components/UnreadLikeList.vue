<template>
  <div class="unread-like-list">
    <div v-for="(like, i) in safeLikes" :key="like?.id ?? like?.likeId ?? i" class="like-item">
      <div class="like-content">
        <a-avatar :src="safeUrl(like.user?.userAvatar) || defaultImage" :size="36" class="user-avatar" />
        <div class="like-main">
          <div class="like-header">
            <span class="user-name">{{ (like.user && like.user.userName) ? like.user.userName : '未知用户' }}</span>
            <span class="like-desc">{{ isReceived ? '赞了你的' : '你赞过的' }}</span>
            <span class="like-time">{{ formatTime(like.lastLikeTime) }}</span>
          </div>
          <div class="like-target" @click="handleTargetClick(like)">
            <template v-if="like.targetType === 1 && like.target">
              <div class="target-card picture-card">
                <img :src="safeUrl(like.target?.thumbnailUrl) || defaultImage" :alt="(like.target && like.target.name) ? like.target.name : '图片已失效'" class="target-img" @error="onTargetImgError">
                <div class="target-info">
                  <div class="target-type">
                    <PictureOutlined class="type-icon" />
                    <span>图片</span>
                  </div>
                  <div class="target-title">{{ like.target?.name || '图片已失效' }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="like.targetType === 1">
              <div class="target-card picture-card">
                <img :src="defaultImage" alt="图片已失效" class="target-img">
                <div class="target-info">
                  <div class="target-type">
                    <PictureOutlined class="type-icon" />
                    <span>图片</span>
                  </div>
                  <div class="target-title">图片已失效</div>
                </div>
              </div>
            </template>
            <template v-else-if="like.targetType === 2 && like.target">
              <div class="target-card post-card">
                <div class="target-type">
                  <FileTextOutlined class="type-icon" />
                  <span>帖子</span>
                </div>
                <div class="target-title">{{ like.target?.title || '帖子已失效' }}</div>
                <div class="target-content">{{ like.target?.content ? stripHtml(like.target.content) : '' }}</div>
              </div>
            </template>
            <template v-else-if="like.targetType === 2">
              <div class="target-card post-card">
                <div class="target-type">
                  <FileTextOutlined class="type-icon" />
                  <span>帖子</span>
                </div>
                <div class="target-title">帖子已失效</div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
    <div v-if="safeLikes.length === 0" class="empty-state">
      <LikeOutlined class="empty-icon" />
      <p>暂无未读点赞</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { useRouter } from 'vue-router'
import { LikeOutlined, PictureOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import { computed } from 'vue'
import defaultImage from '@/assets/pictureempty.png'

const router = useRouter()

const props = defineProps<{
  likes: any[]
  isReceived?: boolean
}>()

const safeLikes = computed(() => (props.likes || []).filter((item) => !!item))

// 格式化时间
const formatTime = (time: string) => {
  try {
    return time ? formatDistanceToNow(new Date(time), { addSuffix: true, locale: zhCN }) : '-'
  } catch (error) {
    console.error('时间格式化错误:', error)
    return '-'
  }
}

// 处理目标点击
const handleTargetClick = (like: any) => {
  if (like.targetType === 1 && like.target?.id) {
    router.push(`/picture-redirect/${like.target.id}`)
  } else if (like.targetType === 2 && like.target?.id) {
    router.push(`/post/${like.target.id}`)
  }
}

// 去除 HTML 标签
const stripHtml = (html: string) => {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').slice(0, 60) + '...'
}

const safeUrl = (url?: string) => {
  if (!url) return ''
  return url.replace(/`/g, '').trim()
}

const onTargetImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = defaultImage
}
</script>

<style scoped>
.unread-like-list {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 12px;
  width: 100%;
  box-sizing: border-box;
}

.like-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;
  width: 100%;
  box-sizing: border-box;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
    border-radius: 8px;
  }
}

/* 核心：横向布局，压缩整体高度 */
.like-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
}

.user-avatar {
  flex-shrink: 0;
}

.like-main {
  flex: 1;
  min-width: 0;
}

/* 点赞者信息：一行紧凑排列，支持换行 */
.like-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  flex-wrap: wrap;
}

.user-name {
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.like-desc {
  color: #ff6b6b;
  font-size: 12px;
}

.like-time {
  font-size: 11px;
  color: #999;
  margin-left: auto;
  white-space: nowrap;
}

/* 点赞目标卡片：紧凑样式，宽度自适应 */
.like-target {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
  width: 100%;
  box-sizing: border-box;

  &:hover {
    background: #f1f5f9;
    border-color: #e2e8f0;
    transform: none;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
  }
}

.target-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  box-sizing: border-box;
}

/* 图片类型：左图右文，缩小图片 */
.picture-card {
  flex-direction: row;
  align-items: center;
  gap: 10px;
}

.target-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
}

/* 目标类型标签：紧凑样式 */
.target-type {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #ff6b6b;
  font-size: 11px;
  margin-bottom: 4px;
}

.type-icon {
  font-size: 12px;
}

/* 目标标题和内容：支持自动换行 */
.target-title {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 13px;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.target-content {
  font-size: 11px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

/* 空状态：保持简洁 */
.empty-state {
  text-align: center;
  padding: 32px 0;
  color: #666;
  width: 100%;
  box-sizing: border-box;
}

.empty-icon {
  font-size: 40px;
  color: #ff6b6b;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 13px;
  color: #666;
  margin: 0;
}

/* 移动端适配：进一步压缩并优化换行 */
@media screen and (max-width: 768px) {
  .like-item {
    padding: 10px;
  }

  .like-target {
    padding: 8px;
  }

  .picture-card .target-img {
    width: 50px;
    height: 50px;
  }

  .target-title {
    font-size: 12px;
    -webkit-line-clamp: 2;
  }

  .target-content {
    font-size: 10px;
    -webkit-line-clamp: 2;
  }

  .like-header {
    flex-wrap: wrap;
  }

  .like-time {
    margin-left: 0;
    margin-top: 2px;
    width: 100%;
    text-align: right;
  }
}
</style>
