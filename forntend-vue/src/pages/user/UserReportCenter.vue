<template>
  <div class="report-center">
    <!-- 统计卡片区域 -->
    <section class="stats-container">
      <div class="stats-inner">
        <h2 class="stats-title">举报数据分析</h2>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalReports }}</div>
              <div class="stat-label">总举报</div>
            </div>
          </div>
          <div class="stat-item pending">
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.4 0-8-3.6-8-8s3.6-8 8-8 8 3.6 8 8-3.6 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"></path>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.pendingReports }}</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
          <div class="stat-item handled">
            <div class="stat-icon">
              <svg viewBox="0 0 24 24" width="20" height="20">
                <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41L9 16.17z"></path>
              </svg>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.handledReports }}</div>
              <div class="stat-label">已处理</div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 浮动举报按钮 -->
    <div class="floating-report-btn" @click="openReportModal">
      <svg viewBox="0 0 24 24" width="24" height="24">
        <path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
      </svg>
    </div>

    <!-- 举报模态框 -->
    <ReportModal ref="reportModalRef" @success="handleReportSuccess" />

    <!-- 筛选区域 -->
    <section class="filter-container">
      <div class="filter-inner">
        <h3 class="filter-title">举报筛选</h3>
        <div class="filter-row">
          <div class="filter-item">
            <div class="select-box" @click="toggleSelect('type')">
              <div class="select-text">{{ getSelectText('type') }}</div>
              <div class="select-arrow" :class="{ active: selectOpen.type }">
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path fill="currentColor" d="M7 10l5 5 5-5z"></path>
                </svg>
              </div>
              <div class="select-options" v-show="selectOpen.type">
                <div class="option-item" @click="setSelectValue('type', '')">全部类型</div>
                <div class="option-item" @click="setSelectValue('type', '1')">垃圾广告</div>
                <div class="option-item" @click="setSelectValue('type', '2')">违规内容</div>
                <div class="option-item" @click="setSelectValue('type', '3')">有害信息</div>
                <div class="option-item" @click="setSelectValue('type', '4')">人身攻击</div>
                <div class="option-item" @click="setSelectValue('type', '5')">侵犯隐私</div>
                <div class="option-item" @click="setSelectValue('type', '6')">版权问题</div>
                <div class="option-item" @click="setSelectValue('type', '7')">其他</div>
              </div>
            </div>
          </div>
          <div class="filter-item">
            <div class="select-box" @click="toggleSelect('status')">
              <div class="select-text">{{ getSelectText('status') }}</div>
              <div class="select-arrow" :class="{ active: selectOpen.status }">
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path fill="currentColor" d="M7 10l5 5 5-5z"></path>
                </svg>
              </div>
              <div class="select-options" v-show="selectOpen.status">
                <div class="option-item" @click="setSelectValue('status', '')">全部状态</div>
                <div class="option-item" @click="setSelectValue('status', '0')">待处理</div>
                <div class="option-item" @click="setSelectValue('status', '1')">已处理</div>
                <div class="option-item" @click="setSelectValue('status', '2')">驳回</div>
              </div>
            </div>
          </div>
          <div class="filter-item">
            <div class="select-box" @click="toggleSelect('targetType')">
              <div class="select-text">{{ getSelectText('targetType') }}</div>
              <div class="select-arrow" :class="{ active: selectOpen.targetType }">
                <svg viewBox="0 0 24 24" width="14" height="14">
                  <path fill="currentColor" d="M7 10l5 5 5-5z"></path>
                </svg>
              </div>
              <div class="select-options" v-show="selectOpen.targetType">
                <div class="option-item" @click="setSelectValue('targetType', '')">全部类型</div>
                <div class="option-item" @click="setSelectValue('targetType', '2')">帖子</div>
                <div class="option-item" @click="setSelectValue('targetType', '3')">评论</div>
              </div>
            </div>
          </div>
          <div class="filter-actions">
            <button class="reset-btn" @click="resetSearch">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path fill="currentColor" d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"></path>
              </svg>
              <span>重置</span>
            </button>
            <button class="search-btn" @click="doSearch">筛选</button>
          </div>
        </div>
      </div>
    </section>

    <!-- 举报列表区域 -->
    <section class="list-container" ref="listContainer">
      <div class="list-inner">
        <!-- 加载状态 -->
        <div v-if="loading && dataList.length === 0" class="loading-wrap">
          <div class="loading-spinner"></div>
          <div class="loading-text">加载中...</div>
        </div>

        <!-- 空数据 -->
        <div v-else-if="!loading && dataList.length === 0" class="empty-wrap">
          <div class="empty-icon">
            <svg viewBox="0 0 24 24" width="48" height="48">
              <path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM12 7c2.76 0 5 2.24 5 5s-2.24 5-5 5-5-2.24-5-5 2.24-5 5-5zm0 8c1.65 0 3-1.35 3-3s-1.35-3-3-3-3 1.35-3 3 1.35 3 3 3z"></path>
            </svg>
          </div>
          <div class="empty-text">暂无举报记录</div>
        </div>

        <!-- 列表内容 -->
        <div v-else class="report-list">
          <div v-for="report in dataList" :key="report.id" class="report-card">
            <div class="card-header">
              <div class="report-id">举报ID: {{ report.id }}</div>
              <div class="status-tag" :class="getStatusClass(report.status)">
                {{ report.statusText }}
              </div>
            </div>

            <div class="card-body">
              <!-- 举报人信息 -->
              <div class="info-section">
                <div class="section-title">举报人信息</div>
                <div class="info-row">
                  <div class="info-item">
                    <div class="info-key">用户ID</div>
                    <div class="info-value">{{ report.userId }}</div>
                  </div>
                  <div class="info-item">
                    <div class="info-key">用户名</div>
                    <div class="info-value">{{ report.userName || '未知' }}</div>
                  </div>
                </div>
                <div class="info-row">
                  <div class="info-item">
                    <div class="info-key">用户头像</div>
                    <div class="info-value">
                      <img
                        v-if="report.userAvatar"
                        :src="report.userAvatar"
                        class="user-avatar"
                        alt="用户头像"
                      >
                      <span v-else>无头像</span>
                    </div>
                  </div>
                  <div class="info-item">
                    <div class="info-key">举报时间</div>
                    <div class="info-value">{{ formatTime(report.createTime) }}</div>
                  </div>
                </div>
              </div>

              <!-- 举报对象信息 -->
              <div class="info-section">
                <div class="section-title">举报对象信息</div>
                <div class="info-row">
                  <div class="info-item">
                    <div class="info-key">对象ID</div>
                    <div class="info-value">{{ report.targetId }}</div>
                  </div>
                  <div class="info-item">
                    <div class="info-key">对象类型</div>
                    <div class="info-value">{{ report.targetTypeText || '未知' }}</div>
                  </div>
                </div>
              </div>

              <!-- 举报内容信息 -->
              <div class="info-section">
                <div class="section-title">举报内容信息</div>
                <div class="info-row">
                  <div class="info-item">
                    <div class="info-key">举报类型</div>
                    <div class="info-value">{{ report.reportTypeText || '未知' }}</div>
                  </div>
                  <div class="info-item">
                    <div class="info-key">更新时间</div>
                    <div class="info-value">{{ formatTime(report.updateTime) }}</div>
                  </div>
                </div>
                <div class="info-row full-width">
                  <div class="info-item full-width">
                    <div class="info-key">举报原因</div>
                    <div class="info-value text-content">{{ report.reason || '无' }}</div>
                  </div>
                </div>

                <!-- 截图区域 -->
                <div v-if="report.screenshotUrls && report.screenshotUrls.length > 0" class="screenshot-wrap">
                  <div class="info-key">举报截图</div>
                  <div class="screenshot-grid">
                    <div
                      v-for="(url, index) in report.screenshotUrls"
                      :key="index"
                      class="screenshot-item"
                      @click="previewImage(url)"
                    >
                      <img :src="url" class="screenshot-img" :alt="'截图' + (index + 1)" />
                    </div>
                  </div>
                </div>
              </div>

              <!-- 处理信息 -->
              <div class="info-section">
                <div class="section-title">处理信息</div>
                <div class="info-row">
                  <div class="info-item">
                    <div class="info-key">处理人ID</div>
                    <div class="info-value">{{ report.handlerId || '未处理' }}</div>
                  </div>
                  <div class="info-item">
                    <div class="info-key">处理人名称</div>
                    <div class="info-value">{{ report.handlerName || '未处理' }}</div>
                  </div>
                </div>
                <div class="info-row">
                  <div class="info-item">
                    <div class="info-key">处理时间</div>
                    <div class="info-value">{{ report.handleTime ? formatTime(report.handleTime) : '未处理' }}</div>
                  </div>
                </div>
                <div v-if="report.handleResult" class="info-row full-width">
                  <div class="info-item full-width">
                    <div class="info-key">处理结果</div>
                    <div class="info-value text-content">{{ report.handleResult }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 触底加载提示 -->
      <div v-if="loadingMore" class="loading-more">
        <div class="loading-spinner small"></div>
        <div class="loading-text">加载更多...</div>
      </div>

      <!-- 没有更多数据 -->
      <div v-if="!hasMore && dataList.length > 0" class="no-more">
        没有更多数据了
      </div>
    </section>

    <!-- 图片预览遮罩 -->
    <div v-if="previewImageUrl" class="preview-mask" @click="previewImageUrl = ''">
      <div class="preview-content" @click.stop>
        <img :src="previewImageUrl" class="preview-img" />
        <div class="preview-close" @click="previewImageUrl = ''">
          <svg viewBox="0 0 24 24" width="24" height="24">
            <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z"></path>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, onUnmounted } from 'vue';
import { listMyReportByPageUsingPost } from '@/api/reportController.ts';
import ReportModal from '@/components/ReportModal.vue';
import dayjs from 'dayjs';

// 定义举报记录类型
interface ReportRecord {
  id: number;
  userId: number;
  userName: string;
  userAvatar: string;
  reportType: number;
  reportTypeText: string;
  targetType: number;
  targetTypeText: string;
  targetId: number;
  reason: string;
  status: number;
  statusText: string;
  handlerId: number;
  handlerName: string;
  handleTime: string;
  handleResult: string;
  screenshotUrls: string[];
  createTime: string;
  updateTime: string;
}

// 格式化时间
const formatTime = (time: string | null) => {
  if (!time) return '无';
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
};

// 预览图片
const previewImageUrl = ref('');
const previewImage = (url: string) => {
  previewImageUrl.value = url;
};

// 报告模态框引用
const reportModalRef = ref();

// 打开举报模态框
const openReportModal = () => {
  if (reportModalRef.value) {
    reportModalRef.value.openModal();
  }
};

// 举报成功回调
const handleReportSuccess = () => {
  // 可以在这里添加刷新数据的逻辑
  console.log('举报提交成功');
};

// 选择器状态
const selectOpen = ref({
  type: false,
  status: false,
  targetType: false
});

// 切换选择器
const toggleSelect = (type: string) => {
  Object.keys(selectOpen.value).forEach(key => {
    if (key !== type) selectOpen.value[key as keyof typeof selectOpen.value] = false;
  });
  selectOpen.value[type as keyof typeof selectOpen.value] = !selectOpen.value[type as keyof typeof selectOpen.value];
};

// 搜索条件
const searchParams = reactive({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
  reportType: '',
  status: '',
  targetType: ''
});

// 数据
const dataList = ref<ReportRecord[]>([]);
const total = ref(0);
const loading = ref(false);
const loadingMore = ref(false);
const hasMore = ref(true);
const listContainer = ref<HTMLElement | null>(null);

// 统计数据
const stats = reactive({
  totalReports: 0,
  pendingReports: 0,
  handledReports: 0
});

// 获取选择器显示文本
const getSelectText = (type: string) => {
  const typeMap: Record<string, Record<string, string>> = {
    type: {
      '': '全部类型',
      '1': '垃圾广告',
      '2': '违规内容',
      '3': '有害信息',
      '4': '人身攻击',
      '5': '侵犯隐私',
      '6': '版权问题',
      '7': '其他'
    },
    status: {
      '': '全部状态',
      '0': '待处理',
      '1': '已处理',
      '2': '驳回'
    },
    targetType: {
      '': '全部类型',
      '2': '帖子',
      '3': '评论'
    }
  };
  const paramKey = type === 'type' ? 'reportType' : type;
  return typeMap[type][searchParams[paramKey as keyof typeof searchParams] as string] || '全部';
};

// 设置选择器值
const setSelectValue = (type: string, value: string) => {
  if (type === 'type') searchParams.reportType = value;
  else if (type === 'targetType') searchParams.targetType = value;
  else searchParams.status = value;
  selectOpen.value[type as keyof typeof selectOpen.value] = false;
};

// 获取状态样式
const getStatusClass = (status: number) => {
  if (status === 0) return 'pending';
  if (status === 1) return 'handled';
  if (status === 2) return 'rejected';
  return '';
};

// 重置数据
const resetData = () => {
  dataList.value = [];
  total.value = 0;
  searchParams.current = 1;
  hasMore.value = true;
};

// 重置搜索
const resetSearch = () => {
  searchParams.reportType = '';
  searchParams.status = '';
  searchParams.targetType = '';
  resetData();
  fetchData();
};

// 获取数据
const fetchData = async (isLoadMore = false) => {
  if (isLoadMore && !hasMore.value) return;
  if (isLoadMore) loadingMore.value = true;
  else loading.value = true;

  try {
    const params = {
      ...searchParams,
      reportType: searchParams.reportType ? Number(searchParams.reportType) : undefined,
      status: searchParams.status ? Number(searchParams.status) : undefined,
      targetType: searchParams.targetType ? Number(searchParams.targetType) : undefined
    };
    const res = await listMyReportByPageUsingPost(params);
    if (res.data?.code === 0) {
      const { records, total: totalCount } = res.data.data;
      dataList.value = isLoadMore ? [...dataList.value, ...records] : records;
      total.value = totalCount;
      hasMore.value = dataList.value.length < totalCount;
      stats.totalReports = totalCount;
      stats.pendingReports = dataList.value.filter(item => item.status === 0).length;
      stats.handledReports = dataList.value.filter(item => item.status !== 0).length;
    }
  } catch (error) {
    console.error('获取举报列表失败:', error);
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

// 搜索
const doSearch = () => {
  resetData();
  fetchData();
};

// 加载更多
const loadMore = () => {
  if (loading.value || loadingMore.value || !hasMore.value) return;
  searchParams.current += 1;
  fetchData(true);
};

// 监听滚动
const handleScroll = () => {
  if (!listContainer.value) return;
  const { scrollTop, scrollHeight, clientHeight } = listContainer.value;
  if (scrollTop + clientHeight >= scrollHeight - 50) loadMore();
};

// 初始化
onMounted(() => {
  fetchData();
  if (listContainer.value) listContainer.value.addEventListener('scroll', handleScroll);
});

// 清理
onUnmounted(() => {
  if (listContainer.value) listContainer.value.removeEventListener('scroll', handleScroll);
});
</script>

<style scoped>
/* 全局重置 & 基础样式 */
.report-center {
  background: var(--background);
  font-family: var(--font-family-base);
  font-size: 14px;
  color: var(--text-primary);
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 20px;
  transition: var(--theme-transition);
}

/* 统计区域 - 扁平化风格 */
.stats-container {
  padding: 20px 0;
}
.stats-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px;
  color: var(--text-primary);
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}
.stat-item {
  background: var(--card-background);
  padding: 16px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.stat-item.pending {
  border-color: rgba(244, 67, 54, 0.2);
  background: rgba(244, 67, 54, 0.05);
}
.stat-item.handled {
  border-color: rgba(76, 175, 80, 0.2);
  background: rgba(76, 175, 80, 0.05);
}
.stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-item .stat-icon {
  color: var(--text-secondary);
  background: var(--hover-background);
}
.stat-item.pending .stat-icon {
  color: #f44336;
  background: rgba(244, 67, 54, 0.1);
}
.stat-item.handled .stat-icon {
  color: #4caf50;
  background: rgba(76, 175, 80, 0.1);
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}
.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 筛选区域 - 扁平化 */
.filter-container {
  padding: 16px 0;
  border-top: 1px solid var(--border-color);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
  transition: var(--theme-transition);
}
.filter-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 12px;
  color: var(--text-primary);
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-item {
  flex: 1;
  min-width: 180px;
}
.select-box {
  position: relative;
  height: 36px;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  background: var(--card-background);
  transition: var(--theme-transition);
}
.select-box:hover {
  border-color: var(--text-secondary);
  background: var(--hover-background);
}
.select-text {
  font-size: 14px;
  color: var(--text-primary);
}
.select-arrow {
  color: var(--text-secondary);
  transition: transform 0.2s, var(--theme-transition);
}
.select-arrow.active {
  transform: rotate(180deg);
}
.select-options {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: var(--card-background);
  border: 1px solid var(--border-color);
  border-top: none;
  border-radius: 0 0 4px 4px;
  z-index: 99;
  max-height: 200px;
  overflow-y: auto;
  transition: var(--theme-transition);
}
.option-item {
  padding: 8px 12px;
  font-size: 14px;
  color: var(--text-primary);
  cursor: pointer;
  transition: var(--theme-transition);
}
.option-item:hover {
  background: var(--hover-background);
}
.filter-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.reset-btn, .search-btn {
  height: 36px;
  padding: 0 12px;
  border-radius: 4px;
  border: 1px solid var(--border-color);
  background: var(--card-background);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-primary);
  transition: var(--theme-transition);
}
.reset-btn:hover, .search-btn:hover {
  background: var(--hover-background);
}
.search-btn {
  background: var(--nav-item-active);
  color: var(--nav-item-active-text);
  border-color: var(--nav-item-active-text);
}
.search-btn:hover {
  background: rgba(255, 142, 83, 0.2);
}

/* 列表区域 - 扁平化 */
.list-container {
  padding: 0 0 30px;
  overflow-y: auto;
  transition: var(--theme-transition);
}
.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--text-secondary);
}
.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-color);
  border-top: 2px solid var(--nav-item-active-text);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}
