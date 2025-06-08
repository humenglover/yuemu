<template>
  <div id="activityDetailPage" v-show="mounted" :class="{ mounted }">
    <!-- 背景 -->
    <div class="page-background"></div>

    <!-- 主要内容区域 -->
    <div class="content-container">
      <div class="preview-card">
        <!-- 封面图容器 -->
        <div class="image-container">
          <img :src="activity.coverUrl" :alt="activity.title" class="main-image" />
          <div class="image-overlay"></div>
        </div>

        <!-- 活动信息区域 -->
        <div class="info-section">
          <!-- 标题和作者信息 -->
          <div class="header-section">
            <h1 class="title">{{ activity.title }}</h1>
            <div class="author-info">
              <div class="author-avatar">
                <img :src="activity.user?.userAvatar || getDefaultAvatar(activity.user?.userName)" :alt="activity.user?.userName" />
              </div>
              <div class="author-details">
                <span class="author-name">{{ activity.user?.userName }}</span>
                <span class="publish-time">{{ formatTime(activity.createTime, 'date') }}</span>
              </div>
              <div class="view-count">
                <i class="icon-eye"></i>
                <span>{{ activity.viewCount || 0 }}</span>
              </div>
              <div class="share-btn" @click="handleShare">
                <ShareAltOutlined class="share-icon" />
                <span class="share-text">分享</span>
              </div>
            </div>
          </div>

          <!-- 活动状态 -->
          <div class="status-section">
            <div :class="['status-tag', activity.isExpired === 1 ? 'expired' : 'ongoing']">
              {{ activity.isExpired === 1 ? '已结束' : `进行中 · ${formatTime(activity.expireTime, 'full')}截止` }}
            </div>
          </div>

          <!-- 活动内容 -->
          <div class="content-section">
            <markdown-content :content="activity.content" class="markdown-body" />
          </div>
        </div>
      </div>
    </div>

    <!-- 分享模态框 -->
    <ShareModal ref="shareModalRef" :link="shareLink" :imageUrl="shareImage" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ShareAltOutlined } from '@ant-design/icons-vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import ShareModal from '@/components/ShareModal.vue'
import { getActivityByIdUsingGet } from '@/api/activityController'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { doShareUsingPost } from '@/api/shareRecordController'

const route = useRoute()
const router = useRouter()
const mounted = ref(false)
const activity = ref<API.Activity>({} as API.Activity)

// 分享相关
const shareModalRef = ref()
const shareLink = ref('')
const shareImage = ref('')

// 获取活动详情
const getActivityDetail = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('活动ID不能为空')
    return router.back()
  }

  try {
    const res = await getActivityByIdUsingGet({ id })
    if (res.data?.data) {
      activity.value = res.data.data
      mounted.value = true
    }
  } catch (error) {
    message.error('获取活动详情失败')
    router.back()
  }
}

// 处理分享
const handleShare = async () => {
  if (!activity.value?.id) {
    message.error('活动信息获取失败')
    return
  }

  // 设置分享链接和图片
  shareLink.value = window.location.href
  shareImage.value = activity.value.coverUrl || ''
  shareModalRef.value?.openModal()

  // 调用分享接口
  try {
    const requestBody = {
      targetId: activity.value.id,
      targetType: 2, // 2表示活动类型
      isShared: true
    }
    const res = await doShareUsingPost(requestBody)
    if (res.data.code === 0) {
      activity.value.shareCount = (activity.value.shareCount || 0) + 1
    }
  } catch (error) {
    console.error('分享失败:', error)
  }
}

onMounted(() => {
  getActivityDetail()
})
</script>

<style scoped lang="scss">
#activityDetailPage {
  min-height: 88vh;
  margin: -28px;
  margin-top: -38px;
  margin-bottom: -45px;
  position: relative;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%);
  color: #fff;

  &.mounted {
    opacity: 1;
    transform: translateY(0);
  }
}

.page-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 20% 20%, rgba(103, 232, 249, 0.1) 0%, transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(236, 72, 153, 0.1) 0%, transparent 40%);
  z-index: 0;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%239C92AC' fill-opacity='0.05'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E") repeat;
    opacity: 0.5;
    animation: backgroundMove 30s linear infinite;
  }
}

.content-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
  position: relative;
  z-index: 1;
}

.preview-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.2),
    0 1px 2px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-4px);
    box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.3),
      0 2px 4px rgba(0, 0, 0, 0.2);
    border-color: rgba(255, 255, 255, 0.2);
  }
}

