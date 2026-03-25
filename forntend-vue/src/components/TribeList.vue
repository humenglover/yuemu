<template>
  <div class="tribe-list-container" ref="containerRef" @scroll="handleScroll">
    <!-- 公共部落 -->
    <div class="public-spaces-header">
      <h3 class="public-spaces-title">所有部落</h3>
    </div>
    <div class="public-spaces-list">
      <TeamSpaceCard
        v-for="space in publicSpaces"
        :key="`public-${space.id}`"
        :space="space as API.SpaceVO"
      />

      <!-- 加载中占位 -->
      <div v-if="loadingPublic && !isFirstLoad" class="loading-placeholder">
        <div class="loading-item" v-for="i in 3" :key="`loading-${i}`">
          <div class="skeleton-card">
            <div class="skeleton-cover"></div>
            <div class="skeleton-content">
              <div class="skeleton-title"></div>
              <div class="skeleton-desc"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 初始加载状态 -->
      <div v-if="loadingPublic && isFirstLoad" class="loading-state">
        <div class="loading-item" v-for="i in 3" :key="`loading-${i}`">
          <div class="skeleton-card">
            <div class="skeleton-cover"></div>
            <div class="skeleton-content">
              <div class="skeleton-title"></div>
              <div class="skeleton-desc"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 到底提示 -->
      <div v-if="isPublicSpacesLoaded && !publicHasMore && publicSpaces.length > 0" class="list-bottom-tip">
        <div class="tip-line">
          <span class="line"></span>
          <span class="text">已经到底啦</span>
          <span class="line"></span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="isPublicSpacesLoaded && publicSpaces.length === 0 && !loadingPublic" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" class="empty-svg">
          <path fill="#2563eb" d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z" />
        </svg>
      </div>
      <h3>暂无公共部落</h3>
      <p>暂时没有公开的团队部落</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import TeamSpaceCard from '@/components/TeamSpaceCard.vue';
import { listSpaceVoByPageUsingPost, listRecommendedSpacesUsingGet } from '@/api/spaceController';
import { throttle } from 'lodash-es';

// 推荐部落相关数据
const recommendedSpaces = ref<API.SpaceVO[]>([]);
const loadingRecommended = ref(false);

// 公共部落相关数据
const publicSpaces = ref<API.SpaceVO[]>([]);
const loadingPublic = ref(false);
const currentPage = ref(1); // 当前加载的页数（内部使用）
const publicHasMore = ref(true); // 是否还有更多数据
const isPublicSpacesLoaded = ref(false); // 首次加载是否完成
const isFirstLoad = ref(true); // 是否是首次加载
const containerRef = ref<HTMLDivElement | null>(null); // 容器ref
const loadMoreThreshold = ref(200); // 距离底部多少像素触发加载更多

// 防抖处理滚动事件
const handleScrollThrottled = throttle(handleScroll, 200);

// 获取推荐部落
const getRecommendedSpaces = async () => {
  if (loadingRecommended.value) return;

  try {
    loadingRecommended.value = true;
    const res = await listRecommendedSpacesUsingGet();
    if (res.data.code === 0) {
      recommendedSpaces.value = res.data.data || [];
    }
  } catch (error) {
    console.error('获取推荐部落失败:', error);
  } finally {
    loadingRecommended.value = false;
  }
};

