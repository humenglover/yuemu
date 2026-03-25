<template>
  <div id="activityDetailPage" :class="{ mounted }">
    <div v-if="loading" class="loading-overlay">
      <div class="liquid-loader">
        <div class="liquid-dot dot-1"></div>
        <div class="liquid-dot dot-2"></div>
        <div class="liquid-dot dot-3"></div>
      </div>
    </div>

    <div class="page-background"></div>

    <div class="content-container">
      <div class="preview-card">
        <div class="image-container">
          <div v-if="loading" class="skeleton-loading image-skeleton"></div>
          <img
            v-else
            :src="activity.coverUrl"
            :alt="activity.title"
            class="main-image"
            loading="lazy"
          />
          <div class="image-overlay"></div>
        </div>

        <div class="info-section">
          <div class="header-section">
            <div v-if="loading" class="skeleton-loading title-skeleton"></div>
            <h1 v-else class="title">{{ activity.title }}</h1>

            <div v-if="loading" class="skeleton-loading author-info-skeleton"></div>
            <div v-else class="author-info">
              <div class="author-avatar">
                <img
                  :src="activity.user?.userAvatar || getDefaultAvatar(activity.user?.userName)"
                  :alt="activity.user?.userName"
                  loading="lazy"
                />
              </div>
              <div class="author-details">
                <span class="author-name">{{ activity.user?.userName }}</span>
                <span class="publish-time">{{ formatTime(activity.createTime, 'date') }}</span>
              </div>
              <div class="view-count">
                <i class="fas fa-eye icon-eye"></i>
                <span>{{ activity.viewCount || 0 }}</span>
              </div>
              <div class="share-btn" @click="handleShare">
                <i class="fas fa-share-alt share-icon"></i>
                <span class="share-text">分享</span>
              </div>
            </div>
          </div>

          <div v-if="loading" class="skeleton-loading status-skeleton"></div>
          <div v-else class="status-section">
            <div :class="['status-tag', activity.isExpired === 1 ? 'expired' : 'ongoing']">
              {{ activity.isExpired === 1 ? '已结束' : `进行中 · ${formatTime(activity.expireTime, 'full')}截止` }}
            </div>
          </div>

          <div v-if="loading" class="skeleton-loading content-skeleton"></div>
          <div v-else class="content-section">
            <Html-content :content="activity.content" class="Html-body" />
          </div>
        </div>
      </div>
    </div>

    <ShareModal
      ref="shareModalRef"
      :title="activity.title"
      :link="shareLink"
      :imageUrl="shareImage"
      :user="activity.user"
      :create-time="activity.createTime"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import HtmlContent from '@/components/HtmlContent.vue'
import ShareModal from '@/components/ShareModal.vue'
import { getActivityByIdUsingGet } from '@/api/activityController'
import { getDefaultAvatar } from '@/utils/userUtils'
import { formatTime } from '@/utils/dateUtils'
import { doShareUsingPost } from '@/api/shareRecordController'

const route = useRoute()
const router = useRouter()
const mounted = ref(false)
const loading = ref(true)
const activity = ref<API.Activity>({} as API.Activity)

const shareModalRef = ref()
const shareLink = ref('')
const shareImage = ref('')

const getActivityDetail = async () => {
  loading.value = true
  const id = route.params.id as string

  if (!id) {
    message.error('活动ID不能为空')
    loading.value = false
    return router.back()
  }

  try {
    const res = await getActivityByIdUsingGet({ id: id as any })
    if (res.data?.data) {
      activity.value = res.data.data
      mounted.value = true
      setTimeout(() => {
        loading.value = false
      }, 800)
    } else {
      loading.value = false
      message.error('未找到该活动')
      router.back()
    }
  } catch (error) {
    loading.value = false
    message.error('获取活动详情失败')
    router.back()
  }
}

