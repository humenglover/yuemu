<template>
  <div id="searchPicturePage">
    <!-- 顶部导航栏 -->
    <div class="top-nav">
      <div class="nav-left" @click="goBack">
        <i class="fas fa-arrow-left back-icon"></i>
      </div>
      <h1 class="nav-title">以图搜图</h1>
      <div class="nav-right"></div>
    </div>

    <!-- 原图展示区 - 带装饰效果 -->
    <div class="source-container">
      <div class="source-card">
        <div class="card-decorator top-left"></div>
        <div class="card-decorator top-right"></div>
        <div class="card-decorator bottom-left"></div>
        <div class="card-decorator bottom-right"></div>

        <div class="source-image-wrapper" :class="{ loading: !picture.url }">
          <img
            v-if="picture.url"
            :alt="picture.name || '原图'"
            :src="picture.thumbnailUrl ?? picture.url"
            class="source-image"
            loading="lazy"
          />
          <div v-else class="image-loading">
            <div class="loading-spinner"></div>
          </div>
        </div>

        <div class="source-info">
          <span class="info-text">正在识别相似图片...</span>
        </div>
      </div>
    </div>

    <!-- 结果统计 - 带渐变文字 -->
    <div class="result-stats" v-if="!loading && dataList.length > 0">
      <h2 class="stats-title">
        <span class="gradient-text">找到 {{ dataList.length }} 张相似图片</span>
      </h2>
      <p class="stats-desc">为你推荐相似的高清图片资源</p>
    </div>

    <!-- 加载状态 - 优化动画 -->
    <div v-if="loading" class="loading-container">
      <div class="pulse-loader">
        <div class="pulse-dot"></div>
        <div class="pulse-dot"></div>
        <div class="pulse-dot"></div>
      </div>
      <p class="loading-text">正在深度识别中...</p>
    </div>

    <!-- 空状态 - 优化设计 -->
    <div v-else-if="dataList.length === 0" class="empty-container">
      <div class="empty-illustration">
        <div class="illustration-bg"></div>
        <i class="fas fa-search empty-icon"></i>
      </div>
      <h3 class="empty-title">未找到相似图片</h3>
      <p class="empty-desc">尝试使用更清晰的图片或调整搜索方式</p>
      <button class="retry-btn" @click="fetchResultData">重新搜索</button>
    </div>

    <!-- 结果网格 - 瀑布流布局 + 卡片效果 -->
    <div v-else class="results-grid">
      <div
        v-for="(item, index) in dataList"
        :key="index"
        class="result-card"
        @click="item.fromUrl ? window.open(item.fromUrl, '_blank') : ''"
      >
        <!-- 图片层 - 带过渡效果 -->
        <div class="card-image-wrapper">
          <img
            :alt="item.name || `相似图片${index+1}`"
            :src="item.thumbUrl || item.url"
            class="card-image"
            loading="lazy"
          />
          <div class="image-overlay"></div>
        </div>

        <!-- 信息层 - 悬停显示 -->
        <div class="card-info">
          <div class="info-left">
            <span class="image-count">{{ Math.floor(Math.random() * 1000) + 1 }}+ 收藏</span>
          </div>
          <div class="info-right">
            <button
              class="action-btn view-btn"
              :title="item.fromUrl ? '查看原图' : '无原图链接'"
              @click.stop="item.fromUrl ? window.open(item.fromUrl, '_blank') : showToast('无原图链接')"
            >
              <i class="fas fa-external-link-alt"></i>
            </button>
            <button
              class="action-btn copy-btn"
              :title="item.fromUrl ? '复制链接' : '无链接可复制'"
              @click.stop="copyLink(item.fromUrl)"
            >
              <i class="fas fa-copy"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  getPictureVoByIdUsingGet,
  searchPictureByPictureUsingPost,
} from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRouter().currentRoute

// 获取图片ID
const pictureId = computed(() => {
  return route.value.query?.pictureId
})

// 原图信息
const picture = ref<API.PictureVO>({})

// 搜索结果数据
const dataList = ref<API.ImageSearchResult[]>([])
const loading = ref<boolean>(true)

// 返回上一页
const goBack = () => {
  router.back()
}

// 显示提示
const showToast = (msg: string) => {
  message.warning(msg)
}

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    const res = await getPictureVoByIdUsingGet({
      id: pictureId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

// 获取搜图结果
const fetchResultData = async () => {
  loading.value = true
  try {
    const res = await searchPictureByPictureUsingPost({
      pictureId: pictureId.value,
    })
    if (res.data.code === 0 && res.data.data) {
      dataList.value = res.data.data ?? []
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取数据失败，' + e.message)
  }
  loading.value = false
}

// 复制链接
const copyLink = (url?: string) => {
  if (!url) {
    message.warning('无可用链接')
    return
  }

  navigator.clipboard.writeText(url)
    .then(() => message.success('链接已复制'))
    .catch(() => message.error('复制失败，请手动复制'))
}

// 页面加载时请求数据
onMounted(() => {
  fetchPictureDetail()
  fetchResultData()
})
</script>

<style scoped>
#searchPicturePage {
  min-height: 100vh;
  background-color: var(--background);
  color: var(--text-primary);
  transition: var(--theme-transition);
  padding-bottom: 60px;
}

/* 顶部导航栏 - 抖音风格 */
.top-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background-color: var(--header-background);
  backdrop-filter: blur(10px);
  position: sticky;
  top: 0;
  z-index: 999;
  border-bottom: 1px solid var(--border-color);
}

.nav-left {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--hover-background);
  cursor: pointer;
  transition: all 0.2s ease;
}

