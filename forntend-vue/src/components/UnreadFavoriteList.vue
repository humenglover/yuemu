<template>
  <div class="unread-favorite-list">
    <div v-for="(favorite, i) in safeFavorites" :key="favorite?.id ?? i" class="favorite-item">
      <!-- 核心：横向布局（头像+主体内容） -->
      <div class="favorite-content">
        <!-- 左侧：头像（固定尺寸，不缩放） -->
        <img :src="safeUrl(favorite.user?.userAvatar) || defaultImage" :alt="favorite.user?.userName" class="user-avatar" />

        <!-- 右侧：所有信息紧凑排列 -->
        <div class="favorite-main">
          <!-- 收藏者信息（一行展示） -->
          <div class="favorite-header">
            <span class="user-name">{{ (favorite.user && favorite.user.userName) ? favorite.user.userName : '未知用户' }}</span>
            <span class="favorite-desc">{{ isReceived ? '收藏了你的' : '你收藏的' }}</span>
            <span class="favorite-time">{{ formatTime(favorite.lastFavoriteTime) }}</span>
          </div>

          <!-- 收藏目标（紧凑卡片） -->
          <div class="favorite-target" @click="handleTargetClick(favorite)">
            <template v-if="favorite.targetType === 1 && favorite.target">
              <!-- 图片类型：左图右文 -->
              <div class="target-card picture-card">
                <img :src="safeUrl(favorite.target?.thumbnailUrl) || defaultImage" :alt="(favorite.target && favorite.target.name) ? favorite.target.name : '图片已失效'" class="target-img" @error="onTargetImgError">
                <div class="target-info">
                  <div class="target-type">
                    <i class="far fa-image type-icon"></i>
                    <span>图片</span>
                  </div>
                  <div class="target-title">{{ favorite.target?.name || '图片已失效' }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="favorite.targetType === 1">
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
            <template v-else-if="favorite.targetType === 2 && favorite.target">
              <!-- 帖子类型：纯文字紧凑展示 -->
              <div class="target-card post-card">
                <div class="target-type">
                  <i class="far fa-file-alt type-icon"></i>
                  <span>帖子</span>
                </div>
                <div class="target-title">{{ favorite.target?.title || '帖子已失效' }}</div>
                <div class="target-content">{{ favorite.target?.content ? stripHtml(favorite.target.content) : '' }}</div>
              </div>
            </template>
            <template v-else-if="favorite.targetType === 2">
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
    <div v-if="safeFavorites.length === 0" class="empty-state">
      <i class="far fa-heart empty-icon"></i>
      <p>{{ isReceived ? '暂无收到的收藏' : '暂无收藏记录' }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { computed } from 'vue'
import defaultImage from '@/assets/default.png'

const router = useRouter()

interface FavoriteRecord {
  id: string | number;
  lastFavoriteTime: string;
  user: {
    id: string | number;
    userName: string;
    userAvatar?: string;
  };
  targetType: number; // 1-图片 2-帖子 3-空间
  targetUserId: string | number;
  target: any;
  isRead: number;
}

const props = withDefaults(defineProps<{
  favorites?: FavoriteRecord[]
  isReceived?: boolean
}>(), {
  favorites: () => [],
  isReceived: false
})

const safeFavorites = computed<FavoriteRecord[]>(() => (props.favorites || []).filter((item) => !!item))

// 格式化时间（增加异常处理）
const formatTime = (timeString: string) => {
  if (!timeString) return '';

  const date = new Date(timeString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) {
    return '刚刚';
  } else if (diffInSeconds < 3600) {
    return `${Math.floor(diffInSeconds / 60)}分钟前`;
  } else if (diffInSeconds < 86400) {
    return `${Math.floor(diffInSeconds / 3600)}小时前`;
  } else {
    return date.toLocaleDateString('zh-CN');
  }
}

// 处理目标点击
const handleTargetClick = (favorite: any) => {
  if (favorite.targetType === 1 && favorite.target?.id) {
    router.push(`/picture-redirect/${favorite.target.id}`)
  } else if (favorite.targetType === 2 && favorite.target?.id) {
    router.push(`/post/${favorite.target.id}`)
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
.unread-favorite-list {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 12px;
  padding: 12px;
  width: 100%;
  box-sizing: border-box; /* 确保内边距不影响宽度 */
}

.favorite-item {
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
.favorite-content {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  box-sizing: border-box;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0; /* 头像不缩放，保持紧凑 */
}

.favorite-main {
  flex: 1;
  min-width: 0; /* 解决文字溢出问题 */
}

/* 收藏者信息：一行紧凑排列，支持换行 */
.favorite-header {
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

.favorite-desc {
  color: #60c3d5;
  font-size: 12px;
}

.favorite-time {
  font-size: 11px;
  color: #999;
  margin-left: auto;
  white-space: nowrap;
}

/* 收藏目标卡片：紧凑样式，宽度自适应 */
.favorite-target {
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
  .favorite-item {
    padding: 10px;
  }

  .favorite-target {
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
  .favorite-header {
    flex-wrap: wrap;
  }

  .favorite-time {
    margin-left: 0;
    margin-top: 2px;
    width: 100%;
    text-align: right;
  }
}
</style>
