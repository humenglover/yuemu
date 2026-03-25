<template>
  <div class="love-board-list">
    <!-- 动态爱心背景层（保留） -->
    <div class="romantic-bg">
      <div class="floating-hearts"></div>
      <div class="light-particles"></div>
    </div>

    <div class="main-container">
      <!-- 头部区域（紧凑布局） -->
      <header class="hero-section">
        <div class="title-wrapper">
          <h1 class="main-title">
            <span class="title-text">恋爱画板广场</span>
          </h1>
          <p class="subtitle">记录每一份双向奔赴的甜蜜时光 💌</p>
        </div>
        <div class="cta-group">
          <a-button
            v-if="loginUserStore.loginUser?.id"
            type="primary"
            class="cta-button"
            @click="goToMyBoard"
          >
            <template #icon><HeartFilled /></template>
            我的画板
          </a-button>
          <a-button
            v-else
            type="primary"
            class="cta-button"
            @click="router.push('/loveboard')"
          >
            <template #icon><HeartFilled /></template>
            创建专属画板
          </a-button>
        </div>
      </header>

      <!-- 搜索区域（移动端强制单行，适配小屏幕） -->
      <div class="search-section">
        <div class="search-panel">
          <!-- 左侧搜索容器（占满剩余空间，避免换行） -->
          <div class="search-left">
            <a-input
              v-model:value="searchForm.manName"
              placeholder="他的名字"
              class="love-input male-input"
              @pressEnter="handleSearch"
            >
              <template #prefix><UserOutlined class="icon-male" /></template>
            </a-input>
            <span class="love-symbol">💞</span>
            <a-input
              v-model:value="searchForm.womanName"
              placeholder="她的名字"
              class="love-input female-input"
              @pressEnter="handleSearch"
            >
              <template #prefix><UserOutlined class="icon-female" /></template>
            </a-input>
          </div>
          <!-- 右侧按钮（固定宽度，不挤压搜索框） -->
          <a-button
            type="primary"
            class="search-btn"
            @click="handleSearch"
            :loading="loading"
          >
            <template #icon><SearchOutlined /></template>
            搜索
          </a-button>
        </div>
      </div>

      <!-- 画板展示区（紧凑卡片布局） -->
      <section class="board-gallery">
        <div class="gallery-header">
          <h2 class="gallery-title">
            <span class="title-icon">💖</span> 公开的甜蜜故事
          </h2>
          <p class="gallery-desc">{{ infinitePagination.total }} 对情侣正在分享幸福</p>
        </div>

        <div class="gallery-grid">
          <div
            v-for="board in boardList"
            :key="board.id"
            class="love-card"
            @click="goBoardDetail(board.id)"
          >
            <div class="card-header">
              <img
                :src="board.bgCover || defaultBg"
                alt="背景"
                class="card-bg"
              >
              <div class="card-overlay"></div>
              <div class="view-tag">
                <EyeOutlined class="tag-icon" />
                <span class="tag-text">{{ board.viewCount || 0 }} 次见证</span>
              </div>
            </div>
            <div class="card-body">
              <div class="couple-info">
                <div class="avatar-group">
                  <div class="avatar-wrapper">
                    <img
                      :src="board.manCover || defaultAvatar"
                      alt="他的头像"
                      class="avatar male-avatar"
                    >
                    <div class="avatar-border male-border"></div>
                  </div>
                  <div class="avatar-connector">
                    <span class="connector-heart">💖</span>
                  </div>
                  <div class="avatar-wrapper">
                    <img
                      :src="board.womanCover || defaultAvatar"
                      alt="她的头像"
                      class="avatar female-avatar"
                    >
                    <div class="avatar-border female-border"></div>
                  </div>
                </div>
                <h3 class="couple-names">
                  <span class="man-name">{{ board.manName || '他' }}</span>
                  <span class="and-symbol">&</span>
                  <span class="woman-name">{{ board.womanName || '她' }}</span>
                </h3>
              </div>
              <div class="card-footer">
                <div class="create-time">
                  <CalendarOutlined class="time-icon" />
                  <span class="time-text">{{ formatDate(board.createTime) }}</span>
                </div>
                <div class="card-arrow">→</div>
              </div>
            </div>
            <div class="card-hover-effect"></div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="!loading && boardList.length === 0" class="empty-state">
          <div class="empty-container">
            <div class="empty-icon">💌</div>
            <h3 class="empty-title">还没有公开的恋爱故事</h3>
            <p class="empty-desc">成为第一个分享甜蜜的人～</p>
            <a-button type="primary" shape="round" class="empty-cta" @click="router.push('/loveboard')">
              创建我的恋爱画板
            </a-button>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <div class="loading-container">
            <p class="loading-text">加载甜蜜的恋爱故事中...</p>
          </div>
        </div>
      </section>

      <!-- 加载更多提示 -->
      <div class="load-more-footer">
        <div v-if="loadingMore" class="loading-more">
          <div class="loading-spinner"></div>
          <span>加载更多恋爱故事中...</span>
        </div>
        <div v-else-if="!hasMore" class="no-more">
          <span>已经到底啦～ 共{{ infinitePagination.total }}个甜蜜故事</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// 脚本逻辑完全不变，保留原功能
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  HeartFilled,
  UserOutlined,
  SearchOutlined,
  CalendarOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { listPublicLoveBoardsUsingGet, getMyLoveBoardUsingGet } from '@/api/loveBoardController'
