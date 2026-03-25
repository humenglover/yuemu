<template>
  <div class="redis-monitor-page">
    <div class="page-header">
      <h2>Redis缓存监控</h2>
      <a-button type="primary" @click="refreshData">
        <template #icon><ReloadOutlined /></template>
        刷新数据
      </a-button>
    </div>

    <a-row :gutter="16">
      <!-- 基本信息卡片 -->
      <a-col :span="24">
        <a-card class="info-card" title="Redis基本信息">
          <a-descriptions :column="{ xxl: 4, xl: 3, lg: 2, md: 2, sm: 1, xs: 1 }">
            <a-descriptions-item label="Redis版本">{{ redisInfo.version }}</a-descriptions-item>
            <a-descriptions-item label="运行模式">{{ redisInfo.mode }}</a-descriptions-item>
            <a-descriptions-item label="操作系统">{{ redisInfo.os }}</a-descriptions-item>
            <a-descriptions-item label="进程ID">{{ redisInfo.processId }}</a-descriptions-item>
            <a-descriptions-item label="运行时长">{{ redisInfo.uptime }} 天</a-descriptions-item>
            <a-descriptions-item label="连接数">{{ redisInfo.clients }}</a-descriptions-item>
            <a-descriptions-item label="内存使用">{{ redisInfo.usedMemory }}</a-descriptions-item>
            <a-descriptions-item label="内存峰值">{{ redisInfo.usedMemoryPeak }}</a-descriptions-item>
            <a-descriptions-item label="总内存">{{ redisInfo.totalMemory }}</a-descriptions-item>
            <a-descriptions-item label="内存碎片率">{{ redisInfo.memoryFragmentation }}</a-descriptions-item>
            <a-descriptions-item label="缓存命中率">{{ redisInfo.hitRate }}</a-descriptions-item>
            <a-descriptions-item label="总命令数">{{ redisInfo.commandsProcessed }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>

      <!-- 内存使用图表 -->
      <a-col :xs="24" :sm="24" :md="12">
        <a-card class="chart-card">
          <div ref="memoryChartRef" class="chart-container"></div>
        </a-card>
      </a-col>

      <!-- 命令统计图表 -->
      <a-col :xs="24" :sm="24" :md="12">
        <a-card class="chart-card">
          <div ref="commandChartRef" class="chart-container"></div>
        </a-card>
      </a-col>

      <!-- 键值统计卡片 -->
      <a-col :span="24">
        <a-card class="stats-card" title="键值统计">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-statistic
                title="总键数"
                :value="keysStats.totalKeys"
                :value-style="{ color: '#3f8600' }"
              >
                <template #prefix>
                  <DatabaseOutlined />
                </template>
              </a-statistic>
            </a-col>
            <a-col :span="8">
              <a-statistic
                title="过期键数"
                :value="keysStats.expiredKeys"
                :value-style="{ color: '#cf1322' }"
              >
                <template #prefix>
                  <ClockCircleOutlined />
                </template>
              </a-statistic>
            </a-col>
            <a-col :span="8">
              <a-statistic
                title="每秒过期键数"
                :value="keysStats.expiredKeysPerSec"
                :precision="2"
                :value-style="{ color: '#1890ff' }"
              >
                <template #prefix>
                  <ThunderboltOutlined />
                </template>
              </a-statistic>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getRedisInfoUsingGet, getKeysStatisticsUsingGet, getMemoryInfoUsingGet } from '@/api/redisCacheMonitorController'