// 获取公共部落
const getPublicSpaces = async (isRefresh = false) => {
  // 如果正在加载或没有更多数据，直接返回
  if (loadingPublic.value || (!isRefresh && !publicHasMore.value)) return;

  try {
    loadingPublic.value = true;

    // 刷新时重置页码和数据
    if (isRefresh) {
      currentPage.value = 1;
      publicSpaces.value = [];
      publicHasMore.value = true;
      isFirstLoad.value = true;
    }

    const res = await listSpaceVoByPageUsingPost({
      current: currentPage.value,
      pageSize: 10, // 每页获取10个部落
      spaceType: 1, // 只获取团队部落（非私有部落）
    });

    if (res.data.code === 0 && res.data.data) {
      const { records, total, hasMore } = res.data.data;
      const newRecords = records || [];

      // 追加新数据
      if (isRefresh) {
        publicSpaces.value = newRecords;
      } else {
        publicSpaces.value = [...publicSpaces.value, ...newRecords];
      }

      // 更新是否有更多数据
      const totalPages = total ? Math.ceil(total / 10) : 0;
      publicHasMore.value = currentPage.value < totalPages;

      // 如果接口返回了hasMore，优先使用接口返回的值
      if (hasMore !== undefined) {
        publicHasMore.value = hasMore;
      }

      // 标记首次加载完成
      if (isRefresh) {
        isPublicSpacesLoaded.value = true;
        isFirstLoad.value = false;
      }
    } else {
      // 请求成功但没有数据
      if (isRefresh) {
        publicSpaces.value = [];
        isPublicSpacesLoaded.value = true;
        isFirstLoad.value = false;
      }
      publicHasMore.value = false;
    }
  } catch (error) {
    console.error('获取公共部落失败:', error);
    if (isRefresh) {
      publicSpaces.value = [];
      isPublicSpacesLoaded.value = true;
      isFirstLoad.value = false;
    }
    publicHasMore.value = false;
  } finally {
    loadingPublic.value = false;
  }
};

// 加载更多公共部落
const loadMorePublicSpaces = async () => {
  if (loadingPublic.value || !publicHasMore.value) return;

  currentPage.value++;
  await getPublicSpaces(false);
};

// 滚动事件处理
function handleScroll() {
  if (!containerRef.value) return;

  const container = containerRef.value;
  // 计算滚动位置
  const { scrollTop, scrollHeight, clientHeight } = container;
  const isReachBottom = scrollTop + clientHeight + loadMoreThreshold.value >= scrollHeight;

  // 到达底部且有更多数据，触发加载更多
  if (isReachBottom && !loadingPublic.value && publicHasMore.value && isPublicSpacesLoaded.value) {
    loadMorePublicSpaces();
  }
}

// 刷新数据
const refresh = async () => {
  await getPublicSpaces(true);
};

// 初始化事件监听
const initScrollListener = () => {
  if (containerRef.value) {
    containerRef.value.addEventListener('scroll', handleScrollThrottled);
  }
};

// 移除事件监听
const removeScrollListener = () => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('scroll', handleScrollThrottled);
  }
  handleScrollThrottled.cancel();
};

// 初始化数据
const init = async () => {
  await Promise.all([
    getRecommendedSpaces(),
    getPublicSpaces(true)
  ]);

  // 初始化滚动监听
  nextTick(() => {
    initScrollListener();
  });
};

// 暴露方法
defineExpose({
  init,
  refresh,
  loadMorePublicSpaces
});

onMounted(() => {
  init();
});

onUnmounted(() => {
  // 清理滚动监听
  removeScrollListener();
});
</script>

<style scoped>
.tribe-list-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: var(--card-background);
  /* 确保滚动容器正常工作 */
  position: relative;
}

.recommended-header,
.public-spaces-header {
  margin: 16px 0 8px;
}

.recommended-title,
.public-spaces-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.recommended-spaces-list,
.public-spaces-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
  min-height: calc(100% - 40px);
}

/* 初始加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 0;
  grid-column: 1 / -1; /* 占满整行 */
}

/* 加载更多时的占位 */
.loading-placeholder {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 0;
  grid-column: 1 / -1; /* 占满整行 */
}

.loading-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--card-background);
}

.skeleton-card {
  display: flex;
  gap: 12px;
  width: 100%;
}

.skeleton-cover {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skeleton-title {
  width: 60%;
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

.skeleton-desc {
  width: 100%;
  height: 12px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s infinite;
}

@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  color: var(--text-secondary);
  grid-column: 1 / -1; /* 占满整行 */
}

.empty-state .empty-icon {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.empty-state .empty-svg {
  width: 100%;
  height: 100%;
}

.empty-state h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: var(--text-primary);
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.list-bottom-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 20px 0;
  color: var(--text-secondary);
  grid-column: 1 / -1; /* 占满整行 */
}

.tip-line {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.line {
  flex: 1;
  height: 1px;
  background: var(--border-color);
}

.text {
  white-space: nowrap;
  padding: 0 12px;
  font-size: 14px;
}

/* 移除加载更多按钮样式 */
.load-more-container,
.load-more-btn {
  display: none;
}
</style>