import dayjs from 'dayjs'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 默认图片地址
const defaultAvatar = 'https://img.yuemuchat.com/avatar/default-couple-love.png'
const defaultBg = 'https://img.yuemuchat.com/loveboard/bg-soft-romantic.jpg'

// 响应式数据
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const boardList = ref<API.LoveBoardVO[]>([])
const searchForm = reactive({
  manName: '',
  womanName: ''
})
const infinitePagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0
})

// 加载列表数据
const loadBoardList = async (reset = true) => {
  if (reset) {
    loading.value = true
    infinitePagination.current = 1
  } else {
    loadingMore.value = true
  }
  try {
    const res = await listPublicLoveBoardsUsingGet({
      current: infinitePagination.current,
      size: infinitePagination.pageSize,
      manName: searchForm.manName || undefined,
      womanName: searchForm.womanName || undefined
    })
    if (res.data) {
      if (reset) {
        boardList.value = res.data.data.records || []
      } else {
        boardList.value = [...boardList.value, ...(res.data.data.records || [])]
      }
      infinitePagination.total = res.data.data.total || 0
      hasMore.value = (res.data.data.records || []).length === infinitePagination.pageSize && boardList.value.length < infinitePagination.total
    }
  } catch (err: any) {
    message.error('加载失败：' + (err.message || '未知错误'))
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 搜索事件
const handleSearch = () => {
  infinitePagination.current = 1
  hasMore.value = true
  loadBoardList()
}

// 跳转详情页
const goBoardDetail = (id: number) => {
  router.push(`/loveboard/${id}`)
}

// 跳转我的画板
const goToMyBoard = async () => {
  try {
    const res = await getMyLoveBoardUsingGet()
    if (res.data?.id) {
      router.push({ name: 'LoveBoardDetail', params: { id: res.data.id } })
    } else {
      router.push('/loveboard')
    }
  } catch (err: any) {
    message.error('获取我的画板失败：' + (err.message || '未知错误'))
  }
}

// 日期格式化
const formatDate = (dateStr: string) => {
  return dayjs(dateStr).format('YYYY年MM月DD日')
}

// 触底加载更多
const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return

  infinitePagination.current += 1
  await loadBoardList(false)
}

// 滚动事件处理
const handleScroll = () => {
  // 计算页面滚动到底部的距离
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const clientHeight = document.documentElement.clientHeight
  const scrollHeight = document.documentElement.scrollHeight

  // 当距离底部小于200px时触发加载
  if (scrollHeight - scrollTop - clientHeight < 200) {
    loadMore()
  }
}

// 初始化飘心效果
onMounted(() => {
  loadBoardList()
  initFloatingHearts()

  // 添加滚动事件监听
  window.addEventListener('scroll', handleScroll)
})

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

const initFloatingHearts = () => {
  const container = document.querySelector('.floating-hearts')
  if (!container) return

  const createHeart = () => {
    const heart = document.createElement('div')
    heart.className = 'floating-heart'
    const heartIcons = ['💕', '💗', '💖', '💝', '💘', '💓', '💞', '💟']
    heart.textContent = heartIcons[Math.floor(Math.random() * heartIcons.length)]

    // 随机样式
    const size = Math.random() * 12 + 6
    const left = Math.random() * 100
    const delay = Math.random() * 3
    const duration = Math.random() * 8 + 12
    const opacity = Math.random() * 0.4 + 0.1

    heart.style.cssText = `
      position: absolute;
      left: ${left}%;
      bottom: -30px;
      font-size: ${size}px;
      opacity: ${opacity};
      animation: floatUp ${duration}s linear infinite;
      transform: translateY(0) rotate(${Math.random() * 360}deg);
      pointer-events: none;
      z-index: 0;
    `

    container.appendChild(heart)
    setTimeout(() => {
      heart.remove()
    }, duration * 1000)
  }

  setInterval(createHeart, 800)
}
</script>

<style scoped lang="scss">
// 核心色彩系统（保持浪漫风格）
$primary: #ff7a9e;
$primary-light: #ffccd8;
$primary-dark: #d6336c;
$secondary: #9d8df1;
$secondary-light: #e8e4ff;
$neutral: #f9f5f7;
$text-primary: #4a2c3b;
$text-secondary: #8a6d7c;
$white: #ffffff;
$shadow: rgba(255, 122, 158, 0.15);

// 混合器（保留）
@mixin glass-effect($opacity: 0.95, $blur: 12px) {
  background: rgba(255, 255, 255, $opacity);
  backdrop-filter: blur($blur);
  -webkit-backdrop-filter: blur($blur);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 32px $shadow;
}

@mixin gradient($type: 'primary') {
  @if $type == 'primary' {
    background: linear-gradient(135deg, $primary, $primary-light);
  } @else if $type == 'secondary' {
    background: linear-gradient(135deg, $secondary, $secondary-light);
  } @else if $type == 'dark' {
    background: linear-gradient(135deg, $primary-dark, $primary);
  }
}

// 全局样式
.love-board-list {
  min-height: 100vh;
  position: relative;
  margin-bottom: 6px;
  overflow: hidden;
  background-color: $neutral;
  background-image:
    radial-gradient(circle at 20% 30%, rgba(255, 204, 216, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 70%, rgba(232, 228, 255, 0.1) 0%, transparent 50%);
}

// 动态背景动画
.romantic-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;

  .light-particles {
    position: absolute;
    inset: 0;
    background-image:
      radial-gradient(circle at 20% 30%, rgba(255, 122, 158, 0.08) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(157, 141, 241, 0.08) 0%, transparent 50%);
    animation: shimmer 10s infinite ease-in-out;
  }
}

@keyframes floatUp {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 0.4;
  }
  90% {
    opacity: 0.4;
  }
  100% {
    transform: translateY(-100vh) rotate(360deg);
    opacity: 0;
  }
}