.nav-left:hover {
  background-color: var(--border-color);
}

.back-icon {
  font-size: 20px;
  color: var(--text-primary);
}

.nav-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.nav-right {
  width: 44px;
  height: 44px;
}

/* 原图展示容器 */
.source-container {
  padding: 24px 20px;
  display: flex;
  justify-content: center;
}

.source-card {
  width: 100%;
  max-width: 360px;
  border-radius: 20px;
  overflow: hidden;
  background-color: var(--card-background);
  box-shadow: 0 8px 32px var(--shadow-color);
  position: relative;
  transition: all 0.3s ease;
}

.source-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px var(--shadow-color);
}

/* 卡片装饰角 - 增加设计感 */
.card-decorator {
  position: absolute;
  width: 24px;
  height: 24px;
  z-index: 10;
}

.top-left {
  top: 0;
  left: 0;
  border-top: 2px solid var(--nav-item-active-text);
  border-left: 2px solid var(--nav-item-active-text);
  border-radius: 4px 0 0 0;
}

.top-right {
  top: 0;
  right: 0;
  border-top: 2px solid var(--nav-item-active-text);
  border-right: 2px solid var(--nav-item-active-text);
  border-radius: 0 4px 0 0;
}

.bottom-left {
  bottom: 0;
  left: 0;
  border-bottom: 2px solid var(--nav-item-active-text);
  border-left: 2px solid var(--nav-item-active-text);
  border-radius: 0 0 0 4px;
}

.bottom-right {
  bottom: 0;
  right: 0;
  border-bottom: 2px solid var(--nav-item-active-text);
  border-right: 2px solid var(--nav-item-active-text);
  border-radius: 0 0 4px 0;
}

.source-image-wrapper {
  width: 100%;
  aspect-ratio: 4/3;
  position: relative;
}

.source-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.source-info {
  padding: 16px;
  background: linear-gradient(to top, rgba(0,0,0,0.1) 0%, transparent 100%);
}

.info-text {
  font-size: 14px;
  color: var(--text-secondary);
}

/* 结果统计 */
.result-stats {
  padding: 0 20px 20px;
  text-align: center;
}

.stats-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 8px 0;
}

.gradient-text {
  background: linear-gradient(90deg, var(--nav-item-active-text), var(--link-hover-color));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.stats-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

/* 加载状态 - 优化动画 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.pulse-loader {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
}

.pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--nav-item-active-text);
  animation: pulse 1.5s ease-in-out infinite;
}

.pulse-dot:nth-child(2) {
  animation-delay: 0.3s;
}

.pulse-dot:nth-child(3) {
  animation-delay: 0.6s;
}

.loading-text {
  font-size: 16px;
  color: var(--text-secondary);
}

.image-loading {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--hover-background);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid var(--border-color);
  border-top: 4px solid var(--nav-item-active-text);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 空状态 - 优化设计 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-illustration {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 24px;
}

.illustration-bg {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: var(--nav-item-active);
  z-index: 1;
}

.empty-icon {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 40px;
  color: var(--nav-item-active-text);
  z-index: 2;
}

.empty-title {
  font-size: 20px;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 24px 0;
  max-width: 300px;
}

.retry-btn {
  padding: 12px 24px;
  border-radius: 30px;
  background-color: var(--nav-item-active-text);
  color: var(--text-other);
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.retry-btn:hover {
  background-color: var(--link-hover-color);
  transform: scale(1.05);
}

/* 结果网格 - 瀑布流布局 */
.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  padding: 0 20px;
}

/* 结果卡片 - 抖音风格 */
.result-card {
  border-radius: 16px;
  overflow: hidden;
  background-color: var(--card-background);
  box-shadow: 0 4px 16px var(--shadow-color);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  aspect-ratio: 3/4;
}

.result-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 12px 24px var(--shadow-color);
}

.card-image-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  overflow: hidden;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.result-card:hover .card-image {
  transform: scale(1.1);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to bottom, transparent 50%, rgba(0,0,0,0.6) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.result-card:hover .image-overlay {
  opacity: 1;
}

/* 卡片信息层 - 悬停显示 */
.card-info {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 10;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.result-card:hover .card-info {
  opacity: 1;
}

.info-left {
  display: flex;
  align-items: center;
}

.image-count {
  font-size: 12px;
  color: var(--text-other);
  background-color: rgba(0,0,0,0.5);
  padding: 4px 8px;
  border-radius: 12px;
}

.info-right {
  display: flex;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.dark-theme .action-btn {
  background-color: rgba(0,0,0,0.6);
  color: var(--text-other);
}

.action-btn:hover {
  background-color: var(--nav-item-active-text);
  color: var(--text-other);
  transform: scale(1.1);
}

/* 动画定义 */
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.7; }
  50% { transform: scale(1.5); opacity: 1; }
}

/* 响应式适配 - 移动端优先 */
@media screen and (max-width: 768px) {
  .results-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 12px;
    padding: 0 16px;
  }

  .source-container {
    padding: 16px;
  }

  .result-stats {
    padding: 0 16px 16px;
  }
}

@media screen and (max-width: 480px) {
  .top-nav {
    padding: 12px 16px;
  }

  .nav-title {
    font-size: 18px;
  }

  .results-grid {
    grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
    gap: 8px;
  }

  .result-card {
    aspect-ratio: 1/1;
  }

  .action-btn {
    width: 28px;
    height: 28px;
  }
}
</style>