.loading-spinner.small {
  width: 18px;
  height: 18px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}
.loading-text {
  font-size: 14px;
  color: var(--text-secondary);
}
.empty-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  color: var(--text-secondary);
}
.empty-icon {
  color: var(--border-color);
  margin-bottom: 16px;
  transition: var(--theme-transition);
}
.empty-text {
  font-size: 16px;
  color: var(--text-secondary);
}
.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.report-card {
  background: var(--card-background);
  border-radius: 4px;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.card-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.report-id {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}
.status-tag {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.status-tag.pending {
  background: rgba(244, 67, 54, 0.05);
  color: #f44336;
  border-color: rgba(244, 67, 54, 0.2);
}
.status-tag.handled {
  background: rgba(76, 175, 80, 0.05);
  color: #4caf50;
  border-color: rgba(76, 175, 80, 0.2);
}
.status-tag.rejected {
  background: rgba(255, 152, 0, 0.05);
  color: #ff9800;
  border-color: rgba(255, 152, 0, 0.2);
}
.card-body {
  padding: 16px;
}
.info-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.info-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 12px;
  color: var(--text-primary);
}
.info-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
}
.info-row.full-width {
  flex-direction: column;
  gap: 4px;
}
.info-item {
  flex: 1;
  min-width: 150px;
}
.info-item.full-width {
  width: 100%;
  min-width: auto;
}
.info-key {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}
.info-value {
  font-size: 14px;
  color: var(--text-primary);
}
.text-content {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 8px;
  background: var(--hover-background);
  border-radius: 4px;
  transition: var(--theme-transition);
}
.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid var(--border-color);
}
.screenshot-wrap {
  margin-top: 12px;
}
.screenshot-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 8px;
}
.screenshot-item {
  width: 72px;
  height: 72px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.screenshot-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
}
.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: var(--text-secondary);
}
.no-more {
  text-align: center;
  padding: 16px;
  color: var(--text-secondary);
  font-size: 14px;
  margin-bottom: 16px;
}

/* 图片预览 */
.preview-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--comment-drawer-backdrop);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  transition: var(--theme-transition);
}
.preview-content {
  position: relative;
  max-width: 90%;
  max-height: 90vh;
}
.preview-img {
  max-width: 100%;
  max-height: 90vh;
  border-radius: 4px;
}
.preview-close {
  position: absolute;
  top: -32px;
  right: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--card-background);
  color: var(--text-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid var(--border-color);
  transition: var(--theme-transition);
}
.preview-close:hover {
  background: var(--hover-background);
}

/* 浮动举报按钮 */
.floating-report-btn {
  position: fixed;
  right: 24px;
  bottom: 220px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--nav-item-active);
  color: var(--nav-item-active-text);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: none;
  transition: all 0.3s ease;
}

.floating-report-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.2);
}

.floating-report-btn svg {
  width: 24px;
  height: 24px;
}

/* 举报模态框 */
.report-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}
</style>
