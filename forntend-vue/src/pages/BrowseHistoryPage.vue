<template>
  <div class="browse-history-page">
    <div class="page-container">
      <!-- 顶部导航栏 -->
      <div class="page-header">
        <h1 class="header-title">浏览历史</h1>
        <div class="header-actions">
          <button
            class="delete-selected-btn"
            @click="deleteSelected"
            :class="{ disabled: selectedIds.length === 0 }"
          >
            删除选中
          </button>
        </div>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-tabs">
          <div
            class="filter-tab"
            :class="{ active: activeFilter === 'all' }"
            @click="changeFilter('all')"
          >
            全部
          </div>
          <div
            class="filter-tab"
            :class="{ active: activeFilter === 'image' }"
            @click="changeFilter('image')"
          >
            图片
          </div>
          <div
            class="filter-tab"
            :class="{ active: activeFilter === 'post' }"
            @click="changeFilter('post')"
          >
            帖子
          </div>
        </div>
        <button class="refresh-btn" @click="refreshList">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path fill="currentColor" d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
          </svg>
        </button>
      </div>

      <!-- 列表容器 -->
      <div class="history-grid" ref="gridRef">
        <!-- 空状态 -->
        <div class="empty-state" v-if="!loading && filteredList.length === 0">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" width="60" height="60">
              <path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"/>
            </svg>
          </div>
          <p class="empty-text">暂无浏览记录</p>
          <p class="empty-desc">快去浏览内容吧～</p>
        </div>

        <!-- 加载中 -->
        <div class="loading-state" v-if="loading && filteredList.length === 0">
          <div class="loading-spinner"></div>
        </div>

        <!-- 内容列表 -->
        <div
          class="history-card"
          v-for="record in filteredList"
          :key="record.id"
          :class="{ selected: selectedIds.includes(record.id) }"
          @click="handleCardClick(record)"
        >
          <!-- 选择框 -->
          <div class="card-checkbox" @click.stop="toggleSelect(record.id)">
            <div class="checkbox-inner" v-show="selectedIds.includes(record.id)"></div>
          </div>

          <!-- 封面区域 -->
          <div class="card-cover">
            <div class="cover-aspect-box">
              <img
                class="cover-image"
                :src="getRecordCover(record)"
                :alt="record.targetTitle || '浏览内容'"
                @load="handleImageLoad(record)"
                @error="handleImageError(record)"
              />
            </div>
            <!-- 类型标签 -->
            <div class="type-label">{{ record.targetType === 1 ? '图片' : '帖子' }}</div>
            <!-- 时长标签 -->
            <div class="duration-label" v-if="record.viewDuration">
              {{ formatDuration(record.viewDuration) }}
            </div>
          </div>

          <!-- 信息区域 -->
          <div class="card-info">
            <h3 class="content-title" :title="record.targetTitle || '无标题'">
              {{ record.targetTitle || '无标题' }}
            </h3>
            <div class="info-footer">
              <div class="author-info">
                <img
                  class="author-avatar"
                  :src="getAuthorAvatar(record)"
                  alt="作者头像"
                  @error="handleAvatarError($event, record)"
                />
                <span class="author-name">{{ record.targetAuthorUsername || '未知用户' }}</span>
              </div>
              <span class="view-time">{{ formatTime(record.createTime) }}</span>
            </div>
          </div>

          <!-- 单删按钮 -->
          <button class="card-delete-btn" @click.stop="deleteRecord(record.id)">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 加载更多 -->
      <div class="load-more-area" ref="loadMoreRef">
        <div v-if="loading" class="loading-more">
          <div class="loading-spinner small"></div>
        </div>
        <div v-else-if="!hasMore && filteredList.length > 0" class="no-more">已加载全部</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, onUnmounted, computed, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { listMyViewRecordByPageUsingPost, deleteViewRecordUsingPost } from '@/api/viewRecordController';
import { getDefaultAvatar } from '@/utils/userUtils'; // 导入头像工具类
import { formatTime } from '@/utils/dateUtils';
import { getTextCover } from '@/utils/textCoverGenerator';