@keyframes shimmer {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 1. 主容器（紧凑顶部间距） */
.main-container {
  position: relative;
  z-index: 1;
  max-width: 1320px;
  margin: 0 auto;
  padding: 30px 24px; /* 顶部间距减半 */

  @media (max-width: 768px) {
    padding: 20px 16px;
  }
}

/* 2. 头部区域（压缩垂直空间） */
.hero-section {
  text-align: center;
  margin-bottom: 20px; /* 大幅减少底部间距 */
  padding: 0 20px;

  .title-wrapper {
    margin-bottom: 12px; /* 减少标题底部间距 */

    .main-title {
      font-size: 32px; /* 缩小标题字体 */
      font-weight: 700;
      color: $text-primary;
      margin-bottom: 6px; /* 减少副标题间距 */
      letter-spacing: 0.3px;

      .title-text {
        background: linear-gradient(90deg, $primary-dark, $primary, $secondary);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        display: inline-block;
        position: relative;

        &::after {
          content: '';
          position: absolute;
          bottom: -4px; /* 缩小下划线间距 */
          left: 15%;
          width: 70%;
          height: 3px; /* 缩小下划线高度 */
          background: linear-gradient(90deg, transparent, $primary-light, transparent);
          border-radius: 2px;
        }
      }
    }

    .subtitle {
      font-size: 15px; /* 缩小副标题字体 */
      color: $text-secondary;
      font-weight: 500;
      line-height: 1.5;
    }
  }

  .cta-group {
    display: flex;
    justify-content: center;
  }
}

/* 3. 按钮样式（紧凑尺寸） */
.cta-button {
  @include gradient('primary');
  border-radius: 28px;
  padding: 0 32px;
  height: 44px; /* 缩小按钮高度 */
  font-size: 15px; /* 缩小按钮字体 */
  font-weight: 600;
  color: $white;
  border: none;
  box-shadow: 0 4px 16px rgba($primary, 0.25);
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba($primary, 0.3);
    background: linear-gradient(135deg, $primary-dark, $primary);
    color: $white;
  }

  @media (max-width: 768px) {
    width: 100%;
    max-width: 280px;
  }
}