const handleShare = async () => {
  if (!activity.value?.id) {
    message.error('活动信息获取失败')
    return
  }

  shareLink.value = window.location.href
  shareImage.value = activity.value.coverUrl || ''
  shareModalRef.value?.openModal()

  try {
    const requestBody = {
      targetId: activity.value.id,
      targetType: 2,
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
  min-height: calc(100vh - 132px);
  max-width: 1400px;
  margin: 0 auto;
  position: static;
  border-radius: 4px;
  opacity: 0;
  visibility: hidden;
  transform: none;
  background: var(--header-background);
  color: var(--text-primary);
  overflow: hidden;
  display: flex;
  justify-content: center;
  transition: all 0.6s cubic-bezier(0.22, 1, 0.36, 1);

  &.mounted {
    opacity: 1;
    visibility: visible;
    transform: none;
  }
}

.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.4);
  .dark-theme & {
    background: rgba(15, 23, 42, 0.6);
  }
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

.liquid-loader {
  position: relative;
  width: 80px;
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.liquid-dot {
  position: absolute;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  filter: blur(1px);

  &.dot-1 {
    width: 32px;
    height: 32px;
    z-index: 3;
    animation: liquid-morph 4s infinite, liquid-pulse 2s ease-in-out infinite;
  }

  &.dot-2 {
    width: 48px;
    height: 48px;
    background: rgba(37, 99, 235, 0.25);
    z-index: 2;
    animation: liquid-morph 5s infinite reverse, liquid-pulse 2.5s ease-in-out infinite 0.2s;
  }

  &.dot-3 {
    width: 70px;
    height: 70px;
    background: rgba(96, 165, 250, 0.12);
    z-index: 1;
    animation: liquid-morph 6s infinite, liquid-pulse 3s ease-in-out infinite 0.4s;
  }
}

@keyframes liquid-morph {
  0%, 100% { border-radius: 42% 58% 70% 30% / 45% 45% 55% 55%; transform: rotate(0deg); }
  34% { border-radius: 70% 30% 46% 54% / 30% 29% 71% 70%; }
  67% { border-radius: 100% 60% 60% 100% / 100% 100% 60% 60%; transform: rotate(180deg); }
}

@keyframes liquid-pulse {
  0%, 100% { transform: scale(0.9); opacity: 0.6; }
  50% { transform: scale(1.1); opacity: 0.9; }
}

// 移除不再使用的旧加载样式
.loading-spinner, .loading-circle, .loading-text, .breath-bubble {
  display: none;
}

.skeleton-loading {
  background: linear-gradient(90deg, rgba(240,240,240,0.8) 25%, rgba(224,224,224,0.9) 50%, rgba(240,240,240,0.8) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.5s infinite ease-in-out;
  border-radius: 4px;
}
.image-skeleton {
  width: 100%;
  height: 500px;
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1;
}
.title-skeleton {
  height: 60px;
  width: 80%;
  margin-bottom: 24px;
}
.author-info-skeleton {
  height: 40px;
  width: 100%;
  margin-bottom: 16px;
}
.status-skeleton {
  height: 40px;
  width: 30%;
  margin: 24px 0;
}
.content-skeleton {
  height: 400px;
  width: 100%;
  margin-top: 32px;
  border-radius: 12px;
}

.page-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #f8fafd;
  .dark-theme & {
    background: #0f172a;
  }
  z-index: 0;

  &::before {
    content: '';
    position: absolute;
    top: -20vh;
    left: 50%;
    transform: translateX(-50%);
    width: 140vw;
    height: 80vh;
    background: radial-gradient(
        ellipse at center,
        rgba(37, 99, 235, 0.05) 0%,
        rgba(37, 99, 235, 0.02) 40%,
        transparent 80%
    );
    filter: blur(60px);
    .dark-theme & {
      background: radial-gradient(
          ellipse at center,
          rgba(37, 99, 235, 0.12) 0%,
          rgba(37, 99, 235, 0.08) 40%,
          transparent 80%
      );
    }
  }
}

.content-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
  position: relative;
  z-index: 1;
  width: 100%;
  transition: all 0.5s ease-out 0.2s;
  opacity: 0;
  transform: translateY(20px);

  #activityDetailPage.mounted & {
    opacity: 1;
    transform: translateY(0);
  }
}