// 类型定义
interface ViewRecordVO {
  id?: string;
  userId?: string;
  targetId?: string;
  targetType?: number;
  viewDuration?: number;
  createTime?: string;
  targetTitle?: string;
  targetCover?: string;
  targetAuthorUsername?: string;
  targetAuthorAvatar?: string;
  imageLoaded?: boolean;
  imageAspectRatio?: number;
}

// 路由实例
const router = useRouter();

// 状态管理
const loading = ref(false);
const hasMore = ref(true);
const selectedIds = ref<string[]>([]);
const viewRecordList = ref<ViewRecordVO[]>([]);
const activeFilter = ref('all');
const gridRef = ref(null);
const loadMoreRef = ref<HTMLElement | null>(null);
let observer: IntersectionObserver | null = null;

// 文本封面缓存
const textCoverCache = ref({});
const loadingCovers = ref(new Set());

// 搜索参数
const searchParams = reactive({
  current: 1,
  pageSize: 20,
  targetType: null as number | null,
});

// 筛选后的列表
const filteredList = computed(() => {
  if (activeFilter.value === 'all') return viewRecordList.value;
  if (activeFilter.value === 'image') return viewRecordList.value.filter(item => item.targetType === 1);
  if (activeFilter.value === 'post') return viewRecordList.value.filter(item => item.targetType === 2);
  return viewRecordList.value;
});

// 获取浏览记录
const fetchViewRecords = async (loadMore = false) => {
  if (loading.value) return;
  try {
    loading.value = true;
    const response = await listMyViewRecordByPageUsingPost({
      current: searchParams.current,
      pageSize: searchParams.pageSize,
      targetType: searchParams.targetType || undefined,
    });
    if (response.data.code === 0 && response.data) {
      const newRecords = response.data.data.records || [];
      viewRecordList.value = loadMore
        ? [...viewRecordList.value, ...newRecords]
        : newRecords;
      const total = response.data.data.total || 0;
      hasMore.value = viewRecordList.value.length < total;

      nextTick(() => {
        newRecords.forEach(record => {
          if (!record.targetCover && record.targetTitle) {
            generateTextCoverForRecord(record);
          }
        });
      });
    } else {
      if (!loadMore) {
        viewRecordList.value = [];
        hasMore.value = false;
      }
    }
  } catch (error: any) {
    console.error('获取浏览记录失败:', error);
    if (!loadMore) {
      viewRecordList.value = [];
      hasMore.value = false;
    }
  } finally {
    loading.value = false;
  }
};

// 刷新列表
const refreshList = () => {
  searchParams.current = 1;
  selectedIds.value = [];
  fetchViewRecords();
};

// 加载更多
const loadMoreRecords = () => {
  if (!hasMore.value || loading.value) return;
  searchParams.current += 1;
  fetchViewRecords(true);
};

// 删除单条记录
const deleteRecord = async (id: string) => {
  try {
    const response = await deleteViewRecordUsingPost([id.toString()]);
    if (response.data.code === 0) {
      viewRecordList.value = viewRecordList.value.filter(item => item.id !== id);
      selectedIds.value = selectedIds.value.filter(item => item !== id);
    }
  } catch (error: any) {
    console.error('删除失败:', error);
  }
};

// 删除选中记录
const deleteSelected = async () => {
  if (selectedIds.value.length === 0) return;
  try {
    const response = await deleteViewRecordUsingPost(selectedIds.value.map(id => id.toString()));
    if (response.data.code === 0) {
      viewRecordList.value = viewRecordList.value.filter(
        item => !selectedIds.value.includes(item.id!)
      );
      selectedIds.value = [];
    }
  } catch (error: any) {
    console.error('删除失败:', error);
  }
};

// 切换选择状态
const toggleSelect = (id: string) => {
  const index = selectedIds.value.indexOf(id);
  index > -1 ? selectedIds.value.splice(index, 1) : selectedIds.value.push(id);
};

// 切换筛选条件
const changeFilter = (type: string) => {
  activeFilter.value = type;
  if (type === 'image') searchParams.targetType = 1;
  else if (type === 'post') searchParams.targetType = 2;
  else searchParams.targetType = null;
  searchParams.current = 1;
  fetchViewRecords();
};

