<template>
  <div class="post-draft-list">
    <!-- 草稿列表 -->
    <div v-if="drafts && drafts.length > 0" class="drafts-container">
      <div
        v-for="draft in drafts"
        :key="draft.id"
        class="draft-item"
        @click="editDraft(draft)"
      >
        <div class="draft-header">
          <div class="draft-title">{{ draft.title || '无标题草稿' }}</div>
          <div class="draft-actions">
            <a-button
              size="small"
              type="link"
              danger
              @click.stop="deleteDraft(draft.id)"
              class="action-btn"
            >
              <template #icon>
                <i class="fas fa-trash"></i>
              </template>
              删除
            </a-button>
          </div>
        </div>
        <div class="draft-content" v-if="draft.content">
          <HtmlContent :content="draft.content.substring(0, 100) + (draft.content.length > 100 ? '...' : '')" />
        </div>
        <div class="draft-meta">
          <span class="draft-time">{{ formatDate(draft.createTime || draft.updateTime) }}</span>
          <span class="draft-tags" v-if="draft.tags && draft.tags.trim()">
            <span v-for="tag in parseTags(draft.tags)" :key="tag" class="draft-tag">{{ tag }}</span>
          </span>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <div class="custom-empty-state">
        <img :src="emptyImage" alt="暂无草稿" class="empty-illustration" />
        <div class="empty-text">暂无草稿</div>
        <a-button type="primary" @click="goToPostEdit" class="publish-btn">
          <template #icon>
            <i class="fas fa-edit"></i>
          </template>
          去发布
        </a-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && drafts.length === 0" class="loading-state">
      <a-spin />
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" class="load-more">
      <a-button @click="loadMore" :loading="loadingMore">
        {{ loadingMore ? '加载中...' : '加载更多' }}
      </a-button>
    </div>

    <!-- 底部提示 -->
    <div v-if="!hasMore && drafts.length > 0" class="no-more">
      已加载全部草稿
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import {
  listPostDraftsUsingGet,
  deletePostDraftUsingDelete,
  getPostLatestDraftUsingGet
} from '@/api/postController';
import HtmlContent from './HtmlContent.vue';
import emptyImage from '@/assets/illustrations/empty.png';

interface Draft {
  id: number;
  title?: string;
  content?: string;
  tags?: string;
  coverUrl?: string;
  createTime?: string;
  updateTime?: string;
  [key: string]: any;
}

const emit = defineEmits(['create-new', 'reload']);
const router = useRouter();

const drafts = ref<Draft[]>([]);
const loading = ref(false);
const loadingMore = ref(false);
const hasMore = ref(true);
const currentPage = ref(0);
const pageSize = ref(10);

// 获取草稿列表
const fetchDrafts = async (reset = false) => {
  if (reset) {
    currentPage.value = 0;
    drafts.value = [];
  }

  loading.value = true;
  try {

    const res = await listPostDraftsUsingGet();
    if (res.data.code === 0) {
      const newDrafts = (res.data.data || []) as Draft[];
      if (reset) {
        drafts.value = newDrafts;
      } else {
        drafts.value = [...drafts.value, ...newDrafts];
      }

      // 假设少于pageSize条数据表示没有更多数据
      hasMore.value = newDrafts.length >= pageSize.value;
    } else {
      message.error(res.data.message || '获取草稿列表失败');
    }
  } catch (error: any) {
    console.error('获取草稿列表失败:', error);
    message.error(error.message || '获取草稿列表失败');
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

// 加载更多
const loadMore = async () => {
  loadingMore.value = true;
  currentPage.value += 1;
  await fetchDrafts(false);
};

// 删除草稿
const deleteDraft = async (draftId: number) => {
  try {
    const res = await deletePostDraftUsingDelete({ draftId });
    if (res.data.code === 0) {
      message.success('删除成功');
      // 从列表中移除该草稿
      drafts.value = drafts.value.filter(draft => draft.id !== draftId);
    } else {
      message.error(res.data.message || '删除失败');
    }
  } catch (error: any) {
    console.error('删除草稿失败:', error);
    message.error(error.message || '删除失败');
  }
};



// 编辑草稿
const editDraft = (draft: Draft) => {
  console.log('点击草稿:', draft); // 调试日志
  if (!draft.id) {
    console.error('草稿ID不存在:', draft);
    message.error('草稿数据异常，无法编辑');
    return;
  }
  // 跳转到帖子编辑页面并传递草稿数据
  router.push({
    path: '/post/edit/' + draft.id,
    query: { draft: JSON.stringify(draft) }
  }).then(() => {
    console.log('路由跳转成功');
  }).catch(err => {
    console.error('路由跳转失败:', err);
    message.error('页面跳转失败');
  });
};

// 跳转到发布帖子页面
const goToPostEdit = () => {
  router.push('/post/edit');
};

// 格式化时间
const formatDate = (dateString?: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString('zh-CN');
};

// 解析标签
const parseTags = (tags: string) => {
  if (!tags) return [];

  // 检查是否为JSON数组格式
  if (tags.startsWith('[') && tags.endsWith(']')) {
    try {
      const parsed = JSON.parse(tags);
      if (Array.isArray(parsed)) {
        return parsed.filter(tag => tag && tag.length > 0);
      }
    } catch (e) {
      console.error('标签解析失败:', e);
      // 如果JSON解析失败，尝试按普通字符串处理
    }
  }

  // 如果不是JSON格式或解析失败，则按原来的逗号分隔处理
  return tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
};

// 刷新数据
const refresh = async () => {
  await fetchDrafts(true);
};

// 暴露方法给父组件
defineExpose({
  refresh,
  fetchDrafts
});

onMounted(() => {
  fetchDrafts(true);
});
</script>

<style scoped>
.post-draft-list {
  padding: 4px;
  background: var(--bg-color);
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.drafts-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.draft-item {
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 8px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  position: relative;
  overflow: hidden;
}

.draft-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #1890ff, #52c41a, #fadb14);
  transform: scaleX(0);
  transition: transform 0.3s ease;
}

.draft-item:hover::before {
  transform: scaleX(1);
}

.draft-item:hover {
  border-color: #1890ff;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.draft-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}

.draft-title {
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  margin-right: 12px;
  word-break: break-all;
  font-size: 16px;
}

.draft-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  padding: 2px 6px !important;
  border-radius: 4px;
}

.draft-content {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 12px;
  word-break: break-all;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border-color);
  max-height: 120px;
  overflow: hidden;
  position: relative;
}

.draft-content::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 30px;
  background: linear-gradient(to bottom, transparent, var(--card-background));
  pointer-events: none;
  display: none;
}

.draft-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--text-secondary);
  flex-wrap: wrap;
  gap: 8px;
}

.draft-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.draft-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border: 1px solid #d9e2ec;
  border-radius: 16px;
  font-size: 12px;
  color: #4a6572;
  transition: all 0.3s ease;
}

.draft-tag:hover {
  background: linear-gradient(135deg, #e2e7f0 0%, #a3b6cc 100%);
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  text-align: center;
}

.custom-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-illustration {
  width: 100%;
  max-width: 200px;
  height: auto;
  opacity: 0.8;
  filter: drop-shadow(0 10px 20px rgba(0, 0, 0, 0.05));
  margin-bottom: 8px;
}

.empty-text {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
}

.publish-btn {
  border-radius: 20px;
  height: 36px;
  padding: 0 20px;
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.load-more {
  text-align: center;
  margin-top: 24px;
}

.no-more {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  margin: 24px 0;
  padding: 12px;
  border-top: 1px solid var(--border-color);
}
</style>