.preview-card {
  background: rgba(255, 255, 255, 0.85);
  .dark-theme & {
    background: rgba(30, 41, 59, 0.7);
  }
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  .dark-theme & {
    border-color: rgba(255, 255, 255, 0.08);
  }
  box-shadow:
    0 20px 40px -10px rgba(0, 0, 0, 0.06),
    0 0 20px rgba(37, 99, 235, 0.02);
  border-radius: 24px; // 更加精致紧致的圆角
  position: relative;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  width: 100%;
  max-width: 100%;
  overflow: hidden;
  margin-bottom: 40px; // 压缩底部间距
}

.image-container {
  position: relative;
  width: 100%;
  height: 500px;
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(to bottom,
      transparent 0%,
      rgba(0, 0, 0, 0.02) 40%,
      rgba(0, 0, 0, 0.15) 100%
    );
    pointer-events: none;
    z-index: 2;
  }

  .main-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 1.2s cubic-bezier(0.16, 1, 0.3, 1);
    filter: brightness(1.1) saturate(1.2);
    position: relative;
    z-index: 1;

    &:hover {
      transform: scale(1.05);
    }
  }
}

.info-section {
  padding: 32px 40px; // 压缩内边距至紧致版
  color: var(--text-primary);

  @media (max-width: 768px) {
    padding: 24px 16px;
  }
}

.header-section {
  margin-bottom: 24px;

  .title {
    color: var(--text-primary);
    font-size: 36px; // 压缩字号
    font-weight: 850;
    line-height: 1.2;
    margin-bottom: 1.5rem; // 压缩下边距
    letter-spacing: -0.025em;
    max-width: 100%;
    word-wrap: break-word;
    word-break: break-all;
    white-space: pre-wrap;
    overflow-wrap: break-word;
    transition: all 0.4s ease 0.3s;
    opacity: 0;
    transform: translateY(10px);

    #activityDetailPage.mounted & {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

.author-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  transition: all 0.4s ease 0.4s;
  opacity: 0;
  transform: translateY(10px);

  #activityDetailPage.mounted & {
    opacity: 1;
    transform: translateY(0);
  }

  .author-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid #f3f4f6;
    transition: all 0.2s ease;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &:hover {
      border-color: var(--text-primary);
    }
  }

  .author-details {
    .author-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 4px;
      display: block;
    }

    .publish-time {
      font-size: 13px;
      color: var(--text-primary);
      display: block;
    }
  }

  .view-count {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 6px 14px;
    background: rgba(0, 0, 0, 0.02);
    .dark-theme & {
      background: rgba(255, 255, 255, 0.05);
    }
    border-radius: 40px;
    color: var(--text-secondary);
    font-size: 13px;
    margin-left: auto;
    font-weight: 600;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(0, 0, 0, 0.04);
      .dark-theme & { background: rgba(255, 255, 255, 0.08); }
    }

    .icon-eye {
      font-size: 14px;
      opacity: 0.6;
    }
  }

  .share-btn {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 18px;
    border-radius: 40px;
    background: linear-gradient(135deg, #2563eb, #1d4ed8);
    color: #ffffff !important; // 强制白色以防被继承覆盖
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);

    .share-icon, .share-text {
      color: #ffffff !important;
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 20px rgba(37, 99, 235, 0.3);
    }
  }
}

.status-section {
  margin: 24px 0;
  transition: all 0.4s ease 0.5s;
  opacity: 0;
  transform: translateY(10px);

  #activityDetailPage.mounted & {
    opacity: 1;
    transform: translateY(0);
  }

  .status-tag {
    display: inline-flex;
    align-items: center;
    padding: 10px 24px;
    border-radius: 40px;
    font-size: 14px;
    font-weight: 700;
    transition: all 0.3s ease;

    &.expired {
      background: rgba(0, 0, 0, 0.04);
      .dark-theme & { background: rgba(255, 255, 255, 0.05); }
      color: var(--text-secondary);
    }

    &.ongoing {
      background: rgba(37, 99, 235, 0.08);
      color: #2563eb;
      .dark-theme & { color: #60a5fa; }
    }
  }
}

