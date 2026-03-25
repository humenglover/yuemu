<template>
  <div id="spaceAnalyzePage">
    <!-- 头部导航 -->
    <div class="page-header">
      <div class="header-title">
        <a-button type="link" class="title-text">
          <i class="fas fa-chart-pie title-icon"></i> 图库分析
        </a-button>
        <span class="divider">-</span>
        <span class="space-name" v-if="queryAll">
          <i class="fas fa-layer-group space-type-icon"></i> 全部空间
        </span>
        <span class="space-name" v-else-if="queryPublic">
          <i class="fas fa-globe space-type-icon"></i> 公共图库
        </span>
        <a class="space-link" v-else :href="`/space/${spaceId}`">
          <i class="fas fa-folder-open space-type-icon"></i> {{ spaceId }}
        </a>
      </div>
    </div>

    <!-- 分析卡片网格 -->
    <a-row :gutter="[16, 16]" class="analysis-grid">
      <!-- 空间使用分析 -->
      <a-col :xs="24" :md="12" class="analysis-col">
        <div class="analysis-card">
          <div class="card-header-icon">
            <i class="fas fa-database"></i>
          </div>
          <SpaceUsageAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </div>
      </a-col>

      <!-- 空间分类分析 -->
      <a-col :xs="24" :md="12" class="analysis-col">
        <div class="analysis-card">
          <div class="card-header-icon">
            <i class="fas fa-tags"></i>
          </div>
          <SpaceCategoryAnalyze
            :spaceId="spaceId"
            :queryAll="queryAll"
            :queryPublic="queryPublic"
          />
        </div>
      </a-col>

      <!-- 标签分析 -->
      <a-col :xs="24" :md="12" class="analysis-col">
        <div class="analysis-card">
          <div class="card-header-icon">
            <i class="fas fa-tag"></i>
          </div>
          <SpaceTagAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </div>
      </a-col>

      <!-- 图片大小分段分析 -->
      <a-col :xs="24" :md="12" class="analysis-col">
        <div class="analysis-card">
          <div class="card-header-icon">
            <i class="fas fa-file-size"></i>
          </div>
          <SpaceSizeAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </div>
      </a-col>

      <!-- 用户上传行为分析 -->
      <a-col :xs="24" :md="12" class="analysis-col">
        <div class="analysis-card">
          <div class="card-header-icon">
            <i class="fas fa-users"></i>
          </div>
          <SpaceUserAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </div>
      </a-col>

      <!-- 空间使用排行分析 -->
      <a-col :xs="24" :md="12" class="analysis-col" v-if="isAdmin">
        <div class="analysis-card">
          <div class="card-header-icon">
            <i class="fas fa-ranking-star"></i>
          </div>
          <SpaceRankAnalyze :spaceId="spaceId" :queryAll="queryAll" :queryPublic="queryPublic" />
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import SpaceUsageAnalyze from '@/components/analyze/SpaceUsageAnalyze.vue'
import SpaceCategoryAnalyze from '@/components/analyze/SpaceCategoryAnalyze.vue'
import SpaceTagAnalyze from '@/components/analyze/SpaceTagAnalyze.vue'
import SpaceSizeAnalyze from '@/components/analyze/SpaceSizeAnalyze.vue'
import SpaceUserAnalyze from '@/components/analyze/SpaceUserAnalyze.vue'
import SpaceRankAnalyze from '@/components/analyze/SpaceRankAnalyze.vue'
import { useRoute } from 'vue-router'
import { computed } from 'vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const route = useRoute()

// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId as string
})

// 是否查询所有空间
const queryAll = computed(() => {
  return !!route.query?.queryAll
})

// 是否查询公共空间
const queryPublic = computed(() => {
  return !!route.query?.queryPublic
})

// 判断用户是否为管理员
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser
const isAdmin = computed(() => {
  return loginUser.userRole === 'admin'
})
</script>

<style scoped>
#spaceAnalyzePage {
  margin-bottom: 16px;
  padding: 0 4px;
  background: var(--header-background);
  color: var(--text-primary);
}

.page-header {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 16px;
  padding: 16px 24px;
  margin: -15px -16px 24px;
  box-shadow: 0 2px 8px rgba(255, 142, 83, 0.1);
  backdrop-filter: blur(10px);
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;

  .title-text {
    color: #345750;
    font-size: 18px;
    font-weight: 600;
    padding: 0;
  }

  .divider {
    color: #94a3b8;
    font-weight: 500;
  }

  .space-name {
    color: #64748b;
    font-size: 16px;
  }

  .space-link {
    color: #ff8e53;
    font-size: 16px;
    text-decoration: none;
    transition: all 0.3s ease;

    &:hover {
      color: #ff6b6b;
      text-decoration: underline;
    }
  }
}

.analysis-grid {
  margin: 0 -8px;
}

.analysis-col {
  padding: 8px;
}

.analysis-card {
  background: var(--header-background);
  color: var(--text-primary);
  border-radius: 16px;
  padding: 20px;
  height: 100%;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  }

  /* 统一子组件样式 */
  :deep(.ant-card) {
    border-radius: 12px;
  }

  :deep(.ant-card-head) {
    border-bottom-color: #f0f0f0;
    min-height: 48px;
  }

  :deep(.ant-card-head-title) {
    color: #345750;
    font-weight: 600;
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  #spaceAnalyzePage {
    padding: 0 12px;
  }

  .page-header {
    padding: 12px 16px;
    margin: -10px -12px 16px;
  }

  .header-title {
    .title-text {
      font-size: 16px;
    }

    .space-name,
    .space-link {
      font-size: 14px;
    }
  }

  .analysis-card {
    padding: 16px;

    :deep(.ant-card-head) {
      min-height: 40px;
      padding: 0 12px;
    }

    :deep(.ant-card-head-title) {
      font-size: 14px;
    }

    :deep(.ant-card-body) {
      padding: 12px;
    }
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .page-header {
    background: linear-gradient(135deg, rgba(255, 246, 243, 0.1) 0%, rgba(255, 237, 231, 0.1) 100%);
  }

  .analysis-card {
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
  }
}
</style>
