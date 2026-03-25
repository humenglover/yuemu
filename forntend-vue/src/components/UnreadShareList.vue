<template>
  <div class="unread-share-list">
    <div v-for="(share, i) in safeShares" :key="share?.id ?? share?.shareId ?? i" class="share-item">
      <!-- 核心：横向布局（头像+主体内容） -->
      <div class="share-content">
        <!-- 左侧：头像（固定尺寸，不缩放） -->
        <a-avatar :src="safeUrl(share.user?.userAvatar) || defaultImage" :size="36" class="user-avatar" />

        <!-- 右侧：所有信息紧凑排列 -->
        <div class="share-main">
          <!-- 分享者信息（一行展示） -->
          <div class="share-header">
            <span class="user-name">{{ (share.user && share.user.userName) ? share.user.userName : '未知用户' }}</span>
            <span class="share-desc">{{ isReceived ? '分享了你的' : '你分享的' }}</span>
            <span class="share-time">{{ formatTime(share.shareTime) }}</span>
          </div>

          <!-- 分享目标（紧凑卡片） -->
          <div class="share-target" @click="handleTargetClick(share)">
            <template v-if="share.targetType === 1 && share.target">
              <!-- 图片类型：左图右文 -->
              <div class="target-card picture-card">
                <img :src="safeUrl(share.target?.thumbnailUrl) || defaultImage" :alt="(share.target && share.target.name) ? share.target.name : '图片已失效'" class="target-img" @error="onTargetImgError">
                <div class="target-info">
                  <div class="target-type">
                    <i class="far fa-image type-icon"></i>
                    <span>图片</span>
                  </div>
                  <div class="target-title">{{ share.target?.name || '图片已失效' }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="share.targetType === 1">
              <div class="target-card picture-card">
                <img :src="defaultImage" alt="图片已失效" class="target-img">
                <div class="target-info">
                  <div class="target-type">
                    <i class="far fa-image type-icon"></i>
                    <span>图片</span>
                  </div>
                  <div class="target-title">图片已失效</div>
                </div>
              </div>
            </template>
            <template v-else-if="share.targetType === 2 && share.target">
              <!-- 帖子类型：纯文字紧凑展示 -->
              <div class="target-card post-card">
                <div class="target-type">
                  <i class="far fa-file-alt type-icon"></i>
                  <span>帖子</span>
                </div>
                <div class="target-title">{{ share.target?.title || '帖子已失效' }}</div>
                <div class="target-content">{{ share.target?.content ? stripHtml(share.target.content) : '' }}</div>
              </div>
            </template>
            <template v-else-if="share.targetType === 2">
              <div class="target-card post-card">
                <div class="target-type">
                  <i class="far fa-file-alt type-icon"></i>
                  <span>帖子</span>
                </div>
                <div class="target-title">帖子已失效</div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态（精简尺寸） -->
    <div v-if="safeShares.length === 0" class="empty-state">
      <i class="far fa-share-alt empty-icon"></i>
      <p>暂无未读分享</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { formatDistanceToNow } from 'date-fns'
import { zhCN } from 'date-fns/locale'
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import defaultImage from '@/assets/default.png'

// 移除 Ant Design 图标导入
// import { ShareAltOutlined, PictureOutlined, FileTextOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const props = withDefaults(defineProps<{
  shares?: any[]
  isReceived?: boolean
}>(), {
  shares: () => [],
  isReceived: false
})

const safeShares = computed(() => (props.shares || []).filter((item) => !!item))

// 格式化时间（增加异常处理）
const formatTime = (time: string) => {
  try {
    return time ? formatDistanceToNow(new Date(time), { addSuffix: true, locale: zhCN }) : '-'
  } catch (error) {
    console.error('时间格式化错误:', error)
    return '-'
  }
}

// 处理目标点击
const handleTargetClick = (share: any) => {
  if (share.targetType === 1 && share.target?.id) {
    router.push(`/picture-redirect/${share.target.id}`)
  } else if (share.targetType === 2 && share.target?.id) {
    router.push(`/post/${share.target.id}`)
  }
}

// 去除 HTML 标签（缩减截取长度）
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
.unread-share-list {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 12px;
  width: 100%;
  box-sizing: border-box; /* 确保内边距不影响宽度 */
}

.share-item {
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

/* 核心：横向布局，压缩垂直高度 */
.share-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
}

.user-avatar {
  flex-shrink: 0; /* 头像不缩放，保持紧凑 */
}

.share-main {
  flex: 1;
  min-width: 0; /* 解决文字溢出问题 */
}

/* 分享者信息：一行紧凑排列，支持换行 */
.share-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 13px;
  flex-wrap: wrap; /* 空间不足时自动换行 */
}

.user-name {
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-desc {
  color: #60c3d5;
  font-size: 12px;
}

.share-time {
  font-size: 11px;
  color: #999;
  margin-left: auto;
  white-space: nowrap;
}

/* 分享目标卡片：紧凑样式，宽度自适应 */
.share-target {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  border: 1px solid #f0f0f0;
  margin-left: 0;
  width: 100%;
  box-sizing: border-box; /* 确保内边距不影响宽度 */

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

/* 图片类型：左图右文，缩小图片尺寸 */
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
  color: #60c3d5;
  font-size: 11px;
  margin-bottom: 4px;
}

/* 适配 FontAwesome 图标样式 */
.type-icon {
  font-size: 12px;
  /* 非实心图标增强视觉效果 */
  text-shadow: 0 0 1px currentColor;
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
  -webkit-line-clamp: 2; /* 最多显示2行 */
  -webkit-box-orient: vertical;
  word-break: break-word; /* 强制换行 */
}

.target-content {
  font-size: 11px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2; /* 最多显示2行 */
  -webkit-box-orient: vertical;
  word-break: break-word; /* 强制换行 */
}

/* 空状态：精简尺寸 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 0;
  color: #94a3b8;
  border-radius: 12px;
  width: 100%;
  box-sizing: border-box;
}

/* 空状态图标样式适配 */
.empty-icon {
  font-size: 40px;
  color: #60c3d5;
  margin-bottom: 12px;
  opacity: 0.5;
  transition: all 0.3s ease;

  &:hover {
    opacity: 0.8;
    transform: scale(1.1);
  }
}

.empty-state p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

/* 移动端适配：进一步压缩并优化换行 */
@media screen and (max-width: 768px) {
  .share-item {
    padding: 10px;
  }

  .share-target {
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

  /* 移动端头部自适应换行 */
  .share-header {
    flex-wrap: wrap;
  }

  .share-time {
    margin-left: 0;
    margin-top: 2px;
    width: 100%;
    text-align: right;
  }
}
</style>