.content-section {
  margin-top: 24px; // 压缩模块间距
  background: transparent;
  color: var(--text-primary);
  border-radius: 0;
  box-shadow: none;
  transition: all 0.4s ease 0.6s;
  opacity: 0;
  transform: translateY(10px);

  #activityDetailPage.mounted & {
    opacity: 1;
    transform: translateY(0);
  }

  .Html-body {
    font-size: 15px;
    line-height: 1.8;
    letter-spacing: 0.01em;
    word-wrap: break-word;
    word-break: break-all;
    white-space: pre-wrap;
    overflow-wrap: break-word;
    color: var(--text-primary)!important;
    * {
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(*) {
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(p) {
      margin-bottom: 16px;
      font-weight: 400;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(h1), :deep(h2), :deep(h3) {
      color: var(--text-primary);
      margin: 32px 0 16px;
      font-weight: 600;
      max-width: 100%;
      word-wrap: break-word;
      word-break: break-all;
      white-space: pre-wrap;
      overflow-wrap: break-word;
    }

    :deep(a) {
      color: var(--text-primary);
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
        color: var(--text-primary);
        background: rgba(37, 99, 235, 0.1) !important;
      }
    }

    :deep(ul), :deep(ol) {
      padding-left: 24px;
      margin: 16px 0;
      max-width: 100%;

      li {
        margin-bottom: 8px;
        color: var(--text-primary);
        word-wrap: break-word;
        word-break: break-all;
        white-space: pre-wrap;
        overflow-wrap: break-word;
      }
    }

    :deep(blockquote) {
      border-left: 4px solid #e5e7eb;
      padding-left: 20px;
      margin: 24px 0;
      color: var(--text-primary);
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
      color: var(--text-primary);
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

// 移除由于之前的冗余定义导致的混乱样式
.share-btn-secondary-container {
  display: none;
}

// 原有动画保留
@keyframes loading-rotate {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
// 保留核心骨架屏动画
@keyframes skeleton-loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media screen and (max-width: 768px) {
  #activityDetailPage {
    min-height: calc(100vh - 80px);
    transform: scale(0.99);
  }

  .loading-circle {
    width: 60px;
    height: 60px;
  }
  // 移动端适配呼吸气泡大小
  .breath-bubble {
    width: 30px;
    height: 30px;
  }

  .image-skeleton {
    height: 200px;
  }
  .title-skeleton {
    height: 40px;
  }
  .content-skeleton {
    height: 300px;
  }

  .page-background {
    display: none;
  }

  .content-container {
    padding: 0;
    margin: 0;
    width: 100%;
  }

  .preview-card {
    border-radius: 0;
    backdrop-filter: none;
    background: rgba(255, 255, 255, 0.03);
    transform: none !important;
    box-shadow: none;
    border: none;

    &:hover {
      transform: none !important;
      box-shadow: none;
    }
  }

  .image-container {
    height: 200px;

    .main-image {
      transition: none;
      will-change: auto;

      &:hover {
        transform: none;
      }
    }
  }

  .info-section {
    padding: 4px;
    backdrop-filter: none;
  }

  .header-section .title {
    font-size: 24px;
  }

  .author-info {
    gap: 12px;

    .author-avatar {
      width: 40px;
      height: 40px;
      transition: none;
      &:hover {
        transform: none;
      }
    }

    .view-count {
      padding: 4px 10px;
      font-size: 12px;
      &:hover {
        transform: none;
        box-shadow: none;
      }
    }

    .share-btn {
      padding: 6px 14px;
      margin-left: 8px;
      &:hover {
        transform: none;
        box-shadow: none;
      }
    }
  }

  .status-section {
    margin: 16px 0;

    .status-tag {
      padding: 8px 18px;
      font-size: 13px;
      backdrop-filter: none;
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
    backdrop-filter: none;
    background: rgba(255, 255, 255, 0.03);
    border: none;

    &:hover {
      background: rgba(255, 255, 255, 0.03);
      border: none;
    }

    .Html-body {
      font-size: 14px;
      text-shadow: none;

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

  .share-btn {
    padding: 4px 8px;

    .share-text {
      display: none;
    }
  }
}
</style>