/* 4. 搜索区域（核心优化：移动端单行适配） */
.search-section {
  margin-bottom: 24px; /* 减少底部间距 */

  .search-panel {
    @include glass-effect();
    border-radius: 28px;
    padding: 8px 12px; /* 减少内边距 */
    max-width: 100%;
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: 8px; /* 缩小元素间距 */
    /* 禁止换行，强制单行 */
    white-space: nowrap;
    overflow: hidden;

    @media (max-width: 768px) {
      padding: 6px 8px;
      gap: 6px;
    }

    /* 左侧搜索容器（占满剩余空间，内部Flex布局） */
    .search-left {
      display: flex;
      align-items: center;
      gap: 6px; /* 缩小搜索框之间的间距 */
      flex: 1; /* 占满剩余空间 */
      overflow: hidden; /* 隐藏溢出内容 */

      .love-input {
        flex: 1; /* 两个搜索框平分空间 */
        min-width: 80px; /* 移动端最小宽度，避免过窄 */
        max-width: calc(50% - 12px); /* 限制最大宽度，防止挤压 */
        border-radius: 22px;
        background: rgba(255, 255, 255, 0.95);
        border: 1px solid rgba($primary, 0.15);
        transition: all 0.3s ease;
        overflow: hidden; /* 隐藏输入框溢出内容 */

        &.male-input {
          :deep(.ant-input-prefix) {
            color: #64b5f6;
            font-size: 14px; /* 缩小图标尺寸 */
          }
        }

        &.female-input {
          :deep(.ant-input-prefix) {
            color: $primary;
            font-size: 14px; /* 缩小图标尺寸 */
          }
        }

        &:focus-within {
          border-color: $primary;
          box-shadow: 0 0 0 3px rgba($primary, 0.1);
        }

        :deep(.ant-input) {
          border-radius: 22px;
          padding: 7px 12px 7px 34px; /* 缩小内边距 */
          font-size: 13px; /* 缩小输入框字体 */
          color: $text-primary;
          background: transparent;
          /* 强制单行输入，不换行 */
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        :deep(.ant-input::placeholder) {
          color: $text-secondary;
          opacity: 0.8;
          font-size: 12px; /* 缩小占位符字体 */
        }
      }

      .love-symbol {
        font-size: 14px; /* 缩小爱心符号 */
        color: $primary;
        flex-shrink: 0; /* 不压缩符号 */
      }
    }

    /* 搜索按钮（固定尺寸，不挤压） */
    .search-btn {
      @include gradient('dark');
      border-radius: 22px;
      padding: 0 18px; /* 缩小按钮内边距 */
      height: 34px; /* 缩小按钮高度 */
      font-size: 13px; /* 缩小按钮字体 */
      font-weight: 600;
      color: $white;
      border: none;
      box-shadow: 0 3px 10px rgba($primary-dark, 0.2);
      transition: all 0.3s ease;
      flex-shrink: 0; /* 不压缩按钮 */
      white-space: nowrap;

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba($primary-dark, 0.3);
        background: linear-gradient(135deg, #c22a57, $primary-dark);
        color: $white;
      }

      :deep(.anticon) {
        font-size: 14px; /* 缩小图标尺寸 */
        margin-right: 4px;
      }
    }
  }
}

/* 5. 画廊头部（紧凑布局） */
.gallery-header {
  margin-bottom: 16px; /* 减少底部间距 */
  padding: 0 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  .gallery-title {
    font-size: 18px; /* 缩小标题字体 */
    font-weight: 600;
    color: $text-primary;
    display: flex;
    align-items: center;
    gap: 8px;

    .title-icon {
      animation: floatHeart 3s infinite ease-in-out;
      font-size: 16px;
    }
  }

  .gallery-desc {
    font-size: 13px; /* 缩小描述字体 */
    color: $text-secondary;
    margin-top: 0;
    white-space: nowrap;
  }
}

@keyframes floatHeart {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* 6. 画廊网格（紧凑卡片间距） */
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); /* 缩小卡片宽度 */
  gap: 22px; /* 缩小卡片间距 */

  @media (max-width: 1024px) {
    gap: 18px;
  }

  @media (max-width: 768px) {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); /* 移动端自适应宽度 */
    gap: 16px;
  }
}

