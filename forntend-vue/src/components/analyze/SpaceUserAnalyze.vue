<template>
  <div class="space-user-analyze">
    <a-card title="空间图片用户分析" class="analyze-card" :bodyStyle="{ padding: '24px' }">
      <template #extra>
        <a-space size="middle" align="center">
          <a-segmented
            v-model:value="timeDimension"
            :options="timeDimensionOptions"
            class="time-selector"
          />
          <a-input-search
            v-model:value="searchText"
            placeholder="请输入用户 id"
            class="search-input"
            :style="{ width: '120px' }"
            @search="doSearch"
          >
            <template #enterButton>
              <SearchOutlined />
            </template>
          </a-input-search>
          <a-tooltip title="展示用户图片上传趋势">
            <InfoCircleOutlined class="info-icon" />
          </a-tooltip>
        </a-space>
      </template>
      <v-chart :option="options" class="chart-container" :loading="loading" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceUserAnalyzeUsingPost } from '@/api/spaceAnalyzeController'
import { message } from 'ant-design-vue'
import { InfoCircleOutlined, SearchOutlined } from '@ant-design/icons-vue'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

// 时间维度选项
const timeDimension = ref<'day' | 'week' | 'month'>('day')
// 分段选择器组件的选项
const timeDimensionOptions = [
  {
    label: '日',
    value: 'day',
  },
  {
    label: '周',
    value: 'week',
  },
  {
    label: '月',
    value: 'month',
  },
]
// 用户选项
const userId = ref<string>()
const searchText = ref('')
const doSearch = (value: string) => {
  userId.value = value
}

// 图表数据
const dataList = ref<API.SpaceCategoryAnalyzeResponse>([])
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceUserAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    timeDimension: timeDimension.value,
    userId: userId.value,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
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

// 图表选项
const options = computed(() => {
  const periods = dataList.value.map((item) => item.period)
  const counts = dataList.value.map((item) => item.count)

  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e5e7eb',
      textStyle: { color: '#1f2937' },
      borderWidth: 1,
      padding: [8, 12],
    },
    grid: {
      top: 40,
      bottom: 40,
      left: '3%',
      right: '4%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: periods,
      name: '时间区间',
      axisLabel: {
        color: '#64748b',
        fontSize: 12,
      },
      axisLine: {
        lineStyle: { color: '#e2e8f0' },
      },
    },
    yAxis: {
      type: 'value',
      name: '上传数量',
      nameTextStyle: { color: '#64748b' },
      axisLabel: { color: '#64748b' },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9',
          type: 'dashed',
        },
      },
    },
    series: [
      {
        name: '上传数量',
        type: 'line',
        data: counts,
        smooth: true,
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#ff8e53' },
            { offset: 1, color: '#ff6b6b' },
          ]),
        },
        itemStyle: {
          color: '#ff8e53',
          borderWidth: 2,
          borderColor: '#fff',
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255, 142, 83, 0.2)' },
            { offset: 1, color: 'rgba(255, 107, 107, 0.05)' },
          ]),
        },
        emphasis: {
          focus: 'series',
          scale: true,
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.2)',
          },
        },
      },
    ],
  }
})
</script>

<style scoped>
.analyze-card {
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

.time-selector {
  :deep(.ant-segmented) {
    background: #f8fafc;

    .ant-segmented-item-selected {
      background: #ff8e53;
      color: white;
    }
  }
}

.search-input {
  :deep(.ant-input) {
    border-radius: 8px 0 0 8px;
    border-color: #e2e8f0;

    &:hover,
    &:focus {
      border-color: #ff8e53;
    }
  }

  :deep(.ant-btn) {
    border-radius: 0 8px 8px 0;
    background: #ff8e53;
    border-color: #ff8e53;

    &:hover {
      background: #ff6b6b;
      border-color: #ff6b6b;
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

.chart-container {
  height: 320px;
  max-width: 100%;
  margin: 0 -12px;
}

/* 响应式调整 */
@media screen and (max-width: 768px) {
  .chart-container {
    height: 280px;
  }
}
</style>