// 卡片点击跳转
const handleCardClick = (record: ViewRecordVO) => {
  if (!record.targetId) return;
  router.push(record.targetType === 1 ? `/picture-redirect/${record.targetId}` : `/post/${record.targetId}`);
};

// 格式化时长
const formatDuration = (seconds: number) => {
  if (seconds < 60) return `${seconds}s`;
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}m${secs}s`;
};

// 处理图片加载
const handleImageLoad = (record: ViewRecordVO, event?: Event) => {
  record.imageLoaded = true;
  if (event) {
    const img = event.target as HTMLImageElement;
    if (img.naturalWidth && img.naturalHeight) {
      record.imageAspectRatio = img.naturalWidth / img.naturalHeight;
    }
  }
};

// 处理封面图片加载失败
const handleImageError = (record: ViewRecordVO) => {
  record.imageLoaded = true;
  // 使用getDefaultAvatar生成默认封面图
  record.targetCover = getDefaultAvatar(record.targetTitle || '未知内容');

  // 如果有标题，尝试生成文本封面
  if (record.targetTitle && !loadingCovers.value.has(record.id)) {
    generateTextCoverForRecord(record);
  }
};

// 处理作者头像加载失败
const handleAvatarError = (e: Event, record: ViewRecordVO) => {
  const target = e.target as HTMLImageElement;
  // 使用getDefaultAvatar生成默认头像，基于用户名
  target.src = getDefaultAvatar(record.targetAuthorUsername || '未知用户');
};

// 获取作者头像（使用getDefaultAvatar工具类）
const getAuthorAvatar = (record: ViewRecordVO) => {
  if (record.targetAuthorAvatar) {
    return record.targetAuthorAvatar;
  }
  // 使用用户名生成默认头像
  return getDefaultAvatar(record.targetAuthorUsername || '未知用户');
};

// 生成文本封面
const generateTextCoverForRecord = async (record: ViewRecordVO) => {
  if (!record.targetTitle || loadingCovers.value.has(record.id)) return;

  const cacheKey = `text_cover_${record.id}_${record.targetTitle}`;
  if (textCoverCache.value[cacheKey]) {
    record.targetCover = textCoverCache.value[cacheKey];
    return;
  }

  try {
    loadingCovers.value.add(record.id);
    const cover = await getTextCover(record.targetTitle, 300, 400);
    textCoverCache.value[cacheKey] = cover;
    record.targetCover = cover;

    nextTick(() => {
      const imgs = document.querySelectorAll(`img[alt="${record.targetTitle}"]`);
      imgs.forEach(img => {
        if ((img as HTMLImageElement).src !== cover) {
          (img as HTMLImageElement).src = cover;
        }
      });
    });
  } catch (error) {
    console.error('生成文本封面失败:', error);
    // 生成失败时使用getDefaultAvatar作为兜底
    record.targetCover = getDefaultAvatar(record.targetTitle || '未知内容');
  } finally {
    loadingCovers.value.delete(record.id);
  }
};

// 获取记录封面（使用getDefaultAvatar工具类）
const getRecordCover = (record: ViewRecordVO) => {
  if (record.targetCover) return record.targetCover;

  // 如果有标题，先尝试从缓存获取文本封面
  if (record.targetTitle) {
    const cacheKey = `text_cover_${record.id}_${record.targetTitle}`;
    if (textCoverCache.value[cacheKey]) {
      return textCoverCache.value[cacheKey];
    }
    // 异步生成文本封面
    generateTextCoverForRecord(record);
  }

  // 兜底使用getDefaultAvatar生成默认封面
  return getDefaultAvatar(record.targetTitle || '未知内容');
};

// 初始化滚动监听 (改为使用 IntersectionObserver)
const setupIntersectionObserver = () => {
  if (observer) observer.disconnect();

  observer = new IntersectionObserver((entries) => {
    const entry = entries[0];
    if (entry.isIntersecting && hasMore.value && !loading.value) {
      loadMoreRecords();
    }
  }, {
    rootMargin: '100px', // 提前 100px 触发
    threshold: 0.1
  });

  if (loadMoreRef.value) {
    observer.observe(loadMoreRef.value);
  }
};

// 初始化
onMounted(() => {
  fetchViewRecords();
  setupIntersectionObserver();

  watch(() => viewRecordList.value, (newList) => {
    newList.forEach(record => {
      if (!record.targetCover && record.targetTitle) {
        generateTextCoverForRecord(record);
      }
    });
  }, { deep: true, immediate: true });
});

// 卸载监听
onUnmounted(() => {
  if (observer) {
    observer.disconnect();
    observer = null;
  }
});
</script>

<style scoped>
.browse-history-page {
  width: 100%;
  background-color: var(--background);
  color: var(--text-primary);
  transition: var(--theme-transition);
  overflow-x: hidden;
}

.page-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 6px;
}

/* 顶部导航 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 8px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 16px;
}

.header-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.delete-selected-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 20px;
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.delete-selected-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.delete-selected-btn:not(.disabled):hover {
  background-color: #ef4444;
  color: white;
}

/* 筛选栏 */
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 8px;
}

.filter-tabs {
  display: flex;
  gap: 24px;
}

.filter-tab {
  font-size: 15px;
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
  padding-bottom: 4px;
  transition: color 0.2s ease;
}

.filter-tab.active {
  color: var(--link-color);
  font-weight: 500;
}

.filter-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background-color: var(--link-color);
  border-radius: 1px;
}

.filter-tab:hover {
  color: var(--link-color);
}

.refresh-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background-color: var(--card-background);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.refresh-btn:hover {
  background-color: var(--hover-background);
}

/* 网格布局 */
.history-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

@media (max-width: 1200px) {
  .history-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .history-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .filter-tabs {
    gap: 16px;
  }

  .header-title {
    font-size: 18px;
  }
}

/* 空状态 */
.empty-state {
  grid-column: 1 / -1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--text-secondary);
}

.empty-icon {
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 18px;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
  opacity: 0.8;
}

/* 加载中 */
.loading-state {
  grid-column: 1 / -1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 80px 0;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border-color);
  border-top-color: var(--link-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
  border-width: 2px;
}

/* 历史卡片 */
.history-card {
  background-color: var(--card-background);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: all 0.2s ease;
  box-shadow: 0 1px 2px var(--shadow-color);
}

.history-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px var(--shadow-color);
}

.history-card.selected {
  border: 2px solid var(--link-color);
}

/* 选择框 */
.card-checkbox {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.8);
  background-color: rgba(0,0,0,0.3);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.checkbox-inner {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: var(--link-color);
}

/* 封面区域 */
.card-cover {
  position: relative;
  width: 100%;
}

.cover-aspect-box {
  width: 100%;
  padding-top: 133.33%;
  position: relative;
  overflow: hidden;
}

.cover-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.history-card:hover .cover-image {
  transform: scale(1.03);
}

.type-label {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 6px;
  background-color: rgba(0,0,0,0.5);
  color: white;
  font-size: 11px;
  border-radius: 4px;
}

.duration-label {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 2px 6px;
  background-color: rgba(0,0,0,0.5);
  color: white;
  font-size: 11px;
  border-radius: 4px;
}

/* 信息区域 */
.card-info {
  padding: 12px;
}

.content-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.info-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.author-avatar {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  object-fit: cover;
}

.author-name {
  max-width: 80px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.view-time {
  font-size: 11px;
  opacity: 0.8;
}

/* 删除按钮 */
.card-delete-btn {
  position: absolute;
  bottom: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background-color: rgba(239, 68, 68, 0.8);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s ease;
  z-index: 10;
}

.history-card:hover .card-delete-btn {
  opacity: 1;
}

/* 加载更多 */
.load-more-area {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-more {
  display: flex;
  align-items: center;
  gap: 8px;
}

.no-more {
  opacity: 0.7;
}

/* 动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 暗色主题适配 */
.dark-theme .history-card {
  background-color: var(--card-background);
  box-shadow: 0 1px 2px var(--shadow-color);
}

.dark-theme .card-checkbox {
  border-color: rgba(255,255,255,0.5);
  background-color: rgba(0,0,0,0.5);
}
</style>
