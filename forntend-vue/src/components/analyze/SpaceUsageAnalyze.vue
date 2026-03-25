<template>
  <div class="space-user-analyze">
    <a-flex gap="16">
      <a-card title="存储空间" class="analyze-card" :bodyStyle="{ padding: '24px' }">
        <template #extra>
          <a-tooltip title="展示空间存储容量使用情况">
            <InfoCircleOutlined class="info-icon" />
          </a-tooltip>
        </template>
        <div class="usage-container">
          <h3>
            {{ formatSize(data.usedSize) }} /
            {{ data.maxSize ? formatSize(data.maxSize) : '无限制' }}
          </h3>
          <a-progress
            type="dashboard"
            :percent="data.sizeUsageRatio ?? 0"
            :strokeColor="{
              '0%': '#ff8e53',
              '100%': '#ff6b6b',
            }"
            :strokeWidth="8"
          />
        </div>
      </a-card>
      <a-card title="图片数量" class="analyze-card" :bodyStyle="{ padding: '24px' }">
        <template #extra>
          <a-tooltip title="展示空间图片数量使用情况">
            <InfoCircleOutlined class="info-icon" />
          </a-tooltip>
        </template>
        <div class="usage-container">
          <h3>{{ data.usedCount }} / {{ data.maxCount ?? '无限制' }}</h3>
          <a-progress
            type="dashboard"
            :percent="data.countUsageRatio ?? 0"
            :strokeColor="{
              '0%': '#ff8e53',
              '100%': '#ff6b6b',
            }"
            :strokeWidth="8"
          />
        </div>
      </a-card>
    </a-flex>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect } from 'vue'
import { getSpaceUsageAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'
import { InfoCircleOutlined } from '@ant-design/icons-vue'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

// 图表数据
const data = ref<API.SpaceUsageAnalyzeResponse>({})
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceUsageAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})
</script>

<style scoped>
.space-user-analyze {
  .analyze-card {
    flex: 1;
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);

    :deep(.ant-card-head) {
      background: linear-gradient(to right, #fff6f3, #fff);
      border-bottom: 1px solid #f1f5f9;
      padding: 0 24px;
      min-height: 56px;
    }

    :deep(.ant-card-head-title) {
      font-size: 16px;
      font-weight: 600;
      color: #1a1a1a;
    }
  }

  .usage-container {
    height: 320px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 24px;

    h3 {
      color: #1a1a1a;
      font-size: 18px;
      font-weight: 600;
      margin: 0;
    }

    :deep(.ant-progress) {
      .ant-progress-text {
        color: #64748b;
        font-size: 16px;
        font-weight: 500;
      }

      .ant-progress-inner {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      }
    }
  }

  .info-icon {
    color: #94a3b8;
    font-size: 16px;
    cursor: help;
    transition: color 0.3s;

    &:hover {
      color: #ff8e53;
    }
  }
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .space-user-analyze {
    .usage-container {
      height: 280px;

      h3 {
        font-size: 16px;
      }

      :deep(.ant-progress) {
        transform: scale(0.9);
      }
    }
  }
}
</style>
