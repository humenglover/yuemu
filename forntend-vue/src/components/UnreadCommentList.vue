<template>
  <div class="unread-comment-list">
    <template v-if="safeComments && safeComments.length > 0">
      <div v-for="(comment, i) in safeComments" :key="comment?.id ?? comment?.commentId ?? i" class="comment-item">
        <div class="comment-content-wrap">
          <a-avatar :src="safeUrl(comment.commentUser?.userAvatar) || 'src/assets/pictureempty.png'" :size="36" class="user-avatar" />
          <div class="comment-main">
            <div class="comment-header">
              <span class="user-name">{{ comment.commentUser?.userName || '未知用户' }}</span>
              <span class="comment-desc">{{ isReceived ? '评论了你的' : '你评论的' }}</span>
              <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
            </div>
            <div class="comment-content">{{ comment.content || '' }}</div>
            <div
              class="comment-target"
              :class="{ 'clickable': isTargetClickable(comment) }"
              @click="handleTargetClick(comment)"
            >
              <template v-if="comment.targetType === 1 && comment.picture">
                <div class="target-card picture-card">
                  <img
                    :src="comment.picture?.thumbnailUrl || 'src/assets/pictureempty.png'"
                    :alt="comment.picture?.name || '图片已失效'"
                    @error="handleImageError"
                    class="target-img"
                  >
                  <div class="target-info">
                    <div class="target-type">
                      <PictureOutlined class="type-icon" />
                      <span>图片</span>
                    </div>
                    <div class="target-title">{{ comment.picture?.name || '图片已失效' }}</div>
                    <div v-if="!isTargetClickable(comment)" class="target-status">
                      该图片已被删除或无法访问
                    </div>
                  </div>
                </div>
              </template>
              <template v-else-if="comment.targetType === 1">
                <div class="target-card picture-card">
                  <img :src="'src/assets/pictureempty.png'" alt="图片已失效" class="target-img" />
                  <div class="target-info">
                    <div class="target-type">
                      <PictureOutlined class="type-icon" />
                      <span>图片</span>
                    </div>
                    <div class="target-title">图片已失效</div>
                  </div>
                </div>
              </template>
              <template v-else-if="comment.targetType === 2 && comment.post">
                <div class="target-card post-card">
                  <div class="target-type">
                    <FileTextOutlined class="type-icon" />
                    <span>帖子</span>
                  </div>
                  <div class="target-title">{{ comment.post?.title || '帖子已失效' }}</div>
                  <div class="target-content">
                    {{ comment.post?.content ? stripHtml(comment.post.content) : '' }}
                  </div>
                  <div v-if="!isTargetClickable(comment)" class="target-status">
                    该帖子已被删除或无法访问
                  </div>
                </div>
              </template>
              <template v-else-if="comment.targetType === 2">
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
    </template>
    <div v-else class="empty-state">
      <CommentOutlined class="empty-icon" />
      <p>暂无未读评论</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { useRouter } from 'vue-router'
import { CommentOutlined, PictureOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { computed } from 'vue'

const router = useRouter()

const props = defineProps<{
  comments: any[]
  isReceived?: boolean
}>()

const safeComments = computed(() => (props.comments || []).filter((item) => !!item))

const formatTime = (time: string) => {
  try {
    return time ? formatDistanceToNow(new Date(time), { addSuffix: true, locale: zhCN }) : '-'
  } catch (error) {
    console.error('时间格式化错误:', error)
    return '-'
  }
}

const isTargetClickable = (comment: any) => {
  if (comment.targetType === 1) {
    return comment.picture && comment.picture.id
  } else if (comment.targetType === 2) {
    return comment.post && comment.post.id
  }
  return false
}

const defaultImage = 'src/assets/pictureempty.png'
const handleImageError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.src = defaultImage
  img.classList.add('error')
}

const handleTargetClick = (comment: any) => {
  try {
    if (!isTargetClickable(comment)) {
      return
    }

    if (comment.targetType === 1 && comment.picture?.id) {
      router.push(`/picture-redirect/${comment.picture.id}`)
    } else if (comment.targetType === 2 && comment.post?.id) {
      router.push(`/post/${comment.post.id}`)
    }
  } catch (error) {
    console.error('点击处理错误:', error)
  }
}

const stripHtml = (html: string) => {
  return html.replace(/<[^>]+>/g, '').slice(0, 50) + '...'
}

const safeUrl = (url?: string) => {
  if (!url) return ''
  return url.replace(/`/g, '').trim()
}
</script>

<style scoped>
.unread-comment-list {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 12px;
}

.comment-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s ease;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f8fafc;
    border-radius: 8px;
  }
}

.comment-content-wrap {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.user-avatar {
  flex-shrink: 0;
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
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

.comment-desc {
  color: #4299e1;
  font-size: 12px;
}

.comment-time {
  font-size: 11px;
  color: #999;
  margin-left: auto;
  white-space: nowrap;
}

.comment-content {
  font-size: 13px;
  color: #1a1a1a;
  line-height: 1.4;
  margin-bottom: 6px;
  padding: 0 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;
}

.comment-target {
  background: #f8fafc;
  border-radius: 8px;
  padding: 8px;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
  width: 100%;
  box-sizing: border-box;

  &.clickable {
    cursor: pointer;

    &:hover {
      background: #f1f5f9;
      border-color: #e2e8f0;
      transform: none;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
    }
  }
}

.target-card {
  display: flex;
  flex-direction: column;
  width: 100%;
}

.picture-card {
  flex-direction: row;
  align-items: center;
  gap: 8px;
}

.target-img {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;

  &.error {
    opacity: 0.5;
    filter: grayscale(100%);
  }
}

.target-type {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #4299e1;
  font-size: 11px;
  margin-bottom: 3px;
}

.type-icon {
  font-size: 12px;
}

.target-title {
  font-weight: 500;
  color: #1a1a1a;
  font-size: 12px;
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

.target-status {
  font-size: 10px;
  color: #999;
  margin-top: 3px;
  font-style: italic;
  padding: 2px 4px;
  background: #f8fafc;
  border-radius: 3px;
  border: 1px dashed #e2e8f0;
  width: fit-content;
}

.post-card {
  .target-info {
    background: transparent;
    padding: 0;
    border: none;
  }
}

.empty-state {
  text-align: center;
  padding: 32px 0;
  color: #666;
}

.empty-icon {
  font-size: 40px;
  color: #4299e1;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  font-size: 13px;
  color: #666;
  margin: 0;
}

@media screen and (max-width: 768px) {
  .comment-item {
    padding: 10px;
  }

  .comment-target {
    padding: 6px;
  }

  .picture-card .target-img {
    width: 45px;
    height: 45px;
  }

  .comment-content {
    -webkit-line-clamp: 2;
    word-break: break-word;
  }

  .target-content {
    -webkit-line-clamp: 2;
    word-break: break-word;
  }

  .target-title {
    -webkit-line-clamp: 2;
    word-break: break-word;
  }

  .comment-header {
    flex-wrap: wrap;
  }

  .comment-time {
    margin-left: 0;
    margin-top: 2px;
    width: 100%;
    text-align: right;
  }
}
</style>
