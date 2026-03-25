<template>
  <div class="recommended-spaces-container" @scroll="handleScroll">
    <div class="recommended-header">
      <h3 class="recommended-title">推荐部落</h3>
    </div>
    <div v-if="recommendedSpaces.length > 0" class="recommended-spaces-list">
      <TeamSpaceCard
        v-for="space in recommendedSpaces"
        :key="space.id"
        :space="space as API.SpaceVO"
      />
      <!-- 加载更多提示 -->
      <div v-if="loadingMore" class="loading-more">
        <div class="loading-spinner"></div>
        <span>加载中...</span>
      </div>
      <div v-else-if="noMore" class="no-more">
        <span>已经到底啦</span>
      </div>
    </div>
    <div v-else-if="loadingRecommended" class="loading-state">
      <div class="loading-item" v-for="i in 3" :key="i">
        <div class="skeleton-card">
          <div class="skeleton-cover"></div>
          <div class="skeleton-content">
            <div class="skeleton-title"></div>
            <div class="skeleton-desc"></div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" class="empty-svg">
          <path fill="#2563eb" d="M20,8L12,13L4,8V6L12,11L20,6M20,4H4C2.89,4 2,4.89 2,6V18A2,2 0 0,0 4,20H20A2,2 0 0,0 22,18V6C22,4.89 21.1,4 20,4Z" />
        </svg>
      </div>
      <h3>暂无推荐部落</h3>
      <p>暂时没有为您推荐的部落，稍后再来看看吧</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import TeamSpaceCard from '@/components/TeamSpaceCard.vue';
import { listRecommendedSpacesUsingGet } from '@/api/spaceController';

// 推荐部落相关数据
const recommendedSpaces = ref<API.SpaceVO[]>([]);
const loadingRecommended = ref(false);

// 触底加载相关数据
const loadingMore = ref(false);
const hasMore = ref(false);
const noMore = ref(true);

// 获取推荐部落
const getRecommendedSpaces = async (isLoadMore = false) => {
  if (isLoadMore && (!hasMore.value || loadingMore.value)) return;
  if (!isLoadMore && loadingRecommended.value) return;

  try {
    if (!isLoadMore) {
      loadingRecommended.value = true;
    }

    const res = await listRecommendedSpacesUsingGet();

    if (res.data.code === 0 && res.data.data) {
      const spaces = res.data.data;

      if (!isLoadMore) {
        recommendedSpaces.value = spaces || [];
      }

      // 推荐部落是完整列表，不支持分页，所以设置为没有更多数据
      hasMore.value = false;
      noMore.value = true;
    }
  } catch (error) {
    console.error('获取推荐部落失败:', error);
  } finally {
    loadingRecommended.value = false;
    loadingMore.value = false;
  }
};

// 初始化数据
const init = async () => {
  await getRecommendedSpaces();
};

// 处理滚动事件，实现触底加载
const handleScroll = (event: Event) => {
  // 推荐部落是完整列表，不支持分页加载，所以不需要触底加载逻辑
};

// 暴露初始化方法
defineExpose({
  init,
  refresh: init
});

onMounted(() => {
  init();
});
</script>

<style scoped>
.recommended-spaces-container {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
  background: var(--card-background);
}

.recommended-header {
  margin: 16px 0 8px;
}

.recommended-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.recommended-spaces-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 0;
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

.loading-more, .no-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  color: var(--text-secondary);
  font-size: 14px;
}

.loading-more {
  gap: 8px;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--border-color);
  border-top: 2px solid var(--primary-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>