import { ReloadOutlined, DatabaseOutlined, ClockCircleOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'

// 图表引用
const memoryChartRef = ref()
const commandChartRef = ref()
let memoryChart: echarts.ECharts | null = null
let commandChart: echarts.ECharts | null = null

// 数据状态
const redisInfo = ref<any>({})
const keysStats = ref<any>({})
const memoryInfo = ref<any>({})

// 格式化内存大小
const formatMemory = (bytes: number | string) => {
  if (!bytes || bytes === '0') return '0 B';
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  if (typeof bytes === 'string') bytes = parseInt(bytes);
  if (bytes === 0) return '0 B';
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${sizes[i]}`;
};

// 格式化数字
const formatNumber = (num: number | string) => {
  if (!num) return '0';
  return num.toString().replace(/(\d)(?=(\d{3})+(?!\d))/g, '$1,');
};

// 处理Redis信息
const processRedisInfo = (info: any) => {
  if (!info) return {};
  return {
    version: info.redis_version || '-',
    mode: info.redis_mode || 'standalone',
    os: info.os ? info.os.trim() : 'Windows',
    processId: info.process_id || '-',
    uptime: info.uptime_in_days || '0',
    clients: info.connected_clients || '0',
    usedMemory: formatMemory(info.used_memory || 0),
    usedMemoryPeak: formatMemory(info.used_memory_peak || 0),
    totalMemory: formatMemory(info.total_system_memory || info.used_memory_peak || 0),
    memoryFragmentation: info.mem_fragmentation_ratio ? Number(info.mem_fragmentation_ratio).toFixed(2) : '1.00',
    hitRate: calculateHitRate(info),
    commandsProcessed: formatNumber(info.total_commands_processed || 0),
  };
};

// 计算命中率
const calculateHitRate = (info: any) => {
  if (!info) return '0%';
  const hits = parseInt(info.keyspace_hits || '0');
  const misses = parseInt(info.keyspace_misses || '0');
  const total = hits + misses;
  if (total === 0) return '0%';
  return ((hits / total) * 100).toFixed(2) + '%';
};

// 处理内存使用图表数据
const processMemoryChartData = (memoryInfo: any) => {
  if (!memoryInfo) return [];
  const used = parseInt(memoryInfo.usedMemory || '0');
  const total = Math.max(used * 2, parseInt(memoryInfo.usedMemoryPeak || '0')); // 如果没有总内存，使用已用内存的两倍作为估计值
  const available = Math.max(0, total - used);

  return [
    { name: '已用内存', value: used },
    { name: '可用内存', value: available }
  ];
};

// 处理命令统计图表数据
const processCommandChartData = (commandStats: any[]) => {
  if (!commandStats || !Array.isArray(commandStats)) return [];

  // 只显示调用次数最多的前10个命令
  return commandStats
    .sort((a, b) => parseInt(b.value) - parseInt(a.value))
    .slice(0, 10)
    .map(item => ({
      name: item.name.toUpperCase(),
      value: parseInt(item.value)
    }));
};

// 初始化echarts图表选项
const initChartOptions = () => {
  const memoryChartOption = {
    title: {
      text: '内存使用情况',
      left: 'center',
      textStyle: {
        color: 'var(--text-primary)'
      }
    },
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left',
      textStyle: {
        color: 'var(--text-primary)'
      }
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '20',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: []
      }
    ]
  };

  const commandChartOption = {
    title: {
      text: '命令执行统计',
      left: 'center',
      textStyle: {
        color: 'var(--text-primary)'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01],
      axisLabel: {
        color: 'var(--text-primary)'
      }
    },
    yAxis: {
      type: 'category',
      data: [],
      axisLabel: {
        color: 'var(--text-primary)'
      }
    },
    series: [
      {
        type: 'bar',
        data: []
      }
    ]
  };

  return { memoryChartOption, commandChartOption };
};

// 初始化内存使用图表
const initMemoryChart = () => {
  if (!memoryChartRef.value) return
  memoryChart = echarts.init(memoryChartRef.value)
  const option = {
    title: {
      text: '内存使用情况',
      left: 'center'
    },
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}: ${formatMemory(params.value)} (${params.percent}%)`
      }
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      name: '内存使用',
      type: 'pie',
      radius: ['50%', '70%'],
      avoidLabelOverlap: false,
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '20',
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: []
    }]
  }
  memoryChart.setOption(option)
}

// 初始化命令统计图表
const initCommandChart = () => {
  if (!commandChartRef.value) return
  commandChart = echarts.init(commandChartRef.value)
  const option = {
    title: {
      text: '命令执行统计',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}: ${formatNumber(data.value)} 次`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '执行次数'
    },
    yAxis: {
      type: 'category',
      data: [],
      axisLabel: {
        interval: 0,
        rotate: 0
      }
    },
    series: [{
      name: '执行次数',
      type: 'bar',
      data: []
    }]
  }
  commandChart.setOption(option)
}

// 获取Redis监控数据
const fetchData = async () => {
  try {
    const [infoRes, keysRes, memoryRes] = await Promise.all([
      getRedisInfoUsingGet(),
      getKeysStatisticsUsingGet(),
      getMemoryInfoUsingGet()
    ])

    if (infoRes.data?.code === 0) {
      redisInfo.value = processRedisInfo(infoRes.data.data.info)
    }
    if (keysRes.data?.code === 0) {
      keysStats.value = keysRes.data.data
    }
    if (memoryRes.data?.code === 0) {
      memoryInfo.value = memoryRes.data.data
    }

    // 更新图表
    if (memoryChart) {
      const memoryData = processMemoryChartData(memoryInfo.value)
      memoryChart.setOption({
        series: [{
          data: memoryData
        }]
      })
    }

    if (commandChart && infoRes.data?.data?.commandStats) {
      const commandData = processCommandChartData(infoRes.data.data.commandStats)
      commandChart.setOption({
        xAxis: {
          type: 'value'
        },
        yAxis: {
          type: 'category',
          data: commandData.map(item => item.name)
        },
        series: [{
          data: commandData.map(item => item.value)
        }]
      })
    }
  } catch (error) {
    console.error('获取Redis监控数据失败:', error)
  }
}

// 刷新数据
const refreshData = () => {
  fetchData()
}

// 监听窗口大小变化
const handleResize = () => {
  memoryChart?.resize()
  commandChart?.resize()
}

onMounted(() => {
  initMemoryChart()
  initCommandChart()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  memoryChart?.dispose()
  commandChart?.dispose()
})
</script>

<style scoped>
.redis-monitor-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  color: var(--text-primary);
}

.info-card,
.chart-card,
.stats-card {
  margin-bottom: 24px;
  background: var(--background-secondary);
}

.chart-container {
  height: 400px;
  width: 100%;
}

:deep(.ant-card-head-title) {
  color: var(--text-primary);
}

:deep(.ant-statistic-title) {
  color: var(--text-secondary);
}

:deep(.ant-statistic-content) {
  color: var(--text-primary);
}

:deep(.ant-descriptions-item-label) {
  color: var(--text-secondary);
}

:deep(.ant-descriptions-item-content) {
  color: var(--text-primary);
}
</style>