/* 7. 恋爱卡片（紧凑尺寸） */
.love-card {
  position: relative;
  border-radius: 20px; /* 缩小圆角 */
  overflow: hidden;
  cursor: pointer;
  @include glass-effect();
  transition: all 0.4s cubic-bezier(0.25, 1, 0.5, 1);
  height: 100%;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 12px 30px $shadow;

    .card-bg {
      transform: scale(1.06);
    }

    .card-hover-effect {
      opacity: 1;
    }

    .card-arrow {
      transform: translateX(3px);
    }
  }

  .card-header {
    position: relative;
    height: 110px; /* 缩小卡片头部高度 */
    overflow: hidden;

    @media (max-width: 768px) {
      height: 85px; /* 移动端进一步缩小 */
    }

    .card-bg {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transition: transform 0.6s ease;
      filter: brightness(0.95);
    }

    .card-overlay {
      position: absolute;
      inset: 0;
      background: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, 0.25) 100%);
    }

    .view-tag {
      position: absolute;
      top: 8px;
      right: 8px;
      background: rgba(255, 255, 255, 0.85);
      border-radius: 14px;
      padding: 3px 10px;
      font-size: 11px; /* 缩小标签字体 */
      color: $text-secondary;
      display: flex;
      align-items: center;
      gap: 5px;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);

      .tag-icon {
        font-size: 12px; /* 缩小图标尺寸 */
        color: $primary;
      }
    }
  }

  .card-body {
    padding: 20px 16px; /* 缩小内边距 */
    flex: 1;
    display: flex;
    flex-direction: column;

    @media (max-width: 768px) {
      padding: 16px 12px;
    }
  }

  .couple-info {
    margin-bottom: 12px; /* 减少底部间距 */

    .avatar-group {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px; /* 缩小头像间距 */
      margin-bottom: 8px; /* 减少底部间距 */

      .avatar-wrapper {
        position: relative;
        width: 48px; /* 缩小头像尺寸 */
        height: 48px;
        border-radius: 50%;
        overflow: hidden;
        z-index: 1;

        @media (max-width: 768px) {
          width: 40px;
          height: 40px;
        }

        .avatar {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.3s ease;
        }

        .avatar-border {
          position: absolute;
          inset: -1.5px;
          border-radius: 50%;
          z-index: -1;
          transition: all 0.3s ease;
        }

        .male-border {
          background: linear-gradient(135deg, #64b5f6, #bbdefb);
          box-shadow: 0 0 10px rgba(100, 181, 246, 0.3);
        }

        .female-border {
          background: linear-gradient(135deg, $primary, $primary-light);
          box-shadow: 0 0 10px rgba($primary, 0.3);
        }

        &:hover .avatar {
          transform: scale(1.05);
        }
      }

      .avatar-connector {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 20px; /* 缩小连接器尺寸 */
        height: 20px;

        @media (max-width: 768px) {
          width: 16px;
          height: 16px;
        }

        .connector-heart {
          font-size: 14px; /* 缩小爱心尺寸 */
          animation: heartbeat 2s infinite;
        }
      }
    }

    .couple-names {
      font-size: 16px; /* 缩小姓名字体 */
      font-weight: 600;
      color: $text-primary;
      text-align: center;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      line-height: 1.4;
      white-space: nowrap; /* 姓名不换行 */
      overflow: hidden;
      text-overflow: ellipsis; /* 溢出省略 */

      @media (max-width: 768px) {
        font-size: 14px;
      }

      .and-symbol {
        font-size: 12px; /* 缩小&符号 */
        color: $primary;
        font-weight: normal;
      }

      .man-name, .woman-name {
        padding: 2px 6px;
        border-radius: 6px;
        transition: background 0.3s ease;

        &:hover {
          background: rgba($primary-light, 0.3);
        }
      }
    }
  }

  .card-footer {
    margin-top: auto;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 8px; /* 减少顶部间距 */
    border-top: 1px solid rgba($primary, 0.1);

    .create-time {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 12px; /* 缩小时间字体 */
      color: $text-secondary;

      .time-icon {
        font-size: 12px; /* 缩小图标尺寸 */
        color: $primary;
      }
    }

    .card-arrow {
      font-size: 14px; /* 缩小箭头尺寸 */
      color: $primary;
      transition: transform 0.3s ease;
    }
  }

  .card-hover-effect {
    position: absolute;
    inset: 0;
    background: linear-gradient(to bottom, rgba($primary-light, 0.05) 0%, rgba($primary-light, 0.1) 100%);
    opacity: 0;
    transition: opacity 0.4s ease;
    pointer-events: none;
    border-radius: 20px;
  }
}