.image-container {
  position: relative;
  width: 100%;
  height: 500px;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(to bottom,
      transparent 0%,
      rgba(26, 26, 46, 0.4) 50%,
      rgba(26, 26, 46, 0.95) 100%
    );
    pointer-events: none;
  }

  .main-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 1.2s cubic-bezier(0.16, 1, 0.3, 1);
    filter: brightness(1.1) saturate(1.2);

    &:hover {
      transform: scale(1.05);
    }
  }
}

.info-section {
  position: relative;
  margin-top: -100px;
  padding: 40px;
  z-index: 2;
}

.header-section {
  margin-bottom: 24px;

  .title {
    font-size: 32px;
    font-weight: 700;
    color: #ffffff;
    margin-bottom: 0;
    line-height: 1.3;
    letter-spacing: -0.02em;
    text-shadow: 0 0 2px rgba(255, 255, 255, 0.3);
    transform: translateY(0);
    transition: all 0.3s ease;
    max-width: 100%;
    word-wrap: break-word;
    word-break: break-all;
    white-space: pre-wrap;
    overflow-wrap: break-word;

    &:hover {
      transform: translateY(-2px);
      text-shadow: 0 0 4px rgba(255, 255, 255, 0.4);
    }
  }
}

.author-info {
  display: inline-flex;
  align-items: center;
  padding: 10px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  transition: all 0.3s ease;
  margin-top: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &:hover {
    background: rgba(255, 255, 255, 0.15);
    transform: translateY(-2px);
    border-color: rgba(255, 255, 255, 0.25);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }

  .author-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    overflow: hidden;
    margin-right: 12px;
    border: 2px solid rgba(255, 255, 255, 0.2);
    transition: all 0.3s ease;

    &:hover {
      transform: scale(1.05);
      border-color: rgba(103, 232, 249, 0.5);
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .author-details {
    margin-right: 16px;

    .author-name {
      font-size: 14px;
      font-weight: 600;
      color: #ffffff;
      margin-bottom: 2px;
      display: block;
      letter-spacing: 0.02em;
      text-shadow: 0 0 1px rgba(255, 255, 255, 0.2);
    }

    .publish-time {
      font-size: 12px;
      color: rgba(255, 255, 255, 0.85);
      display: block;
    }
  }

  .view-count {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 10px;
    background: rgba(255, 255, 255, 0.15);
    border-radius: 20px;
    color: #ffffff;
    font-size: 12px;
    transition: all 0.3s ease;
    height: 24px;
    text-shadow: 0 0 1px rgba(255, 255, 255, 0.2);

    &:hover {
      background: rgba(255, 255, 255, 0.2);
      transform: translateY(-1px);
    }

    .icon-eye {
      width: 14px;
      height: 14px;
      opacity: 1;
    }
  }
}

.status-section {
  margin: 32px 0;

  .status-tag {
    display: inline-flex;
    align-items: center;
    padding: 10px 20px;
    border-radius: 30px;
    font-size: 15px;
    font-weight: 500;
    letter-spacing: 0.5px;
    backdrop-filter: blur(4px);
    transition: all 0.3s ease;

    &.expired {
      background: linear-gradient(135deg, rgba(239, 68, 68, 0.2), rgba(239, 68, 68, 0.1));
      color: #fca5a5;
      border: 1px solid rgba(239, 68, 68, 0.2);
    }

    &.ongoing {
      background: linear-gradient(135deg, rgba(16, 185, 129, 0.2), rgba(16, 185, 129, 0.1));
      color: #6ee7b7;
      border: 1px solid rgba(16, 185, 129, 0.2);
    }

    &:hover {
      transform: translateY(-2px);
      filter: brightness(1.2);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }
  }
}

.content-section {
  margin-top: 32px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: rgba(255, 255, 255, 0.15);
  }

  .markdown-body {
    font-size: 16px;
    line-height: 1.8;
    color: #ffffff;
    background: transparent !important;
    letter-spacing: 0.02em;
    text-shadow: 0 0 1px rgba(255, 255, 255, 0.2);
    word-wrap: break-word;
    word-break: break-all;
    white-space: pre-wrap;
    overflow-wrap: break-word;

    * {
      background: transparent !important;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(*) {
      background: transparent !important;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(p) {
      margin-bottom: 16px;
      color: #ffffff;
      text-shadow: 0 0 1px rgba(255, 255, 255, 0.2);
      font-weight: 400;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(h1), :deep(h2), :deep(h3) {
      color: #ffffff;
      margin: 32px 0 16px;
      font-weight: 600;
      text-shadow: 0 0 2px rgba(255, 255, 255, 0.3);
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(a) {
      color: #7dd3fc;
      text-decoration: none;
      transition: all 0.2s ease;
      padding: 2px 4px;
      border-radius: 4px;
      font-weight: 500;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
      display: inline-block;

      &:hover {
        color: #38bdf8;
        background: rgba(125, 211, 252, 0.1) !important;
        text-shadow: 0 0 8px rgba(125, 211, 252, 0.3);
      }
    }

    :deep(ul), :deep(ol) {
      padding-left: 24px;
      margin: 16px 0;
      max-width: 100%;

      li {
        margin-bottom: 8px;
        color: #ffffff;
        word-wrap: break-word;
        word-break: break-all;
        white-space: pre-wrap;
        overflow-wrap: break-word;
      }
    }

    :deep(blockquote) {
      border-left: 4px solid rgba(125, 211, 252, 0.4);
      padding-left: 20px;
      margin: 24px 0;
      color: #ffffff;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(code) {
      background: rgba(255, 255, 255, 0.1) !important;
      padding: 3px 6px;
      border-radius: 4px;
      font-size: 0.9em;
      color: #7dd3fc;
      text-shadow: 0 0 8px rgba(125, 211, 252, 0.2);
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
      display: inline-block;
    }

    :deep(img) {
      max-width: 100%;
      height: auto;
      border-radius: 8px;
      margin: 16px 0;
    }

    :deep(table) {
      width: 100%;
      margin: 16px 0;
      border-collapse: collapse;

      th, td {
        border: 1px solid rgba(255, 255, 255, 0.1);
        padding: 8px;
        word-wrap: break-word;
        word-break: break-all;
        white-space: pre-wrap;
        overflow-wrap: break-word;
      }
    }
  }
}

@keyframes backgroundMove {
  from {
    background-position: 0 0;
  }
  to {
    background-position: 60px 60px;
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #activityDetailPage {
    transition: none; /* 移动端禁用入场动画 */
    transform: none;
    opacity: 1;
  }

  .page-background {
    display: none; /* 移动端移除复杂背景 */
  }

  .preview-card {
    border-radius: 0; /* 移除圆角以减少渲染压力 */
    backdrop-filter: none; /* 移除模糊效果 */
    background: rgba(255, 255, 255, 0.03);
    transform: none !important; /* 禁用悬停动画 */
    box-shadow: none;
    border: none;

    &:hover {
      transform: none !important;
      box-shadow: none;
    }
  }

  .content-container {
    padding: 0;
    margin: 0;
  }

  .image-container {
    height: 200px; /* 减小图片高度 */

    .main-image {
      transition: none; /* 禁用图片动画 */
      will-change: auto; /* 移除硬件加速 */

      &:hover {
        transform: none;
      }
    }
  }

  .info-section {
    margin-top: -40px;
    padding: 16px;
    backdrop-filter: none; /* 移除模糊效果 */
  }

  .author-info {
    backdrop-filter: none; /* 移除模糊效果 */
    background: rgba(255, 255, 255, 0.08);
    transition: none;
    box-shadow: none;

    &:hover {
      transform: none;
      background: rgba(255, 255, 255, 0.08);
    }

    .author-avatar {
      transition: none;
      &:hover {
        transform: none;
      }
    }
  }

  .status-section {
    margin: 16px 0;

    .status-tag {
      backdrop-filter: none; /* 移除模糊效果 */
      transition: none;

      &:hover {
        transform: none;
        filter: none;
        box-shadow: none;
      }
    }
  }

  .content-section {
    margin-top: 16px;
    padding: 16px;
    backdrop-filter: none; /* 移除模糊效果 */
    background: rgba(255, 255, 255, 0.03);
    border: none;

    &:hover {
      background: rgba(255, 255, 255, 0.03);
      border: none;
    }

    .markdown-body {
      font-size: 14px; /* 减小字体大小 */
      text-shadow: none; /* 移除文字阴影 */

      :deep(img) {
        margin: 8px 0;
        width: 100%;
        height: auto;
        border-radius: 4px;
      }

      :deep(p), :deep(li), :deep(blockquote) {
        text-shadow: none;
      }

      :deep(h1), :deep(h2), :deep(h3) {
        text-shadow: none;
        margin: 16px 0 8px;
      }

      :deep(a) {
        text-shadow: none;
        &:hover {
          text-shadow: none;
        }
      }
    }
  }

  .title {
    font-size: 24px;
  }
}

/* 新的分享按钮样式 */
.share-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  color: #ffffff;
  font-size: 12px;
  transition: all 0.3s ease;
  height: 24px;
  text-shadow: 0 0 1px rgba(255, 255, 255, 0.2);
  cursor: pointer;
  margin-left: 12px;

  &:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: translateY(-1px);
  }

  .share-icon {
    font-size: 14px;
    color: #ffffff;
  }

  .share-text {
    font-size: 12px;
    color: #ffffff;
  }
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .share-btn {
    padding: 4px 8px;

    .share-text {
      display: none; /* 在移动端隐藏文字 */
    }
  }
}
</style>
