<template>
  <div class="space-activity-manage-page">

    <!-- 内容区域 -->
    <div class="content">
      <!-- 操作按钮 -->
      <div class="operations">
        <div class="add-btn" @click="showAddActivityModal">添加活动</div>
        <div class="refresh-btn" @click="loadActivities">
          <i class="fas fa-sync-alt refresh-icon"></i> 刷新
        </div>
      </div>

      <!-- 加载状态 -->
      <div class="loading-state" v-if="loading && activities.length === 0">
        <i class="fas fa-spinner fa-spin loading-spinner"></i>
        <div class="loading-text">加载中...</div>
      </div>

      <!-- 空状态 -->
      <div class="empty-state" v-if="!loading && activities.length === 0">
        <i class="fas fa-file-alt empty-icon"></i>
        <div class="empty-text">暂无活动数据</div>
      </div>

      <!-- 活动列表 -->
      <div class="activity-list" v-if="activities.length > 0">
        <div class="activity-card" v-for="item in activities" :key="item.id" @click="handleViewActivity(item.id)">
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="activity-title">{{ item.title }}</div>
            <div class="tag-group">
              <div class="tag orange" v-if="item.status === 0">待审核</div>
              <div class="tag green" v-else-if="item.status === 1">已通过</div>
              <div class="tag red" v-else-if="item.status === 2">已拒绝</div>
              <div class="tag gray" v-if="item.isExpired">已过期</div>
            </div>
          </div>

          <!-- 卡片元信息 -->
          <div class="card-meta">
            <span>{{ formatDate(item.createTime) }}</span>
            <span class="separator">|</span>
            <span>{{ item.user?.userName || '未知用户' }}</span>
          </div>

          <!-- 卡片内容 -->
          <div class="card-content">
            <div class="activity-image" v-if="item.coverUrl">
              <img :src="item.coverUrl" alt="活动封面" />
            </div>
            <div class="activity-text">
              <p class="activity-summary">{{ item.content ? item.content.substring(0, 100) + '...' : '' }}</p>
              <div class="activity-stats">
                <span class="stat-item"><i class="fas fa-eye view-icon"></i> {{ item.viewCount }}</span>
                <span class="stat-item"><i class="fas fa-share-alt share-icon"></i> {{ item.shareCount }}</span>
              </div>
              <div class="activity-time">
                <span><strong>开始:</strong> {{ formatDate(item.createTime) }}</span>
                <span class="separator">|</span>
                <span><strong>过期:</strong> {{ formatDate(item.expireTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="card-actions">
            <div class="action-btn view-btn" @click.stop="handleViewActivity(item.id)">
              <i class="fas fa-eye view-btn-icon"></i> 查看
            </div>
            <div class="action-btn delete-btn" @click.stop="handleDeleteActivity(item.id)">
              <i class="fas fa-trash-alt delete-btn-icon"></i> 删除
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多 -->
      <div class="load-more" v-if="!loading && hasMore">
        <i class="fas fa-spinner fa-spin loading-spinner small"></i>
        <div class="loading-text">加载更多...</div>
      </div>

      <!-- 没有更多 -->
      <div class="no-more" v-if="!loading && !hasMore && activities.length > 0">
        <div class="no-more-text">没有更多数据了</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, reactive, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  listActivityBySpaceIdUsingPost,
  deleteActivityUsingPost,
} from '@/api/activityController';
import type { Activity, ActivityQueryRequest } from '@/api/types';

const route = useRoute();
const router = useRouter();

const spaceId = ref<string>(route.params.spaceId as string);
const spaceName = ref<string>('');

const activities = ref<Activity[]>([]);
const loading = ref<boolean>(false);
const hasMore = ref<boolean>(true);
const page = ref<String>(1);
const pageSize = ref<String>(10);

const loadActivities = async (isLoadMore = false) => {
  if (loading.value) return;

  loading.value = true;
  try {
    const queryRequest: ActivityQueryRequest = {
      current: isLoadMore ? page.value + 1 : 1,
      pageSize: pageSize.value,
      spaceId: String(spaceId.value)
    };

    const res = await listActivityBySpaceIdUsingPost(queryRequest);
    if (res.data.code === 0) {
      const newData = res.data.data?.records ?? [];

      if (isLoadMore) {
        activities.value = [...activities.value, ...newData];
        page.value += 1;
      } else {
        activities.value = newData;
        page.value = 1;
      }

      const total = String(res.data.data.total || 0);
      hasMore.value = activities.value.length < total;

      if (activities.value.length > 0 && activities.value[0].space) {
        spaceName.value = activities.value[0].space?.spaceName || '未知空间';
      }
    } else {
      message.error(res.message || '获取活动列表失败');
    }
  } catch (error) {
    console.error('获取活动列表失败:', error);
    message.error('获取活动列表失败');
  } finally {
    loading.value = false;
  }
};

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
};

const showAddActivityModal = () => {
  router.push(`/activity/edit?spaceId=${spaceId.value}`);
};

const handleViewActivity = (id: string) => {
  router.push(`/activity/detail/${id}`);
};

const handleDeleteActivity = async (id: string) => {
  // 使用原生div实现确认弹框
  const modal = document.createElement('div');
  modal.innerHTML = `
    <div class="delete-confirm-overlay" style="position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 10000;">
      <div class="delete-confirm-modal" style="background: white; padding: 24px; border-radius: 8px; width: 80%; max-width: 400px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);">
        <div class="confirm-content" style="margin-bottom: 20px; font-size: 16px; color: #333;">确定要删除该活动吗？</div>
        <div class="confirm-actions" style="display: flex; justify-content: flex-end; gap: 12px;">
          <button class="cancel-btn" style="padding: 8px 16px; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; cursor: pointer;">取消</button>
          <button class="confirm-btn" style="padding: 8px 16px; border: none; border-radius: 4px; background: #ff4d4f; color: white; cursor: pointer;">删除</button>
        </div>
      </div>
    </div>
  `;

  document.body.appendChild(modal);

  const cancelBtn = modal.querySelector('.cancel-btn');
  const confirmBtn = modal.querySelector('.confirm-btn');

  const closeModal = () => {
    document.body.removeChild(modal);
  };

  cancelBtn?.addEventListener('click', closeModal);

  confirmBtn?.addEventListener('click', async () => {
    try {
      const res = await deleteActivityUsingPost({ id: String(id) });
      if (res.data.code === 0) {
        message.success('删除活动成功');
        loadActivities();
      } else {
        message.error(res.data.message || '删除活动失败');
      }
    } catch (error) {
      console.error('删除活动失败:', error);
      message.error('删除活动失败');
    } finally {
      closeModal();
    }
  });
};

const handleScroll = () => {
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
  const scrollHeight = document.documentElement.scrollHeight;
  const clientHeight = document.documentElement.clientHeight;

  if (scrollHeight - scrollTop - clientHeight < 200 && !loading.value && hasMore.value) {
    loadActivities(true);
  }
};

onMounted(() => {
  loadActivities();
  window.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>


.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--header-background);
  color: var(--text-primary);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
}

.page-title {
  font-size: 17px;
  font-weight: 500;
  color: #333;
}

.empty {
  width: 36px;
}

.content {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

.operations {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.add-btn {
  background-color: #0088ff;
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.refresh-btn {
  background-color: #f5f5f5;
  color: #333;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0f0f0;
  border-top-color: #0088ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
  border-width: 2px;
}

.loading-text {
  color: #999;
  font-size: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  color: #999;
  font-size: 16px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.activity-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.activity-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  flex: 1;
}

.tag-group {
  display: flex;
  gap: 6px;
}

.tag {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.tag.orange {
  background-color: #ff9500;
}

.tag.green {
  background-color: #00c800;
}

.tag.red {
  background-color: #ff3b30;
}

.tag.gray {
  background-color: #999;
}

.card-meta {
  font-size: 12px;
  color: #999;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.separator {
  color: #e0e0e0;
}

.card-content {
  display: flex;
  gap: 12px;
}

.activity-image {
  width: 100px;
  height: 70px;
  flex-shrink: 0;
}

.activity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.activity-text {
  flex: 1;
}

.activity-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.activity-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.activity-time {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-actions {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.action-btn {
  font-size: 14px;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.view-btn {
  color: #0088ff;
  background-color: #f0f8ff;
}

.delete-btn {
  color: #ff3b30;
  background-color: #fff0f0;
}

.load-more {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  gap: 8px;
}

.no-more {
  text-align: center;
  padding: 20px 0;
  color: #999;
  font-size: 14px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .content {
    padding: 12px;
  }

  .activity-card {
    padding: 12px;
  }

  .card-content {
    flex-direction: column;
  }

  .activity-image {
    width: 100%;
    height: 150px;
  }
}
</style>