@keyframes heartbeat {
  0%, 100% { transform: scale(1); }
  20% { transform: scale(1.15); }
  40% { transform: scale(1); }
  60% { transform: scale(1.15); }
  80% { transform: scale(1); }
}

/* 8. 空状态（紧凑尺寸） */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;

  .empty-container {
    @include glass-effect();
    border-radius: 20px;
    padding: 36px 24px;
    text-align: center;
    max-width: 380px;
    width: 100%;
    box-shadow: 0 10px 32px $shadow;

    @media (max-width: 768px) {
      padding: 28px 20px;
    }

    .empty-icon {
      font-size: 60px;
      margin-bottom: 18px;
      animation: floatHeart 3s infinite ease-in-out;
      color: $primary;
      filter: drop-shadow(0 3px 9px rgba($primary, 0.2));

      @media (max-width: 768px) {
        font-size: 50px;
      }
    }

    .empty-title {
      font-size: 20px;
      color: $text-primary;
      font-weight: 600;
      margin-bottom: 10px;

      @media (max-width: 768px) {
        font-size: 18px;
      }
    }

    .empty-desc {
      font-size: 14px;
      color: $text-secondary;
      line-height: 1.5;
      margin-bottom: 24px;

      @media (max-width: 768px) {
        font-size: 13px;
      }
    }

    .empty-cta {
      @include gradient('primary');
      border-radius: 24px;
      padding: 0 32px;
      height: 42px;
      font-size: 15px;
      font-weight: 600;
      color: $white;
      border: none;
      box-shadow: 0 4px 16px rgba($primary, 0.25);
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba($primary, 0.3);
        background: linear-gradient(135deg, $primary-dark, $primary);
        color: $white;
      }

      @media (max-width: 768px) {
        height: 38px;
        font-size: 14px;
        padding: 0 28px;
      }
    }
  }
}

/* 9. 加载状态（紧凑尺寸） */
.loading-state {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;


  .loading-container {
    text-align: center;

    .loading-spinner {
      width: 40px;
      height: 40px;
      border: 3px solid $primary-light;
      border-top: 3px solid $primary;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 12px;
    }

    .loading-text {
      font-size: 14px;
      color: $text-secondary;
    }
  }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 10. 分页（紧凑尺寸） */
.pagination-footer {
  display: flex;
  justify-content: center;
  padding: 20px 20px;
  margin-top: 16px;

  .custom-pagination {
    @include glass-effect();
    border-radius: 20px;
    padding: 8px 16px;

    @media (max-width: 768px) {
      padding: 6px 8px;
    }

    :deep(.ant-pagination) {
      display: flex;
      align-items: center;
      gap: 6px;

      .ant-pagination-item,
      .ant-pagination-prev,
      .ant-pagination-next {
        border-radius: 50%;
        border: 1px solid rgba($primary, 0.15);
        transition: all 0.3s ease;
        min-width: 32px;
        height: 32px;
        line-height: 32px;
        background: transparent;

        &:hover {
          border-color: $primary;
          background: rgba($primary-light, 0.2);
          transform: translateY(-1.5px);
        }

        a, span {
          color: $text-primary;
          font-size: 13px;
        }
      }

      .ant-pagination-item-active {
        background: $primary;
        border-color: $primary;
        box-shadow: 0 3px 10px rgba($primary, 0.2);

        a {
          color: $white;
          font-weight: 600;
        }

        &:hover {
          background: $primary-dark;
          border-color: $primary-dark;
          transform: translateY(-1.5px);
        }
      }

      .ant-pagination-options {
        margin-left: 12px;

        :deep(.ant-select-selector) {
          border-radius: 14px;
          border: 1px solid rgba($primary, 0.15);
          background: transparent;

          &:hover {
            border-color: $primary;
          }

          :deep(.ant-select-selection-item) {
            font-size: 13px;
          }
        }
      }

      .ant-pagination-total-text {
        color: $text-secondary;
        font-size: 13px;
      }
    }
  }
}

/* 加载更多提示 */
.load-more-footer {
  padding: 20px 20px;
  text-align: center;

  .loading-more {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;

    .loading-spinner {
      width: 20px;
      height: 20px;
      border: 2px solid $primary-light;
      border-top: 2px solid $primary;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    span {
      font-size: 14px;
      color: $text-secondary;
    }
  }

  .no-more {
    span {
      font-size: 14px;
      color: $text-secondary;
      padding: 8px 12px;
      background: rgba(255, 255, 255, 0.6);
      border-radius: 20px;
    }
  }
}

/* 移动端特殊适配（确保搜索单行） */
@media (max-width: 480px) {
  .search-panel {
    padding: 4px 6px !important;
  }

  .search-left {
    gap: 4px !important;
  }

  .love-input {
    min-width: 70px !important;
    max-width: calc(50% - 8px) !important;
  }

  .love-input :deep(.ant-input) {
    padding: 6px 10px 6px 30px !important;
    font-size: 12px !important;
  }

  .search-btn {
    padding: 0 14px !important;
    font-size: 12px !important;
    height: 30px !important;
  }

  .gallery-grid {
    grid-template-columns: repeat(2, 1fr) !important; /* 移动端两列布局 */
    gap: 12px !important;
  }

  .card-header {
    height: 75px !important;
  }

  .couple-names {
    font-size: 13px !important;
  }
}
</style>
